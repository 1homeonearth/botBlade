import { randomUUID } from "node:crypto";
import { spawn } from "node:child_process";
import { RequestValidationError } from "./projectStore.js";
import type { DeploymentTargetStorePersistence } from "./persistence.js";

export type DeploymentTargetType = "local_process" | "local_docker";
export const supportedDeploymentTargetTypes = ["local_process", "local_docker"] as const;

export interface DeploymentTarget {
  id: string;
  name: string;
  type: DeploymentTargetType;
  config: Record<string, unknown>;
  secretRefs: string[];
  createdAt: string;
  updatedAt: string;
}

export interface DeploymentTargetTestResult {
  ok: boolean;
  status: "available" | "unavailable";
  message: string;
}

export interface CreateDeploymentTargetInput {
  name: string;
  type: DeploymentTargetType;
  config: Record<string, unknown>;
  secretRefs: string[];
}

export interface UpdateDeploymentTargetInput {
  name?: string;
  type?: DeploymentTargetType;
  config?: Record<string, unknown>;
  secretRefs?: string[];
}

export class DeploymentTargetStore {
  private readonly targets = new Map<string, DeploymentTarget>();

  constructor(private readonly persistence?: DeploymentTargetStorePersistence) {
    for (const target of persistence?.loadDeploymentTargets() ?? []) this.targets.set(target.id, target);
  }

  list(): DeploymentTarget[] { return [...this.targets.values()].sort((a, b) => a.createdAt.localeCompare(b.createdAt)); }
  get(id: string): DeploymentTarget | undefined { return this.targets.get(id); }

  create(input: unknown): DeploymentTarget {
    const parsed = parseTarget(input, false) as CreateDeploymentTargetInput;
    const now = new Date().toISOString();
    const target: DeploymentTarget = { id: `target_${randomUUID()}`, ...parsed, createdAt: now, updatedAt: now };
    this.targets.set(target.id, target);
    this.persistence?.saveDeploymentTarget(target);
    return target;
  }

  update(id: string, input: unknown): DeploymentTarget | undefined {
    const existing = this.targets.get(id);
    if (!existing) return undefined;
    const parsed = parseTarget(input, true) as UpdateDeploymentTargetInput;
    const updated: DeploymentTarget = { ...existing, ...parsed, updatedAt: new Date().toISOString() };
    validateTargetConfig(updated.type, updated.config);
    this.targets.set(id, updated);
    this.persistence?.saveDeploymentTarget(updated);
    return updated;
  }

  delete(id: string): boolean {
    const deleted = this.targets.delete(id);
    if (deleted) this.persistence?.deleteDeploymentTarget(id);
    return deleted;
  }
}

export async function testDeploymentTarget(target: DeploymentTarget): Promise<DeploymentTargetTestResult> {
  if (target.type === "local_process") return { ok: true, status: "available", message: "Backend can launch local child processes." };
  return new Promise((resolve) => {
    const child = spawn("docker", ["--version"], { shell: false });
    let output = "";
    child.stdout.on("data", (chunk) => { output += String(chunk); });
    child.stderr.on("data", (chunk) => { output += String(chunk); });
    child.on("error", () => resolve({ ok: false, status: "unavailable", message: "Docker CLI is not available to this backend process." }));
    child.on("close", (code) => resolve(code === 0
      ? { ok: true, status: "available", message: output.trim() || "Docker CLI is available." }
      : { ok: false, status: "unavailable", message: output.trim() || `Docker CLI exited with code ${code}.` }));
  });
}

function parseTarget(input: unknown, partial: boolean): CreateDeploymentTargetInput | UpdateDeploymentTargetInput {
  const object = asRecord(input);
  const problems: { field: string; message: string }[] = [];
  const output: UpdateDeploymentTargetInput = {};
  if (!partial || "name" in object) {
    if (typeof object.name === "string" && object.name.trim()) output.name = object.name.trim(); else problems.push({ field: "name", message: "Target name is required." });
  }
  if (!partial || "type" in object) {
    if (object.type === "local_process" || object.type === "local_docker") output.type = object.type; else problems.push({ field: "type", message: "Target type must be local_process or local_docker." });
  }
  if ("config" in object) {
    output.config = asRecord(object.config);
  } else if (!partial) output.config = {};
  if ("secretRefs" in object) {
    output.secretRefs = Array.isArray(object.secretRefs) ? object.secretRefs.filter((item): item is string => typeof item === "string" && item.trim().length > 0) : [];
  } else if (!partial) output.secretRefs = [];
  if (output.config && Object.keys(output.config).some((key) => key.toLowerCase().includes("secret") || key.toLowerCase().includes("token") || key.toLowerCase().includes("password"))) problems.push({ field: "config", message: "Do not store secret values in target config; use secretRefs." });
  if (problems.length > 0) throw new RequestValidationError(problems);
  if (output.type && output.config) validateTargetConfig(output.type, output.config);
  return output as CreateDeploymentTargetInput | UpdateDeploymentTargetInput;
}

function validateTargetConfig(type: DeploymentTargetType, config: Record<string, unknown>): void {
  const allowed = type === "local_process" ? new Set(["cwd"]) : new Set(["image", "dockerfile"]);
  const extra = Object.keys(config).filter((key) => !allowed.has(key));
  if (extra.length > 0) throw new RequestValidationError(extra.map((key) => ({ field: `config.${key}`, message: "Unsupported config field for target type." })));
  if ("cwd" in config && typeof config.cwd !== "string") throw new RequestValidationError([{ field: "config.cwd", message: "cwd must be a string." }]);
  if ("image" in config && typeof config.image !== "string") throw new RequestValidationError([{ field: "config.image", message: "image must be a string." }]);
  if ("dockerfile" in config && typeof config.dockerfile !== "string") throw new RequestValidationError([{ field: "config.dockerfile", message: "dockerfile must be a string." }]);
}

function asRecord(value: unknown): Record<string, unknown> { return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {}; }
