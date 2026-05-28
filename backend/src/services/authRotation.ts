import crypto from "node:crypto";
import type { AuthenticatedActor } from "./authService.js";

export interface AuthRotationInput {
  roles?: unknown;
  projectIds?: unknown;
  authMethod?: unknown;
  expiresAt?: unknown;
}

export interface AuthRotationPlan {
  currentTokenId: string;
  nextTokenId: string;
  actorId: string;
  roles: string[];
  projectIds: string[];
  authMethod: "bearer" | "session";
  createdAt: string;
  expiresAt?: string;
  value: string;
  preview: string;
  environmentVariable: "BOTBLADE_AUTH_TOKENS" | "BOTBLADE_SESSION_TOKENS";
  nextEntry: Record<string, unknown>;
  revokeCurrentEntry: Record<string, unknown>;
}

export function createAuthRotationPlan(actor: AuthenticatedActor, input: AuthRotationInput = {}): AuthRotationPlan {
  const authMethod = input.authMethod === "session" ? "session" : "bearer";
  const value = crypto.randomBytes(32).toString("base64url");
  const createdAt = new Date().toISOString();
  const expiresAt = typeof input.expiresAt === "string" && input.expiresAt.trim() ? input.expiresAt.trim() : undefined;
  const nextTokenId = `${authMethod}_${crypto.randomUUID()}`;
  const roles = stringList(input.roles, actor.roles.length > 0 ? actor.roles : ["admin"]);
  const projectIds = stringList(input.projectIds, actor.projectIds.length > 0 ? actor.projectIds : ["*"]);
  const nextEntry = {
    token: value,
    tokenId: nextTokenId,
    actorId: actor.id,
    roles,
    projectIds,
    authMethod,
    createdAt,
    ...(expiresAt ? { expiresAt } : {}),
  };
  return {
    currentTokenId: actor.tokenId,
    nextTokenId,
    actorId: actor.id,
    roles,
    projectIds,
    authMethod,
    createdAt,
    expiresAt,
    value,
    preview: previewCredential(value),
    environmentVariable: authMethod === "session" ? "BOTBLADE_SESSION_TOKENS" : "BOTBLADE_AUTH_TOKENS",
    nextEntry,
    revokeCurrentEntry: { tokenId: actor.tokenId, revokedAt: createdAt },
  };
}

function stringList(value: unknown, fallback: string[]): string[] {
  if (!Array.isArray(value)) return fallback;
  const normalized = value.filter((entry): entry is string => typeof entry === "string" && entry.trim().length > 0).map((entry) => entry.trim());
  return normalized.length > 0 ? normalized : fallback;
}

function previewCredential(value: string): string {
  return `${value.slice(0, 6)}…${value.slice(-4)}`;
}
