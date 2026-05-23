// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import { randomUUID } from "node:crypto";  // line 7: executes this statement as part of this file's behavior
import type { BotCommand, BotCommandHandlerKind, BotCommandOption } from "../models/project.js";  // line 8: executes this statement as part of this file's behavior
import { RequestValidationError } from "./projectStore.js";  // line 9: executes this statement as part of this file's behavior

const commandNamePattern = /^[\p{Ll}\p{N}_-]{1,32}$/u;  // line 11: executes this statement as part of this file's behavior

export function parseCommandDefinition(input: unknown, existingId?: string): BotCommand {  // line 13: executes this statement as part of this file's behavior
  const object = asRecord(input);  // line 14: executes this statement as part of this file's behavior
  const name = stringField(object, "name");  // line 15: executes this statement as part of this file's behavior
  const description = stringField(object, "description");  // line 16: executes this statement as part of this file's behavior
  const type = stringField(object, "type", "chat_input");  // line 17: executes this statement as part of this file's behavior
  const handler = asRecord(object.handler);  // line 18: executes this statement as part of this file's behavior
  const handlerKind = stringField(handler, "kind", "static_response");  // line 19: executes this statement as part of this file's behavior
  const problems = validateCommandFields(name, description, type, handlerKind, handler);  // line 20: executes this statement as part of this file's behavior
  if (problems.length > 0) throw new RequestValidationError(problems);  // line 21: executes this statement as part of this file's behavior
  const permissions = asRecord(object.permissions);  // line 22: executes this statement as part of this file's behavior
  return {  // line 23: executes this statement as part of this file's behavior
    id: typeof object.id === "string" && object.id.trim() ? object.id.trim() : existingId ?? `cmd_${randomUUID()}`,  // line 24: executes this statement as part of this file's behavior
    name,  // line 25: executes this statement as part of this file's behavior
    description,  // line 26: executes this statement as part of this file's behavior
    type: "chat_input",  // line 27: executes this statement as part of this file's behavior
    options: Array.isArray(object.options) ? object.options.filter((item): item is BotCommandOption => typeof item === "object" && item !== null) : [],  // line 28: executes this statement as part of this file's behavior
    permissions: {  // line 29: executes this statement as part of this file's behavior
      defaultMemberPermissions: typeof permissions.defaultMemberPermissions === "string" ? permissions.defaultMemberPermissions : null,  // line 30: executes this statement as part of this file's behavior
      dmPermission: typeof permissions.dmPermission === "boolean" ? permissions.dmPermission : false,  // line 31: executes this statement as part of this file's behavior
    },  // line 32: executes this statement as part of this file's behavior
    handler: {  // line 33: executes this statement as part of this file's behavior
      kind: handlerKind as BotCommandHandlerKind,  // line 34: executes this statement as part of this file's behavior
      ephemeral: typeof handler.ephemeral === "boolean" ? handler.ephemeral : true,  // line 35: executes this statement as part of this file's behavior
      content: typeof handler.content === "string" ? handler.content : undefined,  // line 36: executes this statement as part of this file's behavior
    },  // line 37: executes this statement as part of this file's behavior
  };  // line 38: executes this statement as part of this file's behavior
}  // line 39: executes this statement as part of this file's behavior

export function parseCommandPatch(input: unknown, existing: BotCommand): BotCommand {  // line 41: executes this statement as part of this file's behavior
  const currentHandler = typeof existing.handler === "object" && existing.handler ? existing.handler : { kind: "custom_typescript_placeholder" };  // line 42: executes this statement as part of this file's behavior
  const object = asRecord(input);  // line 43: executes this statement as part of this file's behavior
  return parseCommandDefinition({ ...existing, ...object, handler: { ...currentHandler, ...asRecord(object.handler) }, permissions: { ...(existing.permissions ?? {}), ...asRecord(object.permissions) } }, existing.id);  // line 44: executes this statement as part of this file's behavior
}  // line 45: executes this statement as part of this file's behavior

export function validateCommands(commands: BotCommand[]): void {  // line 47: executes this statement as part of this file's behavior
  const names = new Set<string>();  // line 48: executes this statement as part of this file's behavior
  const problems = commands.flatMap((command, index) => {  // line 49: executes this statement as part of this file's behavior
    const handler = typeof command.handler === "object" && command.handler ? command.handler : { kind: typeof command.handler === "string" ? "custom_typescript_placeholder" : undefined };  // line 50: executes this statement as part of this file's behavior
    const commandProblems = validateCommandFields(command.name, command.description, command.type ?? "chat_input", handler.kind, handler);  // line 51: executes this statement as part of this file's behavior
    if (names.has(command.name)) commandProblems.push({ field: `commands.${index}.name`, message: "Command names must be unique." });  // line 52: executes this statement as part of this file's behavior
    names.add(command.name);  // line 53: executes this statement as part of this file's behavior
    return commandProblems.map((problem) => ({ ...problem, field: `commands.${index}.${problem.field}` }));  // line 54: executes this statement as part of this file's behavior
  });  // line 55: executes this statement as part of this file's behavior
  if (problems.length > 0) throw new RequestValidationError(problems);  // line 56: executes this statement as part of this file's behavior
}  // line 57: executes this statement as part of this file's behavior

function validateCommandFields(name: string, description: string, type: string | undefined, handlerKind: string | undefined, handler: Record<string, unknown>) {  // line 59: executes this statement as part of this file's behavior
  const problems: { field: string; message: string }[] = [];  // line 60: executes this statement as part of this file's behavior
  if (!commandNamePattern.test(name)) problems.push({ field: "name", message: "Discord command names must be 1-32 lowercase characters, numbers, underscores, or hyphens." });  // line 61: executes this statement as part of this file's behavior
  if (description.length < 1 || description.length > 100) problems.push({ field: "description", message: "Discord command descriptions must be 1-100 characters." });  // line 62: executes this statement as part of this file's behavior
  if (type !== "chat_input") problems.push({ field: "type", message: "Only chat_input commands are supported." });  // line 63: executes this statement as part of this file's behavior
  if (handlerKind !== "static_response" && handlerKind !== "custom_typescript_placeholder") problems.push({ field: "handler.kind", message: "Handler kind must be static_response or custom_typescript_placeholder." });  // line 64: executes this statement as part of this file's behavior
  if (handlerKind === "static_response" && (typeof handler.content !== "string" || handler.content.trim().length === 0)) problems.push({ field: "handler.content", message: "Static response commands require response content." });  // line 65: executes this statement as part of this file's behavior
  return problems;  // line 66: executes this statement as part of this file's behavior
}  // line 67: executes this statement as part of this file's behavior

function asRecord(value: unknown): Record<string, unknown> { return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {}; }  // line 69: executes this statement as part of this file's behavior

function stringField(object: Record<string, unknown>, field: string, fallback?: string): string {  // line 71: executes this statement as part of this file's behavior
  const value = object[field];  // line 72: executes this statement as part of this file's behavior
  if (typeof value === "string" && value.trim()) return value.trim();  // line 73: executes this statement as part of this file's behavior
  if (fallback !== undefined) return fallback;  // line 74: executes this statement as part of this file's behavior
  throw new RequestValidationError([{ field, message: `${field} is required.` }]);  // line 75: executes this statement as part of this file's behavior
}  // line 76: executes this statement as part of this file's behavior
