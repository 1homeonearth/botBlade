// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
export const SERVICE_NAME = "botBlade-backend";  // line 7: executes this statement as part of this file's behavior
export const SERVICE_VERSION = "0.1.0";  // line 8: executes this statement as part of this file's behavior
export const API_VERSION = "v1";  // line 9: executes this statement as part of this file's behavior

export type CommandRegistration = "guild" | "global";  // line 11: executes this statement as part of this file's behavior
export type ProjectLanguage = "typescript";  // line 12: executes this statement as part of this file's behavior
export type ProjectRuntime = "node22";  // line 13: executes this statement as part of this file's behavior

export interface DiscordConfig {  // line 15: executes this statement as part of this file's behavior
  applicationId: string | null;  // line 16: executes this statement as part of this file's behavior
  clientId: string | null;  // line 17: executes this statement as part of this file's behavior
  defaultGuildId: string | null;  // line 18: executes this statement as part of this file's behavior
  tokenSecretRef: string | null;  // line 19: executes this statement as part of this file's behavior
  commandRegistration: CommandRegistration;  // line 20: executes this statement as part of this file's behavior
}  // line 21: executes this statement as part of this file's behavior

export interface PermissionConfig {  // line 23: executes this statement as part of this file's behavior
  intents: string[];  // line 24: executes this statement as part of this file's behavior
  botPermissions: string[];  // line 25: executes this statement as part of this file's behavior
}  // line 26: executes this statement as part of this file's behavior

export interface BotCommandOption {  // line 28: executes this statement as part of this file's behavior
  name: string;  // line 29: executes this statement as part of this file's behavior
  type: string;  // line 30: executes this statement as part of this file's behavior
  description?: string;  // line 31: executes this statement as part of this file's behavior
  required?: boolean;  // line 32: executes this statement as part of this file's behavior
}  // line 33: executes this statement as part of this file's behavior

export type BotCommandType = "chat_input";  // line 35: executes this statement as part of this file's behavior
export type BotCommandHandlerKind = "static_response" | "custom_typescript_placeholder";  // line 36: executes this statement as part of this file's behavior

export interface BotCommand {  // line 38: executes this statement as part of this file's behavior
  id?: string;  // line 39: executes this statement as part of this file's behavior
  name: string;  // line 40: executes this statement as part of this file's behavior
  description: string;  // line 41: executes this statement as part of this file's behavior
  type?: BotCommandType;  // line 42: executes this statement as part of this file's behavior
  options?: BotCommandOption[];  // line 43: executes this statement as part of this file's behavior
  permissions?: {  // line 44: executes this statement as part of this file's behavior
    defaultMemberPermissions: string | null;  // line 45: executes this statement as part of this file's behavior
    dmPermission: boolean;  // line 46: executes this statement as part of this file's behavior
  };  // line 47: executes this statement as part of this file's behavior
  handler?: string | {  // line 48: executes this statement as part of this file's behavior
    kind: BotCommandHandlerKind;  // line 49: executes this statement as part of this file's behavior
    ephemeral?: boolean;  // line 50: executes this statement as part of this file's behavior
    content?: string;  // line 51: executes this statement as part of this file's behavior
  };  // line 52: executes this statement as part of this file's behavior
}  // line 53: executes this statement as part of this file's behavior

export interface BotEvent {  // line 55: executes this statement as part of this file's behavior
  name: string;  // line 56: executes this statement as part of this file's behavior
  handler?: string;  // line 57: executes this statement as part of this file's behavior
}  // line 58: executes this statement as part of this file's behavior

export interface DeploymentConfig {  // line 60: executes this statement as part of this file's behavior
  targetId: string | null;  // line 61: executes this statement as part of this file's behavior
  lastDeploymentId: string | null;  // line 62: executes this statement as part of this file's behavior
}  // line 63: executes this statement as part of this file's behavior

export interface GitHubProjectConfig {  // line 65: executes this statement as part of this file's behavior
  owner: string | null;  // line 66: executes this statement as part of this file's behavior
  repo: string | null;  // line 67: executes this statement as part of this file's behavior
  defaultBranch: string;  // line 68: executes this statement as part of this file's behavior
  lastPushedAt: string | null;  // line 69: executes this statement as part of this file's behavior
}  // line 70: executes this statement as part of this file's behavior

export interface BotProject {  // line 72: executes this statement as part of this file's behavior
  id: string;  // line 73: executes this statement as part of this file's behavior
  name: string;  // line 74: executes this statement as part of this file's behavior
  slug: string;  // line 75: executes this statement as part of this file's behavior
  description: string;  // line 76: executes this statement as part of this file's behavior
  templateId: string;  // line 77: executes this statement as part of this file's behavior
  language: ProjectLanguage;  // line 78: executes this statement as part of this file's behavior
  runtime: ProjectRuntime;  // line 79: executes this statement as part of this file's behavior
  discord: DiscordConfig;  // line 80: executes this statement as part of this file's behavior
  permissions: PermissionConfig;  // line 81: executes this statement as part of this file's behavior
  commands: BotCommand[];  // line 82: executes this statement as part of this file's behavior
  events: BotEvent[];  // line 83: executes this statement as part of this file's behavior
  deployment: DeploymentConfig;  // line 84: executes this statement as part of this file's behavior
  github?: GitHubProjectConfig;  // line 85: executes this statement as part of this file's behavior
  archivedAt: string | null;  // line 86: executes this statement as part of this file's behavior
  createdAt: string;  // line 87: executes this statement as part of this file's behavior
  updatedAt: string;  // line 88: executes this statement as part of this file's behavior
}  // line 89: executes this statement as part of this file's behavior

export interface BotRuntimeStatus {  // line 91: executes this statement as part of this file's behavior
  running: boolean;  // line 92: executes this statement as part of this file's behavior
  projectId: string;  // line 93: executes this statement as part of this file's behavior
  status: "running" | "stopped";  // line 94: executes this statement as part of this file's behavior
  message: string;  // line 95: executes this statement as part of this file's behavior
}  // line 96: executes this statement as part of this file's behavior

export interface ValidationIssue {  // line 98: executes this statement as part of this file's behavior
  code: string;  // line 99: executes this statement as part of this file's behavior
  message: string;  // line 100: executes this statement as part of this file's behavior
  field?: string;  // line 101: executes this statement as part of this file's behavior
}  // line 102: executes this statement as part of this file's behavior

export interface ProjectValidationResult {  // line 104: executes this statement as part of this file's behavior
  valid: boolean;  // line 105: executes this statement as part of this file's behavior
  errors: ValidationIssue[];  // line 106: executes this statement as part of this file's behavior
  warnings: ValidationIssue[];  // line 107: executes this statement as part of this file's behavior
}  // line 108: executes this statement as part of this file's behavior
