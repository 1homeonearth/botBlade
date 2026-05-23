// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import fs from "node:fs/promises";  // line 7: executes this statement as part of this file's behavior
import * as path from "node:path";  // line 8: executes this statement as part of this file's behavior
import { BLADE_PACKS } from "../../bladepacks/packs.js";  // line 9: executes this statement as part of this file's behavior
import type { BladePack, BladePackDetector, BladePackRuntime } from "../../bladepacks/schema.js";  // line 10: executes this statement as part of this file's behavior

export type DetectorConfidence = "weak" | "possible" | "likely" | "high";  // line 12: executes this statement as part of this file's behavior

export type DetectionMatch = {  // line 14: executes this statement as part of this file's behavior
  id: string;  // line 15: executes this statement as part of this file's behavior
  name: string;  // line 16: executes this statement as part of this file's behavior
  score: number;  // line 17: executes this statement as part of this file's behavior
  confidence: DetectorConfidence;  // line 18: executes this statement as part of this file's behavior
  matchedEvidence: string[];  // line 19: executes this statement as part of this file's behavior
  runtime: BladePackRuntime;  // line 20: executes this statement as part of this file's behavior
  commands: BladePack["commands"];  // line 21: executes this statement as part of this file's behavior
  requiredSecrets: string[];  // line 22: executes this statement as part of this file's behavior
};  // line 23: executes this statement as part of this file's behavior

export type ScanDetectionResult = {  // line 25: executes this statement as part of this file's behavior
  workspacePath: string;  // line 26: executes this statement as part of this file's behavior
  recommendedPackId: string;  // line 27: executes this statement as part of this file's behavior
  matches: DetectionMatch[];  // line 28: executes this statement as part of this file's behavior
  warnings: string[];  // line 29: executes this statement as part of this file's behavior
  fallbackNotes: string[];  // line 30: executes this statement as part of this file's behavior
};  // line 31: executes this statement as part of this file's behavior

export async function scanWorkspaceForBladePacks(workspacePath: string): Promise<ScanDetectionResult> {  // line 33: executes this statement as part of this file's behavior
  const ctx = await buildContext(workspacePath);  // line 34: executes this statement as part of this file's behavior
  const matches: DetectionMatch[] = [];  // line 35: executes this statement as part of this file's behavior
  for (const pack of BLADE_PACKS) {  // line 36: executes this statement as part of this file's behavior
    let score = 0;  // line 37: executes this statement as part of this file's behavior
    const evidence: string[] = [];  // line 38: executes this statement as part of this file's behavior
    for (const detector of pack.detectors) {  // line 39: executes this statement as part of this file's behavior
      const matched = matchDetector(detector, ctx);  // line 40: executes this statement as part of this file's behavior
      if (matched) {  // line 41: executes this statement as part of this file's behavior
        score += detector.weight;  // line 42: executes this statement as part of this file's behavior
        evidence.push(matched);  // line 43: executes this statement as part of this file's behavior
      }  // line 44: executes this statement as part of this file's behavior
    }  // line 45: executes this statement as part of this file's behavior
    if (score > 0) {  // line 46: executes this statement as part of this file's behavior
      matches.push({  // line 47: executes this statement as part of this file's behavior
        id: pack.id,  // line 48: executes this statement as part of this file's behavior
        name: pack.name,  // line 49: executes this statement as part of this file's behavior
        score: Math.min(score, 100),  // line 50: executes this statement as part of this file's behavior
        confidence: scoreToConfidence(score),  // line 51: executes this statement as part of this file's behavior
        matchedEvidence: evidence,  // line 52: executes this statement as part of this file's behavior
        runtime: pack.runtime,  // line 53: executes this statement as part of this file's behavior
        commands: pack.commands,  // line 54: executes this statement as part of this file's behavior
        requiredSecrets: pack.secrets.filter((s) => s.required).map((s) => s.name)  // line 55: executes this statement as part of this file's behavior
      });  // line 56: executes this statement as part of this file's behavior
    }  // line 57: executes this statement as part of this file's behavior
  }  // line 58: executes this statement as part of this file's behavior
  matches.sort((a, b) => b.score - a.score);  // line 59: executes this statement as part of this file's behavior
  const top = matches[0];  // line 60: executes this statement as part of this file's behavior
  return {  // line 61: executes this statement as part of this file's behavior
    workspacePath,  // line 62: executes this statement as part of this file's behavior
    recommendedPackId: top && top.score >= 40 ? top.id : "unknown",  // line 63: executes this statement as part of this file's behavior
    matches,  // line 64: executes this statement as part of this file's behavior
    warnings: top && top.score < 60 ? ["Detection confidence is below likely threshold."] : [],  // line 65: executes this statement as part of this file's behavior
    fallbackNotes: top ? [] : ["No strong Blade Pack signals found. Open project in editor and configure commands manually."]  // line 66: executes this statement as part of this file's behavior
  };  // line 67: executes this statement as part of this file's behavior
}  // line 68: executes this statement as part of this file's behavior

function scoreToConfidence(score: number): DetectorConfidence {  // line 70: executes this statement as part of this file's behavior
  if (score >= 80) return "high";  // line 71: executes this statement as part of this file's behavior
  if (score >= 60) return "likely";  // line 72: executes this statement as part of this file's behavior
  if (score >= 40) return "possible";  // line 73: executes this statement as part of this file's behavior
  return "weak";  // line 74: executes this statement as part of this file's behavior
}  // line 75: executes this statement as part of this file's behavior

type ScanContext = {  // line 77: executes this statement as part of this file's behavior
  root: string;  // line 78: executes this statement as part of this file's behavior
  files: Set<string>;  // line 79: executes this statement as part of this file's behavior
  directories: Set<string>;  // line 80: executes this statement as part of this file's behavior
  packageJson: Record<string, unknown> | null;  // line 81: executes this statement as part of this file's behavior
  packageDeps: Set<string>;  // line 82: executes this statement as part of this file's behavior
  packageScripts: Set<string>;  // line 83: executes this statement as part of this file's behavior
  envText: string;  // line 84: executes this statement as part of this file's behavior
  sourceText: string;  // line 85: executes this statement as part of this file's behavior
  workflowJson: Record<string, unknown> | null;  // line 86: executes this statement as part of this file's behavior
};  // line 87: executes this statement as part of this file's behavior

async function buildContext(workspacePath: string): Promise<ScanContext> {  // line 89: executes this statement as part of this file's behavior
  const workspaceExists = await pathExists(workspacePath);  // line 90: executes this statement as part of this file's behavior
  if (!workspaceExists) {  // line 91: executes this statement as part of this file's behavior
    return {  // line 92: executes this statement as part of this file's behavior
      root: workspacePath,  // line 93: executes this statement as part of this file's behavior
      files: new Set<string>(),  // line 94: executes this statement as part of this file's behavior
      directories: new Set<string>(),  // line 95: executes this statement as part of this file's behavior
      packageJson: null,  // line 96: executes this statement as part of this file's behavior
      packageDeps: new Set<string>(),  // line 97: executes this statement as part of this file's behavior
      packageScripts: new Set<string>(),  // line 98: executes this statement as part of this file's behavior
      envText: "",  // line 99: executes this statement as part of this file's behavior
      sourceText: "",  // line 100: executes this statement as part of this file's behavior
      workflowJson: null  // line 101: executes this statement as part of this file's behavior
    };  // line 102: executes this statement as part of this file's behavior
  }  // line 103: executes this statement as part of this file's behavior
  const files = new Set<string>();  // line 104: executes this statement as part of this file's behavior
  const directories = new Set<string>();  // line 105: executes this statement as part of this file's behavior
  await walk(workspacePath, workspacePath, files, directories);  // line 106: executes this statement as part of this file's behavior
  const packageJson = await readJsonIfExists(path.join(workspacePath, "package.json"));  // line 107: executes this statement as part of this file's behavior
  const packageDeps = new Set<string>([  // line 108: executes this statement as part of this file's behavior
    ...Object.keys((packageJson?.dependencies as Record<string, unknown>) ?? {}),  // line 109: executes this statement as part of this file's behavior
    ...Object.keys((packageJson?.devDependencies as Record<string, unknown>) ?? {})  // line 110: executes this statement as part of this file's behavior
  ]);  // line 111: executes this statement as part of this file's behavior
  const packageScripts = new Set<string>(Object.keys((packageJson?.scripts as Record<string, unknown>) ?? {}));  // line 112: executes this statement as part of this file's behavior
  const envText = await readTextIfExists(path.join(workspacePath, ".env.example")) + "\n" + await readTextIfExists(path.join(workspacePath, ".env.local"));  // line 113: executes this statement as part of this file's behavior
  const sourceFiles = [...files].filter((f) => /\.(ts|js|tsx|jsx|py|json)$/.test(f)).slice(0, 80);  // line 114: executes this statement as part of this file's behavior
  const sourceText = (await Promise.all(sourceFiles.map((f) => readTextIfExists(path.join(workspacePath, f))))).join("\n");  // line 115: executes this statement as part of this file's behavior
  const workflowJson = await readJsonIfExists(path.join(workspacePath, "workflow.json"));  // line 116: executes this statement as part of this file's behavior
  return { root: workspacePath, files, directories, packageJson, packageDeps, packageScripts, envText, sourceText, workflowJson };  // line 117: executes this statement as part of this file's behavior
}  // line 118: executes this statement as part of this file's behavior

function matchDetector(detector: BladePackDetector, ctx: ScanContext): string | null {  // line 120: executes this statement as part of this file's behavior
  if (detector.kind === "packageDependency") {  // line 121: executes this statement as part of this file's behavior
    const hit = ctx.packageDeps.has(detector.name);  // line 122: executes this statement as part of this file's behavior
    return hit ? `dependency:${detector.name}` : null;  // line 123: executes this statement as part of this file's behavior
  }  // line 124: executes this statement as part of this file's behavior
  if (detector.kind === "packageScript") return ctx.packageScripts.has(detector.name) ? `script:${detector.name}` : null;  // line 125: executes this statement as part of this file's behavior
  if (detector.kind === "sourceImport") return new RegExp(detector.pattern, "i").test(ctx.sourceText) ? `pattern:${detector.pattern}` : null;  // line 126: executes this statement as part of this file's behavior
  if (detector.kind === "envKey") return new RegExp(detector.pattern, "i").test(ctx.envText + "\n" + ctx.sourceText) ? `env:${detector.pattern}` : null;  // line 127: executes this statement as part of this file's behavior
  if (detector.kind === "fileExists" || detector.kind === "knownFilename") return ctx.files.has(detector.path) ? `file:${detector.path}` : null;  // line 128: executes this statement as part of this file's behavior
  if (detector.kind === "knownDirectory") return ctx.directories.has(detector.path) ? `dir:${detector.path}` : null;  // line 129: executes this statement as part of this file's behavior
  if (detector.kind === "jsonShape") {  // line 130: executes this statement as part of this file's behavior
    if (detector.file !== "workflow.json" || !ctx.workflowJson) return null;  // line 131: executes this statement as part of this file's behavior
    const hasKeys = detector.keys.every((k) => k in ctx.workflowJson!);  // line 132: executes this statement as part of this file's behavior
    return hasKeys ? `json-shape:${detector.keys.join(",")}` : null;  // line 133: executes this statement as part of this file's behavior
  }  // line 134: executes this statement as part of this file's behavior
  return null;  // line 135: executes this statement as part of this file's behavior
}  // line 136: executes this statement as part of this file's behavior

async function walk(root: string, current: string, files: Set<string>, dirs: Set<string>): Promise<void> {  // line 138: executes this statement as part of this file's behavior
  const entries = await fs.readdir(current, { withFileTypes: true });  // line 139: executes this statement as part of this file's behavior
  for (const entry of entries) {  // line 140: executes this statement as part of this file's behavior
    if (["node_modules", "dist", ".git"].includes(entry.name)) continue;  // line 141: executes this statement as part of this file's behavior
    const full = path.join(current, entry.name);  // line 142: executes this statement as part of this file's behavior
    const relative = path.relative(root, full).split(path.sep).join("/");  // line 143: executes this statement as part of this file's behavior
    if (entry.isDirectory()) {  // line 144: executes this statement as part of this file's behavior
      dirs.add(relative);  // line 145: executes this statement as part of this file's behavior
      await walk(root, full, files, dirs);  // line 146: executes this statement as part of this file's behavior
    } else if (entry.isFile()) files.add(relative);  // line 147: executes this statement as part of this file's behavior
  }  // line 148: executes this statement as part of this file's behavior
}  // line 149: executes this statement as part of this file's behavior

async function readJsonIfExists(file: string): Promise<Record<string, unknown> | null> {  // line 151: executes this statement as part of this file's behavior
  try { return JSON.parse(await fs.readFile(file, "utf8")) as Record<string, unknown>; } catch { return null; }  // line 152: executes this statement as part of this file's behavior
}  // line 153: executes this statement as part of this file's behavior

async function readTextIfExists(file: string): Promise<string> {  // line 155: executes this statement as part of this file's behavior
  try { return await fs.readFile(file, "utf8"); } catch { return ""; }  // line 156: executes this statement as part of this file's behavior
}  // line 157: executes this statement as part of this file's behavior

async function pathExists(targetPath: string): Promise<boolean> {  // line 159: executes this statement as part of this file's behavior
  try {  // line 160: executes this statement as part of this file's behavior
    await fs.access(targetPath);  // line 161: executes this statement as part of this file's behavior
    return true;  // line 162: executes this statement as part of this file's behavior
  } catch {  // line 163: executes this statement as part of this file's behavior
    return false;  // line 164: executes this statement as part of this file's behavior
  }  // line 165: executes this statement as part of this file's behavior
}  // line 166: executes this statement as part of this file's behavior
