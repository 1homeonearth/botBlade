import fs from "node:fs/promises";
import path from "node:path";
import { BLADE_PACKS } from "../../bladepacks/packs.js";
import {
  BOT_PROFILE_SCHEMA_VERSION,
  type BotProfile,
  type BotProfileCommandPlan,
  type BotProfileScriptProfile,
  type ScriptProfileRuntime,
} from "../../models/botProfile.js";
import type { ScanDetectionResult } from "./detector.js";

export async function writeBotbladeMetadata(
  workspacePath: string,
  detection: ScanDetectionResult,
  importSource?: { kind: string; url?: string },
): Promise<string> {
  const selected =
    detection.recommendedPackId === "unknown"
      ? undefined
      : detection.matches.find(
          (match) => match.id === detection.recommendedPackId,
        );
  const selectedPack = BLADE_PACKS.find((pack) => pack.id === selected?.id);
  const generatedAt = new Date().toISOString();
  const secretRefs = [
    ...detection.secretRequirements.required,
    ...detection.secretRequirements.optional,
  ].map((secret) => secret.name);
  const packageManager = await detectPackageManager(workspacePath);
  const metadata: BotProfile = {
    schemaVersion: BOT_PROFILE_SCHEMA_VERSION,
    generatedBy: "botblade",
    generatedAt,
    project: {
      name:
        workspacePath.replace(/\\/g, "/").split("/").filter(Boolean).pop() ??
        "project",
      type: selected?.id ?? "unknown",
      root: ".",
      importSource: importSource ?? { kind: "local" },
    },
    runtime: {
      type: selectedPack?.runtime.type ?? "unknown",
      version:
        selectedPack?.runtime.versionRange?.replace(">=", "") ?? "unknown",
      packageManager,
      detectedLanguages: detection.detectedLanguages,
      detectedFrameworks: detection.detectedFrameworks,
    },
    bladePack: {
      selected: selected?.id ?? "unknown",
      version: selectedPack?.version ?? "0.1.0",
      detected: detection.matches.map((m) => ({
        id: m.id,
        name: m.name,
        score: m.score,
        confidence: m.confidence,
        matchedEvidence: m.matchedEvidence,
      })),
    },
    commandPlan: detection.commandPlan,
    scriptProfiles: await buildScriptProfiles(
      workspacePath,
      detection.commandPlan,
      selectedPack?.runtime.type,
      packageManager,
      secretRefs,
      generatedAt,
    ),
    secrets: {
      required: detection.secretRequirements.required,
      optional: detection.secretRequirements.optional,
    },
    permissions: detection.permissions,
    capabilities: detection.capabilities,
    importantFiles: detection.importantFiles,
    warnings: [...detection.diagnostics.warnings, ...detection.fallbackNotes],
    repairCards: detection.diagnostics.repairCards,
    git: detection.git,
  };
  await fs.mkdir(workspacePath, { recursive: true });
  const target = path.join(workspacePath, "botblade.json");
  await fs.writeFile(target, JSON.stringify(metadata, null, 2) + "\n", "utf8");
  return target;
}

async function detectPackageManager(
  workspacePath: string,
): Promise<"npm" | "pnpm" | "yarn" | "pip" | "unknown"> {
  const checks: Array<[string, "npm" | "pnpm" | "yarn" | "pip"]> = [
    ["pnpm-lock.yaml", "pnpm"],
    ["yarn.lock", "yarn"],
    ["package-lock.json", "npm"],
    ["requirements.txt", "pip"],
  ];
  for (const [file, pm] of checks) {
    try {
      await fs.access(`${workspacePath}/${file}`);
      return pm;
    } catch {}
  }
  return "unknown";
}

async function buildScriptProfiles(
  workspacePath: string,
  commandPlan: BotProfileCommandPlan,
  packRuntime: string | undefined,
  packageManager: "npm" | "pnpm" | "yarn" | "pip" | "unknown",
  secretRefs: string[],
  timestamp: string,
): Promise<BotProfileScriptProfile[]> {
  const profiles: BotProfileScriptProfile[] = [];
  const runtime = mapScriptRuntime(packRuntime);

  for (const [action, commands] of Object.entries(commandPlan) as Array<
    [keyof BotProfileCommandPlan, string[]]
  >) {
    for (const [index, command] of commands.entries()) {
      profiles.push({
        id: `blade-pack:${action}${commands.length > 1 ? `:${index + 1}` : ""}`,
        name: `Blade Pack ${action}`,
        description: "Command recommended by the detected Blade Pack.",
        source: "blade_pack",
        runtime,
        command,
        workingDirectory: ".",
        envRefs: [],
        secretRefs,
        timeoutSeconds: defaultTimeoutSeconds(action),
        requiresConfirmation: ["deploy", "stop", "restart"].includes(action),
        tags: ["blade_pack", action],
        createdAt: timestamp,
        updatedAt: timestamp,
      });
    }
  }

  const packageScripts = await readPackageScripts(workspacePath);
  const runPrefix = packageScriptRunPrefix(packageManager);
  for (const scriptName of packageScripts) {
    profiles.push({
      id: `package-json:${scriptName}`,
      name: `package.json ${scriptName}`,
      description: `Detected package.json script named ${scriptName}.`,
      source: "package_json",
      runtime: "node",
      command: `${runPrefix} ${quotePackageScriptName(scriptName)}`,
      workingDirectory: ".",
      envRefs: [],
      secretRefs,
      timeoutSeconds: defaultTimeoutSeconds(scriptName),
      requiresConfirmation: ["deploy", "publish", "release"].some((keyword) =>
        scriptName.toLowerCase().includes(keyword),
      ),
      tags: ["package_json", scriptName],
      createdAt: timestamp,
      updatedAt: timestamp,
    });
  }

  return profiles;
}

function mapScriptRuntime(runtime: string | undefined): ScriptProfileRuntime {
  if (runtime === "node" || runtime === "python" || runtime === "workflow")
    return runtime;
  return "custom";
}

function defaultTimeoutSeconds(action: string): number {
  if (["install", "build", "test", "validate"].includes(action)) return 600;
  if (
    ["deploy", "publish", "release"].some((keyword) =>
      action.toLowerCase().includes(keyword),
    )
  )
    return 900;
  return 300;
}

async function readPackageScripts(workspacePath: string): Promise<string[]> {
  try {
    const parsed = JSON.parse(
      await fs.readFile(path.join(workspacePath, "package.json"), "utf8"),
    ) as { scripts?: unknown };
    if (
      !parsed.scripts ||
      typeof parsed.scripts !== "object" ||
      Array.isArray(parsed.scripts)
    )
      return [];
    return Object.entries(parsed.scripts)
      .filter(([, command]) => typeof command === "string")
      .map(([name]) => name)
      .sort((a, b) => a.localeCompare(b));
  } catch {
    return [];
  }
}

function packageScriptRunPrefix(
  packageManager: "npm" | "pnpm" | "yarn" | "pip" | "unknown",
): string {
  if (packageManager === "pnpm") return "pnpm run";
  if (packageManager === "yarn") return "yarn run";
  return "npm run";
}

function quotePackageScriptName(scriptName: string): string {
  if (/^[A-Za-z0-9:_-]+$/.test(scriptName)) return scriptName;
  return JSON.stringify(scriptName);
}
