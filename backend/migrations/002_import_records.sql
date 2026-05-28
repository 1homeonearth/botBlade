CREATE TABLE IF NOT EXISTS imports (
  id TEXT PRIMARY KEY,
  source_type TEXT NOT NULL,
  state TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  data_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_imports_updated_at ON imports(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_imports_state ON imports(state);
CREATE INDEX IF NOT EXISTS idx_imports_source_type ON imports(source_type);
