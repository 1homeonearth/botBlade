import { randomUUID } from "node:crypto";
import type { AuditService } from "../auditService.js";
import { scanAndGenerateBotbladeMetadata } from "../importScan/index.js";
import type { ImportStorePersistence } from "../persistence.js";

export type ImportSource =
  | { type: "git"; repoUrl: string; ref?: string }
  | { type: "zip"; archivePath: string }
  | { type: "folder"; folderPath: string }
  | { type: "workflow_json"; workflowPath: string }
  | { type: "template"; templateId: string }
  | { type: "repair"; projectId: string; reason?: string };

export type ImportState = "pending" | "scanning" | "detecting" | "profile_ready" | "needs_secrets" | "ready" | "blocked" | "failed";

export interface ImportFailureDetails { cause: string; evidence: string; safeAction: string }

export interface ImportRecord {
  id: string;
  source: ImportSource;
  workspacePath: string;
  state: ImportState;
  createdAt: string;
  updatedAt: string;
  failure?: ImportFailureDetails;
}

export class ImportStore {
  private readonly records = new Map<string, ImportRecord>();
  constructor(private readonly persistence?: ImportStorePersistence) { for (const record of persistence?.loadImportRecords() ?? []) this.records.set(record.id, record); }
  get(id: string): ImportRecord | undefined { return this.records.get(id); }

  async createAndRun(source: ImportSource, workspacePath: string, auditService: AuditService, actorId: string, requestId: string): Promise<ImportRecord> {
    const now = new Date().toISOString();
    const id = `import_${randomUUID()}`;
    const record: ImportRecord = { id, source, workspacePath, state: "pending", createdAt: now, updatedAt: now };
    this.save(record);
    auditService.record({ action: "import.start", actorId, projectId: "global", resourceType: "import", resourceId: id, metadata: { sourceType: source.type, workspacePath }, requestId });
    try {
      this.transition(record, "scanning", auditService, actorId, requestId);
      this.transition(record, "detecting", auditService, actorId, requestId);
      const result = await scanAndGenerateBotbladeMetadata(workspacePath, { kind: source.type, url: source.type === "git" ? source.repoUrl : undefined });
      this.transition(record, "profile_ready", auditService, actorId, requestId);
      this.transition(record, (result.detection.matches[0]?.requiredSecrets.length ?? 0) > 0 ? "needs_secrets" : "ready", auditService, actorId, requestId);
    } catch (error) {
      record.failure = { cause: "Import scan failed.", evidence: error instanceof Error ? error.message : String(error), safeAction: "Verify source path/archive integrity and retry import." };
      this.transition(record, "failed", auditService, actorId, requestId);
      auditService.record({ action: "import.failure", actorId, projectId: "global", resourceType: "import", resourceId: id, metadata: { cause: record.failure.cause, safeAction: record.failure.safeAction }, requestId });
    }
    return record;
  }

  private transition(record: ImportRecord, state: ImportState, auditService: AuditService, actorId: string, requestId: string): void {
    record.state = state;
    record.updatedAt = new Date().toISOString();
    this.save(record);
    auditService.record({ action: "import.state_transition", actorId, projectId: "global", resourceType: "import", resourceId: record.id, metadata: { state }, requestId });
  }

  private save(record: ImportRecord): void { this.records.set(record.id, record); this.persistence?.saveImportRecord(record); }
}
