// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import type { IncomingMessage } from "node:http";  // line 7: executes this statement as part of this file's behavior

export interface AuthenticatedActor {  // line 9: executes this statement as part of this file's behavior
  id: string;  // line 10: executes this statement as part of this file's behavior
  tokenId: string;  // line 11: executes this statement as part of this file's behavior
  roles: string[];  // line 12: executes this statement as part of this file's behavior
  projectIds: string[];  // line 13: executes this statement as part of this file's behavior
  authMethod: "bearer" | "session";  // line 14: executes this statement as part of this file's behavior
}  // line 15: executes this statement as part of this file's behavior

interface AuthCredential {  // line 17: executes this statement as part of this file's behavior
  token: string;  // line 18: executes this statement as part of this file's behavior
  actorId: string;  // line 19: executes this statement as part of this file's behavior
  tokenId: string;  // line 20: executes this statement as part of this file's behavior
  roles: string[];  // line 21: executes this statement as part of this file's behavior
  projectIds: string[];  // line 22: executes this statement as part of this file's behavior
  authMethod?: "bearer" | "session";  // line 23: executes this statement as part of this file's behavior
}  // line 24: executes this statement as part of this file's behavior

const GLOBAL_PROJECT = "*";  // line 26: executes this statement as part of this file's behavior
const MIN_TOKEN_LENGTH = 8;  // line 27: executes this statement as part of this file's behavior

export function authenticateRequest(req: IncomingMessage): AuthenticatedActor {  // line 29: executes this statement as part of this file's behavior
  const bearerToken = readBearerToken(req);  // line 30: executes this statement as part of this file's behavior
  const sessionToken = readSessionToken(req);  // line 31: executes this statement as part of this file's behavior
  const credentials = configuredCredentials();  // line 32: executes this statement as part of this file's behavior
  const match = credentials.find((credential) => safeTokenEquals(credential.token, bearerToken) || safeTokenEquals(credential.token, sessionToken));  // line 33: executes this statement as part of this file's behavior
  if (!match) {  // line 34: executes this statement as part of this file's behavior
    throw { statusCode: 401, code: "AUTHENTICATION_REQUIRED", message: "A valid bearer token or session credential is required.", details: { schemes: ["Bearer", "botBladeSession"] } };  // line 35: executes this statement as part of this file's behavior
  }  // line 36: executes this statement as part of this file's behavior
  return {  // line 37: executes this statement as part of this file's behavior
    id: match.actorId,  // line 38: executes this statement as part of this file's behavior
    tokenId: match.tokenId,  // line 39: executes this statement as part of this file's behavior
    roles: match.roles,  // line 40: executes this statement as part of this file's behavior
    projectIds: match.projectIds,  // line 41: executes this statement as part of this file's behavior
    authMethod: match.authMethod ?? (match.token === bearerToken ? "bearer" : "session"),  // line 42: executes this statement as part of this file's behavior
  };  // line 43: executes this statement as part of this file's behavior
}  // line 44: executes this statement as part of this file's behavior

export function canAccessProject(actor: AuthenticatedActor, projectId: string | null | undefined): boolean {  // line 46: executes this statement as part of this file's behavior
  if (hasGlobalProjectAccess(actor)) return true;  // line 47: executes this statement as part of this file's behavior
  return typeof projectId === "string" && actor.projectIds.includes(projectId);  // line 48: executes this statement as part of this file's behavior
}  // line 49: executes this statement as part of this file's behavior

export function hasGlobalProjectAccess(actor: AuthenticatedActor): boolean {  // line 51: executes this statement as part of this file's behavior
  return actor.roles.includes("admin") || actor.projectIds.includes(GLOBAL_PROJECT);  // line 52: executes this statement as part of this file's behavior
}  // line 53: executes this statement as part of this file's behavior

export function assertProjectAccess(actor: AuthenticatedActor, projectId: string | null | undefined): void {  // line 55: executes this statement as part of this file's behavior
  if (canAccessProject(actor, projectId)) return;  // line 56: executes this statement as part of this file's behavior
  throw { statusCode: 403, code: "PROJECT_ACCESS_DENIED", message: "The authenticated actor is not authorized for this project.", details: { projectId: projectId ?? null } };  // line 57: executes this statement as part of this file's behavior
}  // line 58: executes this statement as part of this file's behavior

export function assertGlobalAccess(actor: AuthenticatedActor, resource: string): void {  // line 60: executes this statement as part of this file's behavior
  if (hasGlobalProjectAccess(actor)) return;  // line 61: executes this statement as part of this file's behavior
  throw { statusCode: 403, code: "GLOBAL_ACCESS_DENIED", message: `The authenticated actor is not authorized to access ${resource}.`, details: { resource } };  // line 62: executes this statement as part of this file's behavior
}  // line 63: executes this statement as part of this file's behavior

export function assertExecutionAccess(actor: AuthenticatedActor, resource: string): void {  // line 65: executes this statement as part of this file's behavior
  if (actor.roles.includes("admin") || actor.roles.includes("execute")) return;  // line 66: executes this statement as part of this file's behavior
  throw { statusCode: 403, code: "EXECUTION_ACCESS_DENIED", message: `The authenticated actor is not authorized to execute ${resource}.`, details: { resource } };  // line 67: executes this statement as part of this file's behavior
}  // line 68: executes this statement as part of this file's behavior

function readBearerToken(req: IncomingMessage): string | undefined {  // line 70: executes this statement as part of this file's behavior
  const header = req.headers.authorization;  // line 71: executes this statement as part of this file's behavior
  if (typeof header !== "string") return undefined;  // line 72: executes this statement as part of this file's behavior
  const match = header.match(/^Bearer\s+(.+)$/i);  // line 73: executes this statement as part of this file's behavior
  return match?.[1]?.trim();  // line 74: executes this statement as part of this file's behavior
}  // line 75: executes this statement as part of this file's behavior

function readSessionToken(req: IncomingMessage): string | undefined {  // line 77: executes this statement as part of this file's behavior
  const explicitHeader = req.headers["x-session-token"];  // line 78: executes this statement as part of this file's behavior
  if (typeof explicitHeader === "string" && explicitHeader.trim()) return explicitHeader.trim();  // line 79: executes this statement as part of this file's behavior
  const cookieHeader = req.headers.cookie;  // line 80: executes this statement as part of this file's behavior
  if (typeof cookieHeader !== "string") return undefined;  // line 81: executes this statement as part of this file's behavior
  for (const cookie of cookieHeader.split(";")) {  // line 82: executes this statement as part of this file's behavior
    const [name, ...valueParts] = cookie.trim().split("=");  // line 83: executes this statement as part of this file's behavior
    if (name === "botBladeSession") return decodeURIComponent(valueParts.join("=")).trim();  // line 84: executes this statement as part of this file's behavior
  }  // line 85: executes this statement as part of this file's behavior
  return undefined;  // line 86: executes this statement as part of this file's behavior
}  // line 87: executes this statement as part of this file's behavior

function configuredCredentials(): AuthCredential[] {  // line 89: executes this statement as part of this file's behavior
  const credentials = [...parseJsonCredentials(process.env.BOTBLADE_AUTH_TOKENS, "bearer"), ...parseJsonCredentials(process.env.BOTBLADE_SESSION_TOKENS, "session")];  // line 90: executes this statement as part of this file's behavior
  ensureUniqueTokenIds(credentials);  // line 91: executes this statement as part of this file's behavior
  if (credentials.length > 0) return credentials;  // line 92: executes this statement as part of this file's behavior
  if (process.env.BOTBLADE_LOCAL_DEV !== "true") return [];  // line 93: executes this statement as part of this file's behavior
  const legacyTokens = splitTokens(process.env.BOTBLADE_API_TOKENS ?? process.env.BOTBLADE_API_TOKEN);  // line 94: executes this statement as part of this file's behavior
  return legacyTokens.map((token, index) => ({ token, actorId: index === 0 ? "local_admin" : `local_user_${index + 1}`, tokenId: `env_token_${index + 1}`, roles: ["admin"], projectIds: [GLOBAL_PROJECT], authMethod: "bearer" }));  // line 95: executes this statement as part of this file's behavior
}  // line 96: executes this statement as part of this file's behavior

function parseJsonCredentials(value: string | undefined, authMethod: "bearer" | "session"): AuthCredential[] {  // line 98: executes this statement as part of this file's behavior
  if (!value?.trim()) return [];  // line 99: executes this statement as part of this file's behavior
  try {  // line 100: executes this statement as part of this file's behavior
    const parsed = JSON.parse(value) as unknown;  // line 101: executes this statement as part of this file's behavior
    if (Array.isArray(parsed)) return parsed.flatMap((entry, index) => normalizeCredential(entry, index, authMethod));  // line 102: executes this statement as part of this file's behavior
    return normalizeCredential(parsed, 0, authMethod);  // line 103: executes this statement as part of this file's behavior
  } catch {  // line 104: executes this statement as part of this file's behavior
    return splitTokens(value).map((token, index) => ({ token, actorId: `${authMethod}_user_${index + 1}`, tokenId: `${authMethod}_token_${index + 1}`, roles: ["admin"], projectIds: [GLOBAL_PROJECT], authMethod }));  // line 105: executes this statement as part of this file's behavior
  }  // line 106: executes this statement as part of this file's behavior
}  // line 107: executes this statement as part of this file's behavior

function normalizeCredential(value: unknown, index: number, authMethod: "bearer" | "session"): AuthCredential[] {  // line 109: executes this statement as part of this file's behavior
  if (typeof value === "string" && value.trim().length >= MIN_TOKEN_LENGTH) return [{ token: value.trim(), actorId: `${authMethod}_user_${index + 1}`, tokenId: `${authMethod}_token_${index + 1}`, roles: ["admin"], projectIds: [GLOBAL_PROJECT], authMethod }];  // line 110: executes this statement as part of this file's behavior
  if (!value || typeof value !== "object" || Array.isArray(value)) return [];  // line 111: executes this statement as part of this file's behavior
  const record = value as Record<string, unknown>;  // line 112: executes this statement as part of this file's behavior
  const token = typeof record.token === "string" ? record.token.trim() : "";  // line 113: executes this statement as part of this file's behavior
  if (!token || token.length < MIN_TOKEN_LENGTH) return [];  // line 114: executes this statement as part of this file's behavior
  return [{  // line 115: executes this statement as part of this file's behavior
    token,  // line 116: executes this statement as part of this file's behavior
    actorId: typeof record.actorId === "string" && record.actorId.trim() ? record.actorId.trim() : typeof record.userId === "string" && record.userId.trim() ? record.userId.trim() : `${authMethod}_user_${index + 1}`,  // line 117: executes this statement as part of this file's behavior
    tokenId: typeof record.tokenId === "string" && record.tokenId.trim() ? record.tokenId.trim() : `${authMethod}_token_${index + 1}`,  // line 118: executes this statement as part of this file's behavior
    roles: stringArray(record.roles),  // line 119: executes this statement as part of this file's behavior
    projectIds: stringArray(record.projectIds).length > 0 ? stringArray(record.projectIds) : [GLOBAL_PROJECT],  // line 120: executes this statement as part of this file's behavior
    authMethod,  // line 121: executes this statement as part of this file's behavior
  }];  // line 122: executes this statement as part of this file's behavior
}  // line 123: executes this statement as part of this file's behavior

function stringArray(value: unknown): string[] {  // line 125: executes this statement as part of this file's behavior
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string" && item.trim().length > 0).map((item) => item.trim()) : [];  // line 126: executes this statement as part of this file's behavior
}  // line 127: executes this statement as part of this file's behavior

function splitTokens(value: string | undefined): string[] {  // line 129: executes this statement as part of this file's behavior
  return value?.split(",").map((token) => token.trim()).filter((token) => token.length >= MIN_TOKEN_LENGTH) ?? [];  // line 130: executes this statement as part of this file's behavior
}  // line 131: executes this statement as part of this file's behavior

function safeTokenEquals(expected: string, provided?: string): boolean {  // line 133: executes this statement as part of this file's behavior
  if (!provided) return false;  // line 134: executes this statement as part of this file's behavior
  const a = expected;  // line 135: executes this statement as part of this file's behavior
  const b = provided;  // line 136: executes this statement as part of this file's behavior
  if (a.length !== b.length) return false;  // line 137: executes this statement as part of this file's behavior
  let diff = 0;  // line 138: executes this statement as part of this file's behavior
  for (let i = 0; i < a.length; i += 1) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);  // line 139: executes this statement as part of this file's behavior
  return diff === 0;  // line 140: executes this statement as part of this file's behavior
}  // line 141: executes this statement as part of this file's behavior

function ensureUniqueTokenIds(credentials: AuthCredential[]): void {  // line 143: executes this statement as part of this file's behavior
  const ids = new Set<string>();  // line 144: executes this statement as part of this file's behavior
  for (const credential of credentials) {  // line 145: executes this statement as part of this file's behavior
    if (ids.has(credential.tokenId)) throw new Error(`Duplicate auth tokenId detected: ${credential.tokenId}`);  // line 146: executes this statement as part of this file's behavior
    ids.add(credential.tokenId);  // line 147: executes this statement as part of this file's behavior
  }  // line 148: executes this statement as part of this file's behavior
}  // line 149: executes this statement as part of this file's behavior
