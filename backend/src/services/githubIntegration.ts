import type { BotProject } from "../models/project.js";
import { RequestValidationError } from "./projectStore.js";

export interface GitHubStatus { connected: boolean; tokenSecretRef: string | null; message: string; }

export class GitHubIntegrationService {
  private tokenSecretRef: string | null = null;

  constructor(private readonly secretExists: (secretId: string) => boolean) {}

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

  push(project: BotProject): never {
    if (!this.tokenSecretRef || !this.secretExists(this.tokenSecretRef)) throw notConfigured();
    if (!project.github?.owner || !project.github?.repo) throw { statusCode: 400, code: "GITHUB_REPO_NOT_LINKED", message: "Link owner/repo before pushing to GitHub.", details: {} };
    throw { statusCode: 501, code: "GITHUB_PUSH_NOT_IMPLEMENTED", message: "GitHub push is not implemented yet; no fake success was returned.", details: {} };
  }

  workflow(project: BotProject): { path: string; content: string } {
    if (!project.github?.owner || !project.github?.repo) throw { statusCode: 400, code: "GITHUB_REPO_NOT_LINKED", message: "Link owner/repo before creating workflow content.", details: {} };
    return { path: ".github/workflows/royalscepter-build.yml", content: `name: royalScepter Build\n\non:\n  push:\n    branches: [${project.github?.defaultBranch || "main"}]\n\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-node@v4\n        with:\n          node-version: 22\n      - run: npm ci\n      - run: npm run build\n` };
  }
}

function notConfigured(): never {
  throw { statusCode: 400, code: "NOT_CONFIGURED", message: "Configure a GitHub token secret reference before running this operation.", details: {} };
}
