// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import crypto, { randomUUID } from "node:crypto";  // line 7: executes this statement as part of this file's behavior
import { registerSecretValue, unregisterSecretValue } from "./redaction.js";  // line 8: executes this statement as part of this file's behavior
import { RequestValidationError } from "./projectStore.js";  // line 9: executes this statement as part of this file's behavior
import type { SecretRecord, SecretStorePersistence, SecretStorePort } from "./persistence.js";  // line 10: executes this statement as part of this file's behavior

export const supportedSecretTypes = new Set([  // line 12: executes this statement as part of this file's behavior
  "discord_bot_token",  // line 13: executes this statement as part of this file's behavior
  "github_token",  // line 14: executes this statement as part of this file's behavior
  "ssh_private_key",  // line 15: executes this statement as part of this file's behavior
  "deployment_api_token",  // line 16: executes this statement as part of this file's behavior
  "webhook_url",  // line 17: executes this statement as part of this file's behavior
  "database_url",  // line 18: executes this statement as part of this file's behavior
  "custom",  // line 19: executes this statement as part of this file's behavior
]);  // line 20: executes this statement as part of this file's behavior

export interface SecretSummary {  // line 22: executes this statement as part of this file's behavior
  id: string;  // line 23: executes this statement as part of this file's behavior
  projectId: string | null;  // line 24: executes this statement as part of this file's behavior
  name: string;  // line 25: executes this statement as part of this file's behavior
  type: string;  // line 26: executes this statement as part of this file's behavior
  storageMode: "local_dev";  // line 27: executes this statement as part of this file's behavior
  fingerprint: string;  // line 28: executes this statement as part of this file's behavior
  createdAt: string;  // line 29: executes this statement as part of this file's behavior
  updatedAt: string;  // line 30: executes this statement as part of this file's behavior
  rotatedAt: string | null;  // line 31: executes this statement as part of this file's behavior
}  // line 32: executes this statement as part of this file's behavior

export interface StoredSecret extends SecretSummary {  // line 34: executes this statement as part of this file's behavior
  value: string;  // line 35: executes this statement as part of this file's behavior
}  // line 36: executes this statement as part of this file's behavior

export interface CreateSecretInput {  // line 38: executes this statement as part of this file's behavior
  projectId: string | null;  // line 39: executes this statement as part of this file's behavior
  name: string;  // line 40: executes this statement as part of this file's behavior
  type: string;  // line 41: executes this statement as part of this file's behavior
  value: string;  // line 42: executes this statement as part of this file's behavior
}  // line 43: executes this statement as part of this file's behavior

export interface UpdateSecretInput {  // line 45: executes this statement as part of this file's behavior
  projectId?: string | null;  // line 46: executes this statement as part of this file's behavior
  name?: string;  // line 47: executes this statement as part of this file's behavior
  type?: string;  // line 48: executes this statement as part of this file's behavior
}  // line 49: executes this statement as part of this file's behavior

export class SecretStore implements SecretStorePort {  // line 51: executes this statement as part of this file's behavior
  private readonly secrets = new Map<string, StoredSecret>();  // line 52: executes this statement as part of this file's behavior

  constructor(private readonly persistence?: SecretStorePersistence) {  // line 54: executes this statement as part of this file's behavior
    for (const secret of persistence?.loadSecrets() ?? []) {  // line 55: executes this statement as part of this file's behavior
      this.secrets.set(secret.id, secret as SecretRecord);  // line 56: executes this statement as part of this file's behavior
      registerSecretValue(secret.value);  // line 57: executes this statement as part of this file's behavior
    }  // line 58: executes this statement as part of this file's behavior
  }  // line 59: executes this statement as part of this file's behavior

  list(): SecretSummary[] {  // line 61: executes this statement as part of this file's behavior
    return [...this.secrets.values()].map(toSummary).sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));  // line 62: executes this statement as part of this file's behavior
  }  // line 63: executes this statement as part of this file's behavior

  get(secretId: string): SecretSummary | undefined {  // line 65: executes this statement as part of this file's behavior
    const secret = this.secrets.get(secretId);  // line 66: executes this statement as part of this file's behavior
    return secret ? toSummary(secret) : undefined;  // line 67: executes this statement as part of this file's behavior
  }  // line 68: executes this statement as part of this file's behavior

  has(secretId: string): boolean {  // line 70: executes this statement as part of this file's behavior
    return this.secrets.has(secretId);  // line 71: executes this statement as part of this file's behavior
  }  // line 72: executes this statement as part of this file's behavior

  getValue(secretId: string): string | undefined {  // line 74: executes this statement as part of this file's behavior
    return this.secrets.get(secretId)?.value;  // line 75: executes this statement as part of this file's behavior
  }  // line 76: executes this statement as part of this file's behavior

  create(input: CreateSecretInput): SecretSummary {  // line 78: executes this statement as part of this file's behavior
    validateNameTypeValue(input.name, input.type, input.value);  // line 79: executes this statement as part of this file's behavior
    const now = new Date().toISOString();  // line 80: executes this statement as part of this file's behavior
    const secret: StoredSecret = {  // line 81: executes this statement as part of this file's behavior
      id: `secret_${randomUUID()}`,  // line 82: executes this statement as part of this file's behavior
      projectId: input.projectId,  // line 83: executes this statement as part of this file's behavior
      name: input.name.trim(),  // line 84: executes this statement as part of this file's behavior
      type: input.type,  // line 85: executes this statement as part of this file's behavior
      storageMode: "local_dev",  // line 86: executes this statement as part of this file's behavior
      value: input.value,  // line 87: executes this statement as part of this file's behavior
      fingerprint: fingerprint(input.value),  // line 88: executes this statement as part of this file's behavior
      createdAt: now,  // line 89: executes this statement as part of this file's behavior
      updatedAt: now,  // line 90: executes this statement as part of this file's behavior
      rotatedAt: null,  // line 91: executes this statement as part of this file's behavior
    };  // line 92: executes this statement as part of this file's behavior
    this.secrets.set(secret.id, secret);  // line 93: executes this statement as part of this file's behavior
    registerSecretValue(input.value);  // line 94: executes this statement as part of this file's behavior
    this.persistence?.saveSecret(secret);  // line 95: executes this statement as part of this file's behavior
    return toSummary(secret);  // line 96: executes this statement as part of this file's behavior
  }  // line 97: executes this statement as part of this file's behavior

  update(secretId: string, input: UpdateSecretInput): SecretSummary | undefined {  // line 99: executes this statement as part of this file's behavior
    const existing = this.secrets.get(secretId);  // line 100: executes this statement as part of this file's behavior
    if (!existing) return undefined;  // line 101: executes this statement as part of this file's behavior
    if (input.name !== undefined && input.name.trim().length === 0) throw new RequestValidationError([{ field: "name", message: "Secret name is required." }]);  // line 102: executes this statement as part of this file's behavior
    if (input.type !== undefined && !supportedSecretTypes.has(input.type)) throw new RequestValidationError([{ field: "type", message: "Unsupported secret type." }]);  // line 103: executes this statement as part of this file's behavior
    const updated: StoredSecret = {  // line 104: executes this statement as part of this file's behavior
      ...existing,  // line 105: executes this statement as part of this file's behavior
      projectId: input.projectId !== undefined ? input.projectId : existing.projectId,  // line 106: executes this statement as part of this file's behavior
      name: input.name?.trim() ?? existing.name,  // line 107: executes this statement as part of this file's behavior
      type: input.type ?? existing.type,  // line 108: executes this statement as part of this file's behavior
      updatedAt: new Date().toISOString(),  // line 109: executes this statement as part of this file's behavior
    };  // line 110: executes this statement as part of this file's behavior
    this.secrets.set(secretId, updated);  // line 111: executes this statement as part of this file's behavior
    this.persistence?.saveSecret(updated);  // line 112: executes this statement as part of this file's behavior
    return toSummary(updated);  // line 113: executes this statement as part of this file's behavior
  }  // line 114: executes this statement as part of this file's behavior

  rotate(secretId: string, value: string): SecretSummary | undefined {  // line 116: executes this statement as part of this file's behavior
    const existing = this.secrets.get(secretId);  // line 117: executes this statement as part of this file's behavior
    if (!existing) return undefined;  // line 118: executes this statement as part of this file's behavior
    if (!value) throw new RequestValidationError([{ field: "value", message: "Secret value is required." }]);  // line 119: executes this statement as part of this file's behavior
    unregisterSecretValue(existing.value);  // line 120: executes this statement as part of this file's behavior
    registerSecretValue(value);  // line 121: executes this statement as part of this file's behavior
    const now = new Date().toISOString();  // line 122: executes this statement as part of this file's behavior
    const updated: StoredSecret = { ...existing, value, fingerprint: fingerprint(value), updatedAt: now, rotatedAt: now };  // line 123: executes this statement as part of this file's behavior
    this.secrets.set(secretId, updated);  // line 124: executes this statement as part of this file's behavior
    this.persistence?.saveSecret(updated);  // line 125: executes this statement as part of this file's behavior
    return toSummary(updated);  // line 126: executes this statement as part of this file's behavior
  }  // line 127: executes this statement as part of this file's behavior

  delete(secretId: string): boolean {  // line 129: executes this statement as part of this file's behavior
    const existing = this.secrets.get(secretId);  // line 130: executes this statement as part of this file's behavior
    if (existing) unregisterSecretValue(existing.value);  // line 131: executes this statement as part of this file's behavior
    const deleted = this.secrets.delete(secretId);  // line 132: executes this statement as part of this file's behavior
    if (deleted) this.persistence?.deleteSecret(secretId);  // line 133: executes this statement as part of this file's behavior
    return deleted;  // line 134: executes this statement as part of this file's behavior
  }  // line 135: executes this statement as part of this file's behavior
}  // line 136: executes this statement as part of this file's behavior

export function parseCreateSecretInput(value: unknown): CreateSecretInput {  // line 138: executes this statement as part of this file's behavior
  const object = asRecord(value);  // line 139: executes this statement as part of this file's behavior
  const input = {  // line 140: executes this statement as part of this file's behavior
    projectId: stringField(object, "projectId", false) ?? null,  // line 141: executes this statement as part of this file's behavior
    name: stringField(object, "name", true) ?? "",  // line 142: executes this statement as part of this file's behavior
    type: stringField(object, "type", false) ?? "",  // line 143: executes this statement as part of this file's behavior
    value: stringField(object, "value", false) ?? "",  // line 144: executes this statement as part of this file's behavior
  };  // line 145: executes this statement as part of this file's behavior
  validateNameTypeValue(input.name, input.type, input.value);  // line 146: executes this statement as part of this file's behavior
  return input;  // line 147: executes this statement as part of this file's behavior
}  // line 148: executes this statement as part of this file's behavior

export function parseUpdateSecretInput(value: unknown): UpdateSecretInput {  // line 150: executes this statement as part of this file's behavior
  const object = asRecord(value);  // line 151: executes this statement as part of this file's behavior
  const output: UpdateSecretInput = {};  // line 152: executes this statement as part of this file's behavior
  if ("projectId" in object) output.projectId = stringField(object, "projectId", false) ?? null;  // line 153: executes this statement as part of this file's behavior
  if ("name" in object) output.name = stringField(object, "name", true) ?? "";  // line 154: executes this statement as part of this file's behavior
  if ("type" in object) output.type = stringField(object, "type", false) ?? "";  // line 155: executes this statement as part of this file's behavior
  return output;  // line 156: executes this statement as part of this file's behavior
}  // line 157: executes this statement as part of this file's behavior

export function parseRotateSecretInput(value: unknown): string {  // line 159: executes this statement as part of this file's behavior
  const object = asRecord(value);  // line 160: executes this statement as part of this file's behavior
  const nextValue = stringField(object, "value", false);  // line 161: executes this statement as part of this file's behavior
  if (!nextValue) throw new RequestValidationError([{ field: "value", message: "Secret value is required." }]);  // line 162: executes this statement as part of this file's behavior
  return nextValue;  // line 163: executes this statement as part of this file's behavior
}  // line 164: executes this statement as part of this file's behavior

function validateNameTypeValue(name: string, type: string, value: string): void {  // line 166: executes this statement as part of this file's behavior
  const problems = [];  // line 167: executes this statement as part of this file's behavior
  if (!name.trim()) problems.push({ field: "name", message: "Secret name is required." });  // line 168: executes this statement as part of this file's behavior
  if (!supportedSecretTypes.has(type)) problems.push({ field: "type", message: "Unsupported secret type." });  // line 169: executes this statement as part of this file's behavior
  if (!value) problems.push({ field: "value", message: "Secret value is required." });  // line 170: executes this statement as part of this file's behavior
  if (problems.length > 0) throw new RequestValidationError(problems);  // line 171: executes this statement as part of this file's behavior
}  // line 172: executes this statement as part of this file's behavior

function fingerprint(value: string): string {  // line 174: executes this statement as part of this file's behavior
  return `sha256:${crypto.createHash("sha256").update(value).digest("hex")}`;  // line 175: executes this statement as part of this file's behavior
}  // line 176: executes this statement as part of this file's behavior

function toSummary(secret: StoredSecret): SecretSummary {  // line 178: executes this statement as part of this file's behavior
  const { value: _value, ...summary } = secret;  // line 179: executes this statement as part of this file's behavior
  return summary;  // line 180: executes this statement as part of this file's behavior
}  // line 181: executes this statement as part of this file's behavior

function asRecord(value: unknown): Record<string, unknown> {  // line 183: executes this statement as part of this file's behavior
  return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};  // line 184: executes this statement as part of this file's behavior
}  // line 185: executes this statement as part of this file's behavior

function stringField(object: Record<string, unknown>, field: string, trim: boolean): string | undefined {  // line 187: executes this statement as part of this file's behavior
  const value = object[field];  // line 188: executes this statement as part of this file's behavior
  if (typeof value !== "string") return undefined;  // line 189: executes this statement as part of this file's behavior
  const normalized = trim ? value.trim() : value;  // line 190: executes this statement as part of this file's behavior
  return normalized.length > 0 ? normalized : undefined;  // line 191: executes this statement as part of this file's behavior
}  // line 192: executes this statement as part of this file's behavior
