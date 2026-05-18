import { randomUUID } from "node:crypto";
import type { BotProject } from "../models/project.js";
import type { BuildService } from "./buildService.js";
import { RequestValidationError } from "./projectStore.js";
import { redactSecrets } from "./redaction.js";
import type { DeploymentTargetStore } from "./deploymentTargets.js";
import type { LocalProcessRuntimeService } from "./localProcessRuntimeService.js";
import { adapterFor } from "./deploymentAdapters.js";
import type { AuditAction } from "./auditService.js";
import type { DeploymentJobStorePersistence } from "./persistence.js";

export type DeploymentStatus = "queued" | "preparing" | "deploying" | "deployed" | "running" | "failed" | "rolled_back" | "canceled";

export interface DeploymentJob {
  deploymentId: string;
  projectId: string;
  targetId: string;
  buildId: string;
  status: DeploymentStatus;
  createdAt: string;
  updatedAt: string;
  finishedAt: string | null;
  errorMessage: string | null;
  logUrl: string;
  auditEventId: string;
}

export class DeploymentJobStore {
  private readonly jobs = new Map<string, DeploymentJob[]>();
  private readonly logs = new Map<string, string>();

  constructor(private readonly buildService: BuildService, private readonly targetStore: DeploymentTargetStore, private readonly runtime: LocalProcessRuntimeService, private readonly audit?: (event: { action: AuditAction; projectId: string; resourceType: string; resourceId: string; metadata: Record<string, unknown>; requestId: string; actorId?: string }) => void, private readonly persistence?: DeploymentJobStorePersistence) {
    for (const { job, logs } of persistence?.loadDeploymentJobs() ?? []) {
      this.jobs.set(job.projectId, [job, ...(this.jobs.get(job.projectId) ?? [])]);
      this.logs.set(job.deploymentId, logs);
    }
  }

  list(projectId: string): DeploymentJob[] { return [...(this.jobs.get(projectId) ?? [])].sort((a, b) => b.createdAt.localeCompare(a.createdAt)); }
  get(projectId: string, deploymentId: string): DeploymentJob | undefined { return this.jobs.get(projectId)?.find((job) => job.deploymentId === deploymentId); }
  getLogs(projectId: string, deploymentId: string): string | undefined { return this.get(projectId, deploymentId) ? this.logs.get(deploymentId) ?? "" : undefined; }

  async create(project: BotProject, request: unknown, auditEventId = `audit_${randomUUID()}`, requestId = "system", actorId?: string): Promise<DeploymentJob> {
    const body = asRecord(request);
    const targetId = stringField(body, "targetId");
    const buildId = stringField(body, "buildId");
    const target = this.targetStore.get(targetId);
    if (!target) throw { statusCode: 404, code: "TARGET_NOT_FOUND", message: `Deployment target '${targetId}' was not found.`, details: {} };
    const build = this.buildService.get(project.id, buildId);
    if (!build) throw { statusCode: 404, code: "BUILD_NOT_FOUND", message: `Build '${buildId}' was not found.`, details: {} };
    if (build.status !== "succeeded") throw { statusCode: 400, code: "BUILD_NOT_DEPLOYABLE", message: "Only succeeded builds can be deployed.", details: { buildId, status: build.status } };

    const now = new Date().toISOString();
    const job: DeploymentJob = { deploymentId: `deployment_${randomUUID()}`, projectId: project.id, targetId, buildId, status: "queued", createdAt: now, updatedAt: now, finishedAt: null, errorMessage: null, logUrl: `/api/projects/${project.id}/deployments/deployment_${randomUUID()}/logs`, auditEventId };
    job.logUrl = `/api/projects/${project.id}/deployments/${job.deploymentId}/logs`;
    this.jobs.set(project.id, [job, ...(this.jobs.get(project.id) ?? [])]);
    this.persist(job);
    await this.run(project, job, requestId, actorId);
    return job;
  }

  rollback(project: BotProject, deploymentId: string): DeploymentJob {
    const job = this.get(project.id, deploymentId);
    if (!job) throw { statusCode: 404, code: "DEPLOYMENT_NOT_FOUND", message: `Deployment '${deploymentId}' was not found.`, details: {} };
    throw { statusCode: 400, code: "ROLLBACK_UNSUPPORTED", message: "Rollback is not supported by the selected deployment adapter yet.", details: { deploymentId } };
  }

  private async run(project: BotProject, job: DeploymentJob, requestId: string, actorId?: string): Promise<void> {
    const append = (line: string) => {
      this.logs.set(job.deploymentId, `${this.logs.get(job.deploymentId) ?? ""}${redactSecrets(line)}\n`);
      this.persist(job);
    };
    try {
      const target = this.targetStore.get(job.targetId);
      const build = this.buildService.get(project.id, job.buildId);
      if (!target || !build) throw new Error("Deployment dependency disappeared.");
      const adapter = adapterFor(target);
      adapter.validateTarget(target);
      job.status = "preparing"; job.updatedAt = new Date().toISOString();
      for (const line of await adapter.prepare({ project, target, build, runtime: this.runtime })) append(line);
      job.status = "deploying"; job.updatedAt = new Date().toISOString();
      for (const line of await adapter.deploy({ project, target, build, runtime: this.runtime })) append(line);
      job.status = "deployed";
      project.deployment = { targetId: target.id, lastDeploymentId: job.deploymentId };
      project.updatedAt = new Date().toISOString();
      append("Deployment completed.");
    } catch (error) {
      job.status = "failed";
      job.errorMessage = redactSecrets(error instanceof Error ? error.message : typeof error === "object" && error && "message" in error ? String(error.message) : String(error));
      append(`Deployment failed: ${job.errorMessage}`);
    } finally {
      job.updatedAt = new Date().toISOString();
      job.finishedAt = new Date().toISOString();
      this.persist(job);
      this.audit?.({
        action: job.status === "failed" ? "deployment.failed" : "deployment.succeeded",
        projectId: project.id,
        resourceType: "deployment",
        resourceId: job.deploymentId,
        metadata: { status: job.status, targetId: job.targetId, buildId: job.buildId, errorMessage: job.errorMessage },
        requestId,
        actorId,
      });
    }
  }

  private persist(job: DeploymentJob): void {
    this.persistence?.saveDeploymentJob(job, this.logs.get(job.deploymentId) ?? "");
  }
}

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
}

function stringField(object: Record<string, unknown>, field: string): string {
  if (typeof object[field] === "string" && object[field].trim()) return object[field].trim();
  throw new RequestValidationError([{ field, message: `${field} is required.` }]);
}
