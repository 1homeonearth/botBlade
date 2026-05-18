import fs from "node:fs/promises";
import path from "node:path";
import type { BotProject } from "../models/project.js";
import type { BuildJob, CommandRunner } from "./buildService.js";
import type { DeploymentTarget, DeploymentTargetType } from "./deploymentTargets.js";
import type { LocalProcessRuntimeService, RuntimeStatus } from "./localProcessRuntimeService.js";

export type DeploymentAction = "deploy" | "status" | "start" | "stop" | "restart" | "logs" | "rollback";

export interface DeploymentAdapterCapabilities {
  targetType: DeploymentTargetType;
  supported: boolean;
  actions: Record<DeploymentAction, boolean>;
  notes: string[];
}

export interface ResolvedSecret {
  id: string;
  name: string;
  value: string;
}

export interface DeploymentAdapter {
  capabilities: DeploymentAdapterCapabilities;
  validateTarget(target: DeploymentTarget): Promise<void> | void;
  prepare(context: DeploymentAdapterContext): Promise<string[]>;
  deploy(context: DeploymentAdapterContext): Promise<string[]>;
  status(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string; running?: boolean }>;
  start(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string; running?: boolean }>;
  stop(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string; running?: boolean }>;
  restart(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string; running?: boolean }>;
  logs(context: DeploymentAdapterContext): Promise<string>;
  rollback?(context: DeploymentAdapterContext): Promise<string[]>;
}

export interface DeploymentAdapterContext {
  project: BotProject;
  target: DeploymentTarget;
  build: BuildJob;
  runtime: LocalProcessRuntimeService;
  resolveSecretRef: (secretId: string) => ResolvedSecret | undefined;
  previousBuild?: BuildJob;
  commandRunner: CommandRunner;
}

export const localProcessCapabilities: DeploymentAdapterCapabilities = {
  targetType: "local_process",
  supported: true,
  actions: { deploy: true, status: true, start: true, stop: true, restart: true, logs: true, rollback: false },
  notes: ["Runs the generated bot as a backend child process for development.", "Rollback is unsupported because local_process does not maintain versioned releases."],
};

export const localDockerCapabilities: DeploymentAdapterCapabilities = {
  targetType: "local_docker",
  supported: true,
  actions: { deploy: true, status: true, start: true, stop: true, restart: true, logs: true, rollback: true },
  notes: ["Builds a Docker image from the generated bot workspace and runs it on the local Docker daemon.", "Secrets are resolved from secretRefs into a temporary env file; secret values are never read from target config."],
};

export function capabilitiesForTargetType(type: DeploymentTargetType): DeploymentAdapterCapabilities {
  return type === "local_process" ? localProcessCapabilities : localDockerCapabilities;
}

export class LocalProcessDeploymentAdapter implements DeploymentAdapter {
  readonly capabilities = localProcessCapabilities;

  validateTarget(target: DeploymentTarget): void {
    if (target.type !== "local_process") throw structuredError("INVALID_TARGET_TYPE", "local_process adapter requires a local_process target.");
  }

  async prepare(): Promise<string[]> {
    return ["Prepared local process deployment using generated project files."];
  }

  async deploy(context: DeploymentAdapterContext): Promise<string[]> {
    return [`Build ${context.build.buildId} is now deployable by the local process runtime.`];
  }

  async status(context: DeploymentAdapterContext): Promise<RuntimeStatus> { return context.runtime.getStatus(context.project.id); }
  async start(context: DeploymentAdapterContext): Promise<RuntimeStatus> { return context.runtime.start(context.project); }
  async stop(context: DeploymentAdapterContext): Promise<RuntimeStatus> { return context.runtime.stop(context.project.id); }
  async restart(context: DeploymentAdapterContext): Promise<RuntimeStatus> { return context.runtime.restart(context.project); }
  async logs(context: DeploymentAdapterContext): Promise<string> { return context.runtime.getLogs(context.project.id); }
}

export class LocalDockerDeploymentAdapter implements DeploymentAdapter {
  readonly capabilities = localDockerCapabilities;

  constructor(private readonly commandRunner: CommandRunner) {}

  validateTarget(target: DeploymentTarget): void {
    if (target.type !== "local_docker") throw structuredError("INVALID_TARGET_TYPE", "local_docker adapter requires a local_docker target.");
  }

  async prepare(context: DeploymentAdapterContext): Promise<string[]> {
    const artifact = context.build.artifactPath ? ` using artifact ${context.build.artifactPath}` : "";
    return [`Preparing Docker deployment for ${dockerImage(context)}${artifact}.`];
  }

  async deploy(context: DeploymentAdapterContext): Promise<string[]> {
    const cwd = generatedWorkspace(context.project.id);
    await this.commandRunner("docker", ["build", "-t", taggedImage(context), "-f", dockerfile(context), "."], cwd, () => undefined);
    const envFile = await writeSecretEnvFile(context);
    await removeContainer(context);
    try {
      await this.commandRunner("docker", ["run", "-d", "--name", containerName(context), "--restart", "unless-stopped", "--env-file", envFile, taggedImage(context)], cwd, () => undefined);
    } finally {
      await fs.rm(envFile, { force: true });
    }
    return [`Built Docker image ${taggedImage(context)}.`, `Started container ${containerName(context)} with ${context.target.secretRefs.length} target secret reference(s).`];
  }

  async status(context: DeploymentAdapterContext): Promise<{ status: string; message: string; running: boolean }> {
    const lines: string[] = [];
    try {
      await this.commandRunner("docker", ["inspect", "--format", "{{.State.Status}}", containerName(context)], generatedWorkspace(context.project.id), (line) => lines.push(line));
      const status = lines.join("\n").trim() || "unknown";
      return { status, running: status === "running", message: `Docker container ${containerName(context)} is ${status}.` };
    } catch (error) {
      return { status: "missing", running: false, message: error instanceof Error ? error.message : String(error) };
    }
  }

  async start(context: DeploymentAdapterContext): Promise<{ status: string; message: string; running: boolean }> {
    await this.commandRunner("docker", ["start", containerName(context)], generatedWorkspace(context.project.id), () => undefined);
    return { status: "running", running: true, message: `Started Docker container ${containerName(context)}.` };
  }

  async stop(context: DeploymentAdapterContext): Promise<{ status: string; message: string; running: boolean }> {
    await this.commandRunner("docker", ["stop", containerName(context)], generatedWorkspace(context.project.id), () => undefined);
    return { status: "stopped", running: false, message: `Stopped Docker container ${containerName(context)}.` };
  }

  async restart(context: DeploymentAdapterContext): Promise<{ status: string; message: string; running: boolean }> {
    await this.commandRunner("docker", ["restart", containerName(context)], generatedWorkspace(context.project.id), () => undefined);
    return { status: "running", running: true, message: `Restarted Docker container ${containerName(context)}.` };
  }

  async logs(context: DeploymentAdapterContext): Promise<string> {
    const lines: string[] = [];
    await this.commandRunner("docker", ["logs", "--tail", "200", containerName(context)], generatedWorkspace(context.project.id), (line) => lines.push(line));
    return lines.join("\n");
  }

  async rollback(context: DeploymentAdapterContext): Promise<string[]> {
    if (!context.previousBuild) throw structuredError("ROLLBACK_UNAVAILABLE", "No previous successful deployment is available for rollback.");
    const previousContext = { ...context, build: context.previousBuild };
    const envFile = await writeSecretEnvFile(context);
    await removeContainer(context);
    try {
      await this.commandRunner("docker", ["run", "-d", "--name", containerName(context), "--restart", "unless-stopped", "--env-file", envFile, taggedImage(previousContext)], generatedWorkspace(context.project.id), () => undefined);
    } finally {
      await fs.rm(envFile, { force: true });
    }
    return [`Rolled back ${containerName(context)} to Docker image ${taggedImage(previousContext)}.`];
  }
}

export function adapterFor(target: DeploymentTarget, commandRunner: CommandRunner): DeploymentAdapter {
  return target.type === "local_process" ? new LocalProcessDeploymentAdapter() : new LocalDockerDeploymentAdapter(commandRunner);
}

function generatedWorkspace(projectId: string): string {
  return path.join(process.cwd(), "generated-projects", projectId);
}

function dockerImage(context: DeploymentAdapterContext): string {
  const configured = context.target.config.image;
  return typeof configured === "string" && configured.trim() ? configured.trim() : `royalscepter/${context.project.slug}`;
}

function taggedImage(context: DeploymentAdapterContext): string {
  return `${dockerImage(context)}:${context.build.buildId}`;
}

function dockerfile(context: DeploymentAdapterContext): string {
  const configured = context.target.config.dockerfile;
  return typeof configured === "string" && configured.trim() ? configured.trim() : "Dockerfile";
}

function containerName(context: DeploymentAdapterContext): string {
  return `royalscepter-${context.project.slug}`.replace(/[^a-zA-Z0-9_.-]/g, "-").slice(0, 128);
}

async function removeContainer(context: DeploymentAdapterContext): Promise<void> {
  try {
    await context.commandRunner("docker", ["rm", "-f", containerName(context)], generatedWorkspace(context.project.id), () => undefined);
  } catch {
    // docker rm -f fails when the container does not exist; deploy can continue.
  }
}

async function writeSecretEnvFile(context: DeploymentAdapterContext): Promise<string> {
  const env: Record<string, string> = {};
  for (const secretId of context.target.secretRefs) {
    const secret = context.resolveSecretRef(secretId);
    if (!secret) throw structuredError("SECRET_REF_NOT_FOUND", `Secret reference '${secretId}' was not found.`);
    env[envName(secret.name)] = secret.value;
  }
  if (context.project.discord.tokenSecretRef) {
    const secret = context.resolveSecretRef(context.project.discord.tokenSecretRef);
    if (!secret) throw structuredError("SECRET_REF_NOT_FOUND", `Secret reference '${context.project.discord.tokenSecretRef}' was not found.`);
    env.DISCORD_TOKEN = secret.value;
  }
  const envFile = path.join(generatedWorkspace(context.project.id), `.royalscepter-deploy-${context.build.buildId}.env`);
  await fs.writeFile(envFile, Object.entries(env).map(([key, value]) => `${key}=${escapeEnvValue(value)}`).join("\n"), "utf8");
  return envFile;
}

function envName(name: string): string {
  return name.trim().replace(/[^A-Za-z0-9_]/g, "_").replace(/^([0-9])/, "_$1").toUpperCase();
}

function escapeEnvValue(value: string): string {
  return value.replace(/\r?\n/g, "\\n");
}

function structuredError(code: string, message: string): never {
  throw { statusCode: 400, code, message, details: {} };
}
