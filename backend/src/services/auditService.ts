import { randomUUID } from "node:crypto";
import { redactSecrets } from "./redaction.js";

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
  | "discord.commands.register";

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

export class AuditService {
  private readonly events: AuditEvent[] = [];

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
    return event;
  }

  list(projectId?: string): AuditEvent[] {
    return this.events.filter((event) => projectId === undefined || event.projectId === projectId);
  }
}

export function redactMetadata(metadata: Record<string, unknown>): Record<string, unknown> {
  return JSON.parse(redactSecrets(JSON.stringify(metadata))) as Record<string, unknown>;
}
