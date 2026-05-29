CREATE TABLE IF NOT EXISTS script_profiles (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL,
  source TEXT NOT NULL,
  runtime TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  data_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_script_profiles_project_id ON script_profiles(project_id);
CREATE INDEX IF NOT EXISTS idx_script_profiles_updated_at ON script_profiles(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_script_profiles_source ON script_profiles(source);
CREATE INDEX IF NOT EXISTS idx_script_profiles_runtime ON script_profiles(runtime);
