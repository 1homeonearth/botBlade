import type { BotProfileCommandPlan, BotProfileRepairCard, BotProfileScriptProfile } from "../../models/botProfile.js";
import type { DetectionMatch, ScanSecretRequirement } from "./detector.js";
import type { GitStatusMetadata } from "../gitStatusService.js";

export type RepairCardEvidence = {
  recommendedPackId: string;
  matches: DetectionMatch[];
  commandPlan: BotProfileCommandPlan;
  scriptProfiles: BotProfileScriptProfile[];
  secretRequirements: { required: ScanSecretRequirement[]; optional: ScanSecretRequirement[] };
  files: Set<string>;
  fileExtensions: Set<string>;
  packageJson: Record<string, unknown> | null;
  packageScripts: Set<string>;
  workflowJson: Record<string, unknown> | null;
  workflowJsonExists: boolean;
  workflowJsonParseError: boolean;
  detectedLanguages: string[];
  git: GitStatusMetadata;
};

const NODE_COMMAND_SCRIPTS = ["start", "build", "test"] as const;
const COMMAND_PROFILE_ACTIONS = ["start", "build", "test"] as const;

export function generateRepairCards(evidence: RepairCardEvidence): BotProfileRepairCard[] {
  return dedupeRepairCards([
    ...unknownProjectProfileCards(evidence),
    ...lowDetectionConfidenceCards(evidence),
    ...missingNodeScriptCards(evidence),
    ...missingPythonManifestCards(evidence),
    ...missingCommandProfileCards(evidence),
    ...missingRequiredSecretCards(evidence),
    ...invalidN8nWorkflowCards(evidence),
    ...gitMetadataUnavailableCards(evidence.git),
  ]);
}

export function appendGitMetadataUnavailableRepairCard(
  cards: Array<BotProfileRepairCard | Record<string, unknown>>,
  git: GitStatusMetadata | Record<string, unknown>,
): BotProfileRepairCard[] {
  return dedupeRepairCards([
    ...cards.map(toRepairCard).filter((card): card is BotProfileRepairCard => Boolean(card)),
    ...gitMetadataUnavailableCards(normalizeGitMetadata(git)),
  ]);
}

function unknownProjectProfileCards(evidence: RepairCardEvidence): BotProfileRepairCard[] {
  if (evidence.recommendedPackId !== "unknown") return [];
  return [{
    title: "Unknown project profile",
    evidence: evidence.matches.length === 0 ? "No Blade Pack detector reached the project-profile threshold." : "Detector evidence was too weak to select a Blade Pack.",
    safeAction: "Preview the detected files, choose a Blade Pack or configure command metadata manually, then rerun the static scan.",
  }];
}

function lowDetectionConfidenceCards(evidence: RepairCardEvidence): BotProfileRepairCard[] {
  const top = evidence.matches[0];
  if (!top || top.score >= 60) return [];
  return [{
    title: "Low detection confidence",
    evidence: `${top.name} matched with ${top.confidence} confidence (${top.score}/100) from ${top.matchedEvidence.join(", ") || "limited evidence"}.`,
    safeAction: "Preview the matched evidence and confirm or override the project profile before running install, build, or start commands.",
  }];
}

function missingNodeScriptCards(evidence: RepairCardEvidence): BotProfileRepairCard[] {
  if (!isNodeProject(evidence) || !evidence.packageJson) return [];
  const missingScripts = NODE_COMMAND_SCRIPTS.filter((script) => !evidence.packageScripts.has(script));
  if (missingScripts.length === 0) return [];
  return [{
    title: "Missing package.json command scripts",
    evidence: `package.json is present, but these common scripts are missing: ${missingScripts.join(", ")}.`,
    safeAction: "Preview package.json and add reviewed script metadata for start, build, or test only after confirming the correct entrypoints.",
  }];
}

function missingPythonManifestCards(evidence: RepairCardEvidence): BotProfileRepairCard[] {
  if (!isPythonProject(evidence)) return [];
  if (evidence.files.has("requirements.txt") || evidence.files.has("pyproject.toml")) return [];
  return [{
    title: "Missing Python dependency manifest",
    evidence: "Python source files were detected without requirements.txt or pyproject.toml at the project root.",
    safeAction: "Preview the imports and create requirements.txt or pyproject.toml metadata after confirming the dependency list.",
  }];
}

function missingCommandProfileCards(evidence: RepairCardEvidence): BotProfileRepairCard[] {
  const missingActions = COMMAND_PROFILE_ACTIONS.filter((action) => !hasCommandProfile(evidence.scriptProfiles, action));
  if (missingActions.length !== COMMAND_PROFILE_ACTIONS.length) return [];
  return [{
    title: "No start/build/test command profile found",
    evidence: "The static scan did not find package scripts, Make targets, entrypoints, or metadata profiles for start, build, or test.",
    safeAction: "Create preview-only command metadata for the reviewed start, build, and test actions; keep execution disabled until the user confirms in the next repair phase.",
  }];
}

function missingRequiredSecretCards(evidence: RepairCardEvidence): BotProfileRepairCard[] {
  const missing = evidence.secretRequirements.required.filter((secret) => !secret.configured);
  if (missing.length === 0) return [];
  return [{
    title: "Missing required secret metadata",
    evidence: `Required secret references are not configured: ${missing.map((secret) => secret.name).join(", ")}.`,
    safeAction: "Create BotBlade secret references for the missing names without storing or displaying secret values in botblade.json.",
  }];
}

function invalidN8nWorkflowCards(evidence: RepairCardEvidence): BotProfileRepairCard[] {
  if (!evidence.workflowJsonExists) return [];
  if (isSupportedN8nWorkflowShape(evidence.workflowJson)) return [];
  return [{
    title: "Invalid or unsupported n8n workflow shape",
    evidence: evidence.workflowJsonParseError
      ? "workflow.json could not be parsed as JSON."
      : "workflow.json must contain a nodes array and a connections object for BotBlade workflow metadata import.",
    safeAction: "Preview and fix workflow.json shape before importing credentials or creating workflow command metadata.",
  }];
}

function normalizeGitMetadata(git: GitStatusMetadata | Record<string, unknown>): GitStatusMetadata {
  return {
    branch: typeof git.branch === "string" ? git.branch : null,
    status: git.status === "clean" || git.status === "dirty" || git.status === "unknown" ? git.status : "unknown",
    dirtyFileCount: typeof git.dirtyFileCount === "number" ? git.dirtyFileCount : undefined,
    remotes: Array.isArray(git.remotes) ? git.remotes as GitStatusMetadata["remotes"] : [],
  };
}

function gitMetadataUnavailableCards(git: GitStatusMetadata): BotProfileRepairCard[] {
  if (git.status !== "unknown") return [];
  return [{
    title: "Git metadata unavailable",
    evidence: "The workspace is not a readable Git repository root or Git status could not be collected.",
    safeAction: "Initialize Git in the workspace or verify repository access, then refresh the profile.",
  }];
}

function isNodeProject(evidence: RepairCardEvidence): boolean {
  return Boolean(
    evidence.packageJson ||
      evidence.detectedLanguages.includes("javascript") ||
      evidence.detectedLanguages.includes("typescript") ||
      evidence.matches.some((match) => match.runtime.type === "node"),
  );
}

function isPythonProject(evidence: RepairCardEvidence): boolean {
  return Boolean(
    evidence.detectedLanguages.includes("python") ||
      evidence.fileExtensions.has(".py") ||
      evidence.matches.some((match) => match.runtime.type === "python"),
  );
}

function hasCommandProfile(profiles: BotProfileScriptProfile[], action: typeof COMMAND_PROFILE_ACTIONS[number]): boolean {
  return profiles.some((profile) =>
    profile.tags.includes(action) ||
    profile.id.toLowerCase().endsWith(`:${action}`) ||
    profile.name.toLowerCase().includes(` ${action}`) ||
    profile.name.toLowerCase().endsWith(`: ${action}`),
  );
}

function isSupportedN8nWorkflowShape(workflowJson: Record<string, unknown> | null): boolean {
  if (!workflowJson) return false;
  return Array.isArray(workflowJson.nodes) && Boolean(workflowJson.connections) && typeof workflowJson.connections === "object" && !Array.isArray(workflowJson.connections);
}

function toRepairCard(card: BotProfileRepairCard | Record<string, unknown>): BotProfileRepairCard | null {
  if (typeof card.title !== "string" || typeof card.safeAction !== "string") return null;
  return {
    title: card.title,
    evidence: typeof card.evidence === "string" ? card.evidence : undefined,
    safeAction: card.safeAction,
  };
}

function dedupeRepairCards(cards: BotProfileRepairCard[]): BotProfileRepairCard[] {
  const seen = new Set<string>();
  const deduped: BotProfileRepairCard[] = [];
  for (const card of cards) {
    const key = `${card.title}\n${card.evidence ?? ""}\n${card.safeAction}`;
    if (seen.has(key)) continue;
    seen.add(key);
    deduped.push(card);
  }
  return deduped;
}
