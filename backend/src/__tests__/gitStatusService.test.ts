import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { execFileSync } from "node:child_process";
import { GitStatusService } from "../services/gitStatusService.js";

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
  assert.equal(status.branch, null);
  assert.equal(status.clean, false);
  assert.ok(status.changedFiles.some((entry) => entry.path === "fresh.txt"));
});
