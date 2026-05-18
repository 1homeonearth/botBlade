import test from "node:test";
import assert from "node:assert/strict";
import { GitHubIntegrationService } from "../services/githubIntegration.js";
import type { BotProject } from "../models/project.js";

function project(): BotProject {
  const now = new Date().toISOString();
  return {
    id: "project_1", name: "P", slug: "p", description: "", templateId: "discord_ts", language: "typescript", runtime: "node22",
    discord: { applicationId: null, clientId: null, defaultGuildId: null, tokenSecretRef: null, commandRegistration: "guild" },
    permissions: { intents: ["Guilds"], botPermissions: [] }, commands: [], events: [], deployment: { targetId: null, lastDeploymentId: null },
    github: { owner: "octo", repo: "repo", defaultBranch: "main", lastPushedAt: null }, archivedAt: null, createdAt: now, updatedAt: now,
  };
}

test("push fails when github integration is not configured", async () => {
  const svc = new GitHubIntegrationService(() => false);
  let code = "";
  try { await svc.push(project()); } catch (err: any) { code = err.code; }
  assert.equal(code, "NOT_CONFIGURED");
});

test("push fails when repo is not linked", async () => {
  const svc = new GitHubIntegrationService(() => true, () => "token", undefined, { pushProject: async () => ({ branch: "main", remoteUrl: "x", filesPushed: 1 }) });
  svc.connect({ tokenSecretRef: "secret_1" });
  const p = project();
  p.github = { owner: "", repo: "", defaultBranch: "main", lastPushedAt: null };
  let code = "";
  try { await svc.push(p); } catch (err: any) { code = err.code; }
  assert.equal(code, "GITHUB_REPO_NOT_LINKED");
});

test("push updates lastPushedAt only on success and records audit", async () => {
  const events: Array<Record<string, unknown>> = [];
  const svc = new GitHubIntegrationService(() => true, () => "token", (input) => { events.push(input as unknown as Record<string, unknown>); }, {
    pushProject: async () => ({ branch: "main", remoteUrl: "https://github.com/octo/repo.git", filesPushed: 3 }),
  });
  svc.connect({ tokenSecretRef: "secret_1" });
  const p = project();
  const result = await svc.push(p, "req_1");
  assert.equal(result.branch, "main");
  assert.equal(typeof p.github?.lastPushedAt, "string");
  assert.equal(events.length, 1);
  assert.equal(events[0].action, "github.push");
  assert.equal((events[0].metadata as Record<string, unknown>).status, "success");
});
