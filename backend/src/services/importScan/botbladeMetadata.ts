import fs from "node:fs/promises";
import path from "node:path";
import { BLADE_PACKS } from "../../bladepacks/packs.js";
import type { ScanDetectionResult } from "./detector.js";

export async function writeBotbladeMetadata(workspacePath: string, detection: ScanDetectionResult, importSource?: { kind: string; url?: string }): Promise<string> {
  const selected = detection.recommendedPackId === "unknown"
    ? undefined
    : detection.matches.find((match) => match.id === detection.recommendedPackId);
  const selectedPack = BLADE_PACKS.find((pack) => pack.id === selected?.id);
  const packageManager = await detectPackageManager(workspacePath);
  const metadata = {
    schemaVersion: "0.1.0",
    generatedBy: "botblade",
    generatedAt: new Date().toISOString(),
    project: {
      name: workspacePath.replace(/\\/g, "/").split("/").filter(Boolean).pop() ?? "project",
      type: selected?.id ?? "unknown",
      root: ".",
      importSource: importSource ?? { kind: "local" }
    },
    runtime: {
      type: selectedPack?.runtime.type ?? "unknown",
      version: selectedPack?.runtime.versionRange?.replace(">=", "") ?? "unknown",
      packageManager
    },
    bladePack: {
      selected: selected?.id ?? "unknown",
      version: selectedPack?.version ?? "0.1.0",
      detected: detection.matches.map((m) => ({ id: m.id, score: m.score, confidence: m.confidence }))
    },
    commands: selected?.commands ?? {},
    secrets: (selectedPack?.secrets ?? []).map((secret) => ({ name: secret.name, required: secret.required, configured: false, storage: ".env.local" })),
    panels: selectedPack?.panels ?? ["projectMap", "editor", "logs", "secrets", "git", "health"],
    healthSignals: { lastBuildPassed: null, lastTestsPassed: null, lastRuntimeHealthy: null, lastCrashAt: null, gitClean: null, dependencyState: "unknown" }
  };
  await fs.mkdir(workspacePath, { recursive: true });
  const target = path.join(workspacePath, "botblade.json");
  await fs.writeFile(target, JSON.stringify(metadata, null, 2) + "\n", "utf8");
  return target;
}

async function detectPackageManager(workspacePath: string): Promise<string> {
  const checks: Array<[string, string]> = [["pnpm-lock.yaml", "pnpm"], ["yarn.lock", "yarn"], ["package-lock.json", "npm"], ["requirements.txt", "pip"]];
  for (const [file, pm] of checks) {
    try { await fs.access(`${workspacePath}/${file}`); return pm; } catch {}
  }
  return "unknown";
}
