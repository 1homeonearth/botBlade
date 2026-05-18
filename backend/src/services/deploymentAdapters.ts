import type { BotProject } from "../models/project.js";
import type { BuildJob } from "./buildService.js";
import type { DeploymentTarget } from "./deploymentTargets.js";
import type { LocalProcessRuntimeService, RuntimeStatus } from "./localProcessRuntimeService.js";

export interface DeploymentAdapter {
  validateTarget(target: DeploymentTarget): Promise<void> | void;
  prepare(context: DeploymentAdapterContext): Promise<string[]>;
  deploy(context: DeploymentAdapterContext): Promise<string[]>;
  status(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string }>;
  start(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string }>;
  stop(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string }>;
  restart(context: DeploymentAdapterContext): Promise<RuntimeStatus | { status: string; message: string }>;
  logs(context: DeploymentAdapterContext): Promise<string>;
  rollback?(context: DeploymentAdapterContext): Promise<string[]>;
}

export interface DeploymentAdapterContext {
  project: BotProject;
  target: DeploymentTarget;
  build: BuildJob;
  runtime: LocalProcessRuntimeService;
}

export class LocalProcessDeploymentAdapter implements DeploymentAdapter {
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
  validateTarget(target: DeploymentTarget): void {
    if (target.type !== "local_docker") throw structuredError("INVALID_TARGET_TYPE", "local_docker adapter requires a local_docker target.");
    throw structuredError("DOCKER_NOT_CONFIGURED", "Docker deployments are not configured in this backend build yet.");
  }
  async prepare(): Promise<string[]> { return []; }
  async deploy(): Promise<string[]> { return []; }
  async status(): Promise<{ status: string; message: string }> { return { status: "unknown", message: "Docker deployments are unavailable." }; }
  async start(): Promise<{ status: string; message: string }> { return { status: "unknown", message: "Docker deployments are unavailable." }; }
  async stop(): Promise<{ status: string; message: string }> { return { status: "unknown", message: "Docker deployments are unavailable." }; }
  async restart(): Promise<{ status: string; message: string }> { return { status: "unknown", message: "Docker deployments are unavailable." }; }
  async logs(): Promise<string> { return "Docker deployments are unavailable."; }
}

export function adapterFor(target: DeploymentTarget): DeploymentAdapter {
  return target.type === "local_process" ? new LocalProcessDeploymentAdapter() : new LocalDockerDeploymentAdapter();
}

function structuredError(code: string, message: string): never {
  throw { statusCode: 400, code, message, details: {} };
}
