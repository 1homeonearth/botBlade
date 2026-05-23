// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import { randomUUID } from "node:crypto";  // line 7: executes this statement as part of this file's behavior
import type { BotProject } from "../models/project.js";  // line 8: executes this statement as part of this file's behavior
import type { BuildService, CommandRunner } from "./buildService.js";  // line 9: executes this statement as part of this file's behavior
import { runCommand } from "./buildService.js";  // line 10: executes this statement as part of this file's behavior
import { RequestValidationError } from "./projectStore.js";  // line 11: executes this statement as part of this file's behavior
import { redactSecrets } from "./redaction.js";  // line 12: executes this statement as part of this file's behavior
import type { DeploymentTargetStore } from "./deploymentTargets.js";  // line 13: executes this statement as part of this file's behavior
import type { LocalProcessRuntimeService } from "./localProcessRuntimeService.js";  // line 14: executes this statement as part of this file's behavior
import { adapterFor, type DeploymentAction, type ResolvedSecret } from "./deploymentAdapters.js";  // line 15: executes this statement as part of this file's behavior
import type { AuditAction } from "./auditService.js";  // line 16: executes this statement as part of this file's behavior
import type { DeploymentJobStorePersistence } from "./persistence.js";  // line 17: executes this statement as part of this file's behavior

export type DeploymentStatus = "queued" | "preparing" | "deploying" | "deployed" | "running" | "failed" | "rolled_back" | "canceled";  // line 19: executes this statement as part of this file's behavior

export interface DeploymentJob {  // line 21: executes this statement as part of this file's behavior
  deploymentId: string;  // line 22: executes this statement as part of this file's behavior
  projectId: string;  // line 23: executes this statement as part of this file's behavior
  targetId: string;  // line 24: executes this statement as part of this file's behavior
  buildId: string;  // line 25: executes this statement as part of this file's behavior
  status: DeploymentStatus;  // line 26: executes this statement as part of this file's behavior
  createdAt: string;  // line 27: executes this statement as part of this file's behavior
  updatedAt: string;  // line 28: executes this statement as part of this file's behavior
  finishedAt: string | null;  // line 29: executes this statement as part of this file's behavior
  errorMessage: string | null;  // line 30: executes this statement as part of this file's behavior
  logUrl: string;  // line 31: executes this statement as part of this file's behavior
  auditEventId: string;  // line 32: executes this statement as part of this file's behavior
}  // line 33: executes this statement as part of this file's behavior

export class DeploymentJobStore {  // line 35: executes this statement as part of this file's behavior
  private readonly jobs = new Map<string, DeploymentJob[]>();  // line 36: executes this statement as part of this file's behavior
  private readonly logs = new Map<string, string>();  // line 37: executes this statement as part of this file's behavior

  constructor(private readonly buildService: BuildService, private readonly targetStore: DeploymentTargetStore, private readonly runtime: LocalProcessRuntimeService, private readonly audit?: (event: { action: AuditAction; projectId: string; resourceType: string; resourceId: string; metadata: Record<string, unknown>; requestId: string; actorId?: string }) => void, private readonly persistence?: DeploymentJobStorePersistence, private readonly resolveSecretRef: (secretId: string) => ResolvedSecret | undefined = () => undefined, private readonly commandRunner: CommandRunner = runCommand) {  // line 39: executes this statement as part of this file's behavior
    for (const { job, logs } of persistence?.loadDeploymentJobs() ?? []) {  // line 40: executes this statement as part of this file's behavior
      this.jobs.set(job.projectId, [job, ...(this.jobs.get(job.projectId) ?? [])]);  // line 41: executes this statement as part of this file's behavior
      this.logs.set(job.deploymentId, logs);  // line 42: executes this statement as part of this file's behavior
    }  // line 43: executes this statement as part of this file's behavior
  }  // line 44: executes this statement as part of this file's behavior

  list(projectId: string): DeploymentJob[] { return [...(this.jobs.get(projectId) ?? [])].sort((a, b) => b.createdAt.localeCompare(a.createdAt)); }  // line 46: executes this statement as part of this file's behavior
  get(projectId: string, deploymentId: string): DeploymentJob | undefined { return this.jobs.get(projectId)?.find((job) => job.deploymentId === deploymentId); }  // line 47: executes this statement as part of this file's behavior
  getLogs(projectId: string, deploymentId: string): string | undefined { return this.get(projectId, deploymentId) ? this.logs.get(deploymentId) ?? "" : undefined; }  // line 48: executes this statement as part of this file's behavior

  async create(project: BotProject, request: unknown, auditEventId = `audit_${randomUUID()}`, requestId = "system", actorId?: string): Promise<DeploymentJob> {  // line 50: executes this statement as part of this file's behavior
    const body = asRecord(request);  // line 51: executes this statement as part of this file's behavior
    const targetId = stringField(body, "targetId");  // line 52: executes this statement as part of this file's behavior
    const buildId = stringField(body, "buildId");  // line 53: executes this statement as part of this file's behavior
    const target = this.targetStore.get(targetId);  // line 54: executes this statement as part of this file's behavior
    if (!target) throw { statusCode: 404, code: "TARGET_NOT_FOUND", message: `Deployment target '${targetId}' was not found.`, details: {} };  // line 55: executes this statement as part of this file's behavior
    const build = this.buildService.get(project.id, buildId);  // line 56: executes this statement as part of this file's behavior
    if (!build) throw { statusCode: 404, code: "BUILD_NOT_FOUND", message: `Build '${buildId}' was not found.`, details: {} };  // line 57: executes this statement as part of this file's behavior
    if (build.status !== "succeeded") throw { statusCode: 400, code: "BUILD_NOT_DEPLOYABLE", message: "Only succeeded builds can be deployed.", details: { buildId, status: build.status } };  // line 58: executes this statement as part of this file's behavior

    const now = new Date().toISOString();  // line 60: executes this statement as part of this file's behavior
    const job: DeploymentJob = { deploymentId: `deployment_${randomUUID()}`, projectId: project.id, targetId, buildId, status: "queued", createdAt: now, updatedAt: now, finishedAt: null, errorMessage: null, logUrl: `/api/projects/${project.id}/deployments/deployment_${randomUUID()}/logs`, auditEventId };  // line 61: executes this statement as part of this file's behavior
    job.logUrl = `/api/projects/${project.id}/deployments/${job.deploymentId}/logs`;  // line 62: executes this statement as part of this file's behavior
    this.jobs.set(project.id, [job, ...(this.jobs.get(project.id) ?? [])]);  // line 63: executes this statement as part of this file's behavior
    this.persist(job);  // line 64: executes this statement as part of this file's behavior
    await this.run(project, job, requestId, actorId);  // line 65: executes this statement as part of this file's behavior
    return job;  // line 66: executes this statement as part of this file's behavior
  }  // line 67: executes this statement as part of this file's behavior

  async action(project: BotProject, deploymentId: string, action: Exclude<DeploymentAction, "deploy">): Promise<unknown> {  // line 69: executes this statement as part of this file's behavior
    const job = this.get(project.id, deploymentId);  // line 70: executes this statement as part of this file's behavior
    if (!job) throw { statusCode: 404, code: "DEPLOYMENT_NOT_FOUND", message: `Deployment '${deploymentId}' was not found.`, details: {} };  // line 71: executes this statement as part of this file's behavior
    const target = this.targetStore.get(job.targetId);  // line 72: executes this statement as part of this file's behavior
    const build = this.buildService.get(project.id, job.buildId);  // line 73: executes this statement as part of this file's behavior
    if (!target || !build) throw new Error("Deployment dependency disappeared.");  // line 74: executes this statement as part of this file's behavior
    const adapter = adapterFor(target, this.commandRunner);  // line 75: executes this statement as part of this file's behavior
    adapter.validateTarget(target);  // line 76: executes this statement as part of this file's behavior
    if (!adapter.capabilities.actions[action]) throw { statusCode: 400, code: "DEPLOYMENT_ACTION_UNSUPPORTED", message: `${action} is not supported by ${target.type}.`, details: { action, targetType: target.type } };  // line 77: executes this statement as part of this file's behavior
    const context = { project, target, build, runtime: this.runtime, resolveSecretRef: this.resolveSecretRef, previousBuild: this.previousSuccessfulBuild(project.id, job), commandRunner: this.commandRunner };  // line 78: executes this statement as part of this file's behavior
    if (action === "logs") return { logs: await adapter.logs(context) };  // line 79: executes this statement as part of this file's behavior
    if (action === "rollback") {  // line 80: executes this statement as part of this file's behavior
      const lines = await adapter.rollback?.(context);  // line 81: executes this statement as part of this file's behavior
      job.status = "rolled_back";  // line 82: executes this statement as part of this file's behavior
      job.updatedAt = new Date().toISOString();  // line 83: executes this statement as part of this file's behavior
      job.finishedAt = new Date().toISOString();  // line 84: executes this statement as part of this file's behavior
      this.logs.set(job.deploymentId, `${this.logs.get(job.deploymentId) ?? ""}${(lines ?? []).map(redactSecrets).join("\n")}\n`);  // line 85: executes this statement as part of this file's behavior
      this.persist(job);  // line 86: executes this statement as part of this file's behavior
      return job;  // line 87: executes this statement as part of this file's behavior
    }  // line 88: executes this statement as part of this file's behavior
    return adapter[action](context);  // line 89: executes this statement as part of this file's behavior
  }  // line 90: executes this statement as part of this file's behavior

  rollback(project: BotProject, deploymentId: string): Promise<unknown> {  // line 92: executes this statement as part of this file's behavior
    return this.action(project, deploymentId, "rollback");  // line 93: executes this statement as part of this file's behavior
  }  // line 94: executes this statement as part of this file's behavior

  private async run(project: BotProject, job: DeploymentJob, requestId: string, actorId?: string): Promise<void> {  // line 96: executes this statement as part of this file's behavior
    const append = (line: string) => {  // line 97: executes this statement as part of this file's behavior
      this.logs.set(job.deploymentId, `${this.logs.get(job.deploymentId) ?? ""}${redactSecrets(line)}\n`);  // line 98: executes this statement as part of this file's behavior
      this.persist(job);  // line 99: executes this statement as part of this file's behavior
    };  // line 100: executes this statement as part of this file's behavior
    try {  // line 101: executes this statement as part of this file's behavior
      const target = this.targetStore.get(job.targetId);  // line 102: executes this statement as part of this file's behavior
      const build = this.buildService.get(project.id, job.buildId);  // line 103: executes this statement as part of this file's behavior
      if (!target || !build) throw new Error("Deployment dependency disappeared.");  // line 104: executes this statement as part of this file's behavior
      const adapter = adapterFor(target, this.commandRunner);  // line 105: executes this statement as part of this file's behavior
      adapter.validateTarget(target);  // line 106: executes this statement as part of this file's behavior
      const context = { project, target, build, runtime: this.runtime, resolveSecretRef: this.resolveSecretRef, previousBuild: this.previousSuccessfulBuild(project.id, job), commandRunner: this.commandRunner };  // line 107: executes this statement as part of this file's behavior
      job.status = "preparing"; job.updatedAt = new Date().toISOString();  // line 108: executes this statement as part of this file's behavior
      for (const line of await adapter.prepare(context)) append(line);  // line 109: executes this statement as part of this file's behavior
      job.status = "deploying"; job.updatedAt = new Date().toISOString();  // line 110: executes this statement as part of this file's behavior
      for (const line of await adapter.deploy(context)) append(line);  // line 111: executes this statement as part of this file's behavior
      job.status = "deployed";  // line 112: executes this statement as part of this file's behavior
      project.deployment = { targetId: target.id, lastDeploymentId: job.deploymentId };  // line 113: executes this statement as part of this file's behavior
      project.updatedAt = new Date().toISOString();  // line 114: executes this statement as part of this file's behavior
      append("Deployment completed.");  // line 115: executes this statement as part of this file's behavior
    } catch (error) {  // line 116: executes this statement as part of this file's behavior
      job.status = "failed";  // line 117: executes this statement as part of this file's behavior
      job.errorMessage = redactSecrets(error instanceof Error ? error.message : typeof error === "object" && error && "message" in error ? String(error.message) : String(error));  // line 118: executes this statement as part of this file's behavior
      append(`Deployment failed: ${job.errorMessage}`);  // line 119: executes this statement as part of this file's behavior
    } finally {  // line 120: executes this statement as part of this file's behavior
      job.updatedAt = new Date().toISOString();  // line 121: executes this statement as part of this file's behavior
      job.finishedAt = new Date().toISOString();  // line 122: executes this statement as part of this file's behavior
      this.persist(job);  // line 123: executes this statement as part of this file's behavior
      this.audit?.({  // line 124: executes this statement as part of this file's behavior
        action: job.status === "failed" ? "deployment.failed" : "deployment.succeeded",  // line 125: executes this statement as part of this file's behavior
        projectId: project.id,  // line 126: executes this statement as part of this file's behavior
        resourceType: "deployment",  // line 127: executes this statement as part of this file's behavior
        resourceId: job.deploymentId,  // line 128: executes this statement as part of this file's behavior
        metadata: { status: job.status, targetId: job.targetId, buildId: job.buildId, errorMessage: job.errorMessage },  // line 129: executes this statement as part of this file's behavior
        requestId,  // line 130: executes this statement as part of this file's behavior
        actorId,  // line 131: executes this statement as part of this file's behavior
      });  // line 132: executes this statement as part of this file's behavior
    }  // line 133: executes this statement as part of this file's behavior
  }  // line 134: executes this statement as part of this file's behavior

  private previousSuccessfulBuild(projectId: string, current: DeploymentJob) {  // line 136: executes this statement as part of this file's behavior
    const previousJob = this.list(projectId).find((job) => job.deploymentId !== current.deploymentId && job.targetId === current.targetId && (job.status === "deployed" || job.status === "rolled_back"));  // line 137: executes this statement as part of this file's behavior
    return previousJob ? this.buildService.get(projectId, previousJob.buildId) : undefined;  // line 138: executes this statement as part of this file's behavior
  }  // line 139: executes this statement as part of this file's behavior

  private persist(job: DeploymentJob): void {  // line 141: executes this statement as part of this file's behavior
    this.persistence?.saveDeploymentJob(job, this.logs.get(job.deploymentId) ?? "");  // line 142: executes this statement as part of this file's behavior
  }  // line 143: executes this statement as part of this file's behavior
}  // line 144: executes this statement as part of this file's behavior

function asRecord(value: unknown): Record<string, unknown> {  // line 146: executes this statement as part of this file's behavior
  return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};  // line 147: executes this statement as part of this file's behavior
}  // line 148: executes this statement as part of this file's behavior

function stringField(object: Record<string, unknown>, field: string): string {  // line 150: executes this statement as part of this file's behavior
  const candidate = object[field];  // line 151: executes this statement as part of this file's behavior
  if (typeof candidate === "string") {  // line 152: executes this statement as part of this file's behavior
    const trimmed = candidate.trim();  // line 153: executes this statement as part of this file's behavior
    if (trimmed) return trimmed;  // line 154: executes this statement as part of this file's behavior
  }  // line 155: executes this statement as part of this file's behavior
  throw new RequestValidationError([{ field, message: `${field} is required.` }]);  // line 156: executes this statement as part of this file's behavior
}  // line 157: executes this statement as part of this file's behavior
