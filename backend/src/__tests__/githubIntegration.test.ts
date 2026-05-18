import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { GitHubIntegrationService, type GitHubClient, type GitHubPushFileResult, type GitHubPushInput } from "../services/githubIntegration.js";
import { ProjectFileService } from "../services/projectFiles.js";
import { ProjectStore } from "../services/projectStore.js";

class MockGitHubClient implements GitHubClient {
  calls: GitHubPushInput[] = [];
  result: GitHubPushFileResult[] = [];

  async pushFiles(input: GitHubPushInput): Promise<GitHubPushFileResult[]> {
    this.calls.push(input);
    return this.result.length > 0 ? this.result : input.files.map((file) => ({ path: file.path, status: "created" as const }));
  }
}

function createProject() {
  return new ProjectStore().create({
    name: "GitHub Push Project",
    github: { owner: "princess", repo: "royal-scepter", defaultBranch: "main" },
  });
}

async function assertPushError(service: GitHubIntegrationService, project: ReturnType<typeof createProject>, code: string) {
  let thrown: unknown;
  try {
    await service.push(project);
  } catch (error) {
    thrown = error;
  }
  assert.ok(thrown, "Expected GitHub push to fail.");
  assert.equal((thrown as { code?: string }).code, code);
}

test("GitHub push rejects when token secret reference is not configured", async () => {
  const client = new MockGitHubClient();
  const fileService = new ProjectFileService(mkdtempSync(path.join(tmpdir(), "rs-gh-not-configured-")));
  const service = new GitHubIntegrationService(() => false, () => undefined, fileService, client);
  const project = createProject();

  await assertPushError(service, project, "NOT_CONFIGURED");

  assert.equal(client.calls.length, 0);
  assert.equal(project.github?.lastPushedAt, null);
});

test("GitHub push rejects when a repository is not linked", async () => {
  const client = new MockGitHubClient();
  const fileService = new ProjectFileService(mkdtempSync(path.join(tmpdir(), "rs-gh-unlinked-")));
  const service = new GitHubIntegrationService((secretId) => secretId === "secret_git", () => "token-from-secret-store", fileService, client);
  service.connect({ tokenSecretRef: "secret_git" });
  const project = new ProjectStore().create({ name: "No Repo" });
  project.github = { owner: null, repo: null, defaultBranch: "main", lastPushedAt: null };

  await assertPushError(service, project, "GITHUB_REPO_NOT_LINKED");

  assert.equal(client.calls.length, 0);
  assert.equal(project.github.lastPushedAt, null);
});

test("GitHub push sends generated project files and workflow through the client", async () => {
  const client = new MockGitHubClient();
  const fileService = new ProjectFileService(mkdtempSync(path.join(tmpdir(), "rs-gh-success-")));
  const pushedAt = "2026-05-18T10:15:00.000Z";
  const service = new GitHubIntegrationService((secretId) => secretId === "secret_git", () => "token-from-secret-store", fileService, client, () => new Date(pushedAt));
  service.connect({ tokenSecretRef: "secret_git" });
  const project = createProject();

  const result = await service.push(project);

  assert.equal(client.calls.length, 1);
  assert.equal(client.calls[0].owner, "princess");
  assert.equal(client.calls[0].repo, "royal-scepter");
  assert.equal(client.calls[0].branch, "main");
  assert.equal(client.calls[0].token, "token-from-secret-store");
  const pushedPaths = client.calls[0].files.map((file) => file.path).sort();
  assert.ok(pushedPaths.includes("package.json"));
  assert.ok(pushedPaths.includes("package-lock.json"));
  assert.ok(pushedPaths.includes("src/index.ts"));
  assert.ok(pushedPaths.includes(".github/workflows/royalscepter-build.yml"));
  const workflow = client.calls[0].files.find((file) => file.path === ".github/workflows/royalscepter-build.yml");
  assert.ok(workflow?.content.includes("npm run build"));
  assert.equal(result.pushedAt, pushedAt);
  assert.equal(result.files.length, client.calls[0].files.length);
  assert.equal(project.github?.lastPushedAt, pushedAt);
});
