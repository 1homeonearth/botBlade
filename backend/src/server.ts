import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
import crypto from "node:crypto";
import { API_VERSION, SERVICE_NAME, SERVICE_VERSION } from "./models/project.js";
import { BotRuntimeStore } from "./services/botRuntimeStore.js";
import { DuplicateSlugError, parseCreateProjectInput, parseToggleAction, parseUpdateProjectInput, ProjectStore, RequestValidationError } from "./services/projectStore.js";
import { validateProject } from "./services/projectValidation.js";

const projectStore = new ProjectStore();
const runtimeStore = new BotRuntimeStore();
const port = Number(process.env.PORT ?? 8000);

createServer(async (req, res) => {
  const requestId = typeof req.headers["x-request-id"] === "string" ? req.headers["x-request-id"] : crypto.randomUUID();
  res.setHeader("x-request-id", requestId);
  res.setHeader("content-type", "application/json");
  try {
    await handleRequest(req, res);
    console.info(JSON.stringify({ level: "info", requestId, method: req.method, url: req.url }));
  } catch (error) {
    writeError(res, error, requestId);
  }
}).listen(port, () => console.info(JSON.stringify({ level: "info", message: "royalScepter backend listening", port })));

async function handleRequest(req: IncomingMessage, res: ServerResponse): Promise<void> {
  const method = req.method ?? "GET";
  const url = new URL(req.url ?? "/", `http://${req.headers.host ?? "localhost"}`);
  const path = url.pathname;

  if (method === "GET" && path === "/api/health") return writeJson(res, 200, { ok: true, service: SERVICE_NAME, version: SERVICE_VERSION });
  if (method === "GET" && path === "/api/version") return writeJson(res, 200, { name: SERVICE_NAME, version: SERVICE_VERSION, apiVersion: API_VERSION });
  if (method === "GET" && path === "/api/bot-status/") return writeJson(res, 200, runtimeStore.getStatus());
  if (method === "POST" && path === "/api/bot-toggle/") return writeJson(res, 200, runtimeStore.toggle(parseToggleAction(await readJson(req))));
  if (method === "GET" && path === "/api/projects") return writeJson(res, 200, { projects: projectStore.list() });
  if (method === "POST" && path === "/api/projects") return writeJson(res, 201, projectStore.create(parseCreateProjectInput(await readJson(req))));

  const projectMatch = path.match(/^\/api\/projects\/([^/]+)(?:\/(archive|clone|validate))?$/);
  if (projectMatch) {
    const [, projectId, action] = projectMatch;
    if (method === "GET" && !action) return writeJson(res, 200, projectStore.get(projectId) ?? notFound(projectId));
    if (method === "PATCH" && !action) return writeJson(res, 200, projectStore.update(projectId, parseUpdateProjectInput(await readJson(req))) ?? notFound(projectId));
    if (method === "DELETE" && !action) {
      if (!projectStore.delete(projectId)) throw notFound(projectId);
      res.statusCode = 204;
      res.end();
      return;
    }
    if (method === "POST" && action === "archive") return writeJson(res, 200, projectStore.archive(projectId) ?? notFound(projectId));
    if (method === "POST" && action === "clone") return writeJson(res, 201, projectStore.clone(projectId) ?? notFound(projectId));
    if (method === "POST" && action === "validate") {
      const project = projectStore.get(projectId);
      if (!project) throw notFound(projectId);
      return writeJson(res, 200, validateProject(project));
    }
  }
  throw { statusCode: 404, code: "NOT_FOUND", message: "Route not found.", details: {} };
}

async function readJson(req: IncomingMessage): Promise<unknown> {
  const chunks: Buffer[] = [];
  for await (const chunk of req) chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
  const body = Buffer.concat(chunks).toString("utf8").trim();
  if (!body) return {};
  try { return JSON.parse(body) as unknown; } catch { throw new RequestValidationError([{ field: "body", message: "Body must be valid JSON." }]); }
}

function writeJson(res: ServerResponse, statusCode: number, body: unknown): void {
  if (isHttpError(body)) throw body;
  res.statusCode = statusCode;
  res.end(JSON.stringify(body));
}

function writeError(res: ServerResponse, error: unknown, requestId: string): void {
  if (error instanceof RequestValidationError) return writeJson(res, 400, { error: { code: "VALIDATION_FAILED", message: error.message, details: { problems: error.problems }, requestId } });
  if (error instanceof DuplicateSlugError) return writeJson(res, 409, { error: { code: "DUPLICATE_PROJECT_SLUG", message: error.message, details: {}, requestId } });
  const statusCode = isHttpError(error) ? error.statusCode : 500;
  const code = isHttpError(error) ? error.code : "INTERNAL_SERVER_ERROR";
  const message = isHttpError(error) ? error.message : "Unexpected backend error.";
  const details = isHttpError(error) ? error.details : {};
  res.statusCode = statusCode;
  res.end(JSON.stringify({ error: { code, message, details, requestId } }));
}

function notFound(projectId: string): never {
  throw { statusCode: 404, code: "NOT_FOUND", message: `Project '${projectId}' was not found.`, details: {} };
}

function isHttpError(value: unknown): value is { statusCode: number; code: string; message: string; details: unknown } {
  return Boolean(value && typeof value === "object" && "statusCode" in value && "code" in value && "message" in value);
}
