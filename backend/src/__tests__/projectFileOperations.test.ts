import assert from "node:assert/strict";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { buildProjectTree, createProjectFile, createProjectFolder, deleteProjectPath, renameProjectPath } from "../services/projectFileOperations.js";
import { ProjectFileService } from "../services/projectFiles.js";

test("buildProjectTree nests folders before files", () => {
  const tree = buildProjectTree([
    { path: "README.md", size: 1, updatedAt: "now", generated: false, editable: true },
    { path: "src/index.ts", size: 1, updatedAt: "now", generated: false, editable: true },
    { path: "src/commands/ping.ts", size: 1, updatedAt: "now", generated: false, editable: true },
  ]);

  assert.equal(tree[0].name, "src");
  assert.equal(tree[0].type, "directory");
  assert.equal(tree[1].name, "README.md");
  assert.equal(tree[0].children?.some((node) => node.path === "src/commands"), true);
});

test("project file operations create folders, create files, rename paths, and delete paths", async () => {
  const root = mkdtempSync(path.join(tmpdir(), "botblade-files-"));
  const service = new ProjectFileService(root);
  const projectId = "project_ops";

  const folder = await createProjectFolder(service, projectId, { path: "src/commands" });
  assert.equal(folder.path, "src/commands");
  assert.equal(folder.created, true);

  const created = await createProjectFile(service, projectId, { path: "src/commands/ping.ts", content: "export const ping = true;" });
  assert.equal(created.path, "src/commands/ping.ts");

  const renamed = await renameProjectPath(service, projectId, { fromPath: "src/commands/ping.ts", toPath: "src/commands/pong.ts" });
  assert.equal(renamed.moved, true);
  assert.equal((await service.read(projectId, "src/commands/pong.ts")).content, "export const ping = true;");

  const deleted = await deleteProjectPath(service, projectId, "src/commands/pong.ts");
  assert.equal(deleted.deleted, true);
  await assert.rejects(() => service.read(projectId, "src/commands/pong.ts"), /Project file was not found/);
});

test("project file operations reject traversal outside workspace", async () => {
  const root = mkdtempSync(path.join(tmpdir(), "botblade-files-"));
  const service = new ProjectFileService(root);

  await assert.rejects(() => createProjectFile(service, "project_safe", { path: "../escape.ts", content: "bad" }), /File path must stay within/);
  await assert.rejects(() => renameProjectPath(service, "project_safe", { fromPath: "missing.ts", toPath: "../../escape.ts" }), /File path must stay within|Project path was not found/);
});
