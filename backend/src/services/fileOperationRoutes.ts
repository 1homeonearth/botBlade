import type { IncomingMessage, ServerResponse } from "node:http";
import type { AuditService } from "./auditService.js";
import type { ProjectFileService } from "./projectFiles.js";
import { RequestValidationError } from "./projectStore.js";
import { buildProjectTree, createProjectFile, createProjectFolder, deleteProjectPath, renameProjectPath } from "./projectFileOperations.js";

export interface FileOperationRouteContext {
  method: string;
  projectId: string;
  filePath?: string;
  actorId: string;
  requestId: string;
  fileService: ProjectFileService;
  auditService: AuditService;
  readJson(req: IncomingMessage): Promise<unknown>;
  writeJson(res: ServerResponse, statusCode: number, body: unknown): void;
}

export async function handleFileOperationRoute(req: IncomingMessage, res: ServerResponse, context: FileOperationRouteContext): Promise<boolean> {
  const { method, projectId, filePath, fileService, auditService, actorId, requestId, readJson, writeJson } = context;

  if (method === "GET" && filePath === undefined) {
    const files = await fileService.list(projectId);
    writeJson(res, 200, { files, tree: buildProjectTree(files) });
    return true;
  }

  if (method === "GET" && filePath !== undefined) {
    writeJson(res, 200, await fileService.read(projectId, filePath));
    return true;
  }

  if (method === "PUT" && filePath !== undefined) {
    const body = await readJson(req) as { content?: unknown };
    if (typeof body.content !== "string") throw new RequestValidationError([{ field: "content", message: "File content must be a string." }]);
    writeJson(res, 200, await fileService.write(projectId, filePath, body.content));
    return true;
  }

  if (method === "POST" && filePath === undefined) {
    const created = await createProjectFile(fileService, projectId, await readJson(req));
    const audit = auditService.record({ action: "file.create", actorId, projectId, resourceType: "file", resourceId: created.path, metadata: { path: created.path, size: created.size }, requestId });
    writeJson(res, 201, { ...created, auditEventId: audit.id });
    return true;
  }

  if (method === "PATCH" && filePath === undefined) {
    const renamed = await renameProjectPath(fileService, projectId, await readJson(req));
    const audit = auditService.record({ action: "file.rename", actorId, projectId, resourceType: "file", resourceId: renamed.toPath, metadata: { fromPath: renamed.fromPath, toPath: renamed.toPath }, requestId });
    writeJson(res, 200, { ...renamed, auditEventId: audit.id });
    return true;
  }

  if (method === "DELETE" && filePath === undefined) {
    const body = await readJson(req) as { path?: unknown };
    const deleted = await deleteProjectPath(fileService, projectId, body.path);
    const audit = auditService.record({ action: "file.delete", actorId, projectId, resourceType: "file", resourceId: deleted.path, metadata: { path: deleted.path }, requestId });
    writeJson(res, 200, { ...deleted, auditEventId: audit.id });
    return true;
  }

  return false;
}

export async function handleFolderOperationRoute(req: IncomingMessage, res: ServerResponse, context: Omit<FileOperationRouteContext, "filePath">): Promise<boolean> {
  if (context.method !== "POST") return false;
  const result = await createProjectFolder(context.fileService, context.projectId, await context.readJson(req));
  const audit = context.auditService.record({ action: "file.create", actorId: context.actorId, projectId: context.projectId, resourceType: "folder", resourceId: result.path, metadata: { path: result.path, created: result.created }, requestId: context.requestId });
  context.writeJson(res, 201, { ...result, auditEventId: audit.id });
  return true;
}
