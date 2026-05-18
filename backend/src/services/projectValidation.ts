import type { BotProject, ProjectValidationResult, ValidationIssue } from "../models/project.js";

const snowflakeRegex = /^[0-9]{17,20}$/;
const discordNameRegex = /^[a-z0-9_-]{1,32}$/;
const supportedOptionTypes = new Set(["string", "integer", "boolean", "user", "channel", "role", "mentionable", "number", "attachment"]);
export const supportedIntents = new Set([
  "Guilds",
  "GuildMembers",
  "GuildModeration",
  "GuildEmojisAndStickers",
  "GuildIntegrations",
  "GuildWebhooks",
  "GuildInvites",
  "GuildVoiceStates",
  "GuildPresences",
  "GuildMessages",
  "GuildMessageReactions",
  "GuildMessageTyping",
  "DirectMessages",
  "DirectMessageReactions",
  "DirectMessageTyping",
  "MessageContent",
  "GuildScheduledEvents",
  "AutoModerationConfiguration",
  "AutoModerationExecution",
]);
export const supportedBotPermissions = new Set([
  "ViewChannel",
  "SendMessages",
  "EmbedLinks",
  "AttachFiles",
  "ReadMessageHistory",
  "UseExternalEmojis",
  "ManageMessages",
  "ManageChannels",
  "KickMembers",
  "BanMembers",
  "ModerateMembers",
  "Administrator",
]);

export function validateProject(project: BotProject): ProjectValidationResult {
  const errors: ValidationIssue[] = [];
  const warnings: ValidationIssue[] = [];

  requireField(project.name, "MISSING_PROJECT_NAME", "Project name is required.", "name", errors);
  requireField(project.slug, "MISSING_PROJECT_SLUG", "Project slug is required.", "slug", errors);
  if (project.runtime !== "node22") {
    errors.push({ code: "UNSUPPORTED_RUNTIME", message: "Only node22 runtime is supported in this phase.", field: "runtime" });
  }
  if (project.language !== "typescript") {
    errors.push({ code: "UNSUPPORTED_LANGUAGE", message: "Only typescript projects are supported in this phase.", field: "language" });
  }

  validateSnowflake(project.discord.applicationId, "discord.applicationId", "INVALID_APPLICATION_ID", errors);
  validateSnowflake(project.discord.clientId, "discord.clientId", "INVALID_CLIENT_ID", errors);
  validateSnowflake(project.discord.defaultGuildId, "discord.defaultGuildId", "INVALID_DEFAULT_GUILD_ID", errors);
  if (!project.discord.tokenSecretRef) {
    warnings.push({
      code: "MISSING_DISCORD_TOKEN",
      message: "Discord token secret reference is required before deployment.",
      field: "discord.tokenSecretRef",
    });
  }
  if (!["guild", "global"].includes(project.discord.commandRegistration)) {
    errors.push({ code: "INVALID_COMMAND_REGISTRATION", message: "Command registration must be guild or global.", field: "discord.commandRegistration" });
  }
  if (project.discord.commandRegistration === "global") {
    warnings.push({ code: "GLOBAL_COMMAND_PROPAGATION", message: "Global command registration can take longer to propagate.", field: "discord.commandRegistration" });
  }

  if (project.commands.length === 0) {
    warnings.push({ code: "NO_COMMANDS_DEFINED", message: "The project has no slash commands yet." });
  }
  const commandNames = new Set<string>();
  project.commands.forEach((command, index) => {
    const base = `commands.${index}`;
    if (!discordNameRegex.test(command.name)) {
      errors.push({ code: "INVALID_COMMAND_NAME", message: "Command names must be lowercase, 1-32 characters, and contain only letters, numbers, underscores, or hyphens.", field: `${base}.name` });
    }
    if (commandNames.has(command.name)) {
      errors.push({ code: "DUPLICATE_COMMAND_NAME", message: `Duplicate command name '${command.name}'.`, field: `${base}.name` });
    }
    commandNames.add(command.name);
    requireField(command.description, "MISSING_COMMAND_DESCRIPTION", "Command description is required.", `${base}.description`, errors);
    requireField(command.handler, "MISSING_COMMAND_HANDLER", "Command handler must exist.", `${base}.handler`, errors);
    (command.options ?? []).forEach((option, optionIndex) => {
      const optionBase = `${base}.options.${optionIndex}`;
      if (!discordNameRegex.test(option.name)) {
        errors.push({ code: "INVALID_OPTION_NAME", message: "Command option names must be Discord-compatible lowercase names.", field: `${optionBase}.name` });
      }
      if (!supportedOptionTypes.has(option.type)) {
        errors.push({ code: "UNSUPPORTED_OPTION_TYPE", message: `Unsupported option type '${option.type}'.`, field: `${optionBase}.type` });
      }
    });
  });

  project.permissions.intents.forEach((intent, index) => {
    if (!supportedIntents.has(intent)) {
      errors.push({ code: "UNSUPPORTED_INTENT", message: `Unsupported intent '${intent}'.`, field: `permissions.intents.${index}` });
    }
  });
  project.permissions.botPermissions.forEach((permission, index) => {
    if (!supportedBotPermissions.has(permission)) {
      errors.push({ code: "UNSUPPORTED_BOT_PERMISSION", message: `Unsupported bot permission '${permission}'.`, field: `permissions.botPermissions.${index}` });
    }
  });

  return { valid: errors.length === 0, errors, warnings };
}

function requireField(value: string | undefined | null, code: string, message: string, field: string, errors: ValidationIssue[]): void {
  if (!value || value.trim().length === 0) {
    errors.push({ code, message, field });
  }
}

function validateSnowflake(value: string | null, field: string, code: string, errors: ValidationIssue[]): void {
  if (value !== null && !snowflakeRegex.test(value)) {
    errors.push({ code, message: "Discord IDs must be numeric snowflakes.", field });
  }
}
