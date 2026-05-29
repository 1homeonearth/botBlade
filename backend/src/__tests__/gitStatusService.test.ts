import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { execFileSync } from "node:child_process";
import { GitStatusService, redactCredentialUrl } from "../services/gitStatusService.js";

async function tempDir(prefix: string): Promise<string> {
  const dir = path.join(os.tmpdir(), `${prefix}-${randomUUID()}`);
  await fs.mkdir(dir, { recursive: true });
  return dir;
}

test("git status service returns clean summary for clean repo", async () => {
  const dir = await tempDir("botblade-git-clean");
  execFileSync("git", ["-C", dir, "init"]);
  await fs.writeFile(path.join(dir, "README.md"), "hello\n", "utf8");
  execFileSync("git", ["-C", dir, "add", "."]);
  execFileSync("git", ["-C", dir, "-c", "user.email=test@example.com", "-c", "user.name=Test", "commit", "-m", "init"]);
  const status = await new GitStatusService().readStatus(dir);
  assert.equal(status.available, true);
  assert.equal(status.clean, true);
  assert.equal(status.dirtyFileCount, 0);
});

test("git status service returns dirty summary for modified repo", async () => {
  const dir = await tempDir("botblade-git-dirty");
  execFileSync("git", ["-C", dir, "init"]);
  await fs.writeFile(path.join(dir, "file.txt"), "v1\n", "utf8");
  execFileSync("git", ["-C", dir, "add", "."]);
  execFileSync("git", ["-C", dir, "-c", "user.email=test@example.com", "-c", "user.name=Test", "commit", "-m", "init"]);
  await fs.writeFile(path.join(dir, "file.txt"), "v2\n", "utf8");
  const status = await new GitStatusService().readStatus(dir);
  assert.equal(status.clean, false);
  assert.ok(status.changedFiles.some((entry) => entry.path === "file.txt"));
});



test("git status static safe mode does not mark all scanned files dirty", async () => {
  const dir = await tempDir("botblade-git-static-clean");
  execFileSync("git", ["-C", dir, "init"]);
  await fs.writeFile(path.join(dir, "README.md"), "hello\n", "utf8");
  execFileSync("git", ["-C", dir, "add", "."]);
  execFileSync("git", ["-C", dir, "-c", "user.email=test@example.com", "-c", "user.name=Test", "commit", "-m", "init"]);

  const status = await new GitStatusService().readStatusStaticSafe(dir, new Set(["README.md", "package.json"]));

  assert.equal(status.available, true);
  assert.equal(status.clean, true);
  assert.equal(status.dirtyFileCount, 0);
  assert.equal(status.changedFiles.length, 0);
  assert.equal(status.note, "static git metadata only; changes not evaluated");
});

test("git status service redacts remote URLs before returning metadata", async () => {
  const dir = await tempDir("botblade-git-remote-redaction");
  execFileSync("git", ["-C", dir, "init"]);
  execFileSync("git", [
    "-C",
    dir,
    "remote",
    "add",
    "origin",
    "https://user:pass@example.com/repo.git?access_token=abc123&expires=1",
  ]);

  const status = await new GitStatusService().readStatus(dir);

  assert.equal(status.remotes.length, 1);
  assert.equal(status.remotes[0]?.name, "origin");
  assert.equal(status.remotes[0]?.url?.includes("user:pass"), false);
  assert.equal(status.remotes[0]?.url?.includes("abc123"), false);
  assert.equal(
    status.remotes[0]?.url,
    "https://[REDACTED]@example.com/repo.git?access_token=[REDACTED]&expires=1",
  );
});

test("git status service safe mode returns unavailable for non-git workspace", async () => {
  const dir = await tempDir("botblade-git-none");
  const status = await new GitStatusService().readStatusSafe(dir);
  assert.equal(status.available, false);
  assert.equal(status.branch, null);
});


test("git status service tolerates unborn HEAD and still reports changed files", async () => {
  const dir = await tempDir("botblade-git-unborn");
  execFileSync("git", ["-C", dir, "init"]);
  await fs.writeFile(path.join(dir, "fresh.txt"), "hello\n", "utf8");
  const status = await new GitStatusService().readStatus(dir);
  assert.equal(status.available, true);
  assert.equal(typeof status.branch, "string");
  assert.equal(status.clean, false);
  assert.ok(status.changedFiles.some((entry) => entry.path === "fresh.txt"));
});


test("git status service does not inherit parent repository metadata", async () => {
  const root = await tempDir("botblade-git-parent");
  execFileSync("git", ["-C", root, "init"]);
  const child = path.join(root, "generated-projects", "project-no-git");
  await fs.mkdir(child, { recursive: true });
  await fs.writeFile(path.join(child, "app.js"), "console.log('x')\n", "utf8");
  const status = await new GitStatusService().readStatusSafe(child);
  assert.equal(status.available, false);
  assert.equal(status.branch, null);
  assert.equal(status.changedFiles.length, 0);
});


test("git status safe mode reports dirty when git status exceeds buffer", async () => {
  const workspacePath = "/tmp/workspace";
  const service = new class extends GitStatusService {
    protected override git(_workspacePath: string, args: string[]): string {
      if (args[0] === "rev-parse" && args[1] === "--show-toplevel") return workspacePath;
      if (args[0] === "status") {
        const error = new Error("stdout maxBuffer length exceeded") as Error & { code?: string };
        error.code = "ENOBUFS";
        throw error;
      }
      return "";
    }
  }();
  const status = await service.readStatusSafe(workspacePath);
  assert.equal(status.available, true);
  assert.equal(status.clean, false);
  assert.equal(status.note, "too many changed files to display");
});


test("redactCredentialUrl redacts sensitive query parameters", () => {
  const input = "https://host/repo.git?access_token=abc123&expires=123&X-Amz-Signature=sig987";
  const output = redactCredentialUrl(input);
  assert.equal(output.includes("abc123"), false);
  assert.equal(output.includes("sig987"), false);
  assert.equal(output.includes("expires=123"), true);
});

test("redactCredentialUrl redacts both userinfo and query credentials", () => {
  const input = "https://user:pass@example.com/repo.git?auth=topsecret";
  const output = redactCredentialUrl(input);
  assert.equal(output.includes("user:pass"), false);
  assert.equal(output.includes("topsecret"), false);
});
