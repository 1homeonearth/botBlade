import { randomUUID } from "node:crypto";
import type { BotCommand, BotCommandHandlerKind, BotCommandOption } from "../models/project.js";
import { RequestValidationError } from "./projectStore.js";

const commandNamePattern = /^[\p{Ll}\p{N}_-]{1,32}$/u;

export function parseCommandDefinition(input: unknown, existingId?: string): BotCommand {
  const object = asRecord(input);
  const name = stringField(object, "name");
  const description = stringField(object, "description");
  const type = stringField(object, "type", "chat_input");
  const handler = asRecord(object.handler);
  const handlerKind = stringField(handler, "kind", "static_response");
  const problems = validateCommandFields(name, description, type, handlerKind, handler);
  if (problems.length > 0) throw new RequestValidationError(problems);
  const permissions = asRecord(object.permissions);
  return {
    id: typeof object.id === "string" && object.id.trim() ? object.id.trim() : existingId ?? `cmd_${randomUUID()}`,
    name,
    description,
    type: "chat_input",
    options: Array.isArray(object.options) ? object.options.filter((item): item is BotCommandOption => typeof item === "object" && item !== null) : [],
    permissions: {
      defaultMemberPermissions: typeof permissions.defaultMemberPermissions === "string" ? permissions.defaultMemberPermissions : null,
      dmPermission: typeof permissions.dmPermission === "boolean" ? permissions.dmPermission : false,
    },
    handler: {
      kind: handlerKind as BotCommandHandlerKind,
      ephemeral: typeof handler.ephemeral === "boolean" ? handler.ephemeral : true,
      content: typeof handler.content === "string" ? handler.content : undefined,
    },
  };
}

export function parseCommandPatch(input: unknown, existing: BotCommand): BotCommand {
  const currentHandler = typeof existing.handler === "object" && existing.handler ? existing.handler : { kind: "custom_typescript_placeholder" };
  const object = asRecord(input);
  return parseCommandDefinition({ ...existing, ...object, handler: { ...currentHandler, ...asRecord(object.handler) }, permissions: { ...(existing.permissions ?? {}), ...asRecord(object.permissions) } }, existing.id);
}

export function validateCommands(commands: BotCommand[]): void {
  const names = new Set<string>();
  const problems = commands.flatMap((command, index) => {
    const handler = typeof command.handler === "object" && command.handler ? command.handler : { kind: typeof command.handler === "string" ? "custom_typescript_placeholder" : undefined };
    const commandProblems = validateCommandFields(command.name, command.description, command.type ?? "chat_input", handler.kind, handler);
    if (names.has(command.name)) commandProblems.push({ field: `commands.${index}.name`, message: "Command names must be unique." });
    names.add(command.name);
    return commandProblems.map((problem) => ({ ...problem, field: `commands.${index}.${problem.field}` }));
  });
  if (problems.length > 0) throw new RequestValidationError(problems);
}

function validateCommandFields(name: string, description: string, type: string | undefined, handlerKind: string | undefined, handler: Record<string, unknown>) {
  const problems: { field: string; message: string }[] = [];
  if (!commandNamePattern.test(name)) problems.push({ field: "name", message: "Discord command names must be 1-32 lowercase characters, numbers, underscores, or hyphens." });
  if (description.length < 1 || description.length > 100) problems.push({ field: "description", message: "Discord command descriptions must be 1-100 characters." });
  if (type !== "chat_input") problems.push({ field: "type", message: "Only chat_input commands are supported." });
  if (handlerKind !== "static_response" && handlerKind !== "custom_typescript_placeholder") problems.push({ field: "handler.kind", message: "Handler kind must be static_response or custom_typescript_placeholder." });
  if (handlerKind === "static_response" && (typeof handler.content !== "string" || handler.content.trim().length === 0)) problems.push({ field: "handler.content", message: "Static response commands require response content." });
  return problems;
}

function asRecord(value: unknown): Record<string, unknown> { return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {}; }

function stringField(object: Record<string, unknown>, field: string, fallback?: string): string {
  const value = object[field];
  if (typeof value === "string" && value.trim()) return value.trim();
  if (fallback !== undefined) return fallback;
  throw new RequestValidationError([{ field, message: `${field} is required.` }]);
}
