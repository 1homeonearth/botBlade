import fs from "node:fs/promises";
import path from "node:path";
import { execFileSync } from "node:child_process";
import type { BotProject } from "../models/project.js";
import { RequestValidationError } from "./projectStore.js";
import { templateFiles } from "./projectFiles.js";
import type { AuditInput } from "./auditService.js";

export interface GitHubStatus { connected: boolean; tokenSecretRef: string | null; message: string; }
export interface GitHubPushResult { pushedAt: string; branch: string; remoteUrl: string; filesPushed: number; }

export interface GitHubClient {
  pushProject(project: BotProject, token: string): Promise<{ branch: string; remoteUrl: string; filesPushed: number }>;
}

class GitCliGitHubClient implements GitHubClient {
  constructor(private readonly workspaceRoot = path.join(process.cwd(), "generated-projects")) {}
  async pushProject(project: BotProject, token: string): Promise<{ branch: string; remoteUrl: string; filesPushed: number }> {
    const owner = project.github?.owner?.trim();
    const repo = project.github?.repo?.trim();
    if (!owner || !repo) throw new Error("GitHub repo not linked");
    const branch = project.github?.defaultBranch || "main";
    const workspace = path.join(this.workspaceRoot, project.id.replace(/[^A-Za-z0-9_-]/g, "_"));
    await fs.mkdir(workspace, { recursive: true });
    const files = { ...templateFiles(project), ".github/workflows/royalscepter-build.yml": workflowContent(branch) };
    for (const [relativePath, content] of Object.entries(files)) {
      const target = path.join(workspace, relativePath);
      await fs.mkdir(path.dirname(target), { recursive: true });
      await fs.writeFile(target, content, "utf8");
    }
    const remoteUrl = `https://x-access-token:${token}@github.com/${owner}/${repo}.git`;
    execFileSync("git", ["-C", workspace, "init"]);
    execFileSync("git", ["-C", workspace, "checkout", "-B", branch]);
    execFileSync("git", ["-C", workspace, "add", "."]);
    execFileSync("git", ["-C", workspace, "config", "user.email", "royalscepter-bot@users.noreply.github.com"]);
    execFileSync("git", ["-C", workspace, "config", "user.name", "royalScepter Bot"]);
    execFileSync("git", ["-C", workspace, "commit", "-m", "chore: generate royalScepter project"]);
    try { execFileSync("git", ["-C", workspace, "remote", "remove", "origin"]); } catch {}
    execFileSync("git", ["-C", workspace, "remote", "add", "origin", remoteUrl]);
    execFileSync("git", ["-C", workspace, "push", "--set-upstream", "origin", branch, "--force"]);
    return { branch, remoteUrl: `https://github.com/${owner}/${repo}.git`, filesPushed: Object.keys(files).length };
  }
}

export class GitHubIntegrationService {
  private tokenSecretRef: string | null = null;

  constructor(
    private readonly secretExists: (secretId: string) => boolean,
    private readonly resolveSecretValue?: (secretId: string) => string | undefined,
    private readonly auditRecord?: (input: AuditInput) => unknown,
    private readonly githubClient: GitHubClient = new GitCliGitHubClient(),
  ) {}

  status(): GitHubStatus {
    return { connected: Boolean(this.tokenSecretRef && this.secretExists(this.tokenSecretRef)), tokenSecretRef: this.tokenSecretRef, message: this.tokenSecretRef ? "GitHub token secret reference configured." : "GitHub token secret reference is not configured." };
  }

  connect(input: unknown): GitHubStatus { /* unchanged */
    const object = input && typeof input === "object" && !Array.isArray(input) ? input as Record<string, unknown> : {};
    const tokenSecretRef = typeof object.tokenSecretRef === "string" ? object.tokenSecretRef.trim() : "";
    if (!tokenSecretRef) throw new RequestValidationError([{ field: "tokenSecretRef", message: "GitHub token secret reference is required." }]);
    if (!this.secretExists(tokenSecretRef)) throw new RequestValidationError([{ field: "tokenSecretRef", message: "Secret reference does not exist." }]);
    this.tokenSecretRef = tokenSecretRef;
    return this.status();
  }

  linkRepo(project: BotProject, input: unknown): BotProject {
    const object = input && typeof input === "object" && !Array.isArray(input) ? input as Record<string, unknown> : {};
    const owner = typeof object.owner === "string" ? object.owner.trim() : "";
    const repo = typeof object.repo === "string" ? object.repo.trim() : "";
    if (!owner) throw new RequestValidationError([{ field: "owner", message: "GitHub owner is required." }]);
    if (!repo) throw new RequestValidationError([{ field: "repo", message: "GitHub repository name is required." }]);
    project.github = { owner, repo, defaultBranch: typeof object.defaultBranch === "string" && object.defaultBranch.trim() ? object.defaultBranch.trim() : "main", lastPushedAt: null };
    project.updatedAt = new Date().toISOString();
    return project;
  }

  async push(project: BotProject, requestId = "github-push"): Promise<GitHubPushResult> {
    if (!this.tokenSecretRef || !this.secretExists(this.tokenSecretRef)) throw notConfigured();
    if (!project.github?.owner?.trim() || !project.github?.repo?.trim()) throw { statusCode: 400, code: "GITHUB_REPO_NOT_LINKED", message: "Link owner/repo before pushing to GitHub.", details: {} };
    const token = this.resolveSecretValue?.(this.tokenSecretRef);
    if (!token) throw { statusCode: 400, code: "GITHUB_TOKEN_UNAVAILABLE", message: "GitHub token secret reference could not be resolved.", details: {} };
    try {
      const pushed = await this.githubClient.pushProject(project, token);
      const pushedAt = new Date().toISOString();
      project.github.lastPushedAt = pushedAt;
      project.updatedAt = pushedAt;
      this.auditRecord?.({ action: "github.push", resourceType: "project", resourceId: project.id, projectId: project.id, requestId, metadata: { status: "success", owner: project.github.owner, repo: project.github.repo, branch: pushed.branch } });
      return { ...pushed, pushedAt };
    } catch (error) {
      this.auditRecord?.({ action: "github.push", resourceType: "project", resourceId: project.id, projectId: project.id, requestId, metadata: { status: "failure", message: error instanceof Error ? error.message : "unknown" } });
      throw { statusCode: 502, code: "GITHUB_PUSH_FAILED", message: "GitHub push failed.", details: { reason: error instanceof Error ? error.message : "unknown" } };
    }
  }

  workflow(project: BotProject): { path: string; content: string } {
    if (!project.github?.owner || !project.github?.repo) throw { statusCode: 400, code: "GITHUB_REPO_NOT_LINKED", message: "Link owner/repo before creating workflow content.", details: {} };
    return { path: ".github/workflows/royalscepter-build.yml", content: workflowContent(project.github?.defaultBranch || "main") };
  }
}

function workflowContent(branch: string): string { return `name: royalScepter Build\n\non:\n  push:\n    branches: [${branch}]\n\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-node@v4\n        with:\n          node-version: 22\n      - run: npm ci\n      - run: npm run build\n`; }

function notConfigured(): never { throw { statusCode: 400, code: "NOT_CONFIGURED", message: "Configure a GitHub token secret reference before running this operation.", details: {} }; }
