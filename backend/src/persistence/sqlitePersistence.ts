import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import type { AuditEvent } from "../services/auditService.js";
import type { BuildJob } from "../services/buildService.js";
import type { DeploymentJob } from "../services/deploymentJobs.js";
import type { DeploymentTarget } from "../services/deploymentTargets.js";
import type { BotProject } from "../models/project.js";
import type { AuditServicePersistence, BuildServicePersistence, DeploymentJobStorePersistence, DeploymentTargetStorePersistence, ProjectStorePersistence, SecretRecord, SecretStorePersistence } from "../services/persistence.js";

const MIGRATION_DIR = fs.existsSync(path.resolve(process.cwd(), "migrations")) ? path.resolve(process.cwd(), "migrations") : path.resolve(process.cwd(), "backend/migrations");
const DEFAULT_KEY_ID = "local-env";

export class SqlitePersistence implements ProjectStorePersistence, SecretStorePersistence, AuditServicePersistence, BuildServicePersistence, DeploymentTargetStorePersistence, DeploymentJobStorePersistence {
  private readonly key: Buffer;

  constructor(private readonly databasePath: string, secretKey = process.env.BOTBLADE_SECRET_KEY) {
    ensureSqliteAvailable();
    fs.mkdirSync(path.dirname(databasePath), { recursive: true });
    this.key = deriveKey(secretKey);
    this.migrate();
  }

  static fromUrl(url = process.env.BOTBLADE_DATABASE_URL ?? process.env.DATABASE_URL ?? defaultSqliteUrl()): SqlitePersistence | undefined {
    if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
      console.warn("Postgres persistence is configured but this dependency-free build only ships the SQLite adapter; use the documented DATABASE_URL for production Postgres deployments.");
      return undefined;
    }
    if (!url.startsWith("sqlite://")) return undefined;
    return new SqlitePersistence(path.resolve(url.slice("sqlite://".length)));
  }

  loadProjects(): BotProject[] { return this.selectJson<BotProject>("SELECT data_json FROM projects ORDER BY updated_at DESC", "data_json"); }
  saveProject(project: BotProject): void { this.exec(`INSERT INTO projects (id, slug, updated_at, data_json) VALUES (${q(project.id)}, ${q(project.slug)}, ${q(project.updatedAt)}, ${q(JSON.stringify(project))}) ON CONFLICT(id) DO UPDATE SET slug=excluded.slug, updated_at=excluded.updated_at, data_json=excluded.data_json;`); }
  deleteProject(projectId: string): void { this.exec(`DELETE FROM projects WHERE id=${q(projectId)};`); }

  loadSecrets(): SecretRecord[] {
    return this.rows<{ summary_json: string; encrypted_value: string }>("SELECT m.summary_json, v.encrypted_value FROM secret_metadata m JOIN secret_values v ON v.id = m.id ORDER BY m.updated_at DESC").map((row) => ({ ...JSON.parse(row.summary_json) as Omit<SecretRecord, "value">, value: decrypt(row.encrypted_value, this.key) }));
  }
  saveSecret(secret: SecretRecord): void {
    const { value: _value, ...summary } = secret;
    const now = new Date().toISOString();
    this.exec(`BEGIN;
INSERT INTO secret_metadata (id, project_id, name, type, storage_mode, fingerprint, created_at, updated_at, rotated_at, summary_json) VALUES (${q(secret.id)}, ${q(secret.projectId)}, ${q(secret.name)}, ${q(secret.type)}, ${q(secret.storageMode)}, ${q(secret.fingerprint)}, ${q(secret.createdAt)}, ${q(secret.updatedAt)}, ${q(secret.rotatedAt)}, ${q(JSON.stringify(summary))}) ON CONFLICT(id) DO UPDATE SET project_id=excluded.project_id, name=excluded.name, type=excluded.type, storage_mode=excluded.storage_mode, fingerprint=excluded.fingerprint, updated_at=excluded.updated_at, rotated_at=excluded.rotated_at, summary_json=excluded.summary_json;
INSERT INTO secret_values (id, encrypted_value, encryption_key_id, updated_at) VALUES (${q(secret.id)}, ${q(encrypt(secret.value, this.key))}, ${q(DEFAULT_KEY_ID)}, ${q(now)}) ON CONFLICT(id) DO UPDATE SET encrypted_value=excluded.encrypted_value, encryption_key_id=excluded.encryption_key_id, updated_at=excluded.updated_at;
COMMIT;`);
  }
  deleteSecret(secretId: string): void { this.exec(`DELETE FROM secret_values WHERE id=${q(secretId)}; DELETE FROM secret_metadata WHERE id=${q(secretId)};`); }

  loadAuditEvents(): AuditEvent[] { return this.selectJson<AuditEvent>("SELECT data_json FROM audit_events ORDER BY created_at DESC", "data_json"); }
  saveAuditEvent(event: AuditEvent): void { this.exec(`INSERT INTO audit_events (id, project_id, action, created_at, data_json) VALUES (${q(event.id)}, ${q(event.projectId)}, ${q(event.action)}, ${q(event.createdAt)}, ${q(JSON.stringify(event))}) ON CONFLICT(id) DO UPDATE SET project_id=excluded.project_id, action=excluded.action, created_at=excluded.created_at, data_json=excluded.data_json;`); }

  loadBuildJobs(): Array<{ job: BuildJob; logs: string }> { return this.rows<{ data_json: string; logs: string }>("SELECT data_json, logs FROM build_jobs ORDER BY started_at DESC").map((row) => ({ job: JSON.parse(row.data_json) as BuildJob, logs: row.logs })); }
  saveBuildJob(job: BuildJob, logs: string): void { this.exec(`INSERT INTO build_jobs (id, project_id, status, started_at, finished_at, data_json, logs) VALUES (${q(job.buildId)}, ${q(job.projectId)}, ${q(job.status)}, ${q(job.startedAt)}, ${q(job.finishedAt)}, ${q(JSON.stringify(job))}, ${q(logs)}) ON CONFLICT(id) DO UPDATE SET status=excluded.status, finished_at=excluded.finished_at, data_json=excluded.data_json, logs=excluded.logs;`); }

  loadDeploymentTargets(): DeploymentTarget[] { return this.selectJson<DeploymentTarget>("SELECT data_json FROM deployment_targets ORDER BY updated_at DESC", "data_json"); }
  saveDeploymentTarget(target: DeploymentTarget): void { this.exec(`INSERT INTO deployment_targets (id, type, updated_at, data_json) VALUES (${q(target.id)}, ${q(target.type)}, ${q(target.updatedAt)}, ${q(JSON.stringify(target))}) ON CONFLICT(id) DO UPDATE SET type=excluded.type, updated_at=excluded.updated_at, data_json=excluded.data_json;`); }
  deleteDeploymentTarget(id: string): void { this.exec(`DELETE FROM deployment_targets WHERE id=${q(id)};`); }

  loadDeploymentJobs(): Array<{ job: DeploymentJob; logs: string }> { return this.rows<{ data_json: string; logs: string }>("SELECT data_json, logs FROM deployment_jobs ORDER BY created_at DESC").map((row) => ({ job: JSON.parse(row.data_json) as DeploymentJob, logs: row.logs })); }
  saveDeploymentJob(job: DeploymentJob, logs: string): void { this.exec(`INSERT INTO deployment_jobs (id, project_id, target_id, build_id, status, created_at, updated_at, data_json, logs) VALUES (${q(job.deploymentId)}, ${q(job.projectId)}, ${q(job.targetId)}, ${q(job.buildId)}, ${q(job.status)}, ${q(job.createdAt)}, ${q(job.updatedAt)}, ${q(JSON.stringify(job))}, ${q(logs)}) ON CONFLICT(id) DO UPDATE SET status=excluded.status, updated_at=excluded.updated_at, data_json=excluded.data_json, logs=excluded.logs;`); }

  backup(destinationPath: string): void {
    fs.mkdirSync(path.dirname(destinationPath), { recursive: true });
    this.exec(`VACUUM INTO ${q(destinationPath)};`);
  }

  private migrate(): void {
    this.exec("CREATE TABLE IF NOT EXISTS schema_migrations (version TEXT PRIMARY KEY, applied_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')));");
    const migrationFiles = fs.readdirSync(MIGRATION_DIR).filter((file) => file.endsWith(".sql")).sort();
    for (const file of migrationFiles) {
      const version = file.replace(/\.sql$/, "");
      const alreadyApplied = this.rows<{ version: string }>("SELECT version FROM schema_migrations").some((row) => row.version === version);
      if (alreadyApplied) continue;
      const sql = fs.readFileSync(path.join(MIGRATION_DIR, file), "utf8");
      this.exec(`BEGIN;\n${sql}\nINSERT OR IGNORE INTO schema_migrations (version) VALUES (${q(version)});\nCOMMIT;`);
    }
  }

  private selectJson<T>(sql: string, field: string): T[] { return this.rows<Record<string, string>>(sql).map((row) => JSON.parse(row[field]) as T); }
  private rows<T>(sql: string): T[] {
    const output = execFileSync("sqlite3", ["-json", this.databasePath, sql], { encoding: "utf8" }).trim();
    return output ? JSON.parse(output) as T[] : [];
  }
  private exec(sql: string): void { execFileSync("sqlite3", [this.databasePath], { input: sql, encoding: "utf8" }); }
}

function defaultSqliteUrl(): string {
  const backendCwd = fs.existsSync(path.resolve(process.cwd(), "src/server.ts"));
  return backendCwd ? "sqlite://./data/botblade.sqlite" : "sqlite://./backend/data/botblade.sqlite";
}

function q(value: string | null): string {
  if (value === null) return "NULL";
  return `'${value.replace(/'/g, "''")}'`;
}

function ensureSqliteAvailable(): void { execFileSync("sqlite3", ["--version"], { encoding: "utf8" }); }

function deriveKey(secretKey?: string): Buffer {
  const allowInsecureDevKey = process.env.BOTBLADE_ALLOW_INSECURE_DEV_KEY === "true";
  if (!secretKey) {
    if (!allowInsecureDevKey) {
      throw new Error("Missing BOTBLADE_SECRET_KEY. Set BOTBLADE_SECRET_KEY to a managed secret value (recommended: `openssl rand -hex 32`). For local/dev test-only fallback, explicitly set BOTBLADE_ALLOW_INSECURE_DEV_KEY=true.");
    }
    return crypto.createHash("sha256").update("botBlade-local-dev-secret-key").digest();
  }
  if (/^[a-f0-9]{64}$/i.test(secretKey)) return Buffer.from(secretKey, "hex");
  const base64 = Buffer.from(secretKey, "base64");
  if (base64.length === 32) return base64;
  return crypto.createHash("sha256").update(secretKey).digest();
}

function encrypt(value: string, key: Buffer): string {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv("aes-256-gcm", key, iv);
  const ciphertext = Buffer.concat([cipher.update(value, "utf8"), cipher.final()]);
  return `v1:${iv.toString("base64")}:${cipher.getAuthTag().toString("base64")}:${ciphertext.toString("base64")}`;
}

function decrypt(payload: string, key: Buffer): string {
  const [version, iv, tag, ciphertext] = payload.split(":");
  if (version !== "v1" || !iv || !tag || !ciphertext) throw new Error("Unsupported encrypted secret payload.");
  const decipher = crypto.createDecipheriv("aes-256-gcm", key, Buffer.from(iv, "base64"));
  decipher.setAuthTag(Buffer.from(tag, "base64"));
  return Buffer.concat([decipher.update(Buffer.from(ciphertext, "base64")), decipher.final()]).toString("utf8");
}
