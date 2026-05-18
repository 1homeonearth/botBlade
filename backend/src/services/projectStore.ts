import { randomUUID } from "node:crypto";
import type { BotCommand, BotEvent, BotProject, CommandRegistration } from "../models/project.js";
import { parseCommandDefinition, validateCommands } from "./commandDefinitions.js";
import type { ProjectStorePersistence, ProjectStorePort } from "./persistence.js";

export const DEFAULT_TEMPLATE_ID = "template_blank_discord_ts";

export interface CreateProjectInput {
  name: string;
  slug?: string;
  description?: string;
  templateId?: string;
  language?: "typescript";
  runtime?: "node22";
  discord?: Partial<BotProject["discord"]>;
  permissions?: Partial<BotProject["permissions"]>;
  commands?: BotCommand[];
  events?: BotEvent[];
  deployment?: Partial<BotProject["deployment"]>;
  github?: Partial<BotProject["github"]>;
}

export interface UpdateProjectInput {
  name?: string;
  slug?: string;
  description?: string;
  templateId?: string;
  discord?: Partial<BotProject["discord"]>;
  permissions?: Partial<BotProject["permissions"]>;
  commands?: BotCommand[];
  events?: BotEvent[];
  deployment?: Partial<BotProject["deployment"]>;
  github?: Partial<BotProject["github"]>;
}

export interface ValidationProblem {
  field: string;
  message: string;
}

export class RequestValidationError extends Error {
  constructor(public readonly problems: ValidationProblem[]) {
    super("Request validation failed.");
    this.name = "RequestValidationError";
  }
}

export function parseCreateProjectInput(value: unknown): CreateProjectInput {
  const object = asRecord(value);
  const name = stringField(object, "name", true);
  const commandRegistration = optionalNestedString(object, "discord", "commandRegistration") ?? "guild";
  const language = stringField(object, "language", false) ?? "typescript";
  const runtime = stringField(object, "runtime", false) ?? "node22";
  const problems: ValidationProblem[] = [];
  if (!name) problems.push({ field: "name", message: "Project name is required." });
  if (!["guild", "global"].includes(commandRegistration)) problems.push({ field: "discord.commandRegistration", message: "Command registration must be guild or global." });
  if (language !== "typescript") problems.push({ field: "language", message: "Language must be typescript." });
  if (runtime !== "node22") problems.push({ field: "runtime", message: "Runtime must be node22." });
  if (problems.length > 0) throw new RequestValidationError(problems);
  const commands = Array.isArray(object.commands) ? object.commands.map((command) => parseCommandDefinition(command)) : [];
  validateCommands(commands);
  return {
    name: name!,
    slug: stringField(object, "slug", false),
    description: stringField(object, "description", false) ?? "",
    templateId: stringField(object, "templateId", false) ?? DEFAULT_TEMPLATE_ID,
    language: "typescript",
    runtime: "node22",
    discord: parseDiscord(object),
    permissions: parsePermissions(object),
    commands,
    events: Array.isArray(object.events) ? object.events as BotEvent[] : [],
    deployment: parseDeployment(object),
    github: parseGithub(object),
  };
}

export function parseUpdateProjectInput(value: unknown): UpdateProjectInput {
  const object = asRecord(value);
  const allowed = new Set(["name", "slug", "description", "templateId", "discord", "permissions", "commands", "events", "deployment", "github"]);
  const problems = Object.keys(object).filter((key) => !allowed.has(key)).map((key) => ({ field: key, message: "Field is not patchable." }));
  const commandRegistration = optionalNestedString(object, "discord", "commandRegistration");
  if (commandRegistration && !["guild", "global"].includes(commandRegistration)) problems.push({ field: "discord.commandRegistration", message: "Command registration must be guild or global." });
  const commands = Array.isArray(object.commands) ? object.commands.map((command) => parseCommandDefinition(command)) : undefined;
  if (commands) validateCommands(commands);
  if (problems.length > 0) throw new RequestValidationError(problems);
  return {
    name: stringField(object, "name", false),
    slug: stringField(object, "slug", false),
    description: stringField(object, "description", false),
    templateId: stringField(object, "templateId", false),
    discord: parseDiscord(object),
    permissions: parsePermissions(object),
    commands,
    events: Array.isArray(object.events) ? object.events as BotEvent[] : undefined,
    deployment: parseDeployment(object),
    github: parseGithub(object),
  };
}

export function parseToggleAction(value: unknown): "start" | "stop" {
  const object = asRecord(value);
  if (object.action === "start" || object.action === "stop") return object.action;
  throw new RequestValidationError([{ field: "action", message: "Action must be start or stop." }]);
}

export function slugify(value: string): string {
  return value.trim().toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "") || "project";
}

export class DuplicateSlugError extends Error {
  constructor(slug: string) {
    super(`Project slug '${slug}' already exists.`);
    this.name = "DuplicateSlugError";
  }
}

export class ProjectStore implements ProjectStorePort {
  private readonly projects = new Map<string, BotProject>();

  constructor(private readonly persistence?: ProjectStorePersistence) {
    for (const project of persistence?.loadProjects() ?? []) this.projects.set(project.id, project);
  }

  list(): BotProject[] {
    return [...this.projects.values()].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
  }

  get(projectId: string): BotProject | undefined {
    return this.projects.get(projectId);
  }

  create(input: CreateProjectInput): BotProject {
    const now = new Date().toISOString();
    const slug = slugify(input.slug ?? input.name);
    this.assertSlugAvailable(slug);
    const project: BotProject = {
      id: `project_${randomUUID()}`,
      name: input.name,
      slug,
      description: input.description ?? "",
      templateId: input.templateId ?? DEFAULT_TEMPLATE_ID,
      language: "typescript",
      runtime: "node22",
      discord: {
        applicationId: input.discord?.applicationId ?? null,
        clientId: input.discord?.clientId ?? null,
        defaultGuildId: input.discord?.defaultGuildId ?? null,
        tokenSecretRef: input.discord?.tokenSecretRef ?? null,
        commandRegistration: input.discord?.commandRegistration ?? "guild",
      },
      permissions: { intents: input.permissions?.intents ?? [], botPermissions: input.permissions?.botPermissions ?? [] },
      commands: input.commands ?? [],
      events: input.events ?? [],
      deployment: { targetId: input.deployment?.targetId ?? null, lastDeploymentId: input.deployment?.lastDeploymentId ?? null },
      github: { owner: input.github?.owner ?? null, repo: input.github?.repo ?? null, defaultBranch: input.github?.defaultBranch ?? "main", lastPushedAt: input.github?.lastPushedAt ?? null },
      archivedAt: null,
      createdAt: now,
      updatedAt: now,
    };
    this.projects.set(project.id, project);
    this.persistence?.saveProject(project);
    return project;
  }

  update(projectId: string, input: UpdateProjectInput): BotProject | undefined {
    const existing = this.projects.get(projectId);
    if (!existing) return undefined;
    const nextSlug = input.slug === undefined ? existing.slug : slugify(input.slug);
    this.assertSlugAvailable(nextSlug, projectId);
    const updated: BotProject = {
      ...existing,
      ...withoutUndefined(input as Record<string, unknown>),
      slug: nextSlug,
      language: existing.language,
      runtime: existing.runtime,
      discord: { ...existing.discord, ...withoutUndefined(input.discord ?? {}) },
      permissions: { ...existing.permissions, ...withoutUndefined(input.permissions ?? {}) },
      deployment: { ...existing.deployment, ...withoutUndefined(input.deployment ?? {}) },
      github: { owner: null, repo: null, defaultBranch: "main", lastPushedAt: null, ...(existing.github ?? {}), ...withoutUndefined(input.github ?? {}) },
      updatedAt: new Date().toISOString(),
    };
    this.projects.set(projectId, updated);
    this.persistence?.saveProject(updated);
    return updated;
  }

  delete(projectId: string): boolean {
    const deleted = this.projects.delete(projectId);
    if (deleted) this.persistence?.deleteProject(projectId);
    return deleted;
  }

  archive(projectId: string): BotProject | undefined {
    const existing = this.projects.get(projectId);
    if (!existing) return undefined;
    const now = new Date().toISOString();
    const archived = { ...existing, archivedAt: existing.archivedAt ?? now, updatedAt: now };
    this.projects.set(projectId, archived);
    this.persistence?.saveProject(archived);
    return archived;
  }

  clone(projectId: string): BotProject | undefined {
    const existing = this.projects.get(projectId);
    if (!existing) return undefined;
    const now = new Date().toISOString();
    const clone = { ...existing, id: `project_${randomUUID()}`, name: `${existing.name} Copy`, slug: this.disambiguateSlug(`${existing.slug}-copy`), archivedAt: null, createdAt: now, updatedAt: now };
    this.projects.set(clone.id, clone);
    this.persistence?.saveProject(clone);
    return clone;
  }

  private assertSlugAvailable(slug: string, currentProjectId?: string): void {
    if ([...this.projects.values()].some((project) => project.slug === slug && project.id !== currentProjectId)) throw new DuplicateSlugError(slug);
  }

  private disambiguateSlug(baseSlug: string): string {
    let candidate = slugify(baseSlug);
    let suffix = 2;
    while ([...this.projects.values()].some((project) => project.slug === candidate)) candidate = `${slugify(baseSlug)}-${suffix++}`;
    return candidate;
  }
}

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
}

function stringField(object: Record<string, unknown>, field: string, trim: boolean): string | undefined {
  const value = object[field];
  if (typeof value !== "string") return undefined;
  const normalized = trim ? value.trim() : value;
  return normalized.length > 0 ? normalized : undefined;
}

function optionalNestedString(object: Record<string, unknown>, parent: string, field: string): string | undefined {
  const nested = asRecord(object[parent]);
  return stringField(nested, field, false);
}

function parseDiscord(object: Record<string, unknown>): Partial<BotProject["discord"]> | undefined {
  const discord = asRecord(object.discord);
  if (Object.keys(discord).length === 0) return undefined;
  const parsed: Partial<BotProject["discord"]> = {};
  if (hasOwn(discord, "applicationId")) parsed.applicationId = stringField(discord, "applicationId", false) ?? null;
  if (hasOwn(discord, "clientId")) parsed.clientId = stringField(discord, "clientId", false) ?? null;
  if (hasOwn(discord, "defaultGuildId")) parsed.defaultGuildId = stringField(discord, "defaultGuildId", false) ?? null;
  if (hasOwn(discord, "tokenSecretRef")) parsed.tokenSecretRef = stringField(discord, "tokenSecretRef", false) ?? null;
  if (hasOwn(discord, "commandRegistration")) parsed.commandRegistration = stringField(discord, "commandRegistration", false) as CommandRegistration | undefined;
  return withoutUndefined(parsed);
}

function parsePermissions(object: Record<string, unknown>): Partial<BotProject["permissions"]> | undefined {
  const permissions = asRecord(object.permissions);
  if (Object.keys(permissions).length === 0) return undefined;
  const parsed: Partial<BotProject["permissions"]> = {};
  if (hasOwn(permissions, "intents")) parsed.intents = Array.isArray(permissions.intents) ? permissions.intents.filter((item): item is string => typeof item === "string") : [];
  if (hasOwn(permissions, "botPermissions")) parsed.botPermissions = Array.isArray(permissions.botPermissions) ? permissions.botPermissions.filter((item): item is string => typeof item === "string") : [];
  return parsed;
}

function parseDeployment(object: Record<string, unknown>): Partial<BotProject["deployment"]> | undefined {
  const deployment = asRecord(object.deployment);
  if (Object.keys(deployment).length === 0) return undefined;
  const parsed: Partial<BotProject["deployment"]> = {};
  if (hasOwn(deployment, "targetId")) parsed.targetId = stringField(deployment, "targetId", false) ?? null;
  if (hasOwn(deployment, "lastDeploymentId")) parsed.lastDeploymentId = stringField(deployment, "lastDeploymentId", false) ?? null;
  return parsed;
}

function hasOwn(object: Record<string, unknown>, field: string): boolean {
  return Object.prototype.hasOwnProperty.call(object, field);
}

function withoutUndefined<T extends Record<string, unknown>>(object: T): T {
  return Object.fromEntries(Object.entries(object).filter(([, value]) => value !== undefined)) as T;
}

function parseGithub(object: Record<string, unknown>): Partial<NonNullable<BotProject["github"]>> | undefined {
  const github = asRecord(object.github);
  if (Object.keys(github).length === 0) return undefined;
  const parsed: Partial<NonNullable<BotProject["github"]>> = {};
  if (hasOwn(github, "owner")) parsed.owner = stringField(github, "owner", false) ?? null;
  if (hasOwn(github, "repo")) parsed.repo = stringField(github, "repo", false) ?? null;
  if (hasOwn(github, "defaultBranch")) parsed.defaultBranch = stringField(github, "defaultBranch", false);
  if (hasOwn(github, "lastPushedAt")) parsed.lastPushedAt = stringField(github, "lastPushedAt", false) ?? null;
  return withoutUndefined(parsed);
}
