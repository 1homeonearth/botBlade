import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { SqlitePersistence } from "../persistence/sqlitePersistence.js";
import { AuditService } from "../services/auditService.js";
import { ScriptProfileService } from "../services/scriptProfiles/scriptProfileService.js";
import type { BotProfileScriptProfile } from "../models/botProfile.js";

function sqliteAvailable(): boolean {
  try {
    execFileSync("sqlite3", ["--version"], { encoding: "utf8" });
    return true;
  } catch {
    return false;
  }
}

function detectedProfile(overrides: Partial<BotProfileScriptProfile> = {}): BotProfileScriptProfile {
  const now = new Date().toISOString();
  return {
    id: "package-json:package.json:start",
    name: "npm: start",
    description: "Detected package.json script named start.",
    source: "package_json",
    runtime: "node",
    command: ["npm", "run", "start"],
    workingDirectory: ".",
    envRefs: [],
    secretRefs: ["DISCORD_TOKEN"],
    timeoutSeconds: 300,
    requiresConfirmation: false,
    tags: ["package_json", "start", "npm"],
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("script profiles persist across SQLite restarts and record audits", () => {
  if (!sqliteAvailable()) return;
  const tempDir = mkdtempSync(path.join(tmpdir(), "botblade-script-profiles-"));
  const dbPath = path.join(tempDir, "botblade.sqlite");
  const key = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
  const projectId = "project_restart";

  const firstPersistence = new SqlitePersistence(dbPath, key);
  const firstAudit = new AuditService(firstPersistence);
  const firstProfiles = new ScriptProfileService(firstPersistence, firstAudit);
  const [detected] = firstProfiles.upsertDetected(projectId, [detectedProfile()], { actorId: "tester", requestId: "req_detect" });
  const created = firstProfiles.create(projectId, { name: "Manual validate", runtime: "shell", command: ["bash", "scripts/validate.sh"], tags: ["manual"] }, { actorId: "tester", requestId: "req_create" });
  const updated = firstProfiles.update(projectId, created.id, { timeoutSeconds: 900, requiresConfirmation: true }, { actorId: "tester", requestId: "req_update" });

  assert.equal(detected.id, "project_restart:package-json:package.json:start");
  assert.equal(updated?.timeoutSeconds, 900);

  const secondPersistence = new SqlitePersistence(dbPath, key);
  const secondAudit = new AuditService(secondPersistence);
  const secondProfiles = new ScriptProfileService(secondPersistence, secondAudit);
  const restartedProfiles = secondProfiles.list(projectId);

  assert.equal(restartedProfiles.length, 2);
  assert.equal(secondProfiles.get(projectId, detected.id)?.name, "npm: start");
  assert.equal(secondProfiles.get(projectId, created.id)?.timeoutSeconds, 900);
  assert.equal(secondAudit.list(projectId).some((event) => event.action === "script.profile.detect"), true);
  assert.equal(secondAudit.list(projectId).some((event) => event.action === "script.profile.create"), true);
  assert.equal(secondAudit.list(projectId).some((event) => event.action === "script.profile.update"), true);

  assert.equal(secondProfiles.delete(projectId, created.id, { actorId: "tester", requestId: "req_delete" }), true);
  const thirdPersistence = new SqlitePersistence(dbPath, key);
  const thirdAudit = new AuditService(thirdPersistence);
  const thirdProfiles = new ScriptProfileService(thirdPersistence, thirdAudit);

  assert.equal(thirdProfiles.get(projectId, created.id), undefined);
  assert.equal(thirdProfiles.get(projectId, detected.id)?.runtime, "node");
  assert.equal(thirdAudit.list(projectId).some((event) => event.action === "script.profile.delete"), true);
});
