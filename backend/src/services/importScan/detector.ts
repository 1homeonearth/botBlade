import fs from "node:fs/promises";
import * as path from "node:path";
import { BLADE_PACKS } from "../../bladepacks/packs.js";
import type { BladePack, BladePackDetector, BladePackRuntime } from "../../bladepacks/schema.js";
import type { BotProfileCommandPlan, BotProfileScriptProfile, PackageManager } from "../../models/botProfile.js";
import { detectScriptProfiles } from "../scriptProfiles/scriptProfileDetector.js";

export type DetectorConfidence = "weak" | "possible" | "likely" | "high";

export type DetectionMatch = {
  id: string;
  name: string;
  score: number;
  confidence: DetectorConfidence;
  matchedEvidence: string[];
  runtime: BladePackRuntime;
  commands: BladePack["commands"];
  requiredSecrets: string[];
};

export type ScanSecretRequirement = { name: string; required: boolean; configured: boolean };

export type ScanDiagnostics = {
  warnings: string[];
  repairCards: Array<{ title: string; evidence?: string; safeAction: string }>;
};

export type ScanDetectionResult = {
  workspacePath: string;
  recommendedPackId: string;
  matches: DetectionMatch[];
  diagnostics: ScanDiagnostics;
  fallbackNotes: string[];
  commandPlan: BotProfileCommandPlan;
  scriptProfiles: BotProfileScriptProfile[];
  secretRequirements: { required: ScanSecretRequirement[]; optional: ScanSecretRequirement[] };
  importantFiles: string[];
  detectedLanguages: string[];
  detectedFrameworks: string[];
  permissions: string[];
  capabilities: string[];
  git: { branch: string | null; status: "clean" | "dirty" | "unknown"; remotes: Array<{ name: string; url: string | null }> };
  packageManager: PackageManager;
};

export async function scanWorkspaceForBladePacks(workspacePath: string): Promise<ScanDetectionResult> {
  const ctx = await buildContext(workspacePath);
  const matches: DetectionMatch[] = [];
  for (const pack of BLADE_PACKS) {
    let score = 0;
    const evidence: string[] = [];
    for (const detector of pack.detectors) {
      const matched = matchDetector(detector, ctx);
      if (matched) {
        score += detector.weight;
        evidence.push(matched);
      }
    }
    if (score > 0) {
      matches.push({
        id: pack.id,
        name: pack.name,
        score: Math.min(score, 100),
        confidence: scoreToConfidence(score),
        matchedEvidence: evidence,
        runtime: pack.runtime,
        commands: pack.commands,
        requiredSecrets: pack.secrets.filter((s) => s.required).map((s) => s.name)
      });
    }
  }
  matches.sort((a, b) => b.score - a.score);
  const top = matches[0];
  const selectedPack = BLADE_PACKS.find((pack) => pack.id === top?.id);
  const commandPlan = {
    install: selectedPack?.commands.install ? [selectedPack.commands.install] : [],
    build: selectedPack?.commands.build ? [selectedPack.commands.build] : [],
    test: selectedPack?.commands.test ? [selectedPack.commands.test] : [],
    validate: selectedPack ? (selectedPack.commands.validate ? [selectedPack.commands.validate] : []) : ["botblade validate"],
    start: selectedPack?.commands.start ? [selectedPack.commands.start] : selectedPack?.commands.run ? [selectedPack.commands.run] : [],
    stop: selectedPack ? (selectedPack.commands.stop ? [selectedPack.commands.stop] : []) : ["botblade runtime stop"],
    restart: selectedPack ? (selectedPack.commands.restart ? [selectedPack.commands.restart] : []) : ["botblade runtime restart"],
    deploy: selectedPack?.commands.deploy ? [selectedPack.commands.deploy] : []
  };
  const required = (selectedPack?.secrets ?? []).filter((s) => s.required).map((s) => ({ name: s.name, required: true, configured: false }));
  const optional = (selectedPack?.secrets ?? []).filter((s) => !s.required).map((s) => ({ name: s.name, required: false, configured: false }));
  const { packageManager, profiles } = await detectScriptProfiles(workspacePath, {
    commandPlan,
    selectedPack,
    secretRefs: [...required, ...optional].map((secret) => secret.name),
  });
  const scriptProfiles = dedupeScriptProfiles(profiles);
  return {
    workspacePath,
    recommendedPackId: top && top.score >= 40 ? top.id : "unknown",
    matches,
    diagnostics: {
      warnings: top && top.score < 60 ? ["Detection confidence is below likely threshold."] : [],
      repairCards: top ? [] : [{ title: "Unknown project profile", safeAction: "Configure install/build/test/start commands manually and rerun scan." }]
    },
    fallbackNotes: top ? [] : ["No strong Blade Pack signals found. Open project in editor and configure commands manually."],
    commandPlan,
    scriptProfiles,
    secretRequirements: { required, optional },
    importantFiles: [...ctx.files].filter((f) => /(package\.json|requirements\.txt|pyproject\.toml|workflow\.json|README\.md|Makefile|Taskfile\.yml|justfile|\.shellcheckrc)$/i.test(f) || f.startsWith("scripts/")).slice(0, 20),
    detectedLanguages: detectedLanguagesForPack(top?.id),
    detectedFrameworks: top ? [top.name] : [],
    permissions: ["read_workspace", "write_workspace"],
    capabilities: ["scan", "diagnose", "run_commands"],
    git: { branch: null, status: "unknown", remotes: [] },
    packageManager
  };
}

function dedupeScriptProfiles(profiles: BotProfileScriptProfile[]): BotProfileScriptProfile[] {
  const seen = new Set<string>();
  return profiles.filter((profile) => {
    const key = scriptProfileDedupeKey(profile);
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function scriptProfileDedupeKey(profile: BotProfileScriptProfile): string {
  return JSON.stringify({
    command: profile.command.map((token) => token.trim()).filter(Boolean),
    workingDirectory: normalizeWorkingDirectory(profile.workingDirectory),
    source: profile.source,
  });
}

function normalizeWorkingDirectory(workingDirectory: string): string {
  const normalized = workingDirectory.trim().replace(/\\/g, "/").replace(/\/+$/g, "");
  return normalized === "" ? "." : normalized;
}

function scoreToConfidence(score: number): DetectorConfidence { if (score >= 80) return "high"; if (score >= 60) return "likely"; if (score >= 40) return "possible"; return "weak"; }

function detectedLanguagesForPack(packId: string | undefined): string[] {
  if (!packId) return ["javascript", "typescript"];
  if (packId.includes("python")) return ["python"];
  if (packId === "generic-shell") return ["shell"];
  if (packId.includes("workflow")) return ["workflow"];
  return ["javascript", "typescript"];
}

function isStaticSourceFile(relativePath: string): boolean {
  if (/\.(ts|js|tsx|jsx|py|json|sh|bash)$/i.test(relativePath)) return true;
  if (relativePath.startsWith("scripts/") && !(relativePath.split("/").pop() ?? "").includes(".")) return true;
  return false;
}

type ScanContext = {
  root: string; files: Set<string>; directories: Set<string>; packageJson: Record<string, unknown> | null; packageDeps: Set<string>; packageScripts: Set<string>; envText: string; sourceText: string; workflowJson: Record<string, unknown> | null; packageManager: "npm" | "pnpm" | "yarn" | "pip" | "unknown";
};

async function buildContext(workspacePath: string): Promise<ScanContext> {
  const workspaceExists = await pathExists(workspacePath);
  if (!workspaceExists) return { root: workspacePath, files: new Set(), directories: new Set(), packageJson: null, packageDeps: new Set(), packageScripts: new Set(), envText: "", sourceText: "", workflowJson: null, packageManager: "unknown" };
  const files = new Set<string>(); const directories = new Set<string>();
  await walk(workspacePath, workspacePath, files, directories);
  const packageJson = await readJsonIfExists(path.join(workspacePath, "package.json"));
  const packageDeps = new Set<string>([...Object.keys((packageJson?.dependencies as Record<string, unknown>) ?? {}), ...Object.keys((packageJson?.devDependencies as Record<string, unknown>) ?? {})]);
  const packageScripts = new Set<string>(Object.keys((packageJson?.scripts as Record<string, unknown>) ?? {}));
  const envText = await readTextIfExists(path.join(workspacePath, ".env.example")) + "\n" + await readTextIfExists(path.join(workspacePath, ".env.local"));
  const sourceFiles = [...files].filter(isStaticSourceFile).slice(0, 80);
  const sourceText = (await Promise.all(sourceFiles.map((f) => readTextIfExists(path.join(workspacePath, f))))).join("\n");
  const workflowJson = await readJsonIfExists(path.join(workspacePath, "workflow.json"));
  const packageManager = files.has("pnpm-lock.yaml") ? "pnpm" : files.has("yarn.lock") ? "yarn" : files.has("package-lock.json") ? "npm" : files.has("requirements.txt") ? "pip" : "unknown";
  return { root: workspacePath, files, directories, packageJson, packageDeps, packageScripts, envText, sourceText, workflowJson, packageManager };
}

function matchDetector(detector: BladePackDetector, ctx: ScanContext): string | null { if (detector.kind === "packageDependency") return ctx.packageDeps.has(detector.name) ? `dependency:${detector.name}` : null; if (detector.kind === "packageScript") return ctx.packageScripts.has(detector.name) ? `script:${detector.name}` : null; if (detector.kind === "sourceImport") return new RegExp(detector.pattern, "i").test(ctx.sourceText) ? `pattern:${detector.pattern}` : null; if (detector.kind === "envKey") return new RegExp(detector.pattern, "i").test(ctx.envText + "\n" + ctx.sourceText) ? `env:${detector.pattern}` : null; if (detector.kind === "fileExists" || detector.kind === "knownFilename") return ctx.files.has(detector.path) ? `file:${detector.path}` : null; if (detector.kind === "knownDirectory") return ctx.directories.has(detector.path) ? `dir:${detector.path}` : null; if (detector.kind === "jsonShape") { if (detector.file !== "workflow.json" || !ctx.workflowJson) return null; return detector.keys.every((k) => k in ctx.workflowJson!) ? `json-shape:${detector.keys.join(",")}` : null; } return null; }
async function walk(root: string, current: string, files: Set<string>, dirs: Set<string>): Promise<void> { const entries = await fs.readdir(current, { withFileTypes: true }); for (const entry of entries) { if (["node_modules", "dist", ".git"].includes(entry.name)) continue; const full = path.join(current, entry.name); const relative = path.relative(root, full).split(path.sep).join("/"); if (entry.isDirectory()) { dirs.add(relative); await walk(root, full, files, dirs); } else if (entry.isFile()) files.add(relative); } }
async function readJsonIfExists(file: string): Promise<Record<string, unknown> | null> { try { return JSON.parse(await fs.readFile(file, "utf8")) as Record<string, unknown>; } catch { return null; } }
async function readTextIfExists(file: string): Promise<string> { try { return await fs.readFile(file, "utf8"); } catch { return ""; } }
async function pathExists(targetPath: string): Promise<boolean> { try { await fs.access(targetPath); return true; } catch { return false; } }
