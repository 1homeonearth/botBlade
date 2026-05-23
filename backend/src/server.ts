// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import { createServer, type IncomingMessage, type ServerResponse } from "node:http";  // line 7: executes this statement as part of this file's behavior
type Socket = any;  // line 8: executes this statement as part of this file's behavior
import crypto from "node:crypto";  // line 9: executes this statement as part of this file's behavior
import { API_VERSION, SERVICE_NAME, SERVICE_VERSION } from "./models/project.js";  // line 10: executes this statement as part of this file's behavior
import { AuditService } from "./services/auditService.js";  // line 11: executes this statement as part of this file's behavior
import { assertExecutionAccess, assertGlobalAccess, assertProjectAccess, authenticateRequest, canAccessProject, hasGlobalProjectAccess } from "./services/authService.js";  // line 12: executes this statement as part of this file's behavior
import { BuildService } from "./services/buildService.js";  // line 13: executes this statement as part of this file's behavior
import { parseCommandDefinition, parseCommandPatch, validateCommands } from "./services/commandDefinitions.js";  // line 14: executes this statement as part of this file's behavior
import { DeploymentJobStore } from "./services/deploymentJobs.js";  // line 15: executes this statement as part of this file's behavior
import { DeploymentTargetStore, deploymentTargetWithCapabilities, testDeploymentTarget } from "./services/deploymentTargets.js";  // line 16: executes this statement as part of this file's behavior
import { GitHubIntegrationService } from "./services/githubIntegration.js";  // line 17: executes this statement as part of this file's behavior
import { LocalProcessRuntimeService } from "./services/localProcessRuntimeService.js";  // line 18: executes this statement as part of this file's behavior
import { ProjectFileService, parseFileWriteInput } from "./services/projectFiles.js";  // line 19: executes this statement as part of this file's behavior
import { DuplicateSlugError, parseCreateProjectInput, parseToggleAction, parseUpdateProjectInput, ProjectStore, RequestValidationError } from "./services/projectStore.js";  // line 20: executes this statement as part of this file's behavior
import { redactSecrets } from "./services/redaction.js";  // line 21: executes this statement as part of this file's behavior
import { parseCreateSecretInput, parseRotateSecretInput, parseUpdateSecretInput, SecretStore } from "./services/secretStore.js";  // line 22: executes this statement as part of this file's behavior
import { validateProject } from "./services/projectValidation.js";  // line 23: executes this statement as part of this file's behavior
import { scanAndGenerateBotbladeMetadata } from "./services/importScan/index.js";  // line 24: executes this statement as part of this file's behavior
import { SqlitePersistence } from "./persistence/sqlitePersistence.js";  // line 25: executes this statement as part of this file's behavior

const persistence = createPersistence();  // line 27: executes this statement as part of this file's behavior
const projectStore = new ProjectStore(persistence);  // line 28: executes this statement as part of this file's behavior
const secretStore = new SecretStore(persistence);  // line 29: executes this statement as part of this file's behavior
const fileService = new ProjectFileService();  // line 30: executes this statement as part of this file's behavior
const auditService = new AuditService(persistence);  // line 31: executes this statement as part of this file's behavior
const auditFromService = (event: { action: Parameters<AuditService["record"]>[0]["action"]; projectId: string; resourceType: string; resourceId: string; metadata: Record<string, unknown>; requestId: string; actorId?: string }) => auditService.record(event);  // line 32: executes this statement as part of this file's behavior
const buildService = new BuildService(fileService, (secretId) => secretStore.has(secretId), auditFromService, undefined, persistence);  // line 33: executes this statement as part of this file's behavior
const runtimeService = new LocalProcessRuntimeService(fileService, (secretId) => secretStore.getValue(secretId));  // line 34: executes this statement as part of this file's behavior
const targetStore = new DeploymentTargetStore(persistence);  // line 35: executes this statement as part of this file's behavior
const deploymentStore = new DeploymentJobStore(buildService, targetStore, runtimeService, auditFromService, persistence, (secretId) => {  // line 36: executes this statement as part of this file's behavior
  const summary = secretStore.get(secretId);  // line 37: executes this statement as part of this file's behavior
  const value = secretStore.getValue(secretId);  // line 38: executes this statement as part of this file's behavior
  return summary && value !== undefined ? { id: summary.id, name: summary.name, value } : undefined;  // line 39: executes this statement as part of this file's behavior
});  // line 40: executes this statement as part of this file's behavior
const githubService = new GitHubIntegrationService((secretId) => secretStore.has(secretId), (secretId) => secretStore.getValue(secretId), (input) => auditService.record(input));  // line 41: executes this statement as part of this file's behavior
const host = "127.0.0.1";  // line 42: executes this statement as part of this file's behavior
const port = 7432;  // line 43: executes this statement as part of this file's behavior

function createPersistence(): SqlitePersistence | undefined {  // line 45: executes this statement as part of this file's behavior
  if (process.env.NODE_ENV === "test" && !process.env.BOTBLADE_DATABASE_URL && !process.env.DATABASE_URL) return undefined;  // line 46: executes this statement as part of this file's behavior
  try {  // line 47: executes this statement as part of this file's behavior
    return SqlitePersistence.fromUrl();  // line 48: executes this statement as part of this file's behavior
  } catch (error) {  // line 49: executes this statement as part of this file's behavior
    if (error instanceof Error) throw new Error(`Persistence startup failed: ${error.message}`);  // line 50: executes this statement as part of this file's behavior
    throw error;  // line 51: executes this statement as part of this file's behavior
  }  // line 52: executes this statement as part of this file's behavior
}  // line 53: executes this statement as part of this file's behavior

export function createRequestListener() {  // line 55: executes this statement as part of this file's behavior
  return async (req: IncomingMessage, res: ServerResponse) => {  // line 56: executes this statement as part of this file's behavior
    const requestId = typeof req.headers["x-request-id"] === "string" ? req.headers["x-request-id"] : crypto.randomUUID();  // line 57: executes this statement as part of this file's behavior
    res.setHeader("x-request-id", requestId);  // line 58: executes this statement as part of this file's behavior
    res.setHeader("content-type", "application/json");  // line 59: executes this statement as part of this file's behavior
    try {  // line 60: executes this statement as part of this file's behavior
      await handleRequest(req, res, requestId);  // line 61: executes this statement as part of this file's behavior
      console.info(redactSecrets(JSON.stringify({ level: "info", requestId, method: req.method, url: req.url })));  // line 62: executes this statement as part of this file's behavior
    } catch (error) {  // line 63: executes this statement as part of this file's behavior
      console.error(redactSecrets(JSON.stringify({ level: "error", requestId, method: req.method, url: req.url, message: error instanceof Error ? error.message : "Request failed" })));  // line 64: executes this statement as part of this file's behavior
      writeError(res, error, requestId);  // line 65: executes this statement as part of this file's behavior
    }  // line 66: executes this statement as part of this file's behavior
  };  // line 67: executes this statement as part of this file's behavior
}  // line 68: executes this statement as part of this file's behavior

const logClients = new Set<Socket>();  // line 70: executes this statement as part of this file's behavior

if (process.env.NODE_ENV !== "test") {  // line 72: executes this statement as part of this file's behavior
  const server = createServer(createRequestListener()) as any;  // line 73: executes this statement as part of this file's behavior
  server.on("upgrade", (req: IncomingMessage, socket: Socket) => handleLogsUpgrade(req, socket));  // line 74: executes this statement as part of this file's behavior
  server.listen(port, host, () => console.info(JSON.stringify({ level: "info", message: "botBlade backend listening", host, port })));  // line 75: executes this statement as part of this file's behavior
}  // line 76: executes this statement as part of this file's behavior

async function handleRequest(req: IncomingMessage, res: ServerResponse, requestId: string): Promise<void> {  // line 78: executes this statement as part of this file's behavior
  const method = req.method ?? "GET";  // line 79: executes this statement as part of this file's behavior
  const path = extractPathname(req.url);  // line 80: executes this statement as part of this file's behavior

  if (method === "GET" && path === "/api/health") return writeJson(res, 200, { ok: true, service: SERVICE_NAME, version: SERVICE_VERSION });  // line 82: executes this statement as part of this file's behavior
  if (method === "GET" && path === "/api/version") return writeJson(res, 200, { name: SERVICE_NAME, version: SERVICE_VERSION, apiVersion: API_VERSION });  // line 83: executes this statement as part of this file's behavior
  if (method === "GET" && path === "/api/diagnostics/startup-crash") {  // line 84: executes this statement as part of this file's behavior
    const artifactPath = process.env.BOTBLADE_STARTUP_CRASH_ARTIFACT;  // line 85: executes this statement as part of this file's behavior
    if (!artifactPath) return writeJson(res, 200, { artifact: null, source: "unconfigured" });  // line 86: executes this statement as part of this file's behavior
    const fs = await import("node:fs/promises");  // line 87: executes this statement as part of this file's behavior
    try {  // line 88: executes this statement as part of this file's behavior
      const artifact = await fs.readFile(artifactPath, "utf8");  // line 89: executes this statement as part of this file's behavior
      return writeJson(res, 200, { artifact, source: "file" });  // line 90: executes this statement as part of this file's behavior
    } catch {  // line 91: executes this statement as part of this file's behavior
      return writeJson(res, 200, { artifact: null, source: "missing" });  // line 92: executes this statement as part of this file's behavior
    }  // line 93: executes this statement as part of this file's behavior
  }  // line 94: executes this statement as part of this file's behavior

  const actor = authenticateRequest(req);  // line 96: executes this statement as part of this file's behavior

  if (method === "GET" && path === "/api/audit-events") {  // line 98: executes this statement as part of this file's behavior
    assertGlobalAccess(actor, "audit events");  // line 99: executes this statement as part of this file's behavior
    return writeJson(res, 200, { auditEvents: auditService.list() });  // line 100: executes this statement as part of this file's behavior
  }  // line 101: executes this statement as part of this file's behavior

  if (method === "GET" && path === "/api/github/status") {  // line 103: executes this statement as part of this file's behavior
    assertGlobalAccess(actor, "GitHub integration");  // line 104: executes this statement as part of this file's behavior
    return writeJson(res, 200, githubService.status());  // line 105: executes this statement as part of this file's behavior
  }  // line 106: executes this statement as part of this file's behavior
  if (method === "POST" && path === "/api/github/connect") {  // line 107: executes this statement as part of this file's behavior
    assertGlobalAccess(actor, "GitHub integration");  // line 108: executes this statement as part of this file's behavior
    return writeJson(res, 200, githubService.connect(await readJson(req)));  // line 109: executes this statement as part of this file's behavior
  }  // line 110: executes this statement as part of this file's behavior

  if (method === "GET" && path === "/api/deployment-targets") {  // line 112: executes this statement as part of this file's behavior
    assertGlobalAccess(actor, "deployment targets");  // line 113: executes this statement as part of this file's behavior
    return writeJson(res, 200, { targets: targetStore.list().map(deploymentTargetWithCapabilities) });  // line 114: executes this statement as part of this file's behavior
  }  // line 115: executes this statement as part of this file's behavior
  if (method === "POST" && path === "/api/deployment-targets") {  // line 116: executes this statement as part of this file's behavior
    assertGlobalAccess(actor, "deployment targets");  // line 117: executes this statement as part of this file's behavior
    return writeJson(res, 201, deploymentTargetWithCapabilities(targetStore.create(await readJson(req))));  // line 118: executes this statement as part of this file's behavior
  }  // line 119: executes this statement as part of this file's behavior
  const targetMatch = path.match(/^\/api\/deployment-targets\/([^/]+)(?:\/(test))?$/);  // line 120: executes this statement as part of this file's behavior
  if (targetMatch) {  // line 121: executes this statement as part of this file's behavior
    const [, targetId, action] = targetMatch;  // line 122: executes this statement as part of this file's behavior
    assertGlobalAccess(actor, "deployment targets");  // line 123: executes this statement as part of this file's behavior
    const target = targetStore.get(targetId);  // line 124: executes this statement as part of this file's behavior
    if (!target) throw notFoundTarget(targetId);  // line 125: executes this statement as part of this file's behavior
    if (method === "GET" && !action) return writeJson(res, 200, deploymentTargetWithCapabilities(target));  // line 126: executes this statement as part of this file's behavior
    if (method === "PATCH" && !action) return writeJson(res, 200, deploymentTargetWithCapabilities(targetStore.update(targetId, await readJson(req)) ?? notFoundTarget(targetId)));  // line 127: executes this statement as part of this file's behavior
    if (method === "DELETE" && !action) {  // line 128: executes this statement as part of this file's behavior
      if (!targetStore.delete(targetId)) throw notFoundTarget(targetId);  // line 129: executes this statement as part of this file's behavior
      res.statusCode = 204;  // line 130: executes this statement as part of this file's behavior
      res.end();  // line 131: executes this statement as part of this file's behavior
      return;  // line 132: executes this statement as part of this file's behavior
    }  // line 133: executes this statement as part of this file's behavior
    if (method === "POST" && action === "test") return writeJson(res, 200, await testDeploymentTarget(target));  // line 134: executes this statement as part of this file's behavior
  }  // line 135: executes this statement as part of this file's behavior

  if (method === "GET" && path === "/api/bot-status/") {  // line 137: executes this statement as part of this file's behavior
    const project = projectStore.list()[0];  // line 138: executes this statement as part of this file's behavior
    if (project) assertProjectAccess(actor, project.id);  // line 139: executes this statement as part of this file's behavior
    return writeJson(res, 200, project ? runtimeService.getStatus(project.id) : { projectId: "default", status: "stopped", running: false, pid: null, startedAt: null, lastExitCode: null, message: "No active/default project exists." });  // line 140: executes this statement as part of this file's behavior
  }  // line 141: executes this statement as part of this file's behavior
  if (method === "POST" && path === "/api/bot-toggle/") {  // line 142: executes this statement as part of this file's behavior
    const project = projectStore.list()[0];  // line 143: executes this statement as part of this file's behavior
    if (!project) throw { statusCode: 400, code: "NO_ACTIVE_PROJECT", message: "Create or select a project before toggling the bot runtime.", details: {} };  // line 144: executes this statement as part of this file's behavior
    assertProjectAccess(actor, project.id);  // line 145: executes this statement as part of this file's behavior
    const action = parseToggleAction(await readJson(req));  // line 146: executes this statement as part of this file's behavior
    return writeJson(res, 200, action === "start" ? await runtimeService.start(project) : await runtimeService.stop(project.id));  // line 147: executes this statement as part of this file's behavior
  }  // line 148: executes this statement as part of this file's behavior

  if (method === "GET" && path === "/api/secrets") return writeJson(res, 200, { secrets: secretStore.list().filter((secret) => canAccessProject(actor, secret.projectId)) });  // line 150: executes this statement as part of this file's behavior
  if (method === "POST" && path === "/api/secrets") {  // line 151: executes this statement as part of this file's behavior
    const input = parseCreateSecretInput(await readJson(req));  // line 152: executes this statement as part of this file's behavior
    assertProjectAccess(actor, input.projectId);  // line 153: executes this statement as part of this file's behavior
    const secret = secretStore.create(input);  // line 154: executes this statement as part of this file's behavior
    const audit = auditService.record({ action: "secret.create", actorId: actor.id, projectId: secret.projectId, resourceType: "secret", resourceId: secret.id, metadata: { name: secret.name, type: secret.type, storageMode: secret.storageMode, fingerprint: secret.fingerprint }, requestId });  // line 155: executes this statement as part of this file's behavior
    return writeJson(res, 201, { ...secret, auditEventId: audit.id });  // line 156: executes this statement as part of this file's behavior
  }  // line 157: executes this statement as part of this file's behavior
  const secretMatch = path.match(/^\/api\/secrets\/([^/]+)(?:\/(rotate))?$/);  // line 158: executes this statement as part of this file's behavior
  if (secretMatch) {  // line 159: executes this statement as part of this file's behavior
    const [, secretId, action] = secretMatch;  // line 160: executes this statement as part of this file's behavior
    const existingSecret = secretStore.get(secretId);  // line 161: executes this statement as part of this file's behavior
    if (!existingSecret) throw notFoundSecret(secretId);  // line 162: executes this statement as part of this file's behavior
    assertProjectAccess(actor, existingSecret.projectId);  // line 163: executes this statement as part of this file's behavior
    if (method === "GET" && !action) return writeJson(res, 200, existingSecret);  // line 164: executes this statement as part of this file's behavior
    if (method === "PATCH" && !action) {  // line 165: executes this statement as part of this file's behavior
      const input = parseUpdateSecretInput(await readJson(req));  // line 166: executes this statement as part of this file's behavior
      if (input.projectId !== undefined) assertProjectAccess(actor, input.projectId);  // line 167: executes this statement as part of this file's behavior
      return writeJson(res, 200, secretStore.update(secretId, input) ?? notFoundSecret(secretId));  // line 168: executes this statement as part of this file's behavior
    }  // line 169: executes this statement as part of this file's behavior
    if (method === "DELETE" && !action) {  // line 170: executes this statement as part of this file's behavior
      const existing = existingSecret;  // line 171: executes this statement as part of this file's behavior
      if (!secretStore.delete(secretId)) throw notFoundSecret(secretId);  // line 172: executes this statement as part of this file's behavior
      auditService.record({ action: "secret.delete", actorId: actor.id, projectId: existing?.projectId ?? null, resourceType: "secret", resourceId: secretId, metadata: { name: existing?.name, type: existing?.type }, requestId });  // line 173: executes this statement as part of this file's behavior
      res.statusCode = 204;  // line 174: executes this statement as part of this file's behavior
      res.end();  // line 175: executes this statement as part of this file's behavior
      return;  // line 176: executes this statement as part of this file's behavior
    }  // line 177: executes this statement as part of this file's behavior
    if (method === "POST" && action === "rotate") {  // line 178: executes this statement as part of this file's behavior
      const secret = secretStore.rotate(secretId, parseRotateSecretInput(await readJson(req))) ?? notFoundSecret(secretId);  // line 179: executes this statement as part of this file's behavior
      const audit = auditService.record({ action: "secret.rotate", actorId: actor.id, projectId: secret.projectId, resourceType: "secret", resourceId: secret.id, metadata: { name: secret.name, type: secret.type, fingerprint: secret.fingerprint }, requestId });  // line 180: executes this statement as part of this file's behavior
      return writeJson(res, 200, { ...secret, auditEventId: audit.id });  // line 181: executes this statement as part of this file's behavior
    }  // line 182: executes this statement as part of this file's behavior
  }  // line 183: executes this statement as part of this file's behavior

  if (method === "GET" && path === "/api/projects") return writeJson(res, 200, { projects: hasGlobalProjectAccess(actor) ? projectStore.list() : projectStore.list().filter((project) => canAccessProject(actor, project.id)) });  // line 185: executes this statement as part of this file's behavior
  if (method === "POST" && path === "/api/projects") {  // line 186: executes this statement as part of this file's behavior
    const input = parseCreateProjectInput(await readJson(req));  // line 187: executes this statement as part of this file's behavior
    if (input.discord?.tokenSecretRef && !secretStore.has(input.discord.tokenSecretRef)) throw new RequestValidationError([{ field: "discord.tokenSecretRef", message: "Secret reference does not exist." }]);  // line 188: executes this statement as part of this file's behavior
    const project = projectStore.create(input);  // line 189: executes this statement as part of this file's behavior
    const audit = auditService.record({ action: "project.create", actorId: actor.id, projectId: project.id, resourceType: "project", resourceId: project.id, metadata: { name: project.name, slug: project.slug, templateId: project.templateId }, requestId });  // line 190: executes this statement as part of this file's behavior
    return writeJson(res, 201, { ...project, auditEventId: audit.id });  // line 191: executes this statement as part of this file's behavior
  }  // line 192: executes this statement as part of this file's behavior

  const auditMatch = path.match(/^\/api\/projects\/([^/]+)\/audit-events$/);  // line 194: executes this statement as part of this file's behavior
  if (auditMatch) {  // line 195: executes this statement as part of this file's behavior
    const [, projectId] = auditMatch;  // line 196: executes this statement as part of this file's behavior
    assertProjectAccess(actor, projectId);  // line 197: executes this statement as part of this file's behavior
    if (!projectStore.get(projectId)) throw notFoundProject(projectId);  // line 198: executes this statement as part of this file's behavior
    if (method === "GET") return writeJson(res, 200, { auditEvents: auditService.list(projectId) });  // line 199: executes this statement as part of this file's behavior
  }  // line 200: executes this statement as part of this file's behavior

  const fileMatch = path.match(/^\/api\/projects\/([^/]+)\/files(?:\/(.*))?$/);  // line 202: executes this statement as part of this file's behavior
  if (fileMatch) {  // line 203: executes this statement as part of this file's behavior
    const [, projectId, filePath] = fileMatch;  // line 204: executes this statement as part of this file's behavior
    assertProjectAccess(actor, projectId);  // line 205: executes this statement as part of this file's behavior
    const project = projectStore.get(projectId);  // line 206: executes this statement as part of this file's behavior
    if (!project) throw notFoundProject(projectId);  // line 207: executes this statement as part of this file's behavior
    if (method === "GET" && filePath === undefined) return writeJson(res, 200, { files: await fileService.list(projectId) });  // line 208: executes this statement as part of this file's behavior
    if (method === "GET" && filePath !== undefined) return writeJson(res, 200, await fileService.read(projectId, filePath));  // line 209: executes this statement as part of this file's behavior
    if (method === "PUT" && filePath !== undefined) { assertExecutionAccess(actor, "project file writes"); return writeJson(res, 200, await fileService.write(projectId, filePath, parseFileWriteInput(await readJson(req)).content)); }  // line 210: executes this statement as part of this file's behavior
  }  // line 211: executes this statement as part of this file's behavior

  const buildMatch = path.match(/^\/api\/projects\/([^/]+)\/builds(?:\/([^/]+)(?:\/(logs))?)?$/);  // line 213: executes this statement as part of this file's behavior
  if (buildMatch) {  // line 214: executes this statement as part of this file's behavior
    const [, projectId, buildId, logs] = buildMatch;  // line 215: executes this statement as part of this file's behavior
    assertProjectAccess(actor, projectId);  // line 216: executes this statement as part of this file's behavior
    const project = projectStore.get(projectId);  // line 217: executes this statement as part of this file's behavior
    if (!project) throw notFoundProject(projectId);  // line 218: executes this statement as part of this file's behavior
    if (method === "POST" && !buildId) {  // line 219: executes this statement as part of this file's behavior
      assertExecutionAccess(actor, "builds");  // line 220: executes this statement as part of this file's behavior
      const body = await readJson(req);  // line 221: executes this statement as part of this file's behavior
      const audit = auditService.record({ action: "build.start", actorId: actor.id, projectId, resourceType: "build", resourceId: "pending", metadata: body && typeof body === "object" && !Array.isArray(body) ? body as Record<string, unknown> : {}, requestId });  // line 222: executes this statement as part of this file's behavior
      const job = await buildService.create(project, body, audit.id, requestId, actor.id);  // line 223: executes this statement as part of this file's behavior
      audit.resourceId = job.buildId;  // line 224: executes this statement as part of this file's behavior
      return writeJson(res, 201, job);  // line 225: executes this statement as part of this file's behavior
    }  // line 226: executes this statement as part of this file's behavior
    if (method === "GET" && !buildId) return writeJson(res, 200, { builds: buildService.list(projectId) });  // line 227: executes this statement as part of this file's behavior
    if (method === "GET" && buildId && !logs) return writeJson(res, 200, buildService.get(projectId, buildId) ?? notFoundBuild(buildId));  // line 228: executes this statement as part of this file's behavior
    if (method === "GET" && buildId && logs) return writeJson(res, 200, { logs: buildService.getLogs(projectId, buildId) ?? notFoundBuild(buildId) });  // line 229: executes this statement as part of this file's behavior
  }  // line 230: executes this statement as part of this file's behavior

  const runtimeMatch = path.match(/^\/api\/projects\/([^/]+)\/runtime\/(status|start|stop|restart|logs)$/);  // line 232: executes this statement as part of this file's behavior
  if (runtimeMatch) {  // line 233: executes this statement as part of this file's behavior
    const [, projectId, action] = runtimeMatch;  // line 234: executes this statement as part of this file's behavior
    assertProjectAccess(actor, projectId);  // line 235: executes this statement as part of this file's behavior
    const project = projectStore.get(projectId);  // line 236: executes this statement as part of this file's behavior
    if (!project) throw notFoundProject(projectId);  // line 237: executes this statement as part of this file's behavior
    if (method === "GET" && action === "status") return writeJson(res, 200, runtimeService.getStatus(projectId));  // line 238: executes this statement as part of this file's behavior
    if (method === "POST" && action === "start") { assertExecutionAccess(actor, "runtime start"); const status = await runtimeService.start(project); const audit = auditService.record({ action: "runtime.start", actorId: actor.id, projectId, resourceType: "runtime", resourceId: projectId, metadata: { status: status.status, running: status.running }, requestId }); return writeJson(res, 200, { ...status, auditEventId: audit.id }); }  // line 239: executes this statement as part of this file's behavior
    if (method === "POST" && action === "stop") { assertExecutionAccess(actor, "runtime stop"); const status = await runtimeService.stop(projectId); const audit = auditService.record({ action: "runtime.stop", actorId: actor.id, projectId, resourceType: "runtime", resourceId: projectId, metadata: { status: status.status, running: status.running }, requestId }); return writeJson(res, 200, { ...status, auditEventId: audit.id }); }  // line 240: executes this statement as part of this file's behavior
    if (method === "POST" && action === "restart") { assertExecutionAccess(actor, "runtime restart"); const status = await runtimeService.restart(project); const audit = auditService.record({ action: "runtime.restart", actorId: actor.id, projectId, resourceType: "runtime", resourceId: projectId, metadata: { status: status.status, running: status.running }, requestId }); return writeJson(res, 200, { ...status, auditEventId: audit.id }); }  // line 241: executes this statement as part of this file's behavior
    if (method === "GET" && action === "logs") return writeJson(res, 200, { logs: runtimeService.getLogs(projectId) });  // line 242: executes this statement as part of this file's behavior
  }  // line 243: executes this statement as part of this file's behavior

  const deploymentMatch = path.match(/^\/api\/projects\/([^/]+)\/deployments(?:\/([^/]+)(?:\/(logs|rollback|status|start|stop|restart))?)?$/);  // line 245: executes this statement as part of this file's behavior
  if (deploymentMatch) {  // line 246: executes this statement as part of this file's behavior
    const [, projectId, deploymentId, action] = deploymentMatch;  // line 247: executes this statement as part of this file's behavior
    assertProjectAccess(actor, projectId);  // line 248: executes this statement as part of this file's behavior
    const project = projectStore.get(projectId);  // line 249: executes this statement as part of this file's behavior
    if (!project) throw notFoundProject(projectId);  // line 250: executes this statement as part of this file's behavior
    if (method === "POST" && !deploymentId) {  // line 251: executes this statement as part of this file's behavior
      assertExecutionAccess(actor, "deployments");  // line 252: executes this statement as part of this file's behavior
      const body = await readJson(req);  // line 253: executes this statement as part of this file's behavior
      const audit = auditService.record({ action: "deployment.start", actorId: actor.id, projectId, resourceType: "deployment", resourceId: "pending", metadata: body && typeof body === "object" && !Array.isArray(body) ? body as Record<string, unknown> : {}, requestId });  // line 254: executes this statement as part of this file's behavior
      const job = await deploymentStore.create(project, body, audit.id, requestId, actor.id);  // line 255: executes this statement as part of this file's behavior
      audit.resourceId = job.deploymentId;  // line 256: executes this statement as part of this file's behavior
      return writeJson(res, 201, job);  // line 257: executes this statement as part of this file's behavior
    }  // line 258: executes this statement as part of this file's behavior
    if (method === "GET" && !deploymentId) return writeJson(res, 200, { deployments: deploymentStore.list(projectId) });  // line 259: executes this statement as part of this file's behavior
    if (method === "GET" && deploymentId && !action) return writeJson(res, 200, deploymentStore.get(projectId, deploymentId) ?? notFoundDeployment(deploymentId));  // line 260: executes this statement as part of this file's behavior
    if (method === "GET" && deploymentId && action === "logs") return writeJson(res, 200, await deploymentStore.action(project, deploymentId, "logs"));  // line 261: executes this statement as part of this file's behavior
    if (method === "GET" && deploymentId && action === "status") return writeJson(res, 200, await deploymentStore.action(project, deploymentId, "status"));  // line 262: executes this statement as part of this file's behavior
    if (method === "POST" && deploymentId && (action === "start" || action === "stop" || action === "restart" || action === "rollback")) { assertExecutionAccess(actor, "deployment actions"); return writeJson(res, 200, await deploymentStore.action(project, deploymentId, action)); }  // line 263: executes this statement as part of this file's behavior
  }  // line 264: executes this statement as part of this file's behavior


  const scanMatch = path.match(/^\/api\/projects\/([^/]+)\/scan$/);  // line 267: executes this statement as part of this file's behavior
  if (scanMatch) {  // line 268: executes this statement as part of this file's behavior
    const [, projectId] = scanMatch;  // line 269: executes this statement as part of this file's behavior
    assertProjectAccess(actor, projectId);  // line 270: executes this statement as part of this file's behavior
    const project = projectStore.get(projectId);  // line 271: executes this statement as part of this file's behavior
    if (!project) throw notFoundProject(projectId);  // line 272: executes this statement as part of this file's behavior
    if (method === "POST") {  // line 273: executes this statement as part of this file's behavior
      assertExecutionAccess(actor, "project scan");  // line 274: executes this statement as part of this file's behavior
      const workspacePath = fileService.workspace(projectId);  // line 275: executes this statement as part of this file's behavior
      const result = await scanAndGenerateBotbladeMetadata(workspacePath, { kind: "generated-project", url: project.github?.repo ? `https://github.com/${project.github.owner}/${project.github.repo}` : undefined });  // line 276: executes this statement as part of this file's behavior
      return writeJson(res, 200, result);  // line 277: executes this statement as part of this file's behavior
    }  // line 278: executes this statement as part of this file's behavior
  }  // line 279: executes this statement as part of this file's behavior

  const commandsMatch = path.match(/^\/api\/projects\/([^/]+)\/commands(?:\/([^/]+))?$/);  // line 281: executes this statement as part of this file's behavior
  if (commandsMatch) {  // line 282: executes this statement as part of this file's behavior
    const [, projectId, commandId] = commandsMatch;  // line 283: executes this statement as part of this file's behavior
    assertProjectAccess(actor, projectId);  // line 284: executes this statement as part of this file's behavior
    const project = projectStore.get(projectId);  // line 285: executes this statement as part of this file's behavior
    if (!project) throw notFoundProject(projectId);  // line 286: executes this statement as part of this file's behavior
    if (method === "GET" && !commandId) return writeJson(res, 200, { commands: project.commands });  // line 287: executes this statement as part of this file's behavior
    if (method === "POST" && !commandId) {  // line 288: executes this statement as part of this file's behavior
      const command = parseCommandDefinition(await readJson(req));  // line 289: executes this statement as part of this file's behavior
      const commands = [...project.commands, command];  // line 290: executes this statement as part of this file's behavior
      validateCommands(commands);  // line 291: executes this statement as part of this file's behavior
      project.commands = commands;  // line 292: executes this statement as part of this file's behavior
      project.updatedAt = new Date().toISOString();  // line 293: executes this statement as part of this file's behavior
      const audit = auditService.record({ action: "discord.commands.register", actorId: actor.id, projectId, resourceType: "command", resourceId: command.id ?? command.name, metadata: { name: command.name, description: command.description }, requestId });  // line 294: executes this statement as part of this file's behavior
      return writeJson(res, 201, { ...command, auditEventId: audit.id });  // line 295: executes this statement as part of this file's behavior
    }  // line 296: executes this statement as part of this file's behavior
    const index = project.commands.findIndex((command) => command.id === commandId);  // line 297: executes this statement as part of this file's behavior
    if (index < 0) throw notFoundCommand(commandId ?? "");  // line 298: executes this statement as part of this file's behavior
    if (method === "PATCH" && commandId) {  // line 299: executes this statement as part of this file's behavior
      const updated = parseCommandPatch(await readJson(req), project.commands[index]);  // line 300: executes this statement as part of this file's behavior
      const commands = [...project.commands];  // line 301: executes this statement as part of this file's behavior
      commands[index] = updated;  // line 302: executes this statement as part of this file's behavior
      validateCommands(commands);  // line 303: executes this statement as part of this file's behavior
      project.commands = commands;  // line 304: executes this statement as part of this file's behavior
      project.updatedAt = new Date().toISOString();  // line 305: executes this statement as part of this file's behavior
      return writeJson(res, 200, updated);  // line 306: executes this statement as part of this file's behavior
    }  // line 307: executes this statement as part of this file's behavior
    if (method === "DELETE" && commandId) {  // line 308: executes this statement as part of this file's behavior
      project.commands = project.commands.filter((command) => command.id !== commandId);  // line 309: executes this statement as part of this file's behavior
      project.updatedAt = new Date().toISOString();  // line 310: executes this statement as part of this file's behavior
      res.statusCode = 204;  // line 311: executes this statement as part of this file's behavior
      res.end();  // line 312: executes this statement as part of this file's behavior
      return;  // line 313: executes this statement as part of this file's behavior
    }  // line 314: executes this statement as part of this file's behavior
  }  // line 315: executes this statement as part of this file's behavior

  const githubProjectMatch = path.match(/^\/api\/projects\/([^/]+)\/github\/(create-repo|push|create-workflow)$/);  // line 317: executes this statement as part of this file's behavior
  if (githubProjectMatch) {  // line 318: executes this statement as part of this file's behavior
    const [, projectId, action] = githubProjectMatch;  // line 319: executes this statement as part of this file's behavior
    assertProjectAccess(actor, projectId);  // line 320: executes this statement as part of this file's behavior
    const project = projectStore.get(projectId);  // line 321: executes this statement as part of this file's behavior
    if (!project) throw notFoundProject(projectId);  // line 322: executes this statement as part of this file's behavior
    if (method === "POST" && action === "create-repo") return writeJson(res, 200, githubService.linkRepo(project, await readJson(req)));  // line 323: executes this statement as part of this file's behavior
    if (method === "POST" && action === "push") {  // line 324: executes this statement as part of this file's behavior
      const result = await githubService.push(project, requestId);  // line 325: executes this statement as part of this file's behavior
      return writeJson(res, 200, result);  // line 326: executes this statement as part of this file's behavior
    }  // line 327: executes this statement as part of this file's behavior
    if (method === "POST" && action === "create-workflow") return writeJson(res, 200, githubService.workflow(project));  // line 328: executes this statement as part of this file's behavior
  }  // line 329: executes this statement as part of this file's behavior

  const projectMatch = path.match(/^\/api\/projects\/([^/]+)(?:\/(archive|clone|validate|generate|regenerate))?$/);  // line 331: executes this statement as part of this file's behavior
  if (projectMatch) {  // line 332: executes this statement as part of this file's behavior
    const [, projectId, action] = projectMatch;  // line 333: executes this statement as part of this file's behavior
    assertProjectAccess(actor, projectId);  // line 334: executes this statement as part of this file's behavior
    if (method === "GET" && !action) return writeJson(res, 200, projectStore.get(projectId) ?? notFoundProject(projectId));  // line 335: executes this statement as part of this file's behavior
    if (method === "PATCH" && !action) {  // line 336: executes this statement as part of this file's behavior
      const input = parseUpdateProjectInput(await readJson(req));  // line 337: executes this statement as part of this file's behavior
      if (input.discord?.tokenSecretRef && !secretStore.has(input.discord.tokenSecretRef)) throw new RequestValidationError([{ field: "discord.tokenSecretRef", message: "Secret reference does not exist." }]);  // line 338: executes this statement as part of this file's behavior
      const project = projectStore.update(projectId, input) ?? notFoundProject(projectId);  // line 339: executes this statement as part of this file's behavior
      const audit = auditService.record({ action: "project.update", actorId: actor.id, projectId, resourceType: "project", resourceId: projectId, metadata: input as Record<string, unknown>, requestId });  // line 340: executes this statement as part of this file's behavior
      return writeJson(res, 200, { ...project, auditEventId: audit.id });  // line 341: executes this statement as part of this file's behavior
    }  // line 342: executes this statement as part of this file's behavior
    if (method === "DELETE" && !action) {  // line 343: executes this statement as part of this file's behavior
      if (!projectStore.delete(projectId)) throw notFoundProject(projectId);  // line 344: executes this statement as part of this file's behavior
      res.statusCode = 204;  // line 345: executes this statement as part of this file's behavior
      res.end();  // line 346: executes this statement as part of this file's behavior
      return;  // line 347: executes this statement as part of this file's behavior
    }  // line 348: executes this statement as part of this file's behavior
    if (method === "POST" && action === "archive") { const project = projectStore.archive(projectId) ?? notFoundProject(projectId); const audit = auditService.record({ action: "project.archive", actorId: actor.id, projectId, resourceType: "project", resourceId: projectId, metadata: { archivedAt: project.archivedAt }, requestId }); return writeJson(res, 200, { ...project, auditEventId: audit.id }); }  // line 349: executes this statement as part of this file's behavior
    if (method === "POST" && action === "clone") { const clone = projectStore.clone(projectId) ?? notFoundProject(projectId); const audit = auditService.record({ action: "project.clone", actorId: actor.id, projectId: clone.id, resourceType: "project", resourceId: clone.id, metadata: { sourceProjectId: projectId, name: clone.name, slug: clone.slug }, requestId }); return writeJson(res, 201, { ...clone, auditEventId: audit.id }); }  // line 350: executes this statement as part of this file's behavior
    if (method === "POST" && action === "validate") {  // line 351: executes this statement as part of this file's behavior
      const project = projectStore.get(projectId);  // line 352: executes this statement as part of this file's behavior
      if (!project) throw notFoundProject(projectId);  // line 353: executes this statement as part of this file's behavior
      return writeJson(res, 200, validateProject(project, (secretId) => secretStore.has(secretId)));  // line 354: executes this statement as part of this file's behavior
    }  // line 355: executes this statement as part of this file's behavior
    if (method === "POST" && (action === "generate" || action === "regenerate")) {  // line 356: executes this statement as part of this file's behavior
      assertExecutionAccess(actor, "project generation");  // line 357: executes this statement as part of this file's behavior
      const project = projectStore.get(projectId);  // line 358: executes this statement as part of this file's behavior
      if (!project) throw notFoundProject(projectId);  // line 359: executes this statement as part of this file's behavior
      const audit = auditService.record({ action: "generate.start", actorId: actor.id, projectId, resourceType: "project", resourceId: projectId, metadata: { force: action === "regenerate" }, requestId });  // line 360: executes this statement as part of this file's behavior
      const result = await fileService.generate(project, action === "regenerate");  // line 361: executes this statement as part of this file's behavior
      return writeJson(res, 200, { ...result, auditEventId: audit.id });  // line 362: executes this statement as part of this file's behavior
    }  // line 363: executes this statement as part of this file's behavior
  }  // line 364: executes this statement as part of this file's behavior
  throw { statusCode: 404, code: "NOT_FOUND", message: "Route not found.", details: {} };  // line 365: executes this statement as part of this file's behavior
}  // line 366: executes this statement as part of this file's behavior

async function readJson(req: IncomingMessage): Promise<unknown> {  // line 368: executes this statement as part of this file's behavior
  const maxBytes = 1024 * 1024;  // line 369: executes this statement as part of this file's behavior
  const chunks: Buffer[] = [];  // line 370: executes this statement as part of this file's behavior
  let size = 0;  // line 371: executes this statement as part of this file's behavior
  for await (const chunk of req) {  // line 372: executes this statement as part of this file's behavior
    const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);  // line 373: executes this statement as part of this file's behavior
    size += buffer.length;  // line 374: executes this statement as part of this file's behavior
    if (size > maxBytes) throw new RequestValidationError([{ field: "body", message: "Request body exceeds 1MB limit." }]);  // line 375: executes this statement as part of this file's behavior
    chunks.push(buffer);  // line 376: executes this statement as part of this file's behavior
  }  // line 377: executes this statement as part of this file's behavior
  const body = Buffer.concat(chunks).toString("utf8").trim();  // line 378: executes this statement as part of this file's behavior
  if (!body) return {};  // line 379: executes this statement as part of this file's behavior
  try { return JSON.parse(body) as unknown; } catch { throw new RequestValidationError([{ field: "body", message: "Body must be valid JSON." }]); }  // line 380: executes this statement as part of this file's behavior
}  // line 381: executes this statement as part of this file's behavior


function extractPathname(rawUrl: string | undefined): string {  // line 384: executes this statement as part of this file's behavior
  const candidate = typeof rawUrl === "string" && rawUrl.length > 0 ? rawUrl : "/";  // line 385: executes this statement as part of this file's behavior
  try {  // line 386: executes this statement as part of this file's behavior
    return new URL(candidate, "http://localhost").pathname;  // line 387: executes this statement as part of this file's behavior
  } catch {  // line 388: executes this statement as part of this file's behavior
    throw { statusCode: 400, code: "INVALID_REQUEST_URL", message: "Request URL is invalid.", details: {} };  // line 389: executes this statement as part of this file's behavior
  }  // line 390: executes this statement as part of this file's behavior
}  // line 391: executes this statement as part of this file's behavior

function writeJson(res: ServerResponse, statusCode: number, body: unknown): void {  // line 393: executes this statement as part of this file's behavior
  if (isHttpError(body)) throw body;  // line 394: executes this statement as part of this file's behavior
  res.statusCode = statusCode;  // line 395: executes this statement as part of this file's behavior
  res.end(redactSecrets(JSON.stringify(body)));  // line 396: executes this statement as part of this file's behavior
}  // line 397: executes this statement as part of this file's behavior

function writeError(res: ServerResponse, error: unknown, requestId: string): void {  // line 399: executes this statement as part of this file's behavior
  if (error instanceof RequestValidationError) return writeJson(res, 400, { error: { code: "VALIDATION_FAILED", message: redactSecrets(error.message), details: { problems: error.problems.map((problem) => ({ ...problem, message: redactSecrets(problem.message) })) }, requestId } });  // line 400: executes this statement as part of this file's behavior
  if (error instanceof DuplicateSlugError) return writeJson(res, 409, { error: { code: "DUPLICATE_PROJECT_SLUG", message: redactSecrets(error.message), details: {}, requestId } });  // line 401: executes this statement as part of this file's behavior
  const statusCode = isHttpError(error) ? error.statusCode : 500;  // line 402: executes this statement as part of this file's behavior
  const code = isHttpError(error) ? error.code : "INTERNAL_SERVER_ERROR";  // line 403: executes this statement as part of this file's behavior
  const message = redactSecrets(isHttpError(error) ? error.message : "Unexpected backend error.");  // line 404: executes this statement as part of this file's behavior
  const details = isHttpError(error) ? error.details : {};  // line 405: executes this statement as part of this file's behavior
  res.statusCode = statusCode;  // line 406: executes this statement as part of this file's behavior
  res.end(redactSecrets(JSON.stringify({ error: { code, message, details, requestId } })));  // line 407: executes this statement as part of this file's behavior
}  // line 408: executes this statement as part of this file's behavior

function notFoundProject(projectId: string): never {  // line 410: executes this statement as part of this file's behavior
  throw { statusCode: 404, code: "NOT_FOUND", message: `Project '${projectId}' was not found.`, details: {} };  // line 411: executes this statement as part of this file's behavior
}  // line 412: executes this statement as part of this file's behavior

function notFoundSecret(secretId: string): never {  // line 414: executes this statement as part of this file's behavior
  throw { statusCode: 404, code: "NOT_FOUND", message: `Secret '${secretId}' was not found.`, details: {} };  // line 415: executes this statement as part of this file's behavior
}  // line 416: executes this statement as part of this file's behavior

function notFoundBuild(buildId: string): never {  // line 418: executes this statement as part of this file's behavior
  throw { statusCode: 404, code: "NOT_FOUND", message: `Build '${buildId}' was not found.`, details: {} };  // line 419: executes this statement as part of this file's behavior
}  // line 420: executes this statement as part of this file's behavior

function notFoundTarget(targetId: string): never {  // line 422: executes this statement as part of this file's behavior
  throw { statusCode: 404, code: "NOT_FOUND", message: `Deployment target '${targetId}' was not found.`, details: {} };  // line 423: executes this statement as part of this file's behavior
}  // line 424: executes this statement as part of this file's behavior

function notFoundDeployment(deploymentId: string): never {  // line 426: executes this statement as part of this file's behavior
  throw { statusCode: 404, code: "NOT_FOUND", message: `Deployment '${deploymentId}' was not found.`, details: {} };  // line 427: executes this statement as part of this file's behavior
}  // line 428: executes this statement as part of this file's behavior

function notFoundCommand(commandId: string): never {  // line 430: executes this statement as part of this file's behavior
  throw { statusCode: 404, code: "NOT_FOUND", message: `Command '${commandId}' was not found.`, details: {} };  // line 431: executes this statement as part of this file's behavior
}  // line 432: executes this statement as part of this file's behavior

function isHttpError(value: unknown): value is { statusCode: number; code: string; message: string; details: unknown } {  // line 434: executes this statement as part of this file's behavior
  return Boolean(value && typeof value === "object" && "statusCode" in value && "code" in value && "message" in value);  // line 435: executes this statement as part of this file's behavior
}  // line 436: executes this statement as part of this file's behavior


function handleLogsUpgrade(req: IncomingMessage, socket: Socket) {  // line 439: executes this statement as part of this file's behavior
  if (extractPathname(req.url) !== "/logs") { socket.destroy(); return; }  // line 440: executes this statement as part of this file's behavior
  const key = req.headers["sec-websocket-key"];  // line 441: executes this statement as part of this file's behavior
  if (typeof key !== "string") { socket.destroy(); return; }  // line 442: executes this statement as part of this file's behavior
  const accept = crypto.createHash("sha1").update(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").digest("base64" as any);  // line 443: executes this statement as part of this file's behavior
  socket.write([  // line 444: executes this statement as part of this file's behavior
    "HTTP/1.1 101 Switching Protocols",  // line 445: executes this statement as part of this file's behavior
    "Upgrade: websocket",  // line 446: executes this statement as part of this file's behavior
    "Connection: Upgrade",  // line 447: executes this statement as part of this file's behavior
    `Sec-WebSocket-Accept: ${accept}`,  // line 448: executes this statement as part of this file's behavior
    "",  // line 449: executes this statement as part of this file's behavior
    ""  // line 450: executes this statement as part of this file's behavior
  ].join("\r\n"));  // line 451: executes this statement as part of this file's behavior
  logClients.add(socket);  // line 452: executes this statement as part of this file's behavior
  socket.on("close", () => logClients.delete(socket));  // line 453: executes this statement as part of this file's behavior
  socket.on("error", () => logClients.delete(socket));  // line 454: executes this statement as part of this file's behavior
}  // line 455: executes this statement as part of this file's behavior

function broadcastLogLine(line: string) {  // line 457: executes this statement as part of this file's behavior
  const payload = Buffer.from(line, "utf8");  // line 458: executes this statement as part of this file's behavior
  const header = payload.length < 126 ? Buffer.from([0x81, payload.length]) : Buffer.from([0x81, 126, (payload.length >> 8) & 0xff, payload.length & 0xff]);  // line 459: executes this statement as part of this file's behavior
  const frame = Buffer.concat([header, payload]);  // line 460: executes this statement as part of this file's behavior
  for (const client of logClients) {  // line 461: executes this statement as part of this file's behavior
    if (!client.destroyed) client.write(frame);  // line 462: executes this statement as part of this file's behavior
  }  // line 463: executes this statement as part of this file's behavior
}  // line 464: executes this statement as part of this file's behavior

const originalInfo = console.info.bind(console);  // line 466: executes this statement as part of this file's behavior
const originalError = console.error.bind(console);  // line 467: executes this statement as part of this file's behavior
console.info = (...args: unknown[]) => { originalInfo(...args); broadcastLogLine(args.map(String).join(" ")); };  // line 468: executes this statement as part of this file's behavior
console.error = (...args: unknown[]) => { originalError(...args); broadcastLogLine(args.map(String).join(" ")); };  // line 469: executes this statement as part of this file's behavior
