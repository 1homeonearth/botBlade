// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import { randomUUID } from "node:crypto";  // line 7: executes this statement as part of this file's behavior
import type { BotCommand, BotEvent, BotProject, CommandRegistration } from "../models/project.js";  // line 8: executes this statement as part of this file's behavior
import { parseCommandDefinition, validateCommands } from "./commandDefinitions.js";  // line 9: executes this statement as part of this file's behavior
import type { ProjectStorePersistence, ProjectStorePort } from "./persistence.js";  // line 10: executes this statement as part of this file's behavior

export const DEFAULT_TEMPLATE_ID = "template_blank_discord_ts";  // line 12: executes this statement as part of this file's behavior

export interface CreateProjectInput {  // line 14: executes this statement as part of this file's behavior
  name: string;  // line 15: executes this statement as part of this file's behavior
  slug?: string;  // line 16: executes this statement as part of this file's behavior
  description?: string;  // line 17: executes this statement as part of this file's behavior
  templateId?: string;  // line 18: executes this statement as part of this file's behavior
  language?: "typescript";  // line 19: executes this statement as part of this file's behavior
  runtime?: "node22";  // line 20: executes this statement as part of this file's behavior
  discord?: Partial<BotProject["discord"]>;  // line 21: executes this statement as part of this file's behavior
  permissions?: Partial<BotProject["permissions"]>;  // line 22: executes this statement as part of this file's behavior
  commands?: BotCommand[];  // line 23: executes this statement as part of this file's behavior
  events?: BotEvent[];  // line 24: executes this statement as part of this file's behavior
  deployment?: Partial<BotProject["deployment"]>;  // line 25: executes this statement as part of this file's behavior
  github?: Partial<BotProject["github"]>;  // line 26: executes this statement as part of this file's behavior
}  // line 27: executes this statement as part of this file's behavior

export interface UpdateProjectInput {  // line 29: executes this statement as part of this file's behavior
  name?: string;  // line 30: executes this statement as part of this file's behavior
  slug?: string;  // line 31: executes this statement as part of this file's behavior
  description?: string;  // line 32: executes this statement as part of this file's behavior
  templateId?: string;  // line 33: executes this statement as part of this file's behavior
  discord?: Partial<BotProject["discord"]>;  // line 34: executes this statement as part of this file's behavior
  permissions?: Partial<BotProject["permissions"]>;  // line 35: executes this statement as part of this file's behavior
  commands?: BotCommand[];  // line 36: executes this statement as part of this file's behavior
  events?: BotEvent[];  // line 37: executes this statement as part of this file's behavior
  deployment?: Partial<BotProject["deployment"]>;  // line 38: executes this statement as part of this file's behavior
  github?: Partial<BotProject["github"]>;  // line 39: executes this statement as part of this file's behavior
}  // line 40: executes this statement as part of this file's behavior

export interface ValidationProblem {  // line 42: executes this statement as part of this file's behavior
  field: string;  // line 43: executes this statement as part of this file's behavior
  message: string;  // line 44: executes this statement as part of this file's behavior
}  // line 45: executes this statement as part of this file's behavior

export class RequestValidationError extends Error {  // line 47: executes this statement as part of this file's behavior
  constructor(public readonly problems: ValidationProblem[]) {  // line 48: executes this statement as part of this file's behavior
    super("Request validation failed.");  // line 49: executes this statement as part of this file's behavior
    this.name = "RequestValidationError";  // line 50: executes this statement as part of this file's behavior
  }  // line 51: executes this statement as part of this file's behavior
}  // line 52: executes this statement as part of this file's behavior

export function parseCreateProjectInput(value: unknown): CreateProjectInput {  // line 54: executes this statement as part of this file's behavior
  const object = asRecord(value);  // line 55: executes this statement as part of this file's behavior
  const name = stringField(object, "name", true);  // line 56: executes this statement as part of this file's behavior
  const commandRegistration = optionalNestedString(object, "discord", "commandRegistration") ?? "guild";  // line 57: executes this statement as part of this file's behavior
  const language = stringField(object, "language", false) ?? "typescript";  // line 58: executes this statement as part of this file's behavior
  const runtime = stringField(object, "runtime", false) ?? "node22";  // line 59: executes this statement as part of this file's behavior
  const problems: ValidationProblem[] = [];  // line 60: executes this statement as part of this file's behavior
  if (!name) problems.push({ field: "name", message: "Project name is required." });  // line 61: executes this statement as part of this file's behavior
  if (!["guild", "global"].includes(commandRegistration)) problems.push({ field: "discord.commandRegistration", message: "Command registration must be guild or global." });  // line 62: executes this statement as part of this file's behavior
  if (language !== "typescript") problems.push({ field: "language", message: "Language must be typescript." });  // line 63: executes this statement as part of this file's behavior
  if (runtime !== "node22") problems.push({ field: "runtime", message: "Runtime must be node22." });  // line 64: executes this statement as part of this file's behavior
  if (problems.length > 0) throw new RequestValidationError(problems);  // line 65: executes this statement as part of this file's behavior
  const commands = Array.isArray(object.commands) ? object.commands.map((command) => parseCommandDefinition(command)) : [];  // line 66: executes this statement as part of this file's behavior
  validateCommands(commands);  // line 67: executes this statement as part of this file's behavior
  return {  // line 68: executes this statement as part of this file's behavior
    name: name!,  // line 69: executes this statement as part of this file's behavior
    slug: stringField(object, "slug", false),  // line 70: executes this statement as part of this file's behavior
    description: stringField(object, "description", false) ?? "",  // line 71: executes this statement as part of this file's behavior
    templateId: stringField(object, "templateId", false) ?? DEFAULT_TEMPLATE_ID,  // line 72: executes this statement as part of this file's behavior
    language: "typescript",  // line 73: executes this statement as part of this file's behavior
    runtime: "node22",  // line 74: executes this statement as part of this file's behavior
    discord: parseDiscord(object),  // line 75: executes this statement as part of this file's behavior
    permissions: parsePermissions(object),  // line 76: executes this statement as part of this file's behavior
    commands,  // line 77: executes this statement as part of this file's behavior
    events: Array.isArray(object.events) ? object.events as BotEvent[] : [],  // line 78: executes this statement as part of this file's behavior
    deployment: parseDeployment(object),  // line 79: executes this statement as part of this file's behavior
    github: parseGithub(object),  // line 80: executes this statement as part of this file's behavior
  };  // line 81: executes this statement as part of this file's behavior
}  // line 82: executes this statement as part of this file's behavior

export function parseUpdateProjectInput(value: unknown): UpdateProjectInput {  // line 84: executes this statement as part of this file's behavior
  const object = asRecord(value);  // line 85: executes this statement as part of this file's behavior
  const allowed = new Set(["name", "slug", "description", "templateId", "discord", "permissions", "commands", "events", "deployment", "github"]);  // line 86: executes this statement as part of this file's behavior
  const problems = Object.keys(object).filter((key) => !allowed.has(key)).map((key) => ({ field: key, message: "Field is not patchable." }));  // line 87: executes this statement as part of this file's behavior
  const commandRegistration = optionalNestedString(object, "discord", "commandRegistration");  // line 88: executes this statement as part of this file's behavior
  if (commandRegistration && !["guild", "global"].includes(commandRegistration)) problems.push({ field: "discord.commandRegistration", message: "Command registration must be guild or global." });  // line 89: executes this statement as part of this file's behavior
  const commands = Array.isArray(object.commands) ? object.commands.map((command) => parseCommandDefinition(command)) : undefined;  // line 90: executes this statement as part of this file's behavior
  if (commands) validateCommands(commands);  // line 91: executes this statement as part of this file's behavior
  if (problems.length > 0) throw new RequestValidationError(problems);  // line 92: executes this statement as part of this file's behavior
  return {  // line 93: executes this statement as part of this file's behavior
    name: stringField(object, "name", false),  // line 94: executes this statement as part of this file's behavior
    slug: stringField(object, "slug", false),  // line 95: executes this statement as part of this file's behavior
    description: stringField(object, "description", false),  // line 96: executes this statement as part of this file's behavior
    templateId: stringField(object, "templateId", false),  // line 97: executes this statement as part of this file's behavior
    discord: parseDiscord(object),  // line 98: executes this statement as part of this file's behavior
    permissions: parsePermissions(object),  // line 99: executes this statement as part of this file's behavior
    commands,  // line 100: executes this statement as part of this file's behavior
    events: Array.isArray(object.events) ? object.events as BotEvent[] : undefined,  // line 101: executes this statement as part of this file's behavior
    deployment: parseDeployment(object),  // line 102: executes this statement as part of this file's behavior
    github: parseGithub(object),  // line 103: executes this statement as part of this file's behavior
  };  // line 104: executes this statement as part of this file's behavior
}  // line 105: executes this statement as part of this file's behavior

export function parseToggleAction(value: unknown): "start" | "stop" {  // line 107: executes this statement as part of this file's behavior
  const object = asRecord(value);  // line 108: executes this statement as part of this file's behavior
  if (object.action === "start" || object.action === "stop") return object.action;  // line 109: executes this statement as part of this file's behavior
  throw new RequestValidationError([{ field: "action", message: "Action must be start or stop." }]);  // line 110: executes this statement as part of this file's behavior
}  // line 111: executes this statement as part of this file's behavior

export function slugify(value: string): string {  // line 113: executes this statement as part of this file's behavior
  return value.trim().toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "") || "project";  // line 114: executes this statement as part of this file's behavior
}  // line 115: executes this statement as part of this file's behavior

export class DuplicateSlugError extends Error {  // line 117: executes this statement as part of this file's behavior
  constructor(slug: string) {  // line 118: executes this statement as part of this file's behavior
    super(`Project slug '${slug}' already exists.`);  // line 119: executes this statement as part of this file's behavior
    this.name = "DuplicateSlugError";  // line 120: executes this statement as part of this file's behavior
  }  // line 121: executes this statement as part of this file's behavior
}  // line 122: executes this statement as part of this file's behavior

export class ProjectStore implements ProjectStorePort {  // line 124: executes this statement as part of this file's behavior
  private readonly projects = new Map<string, BotProject>();  // line 125: executes this statement as part of this file's behavior

  constructor(private readonly persistence?: ProjectStorePersistence) {  // line 127: executes this statement as part of this file's behavior
    for (const project of persistence?.loadProjects() ?? []) this.projects.set(project.id, project);  // line 128: executes this statement as part of this file's behavior
  }  // line 129: executes this statement as part of this file's behavior

  list(): BotProject[] {  // line 131: executes this statement as part of this file's behavior
    return [...this.projects.values()].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));  // line 132: executes this statement as part of this file's behavior
  }  // line 133: executes this statement as part of this file's behavior

  get(projectId: string): BotProject | undefined {  // line 135: executes this statement as part of this file's behavior
    return this.projects.get(projectId);  // line 136: executes this statement as part of this file's behavior
  }  // line 137: executes this statement as part of this file's behavior

  create(input: CreateProjectInput): BotProject {  // line 139: executes this statement as part of this file's behavior
    const now = new Date().toISOString();  // line 140: executes this statement as part of this file's behavior
    const slug = slugify(input.slug ?? input.name);  // line 141: executes this statement as part of this file's behavior
    this.assertSlugAvailable(slug);  // line 142: executes this statement as part of this file's behavior
    const project: BotProject = {  // line 143: executes this statement as part of this file's behavior
      id: `project_${randomUUID()}`,  // line 144: executes this statement as part of this file's behavior
      name: input.name,  // line 145: executes this statement as part of this file's behavior
      slug,  // line 146: executes this statement as part of this file's behavior
      description: input.description ?? "",  // line 147: executes this statement as part of this file's behavior
      templateId: input.templateId ?? DEFAULT_TEMPLATE_ID,  // line 148: executes this statement as part of this file's behavior
      language: "typescript",  // line 149: executes this statement as part of this file's behavior
      runtime: "node22",  // line 150: executes this statement as part of this file's behavior
      discord: {  // line 151: executes this statement as part of this file's behavior
        applicationId: input.discord?.applicationId ?? null,  // line 152: executes this statement as part of this file's behavior
        clientId: input.discord?.clientId ?? null,  // line 153: executes this statement as part of this file's behavior
        defaultGuildId: input.discord?.defaultGuildId ?? null,  // line 154: executes this statement as part of this file's behavior
        tokenSecretRef: input.discord?.tokenSecretRef ?? null,  // line 155: executes this statement as part of this file's behavior
        commandRegistration: input.discord?.commandRegistration ?? "guild",  // line 156: executes this statement as part of this file's behavior
      },  // line 157: executes this statement as part of this file's behavior
      permissions: { intents: input.permissions?.intents ?? [], botPermissions: input.permissions?.botPermissions ?? [] },  // line 158: executes this statement as part of this file's behavior
      commands: input.commands ?? [],  // line 159: executes this statement as part of this file's behavior
      events: input.events ?? [],  // line 160: executes this statement as part of this file's behavior
      deployment: { targetId: input.deployment?.targetId ?? null, lastDeploymentId: input.deployment?.lastDeploymentId ?? null },  // line 161: executes this statement as part of this file's behavior
      github: { owner: input.github?.owner ?? null, repo: input.github?.repo ?? null, defaultBranch: input.github?.defaultBranch ?? "main", lastPushedAt: input.github?.lastPushedAt ?? null },  // line 162: executes this statement as part of this file's behavior
      archivedAt: null,  // line 163: executes this statement as part of this file's behavior
      createdAt: now,  // line 164: executes this statement as part of this file's behavior
      updatedAt: now,  // line 165: executes this statement as part of this file's behavior
    };  // line 166: executes this statement as part of this file's behavior
    this.projects.set(project.id, project);  // line 167: executes this statement as part of this file's behavior
    this.persistence?.saveProject(project);  // line 168: executes this statement as part of this file's behavior
    return project;  // line 169: executes this statement as part of this file's behavior
  }  // line 170: executes this statement as part of this file's behavior

  update(projectId: string, input: UpdateProjectInput): BotProject | undefined {  // line 172: executes this statement as part of this file's behavior
    const existing = this.projects.get(projectId);  // line 173: executes this statement as part of this file's behavior
    if (!existing) return undefined;  // line 174: executes this statement as part of this file's behavior
    const nextSlug = input.slug === undefined ? existing.slug : slugify(input.slug);  // line 175: executes this statement as part of this file's behavior
    this.assertSlugAvailable(nextSlug, projectId);  // line 176: executes this statement as part of this file's behavior
    const updated: BotProject = {  // line 177: executes this statement as part of this file's behavior
      ...existing,  // line 178: executes this statement as part of this file's behavior
      ...withoutUndefined(input as Record<string, unknown>),  // line 179: executes this statement as part of this file's behavior
      slug: nextSlug,  // line 180: executes this statement as part of this file's behavior
      language: existing.language,  // line 181: executes this statement as part of this file's behavior
      runtime: existing.runtime,  // line 182: executes this statement as part of this file's behavior
      discord: { ...existing.discord, ...withoutUndefined(input.discord ?? {}) },  // line 183: executes this statement as part of this file's behavior
      permissions: { ...existing.permissions, ...withoutUndefined(input.permissions ?? {}) },  // line 184: executes this statement as part of this file's behavior
      deployment: { ...existing.deployment, ...withoutUndefined(input.deployment ?? {}) },  // line 185: executes this statement as part of this file's behavior
      github: { owner: null, repo: null, defaultBranch: "main", lastPushedAt: null, ...(existing.github ?? {}), ...withoutUndefined(input.github ?? {}) },  // line 186: executes this statement as part of this file's behavior
      updatedAt: new Date().toISOString(),  // line 187: executes this statement as part of this file's behavior
    };  // line 188: executes this statement as part of this file's behavior
    this.projects.set(projectId, updated);  // line 189: executes this statement as part of this file's behavior
    this.persistence?.saveProject(updated);  // line 190: executes this statement as part of this file's behavior
    return updated;  // line 191: executes this statement as part of this file's behavior
  }  // line 192: executes this statement as part of this file's behavior

  delete(projectId: string): boolean {  // line 194: executes this statement as part of this file's behavior
    const deleted = this.projects.delete(projectId);  // line 195: executes this statement as part of this file's behavior
    if (deleted) this.persistence?.deleteProject(projectId);  // line 196: executes this statement as part of this file's behavior
    return deleted;  // line 197: executes this statement as part of this file's behavior
  }  // line 198: executes this statement as part of this file's behavior

  archive(projectId: string): BotProject | undefined {  // line 200: executes this statement as part of this file's behavior
    const existing = this.projects.get(projectId);  // line 201: executes this statement as part of this file's behavior
    if (!existing) return undefined;  // line 202: executes this statement as part of this file's behavior
    const now = new Date().toISOString();  // line 203: executes this statement as part of this file's behavior
    const archived = { ...existing, archivedAt: existing.archivedAt ?? now, updatedAt: now };  // line 204: executes this statement as part of this file's behavior
    this.projects.set(projectId, archived);  // line 205: executes this statement as part of this file's behavior
    this.persistence?.saveProject(archived);  // line 206: executes this statement as part of this file's behavior
    return archived;  // line 207: executes this statement as part of this file's behavior
  }  // line 208: executes this statement as part of this file's behavior

  clone(projectId: string): BotProject | undefined {  // line 210: executes this statement as part of this file's behavior
    const existing = this.projects.get(projectId);  // line 211: executes this statement as part of this file's behavior
    if (!existing) return undefined;  // line 212: executes this statement as part of this file's behavior
    const now = new Date().toISOString();  // line 213: executes this statement as part of this file's behavior
    const clone = { ...existing, id: `project_${randomUUID()}`, name: `${existing.name} Copy`, slug: this.disambiguateSlug(`${existing.slug}-copy`), archivedAt: null, createdAt: now, updatedAt: now };  // line 214: executes this statement as part of this file's behavior
    this.projects.set(clone.id, clone);  // line 215: executes this statement as part of this file's behavior
    this.persistence?.saveProject(clone);  // line 216: executes this statement as part of this file's behavior
    return clone;  // line 217: executes this statement as part of this file's behavior
  }  // line 218: executes this statement as part of this file's behavior

  private assertSlugAvailable(slug: string, currentProjectId?: string): void {  // line 220: executes this statement as part of this file's behavior
    if ([...this.projects.values()].some((project) => project.slug === slug && project.id !== currentProjectId)) throw new DuplicateSlugError(slug);  // line 221: executes this statement as part of this file's behavior
  }  // line 222: executes this statement as part of this file's behavior

  private disambiguateSlug(baseSlug: string): string {  // line 224: executes this statement as part of this file's behavior
    let candidate = slugify(baseSlug);  // line 225: executes this statement as part of this file's behavior
    let suffix = 2;  // line 226: executes this statement as part of this file's behavior
    while ([...this.projects.values()].some((project) => project.slug === candidate)) candidate = `${slugify(baseSlug)}-${suffix++}`;  // line 227: executes this statement as part of this file's behavior
    return candidate;  // line 228: executes this statement as part of this file's behavior
  }  // line 229: executes this statement as part of this file's behavior
}  // line 230: executes this statement as part of this file's behavior

function asRecord(value: unknown): Record<string, unknown> {  // line 232: executes this statement as part of this file's behavior
  return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};  // line 233: executes this statement as part of this file's behavior
}  // line 234: executes this statement as part of this file's behavior

function stringField(object: Record<string, unknown>, field: string, trim: boolean): string | undefined {  // line 236: executes this statement as part of this file's behavior
  const value = object[field];  // line 237: executes this statement as part of this file's behavior
  if (typeof value !== "string") return undefined;  // line 238: executes this statement as part of this file's behavior
  const normalized = trim ? value.trim() : value;  // line 239: executes this statement as part of this file's behavior
  return normalized.length > 0 ? normalized : undefined;  // line 240: executes this statement as part of this file's behavior
}  // line 241: executes this statement as part of this file's behavior

function optionalNestedString(object: Record<string, unknown>, parent: string, field: string): string | undefined {  // line 243: executes this statement as part of this file's behavior
  const nested = asRecord(object[parent]);  // line 244: executes this statement as part of this file's behavior
  return stringField(nested, field, false);  // line 245: executes this statement as part of this file's behavior
}  // line 246: executes this statement as part of this file's behavior

function parseDiscord(object: Record<string, unknown>): Partial<BotProject["discord"]> | undefined {  // line 248: executes this statement as part of this file's behavior
  const discord = asRecord(object.discord);  // line 249: executes this statement as part of this file's behavior
  if (Object.keys(discord).length === 0) return undefined;  // line 250: executes this statement as part of this file's behavior
  const parsed: Partial<BotProject["discord"]> = {};  // line 251: executes this statement as part of this file's behavior
  if (hasOwn(discord, "applicationId")) parsed.applicationId = stringField(discord, "applicationId", false) ?? null;  // line 252: executes this statement as part of this file's behavior
  if (hasOwn(discord, "clientId")) parsed.clientId = stringField(discord, "clientId", false) ?? null;  // line 253: executes this statement as part of this file's behavior
  if (hasOwn(discord, "defaultGuildId")) parsed.defaultGuildId = stringField(discord, "defaultGuildId", false) ?? null;  // line 254: executes this statement as part of this file's behavior
  if (hasOwn(discord, "tokenSecretRef")) parsed.tokenSecretRef = stringField(discord, "tokenSecretRef", false) ?? null;  // line 255: executes this statement as part of this file's behavior
  if (hasOwn(discord, "commandRegistration")) parsed.commandRegistration = stringField(discord, "commandRegistration", false) as CommandRegistration | undefined;  // line 256: executes this statement as part of this file's behavior
  return withoutUndefined(parsed);  // line 257: executes this statement as part of this file's behavior
}  // line 258: executes this statement as part of this file's behavior

function parsePermissions(object: Record<string, unknown>): Partial<BotProject["permissions"]> | undefined {  // line 260: executes this statement as part of this file's behavior
  const permissions = asRecord(object.permissions);  // line 261: executes this statement as part of this file's behavior
  if (Object.keys(permissions).length === 0) return undefined;  // line 262: executes this statement as part of this file's behavior
  const parsed: Partial<BotProject["permissions"]> = {};  // line 263: executes this statement as part of this file's behavior
  if (hasOwn(permissions, "intents")) parsed.intents = Array.isArray(permissions.intents) ? permissions.intents.filter((item): item is string => typeof item === "string") : [];  // line 264: executes this statement as part of this file's behavior
  if (hasOwn(permissions, "botPermissions")) parsed.botPermissions = Array.isArray(permissions.botPermissions) ? permissions.botPermissions.filter((item): item is string => typeof item === "string") : [];  // line 265: executes this statement as part of this file's behavior
  return parsed;  // line 266: executes this statement as part of this file's behavior
}  // line 267: executes this statement as part of this file's behavior

function parseDeployment(object: Record<string, unknown>): Partial<BotProject["deployment"]> | undefined {  // line 269: executes this statement as part of this file's behavior
  const deployment = asRecord(object.deployment);  // line 270: executes this statement as part of this file's behavior
  if (Object.keys(deployment).length === 0) return undefined;  // line 271: executes this statement as part of this file's behavior
  const parsed: Partial<BotProject["deployment"]> = {};  // line 272: executes this statement as part of this file's behavior
  if (hasOwn(deployment, "targetId")) parsed.targetId = stringField(deployment, "targetId", false) ?? null;  // line 273: executes this statement as part of this file's behavior
  if (hasOwn(deployment, "lastDeploymentId")) parsed.lastDeploymentId = stringField(deployment, "lastDeploymentId", false) ?? null;  // line 274: executes this statement as part of this file's behavior
  return parsed;  // line 275: executes this statement as part of this file's behavior
}  // line 276: executes this statement as part of this file's behavior

function hasOwn(object: Record<string, unknown>, field: string): boolean {  // line 278: executes this statement as part of this file's behavior
  return Object.prototype.hasOwnProperty.call(object, field);  // line 279: executes this statement as part of this file's behavior
}  // line 280: executes this statement as part of this file's behavior

function withoutUndefined<T extends Record<string, unknown>>(object: T): T {  // line 282: executes this statement as part of this file's behavior
  return Object.fromEntries(Object.entries(object).filter(([, value]) => value !== undefined)) as T;  // line 283: executes this statement as part of this file's behavior
}  // line 284: executes this statement as part of this file's behavior

function parseGithub(object: Record<string, unknown>): Partial<NonNullable<BotProject["github"]>> | undefined {  // line 286: executes this statement as part of this file's behavior
  const github = asRecord(object.github);  // line 287: executes this statement as part of this file's behavior
  if (Object.keys(github).length === 0) return undefined;  // line 288: executes this statement as part of this file's behavior
  const parsed: Partial<NonNullable<BotProject["github"]>> = {};  // line 289: executes this statement as part of this file's behavior
  if (hasOwn(github, "owner")) parsed.owner = stringField(github, "owner", false) ?? null;  // line 290: executes this statement as part of this file's behavior
  if (hasOwn(github, "repo")) parsed.repo = stringField(github, "repo", false) ?? null;  // line 291: executes this statement as part of this file's behavior
  if (hasOwn(github, "defaultBranch")) parsed.defaultBranch = stringField(github, "defaultBranch", false);  // line 292: executes this statement as part of this file's behavior
  if (hasOwn(github, "lastPushedAt")) parsed.lastPushedAt = stringField(github, "lastPushedAt", false) ?? null;  // line 293: executes this statement as part of this file's behavior
  return withoutUndefined(parsed);  // line 294: executes this statement as part of this file's behavior
}  // line 295: executes this statement as part of this file's behavior
