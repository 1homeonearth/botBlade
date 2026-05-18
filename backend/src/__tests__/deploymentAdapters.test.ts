import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs/promises";
import path from "node:path";
import type { BotProject } from "../models/project.js";
import type { BuildJob, CommandRunner } from "../services/buildService.js";
import { LocalDockerDeploymentAdapter, capabilitiesForTargetType } from "../services/deploymentAdapters.js";
import type { DeploymentTarget } from "../services/deploymentTargets.js";
import type { LocalProcessRuntimeService } from "../services/localProcessRuntimeService.js";

function projectFixture(): BotProject {
  const now = new Date().toISOString();
  return {
    id: "project_deploy_adapter_test",
    name: "Deploy Adapter Test",
    slug: "deploy-adapter-test",
    description: "",
    templateId: "template_blank_discord_ts",
    language: "typescript",
    runtime: "node22",
    discord: { applicationId: null, clientId: null, defaultGuildId: null, tokenSecretRef: "secret_discord", commandRegistration: "guild" },
    permissions: { intents: ["Guilds"], botPermissions: [] },
    commands: [],
    events: [],
    deployment: { targetId: null, lastDeploymentId: null },
    archivedAt: null,
    createdAt: now,
    updatedAt: now,
  };
}

function buildFixture(buildId = "build_current"): BuildJob {
  return {
    buildId,
    projectId: "project_deploy_adapter_test",
    status: "succeeded",
    logUrl: "/logs",
    auditEventId: "audit_test",
    source: "current_project",
    clean: true,
    runTests: true,
    createDockerImage: false,
    artifactPath: `/tmp/${buildId}.tgz`,
    startedAt: new Date().toISOString(),
    finishedAt: new Date().toISOString(),
    errorMessage: null,
  };
}

function targetFixture(): DeploymentTarget {
  const now = new Date().toISOString();
  return {
    id: "target_docker",
    name: "Docker Local",
    type: "local_docker",
    config: { image: "royalscepter/test-bot" },
    secretRefs: ["secret_extra"],
    createdAt: now,
    updatedAt: now,
  };
}

test("local_docker adapter exposes production actions as supported capabilities", () => {
  const capabilities = capabilitiesForTargetType("local_docker");
  assert.equal(capabilities.supported, true);
  assert.equal(capabilities.actions.deploy, true);
  assert.equal(capabilities.actions.status, true);
  assert.equal(capabilities.actions.logs, true);
  assert.equal(capabilities.actions.restart, true);
  assert.equal(capabilities.actions.rollback, true);
});

test("local_docker deploy builds image, injects secretRefs through env-file, and starts container", async () => {
  const project = projectFixture();
  const workspace = path.join(process.cwd(), "generated-projects", project.id);
  await fs.rm(workspace, { recursive: true, force: true });
  await fs.mkdir(workspace, { recursive: true });
  await fs.writeFile(path.join(workspace, "Dockerfile"), "FROM node:22-alpine\n", "utf8");

  const commands: string[] = [];
  const runner: CommandRunner = async (command, args, _cwd) => { commands.push([command, ...args].join(" ")); };
  const adapter = new LocalDockerDeploymentAdapter(runner);
  const lines = await adapter.deploy({
    project,
    target: targetFixture(),
    build: buildFixture(),
    runtime: {} as LocalProcessRuntimeService,
    commandRunner: runner,
    resolveSecretRef: (id) => id === "secret_discord" ? { id, name: "DISCORD_TOKEN", value: "discord-secret-value" } : { id, name: "EXTRA_TOKEN", value: "extra-secret-value" },
  });

  assert.ok(commands.some((command) => command === "docker build -t royalscepter/test-bot:build_current -f Dockerfile ."));
  assert.ok(commands.some((command) => command.startsWith("docker run -d --name royalscepter-deploy-adapter-test --restart unless-stopped --env-file ")));
  assert.equal(lines.some((line) => line.includes("Started container royalscepter-deploy-adapter-test")), true);
  assert.equal(commands.join("\n").includes("discord-secret-value"), false);
  await fs.rm(workspace, { recursive: true, force: true });
});

test("local_docker status, logs, restart, and rollback call Docker with deterministic image/container names", async () => {
  const project = projectFixture();
  const workspace = path.join(process.cwd(), "generated-projects", project.id);
  await fs.mkdir(workspace, { recursive: true });
  const commands: string[] = [];
  const runner: CommandRunner = async (command, args, _cwd, append) => {
    commands.push([command, ...args].join(" "));
    if (args[0] === "inspect") append("running");
    if (args[0] === "logs") append("bot ready");
  };
  const adapter = new LocalDockerDeploymentAdapter(runner);
  const context = {
    project,
    target: targetFixture(),
    build: buildFixture(),
    previousBuild: buildFixture("build_previous"),
    runtime: {} as LocalProcessRuntimeService,
    commandRunner: runner,
    resolveSecretRef: (id: string) => ({ id, name: id === "secret_discord" ? "DISCORD_TOKEN" : "EXTRA_TOKEN", value: "secret-value" }),
  };

  const status = await adapter.status(context);
  const logs = await adapter.logs(context);
  await adapter.restart(context);
  await adapter.rollback(context);

  assert.equal(status.status, "running");
  assert.equal(logs, "bot ready");
  assert.ok(commands.includes("docker restart royalscepter-deploy-adapter-test"));
  assert.ok(commands.some((command) => command.endsWith("royalscepter/test-bot:build_previous")));
  await fs.rm(workspace, { recursive: true, force: true });
});
