import { randomUUID } from "node:crypto";
import { mkdir } from "node:fs/promises";
import path from "node:path";
import { execFileSync } from "node:child_process";
import type { AuditService } from "../auditService.js";
import { scanAndGenerateBotbladeMetadata } from "../importScan/index.js";
import type { ImportStorePersistence } from "../persistence.js";
import { extractZipFromPlan, validateZipArchive, type ZipViolation } from "./zipSecurity.js";

export type ImportSource =
  | { type: "git"; repoUrl: string; ref?: string }
  | { type: "zip"; archivePath: string }
  | { type: "folder"; folderPath: string }
  | { type: "workflow_json"; workflowPath: string }
  | { type: "template"; templateId: string }
  | { type: "repair"; projectId: string; reason?: string };

export type ImportState = "pending" | "validating_source" | "cloning" | "scanning" | "detecting" | "profile_ready" | "needs_secrets" | "ready" | "blocked" | "blocked_by_policy" | "failed";
export interface ImportFailureDetails { cause: string; evidence: string; safeAction: string }
export interface CloneFailureDetails extends ImportFailureDetails { policyCode: string; errorCode: string }
export interface SecurityCard { title: string; summary: string; remediation: string; violations: ZipViolation[] }
export interface GitCloneMetadata { remoteUrl: string; ref: string | null; clonedAt: string; auditEventIds: string[] }
export interface ImportRecord {
  id: string; source: ImportSource; workspacePath: string; state: ImportState; createdAt: string; updatedAt: string; failure?: ImportFailureDetails; securityCards?: SecurityCard[]; clone?: GitCloneMetadata; managedProject?: { id: string; slug: string };
}

export class ImportStore {
  private readonly records = new Map<string, ImportRecord>();
  constructor(private readonly persistence?: ImportStorePersistence) { for (const record of persistence?.loadImportRecords() ?? []) this.records.set(record.id, record); }
  get(id: string): ImportRecord | undefined { return this.records.get(id); }

  async createAndRun(source: ImportSource, workspacePath: string, auditService: AuditService, actorId: string, requestId: string): Promise<ImportRecord> {
    const now = new Date().toISOString();
    const id = `import_${randomUUID()}`;
    const effectiveWorkspacePath = source.type === "zip" ? path.resolve(workspacePath, id) : workspacePath;
    const record: ImportRecord = { id, source, workspacePath: effectiveWorkspacePath, state: "pending", createdAt: now, updatedAt: now };
    this.save(record);
    auditService.record({ action: "import.start", actorId, projectId: "global", resourceType: "import", resourceId: id, metadata: { sourceType: source.type, workspacePath: effectiveWorkspacePath }, requestId });
    try {
      if (source.type === "git") await this.validateAndCloneGit(record, source, workspacePath, auditService, actorId, requestId);
      if (source.type === "zip") await this.validateAndExtractZip(record, source.archivePath, auditService, actorId, requestId);
      this.transition(record, "scanning", auditService, actorId, requestId);
      this.transition(record, "detecting", auditService, actorId, requestId);
      const result = await scanAndGenerateBotbladeMetadata(effectiveWorkspacePath, { kind: source.type, url: source.type === "git" ? source.repoUrl : undefined });
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

  private transition(record: ImportRecord, state: ImportState, auditService: AuditService, actorId: string, requestId: string): void {
    record.state = state; record.updatedAt = new Date().toISOString(); this.save(record);
    auditService.record({ action: "import.state_transition", actorId, projectId: "global", resourceType: "import", resourceId: record.id, metadata: { state }, requestId });
  }
  private save(record: ImportRecord): void { this.records.set(record.id, record); this.persistence?.saveImportRecord(record); }
}
