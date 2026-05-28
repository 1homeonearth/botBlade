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
    const segments = normalizeInputPath(file.path, "path").split("/").filter(Boolean);
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
  if (!overwrite && await exists(target)) throw conflict("FILE_EXISTS", "File already exists.", relativePath);
  await fs.mkdir(path.dirname(target), { recursive: true });
  await fs.writeFile(target, content, "utf8");
  return fileService.read(projectId, relativePath);
}

export async function createProjectFolder(fileService: ProjectFileService, projectId: string, input: unknown): Promise<{ path: string; created: boolean }> {
  const relativePath = normalizeInputPath(asObject(input).path, "path");
  const target = fileService.safePath(projectId, relativePath);
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
  if (!await exists(from)) throw notFound(fromPath);
  if (await exists(to) && object.overwrite !== true) throw conflict("TARGET_EXISTS", "Target path already exists.", toPath);
  await fs.mkdir(path.dirname(to), { recursive: true });
  if (object.overwrite === true) await fs.rm(to, { recursive: true, force: true });
  await fs.rename(from, to);
  return { fromPath, toPath, moved: true };
}

export async function deleteProjectPath(fileService: ProjectFileService, projectId: string, relativePathValue: unknown): Promise<{ path: string; deleted: true }> {
  const relativePath = normalizeInputPath(relativePathValue, "path");
  const target = fileService.safePath(projectId, relativePath);
  if (!await exists(target)) throw notFound(relativePath);
  await fs.rm(target, { recursive: true, force: true });
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
  const normalized = decodeURIComponent(value).replace(/\\/g, "/").replace(/^\/+/, "").replace(/\/+$/, "");
  if (!normalized || normalized === "." || normalized.includes("\0")) throw new RequestValidationError([{ field, message: "Path is invalid." }]);
  return normalized;
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
