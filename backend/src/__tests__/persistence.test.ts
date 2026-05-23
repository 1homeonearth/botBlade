// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import test from "node:test";  // line 7: executes this statement as part of this file's behavior
import assert from "node:assert/strict";  // line 8: executes this statement as part of this file's behavior
import fs from "node:fs";  // line 9: executes this statement as part of this file's behavior
import os from "node:os";  // line 10: executes this statement as part of this file's behavior
import path from "node:path";  // line 11: executes this statement as part of this file's behavior
import { SqlitePersistence } from "../persistence/sqlitePersistence.js";  // line 12: executes this statement as part of this file's behavior
import { AuditService } from "../services/auditService.js";  // line 13: executes this statement as part of this file's behavior
import { ProjectStore } from "../services/projectStore.js";  // line 14: executes this statement as part of this file's behavior
import { SecretStore } from "../services/secretStore.js";  // line 15: executes this statement as part of this file's behavior
import { DeploymentTargetStore } from "../services/deploymentTargets.js";  // line 16: executes this statement as part of this file's behavior
import type { BuildJob } from "../services/buildService.js";  // line 17: executes this statement as part of this file's behavior
import type { DeploymentJob } from "../services/deploymentJobs.js";  // line 18: executes this statement as part of this file's behavior

test("SQLite persistence survives store restart and keeps secret values encrypted", () => {  // line 20: executes this statement as part of this file's behavior
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "botblade-persistence-"));  // line 21: executes this statement as part of this file's behavior
  const dbPath = path.join(dir, "state.sqlite");  // line 22: executes this statement as part of this file's behavior
  const key = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";  // line 23: executes this statement as part of this file's behavior
  const secretValue = "super-secret-value-that-must-not-leak";  // line 24: executes this statement as part of this file's behavior

  const firstPersistence = new SqlitePersistence(dbPath, key);  // line 26: executes this statement as part of this file's behavior
  const firstProjects = new ProjectStore(firstPersistence);  // line 27: executes this statement as part of this file's behavior
  const firstSecrets = new SecretStore(firstPersistence);  // line 28: executes this statement as part of this file's behavior
  const firstAudit = new AuditService(firstPersistence);  // line 29: executes this statement as part of this file's behavior
  const firstTargets = new DeploymentTargetStore(firstPersistence);  // line 30: executes this statement as part of this file's behavior

  const project = firstProjects.create({ name: "Durable Project", description: "created before restart" });  // line 32: executes this statement as part of this file's behavior
  const secret = firstSecrets.create({ projectId: project.id, name: "Discord Token", type: "discord_bot_token", value: secretValue });  // line 33: executes this statement as part of this file's behavior
  const audit = firstAudit.record({ projectId: project.id, action: "project.create", resourceType: "project", resourceId: project.id, metadata: { secretValue }, requestId: "req_persistence" });  // line 34: executes this statement as part of this file's behavior
  const target = firstTargets.create({ name: "Local", type: "local_process", config: {}, secretRefs: [secret.id] });  // line 35: executes this statement as part of this file's behavior
  const build: BuildJob = { buildId: "build_restart", projectId: project.id, status: "succeeded", logUrl: `/api/projects/${project.id}/builds/build_restart/logs`, auditEventId: audit.id, source: "test", clean: true, runTests: false, createDockerImage: false, artifactPath: null, startedAt: new Date().toISOString(), finishedAt: new Date().toISOString(), errorMessage: null };  // line 36: executes this statement as part of this file's behavior
  const deployment: DeploymentJob = { deploymentId: "deployment_restart", projectId: project.id, targetId: target.id, buildId: build.buildId, status: "deployed", createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), finishedAt: new Date().toISOString(), errorMessage: null, logUrl: `/api/projects/${project.id}/deployments/deployment_restart/logs`, auditEventId: audit.id };  // line 37: executes this statement as part of this file's behavior
  firstPersistence.saveBuildJob(build, "build log");  // line 38: executes this statement as part of this file's behavior
  firstPersistence.saveDeploymentJob(deployment, "deployment log");  // line 39: executes this statement as part of this file's behavior

  assert.equal(fs.readFileSync(dbPath, "utf8").includes(secretValue), false);  // line 41: executes this statement as part of this file's behavior

  const secondPersistence = new SqlitePersistence(dbPath, key);  // line 43: executes this statement as part of this file's behavior
  const secondProjects = new ProjectStore(secondPersistence);  // line 44: executes this statement as part of this file's behavior
  const secondSecrets = new SecretStore(secondPersistence);  // line 45: executes this statement as part of this file's behavior
  const secondAudit = new AuditService(secondPersistence);  // line 46: executes this statement as part of this file's behavior
  const secondTargets = new DeploymentTargetStore(secondPersistence);  // line 47: executes this statement as part of this file's behavior

  assert.equal(secondProjects.get(project.id)?.description, "created before restart");  // line 49: executes this statement as part of this file's behavior
  assert.equal(secondSecrets.get(secret.id)?.fingerprint, secret.fingerprint);  // line 50: executes this statement as part of this file's behavior
  assert.equal(JSON.stringify(secondSecrets.get(secret.id)).includes(secretValue), false);  // line 51: executes this statement as part of this file's behavior
  assert.equal(JSON.stringify(secondSecrets.list()).includes("value"), false);  // line 52: executes this statement as part of this file's behavior
  assert.equal(secondSecrets.getValue(secret.id), secretValue);  // line 53: executes this statement as part of this file's behavior
  assert.equal(secondAudit.list(project.id).some((event) => event.id === audit.id), true);  // line 54: executes this statement as part of this file's behavior
  assert.equal(secondTargets.get(target.id)?.name, "Local");  // line 55: executes this statement as part of this file's behavior
  assert.equal(secondPersistence.loadBuildJobs()[0].job.buildId, build.buildId);  // line 56: executes this statement as part of this file's behavior
  assert.equal(secondPersistence.loadBuildJobs()[0].logs, "build log");  // line 57: executes this statement as part of this file's behavior
  assert.equal(secondPersistence.loadDeploymentJobs()[0].job.deploymentId, deployment.deploymentId);  // line 58: executes this statement as part of this file's behavior
  assert.equal(secondPersistence.loadDeploymentJobs()[0].logs, "deployment log");  // line 59: executes this statement as part of this file's behavior
});  // line 60: executes this statement as part of this file's behavior

test("SQLite persistence fails closed when BOTBLADE_SECRET_KEY is missing", () => {  // line 62: executes this statement as part of this file's behavior
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "botblade-persistence-missing-key-"));  // line 63: executes this statement as part of this file's behavior
  const dbPath = path.join(dir, "state.sqlite");  // line 64: executes this statement as part of this file's behavior
  const originalSecret = process.env.BOTBLADE_SECRET_KEY;  // line 65: executes this statement as part of this file's behavior
  const originalOverride = process.env.BOTBLADE_ALLOW_INSECURE_DEV_KEY;  // line 66: executes this statement as part of this file's behavior
  delete process.env.BOTBLADE_SECRET_KEY;  // line 67: executes this statement as part of this file's behavior
  delete process.env.BOTBLADE_ALLOW_INSECURE_DEV_KEY;  // line 68: executes this statement as part of this file's behavior
  try {  // line 69: executes this statement as part of this file's behavior
    let capturedError: unknown;  // line 70: executes this statement as part of this file's behavior
    try {  // line 71: executes this statement as part of this file's behavior
      new SqlitePersistence(dbPath);  // line 72: executes this statement as part of this file's behavior
    } catch (error) {  // line 73: executes this statement as part of this file's behavior
      capturedError = error;  // line 74: executes this statement as part of this file's behavior
    }  // line 75: executes this statement as part of this file's behavior
    assert.ok(capturedError instanceof Error);  // line 76: executes this statement as part of this file's behavior
    const errorMessage = capturedError instanceof Error ? capturedError.message : "";  // line 77: executes this statement as part of this file's behavior
    assert.ok(/Missing BOTBLADE_SECRET_KEY[\s\S]*BOTBLADE_ALLOW_INSECURE_DEV_KEY=true/.test(errorMessage));  // line 78: executes this statement as part of this file's behavior
  } finally {  // line 79: executes this statement as part of this file's behavior
    if (originalSecret === undefined) delete process.env.BOTBLADE_SECRET_KEY;  // line 80: executes this statement as part of this file's behavior
    else process.env.BOTBLADE_SECRET_KEY = originalSecret;  // line 81: executes this statement as part of this file's behavior
    if (originalOverride === undefined) delete process.env.BOTBLADE_ALLOW_INSECURE_DEV_KEY;  // line 82: executes this statement as part of this file's behavior
    else process.env.BOTBLADE_ALLOW_INSECURE_DEV_KEY = originalOverride;  // line 83: executes this statement as part of this file's behavior
  }  // line 84: executes this statement as part of this file's behavior
});  // line 85: executes this statement as part of this file's behavior

test("SQLite persistence allows explicit insecure dev key override when BOTBLADE_SECRET_KEY is missing", () => {  // line 87: executes this statement as part of this file's behavior
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "botblade-persistence-dev-override-"));  // line 88: executes this statement as part of this file's behavior
  const dbPath = path.join(dir, "state.sqlite");  // line 89: executes this statement as part of this file's behavior
  const originalSecret = process.env.BOTBLADE_SECRET_KEY;  // line 90: executes this statement as part of this file's behavior
  const originalOverride = process.env.BOTBLADE_ALLOW_INSECURE_DEV_KEY;  // line 91: executes this statement as part of this file's behavior
  delete process.env.BOTBLADE_SECRET_KEY;  // line 92: executes this statement as part of this file's behavior
  process.env.BOTBLADE_ALLOW_INSECURE_DEV_KEY = "true";  // line 93: executes this statement as part of this file's behavior
  try {  // line 94: executes this statement as part of this file's behavior
    const persistence = new SqlitePersistence(dbPath);  // line 95: executes this statement as part of this file's behavior
    const projects = new ProjectStore(persistence);  // line 96: executes this statement as part of this file's behavior
    const project = projects.create({ name: "Override Project", description: "dev override" });  // line 97: executes this statement as part of this file's behavior
    assert.equal(project.name, "Override Project");  // line 98: executes this statement as part of this file's behavior
  } finally {  // line 99: executes this statement as part of this file's behavior
    if (originalSecret === undefined) delete process.env.BOTBLADE_SECRET_KEY;  // line 100: executes this statement as part of this file's behavior
    else process.env.BOTBLADE_SECRET_KEY = originalSecret;  // line 101: executes this statement as part of this file's behavior
    if (originalOverride === undefined) delete process.env.BOTBLADE_ALLOW_INSECURE_DEV_KEY;  // line 102: executes this statement as part of this file's behavior
    else process.env.BOTBLADE_ALLOW_INSECURE_DEV_KEY = originalOverride;  // line 103: executes this statement as part of this file's behavior
  }  // line 104: executes this statement as part of this file's behavior
});  // line 105: executes this statement as part of this file's behavior
