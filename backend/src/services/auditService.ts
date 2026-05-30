import { randomUUID } from "node:crypto";
import { redactSecrets } from "./redaction.js";
import type { AuditServicePersistence, AuditServicePort } from "./persistence.js";

export type AuditAction =
  | "project.create"
  | "project.update"
  | "project.archive"
  | "project.clone"
  | "secret.create"
  | "secret.rotate"
  | "secret.delete"
  | "generate.start"
  | "build.start"
  | "build.succeeded"
  | "build.failed"
  | "deployment.start"
  | "deployment.succeeded"
  | "deployment.failed"
  | "runtime.start"
  | "runtime.stop"
  | "runtime.restart"
  | "github.push"
  | "discord.commands.register"
  | "file.create"
  | "file.rename"
  | "file.delete"
  | "import.start"
  | "import.state_transition"
  | "import.failure"
  | "script.profile.detect"
  | "script.profile.create"
  | "script.profile.update"
  | "script.profile.delete";

export interface AuditEvent {
  id: string;
  actorId: string;
  projectId: string | null;
  action: AuditAction;
  resourceType: string;
  resourceId: string;
  metadata: Record<string, unknown>;
  createdAt: string;
  requestId: string;
}

export interface AuditInput {
  actorId?: string;
  projectId?: string | null;
  action: AuditAction;
  resourceType: string;
  resourceId: string;
  metadata?: Record<string, unknown>;
  requestId: string;
}

export interface AuditListOptions {
  limit?: number;
}

export interface AuditRetentionOptions {
  maxEvents?: number;
  retentionDays?: number;
}

const DEFAULT_MAX_EVENTS = 5_000;
const DAY_MS = 24 * 60 * 60 * 1000;

export class AuditService implements AuditServicePort {
  private readonly events: AuditEvent[] = [];
  private readonly maxEvents: number;
  private readonly retentionDays?: number;

  constructor(private readonly persistence?: AuditServicePersistence, options: AuditRetentionOptions = {}) {
    this.maxEvents = positiveInteger(options.maxEvents) ?? DEFAULT_MAX_EVENTS;
    this.retentionDays = positiveInteger(options.retentionDays);
    this.events.push(...(persistence?.loadAuditEvents() ?? []));
    this.enforceRetention();
  }

  record(input: AuditInput): AuditEvent {
    const event: AuditEvent = {
      id: `audit_${randomUUID()}`,
      actorId: input.actorId ?? "local_user",
      projectId: input.projectId ?? null,
      action: input.action,
      resourceType: input.resourceType,
      resourceId: input.resourceId,
      metadata: redactMetadata(input.metadata ?? {}),
      createdAt: new Date().toISOString(),
      requestId: input.requestId,
    };
    this.events.unshift(event);
    this.enforceRetention();
    this.persistence?.saveAuditEvent(event);
    return event;
  }

  list(projectId?: string, options: AuditListOptions = {}): AuditEvent[] {
    const limit = positiveInteger(options.limit);
    const events = this.events.filter((event) => projectId === undefined || event.projectId === projectId);
    return limit === undefined ? events : events.slice(0, limit);
  }

  private enforceRetention(): void {
    const beforeIds = new Set(this.events.map((event) => event.id));
    const cutoff = this.retentionDays === undefined ? undefined : Date.now() - this.retentionDays * DAY_MS;
    if (cutoff !== undefined) {
      for (let index = this.events.length - 1; index >= 0; index -= 1) {
        const createdAt = Date.parse(this.events[index].createdAt);
        if (!Number.isFinite(createdAt) || createdAt < cutoff) this.events.splice(index, 1);
      }
    }
    if (this.events.length > this.maxEvents) this.events.splice(this.maxEvents);
    const afterIds = new Set(this.events.map((event) => event.id));
    const pruned = [...beforeIds].some((id) => !afterIds.has(id));
    if (pruned) this.persistence?.pruneAuditEvents?.(this.events.map((event) => event.id));
  }
}

export function redactMetadata(metadata: Record<string, unknown>): Record<string, unknown> {
  return JSON.parse(redactSecrets(JSON.stringify(dropCommandArrays(metadata)))) as Record<string, unknown>;
}

function dropCommandArrays(value: unknown): unknown {
  if (Array.isArray(value)) return value.map((item) => dropCommandArrays(item));
  if (!value || typeof value !== "object") return value;
  const output: Record<string, unknown> = {};
  for (const [key, entry] of Object.entries(value)) {
    if ((key === "command" || key === "commands") && Array.isArray(entry)) continue;
    output[key] = dropCommandArrays(entry);
  }
  return output;
}

function positiveInteger(value: number | undefined): number | undefined {
  return Number.isInteger(value) && value !== undefined && value > 0 ? value : undefined;
}
