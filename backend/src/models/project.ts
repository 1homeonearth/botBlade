export const SERVICE_NAME = "royalScepter-backend";
export const SERVICE_VERSION = "0.1.0";
export const API_VERSION = "v1";

export type CommandRegistration = "guild" | "global";
export type ProjectLanguage = "typescript";
export type ProjectRuntime = "node22";

export interface DiscordConfig {
  applicationId: string | null;
  clientId: string | null;
  defaultGuildId: string | null;
  tokenSecretRef: string | null;
  commandRegistration: CommandRegistration;
}

export interface PermissionConfig {
  intents: string[];
  botPermissions: string[];
}

export interface BotCommandOption {
  name: string;
  type: string;
  description?: string;
  required?: boolean;
}

export interface BotCommand {
  name: string;
  description: string;
  options?: BotCommandOption[];
  handler?: string;
}

export interface BotEvent {
  name: string;
  handler?: string;
}

export interface DeploymentConfig {
  targetId: string | null;
  lastDeploymentId: string | null;
}

export interface BotProject {
  id: string;
  name: string;
  slug: string;
  description: string;
  templateId: string;
  language: ProjectLanguage;
  runtime: ProjectRuntime;
  discord: DiscordConfig;
  permissions: PermissionConfig;
  commands: BotCommand[];
  events: BotEvent[];
  deployment: DeploymentConfig;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BotRuntimeStatus {
  running: boolean;
  projectId: string;
  status: "running" | "stopped";
  message: string;
}

export interface ValidationIssue {
  code: string;
  message: string;
  field?: string;
}

export interface ProjectValidationResult {
  valid: boolean;
  errors: ValidationIssue[];
  warnings: ValidationIssue[];
}
