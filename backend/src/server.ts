import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
type Socket = any;
import crypto from "node:crypto";
import pathModule from "node:path";
import { API_VERSION, SERVICE_NAME, SERVICE_VERSION } from "./models/project.js";
import { AuditService } from "./services/auditService.js";
import { assertExecutionAccess, assertGlobalAccess, assertProjectAccess, authenticateRequest, canAccessProject, hasGlobalProjectAccess } from "./services/authService.js";
import { BuildService } from "./services/buildService.js";
import { parseCommandDefinition, parseCommandPatch, validateCommands } from "./services/commandDefinitions.js";
import { DeploymentJobStore } from "./services/deploymentJobs.js";
import { DeploymentTargetStore, deploymentTargetWithCapabilities, testDeploymentTarget } from "./services/deploymentTargets.js";
import { GitHubIntegrationService } from "./services/githubIntegration.js";
import { GitStatusService, redactCredentialUrl } from "./services/gitStatusService.js";
import { LocalProcessRuntimeService } from "./services/localProcessRuntimeService.js";
import { ProjectFileService, parseFileWriteInput } from "./services/projectFiles.js";
import { DuplicateSlugError, parseCreateProjectInput, parseToggleAction, parseUpdateProjectInput, ProjectStore, RequestValidationError } from "./services/projectStore.js";
import { redactSecrets } from "./services/redaction.js";
import { parseCreateSecretInput, parseRotateSecretInput, parseUpdateSecretInput, SecretStore } from "./services/secretStore.js";
import { validateProject } from "./services/projectValidation.js";
import { scanAndGenerateBotbladeMetadata } from "./services/importScan/index.js";
import { ImportStore } from "./services/imports/index.js";
import { SqlitePersistence } from "./persistence/sqlitePersistence.js";

const persistence = createPersistence();
const projectStore = new ProjectStore(persistence);
const secretStore = new SecretStore(persistence);
const fileService = new ProjectFileService();
const auditService = new AuditService(persistence);
const auditFromService = (event: { action: Parameters<AuditService["record"]>[0]["action"]; projectId: string; resourceType: string; resourceId: string; metadata: Record<string, unknown>; requestId: string; actorId?: string }) => auditService.record(event);
const buildService = new BuildService(fileService, (secretId) => secretStore.has(secretId), auditFromService, undefined, persistence);
const runtimeService = new LocalProcessRuntimeService(fileService, (secretId) => secretStore.getValue(secretId));
const targetStore = new DeploymentTargetStore(persistence);
const deploymentStore = new DeploymentJobStore(buildService, targetStore, runtimeService, auditFromService, persistence, (secretId) => {
  const summary = secretStore.get(secretId);
  const value = secretStore.getValue(secretId);
  return summary && value !== undefined ? { id: summary.id, name: summary.name, value } : undefined;
});
const githubService = new GitHubIntegrationService((secretId) => secretStore.has(secretId), (secretId) => secretStore.getValue(secretId), (input) => auditService.record(input));
const importStore = new ImportStore(persistence);
const gitStatusService = new GitStatusService();
const defaultHost = "127.0.0.1";
const host = (process.env.BIND_HOST ?? process.env.HOST ?? defaultHost).trim() || defaultHost;
const portValue = process.env.PORT ?? "8000";
const port = Number.parseInt(portValue, 10);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error(`Invalid PORT value: ${portValue}`);

function createPersistence(): SqlitePersistence | undefined {
  if (process.env.NODE_ENV === "test" && !process.env.BOTBLADE_DATABASE_URL && !process.env.DATABASE_URL) return undefined;
  try {
    return SqlitePersistence.fromUrl();
  } catch (error) {
    if (error instanceof Error) throw new Error(`Persistence startup failed: ${error.message}`);
    throw error;
  }
}

export function createRequestListener() {
  return async (req: IncomingMessage, res: ServerResponse) => {
    const requestId = typeof req.headers["x-request-id"] === "string" ? req.headers["x-request-id"] : crypto.randomUUID();
    res.setHeader("x-request-id", requestId);
    res.setHeader("content-type", "application/json");
    try {
      await handleRequest(req, res, requestId);
      console.info(redactSecrets(JSON.stringify({ level: "info", requestId, method: req.method, url: req.url })));
    } catch (error) {
      console.error(redactSecrets(JSON.stringify({ level: "error", requestId, method: req.method, url: req.url, message: error instanceof Error ? error.message : "Request failed" })));
      writeError(res, error, requestId);
    }
  };
}

const logClients = new Set<Socket>();

if (process.env.NODE_ENV !== "test") {
  const server = createServer(createRequestListener()) as any;
  server.on("upgrade", (req: IncomingMessage, socket: Socket) => handleLogsUpgrade(req, socket));
  server.listen(port, host, () => console.info(JSON.stringify({ level: "info", message: "botBlade backend listening", host, port })));
}

async function handleRequest(req: IncomingMessage, res: ServerResponse, requestId: string): Promise<void> {
  const method = req.method ?? "GET";
  const path = extractPathname(req.url);

  if (method === "GET" && path === "/api/health") return writeJson(res, 200, { ok: true, service: SERVICE_NAME, version: SERVICE_VERSION, persistence: persistence ? "sqlite" : "memory" });
  if (method === "GET" && path === "/api/version") return writeJson(res, 200, { name: SERVICE_NAME, version: SERVICE_VERSION, apiVersion: API_VERSION });
  if (method === "GET" && path === "/api/diagnostics/startup-crash") {
    const artifactPath = process.env.BOTBLADE_STARTUP_CRASH_ARTIFACT;
    if (!artifactPath) return writeJson(res, 200, { artifact: null, source: "unconfigured" });
    const fs = await import("node:fs/promises");
    try {
      const artifact = await fs.readFile(artifactPath, "utf8");
      return writeJson(res, 200, { artifact, source: "file" });
    } catch {
      return writeJson(res, 200, { artifact: null, source: "missing" });
    }
  }

  const actor = authenticateRequest(req);

  if (method === "GET" && path === "/api/persistence/status") {
    assertGlobalAccess(actor, "persistence status");
    return writeJson(res, 200, persistence ? persistence.diagnostics() : { adapter: "memory", durable: false });
  }

  if (method === "GET" && path === "/api/audit-events") {
    assertGlobalAccess(actor, "audit events");
    return writeJson(res, 200, { auditEvents: auditService.list() });
  }

  if (method === "GET" && path === "/api/github/status") {
    assertGlobalAccess(actor, "GitHub integration");
    return writeJson(res, 200, githubService.status());
  }
  if (method === "POST" && path === "/api/github/connect") {
    assertGlobalAccess(actor, "GitHub integration");
    return writeJson(res, 200, githubService.connect(await readJson(req)));
  }

  if (method === "GET" && path === "/api/deployment-targets") {
    assertGlobalAccess(actor, "deployment targets");
    return writeJson(res, 200, { targets: targetStore.list().map(deploymentTargetWithCapabilities) });
  }
  if (method === "POST" && path === "/api/deployment-targets") {
    assertGlobalAccess(actor, "deployment targets");
    return writeJson(res, 201, deploymentTargetWithCapabilities(targetStore.create(await readJson(req))));
  }
  const targetMatch = path.match(/^\/api\/deployment-targets\/([^/]+)(?:\/(test))?$/);
  if (targetMatch) {
    const [, targetId, action] = targetMatch;
    assertGlobalAccess(actor, "deployment targets");
    const target = targetStore.get(targetId);
    if (!target) throw notFoundTarget(targetId);
    if (method === "GET" && !action) return writeJson(res, 200, deploymentTargetWithCapabilities(target));
    if (method === "PATCH" && !action) return writeJson(res, 200, deploymentTargetWithCapabilities(targetStore.update(targetId, await readJson(req)) ?? notFoundTarget(targetId)));
    if (method === "DELETE" && !action) {
      if (!targetStore.delete(targetId)) throw notFoundTarget(targetId);
      res.statusCode = 204;
      res.end();
      return;
    }
    if (method === "POST" && action === "test") return writeJson(res, 200, await testDeploymentTarget(target));
  }

  if (method === "GET" && path === "/api/bot-status/") {
    const project = projectStore.list()[0];
    if (project) assertProjectAccess(actor, project.id);
    return writeJson(res, 200, project ? runtimeService.getStatus(project.id) : { projectId: "default", status: "stopped", running: false, pid: null, startedAt: null, lastExitCode: null, message: "No active/default project exists." });
  }
  if (method === "POST" && path === "/api/bot-toggle/") {
    const project = projectStore.list()[0];
    if (!project) throw { statusCode: 400, code: "NO_ACTIVE_PROJECT", message: "Create or select a project before toggling the bot runtime.", details: {} };
    assertProjectAccess(actor, project.id);
    const action = parseToggleAction(await readJson(req));
    return writeJson(res, 200, action === "start" ? await runtimeService.start(project) : await runtimeService.stop(project.id));
  }

  if (method === "GET" && path === "/api/secrets") return writeJson(res, 200, { secrets: secretStore.list().filter((secret) => canAccessProject(actor, secret.projectId)) });
  if (method === "POST" && path === "/api/secrets") {
    const input = parseCreateSecretInput(await readJson(req));
    assertProjectAccess(actor, input.projectId);
    const secret = secretStore.create(input);
    const audit = auditService.record({ action: "secret.create", actorId: actor.id, projectId: secret.projectId, resourceType: "secret", resourceId: secret.id, metadata: { name: secret.name, type: secret.type, storageMode: secret.storageMode, fingerprint: secret.fingerprint }, requestId });
    return writeJson(res, 201, { ...secret, auditEventId: audit.id });
  }
  const secretMatch = path.match(/^\/api\/secrets\/([^/]+)(?:\/(rotate))?$/);
  if (secretMatch) {
    const [, secretId, action] = secretMatch;
    const existingSecret = secretStore.get(secretId);
    if (!existingSecret) throw notFoundSecret(secretId);
    assertProjectAccess(actor, existingSecret.projectId);
    if (method === "GET" && !action) return writeJson(res, 200, existingSecret);
    if (method === "PATCH" && !action) {
      const input = parseUpdateSecretInput(await readJson(req));
      if (input.projectId !== undefined) assertProjectAccess(actor, input.projectId);
      return writeJson(res, 200, secretStore.update(secretId, input) ?? notFoundSecret(secretId));
    }
    if (method === "DELETE" && !action) {
      const existing = existingSecret;
      if (!secretStore.delete(secretId)) throw notFoundSecret(secretId);
      auditService.record({ action: "secret.delete", actorId: actor.id, projectId: existing?.projectId ?? null, resourceType: "secret", resourceId: secretId, metadata: { name: existing?.name, type: existing?.type }, requestId });
      res.statusCode = 204;
      res.end();
      return;
    }
    if (method === "POST" && action === "rotate") {
      const secret = secretStore.rotate(secretId, parseRotateSecretInput(await readJson(req))) ?? notFoundSecret(secretId);
      const audit = auditService.record({ action: "secret.rotate", actorId: actor.id, projectId: secret.projectId, resourceType: "secret", resourceId: secret.id, metadata: { name: secret.name, type: secret.type, fingerprint: secret.fingerprint }, requestId });
      return writeJson(res, 200, { ...secret, auditEventId: audit.id });
    }
  }

  if (method === "GET" && path === "/api/projects") return writeJson(res, 200, { projects: hasGlobalProjectAccess(actor) ? projectStore.list() : projectStore.list().filter((project) => canAccessProject(actor, project.id)) });
  if (method === "POST" && path === "/api/projects") {
    const input = parseCreateProjectInput(await readJson(req));
    if (input.discord?.tokenSecretRef && !secretStore.has(input.discord.tokenSecretRef)) throw new RequestValidationError([{ field: "discord.tokenSecretRef", message: "Secret reference does not exist." }]);
    const project = projectStore.create(input);
    const audit = auditService.record({ action: "project.create", actorId: actor.id, projectId: project.id, resourceType: "project", resourceId: project.id, metadata: { name: project.name, slug: project.slug, templateId: project.templateId }, requestId });
    return writeJson(res, 201, { ...project, auditEventId: audit.id });
  }

  const auditMatch = path.match(/^\/api\/projects\/([^/]+)\/audit-events$/);
  if (auditMatch) {
    const [, projectId] = auditMatch;
    assertProjectAccess(actor, projectId);
    if (!projectStore.get(projectId)) throw notFoundProject(projectId);
    if (method === "GET") return writeJson(res, 200, { auditEvents: auditService.list(projectId) });
  }

  const fileMatch = path.match(/^\/api\/projects\/([^/]+)\/files(?:\/(.*))?$/);
  if (fileMatch) {
    const [, projectId, filePath] = fileMatch;
    assertProjectAccess(actor, projectId);
    const project = projectStore.get(projectId);
    if (!project) throw notFoundProject(projectId);
    if (method === "GET" && filePath === undefined) return writeJson(res, 200, { files: await fileService.list(projectId) });
    if (method === "GET" && filePath !== undefined) return writeJson(res, 200, await fileService.read(projectId, filePath));
    if (method === "PUT" && filePath !== undefined) { assertExecutionAccess(actor, "project file writes"); return writeJson(res, 200, await fileService.write(projectId, filePath, parseFileWriteInput(await readJson(req)).content)); }
  }

  const buildMatch = path.match(/^\/api\/projects\/([^/]+)\/builds(?:\/([^/]+)(?:\/(logs))?)?$/);
  if (buildMatch) {
    const [, projectId, buildId, logs] = buildMatch;
    assertProjectAccess(actor, projectId);
    const project = projectStore.get(projectId);
    if (!project) throw notFoundProject(projectId);
    if (method === "POST" && !buildId) {
      assertExecutionAccess(actor, "builds");
      const body = await readJson(req);
      const audit = auditService.record({ action: "build.start", actorId: actor.id, projectId, resourceType: "build", resourceId: "pending", metadata: { clean: body?.clean !== false, runTests: body?.runTests !== false, createDockerImage: body?.createDockerImage === true }, requestId });
      const build = await buildService.createBuild(projectId, body);
      auditService.record({ action: "build.created", actorId: actor.id, projectId, resourceType: "build", resourceId: build.buildId, metadata: { status: build.status, auditEventId: audit.id }, requestId });
      return writeJson(res, 201, build);
    }
    if (method === "GET" && !buildId) return writeJson(res, 200, { builds: buildService.list(projectId) });
    if (method === "GET" && buildId && logs) return writeJson(res, 200, { buildId, logs: buildService.logs(projectId, buildId) });
  }

  const deploymentMatch = path.match(/^\/api\/projects\/([^/]+)\/deployments(?:\/([^/]+)(?:\/(status|logs|restart|rollback))?)?$/);
  if (deploymentMatch) {
    const [, projectId, deploymentId, action] = deploymentMatch;
    assertProjectAccess(actor, projectId);
    const project = projectStore.get(projectId);
    if (!project) throw notFoundProject(projectId);
    if (method === "GET" && !deploymentId) return writeJson(res, 200, { deployments: deploymentStore.list(projectId) });
    if (method === "POST" && !deploymentId) {
      assertExecutionAccess(actor, "deployments");
      const body = await readJson(req);
      return writeJson(res, 201, await deploymentStore.create(projectId, body?.targetId, body?.buildId, actor.id, requestId));
    }
    const deployment = deploymentId ? deploymentStore.get(projectId, deploymentId) : undefined;
    if (!deployment) throw notFoundDeployment(deploymentId ?? "unknown");
    if (method === "GET" && !action) return writeJson(res, 200, deployment);
    if (method === "GET" && action === "status") return writeJson(res, 200, await deploymentStore.status(projectId, deployment.deploymentId));
    if (method === "GET" && action === "logs") return writeJson(res, 200, { deploymentId: deployment.deploymentId, logs: deploymentStore.logs(projectId, deployment.deploymentId) });
    if (method === "POST" && action === "restart") { assertExecutionAccess(actor, "deployment restart"); return writeJson(res, 200, await deploymentStore.restart(projectId, deployment.deploymentId, actor.id, requestId)); }
    if (method === "POST" && action === "rollback") { assertExecutionAccess(actor, "deployment rollback"); return writeJson(res, 200, await deploymentStore.rollback(projectId, deployment.deploymentId, actor.id, requestId)); }
  }

  const projectMatch = path.match(/^\/api\/projects\/([^/]+)(?:\/(commands|validate|clone|archive|github|git-status|scan))?(?:\/([^/]+))?$/);
  if (projectMatch) {
    const [, projectId, resource, commandId] = projectMatch;
    assertProjectAccess(actor, projectId);
    const project = projectStore.get(projectId);
    if (!project) throw notFoundProject(projectId);
    if (method === "GET" && !resource) return writeJson(res, 200, project);
    if (method === "PATCH" && !resource) return writeJson(res, 200, projectStore.update(projectId, parseUpdateProjectInput(await readJson(req))) ?? notFoundProject(projectId));
    if (method === "DELETE" && !resource) { if (!projectStore.delete(projectId)) throw notFoundProject(projectId); res.statusCode = 204; res.end(); return; }
    if (method === "POST" && resource === "clone") return writeJson(res, 201, projectStore.clone(projectId));
    if (method === "POST" && resource === "archive") return writeJson(res, 200, projectStore.archive(projectId));
    if (method === "POST" && resource === "validate") return writeJson(res, 200, validateProject(project, (secretId) => secretStore.has(secretId)));
    if (resource === "commands") {
      if (method === "GET" && !commandId) return writeJson(res, 200, { commands: project.commands });
      if (method === "POST" && !commandId) {
        const command = parseCommandDefinition(await readJson(req));
        const errors = validateCommands([...project.commands, command]);
        if (errors.length > 0) throw new RequestValidationError(errors);
        return writeJson(res, 201, projectStore.update(projectId, { commands: [...project.commands, command] })?.commands.at(-1));
      }
      if (method === "PATCH" && commandId) {
        const patch = parseCommandPatch(await readJson(req));
        const nextCommands = project.commands.map((command) => command.id === commandId ? { ...command, ...patch } : command);
        const errors = validateCommands(nextCommands);
        if (errors.length > 0) throw new RequestValidationError(errors);
        return writeJson(res, 200, projectStore.update(projectId, { commands: nextCommands })?.commands.find((command) => command.id === commandId));
      }
      if (method === "DELETE" && commandId) return writeJson(res, 200, { deleted: projectStore.update(projectId, { commands: project.commands.filter((command) => command.id !== commandId) }) !== undefined });
    }
    if (method === "POST" && resource === "github") return writeJson(res, 200, githubService.linkProject(projectStore, projectId, await readJson(req)));
    if (method === "POST" && resource === "scan") return writeJson(res, 200, await scanAndGenerateBotbladeMetadata(await fileService.projectRoot(projectId), { kind: "folder" }));
    if (method === "GET" && resource === "git-status") return writeJson(res, 200, await gitStatusService.status(await fileService.projectRoot(projectId)));
  }

  if (method === "POST" && path === "/api/imports") {
    assertExecutionAccess(actor, "imports");
    const body = await readJson(req);
    const source = parseImportSource(body?.sourceType, body?.source);
    const workspacePath = typeof body?.workspacePath === "string" ? body.workspacePath : pathModule.resolve(process.cwd(), "workspace");
    return writeJson(res, 201, await importStore.createAndRun(source, workspacePath, auditService, actor.id, requestId));
  }

  throw { statusCode: 404, code: "NOT_FOUND", message: `No route for ${method} ${path}`, details: {} };
}

function parseImportSource(sourceType: unknown, source: unknown) {
  if (sourceType === "git" && typeof source === "string") return { type: "git" as const, repoUrl: source };
  if (sourceType === "zip" && typeof source === "string") return { type: "zip" as const, archivePath: source };
  if (sourceType === "folder" && typeof source === "string") return { type: "folder" as const, folderPath: source };
  if (sourceType === "workflow_json" && typeof source === "string") return { type: "workflow_json" as const, workflowPath: source };
  if (sourceType === "template" && typeof source === "string") return { type: "template" as const, templateId: source };
  if (sourceType === "repair" && typeof source === "string") return { type: "repair" as const, projectId: source };
  throw { statusCode: 400, code: "INVALID_IMPORT_SOURCE", message: "Import sourceType/source are invalid.", details: { sourceType } };
}

function handleLogsUpgrade(req: IncomingMessage, socket: Socket): void {
  socket.write("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n\r\n");
  logClients.add(socket);
  socket.on("close", () => logClients.delete(socket));
}

async function readJson(req: IncomingMessage): Promise<any> {
  const chunks: Buffer[] = [];
  for await (const chunk of req) chunks.push(typeof chunk === "string" ? Buffer.from(chunk) : chunk);
  if (chunks.length === 0) return {};
  return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}

function writeJson(res: ServerResponse, statusCode: number, body: unknown): void {
  res.statusCode = statusCode;
  res.end(JSON.stringify(body));
}

function writeError(res: ServerResponse, error: unknown, requestId: string): void {
  const typed = error as { statusCode?: number; code?: string; message?: string; details?: unknown };
  res.statusCode = typed.statusCode ?? 500;
  res.end(JSON.stringify({ error: typed.code ?? "INTERNAL_SERVER_ERROR", message: typed.message ?? "Internal server error", details: typed.details ?? {}, requestId }));
}

function extractPathname(url: string | undefined): string {
  return new URL(url ?? "/", "http://localhost").pathname;
}

function notFoundProject(projectId: string) { return { statusCode: 404, code: "PROJECT_NOT_FOUND", message: `Project ${projectId} not found.`, details: { projectId } }; }
function notFoundSecret(secretId: string) { return { statusCode: 404, code: "SECRET_NOT_FOUND", message: `Secret ${secretId} not found.`, details: { secretId } }; }
function notFoundTarget(targetId: string) { return { statusCode: 404, code: "TARGET_NOT_FOUND", message: `Deployment target ${targetId} not found.`, details: { targetId } }; }
function notFoundDeployment(deploymentId: string) { return { statusCode: 404, code: "DEPLOYMENT_NOT_FOUND", message: `Deployment ${deploymentId} not found.`, details: { deploymentId } }; }
