// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import type { AuditEvent, AuditInput } from "./auditService.js";  // line 7: executes this statement as part of this file's behavior
import type { BuildJob } from "./buildService.js";  // line 8: executes this statement as part of this file's behavior
import type { DeploymentJob } from "./deploymentJobs.js";  // line 9: executes this statement as part of this file's behavior
import type { DeploymentTarget } from "./deploymentTargets.js";  // line 10: executes this statement as part of this file's behavior
import type { BotProject } from "../models/project.js";  // line 11: executes this statement as part of this file's behavior
import type { CreateProjectInput, UpdateProjectInput } from "./projectStore.js";  // line 12: executes this statement as part of this file's behavior
import type { CreateSecretInput, SecretSummary, UpdateSecretInput } from "./secretStore.js";  // line 13: executes this statement as part of this file's behavior

export interface ProjectStorePort {  // line 15: executes this statement as part of this file's behavior
  list(): BotProject[];  // line 16: executes this statement as part of this file's behavior
  get(projectId: string): BotProject | undefined;  // line 17: executes this statement as part of this file's behavior
  create(input: CreateProjectInput): BotProject;  // line 18: executes this statement as part of this file's behavior
  update(projectId: string, input: UpdateProjectInput): BotProject | undefined;  // line 19: executes this statement as part of this file's behavior
  archive(projectId: string): BotProject | undefined;  // line 20: executes this statement as part of this file's behavior
  clone(projectId: string): BotProject | undefined;  // line 21: executes this statement as part of this file's behavior
  delete(projectId: string): boolean;  // line 22: executes this statement as part of this file's behavior
}  // line 23: executes this statement as part of this file's behavior

export interface SecretStorePort {  // line 25: executes this statement as part of this file's behavior
  list(): SecretSummary[];  // line 26: executes this statement as part of this file's behavior
  get(secretId: string): SecretSummary | undefined;  // line 27: executes this statement as part of this file's behavior
  has(secretId: string): boolean;  // line 28: executes this statement as part of this file's behavior
  getValue(secretId: string): string | undefined;  // line 29: executes this statement as part of this file's behavior
  create(input: CreateSecretInput): SecretSummary;  // line 30: executes this statement as part of this file's behavior
  update(secretId: string, input: UpdateSecretInput): SecretSummary | undefined;  // line 31: executes this statement as part of this file's behavior
  rotate(secretId: string, value: string): SecretSummary | undefined;  // line 32: executes this statement as part of this file's behavior
  delete(secretId: string): boolean;  // line 33: executes this statement as part of this file's behavior
}  // line 34: executes this statement as part of this file's behavior

export interface AuditServicePort {  // line 36: executes this statement as part of this file's behavior
  record(input: AuditInput): AuditEvent;  // line 37: executes this statement as part of this file's behavior
  list(projectId?: string): AuditEvent[];  // line 38: executes this statement as part of this file's behavior
}  // line 39: executes this statement as part of this file's behavior

export interface BuildServicePersistence {  // line 41: executes this statement as part of this file's behavior
  loadBuildJobs(): Array<{ job: BuildJob; logs: string }>;  // line 42: executes this statement as part of this file's behavior
  saveBuildJob(job: BuildJob, logs: string): void;  // line 43: executes this statement as part of this file's behavior
}  // line 44: executes this statement as part of this file's behavior

export interface DeploymentTargetStorePersistence {  // line 46: executes this statement as part of this file's behavior
  loadDeploymentTargets(): DeploymentTarget[];  // line 47: executes this statement as part of this file's behavior
  saveDeploymentTarget(target: DeploymentTarget): void;  // line 48: executes this statement as part of this file's behavior
  deleteDeploymentTarget(id: string): void;  // line 49: executes this statement as part of this file's behavior
}  // line 50: executes this statement as part of this file's behavior

export interface DeploymentJobStorePersistence {  // line 52: executes this statement as part of this file's behavior
  loadDeploymentJobs(): Array<{ job: DeploymentJob; logs: string }>;  // line 53: executes this statement as part of this file's behavior
  saveDeploymentJob(job: DeploymentJob, logs: string): void;  // line 54: executes this statement as part of this file's behavior
}  // line 55: executes this statement as part of this file's behavior

export interface ProjectStorePersistence {  // line 57: executes this statement as part of this file's behavior
  loadProjects(): BotProject[];  // line 58: executes this statement as part of this file's behavior
  saveProject(project: BotProject): void;  // line 59: executes this statement as part of this file's behavior
  deleteProject(projectId: string): void;  // line 60: executes this statement as part of this file's behavior
}  // line 61: executes this statement as part of this file's behavior

export interface SecretRecord extends SecretSummary {  // line 63: executes this statement as part of this file's behavior
  value: string;  // line 64: executes this statement as part of this file's behavior
}  // line 65: executes this statement as part of this file's behavior

export interface SecretStorePersistence {  // line 67: executes this statement as part of this file's behavior
  loadSecrets(): SecretRecord[];  // line 68: executes this statement as part of this file's behavior
  saveSecret(secret: SecretRecord): void;  // line 69: executes this statement as part of this file's behavior
  deleteSecret(secretId: string): void;  // line 70: executes this statement as part of this file's behavior
}  // line 71: executes this statement as part of this file's behavior

export interface AuditServicePersistence {  // line 73: executes this statement as part of this file's behavior
  loadAuditEvents(): AuditEvent[];  // line 74: executes this statement as part of this file's behavior
  saveAuditEvent(event: AuditEvent): void;  // line 75: executes this statement as part of this file's behavior
}  // line 76: executes this statement as part of this file's behavior
