// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import test from "node:test";  // line 7: executes this statement as part of this file's behavior
import assert from "node:assert/strict";  // line 8: executes this statement as part of this file's behavior
import { GitHubIntegrationService } from "../services/githubIntegration.js";  // line 9: executes this statement as part of this file's behavior
import type { BotProject } from "../models/project.js";  // line 10: executes this statement as part of this file's behavior

function project(): BotProject {  // line 12: executes this statement as part of this file's behavior
  const now = new Date().toISOString();  // line 13: executes this statement as part of this file's behavior
  return {  // line 14: executes this statement as part of this file's behavior
    id: "project_1", name: "P", slug: "p", description: "", templateId: "discord_ts", language: "typescript", runtime: "node22",  // line 15: executes this statement as part of this file's behavior
    discord: { applicationId: null, clientId: null, defaultGuildId: null, tokenSecretRef: null, commandRegistration: "guild" },  // line 16: executes this statement as part of this file's behavior
    permissions: { intents: ["Guilds"], botPermissions: [] }, commands: [], events: [], deployment: { targetId: null, lastDeploymentId: null },  // line 17: executes this statement as part of this file's behavior
    github: { owner: "octo", repo: "repo", defaultBranch: "main", lastPushedAt: null }, archivedAt: null, createdAt: now, updatedAt: now,  // line 18: executes this statement as part of this file's behavior
  };  // line 19: executes this statement as part of this file's behavior
}  // line 20: executes this statement as part of this file's behavior

test("push fails when github integration is not configured", async () => {  // line 22: executes this statement as part of this file's behavior
  const svc = new GitHubIntegrationService(() => false);  // line 23: executes this statement as part of this file's behavior
  let code = "";  // line 24: executes this statement as part of this file's behavior
  try { await svc.push(project()); } catch (err: any) { code = err.code; }  // line 25: executes this statement as part of this file's behavior
  assert.equal(code, "NOT_CONFIGURED");  // line 26: executes this statement as part of this file's behavior
});  // line 27: executes this statement as part of this file's behavior

test("push fails when repo is not linked", async () => {  // line 29: executes this statement as part of this file's behavior
  const svc = new GitHubIntegrationService(() => true, () => "token", undefined, { pushProject: async () => ({ branch: "main", remoteUrl: "x", filesPushed: 1 }) });  // line 30: executes this statement as part of this file's behavior
  svc.connect({ tokenSecretRef: "secret_1" });  // line 31: executes this statement as part of this file's behavior
  const p = project();  // line 32: executes this statement as part of this file's behavior
  p.github = { owner: "", repo: "", defaultBranch: "main", lastPushedAt: null };  // line 33: executes this statement as part of this file's behavior
  let code = "";  // line 34: executes this statement as part of this file's behavior
  try { await svc.push(p); } catch (err: any) { code = err.code; }  // line 35: executes this statement as part of this file's behavior
  assert.equal(code, "GITHUB_REPO_NOT_LINKED");  // line 36: executes this statement as part of this file's behavior
});  // line 37: executes this statement as part of this file's behavior

test("push updates lastPushedAt only on success and records audit", async () => {  // line 39: executes this statement as part of this file's behavior
  const events: Array<Record<string, unknown>> = [];  // line 40: executes this statement as part of this file's behavior
  const svc = new GitHubIntegrationService(() => true, () => "token", (input) => { events.push(input as unknown as Record<string, unknown>); }, {  // line 41: executes this statement as part of this file's behavior
    pushProject: async () => ({ branch: "main", remoteUrl: "https://github.com/octo/repo.git", filesPushed: 3 }),  // line 42: executes this statement as part of this file's behavior
  });  // line 43: executes this statement as part of this file's behavior
  svc.connect({ tokenSecretRef: "secret_1" });  // line 44: executes this statement as part of this file's behavior
  const p = project();  // line 45: executes this statement as part of this file's behavior
  const result = await svc.push(p, "req_1");  // line 46: executes this statement as part of this file's behavior
  assert.equal(result.branch, "main");  // line 47: executes this statement as part of this file's behavior
  assert.equal(typeof p.github?.lastPushedAt, "string");  // line 48: executes this statement as part of this file's behavior
  assert.equal(events.length, 1);  // line 49: executes this statement as part of this file's behavior
  assert.equal(events[0].action, "github.push");  // line 50: executes this statement as part of this file's behavior
  assert.equal((events[0].metadata as Record<string, unknown>).status, "success");  // line 51: executes this statement as part of this file's behavior
});  // line 52: executes this statement as part of this file's behavior

test("push failure redacts token-like content from audit metadata and API details", async () => {  // line 54: executes this statement as part of this file's behavior
  const events: Array<Record<string, unknown>> = [];  // line 55: executes this statement as part of this file's behavior
  const leakedToken = "ghp_superSecretToken123456";  // line 56: executes this statement as part of this file's behavior
  const leakedPat = "github_pat_verySecretToken987";  // line 57: executes this statement as part of this file's behavior
  const svc = new GitHubIntegrationService(() => true, () => "token", (input) => { events.push(input as unknown as Record<string, unknown>); }, {  // line 58: executes this statement as part of this file's behavior
    pushProject: async () => { throw new Error(`fatal: Authentication failed for https://x-access-token:${leakedToken}@github.com/octo/repo.git bearer ${leakedPat}`); },  // line 59: executes this statement as part of this file's behavior
  });  // line 60: executes this statement as part of this file's behavior
  svc.connect({ tokenSecretRef: "secret_1" });  // line 61: executes this statement as part of this file's behavior
  const p = project();  // line 62: executes this statement as part of this file's behavior
  let err: any;  // line 63: executes this statement as part of this file's behavior
  try {  // line 64: executes this statement as part of this file's behavior
    await svc.push(p, "req_fail");  // line 65: executes this statement as part of this file's behavior
  } catch (error: any) {  // line 66: executes this statement as part of this file's behavior
    err = error;  // line 67: executes this statement as part of this file's behavior
  }  // line 68: executes this statement as part of this file's behavior
  assert.equal(err.code, "GITHUB_PUSH_FAILED");  // line 69: executes this statement as part of this file's behavior
  assert.equal(err.details.reason, "git_push_failed");  // line 70: executes this statement as part of this file's behavior
  assert.equal(JSON.stringify(err).includes(leakedToken), false);  // line 71: executes this statement as part of this file's behavior
  assert.equal(JSON.stringify(err).includes(leakedPat), false);  // line 72: executes this statement as part of this file's behavior
  assert.equal(events.length, 1);  // line 73: executes this statement as part of this file's behavior
  const metadata = events[0].metadata as Record<string, unknown>;  // line 74: executes this statement as part of this file's behavior
  assert.equal(metadata.status, "failure");  // line 75: executes this statement as part of this file's behavior
  const message = String(metadata.message);  // line 76: executes this statement as part of this file's behavior
  assert.equal(message.includes(leakedToken), false);  // line 77: executes this statement as part of this file's behavior
  assert.equal(message.includes(leakedPat), false);  // line 78: executes this statement as part of this file's behavior
  assert.equal(message.includes("[REDACTED_TOKEN]"), true);  // line 79: executes this statement as part of this file's behavior
});  // line 80: executes this statement as part of this file's behavior
