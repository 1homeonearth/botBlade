import { randomUUID } from "node:crypto";
import { mkdir, readFile, stat, writeFile } from "node:fs/promises";
import path from "node:path";
import { execFileSync } from "node:child_process";
import type { AuditService } from "../auditService.js";
import { scanAndGenerateBotbladeMetadata } from "../importScan/index.js";
import type { ImportStorePersistence } from "../persistence.js";
import type { BotProfileScriptProfile } from "../../models/botProfile.js";
import { extractZipFromPlan, validateZipArchive, type ZipViolation, type ZipViolationCode } from "./zipSecurity.js";
import { normalizeProjectRelativePath } from "../security/projectPaths.js";

export type ImportSource =
  | { type: "git"; repoUrl: string; ref?: string }
  | { type: "zip"; archivePath: string }
  | { type: "folder"; folderPath: string }
  | { type: "workflow_json"; workflowPath?: string; workflowJson?: unknown }
  | { type: "template"; templateId: string }
  | { type: "repair"; projectId: string; reason?: string };

export type ImportState = "pending" | "validating_source" | "cloning" | "scanning" | "detecting" | "profile_ready" | "needs_secrets" | "ready" | "blocked" | "blocked_by_policy" | "failed";
export interface ImportFailureDetails { cause: string; evidence: string; safeAction: string }
export interface CloneFailureDetails extends ImportFailureDetails { policyCode: string; errorCode: string }
export interface SecurityCard { title: string; summary: string; remediation: string; violations: ZipViolation[] }
export interface GitCloneMetadata { remoteUrl: string; ref: string | null; clonedAt: string; auditEventIds: string[] }

const WORKFLOW_JSON_MAX_BYTES = 512 * 1024;

const FIRST_PARTY_TEMPLATES: Record<string, { name: string; files: Record<string, string> }> = {
  "n8n-empty-workflow": {
    name: "n8n Empty Workflow",
    files: {
      "workflow.json": JSON.stringify({ nodes: [], connections: {}, meta: { templateId: "n8n-empty-workflow" } }, null, 2) + "\n",
      "README.md": "# n8n Empty Workflow\n\nFirst-party BotBlade workflow template for static import inspection. No workflow code is executed during import.\n",
    },
  },
  "discord-slash-bot": {
    name: "Discord Slash Bot",
    files: {
      "package.json": JSON.stringify({ name: "discord-slash-bot", version: "0.1.0", private: true, type: "module", dependencies: { "discord.js": "14.15.3" }, scripts: { start: "node src/index.js" } }, null, 2) + "\n",
      "src/index.js": "import { Client, GatewayIntentBits } from \"discord.js\";\n\nexport const client = new Client({ intents: [GatewayIntentBits.Guilds] });\n",
      "README.md": "# Discord Slash Bot\n\nFirst-party BotBlade template materialized for static import inspection. Configure DISCORD_TOKEN through BotBlade secrets before running.\n",
    },
  },
};

export function knownImportTemplateIds(): string[] { return Object.keys(FIRST_PARTY_TEMPLATES).sort(); }

export interface ImportRecord {
  id: string; source: ImportSource; workspacePath: string; state: ImportState; createdAt: string; updatedAt: string; failure?: ImportFailureDetails; securityCards?: SecurityCard[]; clone?: GitCloneMetadata; managedProject?: { id: string; slug: string }; detectedScriptProfiles?: BotProfileScriptProfile[];
}

export class ImportStore {
  private readonly records = new Map<string, ImportRecord>();
  constructor(private readonly persistence?: ImportStorePersistence) { for (const record of persistence?.loadImportRecords() ?? []) this.records.set(record.id, record); }
  get(id: string): ImportRecord | undefined { return this.records.get(id); }

  attachManagedProject(id: string, managedProject: { id: string; slug: string }): ImportRecord | undefined {
    const record = this.records.get(id);
    if (!record) return undefined;
    record.managedProject = managedProject;
    record.updatedAt = new Date().toISOString();
    this.save(record);
    return record;
  }

  async createAndRun(source: ImportSource, workspacePath: string, auditService: AuditService, actorId: string, requestId: string): Promise<ImportRecord> {
    const now = new Date().toISOString();
    const id = `import_${randomUUID()}`;
    const effectiveWorkspacePath = ["zip", "workflow_json", "template"].includes(source.type) ? path.resolve(workspacePath, id) : workspacePath;
    const record: ImportRecord = { id, source: this.publicImportSource(source), workspacePath: effectiveWorkspacePath, state: "pending", createdAt: now, updatedAt: now };
    this.save(record);
    auditService.record({ action: "import.start", actorId, projectId: "global", resourceType: "import", resourceId: id, metadata: { sourceType: source.type, workspacePath: effectiveWorkspacePath }, requestId });
    try {
      if (source.type === "git") await this.validateAndCloneGit(record, source, workspacePath, auditService, actorId, requestId);
      if (source.type === "zip") await this.validateAndExtractZip(record, source.archivePath, auditService, actorId, requestId);
      if (source.type === "workflow_json") await this.validateAndMaterializeWorkflowJson(record, source, auditService, actorId, requestId);
      if (source.type === "template") await this.validateAndMaterializeTemplate(record, source, auditService, actorId, requestId);
      this.transition(record, "scanning", auditService, actorId, requestId);
      this.transition(record, "detecting", auditService, actorId, requestId);
      const result = await scanAndGenerateBotbladeMetadata(record.workspacePath, { kind: source.type, url: source.type === "git" ? source.repoUrl : undefined });
      record.detectedScriptProfiles = result.detection.scriptProfiles;
      this.save(record);
      this.transition(record, "profile_ready", auditService, actorId, requestId);
      this.transition(record, (result.detection.matches[0]?.requiredSecrets.length ?? 0) > 0 ? "needs_secrets" : "ready", auditService, actorId, requestId);
    } catch (error) {
      if (record.state !== "blocked_by_policy") {
        record.failure = { cause: "Import scan failed.", evidence: error instanceof Error ? error.message : String(error), safeAction: "Verify source path/archive integrity and retry import." };
        this.transition(record, "failed", auditService, actorId, requestId);
      }
      auditService.record({ action: "import.failure", actorId, projectId: "global", resourceType: "import", resourceId: id, metadata: { cause: record.failure?.cause, safeAction: record.failure?.safeAction }, requestId });
    }
    return record;
  }
  private async validateAndCloneGit(record: ImportRecord, source: Extract<ImportSource, { type: "git" }>, workspaceRoot: string, auditService: AuditService, actorId: string, requestId: string): Promise<void> {
    this.transition(record, "validating_source", auditService, actorId, requestId);
    const slug = this.sanitizeSlug(this.extractRepoNameFromUrl(source.repoUrl));
    const managedPath = path.resolve(workspaceRoot, `${record.id}-${slug}`);
    record.workspacePath = managedPath;
    await mkdir(managedPath, { recursive: true });
    this.transition(record, "cloning", auditService, actorId, requestId);
    try {
      const args = source.ref ? ["clone", "--branch", source.ref, "--single-branch", source.repoUrl, managedPath] : ["clone", source.repoUrl, managedPath];
      execFileSync("git", args);
      const cloneAudit = auditService.record({ action: "import.state_transition", actorId, projectId: "global", resourceType: "import", resourceId: record.id, metadata: { state: "clone_complete", remoteUrl: source.repoUrl, ref: source.ref ?? null }, requestId });
      record.clone = { remoteUrl: source.repoUrl, ref: source.ref ?? null, clonedAt: new Date().toISOString(), auditEventIds: [cloneAudit.id] };
      this.save(record);
    } catch (error) {
      const failure: CloneFailureDetails = { cause: "Git clone failed.", evidence: error instanceof Error ? error.message : String(error), safeAction: "Verify repository URL, ref, and access policy before retrying.", policyCode: "IMPORT_GIT_CLONE_BLOCKED", errorCode: "GIT_CLONE_FAILED" };
      record.failure = failure;
      this.transition(record, "failed", auditService, actorId, requestId);
      throw new Error(failure.errorCode);
    }
  }

  private extractRepoNameFromUrl(rawUrl: string): string {
    const value = rawUrl.trim();
    if (!/^https?:\/\/|^ssh:\/\//.test(value)) throw new Error("GIT_PROTOCOL_NOT_ALLOWED");
    if (!/^[a-z]+:\/\/[^/]+\/.+/.test(value)) throw new Error("INVALID_GIT_URL_FORMAT");
    const pathname = value.split("://")[1]?.split("/").slice(1).join("/") ?? "";
    const last = pathname.split("/").filter(Boolean).pop() ?? "repo";
    return last.endsWith(".git") ? last.slice(0, -4) : last;
  }

  private sanitizeSlug(value: string): string { return value.trim().toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "") || "repo"; }

  private async validateAndExtractZip(record: ImportRecord, archivePath: string, auditService: AuditService, actorId: string, requestId: string): Promise<void> {
    const validation = await validateZipArchive(archivePath);
    if (!validation.ok) {
      record.failure = { cause: "Archive blocked by security policy.", evidence: JSON.stringify(validation.violations), safeAction: "Review security card and repackage archive with safe paths and regular files only." };
      record.securityCards = [{ title: "Import blocked by policy", summary: `${validation.violations.length} violation(s) were found in the archive.`, remediation: "Remove traversal paths, absolute/drive-prefixed names, symlinks, and oversized entries before retrying.", violations: validation.violations }];
      this.transition(record, "blocked_by_policy", auditService, actorId, requestId);
      throw new Error("blocked_by_policy");
    }

    await mkdir(record.workspacePath, { recursive: true });
    await extractZipFromPlan(archivePath, record.workspacePath, validation.extractionPlan);
  }

  private async validateAndMaterializeWorkflowJson(record: ImportRecord, source: Extract<ImportSource, { type: "workflow_json" }>, auditService: AuditService, actorId: string, requestId: string): Promise<void> {
    this.transition(record, "validating_source", auditService, actorId, requestId);
    let rawJson: string;
    if (typeof source.workflowPath === "string" && source.workflowPath.trim()) {
      let fileStat;
      try {
        fileStat = await stat(source.workflowPath);
      } catch (error) {
        this.blockByWorkflowPolicy(record, [{ code: "WORKFLOW_JSON_NOT_FOUND", message: error instanceof Error ? error.message : String(error) }], auditService, actorId, requestId);
        throw new Error("blocked_by_policy");
      }
      if (!fileStat.isFile()) {
        this.blockByWorkflowPolicy(record, [{ code: "WORKFLOW_JSON_NOT_FILE", message: "Workflow JSON source must be a regular file." }], auditService, actorId, requestId);
        throw new Error("blocked_by_policy");
      }
      if (fileStat.size > WORKFLOW_JSON_MAX_BYTES) {
        this.blockByWorkflowPolicy(record, [{ code: "WORKFLOW_JSON_TOO_LARGE", message: `Workflow JSON exceeds ${WORKFLOW_JSON_MAX_BYTES} bytes.` }], auditService, actorId, requestId);
        throw new Error("blocked_by_policy");
      }
      rawJson = await readFile(source.workflowPath, "utf8");
    } else if (typeof source.workflowJson === "string") {
      rawJson = source.workflowJson;
    } else if (source.workflowJson !== undefined) {
      rawJson = JSON.stringify(source.workflowJson);
    } else {
      this.blockByWorkflowPolicy(record, [{ code: "WORKFLOW_JSON_SOURCE_REQUIRED", message: "workflowPath or workflowJson is required." }], auditService, actorId, requestId);
      throw new Error("blocked_by_policy");
    }

    if (Buffer.byteLength(rawJson, "utf8") > WORKFLOW_JSON_MAX_BYTES) {
      this.blockByWorkflowPolicy(record, [{ code: "WORKFLOW_JSON_TOO_LARGE", message: `Workflow JSON exceeds ${WORKFLOW_JSON_MAX_BYTES} bytes.` }], auditService, actorId, requestId);
      throw new Error("blocked_by_policy");
    }

    let parsed: unknown;
    try {
      parsed = JSON.parse(rawJson);
    } catch {
      this.blockByWorkflowPolicy(record, [{ code: "WORKFLOW_JSON_INVALID", message: "Workflow JSON must parse as valid JSON before scanning." }], auditService, actorId, requestId);
      throw new Error("blocked_by_policy");
    }
    await mkdir(record.workspacePath, { recursive: true });
    await writeFile(path.join(record.workspacePath, "workflow.json"), JSON.stringify(parsed, null, 2) + "\n", "utf8");
  }

  private async validateAndMaterializeTemplate(record: ImportRecord, source: Extract<ImportSource, { type: "template" }>, auditService: AuditService, actorId: string, requestId: string): Promise<void> {
    this.transition(record, "validating_source", auditService, actorId, requestId);
    const template = FIRST_PARTY_TEMPLATES[source.templateId];
    if (!template) {
      this.blockTemplatePolicy(record, source.templateId, auditService, actorId, requestId);
      throw new Error("blocked_by_policy");
    }
    await mkdir(record.workspacePath, { recursive: true });
    for (const [relativePath, content] of Object.entries(template.files)) {
      const normalized = normalizeProjectRelativePath(relativePath, { allowRoot: false });
      if (!normalized.ok || !normalized.path) {
        this.blockTemplatePolicy(record, source.templateId, auditService, actorId, requestId);
        throw new Error("blocked_by_policy");
      }
      const target = path.resolve(record.workspacePath, normalized.path);
      if (!target.startsWith(path.resolve(record.workspacePath) + path.sep)) {
        this.blockTemplatePolicy(record, source.templateId, auditService, actorId, requestId);
        throw new Error("blocked_by_policy");
      }
      await mkdir(path.dirname(target), { recursive: true });
      await writeFile(target, content, "utf8");
    }
  }

  private blockByWorkflowPolicy(record: ImportRecord, violations: Array<{ code: ZipViolationCode; message: string }>, auditService: AuditService, actorId: string, requestId: string): void {
    record.failure = { cause: "Workflow JSON blocked by import policy.", evidence: JSON.stringify(violations), safeAction: "Provide valid workflow JSON under the size limit and retry import." };
    record.securityCards = [{ title: "Workflow JSON import blocked", summary: `${violations.length} violation(s) were found before scanning.`, remediation: "Use a regular JSON file under the workflow size limit. BotBlade performs static scanning only.", violations: violations.map((violation) => ({ code: violation.code, entryPath: "workflow.json", detail: violation.message })) as ZipViolation[] }];
    this.transition(record, "blocked_by_policy", auditService, actorId, requestId);
  }

  private blockTemplatePolicy(record: ImportRecord, templateId: string, auditService: AuditService, actorId: string, requestId: string): void {
    const allowed = knownImportTemplateIds();
    record.failure = { cause: "Template blocked by import policy.", evidence: JSON.stringify({ templateId, allowedTemplateIds: allowed }), safeAction: "Choose a first-party template ID from the local allowlist." };
    record.securityCards = [{ title: "Template import blocked", summary: "The requested template ID is not in the local first-party allowlist.", remediation: "Use one of the advertised first-party template IDs.", violations: [{ code: "TEMPLATE_ID_NOT_ALLOWED", entryPath: templateId, detail: "Template ID is not allowed for import." }] as ZipViolation[] }];
    this.transition(record, "blocked_by_policy", auditService, actorId, requestId);
  }

  private publicImportSource(source: ImportSource): ImportSource {
    if (source.type !== "workflow_json") return source;
    return source.workflowPath ? { type: "workflow_json", workflowPath: source.workflowPath } : { type: "workflow_json" };
  }

  private transition(record: ImportRecord, state: ImportState, auditService: AuditService, actorId: string, requestId: string): void {
    record.state = state; record.updatedAt = new Date().toISOString(); this.save(record);
    auditService.record({ action: "import.state_transition", actorId, projectId: "global", resourceType: "import", resourceId: record.id, metadata: { state }, requestId });
  }
  private save(record: ImportRecord): void { this.records.set(record.id, record); this.persistence?.saveImportRecord(record); }
}
