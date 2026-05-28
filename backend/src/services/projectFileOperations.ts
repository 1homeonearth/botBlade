import fs from "node:fs/promises";
import path from "node:path";
import type { ProjectFileSummary, ProjectFileService } from "./projectFiles.js";
import { parseFileWriteInput } from "./projectFiles.js";
import { RequestValidationError } from "./projectStore.js";

export interface ProjectTreeNode {
  name: string;
  path: string;
  type: "file" | "directory";
  children?: ProjectTreeNode[];
  file?: ProjectFileSummary;
}

export interface FileCreateInput {
  path: string;
  content: string;
  overwrite: boolean;
}

export interface FolderCreateInput {
  path: string;
}

export interface FileRenameInput {
  fromPath: string;
  toPath: string;
  overwrite: boolean;
}

export function buildProjectTree(files: ProjectFileSummary[]): ProjectTreeNode[] {
  const roots: ProjectTreeNode[] = [];
  for (const file of files) {
    const segments = normalizeSummaryPath(file.path).split("/").filter(Boolean);
    let children = roots;
    let currentPath = "";
    for (const [index, segment] of segments.entries()) {
      currentPath = currentPath ? `${currentPath}/${segment}` : segment;
      const isFile = index === segments.length - 1;
      const existingIndex = children.findIndex((child) => child.name === segment);
      const existing = existingIndex >= 0 ? children[existingIndex] : undefined;
      if (isFile) {
        const node: ProjectTreeNode = { name: segment, path: currentPath, type: "file", file };
        if (existingIndex >= 0) children[existingIndex] = node;
        else children.push(node);
        continue;
      }
      const directory: ProjectTreeNode = existing?.type === "directory"
        ? existing
        : { name: segment, path: currentPath, type: "directory", children: [] };
      if (existingIndex < 0) children.push(directory);
      else if (existing?.type !== "directory") children[existingIndex] = directory;
      children = directory.children ?? (directory.children = []);
    }
  }
  return sortNodes(roots);
}

export async function createProjectFile(fileService: ProjectFileService, projectId: string, input: unknown): Promise<ProjectFileSummary> {
  const object = asObject(input);
  const relativePath = normalizeInputPath(object.path, "path");
  const { content } = parseFileWriteInput(object);
  const overwrite = object.overwrite === true;
  const target = fileService.safePath(projectId, relativePath);
  assertNotWorkspaceRoot(fileService, projectId, target, relativePath);
  if (!overwrite && await exists(target)) throw conflict("FILE_EXISTS", "File already exists.", relativePath);
  await fs.mkdir(path.dirname(target), { recursive: true });
  await fs.writeFile(target, content, "utf8");
  return fileService.read(projectId, relativePath);
}

export async function createProjectFolder(fileService: ProjectFileService, projectId: string, input: unknown): Promise<{ path: string; created: boolean }> {
  const relativePath = normalizeInputPath(asObject(input).path, "path");
  const target = fileService.safePath(projectId, relativePath);
  assertNotWorkspaceRoot(fileService, projectId, target, relativePath);
  const existed = await exists(target);
  await fs.mkdir(target, { recursive: true });
  return { path: relativePath, created: !existed };
}

export async function renameProjectPath(fileService: ProjectFileService, projectId: string, input: unknown): Promise<{ fromPath: string; toPath: string; moved: true }> {
  const object = asObject(input);
  const fromPath = normalizeInputPath(object.fromPath, "fromPath");
  const toPath = normalizeInputPath(object.toPath, "toPath");
  const from = fileService.safePath(projectId, fromPath);
  const to = fileService.safePath(projectId, toPath);
  assertNotWorkspaceRoot(fileService, projectId, from, fromPath);
  assertNotWorkspaceRoot(fileService, projectId, to, toPath);
  if (from === to) throw conflict("SAME_PATH", "Source and target paths must be different.", toPath);
  if (to.startsWith(from + path.sep)) throw conflict("DESCENDANT_TARGET", "A path cannot be moved into its own descendant.", toPath);
  if (!await exists(from)) throw notFound(fromPath);
  if (await exists(to)) throw conflict("TARGET_EXISTS", "Target path already exists.", toPath);
  await fs.mkdir(path.dirname(to), { recursive: true });
  await fs.rename(from, to);
  return { fromPath, toPath, moved: true };
}

export async function deleteProjectPath(fileService: ProjectFileService, projectId: string, relativePathValue: unknown): Promise<{ path: string; deleted: true }> {
  const relativePath = normalizeInputPath(relativePathValue, "path");
  const target = fileService.safePath(projectId, relativePath);
  assertNotWorkspaceRoot(fileService, projectId, target, relativePath);
  const stat = await fs.stat(target).catch(() => undefined);
  if (!stat) throw notFound(relativePath);
  if (!stat.isFile()) throw conflict("DIRECTORY_DELETE_UNSUPPORTED", "Directory deletion is not supported by this editor operation.", relativePath);
  await fs.rm(target);
  return { path: relativePath, deleted: true };
}

function sortNodes(nodes: ProjectTreeNode[]): ProjectTreeNode[] {
  return nodes.sort((a, b) => {
    if (a.type !== b.type) return a.type === "directory" ? -1 : 1;
    return a.name.localeCompare(b.name);
  }).map((node) => node.children ? { ...node, children: sortNodes(node.children) } : node);
}

function asObject(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new RequestValidationError([{ field: "body", message: "Request body must be an object." }]);
  return value as Record<string, unknown>;
}

function normalizeInputPath(value: unknown, field: string): string {
  if (typeof value !== "string" || !value.trim()) throw new RequestValidationError([{ field, message: "Path is required." }]);
  const normalized = maybeDecodePath(value).replace(/\\/g, "/").replace(/^\/+/, "").replace(/\/+$/, "");
  if (!normalized || normalized === "." || normalized.includes("\0")) throw new RequestValidationError([{ field, message: "Path is invalid." }]);
  return normalized;
}

function maybeDecodePath(value: string): string {
  const trimmed = value.trim();
  try {
    return decodeURIComponent(trimmed);
  } catch {
    return trimmed;
  }
}

function normalizeSummaryPath(value: string): string {
  return value.replace(/\\/g, "/").replace(/^\/+/, "").replace(/\/+$/, "");
}

function assertNotWorkspaceRoot(fileService: ProjectFileService, projectId: string, target: string, originalPath: string): void {
  const workspace = fileService.resolveWorkspace(projectId).workspace;
  if (path.resolve(target) === path.resolve(workspace)) throw { statusCode: 400, code: "INVALID_FILE_PATH", message: "Path must reference a file or folder inside the project workspace.", details: { path: originalPath } };
}

async function exists(filePath: string): Promise<boolean> {
  return fs.access(filePath).then(() => true, () => false);
}

function notFound(relativePath: string): never {
  throw { statusCode: 404, code: "FILE_NOT_FOUND", message: "Project path was not found.", details: { path: relativePath } };
}

function conflict(code: string, message: string, relativePath: string): never {
  throw { statusCode: 409, code, message, details: { path: relativePath } };
}
