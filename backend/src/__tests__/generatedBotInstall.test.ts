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
import { BuildService, type CommandRunner } from "../services/buildService.js";  // line 12: executes this statement as part of this file's behavior
import { ProjectFileService, templateFiles } from "../services/projectFiles.js";  // line 13: executes this statement as part of this file's behavior

function projectFixture(id = "project_generated_bot_install_test"): BotProject {  // line 15: executes this statement as part of this file's behavior
  const now = new Date().toISOString();  // line 16: executes this statement as part of this file's behavior
  return {  // line 17: executes this statement as part of this file's behavior
    id,  // line 18: executes this statement as part of this file's behavior
    name: "Generated Bot Install Test",  // line 19: executes this statement as part of this file's behavior
    slug: "generated-bot-install-test",  // line 20: executes this statement as part of this file's behavior
    description: "",  // line 21: executes this statement as part of this file's behavior
    templateId: "template_blank_discord_ts",  // line 22: executes this statement as part of this file's behavior
    language: "typescript",  // line 23: executes this statement as part of this file's behavior
    runtime: "node22",  // line 24: executes this statement as part of this file's behavior
    discord: {  // line 25: executes this statement as part of this file's behavior
      applicationId: null,  // line 26: executes this statement as part of this file's behavior
      clientId: null,  // line 27: executes this statement as part of this file's behavior
      defaultGuildId: null,  // line 28: executes this statement as part of this file's behavior
      tokenSecretRef: "secret_discord_token",  // line 29: executes this statement as part of this file's behavior
      commandRegistration: "guild",  // line 30: executes this statement as part of this file's behavior
    },  // line 31: executes this statement as part of this file's behavior
    permissions: { intents: ["Guilds"], botPermissions: [] },  // line 32: executes this statement as part of this file's behavior
    commands: [{  // line 33: executes this statement as part of this file's behavior
      id: "cmd_ping",  // line 34: executes this statement as part of this file's behavior
      name: "ping",  // line 35: executes this statement as part of this file's behavior
      description: "Replies with pong.",  // line 36: executes this statement as part of this file's behavior
      type: "chat_input",  // line 37: executes this statement as part of this file's behavior
      options: [],  // line 38: executes this statement as part of this file's behavior
      permissions: { defaultMemberPermissions: null, dmPermission: false },  // line 39: executes this statement as part of this file's behavior
      handler: { kind: "static_response", ephemeral: true, content: "Pong." },  // line 40: executes this statement as part of this file's behavior
    }],  // line 41: executes this statement as part of this file's behavior
    events: [],  // line 42: executes this statement as part of this file's behavior
    deployment: { targetId: null, lastDeploymentId: null },  // line 43: executes this statement as part of this file's behavior
    archivedAt: null,  // line 44: executes this statement as part of this file's behavior
    createdAt: now,  // line 45: executes this statement as part of this file's behavior
    updatedAt: now,  // line 46: executes this statement as part of this file's behavior
  };  // line 47: executes this statement as part of this file's behavior
}  // line 48: executes this statement as part of this file's behavior

test("generated package files pin discord.js and include a reproducible lockfile", () => {  // line 50: executes this statement as part of this file's behavior
  const files = templateFiles(projectFixture());  // line 51: executes this statement as part of this file's behavior
  const packageJson = JSON.parse(files["package.json"]) as { dependencies: Record<string, string>; scripts: Record<string, string> };  // line 52: executes this statement as part of this file's behavior
  const lockfile = JSON.parse(files["package-lock.json"]) as { packages: Record<string, { version?: string; dependencies?: Record<string, string> }> };  // line 53: executes this statement as part of this file's behavior

  assert.equal(packageJson.dependencies["discord.js"], "14.15.3");  // line 55: executes this statement as part of this file's behavior
  assert.equal(lockfile.packages[""].dependencies?.["discord.js"], "14.15.3");  // line 56: executes this statement as part of this file's behavior
  assert.equal(lockfile.packages["node_modules/discord.js"].version, "14.15.3");  // line 57: executes this statement as part of this file's behavior
  assert.equal(files["README.md"].includes("NPM_CONFIG_REGISTRY=https://npm.example.corp/repository/npm/ npm ci"), true);  // line 58: executes this statement as part of this file's behavior
  assert.equal(packageJson.scripts["register:commands"], "node dist/register-commands.js");  // line 59: executes this statement as part of this file's behavior
  assert.equal(files[".env.example"].includes("DISCORD_APPLICATION_ID"), true);  // line 60: executes this statement as part of this file's behavior
  assert.equal(files[".env.example"].includes("DISCORD_GUILD_ID"), true);  // line 61: executes this statement as part of this file's behavior
  assert.equal(files["src/register-commands.ts"].includes("Routes.applicationGuildCommands"), true);  // line 62: executes this statement as part of this file's behavior
  assert.equal(files["src/commands/load.test.ts"].includes("serializable for registration"), true);  // line 63: executes this statement as part of this file's behavior
});  // line 64: executes this statement as part of this file's behavior

test("build install step uses npm ci when the generated lockfile is present", async () => {  // line 66: executes this statement as part of this file's behavior
  const project = projectFixture("project_generated_bot_install_ci_test");  // line 67: executes this statement as part of this file's behavior
  const files = new ProjectFileService();  // line 68: executes this statement as part of this file's behavior
  const workspace = files.workspace(project.id);  // line 69: executes this statement as part of this file's behavior
  await fs.rm(workspace, { recursive: true, force: true });  // line 70: executes this statement as part of this file's behavior
  await files.generate(project, true);  // line 71: executes this statement as part of this file's behavior

  const commands: string[] = [];  // line 73: executes this statement as part of this file's behavior
  const runner: CommandRunner = async (command, args) => {  // line 74: executes this statement as part of this file's behavior
    commands.push([command, ...args].join(" "));  // line 75: executes this statement as part of this file's behavior
  };  // line 76: executes this statement as part of this file's behavior
  const service = new BuildService(files, (secretId) => secretId === "secret_discord_token", undefined, runner);  // line 77: executes this statement as part of this file's behavior

  const job = await service.create(project, { clean: false, runTests: true }, "audit_test", "req_test");  // line 79: executes this statement as part of this file's behavior

  assert.equal(job.status, "succeeded");  // line 81: executes this statement as part of this file's behavior
  assert.equal(commands[0], "npm ci");  // line 82: executes this statement as part of this file's behavior
  assert.equal(commands.includes("npm install"), false);  // line 83: executes this statement as part of this file's behavior
  assert.equal(commands.includes("npm run build"), true);  // line 84: executes this statement as part of this file's behavior
  assert.equal(commands.includes("npm test"), true);  // line 85: executes this statement as part of this file's behavior

  await fs.rm(path.join(process.cwd(), "generated-projects", project.id), { recursive: true, force: true });  // line 87: executes this statement as part of this file's behavior
});  // line 88: executes this statement as part of this file's behavior


test("build install failures include redacted registry diagnostics", async () => {  // line 91: executes this statement as part of this file's behavior
  const project = projectFixture("project_generated_bot_install_failure_test");  // line 92: executes this statement as part of this file's behavior
  const files = new ProjectFileService();  // line 93: executes this statement as part of this file's behavior
  const workspace = files.workspace(project.id);  // line 94: executes this statement as part of this file's behavior
  await fs.rm(workspace, { recursive: true, force: true });  // line 95: executes this statement as part of this file's behavior
  await files.generate(project, true);  // line 96: executes this statement as part of this file's behavior

  const previousRegistry = process.env.NPM_CONFIG_REGISTRY;  // line 98: executes this statement as part of this file's behavior
  try {  // line 99: executes this statement as part of this file's behavior
    process.env.NPM_CONFIG_REGISTRY = "https://registry-user:registry-password@npm.example.corp/repository/npm/";  // line 100: executes this statement as part of this file's behavior
    const runner: CommandRunner = async (command, args) => {  // line 101: executes this statement as part of this file's behavior
      if (command === "npm" && args[0] === "ci") {  // line 102: executes this statement as part of this file's behavior
        throw new Error("npm error code E403\nnpm error 403 Forbidden - GET https://registry.npmjs.org/discord.js?token=secret-token");  // line 103: executes this statement as part of this file's behavior
      }  // line 104: executes this statement as part of this file's behavior
    };  // line 105: executes this statement as part of this file's behavior
    const service = new BuildService(files, (secretId) => secretId === "secret_discord_token", undefined, runner);  // line 106: executes this statement as part of this file's behavior

    const job = await service.create(project, { clean: false, runTests: true }, "audit_test", "req_test");  // line 108: executes this statement as part of this file's behavior
    const logs = service.getLogs(project.id, job.buildId) ?? "";  // line 109: executes this statement as part of this file's behavior

    assert.equal(job.status, "failed");  // line 111: executes this statement as part of this file's behavior
    assert.equal(logs.includes("registry=https://[REDACTED_CREDENTIALS]@npm.example.corp/repository/npm/"), true);  // line 112: executes this statement as part of this file's behavior
    assert.equal(logs.includes("status=403"), true);  // line 113: executes this statement as part of this file's behavior
    assert.equal(logs.includes("url=https://registry.npmjs.org/discord.js?[REDACTED_QUERY]"), true);  // line 114: executes this statement as part of this file's behavior
    assert.equal(logs.includes("registry-password"), false);  // line 115: executes this statement as part of this file's behavior
    assert.equal(logs.includes("secret-token"), false);  // line 116: executes this statement as part of this file's behavior
  } finally {  // line 117: executes this statement as part of this file's behavior
    if (previousRegistry === undefined) delete process.env.NPM_CONFIG_REGISTRY;  // line 118: executes this statement as part of this file's behavior
    else process.env.NPM_CONFIG_REGISTRY = previousRegistry;  // line 119: executes this statement as part of this file's behavior
    await fs.rm(workspace, { recursive: true, force: true });  // line 120: executes this statement as part of this file's behavior
  }  // line 121: executes this statement as part of this file's behavior
});  // line 122: executes this statement as part of this file's behavior

test("generated workspace helper resolves normal roots and project workspaces", () => {  // line 124: executes this statement as part of this file's behavior
  const files = new ProjectFileService(path.join(process.cwd(), "generated-projects", "workspace-validation-normal-test"));  // line 125: executes this statement as part of this file's behavior
  const project = projectFixture("project_workspace_validation_normal_test");  // line 126: executes this statement as part of this file's behavior
  const resolved = files.resolveWorkspace(project.id);  // line 127: executes this statement as part of this file's behavior
  const relativeWorkspace = path.relative(resolved.root, resolved.workspace);  // line 128: executes this statement as part of this file's behavior

  assert.equal(resolved.root, files.generatedRoot());  // line 130: executes this statement as part of this file's behavior
  assert.equal(path.isAbsolute(resolved.root), true);  // line 131: executes this statement as part of this file's behavior
  assert.equal(path.isAbsolute(resolved.workspace), true);  // line 132: executes this statement as part of this file's behavior
  assert.equal(relativeWorkspace, project.id);  // line 133: executes this statement as part of this file's behavior
});  // line 134: executes this statement as part of this file's behavior

test("build rejects crafted workspaces that only contain generated-projects elsewhere in the path", async () => {  // line 136: executes this statement as part of this file's behavior
  const project = projectFixture("project_workspace_validation_crafted_test");  // line 137: executes this statement as part of this file's behavior
  const root = path.join(process.cwd(), "generated-projects", "workspace-validation-root");  // line 138: executes this statement as part of this file's behavior
  const outside = path.join(process.cwd(), "generated-projects-craft", "generated-projects", "outside-root");  // line 139: executes this statement as part of this file's behavior

  class CraftedProjectFileService extends ProjectFileService {  // line 141: executes this statement as part of this file's behavior
    constructor() {  // line 142: executes this statement as part of this file's behavior
      super(root);  // line 143: executes this statement as part of this file's behavior
    }  // line 144: executes this statement as part of this file's behavior

    override workspace(projectId: string): string {  // line 146: executes this statement as part of this file's behavior
      return path.join(outside, projectId);  // line 147: executes this statement as part of this file's behavior
    }  // line 148: executes this statement as part of this file's behavior
  }  // line 149: executes this statement as part of this file's behavior

  const files = new CraftedProjectFileService();  // line 151: executes this statement as part of this file's behavior
  const commands: string[] = [];  // line 152: executes this statement as part of this file's behavior
  const runner: CommandRunner = async (command, args) => {  // line 153: executes this statement as part of this file's behavior
    commands.push([command, ...args].join(" "));  // line 154: executes this statement as part of this file's behavior
  };  // line 155: executes this statement as part of this file's behavior
  const service = new BuildService(files, (secretId) => secretId === "secret_discord_token", undefined, runner);  // line 156: executes this statement as part of this file's behavior

  try {  // line 158: executes this statement as part of this file's behavior
    const job = await service.create(project, { clean: false, runTests: true }, "audit_test", "req_test");  // line 159: executes this statement as part of this file's behavior
    const logs = service.getLogs(project.id, job.buildId) ?? "";  // line 160: executes this statement as part of this file's behavior

    assert.equal(job.status, "failed");  // line 162: executes this statement as part of this file's behavior
    assert.equal(job.errorMessage, "Refusing to build outside generated-projects workspace.");  // line 163: executes this statement as part of this file's behavior
    assert.equal(logs.includes("Refusing to build outside generated-projects workspace."), true);  // line 164: executes this statement as part of this file's behavior
    assert.equal(commands.length, 0);  // line 165: executes this statement as part of this file's behavior
  } finally {  // line 166: executes this statement as part of this file's behavior
    await fs.rm(root, { recursive: true, force: true });  // line 167: executes this statement as part of this file's behavior
    await fs.rm(path.join(process.cwd(), "generated-projects-craft"), { recursive: true, force: true });  // line 168: executes this statement as part of this file's behavior
  }  // line 169: executes this statement as part of this file's behavior
});  // line 170: executes this statement as part of this file's behavior
