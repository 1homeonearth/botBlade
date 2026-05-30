import fs from "node:fs/promises";
import * as path from "node:path";
import { BLADE_PACKS } from "../../bladepacks/packs.js";
import type { BladePack, BladePackDetector, BladePackRuntime } from "../../bladepacks/schema.js";
import type { BotProfileCommandPlan, BotProfileScriptProfile, PackageManager } from "../../models/botProfile.js";
import { gitStatusToMetadata, GitStatusService, type GitStatusMetadata } from "../gitStatusService.js";
import { normalizeProjectRelativePath } from "../security/projectPaths.js";
import { isSafeScriptProfileCommand } from "../scriptProfiles/scriptProfileService.js";
import { detectScriptProfiles } from "../scriptProfiles/scriptProfileDetector.js";
import { generateRepairCards } from "./repairCards.js";

export type DetectorConfidence = "weak" | "possible" | "likely" | "high";

const DETECTED_FRAMEWORK_MIN_SCORE = 40;
const SUPPLEMENTAL_SHELL_SCORE_WITH_LANGUAGE_MANIFEST = 15;
const LANGUAGE_ORDER = ["javascript", "typescript", "python", "shell", "workflow"] as const;
const TRACKED_FILE_EXTENSIONS = new Set([".js", ".ts", ".jsx", ".tsx", ".py", ".sh", ".bash", ".json", ".yml", ".yaml"]);

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
  git: GitStatusMetadata;
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
      const adjustedScore = adjustScoreForContext(pack, score, ctx);
      matches.push({
        id: pack.id,
        name: pack.name,
        score: adjustedScore,
        confidence: scoreToConfidence(adjustedScore),
        matchedEvidence: evidence,
        runtime: pack.runtime,
        commands: pack.commands,
        requiredSecrets: pack.secrets.filter((s) => s.required).map((s) => s.name)
      });
    }
  }
  matches.sort((a, b) => b.score - a.score || a.id.localeCompare(b.id));
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
  const git = gitStatusToMetadata(await new GitStatusService().readStatusStaticSafe(workspacePath, ctx.files));
  const { packageManager, profiles } = await detectScriptProfiles(workspacePath, {
    commandPlan,
    selectedPack,
    secretRefs: [...required, ...optional].map((secret) => secret.name),
  });
  const scriptProfiles = dedupeScriptProfiles(profiles.filter((profile) => isSafeScriptProfileCommand(profile.command)));
  const detectedLanguages = detectLanguages(ctx);
  const detectedFrameworks = matches.filter((match) => match.score >= DETECTED_FRAMEWORK_MIN_SCORE).map((match) => match.name);
  const recommendedPackId = top && top.score >= 40 ? top.id : "unknown";
  const repairCards = generateRepairCards({
    recommendedPackId,
    matches,
    commandPlan,
    scriptProfiles,
    secretRequirements: { required, optional },
    files: ctx.files,
    fileExtensions: ctx.fileExtensions,
    packageJson: ctx.packageJson,
    packageScripts: ctx.packageScripts,
    workflowJson: ctx.workflowJson,
    workflowJsonExists: ctx.workflowJsonExists,
    workflowJsonParseError: ctx.workflowJsonParseError,
    detectedLanguages,
    git,
  });
  return {
    workspacePath,
    recommendedPackId,
    matches,
    diagnostics: {
      warnings: top && top.score < 60 ? ["Detection confidence is below likely threshold."] : [],
      repairCards
    },
    fallbackNotes: top ? [] : ["No strong Blade Pack signals found. Open project in editor and configure commands manually."],
    commandPlan,
    scriptProfiles,
    secretRequirements: { required, optional },
    importantFiles: detectImportantFiles(ctx),
    detectedLanguages,
    detectedFrameworks,
    permissions: ["read_workspace", "write_workspace"],
    capabilities: ["scan", "diagnose", "run_commands"],
    git,
    packageManager
  };
}

function adjustScoreForContext(pack: BladePack, score: number, ctx: ScanContext): number {
  const normalizedScore = Math.min(score, 100);
  if (pack.id === "generic-shell" && hasLanguageManifest(ctx)) {
    return Math.min(normalizedScore, SUPPLEMENTAL_SHELL_SCORE_WITH_LANGUAGE_MANIFEST);
  }
  return normalizedScore;
}

function hasLanguageManifest(ctx: ScanContext): boolean {
  return Boolean(
    ctx.packageJson ||
      ctx.files.has("requirements.txt") ||
      ctx.files.has("pyproject.toml") ||
      ctx.workflowJson ||
      ctx.files.has("botpress.config.json"),
  );
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
  return normalizeProjectRelativePath(workingDirectory, { allowRoot: true }).path ?? ".";
}

function scoreToConfidence(score: number): DetectorConfidence { if (score >= 80) return "high"; if (score >= 60) return "likely"; if (score >= 40) return "possible"; return "weak"; }

function detectLanguages(ctx: ScanContext): string[] {
  const languages = new Set<string>();
  if (ctx.fileExtensions.has(".js") || ctx.fileExtensions.has(".jsx") || ctx.packageJson) languages.add("javascript");
  if (ctx.fileExtensions.has(".ts") || ctx.fileExtensions.has(".tsx") || ctx.files.has("tsconfig.json")) languages.add("typescript");
  if (ctx.fileExtensions.has(".py") || ctx.files.has("requirements.txt") || ctx.files.has("pyproject.toml")) languages.add("python");
  if (
    ctx.fileExtensions.has(".sh") ||
    ctx.fileExtensions.has(".bash") ||
    ctx.files.has("Makefile") ||
    ctx.files.has("Taskfile.yml") ||
    ctx.files.has("Taskfile.yaml") ||
    ctx.files.has("justfile") ||
    ctx.files.has(".shellcheckrc") ||
    [...ctx.files].some((file) => file.startsWith("scripts/"))
  ) {
    languages.add("shell");
  }
  if (ctx.workflowJson) languages.add("workflow");
  return LANGUAGE_ORDER.filter((language) => languages.has(language));
}

function detectImportantFiles(ctx: ScanContext): string[] {
  return [...ctx.files]
    .filter((file) => isImportantFile(file))
    .sort(compareImportantFiles)
    .slice(0, 20);
}

function compareImportantFiles(a: string, b: string): number {
  return importantFilePriority(a) - importantFilePriority(b) || a.localeCompare(b);
}

function importantFilePriority(file: string): number {
  const normalized = file.toLowerCase();
  const basename = normalized.split("/").pop() ?? normalized;
  if (isManifestOrConfigFile(file, basename)) return 0;
  if (/^(?:.*\/)?dockerfile$/i.test(file) || /^(?:.*\/)?docker-compose\.ya?ml$/i.test(file)) return 1;
  if (/^\.github\/workflows\/[^/]+\.ya?ml$/i.test(file)) return 2;
  if (file.startsWith(".botpress/")) return 3;
  if (file.startsWith("scripts/")) return 4;
  return 5;
}

function isManifestOrConfigFile(file: string, basename = file.split("/").pop() ?? file): boolean {
  if (file.includes("/")) return false;
  return /^(package\.json|requirements\.txt|pyproject\.toml|workflow\.json|README\.md|Makefile|Taskfile\.ya?ml|justfile|botpress\.config\.json|\.shellcheckrc)$/i.test(basename);
}

function isImportantFile(file: string): boolean {
  const basename = file.split("/").pop() ?? file;
  if (file.startsWith("scripts/")) return true;
  if (file.startsWith(".botpress/")) return true;
  if (/^\.github\/workflows\/[^/]+\.ya?ml$/i.test(file)) return true;
  if (/^(?:.*\/)?Dockerfile$/i.test(file)) return true;
  if (/^(?:.*\/)?docker-compose\.ya?ml$/i.test(file)) return true;
  return isManifestOrConfigFile(file, basename);
}

function isStaticSourceFile(relativePath: string): boolean {
  if (/\.(ts|js|tsx|jsx|py|json|ya?ml|sh|bash)$/i.test(relativePath)) return true;
  if (relativePath.startsWith("scripts/") && !(relativePath.split("/").pop() ?? "").includes(".")) return true;
  return false;
}

type ScanContext = {
  root: string; files: Set<string>; directories: Set<string>; fileExtensions: Set<string>; packageJson: Record<string, unknown> | null; packageDeps: Set<string>; packageScripts: Set<string>; envText: string; sourceText: string; workflowJson: Record<string, unknown> | null; workflowJsonExists: boolean; workflowJsonParseError: boolean; packageManager: "npm" | "pnpm" | "yarn" | "pip" | "unknown";
};

async function buildContext(workspacePath: string): Promise<ScanContext> {
  const workspaceExists = await pathExists(workspacePath);
  if (!workspaceExists) return { root: workspacePath, files: new Set(), directories: new Set(), fileExtensions: new Set(), packageJson: null, packageDeps: new Set(), packageScripts: new Set(), envText: "", sourceText: "", workflowJson: null, workflowJsonExists: false, workflowJsonParseError: false, packageManager: "unknown" };
  const files = new Set<string>(); const directories = new Set<string>();
  await walk(workspacePath, workspacePath, files, directories);
  const fileExtensions = detectFileExtensions(files);
  const packageJson = await readJsonIfExists(path.join(workspacePath, "package.json"));
  const packageDeps = new Set<string>([...Object.keys((packageJson?.dependencies as Record<string, unknown>) ?? {}), ...Object.keys((packageJson?.devDependencies as Record<string, unknown>) ?? {})]);
  const packageScripts = new Set<string>(Object.keys((packageJson?.scripts as Record<string, unknown>) ?? {}));
  const envText = await readTextIfExists(path.join(workspacePath, ".env.example")) + "\n" + await readTextIfExists(path.join(workspacePath, ".env.local"));
  const sourceFiles = [...files].sort((a, b) => a.localeCompare(b)).filter(isStaticSourceFile).slice(0, 80);
  const sourceText = (await Promise.all(sourceFiles.map((f) => readTextIfExists(path.join(workspacePath, f))))).join("\n");
  const workflowJsonResult = await readJsonFileIfExists(path.join(workspacePath, "workflow.json"));
  const workflowJson = workflowJsonResult.value;
  const workflowJsonExists = workflowJsonResult.exists;
  const workflowJsonParseError = workflowJsonResult.parseError;
  const packageManager = files.has("pnpm-lock.yaml") ? "pnpm" : files.has("yarn.lock") ? "yarn" : files.has("package-lock.json") ? "npm" : files.has("requirements.txt") ? "pip" : "unknown";
  return { root: workspacePath, files, directories, fileExtensions, packageJson, packageDeps, packageScripts, envText, sourceText, workflowJson, workflowJsonExists, workflowJsonParseError, packageManager };
}

function detectFileExtensions(files: Set<string>): Set<string> {
  const extensions = new Set<string>();
  for (const file of files) {
    const extension = detectFileExtension(file);
    if (extension && TRACKED_FILE_EXTENSIONS.has(extension)) extensions.add(extension);
  }
  return extensions;
}

function detectFileExtension(file: string): string | null {
  const basename = file.split("/").pop() ?? file;
  const extensionMatch = /\.[^.]+$/.exec(basename);
  return extensionMatch ? extensionMatch[0].toLowerCase() : null;
}

function matchDetector(detector: BladePackDetector, ctx: ScanContext): string | null { if (detector.kind === "packageDependency") return ctx.packageDeps.has(detector.name) ? `dependency:${detector.name}` : null; if (detector.kind === "packageScript") return ctx.packageScripts.has(detector.name) ? `script:${detector.name}` : null; if (detector.kind === "sourceImport") return new RegExp(detector.pattern, "i").test(ctx.sourceText) ? `pattern:${detector.pattern}` : null; if (detector.kind === "envKey") return new RegExp(detector.pattern, "i").test(ctx.envText + "\n" + ctx.sourceText) ? `env:${detector.pattern}` : null; if (detector.kind === "fileExists" || detector.kind === "knownFilename") return ctx.files.has(detector.path) ? `file:${detector.path}` : null; if (detector.kind === "knownDirectory") return ctx.directories.has(detector.path) ? `dir:${detector.path}` : null; if (detector.kind === "jsonShape") { if (detector.file !== "workflow.json" || !ctx.workflowJson) return null; return detector.keys.every((k) => k in ctx.workflowJson!) ? `json-shape:${detector.keys.join(",")}` : null; } return null; }
async function walk(root: string, current: string, files: Set<string>, dirs: Set<string>): Promise<void> {
  const entries = await fs.readdir(current, { withFileTypes: true });
  for (const entry of entries) {
    if (["node_modules", "dist", ".git"].includes(entry.name)) continue;
    if (entry.isSymbolicLink()) continue;
    const full = path.join(current, entry.name);
    const relativeResult = normalizeProjectRelativePath(path.relative(root, full).split(path.sep).join("/"), { allowRoot: false });
    if (!relativeResult.ok || !relativeResult.path) continue;
    const relative = relativeResult.path;
    if (entry.isDirectory()) {
      dirs.add(relative);
      await walk(root, full, files, dirs);
    } else if (entry.isFile()) files.add(relative);
  }
}
async function readJsonIfExists(file: string): Promise<Record<string, unknown> | null> { return (await readJsonFileIfExists(file)).value; }
async function readJsonFileIfExists(file: string): Promise<{ exists: boolean; parseError: boolean; value: Record<string, unknown> | null }> { try { const text = await fs.readFile(file, "utf8"); try { return { exists: true, parseError: false, value: JSON.parse(text) as Record<string, unknown> }; } catch { return { exists: true, parseError: true, value: null }; } } catch { return { exists: false, parseError: false, value: null }; } }
async function readTextIfExists(file: string): Promise<string> { try { return await fs.readFile(file, "utf8"); } catch { return ""; } }
async function pathExists(targetPath: string): Promise<boolean> { try { await fs.access(targetPath); return true; } catch { return false; } }
