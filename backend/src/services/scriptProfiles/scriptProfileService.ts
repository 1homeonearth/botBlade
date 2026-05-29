import { randomUUID, createHash } from "node:crypto";
import type { AuditService } from "../auditService.js";
import type { ScriptProfileStorePersistence } from "../persistence.js";
import { RequestValidationError } from "../projectStore.js";
import type { BotProfileScriptProfile, ScriptProfileRuntime, ScriptProfileSource } from "../../models/botProfile.js";
import { normalizeProjectRelativePath } from "../security/projectPaths.js";

export type ScriptProfileMetadata = BotProfileScriptProfile & { projectId: string };

export interface CreateScriptProfileInput {
  name: string;
  description?: string;
  source?: ScriptProfileSource;
  runtime: ScriptProfileRuntime;
  command: string[];
  workingDirectory?: string;
  envRefs?: string[];
  secretRefs?: string[];
  timeoutSeconds?: number;
  requiresConfirmation?: boolean;
  tags?: string[];
}

export type UpdateScriptProfileInput = Partial<CreateScriptProfileInput>;

export interface ScriptProfileAuditContext {
  actorId?: string;
  requestId: string;
}

const allowedSources = new Set<ScriptProfileSource>(["package_json", "file", "blade_pack", "repair_card", "user", "codex"]);
const allowedRuntimes = new Set<ScriptProfileRuntime>(["node", "python", "shell", "powershell", "docker", "workflow", "custom"]);
const maxTimeoutSeconds = 86_400;
const secretRefPattern = /^[A-Za-z0-9][A-Za-z0-9._:@-]{0,127}$/;

export class ScriptProfileService {
  private readonly profiles = new Map<string, ScriptProfileMetadata>();

  constructor(
    private readonly persistence?: ScriptProfileStorePersistence,
    private readonly auditService?: AuditService,
  ) {
    for (const profile of persistence?.loadScriptProfiles() ?? []) this.profiles.set(profile.id, profile);
  }

  list(projectId: string): ScriptProfileMetadata[] {
    return [...this.profiles.values()]
      .filter((profile) => profile.projectId === projectId)
      .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt) || a.name.localeCompare(b.name));
  }

  get(projectId: string, profileId: string): ScriptProfileMetadata | undefined {
    const profile = this.profiles.get(profileId);
    return profile?.projectId === projectId ? profile : undefined;
  }

  create(projectId: string, input: unknown, context: ScriptProfileAuditContext): ScriptProfileMetadata {
    const parsed = parseScriptProfileInput(input, false) as CreateScriptProfileInput;
    const now = new Date().toISOString();
    const profile: ScriptProfileMetadata = {
      id: `script_profile_${randomUUID()}`,
      projectId,
      name: parsed.name,
      description: parsed.description,
      source: parsed.source ?? "user",
      runtime: parsed.runtime,
      command: parsed.command,
      workingDirectory: parsed.workingDirectory ?? ".",
      envRefs: parsed.envRefs ?? [],
      secretRefs: parsed.secretRefs ?? [],
      timeoutSeconds: parsed.timeoutSeconds ?? 300,
      requiresConfirmation: parsed.requiresConfirmation ?? false,
      tags: parsed.tags ?? [],
      createdAt: now,
      updatedAt: now,
    };
    this.save(profile);
    this.audit("script.profile.create", profile, { source: profile.source, runtime: profile.runtime }, context);
    return profile;
  }

  update(projectId: string, profileId: string, input: unknown, context: ScriptProfileAuditContext): ScriptProfileMetadata | undefined {
    const existing = this.get(projectId, profileId);
    if (!existing) return undefined;
    const parsed = parseScriptProfileInput(input, true) as UpdateScriptProfileInput;
    const updated: ScriptProfileMetadata = {
      ...existing,
      ...withoutUndefined(parsed as Record<string, unknown>),
      projectId: existing.projectId,
      id: existing.id,
      createdAt: existing.createdAt,
      updatedAt: new Date().toISOString(),
    };
    this.save(updated);
    this.audit("script.profile.update", updated, { source: updated.source, runtime: updated.runtime }, context);
    return updated;
  }

  delete(projectId: string, profileId: string, context: ScriptProfileAuditContext): boolean {
    const existing = this.get(projectId, profileId);
    if (!existing) return false;
    this.profiles.delete(profileId);
    this.persistence?.deleteScriptProfile(profileId);
    this.audit("script.profile.delete", existing, { source: existing.source, runtime: existing.runtime }, context);
    return true;
  }

  upsertDetected(projectId: string, detectedProfiles: BotProfileScriptProfile[], context: ScriptProfileAuditContext): ScriptProfileMetadata[] {
    const timestamp = new Date().toISOString();
    const safeDetectedProfiles = detectedProfiles.filter((detected) =>
      normalizeProjectRelativePath(detected.workingDirectory, { allowRoot: true }).ok &&
      detected.command.length > 0 &&
      detected.command.every((token) => typeof token === "string" && token.trim() && !looksLikeSecretValue(token)),
    );
    const upserted = safeDetectedProfiles.map((detected) => {
      const normalizedWorkingDirectory = normalizeProjectRelativePath(detected.workingDirectory, { allowRoot: true }).path ?? ".";
      const id = detected.id.startsWith(`${projectId}:`) ? detected.id : `${projectId}:${detected.id}`;
      const existing = this.profiles.get(id);
      const profile: ScriptProfileMetadata = {
        ...detected,
        id,
        projectId,
        command: detected.command.map((token) => token.trim()),
        workingDirectory: normalizedWorkingDirectory,
        createdAt: existing?.createdAt ?? detected.createdAt ?? timestamp,
        updatedAt: timestamp,
      };
      this.save(profile);
      return profile;
    });
    this.auditService?.record({
      action: "script.profile.detect",
      actorId: context.actorId,
      projectId,
      resourceType: "script_profile",
      resourceId: projectId,
      metadata: {
        count: upserted.length,
        profileIds: upserted.map((profile) => profile.id),
        sources: [...new Set(upserted.map((profile) => profile.source))],
        runtimes: [...new Set(upserted.map((profile) => profile.runtime))],
      },
      requestId: context.requestId,
    });
    return upserted;
  }

  private save(profile: ScriptProfileMetadata): void {
    this.profiles.set(profile.id, profile);
    this.persistence?.saveScriptProfile(profile);
  }

  private audit(action: "script.profile.create" | "script.profile.update" | "script.profile.delete", profile: ScriptProfileMetadata, metadata: Record<string, unknown>, context: ScriptProfileAuditContext): void {
    this.auditService?.record({
      action,
      actorId: context.actorId,
      projectId: profile.projectId,
      resourceType: "script_profile",
      resourceId: profile.id,
      metadata,
      requestId: context.requestId,
    });
  }
}

function parseScriptProfileInput(input: unknown, partial: boolean): CreateScriptProfileInput | UpdateScriptProfileInput {
  const object = asRecord(input);
  const problems: { field: string; message: string }[] = [];
  const output: UpdateScriptProfileInput = {};
  if (!partial || "name" in object) {
    if (typeof object.name === "string" && object.name.trim()) output.name = object.name.trim();
    else problems.push({ field: "name", message: "Profile name is required." });
  }
  if (!partial || "runtime" in object) {
    if (typeof object.runtime === "string" && allowedRuntimes.has(object.runtime as ScriptProfileRuntime)) output.runtime = object.runtime as ScriptProfileRuntime;
    else problems.push({ field: "runtime", message: "Runtime is required and must be supported." });
  }
  if ("source" in object) {
    if (typeof object.source === "string" && allowedSources.has(object.source as ScriptProfileSource)) output.source = object.source as ScriptProfileSource;
    else problems.push({ field: "source", message: "Source must be supported." });
  }
  if ("description" in object) output.description = optionalString(object.description, "description", problems);
  if (!partial || "command" in object) {
    const command = nonEmptyStringArray(object.command, "command", problems);
    if (command && command.some((token) => looksLikeSecretValue(token))) problems.push({ field: "command", message: "command must not contain secret values; use secretRefs instead." });
    else if (command) output.command = command;
  }
  if ("workingDirectory" in object) {
    const workingDirectory = stringField(object.workingDirectory, "workingDirectory", problems);
    if (workingDirectory !== undefined) {
      const normalized = normalizeProjectRelativePath(workingDirectory, { allowRoot: true });
      if (normalized.ok) output.workingDirectory = normalized.path;
      else problems.push({ field: "workingDirectory", message: "workingDirectory must be a normalized project-relative path that does not escape the workspace." });
    }
  }
  if ("envRefs" in object) output.envRefs = stringArray(object.envRefs, "envRefs", problems);
  if ("secretRefs" in object) output.secretRefs = secretRefArray(object.secretRefs, "secretRefs", problems);
  if ("timeoutSeconds" in object) output.timeoutSeconds = boundedInteger(object.timeoutSeconds, "timeoutSeconds", 1, maxTimeoutSeconds, problems);
  if ("requiresConfirmation" in object) {
    if (typeof object.requiresConfirmation === "boolean") output.requiresConfirmation = object.requiresConfirmation;
    else problems.push({ field: "requiresConfirmation", message: "requiresConfirmation must be a boolean." });
  }
  if ("tags" in object) output.tags = stringArray(object.tags, "tags", problems);
  if (problems.length > 0) throw new RequestValidationError(problems);
  return output as CreateScriptProfileInput | UpdateScriptProfileInput;
}

function asRecord(input: unknown): Record<string, unknown> {
  return input && typeof input === "object" && !Array.isArray(input) ? input as Record<string, unknown> : {};
}

function optionalString(value: unknown, field: string, problems: { field: string; message: string }[]): string | undefined {
  if (value === undefined || value === null) return undefined;
  if (typeof value === "string") return value.trim() || undefined;
  problems.push({ field, message: `${field} must be a string.` });
  return undefined;
}

function stringField(value: unknown, field: string, problems: { field: string; message: string }[]): string | undefined {
  if (typeof value === "string" && value.trim()) return value.trim();
  problems.push({ field, message: `${field} must be a non-empty string.` });
  return undefined;
}

function stringArray(value: unknown, field: string, problems: { field: string; message: string }[]): string[] | undefined {
  if (!Array.isArray(value) || value.some((item) => typeof item !== "string")) {
    problems.push({ field, message: `${field} must be an array of strings.` });
    return undefined;
  }
  return value.map((item) => item.trim()).filter(Boolean);
}

function nonEmptyStringArray(value: unknown, field: string, problems: { field: string; message: string }[]): string[] | undefined {
  if (!Array.isArray(value) || value.length === 0 || value.some((item) => typeof item !== "string" || item.trim().length === 0)) {
    problems.push({ field, message: `${field} must be an array of non-empty strings.` });
    return undefined;
  }
  return value.map((item) => item.trim());
}

function secretRefArray(value: unknown, field: string, problems: { field: string; message: string }[]): string[] | undefined {
  const refs = nonEmptyStringArray(value, field, problems);
  if (!refs) return undefined;
  const invalidRefs = refs.filter((ref) => !secretRefPattern.test(ref) || looksLikeSecretValue(ref));
  if (invalidRefs.length > 0) {
    problems.push({ field, message: `${field} must contain secret reference IDs or names, not secret values.` });
    return undefined;
  }
  return refs;
}

function boundedInteger(value: unknown, field: string, min: number, max: number, problems: { field: string; message: string }[]): number | undefined {
  if (Number.isInteger(value) && typeof value === "number" && value >= min && value <= max) return value;
  problems.push({ field, message: `${field} must be an integer between ${min} and ${max}.` });
  return undefined;
}

function withoutUndefined(input: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(Object.entries(input).filter(([, value]) => value !== undefined));
}

function looksLikeSecretValue(value: string): boolean {
  if (value.startsWith("secret_")) return false;
  return createHash("sha256").update(value).digest("hex").length > 0 && /(?:=|^sk-|^gh[pousr]_|^xox[baprs]-|[A-Za-z0-9_.-]{32,})/i.test(value);
}
