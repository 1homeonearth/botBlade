import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
import crypto from "node:crypto";
import { API_VERSION, SERVICE_NAME, SERVICE_VERSION } from "./models/project.js";
import { BuildService } from "./services/buildService.js";
import { parseCommandDefinition, parseCommandPatch, validateCommands } from "./services/commandDefinitions.js";
import { DeploymentJobStore } from "./services/deploymentJobs.js";
import { DeploymentTargetStore, testDeploymentTarget } from "./services/deploymentTargets.js";
import { GitHubIntegrationService } from "./services/githubIntegration.js";
import { LocalProcessRuntimeService } from "./services/localProcessRuntimeService.js";
import { ProjectFileService, parseFileWriteInput } from "./services/projectFiles.js";
import { DuplicateSlugError, parseCreateProjectInput, parseToggleAction, parseUpdateProjectInput, ProjectStore, RequestValidationError } from "./services/projectStore.js";
import { redactSecrets } from "./services/redaction.js";
import { parseCreateSecretInput, parseRotateSecretInput, parseUpdateSecretInput, SecretStore } from "./services/secretStore.js";
import { validateProject } from "./services/projectValidation.js";

const projectStore = new ProjectStore();
const secretStore = new SecretStore();
const fileService = new ProjectFileService();
const buildService = new BuildService(fileService, (secretId) => secretStore.has(secretId));
const runtimeService = new LocalProcessRuntimeService(fileService, (secretId) => secretStore.getValue(secretId));
const targetStore = new DeploymentTargetStore();
const deploymentStore = new DeploymentJobStore(buildService, targetStore, runtimeService);
const githubService = new GitHubIntegrationService((secretId) => secretStore.has(secretId));
const port = Number(process.env.PORT ?? 8000);

createServer(async (req, res) => {
  const requestId = typeof req.headers["x-request-id"] === "string" ? req.headers["x-request-id"] : crypto.randomUUID();
  res.setHeader("x-request-id", requestId);
  res.setHeader("content-type", "application/json");
  try {
    await handleRequest(req, res);
    console.info(redactSecrets(JSON.stringify({ level: "info", requestId, method: req.method, url: req.url })));
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

  if (method === "GET" && path === "/api/github/status") return writeJson(res, 200, githubService.status());
  if (method === "POST" && path === "/api/github/connect") return writeJson(res, 200, githubService.connect(await readJson(req)));

  if (method === "GET" && path === "/api/deployment-targets") return writeJson(res, 200, { targets: targetStore.list() });
  if (method === "POST" && path === "/api/deployment-targets") return writeJson(res, 201, targetStore.create(await readJson(req)));
  const targetMatch = path.match(/^\/api\/deployment-targets\/([^/]+)(?:\/(test))?$/);
  if (targetMatch) {
    const [, targetId, action] = targetMatch;
    const target = targetStore.get(targetId);
    if (!target) throw notFoundTarget(targetId);
    if (method === "GET" && !action) return writeJson(res, 200, target);
    if (method === "PATCH" && !action) return writeJson(res, 200, targetStore.update(targetId, await readJson(req)) ?? notFoundTarget(targetId));
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
    return writeJson(res, 200, project ? runtimeService.getStatus(project.id) : { projectId: "default", status: "stopped", running: false, pid: null, startedAt: null, lastExitCode: null, message: "No active/default project exists." });
  }
  if (method === "POST" && path === "/api/bot-toggle/") {
    const project = projectStore.list()[0];
    if (!project) throw { statusCode: 400, code: "NO_ACTIVE_PROJECT", message: "Create or select a project before toggling the bot runtime.", details: {} };
    const action = parseToggleAction(await readJson(req));
    return writeJson(res, 200, action === "start" ? await runtimeService.start(project) : await runtimeService.stop(project.id));
  }

  if (method === "GET" && path === "/api/secrets") return writeJson(res, 200, { secrets: secretStore.list() });
  if (method === "POST" && path === "/api/secrets") return writeJson(res, 201, secretStore.create(parseCreateSecretInput(await readJson(req))));
  const secretMatch = path.match(/^\/api\/secrets\/([^/]+)(?:\/(rotate))?$/);
  if (secretMatch) {
    const [, secretId, action] = secretMatch;
    if (method === "GET" && !action) return writeJson(res, 200, secretStore.get(secretId) ?? notFoundSecret(secretId));
    if (method === "PATCH" && !action) return writeJson(res, 200, secretStore.update(secretId, parseUpdateSecretInput(await readJson(req))) ?? notFoundSecret(secretId));
    if (method === "DELETE" && !action) {
      if (!secretStore.delete(secretId)) throw notFoundSecret(secretId);
      res.statusCode = 204;
      res.end();
      return;
    }
    if (method === "POST" && action === "rotate") return writeJson(res, 200, secretStore.rotate(secretId, parseRotateSecretInput(await readJson(req))) ?? notFoundSecret(secretId));
  }

  if (method === "GET" && path === "/api/projects") return writeJson(res, 200, { projects: projectStore.list() });
  if (method === "POST" && path === "/api/projects") {
    const input = parseCreateProjectInput(await readJson(req));
    if (input.discord?.tokenSecretRef && !secretStore.has(input.discord.tokenSecretRef)) throw new RequestValidationError([{ field: "discord.tokenSecretRef", message: "Secret reference does not exist." }]);
    return writeJson(res, 201, projectStore.create(input));
  }

  const fileMatch = path.match(/^\/api\/projects\/([^/]+)\/files(?:\/(.*))?$/);
  if (fileMatch) {
    const [, projectId, filePath] = fileMatch;
    const project = projectStore.get(projectId);
    if (!project) throw notFoundProject(projectId);
    if (method === "GET" && filePath === undefined) return writeJson(res, 200, { files: await fileService.list(projectId) });
    if (method === "GET" && filePath !== undefined) return writeJson(res, 200, await fileService.read(projectId, filePath));
    if (method === "PUT" && filePath !== undefined) return writeJson(res, 200, await fileService.write(projectId, filePath, parseFileWriteInput(await readJson(req)).content));
  }

  const buildMatch = path.match(/^\/api\/projects\/([^/]+)\/builds(?:\/([^/]+)(?:\/(logs))?)?$/);
  if (buildMatch) {
    const [, projectId, buildId, logs] = buildMatch;
    const project = projectStore.get(projectId);
    if (!project) throw notFoundProject(projectId);
    if (method === "POST" && !buildId) return writeJson(res, 201, await buildService.create(project, await readJson(req)));
    if (method === "GET" && !buildId) return writeJson(res, 200, { builds: buildService.list(projectId) });
    if (method === "GET" && buildId && !logs) return writeJson(res, 200, buildService.get(projectId, buildId) ?? notFoundBuild(buildId));
    if (method === "GET" && buildId && logs) return writeJson(res, 200, { logs: buildService.getLogs(projectId, buildId) ?? notFoundBuild(buildId) });
  }

  const runtimeMatch = path.match(/^\/api\/projects\/([^/]+)\/runtime\/(status|start|stop|restart|logs)$/);
  if (runtimeMatch) {
    const [, projectId, action] = runtimeMatch;
    const project = projectStore.get(projectId);
    if (!project) throw notFoundProject(projectId);
    if (method === "GET" && action === "status") return writeJson(res, 200, runtimeService.getStatus(projectId));
    if (method === "POST" && action === "start") return writeJson(res, 200, await runtimeService.start(project));
    if (method === "POST" && action === "stop") return writeJson(res, 200, await runtimeService.stop(projectId));
    if (method === "POST" && action === "restart") return writeJson(res, 200, await runtimeService.restart(project));
    if (method === "GET" && action === "logs") return writeJson(res, 200, { logs: runtimeService.getLogs(projectId) });
  }

  const deploymentMatch = path.match(/^\/api\/projects\/([^/]+)\/deployments(?:\/([^/]+)(?:\/(logs|rollback))?)?$/);
  if (deploymentMatch) {
    const [, projectId, deploymentId, action] = deploymentMatch;
    const project = projectStore.get(projectId);
    if (!project) throw notFoundProject(projectId);
    if (method === "POST" && !deploymentId) return writeJson(res, 201, await deploymentStore.create(project, await readJson(req)));
    if (method === "GET" && !deploymentId) return writeJson(res, 200, { deployments: deploymentStore.list(projectId) });
    if (method === "GET" && deploymentId && !action) return writeJson(res, 200, deploymentStore.get(projectId, deploymentId) ?? notFoundDeployment(deploymentId));
    if (method === "GET" && deploymentId && action === "logs") return writeJson(res, 200, { logs: deploymentStore.getLogs(projectId, deploymentId) ?? notFoundDeployment(deploymentId) });
    if (method === "POST" && deploymentId && action === "rollback") return writeJson(res, 200, deploymentStore.rollback(project, deploymentId));
  }

  const commandsMatch = path.match(/^\/api\/projects\/([^/]+)\/commands(?:\/([^/]+))?$/);
  if (commandsMatch) {
    const [, projectId, commandId] = commandsMatch;
    const project = projectStore.get(projectId);
    if (!project) throw notFoundProject(projectId);
    if (method === "GET" && !commandId) return writeJson(res, 200, { commands: project.commands });
    if (method === "POST" && !commandId) {
      const command = parseCommandDefinition(await readJson(req));
      const commands = [...project.commands, command];
      validateCommands(commands);
      project.commands = commands;
      project.updatedAt = new Date().toISOString();
      return writeJson(res, 201, command);
    }
    const index = project.commands.findIndex((command) => command.id === commandId);
    if (index < 0) throw notFoundCommand(commandId ?? "");
    if (method === "PATCH" && commandId) {
      const updated = parseCommandPatch(await readJson(req), project.commands[index]);
      const commands = [...project.commands];
      commands[index] = updated;
      validateCommands(commands);
      project.commands = commands;
      project.updatedAt = new Date().toISOString();
      return writeJson(res, 200, updated);
    }
    if (method === "DELETE" && commandId) {
      project.commands = project.commands.filter((command) => command.id !== commandId);
      project.updatedAt = new Date().toISOString();
      res.statusCode = 204;
      res.end();
      return;
    }
  }

  const githubProjectMatch = path.match(/^\/api\/projects\/([^/]+)\/github\/(create-repo|push|create-workflow)$/);
  if (githubProjectMatch) {
    const [, projectId, action] = githubProjectMatch;
    const project = projectStore.get(projectId);
    if (!project) throw notFoundProject(projectId);
    if (method === "POST" && action === "create-repo") return writeJson(res, 200, githubService.linkRepo(project, await readJson(req)));
    if (method === "POST" && action === "push") return writeJson(res, 200, githubService.push(project));
    if (method === "POST" && action === "create-workflow") return writeJson(res, 200, githubService.workflow(project));
  }

  const projectMatch = path.match(/^\/api\/projects\/([^/]+)(?:\/(archive|clone|validate|generate|regenerate))?$/);
  if (projectMatch) {
    const [, projectId, action] = projectMatch;
    if (method === "GET" && !action) return writeJson(res, 200, projectStore.get(projectId) ?? notFoundProject(projectId));
    if (method === "PATCH" && !action) {
      const input = parseUpdateProjectInput(await readJson(req));
      if (input.discord?.tokenSecretRef && !secretStore.has(input.discord.tokenSecretRef)) throw new RequestValidationError([{ field: "discord.tokenSecretRef", message: "Secret reference does not exist." }]);
      return writeJson(res, 200, projectStore.update(projectId, input) ?? notFoundProject(projectId));
    }
    if (method === "DELETE" && !action) {
      if (!projectStore.delete(projectId)) throw notFoundProject(projectId);
      res.statusCode = 204;
      res.end();
      return;
    }
    if (method === "POST" && action === "archive") return writeJson(res, 200, projectStore.archive(projectId) ?? notFoundProject(projectId));
    if (method === "POST" && action === "clone") return writeJson(res, 201, projectStore.clone(projectId) ?? notFoundProject(projectId));
    if (method === "POST" && action === "validate") {
      const project = projectStore.get(projectId);
      if (!project) throw notFoundProject(projectId);
      return writeJson(res, 200, validateProject(project, (secretId) => secretStore.has(secretId)));
    }
    if (method === "POST" && (action === "generate" || action === "regenerate")) {
      const project = projectStore.get(projectId);
      if (!project) throw notFoundProject(projectId);
      return writeJson(res, 200, await fileService.generate(project, action === "regenerate"));
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
  res.end(redactSecrets(JSON.stringify(body)));
}

function writeError(res: ServerResponse, error: unknown, requestId: string): void {
  if (error instanceof RequestValidationError) return writeJson(res, 400, { error: { code: "VALIDATION_FAILED", message: redactSecrets(error.message), details: { problems: error.problems.map((problem) => ({ ...problem, message: redactSecrets(problem.message) })) }, requestId } });
  if (error instanceof DuplicateSlugError) return writeJson(res, 409, { error: { code: "DUPLICATE_PROJECT_SLUG", message: redactSecrets(error.message), details: {}, requestId } });
  const statusCode = isHttpError(error) ? error.statusCode : 500;
  const code = isHttpError(error) ? error.code : "INTERNAL_SERVER_ERROR";
  const message = redactSecrets(isHttpError(error) ? error.message : "Unexpected backend error.");
  const details = isHttpError(error) ? error.details : {};
  res.statusCode = statusCode;
  res.end(redactSecrets(JSON.stringify({ error: { code, message, details, requestId } })));
}

function notFoundProject(projectId: string): never {
  throw { statusCode: 404, code: "NOT_FOUND", message: `Project '${projectId}' was not found.`, details: {} };
}

function notFoundSecret(secretId: string): never {
  throw { statusCode: 404, code: "NOT_FOUND", message: `Secret '${secretId}' was not found.`, details: {} };
}

function notFoundBuild(buildId: string): never {
  throw { statusCode: 404, code: "NOT_FOUND", message: `Build '${buildId}' was not found.`, details: {} };
}

function notFoundTarget(targetId: string): never {
  throw { statusCode: 404, code: "NOT_FOUND", message: `Deployment target '${targetId}' was not found.`, details: {} };
}

function notFoundDeployment(deploymentId: string): never {
  throw { statusCode: 404, code: "NOT_FOUND", message: `Deployment '${deploymentId}' was not found.`, details: {} };
}

function notFoundCommand(commandId: string): never {
  throw { statusCode: 404, code: "NOT_FOUND", message: `Command '${commandId}' was not found.`, details: {} };
}

function isHttpError(value: unknown): value is { statusCode: number; code: string; message: string; details: unknown } {
  return Boolean(value && typeof value === "object" && "statusCode" in value && "code" in value && "message" in value);
}
