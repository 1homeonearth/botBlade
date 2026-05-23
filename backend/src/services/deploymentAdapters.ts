// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import fs from "node:fs/promises";  // line 7: executes this statement as part of this file's behavior
import path from "node:path";  // line 8: executes this statement as part of this file's behavior
import type { BotProject } from "../models/project.js";  // line 9: executes this statement as part of this file's behavior
import type { BuildJob, CommandRunner } from "./buildService.js";  // line 10: executes this statement as part of this file's behavior
import type { DeploymentTarget, DeploymentTargetType } from "./deploymentTargets.js";  // line 11: executes this statement as part of this file's behavior
import type { LocalProcessRuntimeService, RuntimeStatus } from "./localProcessRuntimeService.js";  // line 12: executes this statement as part of this file's behavior

export type DeploymentAction = "deploy" | "status" | "start" | "stop" | "restart" | "logs" | "rollback";  // line 14: executes this statement as part of this file's behavior

export interface DeploymentAdapterCapabilities {  // line 16: executes this statement as part of this file's behavior
  targetType: DeploymentTargetType;  // line 17: executes this statement as part of this file's behavior
  supported: boolean;  // line 18: executes this statement as part of this file's behavior
  actions: Record<DeploymentAction, boolean>;  // line 19: executes this statement as part of this file's behavior
  notes: string[];  // line 20: executes this statement as part of this file's behavior
}  // line 21: executes this statement as part of this file's behavior

export interface ResolvedSecret {  // line 23: executes this statement as part of this file's behavior
  id: string;  // line 24: executes this statement as part of this file's behavior
  name: string;  // line 25: executes this statement as part of this file's behavior
  value: string;  // line 26: executes this statement as part of this file's behavior
}  // line 27: executes this statement as part of this file's behavior

export interface DeploymentAdapter {  // line 29: executes this statement as part of this file's behavior
  capabilities: DeploymentAdapterCapabilities;  // line 30: executes this statement as part of this file's behavior
  validateTarget(target: DeploymentTarget): Promise<void> | void;  // line 31: executes this statement as part of this file's behavior
  prepare(context: DeploymentAdapterContext): Promise<string[]>;  // line 32: executes this statement as part of this file's behavior
  deploy(context: DeploymentAdapterContext): Promise<string[]>;  // line 33: executes this statement as part of this file's behavior
  status(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string; running?: boolean }>;  // line 34: executes this statement as part of this file's behavior
  start(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string; running?: boolean }>;  // line 35: executes this statement as part of this file's behavior
  stop(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string; running?: boolean }>;  // line 36: executes this statement as part of this file's behavior
  restart(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string; running?: boolean }>;  // line 37: executes this statement as part of this file's behavior
  logs(context: DeploymentAdapterContext): Promise<string>;  // line 38: executes this statement as part of this file's behavior
  rollback?(context: DeploymentAdapterContext): Promise<string[]>;  // line 39: executes this statement as part of this file's behavior
}  // line 40: executes this statement as part of this file's behavior

export interface DeploymentAdapterContext {  // line 42: executes this statement as part of this file's behavior
  project: BotProject;  // line 43: executes this statement as part of this file's behavior
  target: DeploymentTarget;  // line 44: executes this statement as part of this file's behavior
  build: BuildJob;  // line 45: executes this statement as part of this file's behavior
  runtime: LocalProcessRuntimeService;  // line 46: executes this statement as part of this file's behavior
  resolveSecretRef: (secretId: string) => ResolvedSecret | undefined;  // line 47: executes this statement as part of this file's behavior
  previousBuild?: BuildJob;  // line 48: executes this statement as part of this file's behavior
  commandRunner: CommandRunner;  // line 49: executes this statement as part of this file's behavior
}  // line 50: executes this statement as part of this file's behavior

export const localProcessCapabilities: DeploymentAdapterCapabilities = {  // line 52: executes this statement as part of this file's behavior
  targetType: "local_process",  // line 53: executes this statement as part of this file's behavior
  supported: true,  // line 54: executes this statement as part of this file's behavior
  actions: { deploy: true, status: true, start: true, stop: true, restart: true, logs: true, rollback: false },  // line 55: executes this statement as part of this file's behavior
  notes: ["Runs the generated bot as a backend child process for development.", "Rollback is unsupported because local_process does not maintain versioned releases."],  // line 56: executes this statement as part of this file's behavior
};  // line 57: executes this statement as part of this file's behavior

export const localDockerCapabilities: DeploymentAdapterCapabilities = {  // line 59: executes this statement as part of this file's behavior
  targetType: "local_docker",  // line 60: executes this statement as part of this file's behavior
  supported: true,  // line 61: executes this statement as part of this file's behavior
  actions: { deploy: true, status: true, start: true, stop: true, restart: true, logs: true, rollback: true },  // line 62: executes this statement as part of this file's behavior
  notes: ["Builds a Docker image from the generated bot workspace and runs it on the local Docker daemon.", "Secrets are resolved from secretRefs into a temporary env file; secret values are never read from target config."],  // line 63: executes this statement as part of this file's behavior
};  // line 64: executes this statement as part of this file's behavior

export function capabilitiesForTargetType(type: DeploymentTargetType): DeploymentAdapterCapabilities {  // line 66: executes this statement as part of this file's behavior
  return type === "local_process" ? localProcessCapabilities : localDockerCapabilities;  // line 67: executes this statement as part of this file's behavior
}  // line 68: executes this statement as part of this file's behavior

export class LocalProcessDeploymentAdapter implements DeploymentAdapter {  // line 70: executes this statement as part of this file's behavior
  readonly capabilities = localProcessCapabilities;  // line 71: executes this statement as part of this file's behavior

  validateTarget(target: DeploymentTarget): void {  // line 73: executes this statement as part of this file's behavior
    if (target.type !== "local_process") throw structuredError("INVALID_TARGET_TYPE", "local_process adapter requires a local_process target.");  // line 74: executes this statement as part of this file's behavior
  }  // line 75: executes this statement as part of this file's behavior

  async prepare(): Promise<string[]> {  // line 77: executes this statement as part of this file's behavior
    return ["Prepared local process deployment using generated project files."];  // line 78: executes this statement as part of this file's behavior
  }  // line 79: executes this statement as part of this file's behavior

  async deploy(context: DeploymentAdapterContext): Promise<string[]> {  // line 81: executes this statement as part of this file's behavior
    return [`Build ${context.build.buildId} is now deployable by the local process runtime.`];  // line 82: executes this statement as part of this file's behavior
  }  // line 83: executes this statement as part of this file's behavior

  async status(context: DeploymentAdapterContext): Promise<RuntimeStatus> { return context.runtime.getStatus(context.project.id); }  // line 85: executes this statement as part of this file's behavior
  async start(context: DeploymentAdapterContext): Promise<RuntimeStatus> { return context.runtime.start(context.project); }  // line 86: executes this statement as part of this file's behavior
  async stop(context: DeploymentAdapterContext): Promise<RuntimeStatus> { return context.runtime.stop(context.project.id); }  // line 87: executes this statement as part of this file's behavior
  async restart(context: DeploymentAdapterContext): Promise<RuntimeStatus> { return context.runtime.restart(context.project); }  // line 88: executes this statement as part of this file's behavior
  async logs(context: DeploymentAdapterContext): Promise<string> { return context.runtime.getLogs(context.project.id); }  // line 89: executes this statement as part of this file's behavior
}  // line 90: executes this statement as part of this file's behavior

export class LocalDockerDeploymentAdapter implements DeploymentAdapter {  // line 92: executes this statement as part of this file's behavior
  readonly capabilities = localDockerCapabilities;  // line 93: executes this statement as part of this file's behavior

  constructor(private readonly commandRunner: CommandRunner) {}  // line 95: executes this statement as part of this file's behavior

  validateTarget(target: DeploymentTarget): void {  // line 97: executes this statement as part of this file's behavior
    if (target.type !== "local_docker") throw structuredError("INVALID_TARGET_TYPE", "local_docker adapter requires a local_docker target.");  // line 98: executes this statement as part of this file's behavior
  }  // line 99: executes this statement as part of this file's behavior

  async prepare(context: DeploymentAdapterContext): Promise<string[]> {  // line 101: executes this statement as part of this file's behavior
    const artifact = context.build.artifactPath ? ` using artifact ${context.build.artifactPath}` : "";  // line 102: executes this statement as part of this file's behavior
    return [`Preparing Docker deployment for ${dockerImage(context)}${artifact}.`];  // line 103: executes this statement as part of this file's behavior
  }  // line 104: executes this statement as part of this file's behavior

  async deploy(context: DeploymentAdapterContext): Promise<string[]> {  // line 106: executes this statement as part of this file's behavior
    const cwd = generatedWorkspace(context.project.id);  // line 107: executes this statement as part of this file's behavior
    await this.commandRunner("docker", ["build", "-t", taggedImage(context), "-f", dockerfile(context), "."], cwd, () => undefined);  // line 108: executes this statement as part of this file's behavior
    const envFile = await writeSecretEnvFile(context);  // line 109: executes this statement as part of this file's behavior
    await removeContainer(context);  // line 110: executes this statement as part of this file's behavior
    try {  // line 111: executes this statement as part of this file's behavior
      await this.commandRunner("docker", ["run", "-d", "--name", containerName(context), "--restart", "unless-stopped", "--env-file", envFile, taggedImage(context)], cwd, () => undefined);  // line 112: executes this statement as part of this file's behavior
    } finally {  // line 113: executes this statement as part of this file's behavior
      await fs.rm(envFile, { force: true });  // line 114: executes this statement as part of this file's behavior
    }  // line 115: executes this statement as part of this file's behavior
    return [`Built Docker image ${taggedImage(context)}.`, `Started container ${containerName(context)} with ${context.target.secretRefs.length} target secret reference(s).`];  // line 116: executes this statement as part of this file's behavior
  }  // line 117: executes this statement as part of this file's behavior

  async status(context: DeploymentAdapterContext): Promise<{ status: string; message: string; running: boolean }> {  // line 119: executes this statement as part of this file's behavior
    const lines: string[] = [];  // line 120: executes this statement as part of this file's behavior
    try {  // line 121: executes this statement as part of this file's behavior
      await this.commandRunner("docker", ["inspect", "--format", "{{.State.Status}}", containerName(context)], generatedWorkspace(context.project.id), (line) => lines.push(line));  // line 122: executes this statement as part of this file's behavior
      const status = lines.join("\n").trim() || "unknown";  // line 123: executes this statement as part of this file's behavior
      return { status, running: status === "running", message: `Docker container ${containerName(context)} is ${status}.` };  // line 124: executes this statement as part of this file's behavior
    } catch (error) {  // line 125: executes this statement as part of this file's behavior
      return { status: "missing", running: false, message: error instanceof Error ? error.message : String(error) };  // line 126: executes this statement as part of this file's behavior
    }  // line 127: executes this statement as part of this file's behavior
  }  // line 128: executes this statement as part of this file's behavior

  async start(context: DeploymentAdapterContext): Promise<{ status: string; message: string; running: boolean }> {  // line 130: executes this statement as part of this file's behavior
    await this.commandRunner("docker", ["start", containerName(context)], generatedWorkspace(context.project.id), () => undefined);  // line 131: executes this statement as part of this file's behavior
    return { status: "running", running: true, message: `Started Docker container ${containerName(context)}.` };  // line 132: executes this statement as part of this file's behavior
  }  // line 133: executes this statement as part of this file's behavior

  async stop(context: DeploymentAdapterContext): Promise<{ status: string; message: string; running: boolean }> {  // line 135: executes this statement as part of this file's behavior
    await this.commandRunner("docker", ["stop", containerName(context)], generatedWorkspace(context.project.id), () => undefined);  // line 136: executes this statement as part of this file's behavior
    return { status: "stopped", running: false, message: `Stopped Docker container ${containerName(context)}.` };  // line 137: executes this statement as part of this file's behavior
  }  // line 138: executes this statement as part of this file's behavior

  async restart(context: DeploymentAdapterContext): Promise<{ status: string; message: string; running: boolean }> {  // line 140: executes this statement as part of this file's behavior
    await this.commandRunner("docker", ["restart", containerName(context)], generatedWorkspace(context.project.id), () => undefined);  // line 141: executes this statement as part of this file's behavior
    return { status: "running", running: true, message: `Restarted Docker container ${containerName(context)}.` };  // line 142: executes this statement as part of this file's behavior
  }  // line 143: executes this statement as part of this file's behavior

  async logs(context: DeploymentAdapterContext): Promise<string> {  // line 145: executes this statement as part of this file's behavior
    const lines: string[] = [];  // line 146: executes this statement as part of this file's behavior
    await this.commandRunner("docker", ["logs", "--tail", "200", containerName(context)], generatedWorkspace(context.project.id), (line) => lines.push(line));  // line 147: executes this statement as part of this file's behavior
    return lines.join("\n");  // line 148: executes this statement as part of this file's behavior
  }  // line 149: executes this statement as part of this file's behavior

  async rollback(context: DeploymentAdapterContext): Promise<string[]> {  // line 151: executes this statement as part of this file's behavior
    if (!context.previousBuild) throw structuredError("ROLLBACK_UNAVAILABLE", "No previous successful deployment is available for rollback.");  // line 152: executes this statement as part of this file's behavior
    const previousContext = { ...context, build: context.previousBuild };  // line 153: executes this statement as part of this file's behavior
    const envFile = await writeSecretEnvFile(context);  // line 154: executes this statement as part of this file's behavior
    await removeContainer(context);  // line 155: executes this statement as part of this file's behavior
    try {  // line 156: executes this statement as part of this file's behavior
      await this.commandRunner("docker", ["run", "-d", "--name", containerName(context), "--restart", "unless-stopped", "--env-file", envFile, taggedImage(previousContext)], generatedWorkspace(context.project.id), () => undefined);  // line 157: executes this statement as part of this file's behavior
    } finally {  // line 158: executes this statement as part of this file's behavior
      await fs.rm(envFile, { force: true });  // line 159: executes this statement as part of this file's behavior
    }  // line 160: executes this statement as part of this file's behavior
    return [`Rolled back ${containerName(context)} to Docker image ${taggedImage(previousContext)}.`];  // line 161: executes this statement as part of this file's behavior
  }  // line 162: executes this statement as part of this file's behavior
}  // line 163: executes this statement as part of this file's behavior

export function adapterFor(target: DeploymentTarget, commandRunner: CommandRunner): DeploymentAdapter {  // line 165: executes this statement as part of this file's behavior
  return target.type === "local_process" ? new LocalProcessDeploymentAdapter() : new LocalDockerDeploymentAdapter(commandRunner);  // line 166: executes this statement as part of this file's behavior
}  // line 167: executes this statement as part of this file's behavior

function generatedWorkspace(projectId: string): string {  // line 169: executes this statement as part of this file's behavior
  return path.join(process.cwd(), "generated-projects", projectId);  // line 170: executes this statement as part of this file's behavior
}  // line 171: executes this statement as part of this file's behavior

function dockerImage(context: DeploymentAdapterContext): string {  // line 173: executes this statement as part of this file's behavior
  const configured = context.target.config.image;  // line 174: executes this statement as part of this file's behavior
  return typeof configured === "string" && configured.trim() ? configured.trim() : `botblade/${context.project.slug}`;  // line 175: executes this statement as part of this file's behavior
}  // line 176: executes this statement as part of this file's behavior

function taggedImage(context: DeploymentAdapterContext): string {  // line 178: executes this statement as part of this file's behavior
  return `${dockerImage(context)}:${context.build.buildId}`;  // line 179: executes this statement as part of this file's behavior
}  // line 180: executes this statement as part of this file's behavior

function dockerfile(context: DeploymentAdapterContext): string {  // line 182: executes this statement as part of this file's behavior
  const configured = context.target.config.dockerfile;  // line 183: executes this statement as part of this file's behavior
  return typeof configured === "string" && configured.trim() ? configured.trim() : "Dockerfile";  // line 184: executes this statement as part of this file's behavior
}  // line 185: executes this statement as part of this file's behavior

function containerName(context: DeploymentAdapterContext): string {  // line 187: executes this statement as part of this file's behavior
  return `botblade-${context.project.slug}`.replace(/[^a-zA-Z0-9_.-]/g, "-").slice(0, 128);  // line 188: executes this statement as part of this file's behavior
}  // line 189: executes this statement as part of this file's behavior

async function removeContainer(context: DeploymentAdapterContext): Promise<void> {  // line 191: executes this statement as part of this file's behavior
  try {  // line 192: executes this statement as part of this file's behavior
    await context.commandRunner("docker", ["rm", "-f", containerName(context)], generatedWorkspace(context.project.id), () => undefined);  // line 193: executes this statement as part of this file's behavior
  } catch {  // line 194: executes this statement as part of this file's behavior
    // docker rm -f fails when the container does not exist; deploy can continue.
  }  // line 196: executes this statement as part of this file's behavior
}  // line 197: executes this statement as part of this file's behavior

async function writeSecretEnvFile(context: DeploymentAdapterContext): Promise<string> {  // line 199: executes this statement as part of this file's behavior
  const env: Record<string, string> = {};  // line 200: executes this statement as part of this file's behavior
  for (const secretId of context.target.secretRefs) {  // line 201: executes this statement as part of this file's behavior
    const secret = context.resolveSecretRef(secretId);  // line 202: executes this statement as part of this file's behavior
    if (!secret) throw structuredError("SECRET_REF_NOT_FOUND", `Secret reference '${secretId}' was not found.`);  // line 203: executes this statement as part of this file's behavior
    env[envName(secret.name)] = secret.value;  // line 204: executes this statement as part of this file's behavior
  }  // line 205: executes this statement as part of this file's behavior
  if (context.project.discord.tokenSecretRef) {  // line 206: executes this statement as part of this file's behavior
    const secret = context.resolveSecretRef(context.project.discord.tokenSecretRef);  // line 207: executes this statement as part of this file's behavior
    if (!secret) throw structuredError("SECRET_REF_NOT_FOUND", `Secret reference '${context.project.discord.tokenSecretRef}' was not found.`);  // line 208: executes this statement as part of this file's behavior
    env.DISCORD_TOKEN = secret.value;  // line 209: executes this statement as part of this file's behavior
  }  // line 210: executes this statement as part of this file's behavior
  const envFile = path.join(generatedWorkspace(context.project.id), `.botblade-deploy-${context.build.buildId}.env`);  // line 211: executes this statement as part of this file's behavior
  await fs.writeFile(envFile, Object.entries(env).map(([key, value]) => `${key}=${escapeEnvValue(value)}`).join("\n"), "utf8");  // line 212: executes this statement as part of this file's behavior
  return envFile;  // line 213: executes this statement as part of this file's behavior
}  // line 214: executes this statement as part of this file's behavior

function envName(name: string): string {  // line 216: executes this statement as part of this file's behavior
  return name.trim().replace(/[^A-Za-z0-9_]/g, "_").replace(/^([0-9])/, "_$1").toUpperCase();  // line 217: executes this statement as part of this file's behavior
}  // line 218: executes this statement as part of this file's behavior

function escapeEnvValue(value: string): string {  // line 220: executes this statement as part of this file's behavior
  return value.replace(/\r?\n/g, "\\n");  // line 221: executes this statement as part of this file's behavior
}  // line 222: executes this statement as part of this file's behavior

function structuredError(code: string, message: string): never {  // line 224: executes this statement as part of this file's behavior
  throw { statusCode: 400, code, message, details: {} };  // line 225: executes this statement as part of this file's behavior
}  // line 226: executes this statement as part of this file's behavior
