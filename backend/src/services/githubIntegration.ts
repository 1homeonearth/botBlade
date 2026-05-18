import type { BotProject } from "../models/project.js";
import type { ProjectFileService } from "./projectFiles.js";
import { RequestValidationError } from "./projectStore.js";

export interface GitHubStatus { connected: boolean; tokenSecretRef: string | null; message: string; }

export interface GitHubPushFile {
  path: string;
  content: string;
}

export interface GitHubPushInput {
  owner: string;
  repo: string;
  branch: string;
  token: string;
  files: GitHubPushFile[];
  message: string;
}

export interface GitHubPushFileResult {
  path: string;
  status: "created" | "updated";
}

export interface GitHubPushResult {
  owner: string;
  repo: string;
  branch: string;
  pushedAt: string;
  files: GitHubPushFileResult[];
}

export interface GitHubClient {
  pushFiles(input: GitHubPushInput): Promise<GitHubPushFileResult[]>;
}

export class GitHubRestClient implements GitHubClient {
  constructor(private readonly apiBaseUrl = "https://api.github.com") {}

  async pushFiles(input: GitHubPushInput): Promise<GitHubPushFileResult[]> {
    const results: GitHubPushFileResult[] = [];
    for (const file of input.files) {
      const sha = await this.currentFileSha(input, file.path);
      const status = await this.putFile(input, file, sha);
      results.push({ path: file.path, status });
    }
    return results;
  }

  private async currentFileSha(input: GitHubPushInput, filePath: string): Promise<string | undefined> {
    const response = await fetch(this.contentsUrl(input, filePath, true), {
      method: "GET",
      headers: this.headers(input.token),
    });
    if (response.status === 404) return undefined;
    if (!response.ok) throw githubApiError(response.status, "Failed to inspect existing GitHub repository content.");
    const body = await response.json() as { sha?: unknown };
    return typeof body.sha === "string" ? body.sha : undefined;
  }

  private async putFile(input: GitHubPushInput, file: GitHubPushFile, sha: string | undefined): Promise<"created" | "updated"> {
    const response = await fetch(this.contentsUrl(input, file.path, false), {
      method: "PUT",
      headers: this.headers(input.token),
      body: JSON.stringify({
        message: input.message,
        branch: input.branch,
        content: Buffer.from(file.content).toString("base64"),
        ...(sha ? { sha } : {}),
      }),
    });
    if (!response.ok) throw githubApiError(response.status, "Failed to push generated content to GitHub.");
    return sha ? "updated" : "created";
  }

  private contentsUrl(input: GitHubPushInput, filePath: string, includeRef: boolean): string {
    const normalizedPath = filePath.split("/").map((segment) => encodeURIComponent(segment)).join("/");
    const url = `${this.apiBaseUrl}/repos/${encodeURIComponent(input.owner)}/${encodeURIComponent(input.repo)}/contents/${normalizedPath}`;
    return includeRef ? `${url}?ref=${encodeURIComponent(input.branch)}` : url;
  }

  private headers(token: string): Record<string, string> {
    return {
      authorization: `Bearer ${token}`,
      accept: "application/vnd.github+json",
      "content-type": "application/json",
      "x-github-api-version": "2022-11-28",
      "user-agent": "royalScepter-backend",
    };
  }
}

export class GitHubIntegrationService {
  private tokenSecretRef: string | null = null;

  constructor(
    private readonly secretExists: (secretId: string) => boolean,
    private readonly getSecretValue?: (secretId: string) => string | undefined,
    private readonly fileService?: ProjectFileService,
    private readonly client: GitHubClient = new GitHubRestClient(),
    private readonly now: () => Date = () => new Date(),
  ) {}

  status(): GitHubStatus {
    return { connected: Boolean(this.tokenSecretRef && this.secretExists(this.tokenSecretRef)), tokenSecretRef: this.tokenSecretRef, message: this.tokenSecretRef ? "GitHub token secret reference configured." : "GitHub token secret reference is not configured." };
  }

  connect(input: unknown): GitHubStatus {
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

  async push(project: BotProject): Promise<GitHubPushResult> {
    const token = this.githubToken();
    if (!project.github?.owner || !project.github?.repo) throw repoNotLinked("pushing to GitHub");
    if (!this.fileService) throw { statusCode: 500, code: "GITHUB_FILE_SERVICE_UNAVAILABLE", message: "Project file service is not available for GitHub pushes.", details: {} };

    await this.fileService.ensureGenerated(project);
    const workflow = this.workflow(project);
    const files = await this.generatedFiles(project.id, workflow);
    const pushedAt = this.now().toISOString();
    const pushedFiles = await this.client.pushFiles({
      owner: project.github.owner,
      repo: project.github.repo,
      branch: project.github.defaultBranch || "main",
      token,
      files,
      message: `Update generated royalScepter bot project at ${pushedAt}`,
    });
    project.github.lastPushedAt = pushedAt;
    project.updatedAt = pushedAt;
    return { owner: project.github.owner, repo: project.github.repo, branch: project.github.defaultBranch || "main", pushedAt, files: pushedFiles };
  }

  workflow(project: BotProject): { path: string; content: string } {
    if (!project.github?.owner || !project.github?.repo) throw repoNotLinked("creating workflow content");
    return { path: ".github/workflows/royalscepter-build.yml", content: `name: royalScepter Build\n\non:\n  push:\n    branches: [${project.github?.defaultBranch || "main"}]\n\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-node@v4\n        with:\n          node-version: 22\n      - run: npm ci\n      - run: npm run build\n` };
  }

  private githubToken(): string {
    if (!this.tokenSecretRef || !this.secretExists(this.tokenSecretRef)) throw notConfigured();
    const token = this.getSecretValue?.(this.tokenSecretRef);
    if (!token) throw notConfigured();
    return token;
  }

  private async generatedFiles(projectId: string, workflow: { path: string; content: string }): Promise<GitHubPushFile[]> {
    const summaries = await this.fileService!.list(projectId);
    const files: GitHubPushFile[] = [];
    for (const summary of summaries) {
      if (!summary.generated) continue;
      const content = await this.fileService!.read(projectId, summary.path);
      files.push({ path: content.path, content: content.content });
    }
    files.push(workflow);
    return files.sort((a, b) => a.path.localeCompare(b.path));
  }
}

function notConfigured(): never {
  throw { statusCode: 400, code: "NOT_CONFIGURED", message: "Configure a GitHub token secret reference before running this operation.", details: {} };
}

function repoNotLinked(action: string): never {
  throw { statusCode: 400, code: "GITHUB_REPO_NOT_LINKED", message: `Link owner/repo before ${action}.`, details: {} };
}

function githubApiError(statusCode: number, message: string): never {
  throw { statusCode: statusCode >= 400 && statusCode < 600 ? statusCode : 502, code: "GITHUB_API_ERROR", message, details: { statusCode } };
}
