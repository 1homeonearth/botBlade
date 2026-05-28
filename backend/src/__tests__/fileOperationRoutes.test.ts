import assert from "node:assert/strict";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import type { AuditService } from "../services/auditService.js";
import { handleFileOperationRoute, handleFolderOperationRoute } from "../services/fileOperationRoutes.js";
import { ProjectFileService } from "../services/projectFiles.js";

interface TestResponse {
  statusCode: number;
  body: unknown;
}

function response(): TestResponse {
  return { statusCode: 0, body: null };
}

function writeJson(res: TestResponse, statusCode: number, body: unknown): void {
  res.statusCode = statusCode;
  res.body = body;
}

function request(body: unknown = {}): never {
  return { body } as never;
}

function readJson(req: { body: unknown }): Promise<unknown> {
  return Promise.resolve(req.body);
}

function auditService(): AuditService {
  return {
    record(input: { action: string }) {
      return { ...input, id: `audit_${input.action}` };
    },
  } as unknown as AuditService;
}

function context(method: string, projectId: string, fileService: ProjectFileService, filePath?: string) {
  return {
    method,
    projectId,
    filePath,
    actorId: "test_actor",
    requestId: "test_request",
    fileService,
    auditService: auditService(),
    readJson: readJson as never,
    writeJson: writeJson as never,
  };
}

test("file operation route helper creates folders, files, trees, renames, and deletes", async () => {
  const root = mkdtempSync(path.join(tmpdir(), "botblade-route-files-"));
  const fileService = new ProjectFileService(root);
  const projectId = "project_route";

  const folderResponse = response();
  const folderHandled = await handleFolderOperationRoute(request({ path: "src/commands" }), folderResponse as never, context("POST", projectId, fileService));
  assert.equal(folderHandled, true);
  assert.equal(folderResponse.statusCode, 201);

  const createResponse = response();
  const createHandled = await handleFileOperationRoute(request({ path: "src/commands/ping.ts", content: "export const ping = true;" }), createResponse as never, context("POST", projectId, fileService));
  assert.equal(createHandled, true);
  assert.equal(createResponse.statusCode, 201);

  const treeResponse = response();
  await handleFileOperationRoute(request(), treeResponse as never, context("GET", projectId, fileService));
  assert.equal(treeResponse.statusCode, 200);
  const treeBody = treeResponse.body as { tree: Array<{ path: string; children?: Array<{ path: string }> }> };
  assert.equal(treeBody.tree[0].path, "src");
  assert.equal(treeBody.tree[0].children?.[0].path, "src/commands");

  const renameResponse = response();
  await handleFileOperationRoute(request({ fromPath: "src/commands/ping.ts", toPath: "src/commands/pong.ts" }), renameResponse as never, context("PATCH", projectId, fileService));
  assert.equal(renameResponse.statusCode, 200);
  assert.equal((await fileService.read(projectId, "src/commands/pong.ts")).content, "export const ping = true;");

  const deleteResponse = response();
  await handleFileOperationRoute(request({ path: "src/commands/pong.ts" }), deleteResponse as never, context("DELETE", projectId, fileService));
  assert.equal(deleteResponse.statusCode, 200);
  await assert.rejects(() => fileService.read(projectId, "src/commands/pong.ts"));
});

test("file operation route helper preserves editor write size limit", async () => {
  const root = mkdtempSync(path.join(tmpdir(), "botblade-route-files-"));
  const fileService = new ProjectFileService(root);
  const projectId = "project_limit";
  await handleFileOperationRoute(request({ path: "README.md", content: "ok" }), response() as never, context("POST", projectId, fileService));

  await assert.rejects(
    () => handleFileOperationRoute(request({ content: "x".repeat((512 * 1024) + 1) }), response() as never, context("PUT", projectId, fileService, "README.md")),
    /512KB/,
  );
});

test("file operation route helper returns false for unsupported methods", async () => {
  const root = mkdtempSync(path.join(tmpdir(), "botblade-route-files-"));
  const fileService = new ProjectFileService(root);
  const handled = await handleFileOperationRoute(request(), response() as never, context("OPTIONS", "project_route", fileService));
  assert.equal(handled, false);
});
