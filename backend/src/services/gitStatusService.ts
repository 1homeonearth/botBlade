import { execFileSync } from "node:child_process";

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
    const branch = this.currentBranch(workspacePath);
    const remotes = this.remotes(workspacePath);
    const changedFiles = this.changedFiles(workspacePath);
    return { available: true, branch, remotes, clean: changedFiles.length === 0, dirtyFileCount: changedFiles.length, changedFiles };
  }
  async readStatusSafe(workspacePath: string): Promise<GitStatusSummary> {
    try { return await this.readStatus(workspacePath); } catch {
      return { available: false, branch: null, remotes: [], clean: true, dirtyFileCount: 0, changedFiles: [], note: "git metadata unavailable" };
    }
  }
  private currentBranch(workspacePath: string): string | null {
    const value = execFileSync("git", ["-C", workspacePath, "rev-parse", "--abbrev-ref", "HEAD"], { encoding: "utf8" }).trim();
    return value && value !== "HEAD" ? value : null;
  }
  private remotes(workspacePath: string): GitStatusRemote[] {
    const stdout = execFileSync("git", ["-C", workspacePath, "remote", "-v"], { encoding: "utf8" });
    const map = new Map<string, string>();
    for (const line of stdout.split(/\r?\n/)) {
      const match = line.match(/^(\S+)\s+(\S+)\s+\((fetch|push)\)$/);
      if (match && !map.has(match[1])) map.set(match[1], redactCredentialUrl(match[2]));
    }
    return [...map.entries()].map(([name, url]) => ({ name, url }));
  }
  private changedFiles(workspacePath: string): GitChangedFileSummary[] {
    const stdout = execFileSync("git", ["-C", workspacePath, "status", "--porcelain"], { encoding: "utf8" });
    return stdout.split(/\r?\n/).filter(Boolean).map((line: string) => ({ status: line.slice(0, 2).trim() || "?", path: line.slice(3).trim() }));
  }
}

export function redactCredentialUrl(url: string): string {
  return url.replace(/:\/\/([^/@\s]+)@/g, "://[REDACTED]@");
}
