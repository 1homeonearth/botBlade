-- BEGIN NEWBIE GUIDE HEADER
-- This file is part of BotBlade. The goal of this header is to help brand-new developers
-- understand what this file is responsible for before reading implementation details.
-- Read top-to-bottom, and treat function/class names as a map of the app flow.
-- If you edit behavior here, also check related tests and docs for consistency.
-- END NEWBIE GUIDE HEADER
-- Durable metadata tables for botBlade.
-- Secrets are split so normal query tables contain summaries/fingerprints only; encrypted_value is stored separately.
CREATE TABLE IF NOT EXISTS schema_migrations (  -- line 9: executes this statement as part of this file's behavior
  version TEXT PRIMARY KEY,  -- line 10: executes this statement as part of this file's behavior
  applied_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))  -- line 11: executes this statement as part of this file's behavior
);  -- line 12: executes this statement as part of this file's behavior

CREATE TABLE IF NOT EXISTS projects (  -- line 14: executes this statement as part of this file's behavior
  id TEXT PRIMARY KEY,  -- line 15: executes this statement as part of this file's behavior
  slug TEXT NOT NULL UNIQUE,  -- line 16: executes this statement as part of this file's behavior
  updated_at TEXT NOT NULL,  -- line 17: executes this statement as part of this file's behavior
  data_json TEXT NOT NULL  -- line 18: executes this statement as part of this file's behavior
);  -- line 19: executes this statement as part of this file's behavior

CREATE TABLE IF NOT EXISTS secret_metadata (  -- line 21: executes this statement as part of this file's behavior
  id TEXT PRIMARY KEY,  -- line 22: executes this statement as part of this file's behavior
  project_id TEXT,  -- line 23: executes this statement as part of this file's behavior
  name TEXT NOT NULL,  -- line 24: executes this statement as part of this file's behavior
  type TEXT NOT NULL,  -- line 25: executes this statement as part of this file's behavior
  storage_mode TEXT NOT NULL,  -- line 26: executes this statement as part of this file's behavior
  fingerprint TEXT NOT NULL,  -- line 27: executes this statement as part of this file's behavior
  created_at TEXT NOT NULL,  -- line 28: executes this statement as part of this file's behavior
  updated_at TEXT NOT NULL,  -- line 29: executes this statement as part of this file's behavior
  rotated_at TEXT,  -- line 30: executes this statement as part of this file's behavior
  summary_json TEXT NOT NULL  -- line 31: executes this statement as part of this file's behavior
);  -- line 32: executes this statement as part of this file's behavior

CREATE TABLE IF NOT EXISTS secret_values (  -- line 34: executes this statement as part of this file's behavior
  id TEXT PRIMARY KEY REFERENCES secret_metadata(id) ON DELETE CASCADE,  -- line 35: executes this statement as part of this file's behavior
  encrypted_value TEXT NOT NULL,  -- line 36: executes this statement as part of this file's behavior
  encryption_key_id TEXT NOT NULL,  -- line 37: executes this statement as part of this file's behavior
  updated_at TEXT NOT NULL  -- line 38: executes this statement as part of this file's behavior
);  -- line 39: executes this statement as part of this file's behavior

CREATE TABLE IF NOT EXISTS audit_events (  -- line 41: executes this statement as part of this file's behavior
  id TEXT PRIMARY KEY,  -- line 42: executes this statement as part of this file's behavior
  project_id TEXT,  -- line 43: executes this statement as part of this file's behavior
  action TEXT NOT NULL,  -- line 44: executes this statement as part of this file's behavior
  created_at TEXT NOT NULL,  -- line 45: executes this statement as part of this file's behavior
  data_json TEXT NOT NULL  -- line 46: executes this statement as part of this file's behavior
);  -- line 47: executes this statement as part of this file's behavior

CREATE TABLE IF NOT EXISTS build_jobs (  -- line 49: executes this statement as part of this file's behavior
  id TEXT PRIMARY KEY,  -- line 50: executes this statement as part of this file's behavior
  project_id TEXT NOT NULL,  -- line 51: executes this statement as part of this file's behavior
  status TEXT NOT NULL,  -- line 52: executes this statement as part of this file's behavior
  started_at TEXT NOT NULL,  -- line 53: executes this statement as part of this file's behavior
  finished_at TEXT,  -- line 54: executes this statement as part of this file's behavior
  data_json TEXT NOT NULL,  -- line 55: executes this statement as part of this file's behavior
  logs TEXT NOT NULL DEFAULT ''  -- line 56: executes this statement as part of this file's behavior
);  -- line 57: executes this statement as part of this file's behavior

CREATE TABLE IF NOT EXISTS deployment_targets (  -- line 59: executes this statement as part of this file's behavior
  id TEXT PRIMARY KEY,  -- line 60: executes this statement as part of this file's behavior
  type TEXT NOT NULL,  -- line 61: executes this statement as part of this file's behavior
  updated_at TEXT NOT NULL,  -- line 62: executes this statement as part of this file's behavior
  data_json TEXT NOT NULL  -- line 63: executes this statement as part of this file's behavior
);  -- line 64: executes this statement as part of this file's behavior

CREATE TABLE IF NOT EXISTS deployment_jobs (  -- line 66: executes this statement as part of this file's behavior
  id TEXT PRIMARY KEY,  -- line 67: executes this statement as part of this file's behavior
  project_id TEXT NOT NULL,  -- line 68: executes this statement as part of this file's behavior
  target_id TEXT NOT NULL,  -- line 69: executes this statement as part of this file's behavior
  build_id TEXT NOT NULL,  -- line 70: executes this statement as part of this file's behavior
  status TEXT NOT NULL,  -- line 71: executes this statement as part of this file's behavior
  created_at TEXT NOT NULL,  -- line 72: executes this statement as part of this file's behavior
  updated_at TEXT NOT NULL,  -- line 73: executes this statement as part of this file's behavior
  data_json TEXT NOT NULL,  -- line 74: executes this statement as part of this file's behavior
  logs TEXT NOT NULL DEFAULT ''  -- line 75: executes this statement as part of this file's behavior
);  -- line 76: executes this statement as part of this file's behavior

CREATE TABLE IF NOT EXISTS runtime_metadata (  -- line 78: executes this statement as part of this file's behavior
  project_id TEXT PRIMARY KEY,  -- line 79: executes this statement as part of this file's behavior
  status_json TEXT NOT NULL,  -- line 80: executes this statement as part of this file's behavior
  updated_at TEXT NOT NULL  -- line 81: executes this statement as part of this file's behavior
);  -- line 82: executes this statement as part of this file's behavior
