import crypto, { randomUUID } from "node:crypto";
import { registerSecretValue, unregisterSecretValue } from "./redaction.js";
import { RequestValidationError } from "./projectStore.js";
import type { SecretRecord, SecretStorePersistence, SecretStorePort } from "./persistence.js";

export const supportedSecretTypes = new Set([
  "discord_bot_token",
  "github_token",
  "ssh_private_key",
  "deployment_api_token",
  "webhook_url",
  "database_url",
  "custom",
]);

export interface SecretSummary {
  id: string;
  projectId: string | null;
  name: string;
  type: string;
  storageMode: "local_dev";
  fingerprint: string;
  createdAt: string;
  updatedAt: string;
  rotatedAt: string | null;
}

export interface StoredSecret extends SecretSummary {
  value: string;
}

export interface CreateSecretInput {
  projectId: string | null;
  name: string;
  type: string;
  value: string;
}

export interface UpdateSecretInput {
  projectId?: string | null;
  name?: string;
  type?: string;
}

export class SecretStore implements SecretStorePort {
  private readonly secrets = new Map<string, StoredSecret>();

  constructor(private readonly persistence?: SecretStorePersistence) {
    for (const secret of persistence?.loadSecrets() ?? []) {
      this.secrets.set(secret.id, secret as SecretRecord);
      registerSecretValue(secret.value);
    }
  }

  list(): SecretSummary[] {
    return [...this.secrets.values()].map(toSummary).sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
  }

  get(secretId: string): SecretSummary | undefined {
    const secret = this.secrets.get(secretId);
    return secret ? toSummary(secret) : undefined;
  }

  has(secretId: string): boolean {
    return this.secrets.has(secretId);
  }

  getValue(secretId: string): string | undefined {
    return this.secrets.get(secretId)?.value;
  }

  create(input: CreateSecretInput): SecretSummary {
    validateNameTypeValue(input.name, input.type, input.value);
    const now = new Date().toISOString();
    const secret: StoredSecret = {
      id: `secret_${randomUUID()}`,
      projectId: input.projectId,
      name: input.name.trim(),
      type: input.type,
      storageMode: "local_dev",
      value: input.value,
      fingerprint: fingerprint(input.value),
      createdAt: now,
      updatedAt: now,
      rotatedAt: null,
    };
    this.secrets.set(secret.id, secret);
    registerSecretValue(input.value);
    this.persistence?.saveSecret(secret);
    return toSummary(secret);
  }

  update(secretId: string, input: UpdateSecretInput): SecretSummary | undefined {
    const existing = this.secrets.get(secretId);
    if (!existing) return undefined;
    if (input.name !== undefined && input.name.trim().length === 0) throw new RequestValidationError([{ field: "name", message: "Secret name is required." }]);
    if (input.type !== undefined && !supportedSecretTypes.has(input.type)) throw new RequestValidationError([{ field: "type", message: "Unsupported secret type." }]);
    const updated: StoredSecret = {
      ...existing,
      projectId: input.projectId !== undefined ? input.projectId : existing.projectId,
      name: input.name?.trim() ?? existing.name,
      type: input.type ?? existing.type,
      updatedAt: new Date().toISOString(),
    };
    this.secrets.set(secretId, updated);
    this.persistence?.saveSecret(updated);
    return toSummary(updated);
  }

  rotate(secretId: string, value: string): SecretSummary | undefined {
    const existing = this.secrets.get(secretId);
    if (!existing) return undefined;
    if (!value) throw new RequestValidationError([{ field: "value", message: "Secret value is required." }]);
    unregisterSecretValue(existing.value);
    registerSecretValue(value);
    const now = new Date().toISOString();
    const updated: StoredSecret = { ...existing, value, fingerprint: fingerprint(value), updatedAt: now, rotatedAt: now };
    this.secrets.set(secretId, updated);
    this.persistence?.saveSecret(updated);
    return toSummary(updated);
  }

  delete(secretId: string): boolean {
    const existing = this.secrets.get(secretId);
    if (existing) unregisterSecretValue(existing.value);
    const deleted = this.secrets.delete(secretId);
    if (deleted) this.persistence?.deleteSecret(secretId);
    return deleted;
  }
}

export function parseCreateSecretInput(value: unknown): CreateSecretInput {
  const object = asRecord(value);
  const input = {
    projectId: stringField(object, "projectId", false) ?? null,
    name: stringField(object, "name", true) ?? "",
    type: stringField(object, "type", false) ?? "",
    value: stringField(object, "value", false) ?? "",
  };
  validateNameTypeValue(input.name, input.type, input.value);
  return input;
}

export function parseUpdateSecretInput(value: unknown): UpdateSecretInput {
  const object = asRecord(value);
  const output: UpdateSecretInput = {};
  if ("projectId" in object) output.projectId = stringField(object, "projectId", false) ?? null;
  if ("name" in object) output.name = stringField(object, "name", true) ?? "";
  if ("type" in object) output.type = stringField(object, "type", false) ?? "";
  return output;
}

export function parseRotateSecretInput(value: unknown): string {
  const object = asRecord(value);
  const nextValue = stringField(object, "value", false);
  if (!nextValue) throw new RequestValidationError([{ field: "value", message: "Secret value is required." }]);
  return nextValue;
}

function validateNameTypeValue(name: string, type: string, value: string): void {
  const problems = [];
  if (!name.trim()) problems.push({ field: "name", message: "Secret name is required." });
  if (!supportedSecretTypes.has(type)) problems.push({ field: "type", message: "Unsupported secret type." });
  if (!value) problems.push({ field: "value", message: "Secret value is required." });
  if (problems.length > 0) throw new RequestValidationError(problems);
}

function fingerprint(value: string): string {
  return `sha256:${crypto.createHash("sha256").update(value).digest("hex")}`;
}

function toSummary(secret: StoredSecret): SecretSummary {
  const { value: _value, ...summary } = secret;
  return summary;
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
