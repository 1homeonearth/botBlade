import { execFileSync } from "node:child_process";
import path from "node:path";

export interface GitStatusRemote { name: string; url: string | null }
export interface GitChangedFileSummary { path: string; status: string }
export interface GitStatusSummary {
  available: boolean;
  branch: string | null;
  remotes: GitStatusRemote[];
  clean: boolean;
  dirtyFileCount: number;
  changedFiles: GitChangedFileSummary[];
  note?: string;
}

export class GitStatusService {
  async readStatus(workspacePath: string): Promise<GitStatusSummary> {
    this.assertWorkspaceIsRepositoryRoot(workspacePath);
    const branch = this.currentBranch(workspacePath);
    const remotes = this.remotes(workspacePath);
    const changedFiles = this.changedFiles(workspacePath);
    return { available: true, branch, remotes, clean: changedFiles.length === 0, dirtyFileCount: changedFiles.length, changedFiles };
  }

  async readStatusSafe(workspacePath: string): Promise<GitStatusSummary> {
    try {
      return await this.readStatus(workspacePath);
    } catch {
      return { available: false, branch: null, remotes: [], clean: true, dirtyFileCount: 0, changedFiles: [], note: "git metadata unavailable" };
    }
  }

  private assertWorkspaceIsRepositoryRoot(workspacePath: string): void {
    const topLevel = this.git(workspacePath, ["rev-parse", "--show-toplevel"]).trim();
    const workspaceResolved = path.resolve(workspacePath);
    const topLevelResolved = path.resolve(topLevel);
    if (workspaceResolved !== topLevelResolved) throw new Error("Workspace is not a Git repository root.");
  }

  private currentBranch(workspacePath: string): string | null {
    try {
      const value = this.git(workspacePath, ["rev-parse", "--abbrev-ref", "HEAD"]).trim();
      return value && value !== "HEAD" ? value : null;
    } catch {
      return null;
    }
  }

  private remotes(workspacePath: string): GitStatusRemote[] {
    const stdout = this.git(workspacePath, ["remote", "-v"]);
    const map = new Map<string, string>();
    for (const line of stdout.split(/\r?\n/)) {
      const match = line.match(/^(\S+)\s+(\S+)\s+\((fetch|push)\)$/);
      if (match && !map.has(match[1])) map.set(match[1], redactCredentialUrl(match[2]));
    }
    return [...map.entries()].map(([name, url]) => ({ name, url }));
  }

  private changedFiles(workspacePath: string): GitChangedFileSummary[] {
    const stdout = this.git(workspacePath, ["status", "--porcelain"]);
    return stdout.split(/\r?\n/).filter(Boolean).map((line: string) => ({ status: line.slice(0, 2).trim() || "?", path: line.slice(3).trim() }));
  }

  private git(workspacePath: string, args: string[]): string {
    return execFileSync("git", [
      "-C", workspacePath,
      "-c", "core.fsmonitor=false",
      "-c", "core.hooksPath=/dev/null",
      ...args,
    ], { encoding: "utf8" });
  }
}

export function redactCredentialUrl(url: string): string {
  return url.replace(/:\/\/([^/@\s]+)@/g, "://[REDACTED]@");
}
