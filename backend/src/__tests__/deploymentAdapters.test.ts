// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import test from "node:test";  // line 7: executes this statement as part of this file's behavior
import assert from "node:assert/strict";  // line 8: executes this statement as part of this file's behavior
import fs from "node:fs/promises";  // line 9: executes this statement as part of this file's behavior
import path from "node:path";  // line 10: executes this statement as part of this file's behavior
import type { BotProject } from "../models/project.js";  // line 11: executes this statement as part of this file's behavior
import type { BuildJob, CommandRunner } from "../services/buildService.js";  // line 12: executes this statement as part of this file's behavior
import { LocalDockerDeploymentAdapter, capabilitiesForTargetType } from "../services/deploymentAdapters.js";  // line 13: executes this statement as part of this file's behavior
import type { DeploymentTarget } from "../services/deploymentTargets.js";  // line 14: executes this statement as part of this file's behavior
import type { LocalProcessRuntimeService } from "../services/localProcessRuntimeService.js";  // line 15: executes this statement as part of this file's behavior

function projectFixture(): BotProject {  // line 17: executes this statement as part of this file's behavior
  const now = new Date().toISOString();  // line 18: executes this statement as part of this file's behavior
  return {  // line 19: executes this statement as part of this file's behavior
    id: "project_deploy_adapter_test",  // line 20: executes this statement as part of this file's behavior
    name: "Deploy Adapter Test",  // line 21: executes this statement as part of this file's behavior
    slug: "deploy-adapter-test",  // line 22: executes this statement as part of this file's behavior
    description: "",  // line 23: executes this statement as part of this file's behavior
    templateId: "template_blank_discord_ts",  // line 24: executes this statement as part of this file's behavior
    language: "typescript",  // line 25: executes this statement as part of this file's behavior
    runtime: "node22",  // line 26: executes this statement as part of this file's behavior
    discord: { applicationId: null, clientId: null, defaultGuildId: null, tokenSecretRef: "secret_discord", commandRegistration: "guild" },  // line 27: executes this statement as part of this file's behavior
    permissions: { intents: ["Guilds"], botPermissions: [] },  // line 28: executes this statement as part of this file's behavior
    commands: [],  // line 29: executes this statement as part of this file's behavior
    events: [],  // line 30: executes this statement as part of this file's behavior
    deployment: { targetId: null, lastDeploymentId: null },  // line 31: executes this statement as part of this file's behavior
    archivedAt: null,  // line 32: executes this statement as part of this file's behavior
    createdAt: now,  // line 33: executes this statement as part of this file's behavior
    updatedAt: now,  // line 34: executes this statement as part of this file's behavior
  };  // line 35: executes this statement as part of this file's behavior
}  // line 36: executes this statement as part of this file's behavior

function buildFixture(buildId = "build_current"): BuildJob {  // line 38: executes this statement as part of this file's behavior
  return {  // line 39: executes this statement as part of this file's behavior
    buildId,  // line 40: executes this statement as part of this file's behavior
    projectId: "project_deploy_adapter_test",  // line 41: executes this statement as part of this file's behavior
    status: "succeeded",  // line 42: executes this statement as part of this file's behavior
    logUrl: "/logs",  // line 43: executes this statement as part of this file's behavior
    auditEventId: "audit_test",  // line 44: executes this statement as part of this file's behavior
    source: "current_project",  // line 45: executes this statement as part of this file's behavior
    clean: true,  // line 46: executes this statement as part of this file's behavior
    runTests: true,  // line 47: executes this statement as part of this file's behavior
    createDockerImage: false,  // line 48: executes this statement as part of this file's behavior
    artifactPath: `/tmp/${buildId}.tgz`,  // line 49: executes this statement as part of this file's behavior
    startedAt: new Date().toISOString(),  // line 50: executes this statement as part of this file's behavior
    finishedAt: new Date().toISOString(),  // line 51: executes this statement as part of this file's behavior
    errorMessage: null,  // line 52: executes this statement as part of this file's behavior
  };  // line 53: executes this statement as part of this file's behavior
}  // line 54: executes this statement as part of this file's behavior

function targetFixture(): DeploymentTarget {  // line 56: executes this statement as part of this file's behavior
  const now = new Date().toISOString();  // line 57: executes this statement as part of this file's behavior
  return {  // line 58: executes this statement as part of this file's behavior
    id: "target_docker",  // line 59: executes this statement as part of this file's behavior
    name: "Docker Local",  // line 60: executes this statement as part of this file's behavior
    type: "local_docker",  // line 61: executes this statement as part of this file's behavior
    config: { image: "botblade/test-bot" },  // line 62: executes this statement as part of this file's behavior
    secretRefs: ["secret_extra"],  // line 63: executes this statement as part of this file's behavior
    createdAt: now,  // line 64: executes this statement as part of this file's behavior
    updatedAt: now,  // line 65: executes this statement as part of this file's behavior
  };  // line 66: executes this statement as part of this file's behavior
}  // line 67: executes this statement as part of this file's behavior

test("local_docker adapter exposes production actions as supported capabilities", () => {  // line 69: executes this statement as part of this file's behavior
  const capabilities = capabilitiesForTargetType("local_docker");  // line 70: executes this statement as part of this file's behavior
  assert.equal(capabilities.supported, true);  // line 71: executes this statement as part of this file's behavior
  assert.equal(capabilities.actions.deploy, true);  // line 72: executes this statement as part of this file's behavior
  assert.equal(capabilities.actions.status, true);  // line 73: executes this statement as part of this file's behavior
  assert.equal(capabilities.actions.logs, true);  // line 74: executes this statement as part of this file's behavior
  assert.equal(capabilities.actions.restart, true);  // line 75: executes this statement as part of this file's behavior
  assert.equal(capabilities.actions.rollback, true);  // line 76: executes this statement as part of this file's behavior
});  // line 77: executes this statement as part of this file's behavior

test("local_docker deploy builds image, injects secretRefs through env-file, and starts container", async () => {  // line 79: executes this statement as part of this file's behavior
  const project = projectFixture();  // line 80: executes this statement as part of this file's behavior
  const workspace = path.join(process.cwd(), "generated-projects", project.id);  // line 81: executes this statement as part of this file's behavior
  await fs.rm(workspace, { recursive: true, force: true });  // line 82: executes this statement as part of this file's behavior
  await fs.mkdir(workspace, { recursive: true });  // line 83: executes this statement as part of this file's behavior
  await fs.writeFile(path.join(workspace, "Dockerfile"), "FROM node:22-alpine\n", "utf8");  // line 84: executes this statement as part of this file's behavior

  const commands: string[] = [];  // line 86: executes this statement as part of this file's behavior
  const runner: CommandRunner = async (command, args, _cwd) => { commands.push([command, ...args].join(" ")); };  // line 87: executes this statement as part of this file's behavior
  const adapter = new LocalDockerDeploymentAdapter(runner);  // line 88: executes this statement as part of this file's behavior
  const lines = await adapter.deploy({  // line 89: executes this statement as part of this file's behavior
    project,  // line 90: executes this statement as part of this file's behavior
    target: targetFixture(),  // line 91: executes this statement as part of this file's behavior
    build: buildFixture(),  // line 92: executes this statement as part of this file's behavior
    runtime: {} as LocalProcessRuntimeService,  // line 93: executes this statement as part of this file's behavior
    commandRunner: runner,  // line 94: executes this statement as part of this file's behavior
    resolveSecretRef: (id) => id === "secret_discord" ? { id, name: "DISCORD_TOKEN", value: "discord-secret-value" } : { id, name: "EXTRA_TOKEN", value: "extra-secret-value" },  // line 95: executes this statement as part of this file's behavior
  });  // line 96: executes this statement as part of this file's behavior

  assert.ok(commands.some((command) => command === "docker build -t botblade/test-bot:build_current -f Dockerfile ."));  // line 98: executes this statement as part of this file's behavior
  assert.ok(commands.some((command) => command.startsWith("docker run -d --name botblade-deploy-adapter-test --restart unless-stopped --env-file ")));  // line 99: executes this statement as part of this file's behavior
  assert.equal(lines.some((line) => line.includes("Started container botblade-deploy-adapter-test")), true);  // line 100: executes this statement as part of this file's behavior
  assert.equal(commands.join("\n").includes("discord-secret-value"), false);  // line 101: executes this statement as part of this file's behavior
  await fs.rm(workspace, { recursive: true, force: true });  // line 102: executes this statement as part of this file's behavior
});  // line 103: executes this statement as part of this file's behavior

test("local_docker status, logs, restart, and rollback call Docker with deterministic image/container names", async () => {  // line 105: executes this statement as part of this file's behavior
  const project = projectFixture();  // line 106: executes this statement as part of this file's behavior
  const workspace = path.join(process.cwd(), "generated-projects", project.id);  // line 107: executes this statement as part of this file's behavior
  await fs.mkdir(workspace, { recursive: true });  // line 108: executes this statement as part of this file's behavior
  const commands: string[] = [];  // line 109: executes this statement as part of this file's behavior
  const runner: CommandRunner = async (command, args, _cwd, append) => {  // line 110: executes this statement as part of this file's behavior
    commands.push([command, ...args].join(" "));  // line 111: executes this statement as part of this file's behavior
    if (args[0] === "inspect") append("running");  // line 112: executes this statement as part of this file's behavior
    if (args[0] === "logs") append("bot ready");  // line 113: executes this statement as part of this file's behavior
  };  // line 114: executes this statement as part of this file's behavior
  const adapter = new LocalDockerDeploymentAdapter(runner);  // line 115: executes this statement as part of this file's behavior
  const context = {  // line 116: executes this statement as part of this file's behavior
    project,  // line 117: executes this statement as part of this file's behavior
    target: targetFixture(),  // line 118: executes this statement as part of this file's behavior
    build: buildFixture(),  // line 119: executes this statement as part of this file's behavior
    previousBuild: buildFixture("build_previous"),  // line 120: executes this statement as part of this file's behavior
    runtime: {} as LocalProcessRuntimeService,  // line 121: executes this statement as part of this file's behavior
    commandRunner: runner,  // line 122: executes this statement as part of this file's behavior
    resolveSecretRef: (id: string) => ({ id, name: id === "secret_discord" ? "DISCORD_TOKEN" : "EXTRA_TOKEN", value: "secret-value" }),  // line 123: executes this statement as part of this file's behavior
  };  // line 124: executes this statement as part of this file's behavior

  const status = await adapter.status(context);  // line 126: executes this statement as part of this file's behavior
  const logs = await adapter.logs(context);  // line 127: executes this statement as part of this file's behavior
  await adapter.restart(context);  // line 128: executes this statement as part of this file's behavior
  await adapter.rollback(context);  // line 129: executes this statement as part of this file's behavior

  assert.equal(status.status, "running");  // line 131: executes this statement as part of this file's behavior
  assert.equal(logs, "bot ready");  // line 132: executes this statement as part of this file's behavior
  assert.ok(commands.includes("docker restart botblade-deploy-adapter-test"));  // line 133: executes this statement as part of this file's behavior
  assert.ok(commands.some((command) => command.endsWith("botblade/test-bot:build_previous")));  // line 134: executes this statement as part of this file's behavior
  await fs.rm(workspace, { recursive: true, force: true });  // line 135: executes this statement as part of this file's behavior
});  // line 136: executes this statement as part of this file's behavior
