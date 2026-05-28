import fs from "node:fs/promises";
import path from "node:path";
import { BLADE_PACKS } from "../../bladepacks/packs.js";
import { BOT_PROFILE_SCHEMA_VERSION, type BotProfile, type BotProfileScriptProfile, type ScriptProfileRuntime } from "../../models/botProfile.js";
import type { BladePackRuntime } from "../../bladepacks/schema.js";
import type { ScanDetectionResult } from "./detector.js";

export async function writeBotbladeMetadata(workspacePath: string, detection: ScanDetectionResult, importSource?: { kind: string; url?: string }): Promise<string> {
  const selected = detection.recommendedPackId === "unknown" ? undefined : detection.matches.find((match) => match.id === detection.recommendedPackId);
  const selectedPack = BLADE_PACKS.find((pack) => pack.id === selected?.id);
  const generatedAt = new Date().toISOString();
  const secrets = { required: detection.secretRequirements.required, optional: detection.secretRequirements.optional };
  const metadata: BotProfile = {
    schemaVersion: BOT_PROFILE_SCHEMA_VERSION,
    generatedBy: "botblade",
    generatedAt,
    project: { name: workspacePath.replace(/\\/g, "/").split("/").filter(Boolean).pop() ?? "project", type: selected?.id ?? "unknown", root: ".", importSource: importSource ?? { kind: "local" } },
    runtime: {
      type: selectedPack?.runtime.type ?? "unknown",
      version: selectedPack?.runtime.versionRange?.replace(">=", "") ?? "unknown",
      packageManager: await detectPackageManager(workspacePath),
      detectedLanguages: detection.detectedLanguages,
      detectedFrameworks: detection.detectedFrameworks
    },
    bladePack: {
      selected: selected?.id ?? "unknown",
      version: selectedPack?.version ?? "0.1.0",
      detected: detection.matches.map((m) => ({ id: m.id, name: m.name, score: m.score, confidence: m.confidence, matchedEvidence: m.matchedEvidence }))
    },
    commandPlan: detection.commandPlan,
    scriptProfiles: await detectScriptProfiles(workspacePath, selectedPack?.runtime, [...secrets.required, ...secrets.optional].map((secret) => secret.name), generatedAt),
    secrets,
    permissions: detection.permissions,
    capabilities: detection.capabilities,
    importantFiles: detection.importantFiles,
    warnings: [...detection.diagnostics.warnings, ...detection.fallbackNotes],
    repairCards: detection.diagnostics.repairCards,
    git: detection.git
  };
  await fs.mkdir(workspacePath, { recursive: true });
  const target = path.join(workspacePath, "botblade.json");
  await fs.writeFile(target, JSON.stringify(metadata, null, 2) + "\n", "utf8");
  return target;
}

async function detectPackageManager(workspacePath: string): Promise<"npm" | "pnpm" | "yarn" | "pip" | "unknown"> {
  const checks: Array<[string, "npm" | "pnpm" | "yarn" | "pip"]> = [["pnpm-lock.yaml", "pnpm"], ["yarn.lock", "yarn"], ["package-lock.json", "npm"], ["requirements.txt", "pip"]];
  for (const [file, pm] of checks) {
    try { await fs.access(`${workspacePath}/${file}`); return pm; } catch {}
  }
  return "unknown";
}

async function detectScriptProfiles(workspacePath: string, runtime: BladePackRuntime | undefined, knownSecretRefs: string[], timestamp: string): Promise<BotProfileScriptProfile[]> {
  const packageJson = await readPackageJson(workspacePath);
  const scripts = packageJson?.scripts;
  if (!isRecord(scripts)) return [];

  return Object.entries(scripts)
    .filter((entry): entry is [string, string] => typeof entry[1] === "string" && entry[1].trim().length > 0)
    .map(([name, command]) => {
      const envRefs = extractEnvRefs(command);
      const secretRefs = unique([...knownSecretRefs.filter((secret) => envRefs.includes(secret)), ...envRefs.filter(isSecretLikeRef)]);
      return {
        id: `package-json-${toProfileId(name)}`,
        name,
        description: `Detected package.json script: ${name}`,
        source: "package_json",
        runtime: toScriptProfileRuntime(runtime),
        command: redactSecretAssignments(command),
        workingDirectory: ".",
        envRefs,
        secretRefs,
        timeoutSeconds: defaultTimeoutSeconds(name),
        requiresConfirmation: requiresConfirmation(name),
        tags: unique(["package_json", name]),
        createdAt: timestamp,
        updatedAt: timestamp
      };
    });
}

async function readPackageJson(workspacePath: string): Promise<Record<string, unknown> | null> {
  try {
    return JSON.parse(await fs.readFile(path.join(workspacePath, "package.json"), "utf8")) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function toScriptProfileRuntime(runtime: BladePackRuntime | undefined): ScriptProfileRuntime {
  if (runtime?.type === "node" || runtime?.type === "python" || runtime?.type === "workflow") return runtime.type;
  return "custom";
}

function toProfileId(name: string): string {
  return name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "") || "script";
}

function defaultTimeoutSeconds(name: string): number {
  return ["start", "dev", "serve"].includes(name) ? 0 : 300;
}

function requiresConfirmation(name: string): boolean {
  return ["deploy", "publish", "release"].some((unsafe) => name.toLowerCase().includes(unsafe));
}

function extractEnvRefs(command: string): string[] {
  const refs = new Set<string>();
  for (const match of command.matchAll(/(?:^|\s|&&|\|\|)([A-Za-z_][A-Za-z0-9_]*)=/g)) refs.add(match[1]);
  for (const match of command.matchAll(/\$\{?([A-Za-z_][A-Za-z0-9_]*)\}?/g)) refs.add(match[1]);
  for (const match of command.matchAll(/%([A-Za-z_][A-Za-z0-9_]*)%/g)) refs.add(match[1]);
  return [...refs].sort();
}

function isSecretLikeRef(ref: string): boolean {
  return /(?:TOKEN|SECRET|PASSWORD|PASS|KEY|CREDENTIAL|PRIVATE|CLIENT_ID)/i.test(ref);
}

function redactSecretAssignments(command: string): string {
  return command.replace(/\b([A-Za-z_][A-Za-z0-9_]*)(\s*=\s*)(["']?)([^\s"']+)(["']?)/g, (match, key: string, separator: string, openQuote: string, _value: string, closeQuote: string) => {
    if (!isSecretLikeRef(key)) return match;
    return `${key}${separator}${openQuote}[redacted]${closeQuote || openQuote}`;
  });
}

function unique(values: string[]): string[] {
  return [...new Set(values)].sort();
}
