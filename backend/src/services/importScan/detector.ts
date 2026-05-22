import fs from "node:fs/promises";
import * as path from "node:path";
import { BLADE_PACKS } from "../../bladepacks/packs.js";
import type { BladePack, BladePackDetector, BladePackRuntime } from "../../bladepacks/schema.js";

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

export type ScanDetectionResult = {
  workspacePath: string;
  recommendedPackId: string;
  matches: DetectionMatch[];
  warnings: string[];
  fallbackNotes: string[];
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
  return {
    workspacePath,
    recommendedPackId: top && top.score >= 40 ? top.id : "unknown",
    matches,
    warnings: top && top.score < 60 ? ["Detection confidence is below likely threshold."] : [],
    fallbackNotes: top ? [] : ["No strong Blade Pack signals found. Open project in editor and configure commands manually."]
  };
}

function scoreToConfidence(score: number): DetectorConfidence {
  if (score >= 80) return "high";
  if (score >= 60) return "likely";
  if (score >= 40) return "possible";
  return "weak";
}

type ScanContext = {
  root: string;
  files: Set<string>;
  directories: Set<string>;
  packageJson: Record<string, unknown> | null;
  packageDeps: Set<string>;
  packageScripts: Set<string>;
  envText: string;
  sourceText: string;
  workflowJson: Record<string, unknown> | null;
};

async function buildContext(workspacePath: string): Promise<ScanContext> {
  const files = new Set<string>();
  const directories = new Set<string>();
  await walk(workspacePath, workspacePath, files, directories);
  const packageJson = await readJsonIfExists(path.join(workspacePath, "package.json"));
  const packageDeps = new Set<string>([
    ...Object.keys((packageJson?.dependencies as Record<string, unknown>) ?? {}),
    ...Object.keys((packageJson?.devDependencies as Record<string, unknown>) ?? {})
  ]);
  const packageScripts = new Set<string>(Object.keys((packageJson?.scripts as Record<string, unknown>) ?? {}));
  const envText = await readTextIfExists(path.join(workspacePath, ".env.example")) + "\n" + await readTextIfExists(path.join(workspacePath, ".env.local"));
  const sourceFiles = [...files].filter((f) => /\.(ts|js|tsx|jsx|py|json)$/.test(f)).slice(0, 80);
  const sourceText = (await Promise.all(sourceFiles.map((f) => readTextIfExists(path.join(workspacePath, f))))).join("\n");
  const workflowJson = await readJsonIfExists(path.join(workspacePath, "workflow.json"));
  return { root: workspacePath, files, directories, packageJson, packageDeps, packageScripts, envText, sourceText, workflowJson };
}

function matchDetector(detector: BladePackDetector, ctx: ScanContext): string | null {
  if (detector.kind === "packageDependency") {
    const hit = [...ctx.packageDeps].some((dep) => dep === detector.name || dep.startsWith(detector.name));
    return hit ? `dependency:${detector.name}` : null;
  }
  if (detector.kind === "packageScript") return ctx.packageScripts.has(detector.name) ? `script:${detector.name}` : null;
  if (detector.kind === "sourceImport") return new RegExp(detector.pattern, "i").test(ctx.sourceText) ? `pattern:${detector.pattern}` : null;
  if (detector.kind === "envKey") return new RegExp(detector.pattern, "i").test(ctx.envText + "\n" + ctx.sourceText) ? `env:${detector.pattern}` : null;
  if (detector.kind === "fileExists" || detector.kind === "knownFilename") return ctx.files.has(detector.path) ? `file:${detector.path}` : null;
  if (detector.kind === "knownDirectory") return ctx.directories.has(detector.path) ? `dir:${detector.path}` : null;
  if (detector.kind === "jsonShape") {
    if (detector.file !== "workflow.json" || !ctx.workflowJson) return null;
    const hasKeys = detector.keys.every((k) => k in ctx.workflowJson!);
    return hasKeys ? `json-shape:${detector.keys.join(",")}` : null;
  }
  return null;
}

async function walk(root: string, current: string, files: Set<string>, dirs: Set<string>): Promise<void> {
  const entries = await fs.readdir(current, { withFileTypes: true });
  for (const entry of entries) {
    if (["node_modules", "dist", ".git"].includes(entry.name)) continue;
    const full = path.join(current, entry.name);
    const relative = path.relative(root, full).split(path.sep).join("/");
    if (entry.isDirectory()) {
      dirs.add(relative);
      await walk(root, full, files, dirs);
    } else if (entry.isFile()) files.add(relative);
  }
}

async function readJsonIfExists(file: string): Promise<Record<string, unknown> | null> {
  try { return JSON.parse(await fs.readFile(file, "utf8")) as Record<string, unknown>; } catch { return null; }
}

async function readTextIfExists(file: string): Promise<string> {
  try { return await fs.readFile(file, "utf8"); } catch { return ""; }
}
