// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import fs from "node:fs/promises";  // line 7: executes this statement as part of this file's behavior
import path from "node:path";  // line 8: executes this statement as part of this file's behavior
import { execFileSync } from "node:child_process";  // line 9: executes this statement as part of this file's behavior
import type { BotProject } from "../models/project.js";  // line 10: executes this statement as part of this file's behavior
import { RequestValidationError } from "./projectStore.js";  // line 11: executes this statement as part of this file's behavior
import { templateFiles } from "./projectFiles.js";  // line 12: executes this statement as part of this file's behavior
import type { AuditInput } from "./auditService.js";  // line 13: executes this statement as part of this file's behavior

export interface GitHubStatus { connected: boolean; tokenSecretRef: string | null; message: string; }  // line 15: executes this statement as part of this file's behavior
export interface GitHubPushResult { pushedAt: string; branch: string; remoteUrl: string; filesPushed: number; }  // line 16: executes this statement as part of this file's behavior

export interface GitHubClient {  // line 18: executes this statement as part of this file's behavior
  pushProject(project: BotProject, token: string): Promise<{ branch: string; remoteUrl: string; filesPushed: number }>;  // line 19: executes this statement as part of this file's behavior
}  // line 20: executes this statement as part of this file's behavior

class GitCliGitHubClient implements GitHubClient {  // line 22: executes this statement as part of this file's behavior
  constructor(private readonly workspaceRoot = path.join(process.cwd(), "generated-projects")) {}  // line 23: executes this statement as part of this file's behavior
  async pushProject(project: BotProject, token: string): Promise<{ branch: string; remoteUrl: string; filesPushed: number }> {  // line 24: executes this statement as part of this file's behavior
    const owner = project.github?.owner?.trim();  // line 25: executes this statement as part of this file's behavior
    const repo = project.github?.repo?.trim();  // line 26: executes this statement as part of this file's behavior
    if (!owner || !repo) throw new Error("GitHub repo not linked");  // line 27: executes this statement as part of this file's behavior
    const branch = project.github?.defaultBranch || "main";  // line 28: executes this statement as part of this file's behavior
    const workspace = path.join(this.workspaceRoot, project.id.replace(/[^A-Za-z0-9_-]/g, "_"));  // line 29: executes this statement as part of this file's behavior
    await fs.mkdir(workspace, { recursive: true });  // line 30: executes this statement as part of this file's behavior
    const files = { ...templateFiles(project), ".github/workflows/botblade-build.yml": workflowContent(branch) };  // line 31: executes this statement as part of this file's behavior
    for (const [relativePath, content] of Object.entries(files)) {  // line 32: executes this statement as part of this file's behavior
      const target = path.join(workspace, relativePath);  // line 33: executes this statement as part of this file's behavior
      await fs.mkdir(path.dirname(target), { recursive: true });  // line 34: executes this statement as part of this file's behavior
      await fs.writeFile(target, content, "utf8");  // line 35: executes this statement as part of this file's behavior
    }  // line 36: executes this statement as part of this file's behavior
    const remoteUrl = `https://x-access-token:${token}@github.com/${owner}/${repo}.git`;  // line 37: executes this statement as part of this file's behavior
    execFileSync("git", ["-C", workspace, "init"]);  // line 38: executes this statement as part of this file's behavior
    execFileSync("git", ["-C", workspace, "checkout", "-B", branch]);  // line 39: executes this statement as part of this file's behavior
    execFileSync("git", ["-C", workspace, "add", "."]);  // line 40: executes this statement as part of this file's behavior
    execFileSync("git", ["-C", workspace, "config", "user.email", "botblade-bot@users.noreply.github.com"]);  // line 41: executes this statement as part of this file's behavior
    execFileSync("git", ["-C", workspace, "config", "user.name", "botBlade Bot"]);  // line 42: executes this statement as part of this file's behavior
    execFileSync("git", ["-C", workspace, "commit", "-m", "chore: generate botBlade project"]);  // line 43: executes this statement as part of this file's behavior
    try { execFileSync("git", ["-C", workspace, "remote", "remove", "origin"]); } catch {}  // line 44: executes this statement as part of this file's behavior
    execFileSync("git", ["-C", workspace, "remote", "add", "origin", remoteUrl]);  // line 45: executes this statement as part of this file's behavior
    execFileSync("git", ["-C", workspace, "push", "--set-upstream", "origin", branch, "--force"]);  // line 46: executes this statement as part of this file's behavior
    return { branch, remoteUrl: `https://github.com/${owner}/${repo}.git`, filesPushed: Object.keys(files).length };  // line 47: executes this statement as part of this file's behavior
  }  // line 48: executes this statement as part of this file's behavior
}  // line 49: executes this statement as part of this file's behavior

export class GitHubIntegrationService {  // line 51: executes this statement as part of this file's behavior
  private tokenSecretRef: string | null = null;  // line 52: executes this statement as part of this file's behavior

  constructor(  // line 54: executes this statement as part of this file's behavior
    private readonly secretExists: (secretId: string) => boolean,  // line 55: executes this statement as part of this file's behavior
    private readonly resolveSecretValue?: (secretId: string) => string | undefined,  // line 56: executes this statement as part of this file's behavior
    private readonly auditRecord?: (input: AuditInput) => unknown,  // line 57: executes this statement as part of this file's behavior
    private readonly githubClient: GitHubClient = new GitCliGitHubClient(),  // line 58: executes this statement as part of this file's behavior
  ) {}  // line 59: executes this statement as part of this file's behavior

  status(): GitHubStatus {  // line 61: executes this statement as part of this file's behavior
    return { connected: Boolean(this.tokenSecretRef && this.secretExists(this.tokenSecretRef)), tokenSecretRef: this.tokenSecretRef, message: this.tokenSecretRef ? "GitHub token secret reference configured." : "GitHub token secret reference is not configured." };  // line 62: executes this statement as part of this file's behavior
  }  // line 63: executes this statement as part of this file's behavior

  connect(input: unknown): GitHubStatus { /* unchanged */  // line 65: executes this statement as part of this file's behavior
    const object = input && typeof input === "object" && !Array.isArray(input) ? input as Record<string, unknown> : {};  // line 66: executes this statement as part of this file's behavior
    const tokenSecretRef = typeof object.tokenSecretRef === "string" ? object.tokenSecretRef.trim() : "";  // line 67: executes this statement as part of this file's behavior
    if (!tokenSecretRef) throw new RequestValidationError([{ field: "tokenSecretRef", message: "GitHub token secret reference is required." }]);  // line 68: executes this statement as part of this file's behavior
    if (!this.secretExists(tokenSecretRef)) throw new RequestValidationError([{ field: "tokenSecretRef", message: "Secret reference does not exist." }]);  // line 69: executes this statement as part of this file's behavior
    this.tokenSecretRef = tokenSecretRef;  // line 70: executes this statement as part of this file's behavior
    return this.status();  // line 71: executes this statement as part of this file's behavior
  }  // line 72: executes this statement as part of this file's behavior

  linkRepo(project: BotProject, input: unknown): BotProject {  // line 74: executes this statement as part of this file's behavior
    const object = input && typeof input === "object" && !Array.isArray(input) ? input as Record<string, unknown> : {};  // line 75: executes this statement as part of this file's behavior
    const owner = typeof object.owner === "string" ? object.owner.trim() : "";  // line 76: executes this statement as part of this file's behavior
    const repo = typeof object.repo === "string" ? object.repo.trim() : "";  // line 77: executes this statement as part of this file's behavior
    if (!owner) throw new RequestValidationError([{ field: "owner", message: "GitHub owner is required." }]);  // line 78: executes this statement as part of this file's behavior
    if (!repo) throw new RequestValidationError([{ field: "repo", message: "GitHub repository name is required." }]);  // line 79: executes this statement as part of this file's behavior
    project.github = { owner, repo, defaultBranch: typeof object.defaultBranch === "string" && object.defaultBranch.trim() ? object.defaultBranch.trim() : "main", lastPushedAt: null };  // line 80: executes this statement as part of this file's behavior
    project.updatedAt = new Date().toISOString();  // line 81: executes this statement as part of this file's behavior
    return project;  // line 82: executes this statement as part of this file's behavior
  }  // line 83: executes this statement as part of this file's behavior

  async push(project: BotProject, requestId = "github-push"): Promise<GitHubPushResult> {  // line 85: executes this statement as part of this file's behavior
    if (!this.tokenSecretRef || !this.secretExists(this.tokenSecretRef)) throw notConfigured();  // line 86: executes this statement as part of this file's behavior
    if (!project.github?.owner?.trim() || !project.github?.repo?.trim()) throw { statusCode: 400, code: "GITHUB_REPO_NOT_LINKED", message: "Link owner/repo before pushing to GitHub.", details: {} };  // line 87: executes this statement as part of this file's behavior
    const token = this.resolveSecretValue?.(this.tokenSecretRef);  // line 88: executes this statement as part of this file's behavior
    if (!token) throw { statusCode: 400, code: "GITHUB_TOKEN_UNAVAILABLE", message: "GitHub token secret reference could not be resolved.", details: {} };  // line 89: executes this statement as part of this file's behavior
    try {  // line 90: executes this statement as part of this file's behavior
      const pushed = await this.githubClient.pushProject(project, token);  // line 91: executes this statement as part of this file's behavior
      const pushedAt = new Date().toISOString();  // line 92: executes this statement as part of this file's behavior
      project.github.lastPushedAt = pushedAt;  // line 93: executes this statement as part of this file's behavior
      project.updatedAt = pushedAt;  // line 94: executes this statement as part of this file's behavior
      this.auditRecord?.({ action: "github.push", resourceType: "project", resourceId: project.id, projectId: project.id, requestId, metadata: { status: "success", owner: project.github.owner, repo: project.github.repo, branch: pushed.branch } });  // line 95: executes this statement as part of this file's behavior
      return { ...pushed, pushedAt };  // line 96: executes this statement as part of this file's behavior
    } catch (error) {  // line 97: executes this statement as part of this file's behavior
      const safeSummary = sanitizeErrorSummary(error);  // line 98: executes this statement as part of this file's behavior
      this.auditRecord?.({ action: "github.push", resourceType: "project", resourceId: project.id, projectId: project.id, requestId, metadata: { status: "failure", message: safeSummary } });  // line 99: executes this statement as part of this file's behavior
      throw { statusCode: 502, code: "GITHUB_PUSH_FAILED", message: "GitHub push failed.", details: { reason: "git_push_failed" } };  // line 100: executes this statement as part of this file's behavior
    }  // line 101: executes this statement as part of this file's behavior
  }  // line 102: executes this statement as part of this file's behavior

  workflow(project: BotProject): { path: string; content: string } {  // line 104: executes this statement as part of this file's behavior
    if (!project.github?.owner || !project.github?.repo) throw { statusCode: 400, code: "GITHUB_REPO_NOT_LINKED", message: "Link owner/repo before creating workflow content.", details: {} };  // line 105: executes this statement as part of this file's behavior
    return { path: ".github/workflows/botblade-build.yml", content: workflowContent(project.github?.defaultBranch || "main") };  // line 106: executes this statement as part of this file's behavior
  }  // line 107: executes this statement as part of this file's behavior
}  // line 108: executes this statement as part of this file's behavior


function sanitizeErrorSummary(input: unknown): string {  // line 111: executes this statement as part of this file's behavior
  if (!(input instanceof Error) || typeof input.message !== "string") return "operation_failed";  // line 112: executes this statement as part of this file's behavior
  const condensed = input.message.replace(/\s+/g, " ").trim();  // line 113: executes this statement as part of this file's behavior
  if (!condensed) return "operation_failed";  // line 114: executes this statement as part of this file's behavior
  const redacted = condensed  // line 115: executes this statement as part of this file's behavior
    .replace(/https?:\/\/[^\s@]+@/gi, "https://[REDACTED]@")  // line 116: executes this statement as part of this file's behavior
    .replace(/\b(?:gh[pousr]_[A-Za-z0-9_]+|github_pat_[A-Za-z0-9_]+|x-access-token:[^\s@]+|Bearer\s+[A-Za-z0-9._-]+|token[=:]\s*[^\s,;]+)/gi, "[REDACTED_TOKEN]");  // line 117: executes this statement as part of this file's behavior
  const summary = redacted.slice(0, 200);  // line 118: executes this statement as part of this file's behavior
  return summary || "operation_failed";  // line 119: executes this statement as part of this file's behavior
}  // line 120: executes this statement as part of this file's behavior

function workflowContent(branch: string): string { return `name: botBlade Build\n\non:\n  push:\n    branches: [${branch}]\n\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-node@v4\n        with:\n          node-version: 22\n      - run: npm ci\n      - run: npm run build\n`; }  // line 122: executes this statement as part of this file's behavior

function notConfigured(): never { throw { statusCode: 400, code: "NOT_CONFIGURED", message: "Configure a GitHub token secret reference before running this operation.", details: {} }; }  // line 124: executes this statement as part of this file's behavior
