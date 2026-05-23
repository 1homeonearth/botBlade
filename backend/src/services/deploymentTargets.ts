// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import { randomUUID } from "node:crypto";  // line 7: executes this statement as part of this file's behavior
import { spawn } from "node:child_process";  // line 8: executes this statement as part of this file's behavior
import { RequestValidationError } from "./projectStore.js";  // line 9: executes this statement as part of this file's behavior
import type { DeploymentTargetStorePersistence } from "./persistence.js";  // line 10: executes this statement as part of this file's behavior
import { capabilitiesForTargetType, type DeploymentAdapterCapabilities } from "./deploymentAdapters.js";  // line 11: executes this statement as part of this file's behavior

export type DeploymentTargetType = "local_process" | "local_docker";  // line 13: executes this statement as part of this file's behavior
export const supportedDeploymentTargetTypes = ["local_process", "local_docker"] as const;  // line 14: executes this statement as part of this file's behavior

export interface DeploymentTarget {  // line 16: executes this statement as part of this file's behavior
  id: string;  // line 17: executes this statement as part of this file's behavior
  name: string;  // line 18: executes this statement as part of this file's behavior
  type: DeploymentTargetType;  // line 19: executes this statement as part of this file's behavior
  config: Record<string, unknown>;  // line 20: executes this statement as part of this file's behavior
  secretRefs: string[];  // line 21: executes this statement as part of this file's behavior
  createdAt: string;  // line 22: executes this statement as part of this file's behavior
  updatedAt: string;  // line 23: executes this statement as part of this file's behavior
  capabilities?: DeploymentAdapterCapabilities;  // line 24: executes this statement as part of this file's behavior
}  // line 25: executes this statement as part of this file's behavior

export interface DeploymentTargetTestResult {  // line 27: executes this statement as part of this file's behavior
  ok: boolean;  // line 28: executes this statement as part of this file's behavior
  status: "available" | "unavailable";  // line 29: executes this statement as part of this file's behavior
  message: string;  // line 30: executes this statement as part of this file's behavior
}  // line 31: executes this statement as part of this file's behavior

export interface CreateDeploymentTargetInput {  // line 33: executes this statement as part of this file's behavior
  name: string;  // line 34: executes this statement as part of this file's behavior
  type: DeploymentTargetType;  // line 35: executes this statement as part of this file's behavior
  config: Record<string, unknown>;  // line 36: executes this statement as part of this file's behavior
  secretRefs: string[];  // line 37: executes this statement as part of this file's behavior
}  // line 38: executes this statement as part of this file's behavior

export interface UpdateDeploymentTargetInput {  // line 40: executes this statement as part of this file's behavior
  name?: string;  // line 41: executes this statement as part of this file's behavior
  type?: DeploymentTargetType;  // line 42: executes this statement as part of this file's behavior
  config?: Record<string, unknown>;  // line 43: executes this statement as part of this file's behavior
  secretRefs?: string[];  // line 44: executes this statement as part of this file's behavior
}  // line 45: executes this statement as part of this file's behavior

export class DeploymentTargetStore {  // line 47: executes this statement as part of this file's behavior
  private readonly targets = new Map<string, DeploymentTarget>();  // line 48: executes this statement as part of this file's behavior

  constructor(private readonly persistence?: DeploymentTargetStorePersistence) {  // line 50: executes this statement as part of this file's behavior
    for (const target of persistence?.loadDeploymentTargets() ?? []) this.targets.set(target.id, target);  // line 51: executes this statement as part of this file's behavior
  }  // line 52: executes this statement as part of this file's behavior

  list(): DeploymentTarget[] { return [...this.targets.values()].sort((a, b) => a.createdAt.localeCompare(b.createdAt)); }  // line 54: executes this statement as part of this file's behavior
  get(id: string): DeploymentTarget | undefined { return this.targets.get(id); }  // line 55: executes this statement as part of this file's behavior

  create(input: unknown): DeploymentTarget {  // line 57: executes this statement as part of this file's behavior
    const parsed = parseTarget(input, false) as CreateDeploymentTargetInput;  // line 58: executes this statement as part of this file's behavior
    const now = new Date().toISOString();  // line 59: executes this statement as part of this file's behavior
    const target: DeploymentTarget = { id: `target_${randomUUID()}`, ...parsed, createdAt: now, updatedAt: now };  // line 60: executes this statement as part of this file's behavior
    this.targets.set(target.id, target);  // line 61: executes this statement as part of this file's behavior
    this.persistence?.saveDeploymentTarget(target);  // line 62: executes this statement as part of this file's behavior
    return target;  // line 63: executes this statement as part of this file's behavior
  }  // line 64: executes this statement as part of this file's behavior

  update(id: string, input: unknown): DeploymentTarget | undefined {  // line 66: executes this statement as part of this file's behavior
    const existing = this.targets.get(id);  // line 67: executes this statement as part of this file's behavior
    if (!existing) return undefined;  // line 68: executes this statement as part of this file's behavior
    const parsed = parseTarget(input, true) as UpdateDeploymentTargetInput;  // line 69: executes this statement as part of this file's behavior
    const updated: DeploymentTarget = { ...existing, ...parsed, updatedAt: new Date().toISOString() };  // line 70: executes this statement as part of this file's behavior
    validateTargetConfig(updated.type, updated.config);  // line 71: executes this statement as part of this file's behavior
    this.targets.set(id, updated);  // line 72: executes this statement as part of this file's behavior
    this.persistence?.saveDeploymentTarget(updated);  // line 73: executes this statement as part of this file's behavior
    return updated;  // line 74: executes this statement as part of this file's behavior
  }  // line 75: executes this statement as part of this file's behavior

  delete(id: string): boolean {  // line 77: executes this statement as part of this file's behavior
    const deleted = this.targets.delete(id);  // line 78: executes this statement as part of this file's behavior
    if (deleted) this.persistence?.deleteDeploymentTarget(id);  // line 79: executes this statement as part of this file's behavior
    return deleted;  // line 80: executes this statement as part of this file's behavior
  }  // line 81: executes this statement as part of this file's behavior
}  // line 82: executes this statement as part of this file's behavior

export async function testDeploymentTarget(target: DeploymentTarget): Promise<DeploymentTargetTestResult> {  // line 84: executes this statement as part of this file's behavior
  if (target.type === "local_process") return { ok: true, status: "available", message: "Backend can launch local child processes." };  // line 85: executes this statement as part of this file's behavior
  return new Promise((resolve) => {  // line 86: executes this statement as part of this file's behavior
    const child = spawn("docker", ["--version"], { shell: false });  // line 87: executes this statement as part of this file's behavior
    let output = "";  // line 88: executes this statement as part of this file's behavior
    child.stdout.on("data", (chunk) => { output += String(chunk); });  // line 89: executes this statement as part of this file's behavior
    child.stderr.on("data", (chunk) => { output += String(chunk); });  // line 90: executes this statement as part of this file's behavior
    child.on("error", () => resolve({ ok: false, status: "unavailable", message: "Docker CLI is not available to this backend process." }));  // line 91: executes this statement as part of this file's behavior
    child.on("close", (code) => resolve(code === 0  // line 92: executes this statement as part of this file's behavior
      ? { ok: true, status: "available", message: output.trim() || "Docker CLI is available." }  // line 93: executes this statement as part of this file's behavior
      : { ok: false, status: "unavailable", message: output.trim() || `Docker CLI exited with code ${code}.` }));  // line 94: executes this statement as part of this file's behavior
  });  // line 95: executes this statement as part of this file's behavior
}  // line 96: executes this statement as part of this file's behavior

function parseTarget(input: unknown, partial: boolean): CreateDeploymentTargetInput | UpdateDeploymentTargetInput {  // line 98: executes this statement as part of this file's behavior
  const object = asRecord(input);  // line 99: executes this statement as part of this file's behavior
  const problems: { field: string; message: string }[] = [];  // line 100: executes this statement as part of this file's behavior
  const output: UpdateDeploymentTargetInput = {};  // line 101: executes this statement as part of this file's behavior
  if (!partial || "name" in object) {  // line 102: executes this statement as part of this file's behavior
    if (typeof object.name === "string" && object.name.trim()) output.name = object.name.trim(); else problems.push({ field: "name", message: "Target name is required." });  // line 103: executes this statement as part of this file's behavior
  }  // line 104: executes this statement as part of this file's behavior
  if (!partial || "type" in object) {  // line 105: executes this statement as part of this file's behavior
    if (object.type === "local_process" || object.type === "local_docker") output.type = object.type; else problems.push({ field: "type", message: "Target type must be local_process or local_docker." });  // line 106: executes this statement as part of this file's behavior
  }  // line 107: executes this statement as part of this file's behavior
  if ("config" in object) {  // line 108: executes this statement as part of this file's behavior
    output.config = asRecord(object.config);  // line 109: executes this statement as part of this file's behavior
  } else if (!partial) output.config = {};  // line 110: executes this statement as part of this file's behavior
  if ("secretRefs" in object) {  // line 111: executes this statement as part of this file's behavior
    output.secretRefs = Array.isArray(object.secretRefs) ? object.secretRefs.filter((item): item is string => typeof item === "string" && item.trim().length > 0) : [];  // line 112: executes this statement as part of this file's behavior
  } else if (!partial) output.secretRefs = [];  // line 113: executes this statement as part of this file's behavior
  if (output.config && Object.keys(output.config).some((key) => key.toLowerCase().includes("secret") || key.toLowerCase().includes("token") || key.toLowerCase().includes("password"))) problems.push({ field: "config", message: "Do not store secret values in target config; use secretRefs." });  // line 114: executes this statement as part of this file's behavior
  if (problems.length > 0) throw new RequestValidationError(problems);  // line 115: executes this statement as part of this file's behavior
  if (output.type && output.config) validateTargetConfig(output.type, output.config);  // line 116: executes this statement as part of this file's behavior
  return output as CreateDeploymentTargetInput | UpdateDeploymentTargetInput;  // line 117: executes this statement as part of this file's behavior
}  // line 118: executes this statement as part of this file's behavior

function validateTargetConfig(type: DeploymentTargetType, config: Record<string, unknown>): void {  // line 120: executes this statement as part of this file's behavior
  const allowed = type === "local_process" ? new Set(["cwd"]) : new Set(["image", "dockerfile"]);  // line 121: executes this statement as part of this file's behavior
  const extra = Object.keys(config).filter((key) => !allowed.has(key));  // line 122: executes this statement as part of this file's behavior
  if (extra.length > 0) throw new RequestValidationError(extra.map((key) => ({ field: `config.${key}`, message: "Unsupported config field for target type." })));  // line 123: executes this statement as part of this file's behavior
  if ("cwd" in config && typeof config.cwd !== "string") throw new RequestValidationError([{ field: "config.cwd", message: "cwd must be a string." }]);  // line 124: executes this statement as part of this file's behavior
  if ("image" in config && typeof config.image !== "string") throw new RequestValidationError([{ field: "config.image", message: "image must be a string." }]);  // line 125: executes this statement as part of this file's behavior
  if ("dockerfile" in config && typeof config.dockerfile !== "string") throw new RequestValidationError([{ field: "config.dockerfile", message: "dockerfile must be a string." }]);  // line 126: executes this statement as part of this file's behavior
}  // line 127: executes this statement as part of this file's behavior

function asRecord(value: unknown): Record<string, unknown> { return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {}; }  // line 129: executes this statement as part of this file's behavior

export function deploymentTargetWithCapabilities(target: DeploymentTarget): DeploymentTarget {  // line 131: executes this statement as part of this file's behavior
  return { ...target, capabilities: capabilitiesForTargetType(target.type) };  // line 132: executes this statement as part of this file's behavior
}  // line 133: executes this statement as part of this file's behavior
