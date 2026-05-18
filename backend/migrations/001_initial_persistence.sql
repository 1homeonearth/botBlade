-- Durable metadata tables for botBlade.
-- Secrets are split so normal query tables contain summaries/fingerprints only; encrypted_value is stored separately.
CREATE TABLE IF NOT EXISTS schema_migrations (
  version TEXT PRIMARY KEY,
  applied_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE IF NOT EXISTS projects (
  id TEXT PRIMARY KEY,
  slug TEXT NOT NULL UNIQUE,
  updated_at TEXT NOT NULL,
  data_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS secret_metadata (
  id TEXT PRIMARY KEY,
  project_id TEXT,
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  storage_mode TEXT NOT NULL,
  fingerprint TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  rotated_at TEXT,
  summary_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS secret_values (
  id TEXT PRIMARY KEY REFERENCES secret_metadata(id) ON DELETE CASCADE,
  encrypted_value TEXT NOT NULL,
  encryption_key_id TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_events (
  id TEXT PRIMARY KEY,
  project_id TEXT,
  action TEXT NOT NULL,
  created_at TEXT NOT NULL,
  data_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS build_jobs (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL,
  status TEXT NOT NULL,
  started_at TEXT NOT NULL,
  finished_at TEXT,
  data_json TEXT NOT NULL,
  logs TEXT NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS deployment_targets (
  id TEXT PRIMARY KEY,
  type TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  data_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS deployment_jobs (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL,
  target_id TEXT NOT NULL,
  build_id TEXT NOT NULL,
  status TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  data_json TEXT NOT NULL,
  logs TEXT NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS runtime_metadata (
  project_id TEXT PRIMARY KEY,
  status_json TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
