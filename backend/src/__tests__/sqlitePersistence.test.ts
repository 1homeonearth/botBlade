import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { SqlitePersistence } from "../persistence/sqlitePersistence.js";

function sqliteAvailable(): boolean {
  try {
    execFileSync("sqlite3", ["--version"], { encoding: "utf8" });
    return true;
  } catch {
    return false;
  }
}

test("SqlitePersistence diagnostics report migrations and table counts", () => {
  if (!sqliteAvailable()) return;
  const tempDir = mkdtempSync(path.join(tmpdir(), "botblade-sqlite-"));
  const dbPath = path.join(tempDir, "botblade.sqlite");
  const persistence = new SqlitePersistence(dbPath, "test-key");
  const diagnostics = persistence.diagnostics();

  assert.equal(diagnostics.adapter, "sqlite");
  assert.equal(diagnostics.databasePath, dbPath);
  assert.ok(diagnostics.appliedMigrations.includes("001_initial_persistence"));
  assert.ok(diagnostics.appliedMigrations.includes("002_import_records"));
  assert.equal(diagnostics.tableCounts.projects, 0);
  assert.equal(diagnostics.tableCounts.imports, 0);
});
