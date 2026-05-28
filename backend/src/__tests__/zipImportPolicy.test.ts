import test from "node:test";
import assert from "node:assert/strict";
import path from "node:path";
import os from "node:os";
import fs from "node:fs/promises";
import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { importZipIntoWorkspace } from "../services/zipImportService.js";

const execFileAsync = promisify(execFile);

async function tempPath(prefix: string): Promise<string> {
  return await fs.mkdtemp(path.join(os.tmpdir(), `${prefix}-`));
}

async function makeZip(zipPath: string, entries: Array<{ name: string; content: string; symlink?: boolean }>): Promise<void> {
  const script = [
    "import os, stat, sys, zipfile",
    "zip_path = sys.argv[1]",
    "entries = [line.split(chr(0), 2) for line in sys.stdin.read().splitlines()]",
    "with zipfile.ZipFile(zip_path, 'w') as z:",
    "    for name, flag, content in entries:",
    "        info = zipfile.ZipInfo(name)",
    "        if flag == 'symlink':",
    "            info.external_attr = (stat.S_IFLNK | 0o777) << 16",
    "        z.writestr(info, content)",
  ].join("\n");
  const stdin = entries.map((entry) => [entry.name, entry.symlink ? "symlink" : "file", entry.content].join("\0")).join("\n");
  await execFileAsync("python3", ["-c", script, zipPath], { input: stdin });
}

test("blocks traversal payload zip deterministically", async () => {
  const dir = await tempPath("zip-import-traversal");
  const zipPath = path.join(dir, "traversal.zip");
  const workspace = path.join(dir, "workspace");
  await fs.mkdir(workspace, { recursive: true });
  try {
    await makeZip(zipPath, [{ name: "../escape.txt", content: "bad" }]);
    const result = await importZipIntoWorkspace(zipPath, workspace);
    assert.equal(result.state, "blocked_by_policy");
    if (result.state === "blocked_by_policy") assert.equal(result.violations.some((v) => v.code === "PATH_TRAVERSAL"), true);
  } finally { await fs.rm(dir, { recursive: true, force: true }); }
});

test("blocks symlink metadata zip deterministically", async () => {
  const dir = await tempPath("zip-import-symlink");
  const zipPath = path.join(dir, "symlink.zip");
  const workspace = path.join(dir, "workspace");
  await fs.mkdir(workspace, { recursive: true });
  try {
    await makeZip(zipPath, [{ name: "project/link", content: "target", symlink: true }]);
    const result = await importZipIntoWorkspace(zipPath, workspace);
    assert.equal(result.state, "blocked_by_policy");
    if (result.state === "blocked_by_policy") assert.equal(result.violations.some((v) => v.code === "SYMLINK_ENTRY"), true);
  } finally { await fs.rm(dir, { recursive: true, force: true }); }
});

test("extracts safe zip into managed workspace", async () => {
  const dir = await tempPath("zip-import-safe");
  const zipPath = path.join(dir, "safe.zip");
  const workspace = path.join(dir, "workspace");
  await fs.mkdir(workspace, { recursive: true });
  try {
    await makeZip(zipPath, [{ name: "project/README.md", content: "ok" }]);
    const result = await importZipIntoWorkspace(zipPath, workspace);
    assert.equal(result.state, "imported");
    const readme = await fs.readFile(path.join(workspace, "project", "README.md"), "utf8");
    assert.equal(readme, "ok");
  } finally { await fs.rm(dir, { recursive: true, force: true }); }
});
