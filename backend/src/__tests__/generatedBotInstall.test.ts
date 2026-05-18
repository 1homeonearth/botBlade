import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs/promises";
import path from "node:path";
import type { BotProject } from "../models/project.js";
import { BuildService, type CommandRunner } from "../services/buildService.js";
import { ProjectFileService, templateFiles } from "../services/projectFiles.js";

function projectFixture(id = "project_generated_bot_install_test"): BotProject {
  const now = new Date().toISOString();
  return {
    id,
    name: "Generated Bot Install Test",
    slug: "generated-bot-install-test",
    description: "",
    templateId: "template_blank_discord_ts",
    language: "typescript",
    runtime: "node22",
    discord: {
      applicationId: null,
      clientId: null,
      defaultGuildId: null,
      tokenSecretRef: "secret_discord_token",
      commandRegistration: "guild",
    },
    permissions: { intents: ["Guilds"], botPermissions: [] },
    commands: [{
      id: "cmd_ping",
      name: "ping",
      description: "Replies with pong.",
      type: "chat_input",
      options: [],
      permissions: { defaultMemberPermissions: null, dmPermission: false },
      handler: { kind: "static_response", ephemeral: true, content: "Pong." },
    }],
    events: [],
    deployment: { targetId: null, lastDeploymentId: null },
    archivedAt: null,
    createdAt: now,
    updatedAt: now,
  };
}

test("generated package files pin discord.js and include a reproducible lockfile", () => {
  const files = templateFiles(projectFixture());
  const packageJson = JSON.parse(files["package.json"]) as { dependencies: Record<string, string> };
  const lockfile = JSON.parse(files["package-lock.json"]) as { packages: Record<string, { version?: string; dependencies?: Record<string, string> }> };

  assert.equal(packageJson.dependencies["discord.js"], "14.15.3");
  assert.equal(lockfile.packages[""].dependencies?.["discord.js"], "14.15.3");
  assert.equal(lockfile.packages["node_modules/discord.js"].version, "14.15.3");
  assert.equal(files["README.md"].includes("NPM_CONFIG_REGISTRY=https://npm.example.corp/repository/npm/ npm ci"), true);
});

test("build install step uses npm ci when the generated lockfile is present", async () => {
  const project = projectFixture("project_generated_bot_install_ci_test");
  const files = new ProjectFileService();
  const workspace = files.workspace(project.id);
  await fs.rm(workspace, { recursive: true, force: true });
  await files.generate(project, true);

  const commands: string[] = [];
  const runner: CommandRunner = async (command, args) => {
    commands.push([command, ...args].join(" "));
  };
  const service = new BuildService(files, (secretId) => secretId === "secret_discord_token", undefined, runner);

  const job = await service.create(project, { clean: false, runTests: true }, "audit_test", "req_test");

  assert.equal(job.status, "succeeded");
  assert.equal(commands[0], "npm ci");
  assert.equal(commands.includes("npm install"), false);
  assert.equal(commands.includes("npm run build"), true);
  assert.equal(commands.includes("npm test"), true);

  await fs.rm(path.join(process.cwd(), "generated-projects", project.id), { recursive: true, force: true });
});


test("build install failures include redacted registry diagnostics", async () => {
  const project = projectFixture("project_generated_bot_install_failure_test");
  const files = new ProjectFileService();
  const workspace = files.workspace(project.id);
  await fs.rm(workspace, { recursive: true, force: true });
  await files.generate(project, true);

  const previousRegistry = process.env.NPM_CONFIG_REGISTRY;
  try {
    process.env.NPM_CONFIG_REGISTRY = "https://registry-user:registry-password@npm.example.corp/repository/npm/";
    const runner: CommandRunner = async (command, args) => {
      if (command === "npm" && args[0] === "ci") {
        throw new Error("npm error code E403\nnpm error 403 Forbidden - GET https://registry.npmjs.org/discord.js?token=secret-token");
      }
    };
    const service = new BuildService(files, (secretId) => secretId === "secret_discord_token", undefined, runner);

    const job = await service.create(project, { clean: false, runTests: true }, "audit_test", "req_test");
    const logs = service.getLogs(project.id, job.buildId) ?? "";

    assert.equal(job.status, "failed");
    assert.equal(logs.includes("registry=https://[REDACTED_CREDENTIALS]@npm.example.corp/repository/npm/"), true);
    assert.equal(logs.includes("status=403"), true);
    assert.equal(logs.includes("url=https://registry.npmjs.org/discord.js?[REDACTED_QUERY]"), true);
    assert.equal(logs.includes("registry-password"), false);
    assert.equal(logs.includes("secret-token"), false);
  } finally {
    if (previousRegistry === undefined) delete process.env.NPM_CONFIG_REGISTRY;
    else process.env.NPM_CONFIG_REGISTRY = previousRegistry;
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("generated workspace helper resolves normal roots and project workspaces", () => {
  const files = new ProjectFileService(path.join(process.cwd(), "generated-projects", "workspace-validation-normal-test"));
  const project = projectFixture("project_workspace_validation_normal_test");
  const resolved = files.resolveWorkspace(project.id);
  const relativeWorkspace = path.relative(resolved.root, resolved.workspace);

  assert.equal(resolved.root, files.generatedRoot());
  assert.equal(path.isAbsolute(resolved.root), true);
  assert.equal(path.isAbsolute(resolved.workspace), true);
  assert.equal(relativeWorkspace, project.id);
});

test("build rejects crafted workspaces that only contain generated-projects elsewhere in the path", async () => {
  const project = projectFixture("project_workspace_validation_crafted_test");
  const root = path.join(process.cwd(), "generated-projects", "workspace-validation-root");
  const outside = path.join(process.cwd(), "generated-projects-craft", "generated-projects", "outside-root");

  class CraftedProjectFileService extends ProjectFileService {
    constructor() {
      super(root);
    }

    override workspace(projectId: string): string {
      return path.join(outside, projectId);
    }
  }

  const files = new CraftedProjectFileService();
  const commands: string[] = [];
  const runner: CommandRunner = async (command, args) => {
    commands.push([command, ...args].join(" "));
  };
  const service = new BuildService(files, (secretId) => secretId === "secret_discord_token", undefined, runner);

  try {
    const job = await service.create(project, { clean: false, runTests: true }, "audit_test", "req_test");
    const logs = service.getLogs(project.id, job.buildId) ?? "";

    assert.equal(job.status, "failed");
    assert.equal(job.errorMessage, "Refusing to build outside generated-projects workspace.");
    assert.equal(logs.includes("Refusing to build outside generated-projects workspace."), true);
    assert.equal(commands.length, 0);
  } finally {
    await fs.rm(root, { recursive: true, force: true });
    await fs.rm(path.join(process.cwd(), "generated-projects-craft"), { recursive: true, force: true });
  }
});
