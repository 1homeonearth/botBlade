import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { SqlitePersistence } from "../persistence/sqlitePersistence.js";
import { AuditService } from "../services/auditService.js";
import { ProjectStore } from "../services/projectStore.js";
import { SecretStore } from "../services/secretStore.js";
import { DeploymentTargetStore } from "../services/deploymentTargets.js";
import type { BuildJob } from "../services/buildService.js";
import type { DeploymentJob } from "../services/deploymentJobs.js";

test("SQLite persistence survives store restart and keeps secret values encrypted", () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "royalscepter-persistence-"));
  const dbPath = path.join(dir, "state.sqlite");
  const key = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
  const secretValue = "super-secret-value-that-must-not-leak";

  const firstPersistence = new SqlitePersistence(dbPath, key);
  const firstProjects = new ProjectStore(firstPersistence);
  const firstSecrets = new SecretStore(firstPersistence);
  const firstAudit = new AuditService(firstPersistence);
  const firstTargets = new DeploymentTargetStore(firstPersistence);

  const project = firstProjects.create({ name: "Durable Project", description: "created before restart" });
  const secret = firstSecrets.create({ projectId: project.id, name: "Discord Token", type: "discord_bot_token", value: secretValue });
  const audit = firstAudit.record({ projectId: project.id, action: "project.create", resourceType: "project", resourceId: project.id, metadata: { secretValue }, requestId: "req_persistence" });
  const target = firstTargets.create({ name: "Local", type: "local_process", config: {}, secretRefs: [secret.id] });
  const build: BuildJob = { buildId: "build_restart", projectId: project.id, status: "succeeded", logUrl: `/api/projects/${project.id}/builds/build_restart/logs`, auditEventId: audit.id, source: "test", clean: true, runTests: false, createDockerImage: false, startedAt: new Date().toISOString(), finishedAt: new Date().toISOString(), errorMessage: null };
  const deployment: DeploymentJob = { deploymentId: "deployment_restart", projectId: project.id, targetId: target.id, buildId: build.buildId, status: "deployed", createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), finishedAt: new Date().toISOString(), errorMessage: null, logUrl: `/api/projects/${project.id}/deployments/deployment_restart/logs`, auditEventId: audit.id };
  firstPersistence.saveBuildJob(build, "build log");
  firstPersistence.saveDeploymentJob(deployment, "deployment log");

  assert.equal(fs.readFileSync(dbPath, "utf8").includes(secretValue), false);

  const secondPersistence = new SqlitePersistence(dbPath, key);
  const secondProjects = new ProjectStore(secondPersistence);
  const secondSecrets = new SecretStore(secondPersistence);
  const secondAudit = new AuditService(secondPersistence);
  const secondTargets = new DeploymentTargetStore(secondPersistence);

  assert.equal(secondProjects.get(project.id)?.description, "created before restart");
  assert.equal(secondSecrets.get(secret.id)?.fingerprint, secret.fingerprint);
  assert.equal(JSON.stringify(secondSecrets.get(secret.id)).includes(secretValue), false);
  assert.equal(JSON.stringify(secondSecrets.list()).includes("value"), false);
  assert.equal(secondSecrets.getValue(secret.id), secretValue);
  assert.equal(secondAudit.list(project.id).some((event) => event.id === audit.id), true);
  assert.equal(secondTargets.get(target.id)?.name, "Local");
  assert.equal(secondPersistence.loadBuildJobs()[0].job.buildId, build.buildId);
  assert.equal(secondPersistence.loadBuildJobs()[0].logs, "build log");
  assert.equal(secondPersistence.loadDeploymentJobs()[0].job.deploymentId, deployment.deploymentId);
  assert.equal(secondPersistence.loadDeploymentJobs()[0].logs, "deployment log");
});
