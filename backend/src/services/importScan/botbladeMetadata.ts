import fs from "node:fs/promises";
import path from "node:path";
import { BLADE_PACKS } from "../../bladepacks/packs.js";
import {
  BOT_PROFILE_SCHEMA_VERSION,
  type BotProfile,
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
  const scriptProfiles = detection.scriptProfiles;
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
      packageManager: detection.packageManager,
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
    scriptProfiles,
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
