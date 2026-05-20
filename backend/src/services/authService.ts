import type { IncomingMessage } from "node:http";

export interface AuthenticatedActor {
  id: string;
  tokenId: string;
  roles: string[];
  projectIds: string[];
  authMethod: "bearer" | "session";
}

interface AuthCredential {
  token: string;
  actorId: string;
  tokenId: string;
  roles: string[];
  projectIds: string[];
  authMethod?: "bearer" | "session";
}

const GLOBAL_PROJECT = "*";
const MIN_TOKEN_LENGTH = 8;

export function authenticateRequest(req: IncomingMessage): AuthenticatedActor {
  const bearerToken = readBearerToken(req);
  const sessionToken = readSessionToken(req);
  const credentials = configuredCredentials();
  const match = credentials.find((credential) => safeTokenEquals(credential.token, bearerToken) || safeTokenEquals(credential.token, sessionToken));
  if (!match) {
    throw { statusCode: 401, code: "AUTHENTICATION_REQUIRED", message: "A valid bearer token or session credential is required.", details: { schemes: ["Bearer", "botBladeSession"] } };
  }
  return {
    id: match.actorId,
    tokenId: match.tokenId,
    roles: match.roles,
    projectIds: match.projectIds,
    authMethod: match.authMethod ?? (match.token === bearerToken ? "bearer" : "session"),
  };
}

export function canAccessProject(actor: AuthenticatedActor, projectId: string | null | undefined): boolean {
  if (hasGlobalProjectAccess(actor)) return true;
  return typeof projectId === "string" && actor.projectIds.includes(projectId);
}

export function hasGlobalProjectAccess(actor: AuthenticatedActor): boolean {
  return actor.roles.includes("admin") || actor.projectIds.includes(GLOBAL_PROJECT);
}

export function assertProjectAccess(actor: AuthenticatedActor, projectId: string | null | undefined): void {
  if (canAccessProject(actor, projectId)) return;
  throw { statusCode: 403, code: "PROJECT_ACCESS_DENIED", message: "The authenticated actor is not authorized for this project.", details: { projectId: projectId ?? null } };
}

export function assertGlobalAccess(actor: AuthenticatedActor, resource: string): void {
  if (hasGlobalProjectAccess(actor)) return;
  throw { statusCode: 403, code: "GLOBAL_ACCESS_DENIED", message: `The authenticated actor is not authorized to access ${resource}.`, details: { resource } };
}

export function assertExecutionAccess(actor: AuthenticatedActor, resource: string): void {
  if (actor.roles.includes("admin") || actor.roles.includes("execute")) return;
  throw { statusCode: 403, code: "EXECUTION_ACCESS_DENIED", message: `The authenticated actor is not authorized to execute ${resource}.`, details: { resource } };
}

function readBearerToken(req: IncomingMessage): string | undefined {
  const header = req.headers.authorization;
  if (typeof header !== "string") return undefined;
  const match = header.match(/^Bearer\s+(.+)$/i);
  return match?.[1]?.trim();
}

function readSessionToken(req: IncomingMessage): string | undefined {
  const explicitHeader = req.headers["x-session-token"];
  if (typeof explicitHeader === "string" && explicitHeader.trim()) return explicitHeader.trim();
  const cookieHeader = req.headers.cookie;
  if (typeof cookieHeader !== "string") return undefined;
  for (const cookie of cookieHeader.split(";")) {
    const [name, ...valueParts] = cookie.trim().split("=");
    if (name === "botBladeSession") return decodeURIComponent(valueParts.join("=")).trim();
  }
  return undefined;
}

function configuredCredentials(): AuthCredential[] {
  const credentials = [...parseJsonCredentials(process.env.BOTBLADE_AUTH_TOKENS, "bearer"), ...parseJsonCredentials(process.env.BOTBLADE_SESSION_TOKENS, "session")];
  ensureUniqueTokenIds(credentials);
  if (credentials.length > 0) return credentials;
  if (process.env.BOTBLADE_LOCAL_DEV !== "true") return [];
  const legacyTokens = splitTokens(process.env.BOTBLADE_API_TOKENS ?? process.env.BOTBLADE_API_TOKEN);
  return legacyTokens.map((token, index) => ({ token, actorId: index === 0 ? "local_admin" : `local_user_${index + 1}`, tokenId: `env_token_${index + 1}`, roles: ["admin"], projectIds: [GLOBAL_PROJECT], authMethod: "bearer" }));
}

function parseJsonCredentials(value: string | undefined, authMethod: "bearer" | "session"): AuthCredential[] {
  if (!value?.trim()) return [];
  try {
    const parsed = JSON.parse(value) as unknown;
    if (Array.isArray(parsed)) return parsed.flatMap((entry, index) => normalizeCredential(entry, index, authMethod));
    return normalizeCredential(parsed, 0, authMethod);
  } catch {
    return splitTokens(value).map((token, index) => ({ token, actorId: `${authMethod}_user_${index + 1}`, tokenId: `${authMethod}_token_${index + 1}`, roles: ["admin"], projectIds: [GLOBAL_PROJECT], authMethod }));
  }
}

function normalizeCredential(value: unknown, index: number, authMethod: "bearer" | "session"): AuthCredential[] {
  if (typeof value === "string" && value.trim().length >= MIN_TOKEN_LENGTH) return [{ token: value.trim(), actorId: `${authMethod}_user_${index + 1}`, tokenId: `${authMethod}_token_${index + 1}`, roles: ["admin"], projectIds: [GLOBAL_PROJECT], authMethod }];
  if (!value || typeof value !== "object" || Array.isArray(value)) return [];
  const record = value as Record<string, unknown>;
  const token = typeof record.token === "string" ? record.token.trim() : "";
  if (!token || token.length < MIN_TOKEN_LENGTH) return [];
  return [{
    token,
    actorId: typeof record.actorId === "string" && record.actorId.trim() ? record.actorId.trim() : typeof record.userId === "string" && record.userId.trim() ? record.userId.trim() : `${authMethod}_user_${index + 1}`,
    tokenId: typeof record.tokenId === "string" && record.tokenId.trim() ? record.tokenId.trim() : `${authMethod}_token_${index + 1}`,
    roles: stringArray(record.roles),
    projectIds: stringArray(record.projectIds).length > 0 ? stringArray(record.projectIds) : [GLOBAL_PROJECT],
    authMethod,
  }];
}

function stringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string" && item.trim().length > 0).map((item) => item.trim()) : [];
}

function splitTokens(value: string | undefined): string[] {
  return value?.split(",").map((token) => token.trim()).filter((token) => token.length >= MIN_TOKEN_LENGTH) ?? [];
}

function safeTokenEquals(expected: string, provided?: string): boolean {
  if (!provided) return false;
  const a = expected;
  const b = provided;
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i += 1) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

function ensureUniqueTokenIds(credentials: AuthCredential[]): void {
  const ids = new Set<string>();
  for (const credential of credentials) {
    if (ids.has(credential.tokenId)) throw new Error(`Duplicate auth tokenId detected: ${credential.tokenId}`);
    ids.add(credential.tokenId);
  }
}
