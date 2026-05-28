import fs from "node:fs/promises";
import * as path from "node:path";
import { BLADE_PACKS } from "../../bladepacks/packs.js";
import type { BladePack, BladePackDetector, BladePackRuntime } from "../../bladepacks/schema.js";
import type { BotProfileCommandPlan } from "../../models/botProfile.js";

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
  secretRequirements: { required: ScanSecretRequirement[]; optional: ScanSecretRequirement[] };
  importantFiles: string[];
  detectedLanguages: string[];
  detectedFrameworks: string[];
  permissions: string[];
  capabilities: string[];
  git: { branch: string | null; status: "clean" | "dirty" | "unknown"; remotes: Array<{ name: string; url: string | null }> };
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
    install: [ctx.packageManager === "pip" ? "pip install -r requirements.txt" : "npm install"],
    build: selectedPack?.commands.build ? [selectedPack.commands.build] : [],
    test: selectedPack?.commands.test ? [selectedPack.commands.test] : [],
    validate: ["botblade validate"],
    start: selectedPack?.commands.run ? [selectedPack.commands.run] : [],
    stop: ["botblade runtime stop"],
    restart: ["botblade runtime restart"],
    deploy: ["botblade deploy"]
  };
  const required = (selectedPack?.secrets ?? []).filter((s) => s.required).map((s) => ({ name: s.name, required: true, configured: false }));
  const optional = (selectedPack?.secrets ?? []).filter((s) => !s.required).map((s) => ({ name: s.name, required: false, configured: false }));
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
    secretRequirements: { required, optional },
    importantFiles: [...ctx.files].filter((f) => /(package\.json|requirements\.txt|pyproject\.toml|workflow\.json|README\.md)$/i.test(f)).slice(0, 20),
    detectedLanguages: top?.id.includes("python") ? ["python"] : ["javascript", "typescript"],
    detectedFrameworks: top ? [top.name] : [],
    permissions: ["read_workspace", "write_workspace"],
    capabilities: ["scan", "diagnose", "run_commands"],
    git: { branch: null, status: "unknown", remotes: [] }
  };
}

function scoreToConfidence(score: number): DetectorConfidence { if (score >= 80) return "high"; if (score >= 60) return "likely"; if (score >= 40) return "possible"; return "weak"; }

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
  const sourceFiles = [...files].filter((f) => /\.(ts|js|tsx|jsx|py|json)$/.test(f)).slice(0, 80);
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
