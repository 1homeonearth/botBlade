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

test("buildProjectTree preserves literal percent signs in filenames", () => {
  const tree = buildProjectTree([{ path: "docs/100%.md", size: 1, updatedAt: "now", generated: false, editable: true }]);

  assert.equal(tree[0].name, "docs");
  assert.equal(tree[0].children?.[0].name, "100%.md");
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
  const readFailure = await captureFailure(() => service.read(projectId, "src/commands/pong.ts"));
  assert.equal(readFailure.code, "FILE_NOT_FOUND");
});

test("project file operations preserve percent text in JSON body paths", async () => {
  const root = mkdtempSync(path.join(tmpdir(), "botblade-files-"));
  const service = new ProjectFileService(root);
  const projectId = "project_percent";

  const literalPercent = await createProjectFile(service, projectId, { path: "docs/100%.md", content: "percent" });
  assert.equal(literalPercent.path, "docs/100%.md");
  assert.equal((await service.read(projectId, "docs/100%25.md")).content, "percent");

  const escapeText = ["a", "2Fb.md"].join("%");
  const escapeApiText = ["a", "252Fb.md"].join("%");
  const escapeLooking = await createProjectFile(service, projectId, { path: `docs/${escapeText}`, content: "encoded text" });
  assert.equal(escapeLooking.path, `docs/${escapeText}`);
  assert.equal((await service.read(projectId, `docs/${escapeApiText}`)).content, "encoded text");
});

test("project file operations reject workspace-root and destructive rename cases", async () => {
  const root = mkdtempSync(path.join(tmpdir(), "botblade-files-"));
  const service = new ProjectFileService(root);
  const projectId = "project_guard";

  await createProjectFolder(service, projectId, { path: "src" });
  await createProjectFile(service, projectId, { path: "src/index.ts", content: "index" });
  await createProjectFile(service, projectId, { path: "src/target.ts", content: "target" });

  const rootDelete = await captureFailure(() => deleteProjectPath(service, projectId, "."));
  assert.equal(rootDelete.code, "INVALID_FILE_PATH");

  const samePath = await captureFailure(() => renameProjectPath(service, projectId, { fromPath: "src/index.ts", toPath: "src/index.ts" }));
  assert.equal(samePath.code, "SAME_PATH");

  const descendant = await captureFailure(() => renameProjectPath(service, projectId, { fromPath: "src", toPath: "src/nested" }));
  assert.equal(descendant.code, "DESCENDANT_TARGET");

  const targetExists = await captureFailure(() => renameProjectPath(service, projectId, { fromPath: "src/index.ts", toPath: "src/target.ts", overwrite: true }));
  assert.equal(targetExists.code, "TARGET_EXISTS");
});

test("project file operations reject file and directory target conflicts predictably", async () => {
  const root = mkdtempSync(path.join(tmpdir(), "botblade-files-"));
  const service = new ProjectFileService(root);
  const projectId = "project_conflicts";

  await createProjectFile(service, projectId, { path: "README.md", content: "readme" });
  const folderAtFile = await captureFailure(() => createProjectFolder(service, projectId, { path: "README.md" }));
  assert.equal(folderAtFile.code, "TARGET_IS_FILE");

  await createProjectFolder(service, projectId, { path: "src" });
  const fileAtFolder = await captureFailure(() => createProjectFile(service, projectId, { path: "src", content: "bad", overwrite: true }));
  assert.equal(fileAtFolder.code, "TARGET_IS_DIRECTORY");
});

async function captureFailure(action: () => Promise<unknown>): Promise<{ code?: string; message?: string }> {
  try {
    await action();
  } catch (error) {
    return error as { code?: string; message?: string };
  }
  throw new Error("Expected action to fail.");
}
