// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import crypto from "node:crypto";  // line 7: executes this statement as part of this file's behavior
import fs from "node:fs";  // line 8: executes this statement as part of this file's behavior
import path from "node:path";  // line 9: executes this statement as part of this file's behavior
import { execFileSync } from "node:child_process";  // line 10: executes this statement as part of this file's behavior
import type { AuditEvent } from "../services/auditService.js";  // line 11: executes this statement as part of this file's behavior
import type { BuildJob } from "../services/buildService.js";  // line 12: executes this statement as part of this file's behavior
import type { DeploymentJob } from "../services/deploymentJobs.js";  // line 13: executes this statement as part of this file's behavior
import type { DeploymentTarget } from "../services/deploymentTargets.js";  // line 14: executes this statement as part of this file's behavior
import type { BotProject } from "../models/project.js";  // line 15: executes this statement as part of this file's behavior
import type { AuditServicePersistence, BuildServicePersistence, DeploymentJobStorePersistence, DeploymentTargetStorePersistence, ProjectStorePersistence, SecretRecord, SecretStorePersistence } from "../services/persistence.js";  // line 16: executes this statement as part of this file's behavior

const MIGRATION_DIR = fs.existsSync(path.resolve(process.cwd(), "migrations")) ? path.resolve(process.cwd(), "migrations") : path.resolve(process.cwd(), "backend/migrations");  // line 18: executes this statement as part of this file's behavior
const DEFAULT_KEY_ID = "local-env";  // line 19: executes this statement as part of this file's behavior

export class SqlitePersistence implements ProjectStorePersistence, SecretStorePersistence, AuditServicePersistence, BuildServicePersistence, DeploymentTargetStorePersistence, DeploymentJobStorePersistence {  // line 21: executes this statement as part of this file's behavior
  private readonly key: Buffer;  // line 22: executes this statement as part of this file's behavior

  constructor(private readonly databasePath: string, secretKey = process.env.BOTBLADE_SECRET_KEY) {  // line 24: executes this statement as part of this file's behavior
    ensureSqliteAvailable();  // line 25: executes this statement as part of this file's behavior
    fs.mkdirSync(path.dirname(databasePath), { recursive: true });  // line 26: executes this statement as part of this file's behavior
    this.key = deriveKey(secretKey);  // line 27: executes this statement as part of this file's behavior
    this.migrate();  // line 28: executes this statement as part of this file's behavior
  }  // line 29: executes this statement as part of this file's behavior

  static fromUrl(url = process.env.BOTBLADE_DATABASE_URL ?? process.env.DATABASE_URL ?? defaultSqliteUrl()): SqlitePersistence | undefined {  // line 31: executes this statement as part of this file's behavior
    if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {  // line 32: executes this statement as part of this file's behavior
      console.warn("Postgres persistence is configured but this dependency-free build only ships the SQLite adapter; use the documented DATABASE_URL for production Postgres deployments.");  // line 33: executes this statement as part of this file's behavior
      return undefined;  // line 34: executes this statement as part of this file's behavior
    }  // line 35: executes this statement as part of this file's behavior
    if (!url.startsWith("sqlite://")) return undefined;  // line 36: executes this statement as part of this file's behavior
    return new SqlitePersistence(path.resolve(url.slice("sqlite://".length)));  // line 37: executes this statement as part of this file's behavior
  }  // line 38: executes this statement as part of this file's behavior

  loadProjects(): BotProject[] { return this.selectJson<BotProject>("SELECT data_json FROM projects ORDER BY updated_at DESC", "data_json"); }  // line 40: executes this statement as part of this file's behavior
  saveProject(project: BotProject): void { this.exec(`INSERT INTO projects (id, slug, updated_at, data_json) VALUES (${q(project.id)}, ${q(project.slug)}, ${q(project.updatedAt)}, ${q(JSON.stringify(project))}) ON CONFLICT(id) DO UPDATE SET slug=excluded.slug, updated_at=excluded.updated_at, data_json=excluded.data_json;`); }  // line 41: executes this statement as part of this file's behavior
  deleteProject(projectId: string): void { this.exec(`DELETE FROM projects WHERE id=${q(projectId)};`); }  // line 42: executes this statement as part of this file's behavior

  loadSecrets(): SecretRecord[] {  // line 44: executes this statement as part of this file's behavior
    return this.rows<{ summary_json: string; encrypted_value: string }>("SELECT m.summary_json, v.encrypted_value FROM secret_metadata m JOIN secret_values v ON v.id = m.id ORDER BY m.updated_at DESC").map((row) => ({ ...JSON.parse(row.summary_json) as Omit<SecretRecord, "value">, value: decrypt(row.encrypted_value, this.key) }));  // line 45: executes this statement as part of this file's behavior
  }  // line 46: executes this statement as part of this file's behavior
  saveSecret(secret: SecretRecord): void {  // line 47: executes this statement as part of this file's behavior
    const { value: _value, ...summary } = secret;  // line 48: executes this statement as part of this file's behavior
    const now = new Date().toISOString();  // line 49: executes this statement as part of this file's behavior
    this.exec(`BEGIN;  // line 50: executes this statement as part of this file's behavior
INSERT INTO secret_metadata (id, project_id, name, type, storage_mode, fingerprint, created_at, updated_at, rotated_at, summary_json) VALUES (${q(secret.id)}, ${q(secret.projectId)}, ${q(secret.name)}, ${q(secret.type)}, ${q(secret.storageMode)}, ${q(secret.fingerprint)}, ${q(secret.createdAt)}, ${q(secret.updatedAt)}, ${q(secret.rotatedAt)}, ${q(JSON.stringify(summary))}) ON CONFLICT(id) DO UPDATE SET project_id=excluded.project_id, name=excluded.name, type=excluded.type, storage_mode=excluded.storage_mode, fingerprint=excluded.fingerprint, updated_at=excluded.updated_at, rotated_at=excluded.rotated_at, summary_json=excluded.summary_json;  // line 51: executes this statement as part of this file's behavior
INSERT INTO secret_values (id, encrypted_value, encryption_key_id, updated_at) VALUES (${q(secret.id)}, ${q(encrypt(secret.value, this.key))}, ${q(DEFAULT_KEY_ID)}, ${q(now)}) ON CONFLICT(id) DO UPDATE SET encrypted_value=excluded.encrypted_value, encryption_key_id=excluded.encryption_key_id, updated_at=excluded.updated_at;  // line 52: executes this statement as part of this file's behavior
COMMIT;`);  // line 53: executes this statement as part of this file's behavior
  }  // line 54: executes this statement as part of this file's behavior
  deleteSecret(secretId: string): void { this.exec(`DELETE FROM secret_values WHERE id=${q(secretId)}; DELETE FROM secret_metadata WHERE id=${q(secretId)};`); }  // line 55: executes this statement as part of this file's behavior

  loadAuditEvents(): AuditEvent[] { return this.selectJson<AuditEvent>("SELECT data_json FROM audit_events ORDER BY created_at DESC", "data_json"); }  // line 57: executes this statement as part of this file's behavior
  saveAuditEvent(event: AuditEvent): void { this.exec(`INSERT INTO audit_events (id, project_id, action, created_at, data_json) VALUES (${q(event.id)}, ${q(event.projectId)}, ${q(event.action)}, ${q(event.createdAt)}, ${q(JSON.stringify(event))}) ON CONFLICT(id) DO UPDATE SET project_id=excluded.project_id, action=excluded.action, created_at=excluded.created_at, data_json=excluded.data_json;`); }  // line 58: executes this statement as part of this file's behavior

  loadBuildJobs(): Array<{ job: BuildJob; logs: string }> { return this.rows<{ data_json: string; logs: string }>("SELECT data_json, logs FROM build_jobs ORDER BY started_at DESC").map((row) => ({ job: JSON.parse(row.data_json) as BuildJob, logs: row.logs })); }  // line 60: executes this statement as part of this file's behavior
  saveBuildJob(job: BuildJob, logs: string): void { this.exec(`INSERT INTO build_jobs (id, project_id, status, started_at, finished_at, data_json, logs) VALUES (${q(job.buildId)}, ${q(job.projectId)}, ${q(job.status)}, ${q(job.startedAt)}, ${q(job.finishedAt)}, ${q(JSON.stringify(job))}, ${q(logs)}) ON CONFLICT(id) DO UPDATE SET status=excluded.status, finished_at=excluded.finished_at, data_json=excluded.data_json, logs=excluded.logs;`); }  // line 61: executes this statement as part of this file's behavior

  loadDeploymentTargets(): DeploymentTarget[] { return this.selectJson<DeploymentTarget>("SELECT data_json FROM deployment_targets ORDER BY updated_at DESC", "data_json"); }  // line 63: executes this statement as part of this file's behavior
  saveDeploymentTarget(target: DeploymentTarget): void { this.exec(`INSERT INTO deployment_targets (id, type, updated_at, data_json) VALUES (${q(target.id)}, ${q(target.type)}, ${q(target.updatedAt)}, ${q(JSON.stringify(target))}) ON CONFLICT(id) DO UPDATE SET type=excluded.type, updated_at=excluded.updated_at, data_json=excluded.data_json;`); }  // line 64: executes this statement as part of this file's behavior
  deleteDeploymentTarget(id: string): void { this.exec(`DELETE FROM deployment_targets WHERE id=${q(id)};`); }  // line 65: executes this statement as part of this file's behavior

  loadDeploymentJobs(): Array<{ job: DeploymentJob; logs: string }> { return this.rows<{ data_json: string; logs: string }>("SELECT data_json, logs FROM deployment_jobs ORDER BY created_at DESC").map((row) => ({ job: JSON.parse(row.data_json) as DeploymentJob, logs: row.logs })); }  // line 67: executes this statement as part of this file's behavior
  saveDeploymentJob(job: DeploymentJob, logs: string): void { this.exec(`INSERT INTO deployment_jobs (id, project_id, target_id, build_id, status, created_at, updated_at, data_json, logs) VALUES (${q(job.deploymentId)}, ${q(job.projectId)}, ${q(job.targetId)}, ${q(job.buildId)}, ${q(job.status)}, ${q(job.createdAt)}, ${q(job.updatedAt)}, ${q(JSON.stringify(job))}, ${q(logs)}) ON CONFLICT(id) DO UPDATE SET status=excluded.status, updated_at=excluded.updated_at, data_json=excluded.data_json, logs=excluded.logs;`); }  // line 68: executes this statement as part of this file's behavior

  backup(destinationPath: string): void {  // line 70: executes this statement as part of this file's behavior
    fs.mkdirSync(path.dirname(destinationPath), { recursive: true });  // line 71: executes this statement as part of this file's behavior
    this.exec(`VACUUM INTO ${q(destinationPath)};`);  // line 72: executes this statement as part of this file's behavior
  }  // line 73: executes this statement as part of this file's behavior

  private migrate(): void {  // line 75: executes this statement as part of this file's behavior
    this.exec("CREATE TABLE IF NOT EXISTS schema_migrations (version TEXT PRIMARY KEY, applied_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')));");  // line 76: executes this statement as part of this file's behavior
    const migrationFiles = fs.readdirSync(MIGRATION_DIR).filter((file) => file.endsWith(".sql")).sort();  // line 77: executes this statement as part of this file's behavior
    for (const file of migrationFiles) {  // line 78: executes this statement as part of this file's behavior
      const version = file.replace(/\.sql$/, "");  // line 79: executes this statement as part of this file's behavior
      const alreadyApplied = this.rows<{ version: string }>("SELECT version FROM schema_migrations").some((row) => row.version === version);  // line 80: executes this statement as part of this file's behavior
      if (alreadyApplied) continue;  // line 81: executes this statement as part of this file's behavior
      const sql = fs.readFileSync(path.join(MIGRATION_DIR, file), "utf8");  // line 82: executes this statement as part of this file's behavior
      this.exec(`BEGIN;\n${sql}\nINSERT OR IGNORE INTO schema_migrations (version) VALUES (${q(version)});\nCOMMIT;`);  // line 83: executes this statement as part of this file's behavior
    }  // line 84: executes this statement as part of this file's behavior
  }  // line 85: executes this statement as part of this file's behavior

  private selectJson<T>(sql: string, field: string): T[] { return this.rows<Record<string, string>>(sql).map((row) => JSON.parse(row[field]) as T); }  // line 87: executes this statement as part of this file's behavior
  private rows<T>(sql: string): T[] {  // line 88: executes this statement as part of this file's behavior
    const output = execFileSync("sqlite3", ["-json", this.databasePath, sql], { encoding: "utf8" }).trim();  // line 89: executes this statement as part of this file's behavior
    return output ? JSON.parse(output) as T[] : [];  // line 90: executes this statement as part of this file's behavior
  }  // line 91: executes this statement as part of this file's behavior
  private exec(sql: string): void { execFileSync("sqlite3", [this.databasePath], { input: sql, encoding: "utf8" }); }  // line 92: executes this statement as part of this file's behavior
}  // line 93: executes this statement as part of this file's behavior

function defaultSqliteUrl(): string {  // line 95: executes this statement as part of this file's behavior
  const backendCwd = fs.existsSync(path.resolve(process.cwd(), "src/server.ts"));  // line 96: executes this statement as part of this file's behavior
  return backendCwd ? "sqlite://./data/botblade.sqlite" : "sqlite://./backend/data/botblade.sqlite";  // line 97: executes this statement as part of this file's behavior
}  // line 98: executes this statement as part of this file's behavior

function q(value: string | null): string {  // line 100: executes this statement as part of this file's behavior
  if (value === null) return "NULL";  // line 101: executes this statement as part of this file's behavior
  return `'${value.replace(/'/g, "''")}'`;  // line 102: executes this statement as part of this file's behavior
}  // line 103: executes this statement as part of this file's behavior

function ensureSqliteAvailable(): void { execFileSync("sqlite3", ["--version"], { encoding: "utf8" }); }  // line 105: executes this statement as part of this file's behavior

function deriveKey(secretKey?: string): Buffer {  // line 107: executes this statement as part of this file's behavior
  const allowInsecureDevKey = process.env.BOTBLADE_ALLOW_INSECURE_DEV_KEY === "true";  // line 108: executes this statement as part of this file's behavior
  if (!secretKey) {  // line 109: executes this statement as part of this file's behavior
    if (!allowInsecureDevKey) {  // line 110: executes this statement as part of this file's behavior
      throw new Error("Missing BOTBLADE_SECRET_KEY. Set BOTBLADE_SECRET_KEY to a managed secret value (recommended: `openssl rand -hex 32`). For local/dev test-only fallback, explicitly set BOTBLADE_ALLOW_INSECURE_DEV_KEY=true.");  // line 111: executes this statement as part of this file's behavior
    }  // line 112: executes this statement as part of this file's behavior
    return crypto.createHash("sha256").update("botBlade-local-dev-secret-key").digest();  // line 113: executes this statement as part of this file's behavior
  }  // line 114: executes this statement as part of this file's behavior
  if (/^[a-f0-9]{64}$/i.test(secretKey)) return Buffer.from(secretKey, "hex");  // line 115: executes this statement as part of this file's behavior
  const base64 = Buffer.from(secretKey, "base64");  // line 116: executes this statement as part of this file's behavior
  if (base64.length === 32) return base64;  // line 117: executes this statement as part of this file's behavior
  return crypto.createHash("sha256").update(secretKey).digest();  // line 118: executes this statement as part of this file's behavior
}  // line 119: executes this statement as part of this file's behavior

function encrypt(value: string, key: Buffer): string {  // line 121: executes this statement as part of this file's behavior
  const iv = crypto.randomBytes(12);  // line 122: executes this statement as part of this file's behavior
  const cipher = crypto.createCipheriv("aes-256-gcm", key, iv);  // line 123: executes this statement as part of this file's behavior
  const ciphertext = Buffer.concat([cipher.update(value, "utf8"), cipher.final()]);  // line 124: executes this statement as part of this file's behavior
  return `v1:${iv.toString("base64")}:${cipher.getAuthTag().toString("base64")}:${ciphertext.toString("base64")}`;  // line 125: executes this statement as part of this file's behavior
}  // line 126: executes this statement as part of this file's behavior

function decrypt(payload: string, key: Buffer): string {  // line 128: executes this statement as part of this file's behavior
  const [version, iv, tag, ciphertext] = payload.split(":");  // line 129: executes this statement as part of this file's behavior
  if (version !== "v1" || !iv || !tag || !ciphertext) throw new Error("Unsupported encrypted secret payload.");  // line 130: executes this statement as part of this file's behavior
  const decipher = crypto.createDecipheriv("aes-256-gcm", key, Buffer.from(iv, "base64"));  // line 131: executes this statement as part of this file's behavior
  decipher.setAuthTag(Buffer.from(tag, "base64"));  // line 132: executes this statement as part of this file's behavior
  return Buffer.concat([decipher.update(Buffer.from(ciphertext, "base64")), decipher.final()]).toString("utf8");  // line 133: executes this statement as part of this file's behavior
}  // line 134: executes this statement as part of this file's behavior
