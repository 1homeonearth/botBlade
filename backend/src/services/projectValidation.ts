// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import type { BotProject, ProjectValidationResult, ValidationIssue } from "../models/project.js";  // line 7: executes this statement as part of this file's behavior

const snowflakeRegex = /^[0-9]{17,20}$/;  // line 9: executes this statement as part of this file's behavior
const discordNameRegex = /^[a-z0-9_-]{1,32}$/;  // line 10: executes this statement as part of this file's behavior
const supportedOptionTypes = new Set(["string", "integer", "boolean", "user", "channel", "role", "mentionable", "number", "attachment"]);  // line 11: executes this statement as part of this file's behavior
export const supportedIntents = new Set([  // line 12: executes this statement as part of this file's behavior
  "Guilds",  // line 13: executes this statement as part of this file's behavior
  "GuildMembers",  // line 14: executes this statement as part of this file's behavior
  "GuildModeration",  // line 15: executes this statement as part of this file's behavior
  "GuildEmojisAndStickers",  // line 16: executes this statement as part of this file's behavior
  "GuildIntegrations",  // line 17: executes this statement as part of this file's behavior
  "GuildWebhooks",  // line 18: executes this statement as part of this file's behavior
  "GuildInvites",  // line 19: executes this statement as part of this file's behavior
  "GuildVoiceStates",  // line 20: executes this statement as part of this file's behavior
  "GuildPresences",  // line 21: executes this statement as part of this file's behavior
  "GuildMessages",  // line 22: executes this statement as part of this file's behavior
  "GuildMessageReactions",  // line 23: executes this statement as part of this file's behavior
  "GuildMessageTyping",  // line 24: executes this statement as part of this file's behavior
  "DirectMessages",  // line 25: executes this statement as part of this file's behavior
  "DirectMessageReactions",  // line 26: executes this statement as part of this file's behavior
  "DirectMessageTyping",  // line 27: executes this statement as part of this file's behavior
  "MessageContent",  // line 28: executes this statement as part of this file's behavior
  "GuildScheduledEvents",  // line 29: executes this statement as part of this file's behavior
  "AutoModerationConfiguration",  // line 30: executes this statement as part of this file's behavior
  "AutoModerationExecution",  // line 31: executes this statement as part of this file's behavior
]);  // line 32: executes this statement as part of this file's behavior
export const supportedBotPermissions = new Set([  // line 33: executes this statement as part of this file's behavior
  "ViewChannel",  // line 34: executes this statement as part of this file's behavior
  "SendMessages",  // line 35: executes this statement as part of this file's behavior
  "EmbedLinks",  // line 36: executes this statement as part of this file's behavior
  "AttachFiles",  // line 37: executes this statement as part of this file's behavior
  "ReadMessageHistory",  // line 38: executes this statement as part of this file's behavior
  "UseExternalEmojis",  // line 39: executes this statement as part of this file's behavior
  "ManageMessages",  // line 40: executes this statement as part of this file's behavior
  "ManageChannels",  // line 41: executes this statement as part of this file's behavior
  "KickMembers",  // line 42: executes this statement as part of this file's behavior
  "BanMembers",  // line 43: executes this statement as part of this file's behavior
  "ModerateMembers",  // line 44: executes this statement as part of this file's behavior
  "Administrator",  // line 45: executes this statement as part of this file's behavior
]);  // line 46: executes this statement as part of this file's behavior

export function validateProject(project: BotProject, secretExists: (secretId: string) => boolean = () => true): ProjectValidationResult {  // line 48: executes this statement as part of this file's behavior
  const errors: ValidationIssue[] = [];  // line 49: executes this statement as part of this file's behavior
  const warnings: ValidationIssue[] = [];  // line 50: executes this statement as part of this file's behavior

  requireField(project.name, "MISSING_PROJECT_NAME", "Project name is required.", "name", errors);  // line 52: executes this statement as part of this file's behavior
  requireField(project.slug, "MISSING_PROJECT_SLUG", "Project slug is required.", "slug", errors);  // line 53: executes this statement as part of this file's behavior
  if (project.runtime !== "node22") {  // line 54: executes this statement as part of this file's behavior
    errors.push({ code: "UNSUPPORTED_RUNTIME", message: "Only node22 runtime is supported in this phase.", field: "runtime" });  // line 55: executes this statement as part of this file's behavior
  }  // line 56: executes this statement as part of this file's behavior
  if (project.language !== "typescript") {  // line 57: executes this statement as part of this file's behavior
    errors.push({ code: "UNSUPPORTED_LANGUAGE", message: "Only typescript projects are supported in this phase.", field: "language" });  // line 58: executes this statement as part of this file's behavior
  }  // line 59: executes this statement as part of this file's behavior

  validateSnowflake(project.discord.applicationId, "discord.applicationId", "INVALID_APPLICATION_ID", errors);  // line 61: executes this statement as part of this file's behavior
  validateSnowflake(project.discord.clientId, "discord.clientId", "INVALID_CLIENT_ID", errors);  // line 62: executes this statement as part of this file's behavior
  validateSnowflake(project.discord.defaultGuildId, "discord.defaultGuildId", "INVALID_DEFAULT_GUILD_ID", errors);  // line 63: executes this statement as part of this file's behavior
  if (!project.discord.tokenSecretRef) {  // line 64: executes this statement as part of this file's behavior
    warnings.push({  // line 65: executes this statement as part of this file's behavior
      code: "MISSING_DISCORD_TOKEN",  // line 66: executes this statement as part of this file's behavior
      message: "Discord token secret reference is required before deployment.",  // line 67: executes this statement as part of this file's behavior
      field: "discord.tokenSecretRef",  // line 68: executes this statement as part of this file's behavior
    });  // line 69: executes this statement as part of this file's behavior
  } else if (!secretExists(project.discord.tokenSecretRef)) {  // line 70: executes this statement as part of this file's behavior
    errors.push({  // line 71: executes this statement as part of this file's behavior
      code: "INVALID_TOKEN_SECRET_REF",  // line 72: executes this statement as part of this file's behavior
      message: "Discord token secret reference does not match an existing secret.",  // line 73: executes this statement as part of this file's behavior
      field: "discord.tokenSecretRef",  // line 74: executes this statement as part of this file's behavior
    });  // line 75: executes this statement as part of this file's behavior
  }  // line 76: executes this statement as part of this file's behavior
  if (!["guild", "global"].includes(project.discord.commandRegistration)) {  // line 77: executes this statement as part of this file's behavior
    errors.push({ code: "INVALID_COMMAND_REGISTRATION", message: "Command registration must be guild or global.", field: "discord.commandRegistration" });  // line 78: executes this statement as part of this file's behavior
  }  // line 79: executes this statement as part of this file's behavior
  if (project.discord.commandRegistration === "global") {  // line 80: executes this statement as part of this file's behavior
    warnings.push({ code: "GLOBAL_COMMAND_PROPAGATION", message: "Global command registration can take longer to propagate.", field: "discord.commandRegistration" });  // line 81: executes this statement as part of this file's behavior
  }  // line 82: executes this statement as part of this file's behavior

  if (project.commands.length === 0) {  // line 84: executes this statement as part of this file's behavior
    warnings.push({ code: "NO_COMMANDS_DEFINED", message: "The project has no slash commands yet." });  // line 85: executes this statement as part of this file's behavior
  }  // line 86: executes this statement as part of this file's behavior
  const commandNames = new Set<string>();  // line 87: executes this statement as part of this file's behavior
  project.commands.forEach((command, index) => {  // line 88: executes this statement as part of this file's behavior
    const base = `commands.${index}`;  // line 89: executes this statement as part of this file's behavior
    if (!discordNameRegex.test(command.name)) {  // line 90: executes this statement as part of this file's behavior
      errors.push({ code: "INVALID_COMMAND_NAME", message: "Command names must be lowercase, 1-32 characters, and contain only letters, numbers, underscores, or hyphens.", field: `${base}.name` });  // line 91: executes this statement as part of this file's behavior
    }  // line 92: executes this statement as part of this file's behavior
    if (commandNames.has(command.name)) {  // line 93: executes this statement as part of this file's behavior
      errors.push({ code: "DUPLICATE_COMMAND_NAME", message: `Duplicate command name '${command.name}'.`, field: `${base}.name` });  // line 94: executes this statement as part of this file's behavior
    }  // line 95: executes this statement as part of this file's behavior
    commandNames.add(command.name);  // line 96: executes this statement as part of this file's behavior
    requireField(command.description, "MISSING_COMMAND_DESCRIPTION", "Command description is required.", `${base}.description`, errors);  // line 97: executes this statement as part of this file's behavior
    if (typeof command.handler === "string") requireField(command.handler, "MISSING_COMMAND_HANDLER", "Command handler must exist.", `${base}.handler`, errors);  // line 98: executes this statement as part of this file's behavior
    else if (!command.handler?.kind) errors.push({ code: "MISSING_COMMAND_HANDLER", message: "Command handler must exist.", field: `${base}.handler` });  // line 99: executes this statement as part of this file's behavior
    (command.options ?? []).forEach((option, optionIndex) => {  // line 100: executes this statement as part of this file's behavior
      const optionBase = `${base}.options.${optionIndex}`;  // line 101: executes this statement as part of this file's behavior
      if (!discordNameRegex.test(option.name)) {  // line 102: executes this statement as part of this file's behavior
        errors.push({ code: "INVALID_OPTION_NAME", message: "Command option names must be Discord-compatible lowercase names.", field: `${optionBase}.name` });  // line 103: executes this statement as part of this file's behavior
      }  // line 104: executes this statement as part of this file's behavior
      if (!supportedOptionTypes.has(option.type)) {  // line 105: executes this statement as part of this file's behavior
        errors.push({ code: "UNSUPPORTED_OPTION_TYPE", message: `Unsupported option type '${option.type}'.`, field: `${optionBase}.type` });  // line 106: executes this statement as part of this file's behavior
      }  // line 107: executes this statement as part of this file's behavior
    });  // line 108: executes this statement as part of this file's behavior
  });  // line 109: executes this statement as part of this file's behavior

  project.permissions.intents.forEach((intent, index) => {  // line 111: executes this statement as part of this file's behavior
    if (!supportedIntents.has(intent)) {  // line 112: executes this statement as part of this file's behavior
      errors.push({ code: "UNSUPPORTED_INTENT", message: `Unsupported intent '${intent}'.`, field: `permissions.intents.${index}` });  // line 113: executes this statement as part of this file's behavior
    }  // line 114: executes this statement as part of this file's behavior
  });  // line 115: executes this statement as part of this file's behavior
  project.permissions.botPermissions.forEach((permission, index) => {  // line 116: executes this statement as part of this file's behavior
    if (!supportedBotPermissions.has(permission)) {  // line 117: executes this statement as part of this file's behavior
      errors.push({ code: "UNSUPPORTED_BOT_PERMISSION", message: `Unsupported bot permission '${permission}'.`, field: `permissions.botPermissions.${index}` });  // line 118: executes this statement as part of this file's behavior
    }  // line 119: executes this statement as part of this file's behavior
  });  // line 120: executes this statement as part of this file's behavior

  return { valid: errors.length === 0, errors, warnings };  // line 122: executes this statement as part of this file's behavior
}  // line 123: executes this statement as part of this file's behavior

function requireField(value: string | undefined | null, code: string, message: string, field: string, errors: ValidationIssue[]): void {  // line 125: executes this statement as part of this file's behavior
  if (!value || value.trim().length === 0) {  // line 126: executes this statement as part of this file's behavior
    errors.push({ code, message, field });  // line 127: executes this statement as part of this file's behavior
  }  // line 128: executes this statement as part of this file's behavior
}  // line 129: executes this statement as part of this file's behavior

function validateSnowflake(value: string | null, field: string, code: string, errors: ValidationIssue[]): void {  // line 131: executes this statement as part of this file's behavior
  if (value !== null && !snowflakeRegex.test(value)) {  // line 132: executes this statement as part of this file's behavior
    errors.push({ code, message: "Discord IDs must be numeric snowflakes.", field });  // line 133: executes this statement as part of this file's behavior
  }  // line 134: executes this statement as part of this file's behavior
}  // line 135: executes this statement as part of this file's behavior
