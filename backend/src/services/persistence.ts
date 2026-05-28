import type { AuditEvent, AuditInput } from "./auditService.js";
import type { BuildJob } from "./buildService.js";
import type { DeploymentJob } from "./deploymentJobs.js";
import type { DeploymentTarget } from "./deploymentTargets.js";
import type { BotProject } from "../models/project.js";
import type { CreateProjectInput, UpdateProjectInput } from "./projectStore.js";
import type { CreateSecretInput, SecretSummary, UpdateSecretInput } from "./secretStore.js";
import type { ImportRecord } from "./imports/index.js";

export interface ProjectStorePort {
  list(): BotProject[];
  get(projectId: string): BotProject | undefined;
  create(input: CreateProjectInput): BotProject;
  update(projectId: string, input: UpdateProjectInput): BotProject | undefined;
  archive(projectId: string): BotProject | undefined;
  clone(projectId: string): BotProject | undefined;
  delete(projectId: string): boolean;
}

export interface SecretStorePort {
  list(): SecretSummary[];
  get(secretId: string): SecretSummary | undefined;
  has(secretId: string): boolean;
  getValue(secretId: string): string | undefined;
  create(input: CreateSecretInput): SecretSummary;
  update(secretId: string, input: UpdateSecretInput): SecretSummary | undefined;
  rotate(secretId: string, value: string): SecretSummary | undefined;
  delete(secretId: string): boolean;
}

export interface AuditServicePort {
  record(input: AuditInput): AuditEvent;
  list(projectId?: string): AuditEvent[];
}

export interface BuildServicePersistence {
  loadBuildJobs(): Array<{ job: BuildJob; logs: string }>;
  saveBuildJob(job: BuildJob, logs: string): void;
}

export interface DeploymentTargetStorePersistence {
  loadDeploymentTargets(): DeploymentTarget[];
  saveDeploymentTarget(target: DeploymentTarget): void;
  deleteDeploymentTarget(id: string): void;
}

export interface DeploymentJobStorePersistence {
  loadDeploymentJobs(): Array<{ job: DeploymentJob; logs: string }>;
  saveDeploymentJob(job: DeploymentJob, logs: string): void;
}

export interface ProjectStorePersistence {
  loadProjects(): BotProject[];
  saveProject(project: BotProject): void;
  deleteProject(projectId: string): void;
}

export interface SecretRecord extends SecretSummary {
  value: string;
}

export interface SecretStorePersistence {
  loadSecrets(): SecretRecord[];
  saveSecret(secret: SecretRecord): void;
  deleteSecret(secretId: string): void;
}

export interface AuditServicePersistence {
  loadAuditEvents(): AuditEvent[];
  saveAuditEvent(event: AuditEvent): void;
  pruneAuditEvents?(keepIds: string[]): void;
}

export interface ImportStorePersistence {
  loadImportRecords(): ImportRecord[];
  saveImportRecord(record: ImportRecord): void;
}
