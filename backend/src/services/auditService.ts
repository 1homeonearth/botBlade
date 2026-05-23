// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import { randomUUID } from "node:crypto";  // line 7: executes this statement as part of this file's behavior
import { redactSecrets } from "./redaction.js";  // line 8: executes this statement as part of this file's behavior
import type { AuditServicePersistence, AuditServicePort } from "./persistence.js";  // line 9: executes this statement as part of this file's behavior

export type AuditAction =  // line 11: executes this statement as part of this file's behavior
  | "project.create"  // line 12: executes this statement as part of this file's behavior
  | "project.update"  // line 13: executes this statement as part of this file's behavior
  | "project.archive"  // line 14: executes this statement as part of this file's behavior
  | "project.clone"  // line 15: executes this statement as part of this file's behavior
  | "secret.create"  // line 16: executes this statement as part of this file's behavior
  | "secret.rotate"  // line 17: executes this statement as part of this file's behavior
  | "secret.delete"  // line 18: executes this statement as part of this file's behavior
  | "generate.start"  // line 19: executes this statement as part of this file's behavior
  | "build.start"  // line 20: executes this statement as part of this file's behavior
  | "build.succeeded"  // line 21: executes this statement as part of this file's behavior
  | "build.failed"  // line 22: executes this statement as part of this file's behavior
  | "deployment.start"  // line 23: executes this statement as part of this file's behavior
  | "deployment.succeeded"  // line 24: executes this statement as part of this file's behavior
  | "deployment.failed"  // line 25: executes this statement as part of this file's behavior
  | "runtime.start"  // line 26: executes this statement as part of this file's behavior
  | "runtime.stop"  // line 27: executes this statement as part of this file's behavior
  | "runtime.restart"  // line 28: executes this statement as part of this file's behavior
  | "github.push"  // line 29: executes this statement as part of this file's behavior
  | "discord.commands.register";  // line 30: executes this statement as part of this file's behavior

export interface AuditEvent {  // line 32: executes this statement as part of this file's behavior
  id: string;  // line 33: executes this statement as part of this file's behavior
  actorId: string;  // line 34: executes this statement as part of this file's behavior
  projectId: string | null;  // line 35: executes this statement as part of this file's behavior
  action: AuditAction;  // line 36: executes this statement as part of this file's behavior
  resourceType: string;  // line 37: executes this statement as part of this file's behavior
  resourceId: string;  // line 38: executes this statement as part of this file's behavior
  metadata: Record<string, unknown>;  // line 39: executes this statement as part of this file's behavior
  createdAt: string;  // line 40: executes this statement as part of this file's behavior
  requestId: string;  // line 41: executes this statement as part of this file's behavior
}  // line 42: executes this statement as part of this file's behavior

export interface AuditInput {  // line 44: executes this statement as part of this file's behavior
  actorId?: string;  // line 45: executes this statement as part of this file's behavior
  projectId?: string | null;  // line 46: executes this statement as part of this file's behavior
  action: AuditAction;  // line 47: executes this statement as part of this file's behavior
  resourceType: string;  // line 48: executes this statement as part of this file's behavior
  resourceId: string;  // line 49: executes this statement as part of this file's behavior
  metadata?: Record<string, unknown>;  // line 50: executes this statement as part of this file's behavior
  requestId: string;  // line 51: executes this statement as part of this file's behavior
}  // line 52: executes this statement as part of this file's behavior

export class AuditService implements AuditServicePort {  // line 54: executes this statement as part of this file's behavior
  private readonly events: AuditEvent[] = [];  // line 55: executes this statement as part of this file's behavior

  constructor(private readonly persistence?: AuditServicePersistence) {  // line 57: executes this statement as part of this file's behavior
    this.events.push(...(persistence?.loadAuditEvents() ?? []));  // line 58: executes this statement as part of this file's behavior
  }  // line 59: executes this statement as part of this file's behavior

  record(input: AuditInput): AuditEvent {  // line 61: executes this statement as part of this file's behavior
    const event: AuditEvent = {  // line 62: executes this statement as part of this file's behavior
      id: `audit_${randomUUID()}`,  // line 63: executes this statement as part of this file's behavior
      actorId: input.actorId ?? "local_user",  // line 64: executes this statement as part of this file's behavior
      projectId: input.projectId ?? null,  // line 65: executes this statement as part of this file's behavior
      action: input.action,  // line 66: executes this statement as part of this file's behavior
      resourceType: input.resourceType,  // line 67: executes this statement as part of this file's behavior
      resourceId: input.resourceId,  // line 68: executes this statement as part of this file's behavior
      metadata: redactMetadata(input.metadata ?? {}),  // line 69: executes this statement as part of this file's behavior
      createdAt: new Date().toISOString(),  // line 70: executes this statement as part of this file's behavior
      requestId: input.requestId,  // line 71: executes this statement as part of this file's behavior
    };  // line 72: executes this statement as part of this file's behavior
    this.events.unshift(event);  // line 73: executes this statement as part of this file's behavior
    this.persistence?.saveAuditEvent(event);  // line 74: executes this statement as part of this file's behavior
    return event;  // line 75: executes this statement as part of this file's behavior
  }  // line 76: executes this statement as part of this file's behavior

  list(projectId?: string): AuditEvent[] {  // line 78: executes this statement as part of this file's behavior
    return this.events.filter((event) => projectId === undefined || event.projectId === projectId);  // line 79: executes this statement as part of this file's behavior
  }  // line 80: executes this statement as part of this file's behavior
}  // line 81: executes this statement as part of this file's behavior

export function redactMetadata(metadata: Record<string, unknown>): Record<string, unknown> {  // line 83: executes this statement as part of this file's behavior
  return JSON.parse(redactSecrets(JSON.stringify(metadata))) as Record<string, unknown>;  // line 84: executes this statement as part of this file's behavior
}  // line 85: executes this statement as part of this file's behavior
