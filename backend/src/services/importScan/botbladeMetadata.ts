// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import fs from "node:fs/promises";  // line 7: executes this statement as part of this file's behavior
import path from "node:path";  // line 8: executes this statement as part of this file's behavior
import { BLADE_PACKS } from "../../bladepacks/packs.js";  // line 9: executes this statement as part of this file's behavior
import type { ScanDetectionResult } from "./detector.js";  // line 10: executes this statement as part of this file's behavior

export async function writeBotbladeMetadata(workspacePath: string, detection: ScanDetectionResult, importSource?: { kind: string; url?: string }): Promise<string> {  // line 12: executes this statement as part of this file's behavior
  const selected = detection.recommendedPackId === "unknown"  // line 13: executes this statement as part of this file's behavior
    ? undefined  // line 14: executes this statement as part of this file's behavior
    : detection.matches.find((match) => match.id === detection.recommendedPackId);  // line 15: executes this statement as part of this file's behavior
  const selectedPack = BLADE_PACKS.find((pack) => pack.id === selected?.id);  // line 16: executes this statement as part of this file's behavior
  const packageManager = await detectPackageManager(workspacePath);  // line 17: executes this statement as part of this file's behavior
  const metadata = {  // line 18: executes this statement as part of this file's behavior
    schemaVersion: "0.1.0",  // line 19: executes this statement as part of this file's behavior
    generatedBy: "botblade",  // line 20: executes this statement as part of this file's behavior
    generatedAt: new Date().toISOString(),  // line 21: executes this statement as part of this file's behavior
    project: {  // line 22: executes this statement as part of this file's behavior
      name: workspacePath.replace(/\\/g, "/").split("/").filter(Boolean).pop() ?? "project",  // line 23: executes this statement as part of this file's behavior
      type: selected?.id ?? "unknown",  // line 24: executes this statement as part of this file's behavior
      root: ".",  // line 25: executes this statement as part of this file's behavior
      importSource: importSource ?? { kind: "local" }  // line 26: executes this statement as part of this file's behavior
    },  // line 27: executes this statement as part of this file's behavior
    runtime: {  // line 28: executes this statement as part of this file's behavior
      type: selectedPack?.runtime.type ?? "unknown",  // line 29: executes this statement as part of this file's behavior
      version: selectedPack?.runtime.versionRange?.replace(">=", "") ?? "unknown",  // line 30: executes this statement as part of this file's behavior
      packageManager  // line 31: executes this statement as part of this file's behavior
    },  // line 32: executes this statement as part of this file's behavior
    bladePack: {  // line 33: executes this statement as part of this file's behavior
      selected: selected?.id ?? "unknown",  // line 34: executes this statement as part of this file's behavior
      version: selectedPack?.version ?? "0.1.0",  // line 35: executes this statement as part of this file's behavior
      detected: detection.matches.map((m) => ({ id: m.id, score: m.score, confidence: m.confidence }))  // line 36: executes this statement as part of this file's behavior
    },  // line 37: executes this statement as part of this file's behavior
    commands: selected?.commands ?? {},  // line 38: executes this statement as part of this file's behavior
    secrets: (selectedPack?.secrets ?? []).map((secret) => ({ name: secret.name, required: secret.required, configured: false, storage: ".env.local" })),  // line 39: executes this statement as part of this file's behavior
    panels: selectedPack?.panels ?? ["projectMap", "editor", "logs", "secrets", "git", "health"],  // line 40: executes this statement as part of this file's behavior
    healthSignals: { lastBuildPassed: null, lastTestsPassed: null, lastRuntimeHealthy: null, lastCrashAt: null, gitClean: null, dependencyState: "unknown" }  // line 41: executes this statement as part of this file's behavior
  };  // line 42: executes this statement as part of this file's behavior
  await fs.mkdir(workspacePath, { recursive: true });  // line 43: executes this statement as part of this file's behavior
  const target = path.join(workspacePath, "botblade.json");  // line 44: executes this statement as part of this file's behavior
  await fs.writeFile(target, JSON.stringify(metadata, null, 2) + "\n", "utf8");  // line 45: executes this statement as part of this file's behavior
  return target;  // line 46: executes this statement as part of this file's behavior
}  // line 47: executes this statement as part of this file's behavior

async function detectPackageManager(workspacePath: string): Promise<string> {  // line 49: executes this statement as part of this file's behavior
  const checks: Array<[string, string]> = [["pnpm-lock.yaml", "pnpm"], ["yarn.lock", "yarn"], ["package-lock.json", "npm"], ["requirements.txt", "pip"]];  // line 50: executes this statement as part of this file's behavior
  for (const [file, pm] of checks) {  // line 51: executes this statement as part of this file's behavior
    try { await fs.access(`${workspacePath}/${file}`); return pm; } catch {}  // line 52: executes this statement as part of this file's behavior
  }  // line 53: executes this statement as part of this file's behavior
  return "unknown";  // line 54: executes this statement as part of this file's behavior
}  // line 55: executes this statement as part of this file's behavior
