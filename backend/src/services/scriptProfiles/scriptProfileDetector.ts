import fs from "node:fs/promises";
import path from "node:path";
import type { BladePack, BladePackCommandName } from "../../bladepacks/schema.js";
import type {
  BotProfileCommandPlan,
  BotProfileScriptProfile,
  PackageManager,
  ScriptProfileRuntime,
  ScriptProfileSource,
} from "../../models/botProfile.js";

export type ScriptProfileDetectorOptions = {
  commandPlan?: BotProfileCommandPlan;
  selectedPack?: BladePack;
  secretRefs?: string[];
  timestamp?: string;
};

type DetectedProfileInput = {
  id: string;
  name: string;
  description: string;
  source: ScriptProfileSource;
  runtime: ScriptProfileRuntime;
  command: string[];
  workingDirectory?: string;
  secretRefs?: string[];
  timeoutSeconds?: number;
  requiresConfirmation?: boolean;
  tags: string[];
};

type WorkspaceFile = {
  relativePath: string;
  absolutePath: string;
};

const nodeLockfilePackageManagers: Array<[string, Extract<PackageManager, "npm" | "pnpm" | "yarn">]> = [
  ["pnpm-lock.yaml", "pnpm"],
  ["yarn.lock", "yarn"],
  ["package-lock.json", "npm"],
];

const pythonEntrypoints = ["main.py", "bot.py", "app.py"];
const makeTargets = ["build", "test", "install", "validate", "deploy"] as const;
const writeOrExternalSideEffectKeywords = [
  "deploy",
  "delete",
  "destroy",
  "drop",
  "external",
  "install",
  "migrate",
  "migration",
  "publish",
  "push",
  "release",
  "remove",
  "repair",
  "restart",
  "rm",
  "stop",
  "upload",
  "write",
];

export async function detectScriptProfiles(
  workspacePath: string,
  options: ScriptProfileDetectorOptions = {},
): Promise<{ packageManager: PackageManager; profiles: BotProfileScriptProfile[] }> {
  const timestamp = options.timestamp ?? new Date().toISOString();
  const secretRefs = options.secretRefs ?? [];
  const files = await listWorkspaceFiles(workspacePath);
  const fileSet = new Set(files.map((file) => file.relativePath));
  const packageManager = detectPackageManagerFromFiles(fileSet);
  const profiles: DetectedProfileInput[] = [];

  profiles.push(
    ...(options.selectedPack
      ? detectBladePackProfiles(options.selectedPack, options.commandPlan, secretRefs)
      : []),
  );
  profiles.push(...(await detectPackageJsonProfiles(workspacePath, packageManager, secretRefs)));
  profiles.push(...detectPythonEntrypointProfiles(fileSet, secretRefs));
  profiles.push(...detectPythonUtilityProfiles(fileSet, secretRefs));
  profiles.push(...(await detectShellProfiles(files, secretRefs)));
  profiles.push(...(await detectMakefileProfiles(workspacePath, fileSet, secretRefs)));
  profiles.push(...(await detectN8nWorkflowProfiles(workspacePath, files, secretRefs)));

  return {
    packageManager,
    profiles: profiles
      .map((profile) => toBotProfileScriptProfile(profile, timestamp))
      .sort((a, b) => a.id.localeCompare(b.id)),
  };
}

export function detectPackageManagerFromFiles(files: Set<string>): PackageManager {
  for (const [lockfile, packageManager] of nodeLockfilePackageManagers) {
    if (files.has(lockfile)) return packageManager;
  }
  if (files.has("requirements.txt") || files.has("pyproject.toml")) return "pip";
  return "npm";
}

function detectBladePackProfiles(
  selectedPack: BladePack,
  commandPlan: BotProfileCommandPlan | undefined,
  secretRefs: string[],
): DetectedProfileInput[] {
  const commands = commandPlanFromBladePack(selectedPack, commandPlan);
  return commands.map(([action, command]) => ({
    id: stableId("blade-pack", "BladePack.commands", action),
    name: `Blade Pack ${action}`,
    description: "Command recommended by the detected Blade Pack.",
    source: "blade_pack",
    runtime: mapScriptRuntime(selectedPack.runtime.type),
    command: normalizeCommand(command),
    secretRefs,
    timeoutSeconds: defaultTimeoutSeconds(action),
    requiresConfirmation: requiresConfirmation(action, command),
    tags: ["blade_pack", selectedPack.id, action],
  }));
}

function commandPlanFromBladePack(
  selectedPack: BladePack,
  commandPlan: BotProfileCommandPlan | undefined,
): Array<[BladePackCommandName, string]> {
  const actionOrder: BladePackCommandName[] = [
    "install",
    "build",
    "test",
    "validate",
    "start",
    "stop",
    "restart",
    "deploy",
  ];
  return actionOrder.flatMap((action): Array<[BladePackCommandName, string]> => {
    const planned = commandPlan?.[action]?.[0];
    const command = planned ?? selectedPack.commands[action] ?? (action === "start" ? selectedPack.commands.run : undefined);
    return command ? [[action, command]] : [];
  });
}

async function detectPackageJsonProfiles(
  workspacePath: string,
  packageManager: PackageManager,
  secretRefs: string[],
): Promise<DetectedProfileInput[]> {
  const packageJsonPath = path.join(workspacePath, "package.json");
  const packageJson = await readJsonIfExists(packageJsonPath);
  const scripts = packageJson?.scripts;
  if (!scripts || typeof scripts !== "object" || Array.isArray(scripts)) return [];

  return Object.entries(scripts)
    .filter((entry): entry is [string, string] => typeof entry[1] === "string")
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([scriptName, scriptCommand]) => ({
      id: stableId("package-json", "package.json", scriptName),
      name: `${packageManagerForNodeCommand(packageManager)}: ${scriptName}`,
      description: `Detected package.json script named ${scriptName}.`,
      source: "package_json",
      runtime: "node",
      command: packageScriptCommand(packageManager, scriptName),
      secretRefs,
      timeoutSeconds: defaultTimeoutSeconds(scriptName),
      requiresConfirmation: requiresConfirmation(scriptName, scriptCommand),
      tags: ["package_json", scriptName, packageManagerForNodeCommand(packageManager)],
    }));
}

function detectPythonEntrypointProfiles(
  files: Set<string>,
  secretRefs: string[],
): DetectedProfileInput[] {
  return pythonEntrypoints
    .filter((entrypoint) => files.has(entrypoint))
    .map((entrypoint) => ({
      id: stableId("file", entrypoint, "start"),
      name: `python: ${entrypoint}`,
      description: `Detected Python entrypoint ${entrypoint}.`,
      source: "file",
      runtime: "python",
      command: ["python", entrypoint],
      secretRefs,
      timeoutSeconds: 300,
      requiresConfirmation: false,
      tags: ["python", "entrypoint"],
    }));
}

function detectPythonUtilityProfiles(
  files: Set<string>,
  secretRefs: string[],
): DetectedProfileInput[] {
  const profiles: DetectedProfileInput[] = [];
  if (files.has("pytest.ini") || files.has("tox.ini") || files.has("conftest.py") || [...files].some((file) => file.startsWith("tests/") && file.endsWith(".py"))) {
    profiles.push({
      id: stableId("file", "python-tests", "pytest"),
      name: "python: pytest",
      description: "Detected Python test files or pytest configuration.",
      source: "file",
      runtime: "python",
      command: ["python", "-m", "pytest"],
      secretRefs,
      timeoutSeconds: 600,
      requiresConfirmation: false,
      tags: ["python", "test"],
    });
  }
  if (files.has("requirements.txt")) {
    profiles.push({
      id: stableId("file", "requirements.txt", "install"),
      name: "python: install requirements",
      description: "Detected requirements.txt dependency manifest.",
      source: "file",
      runtime: "python",
      command: ["python", "-m", "pip", "install", "-r", "requirements.txt"],
      secretRefs,
      timeoutSeconds: 600,
      requiresConfirmation: true,
      tags: ["python", "install"],
    });
  }
  return profiles;
}

async function detectShellProfiles(
  files: WorkspaceFile[],
  secretRefs: string[],
): Promise<DetectedProfileInput[]> {
  const profiles: DetectedProfileInput[] = [];
  for (const file of files.sort((a, b) => a.relativePath.localeCompare(b.relativePath))) {
    if (!(await isShellProfileCandidate(file))) continue;
    profiles.push({
      id: stableId("file", file.relativePath, "shell"),
      name: `shell: ${file.relativePath}`,
      description: `Detected shell script ${file.relativePath}.`,
      source: "file",
      runtime: "shell",
      command: ["bash", file.relativePath],
      secretRefs,
      timeoutSeconds: defaultTimeoutSeconds(file.relativePath),
      requiresConfirmation: requiresConfirmation(file.relativePath, await readSmallText(file.absolutePath)),
      tags: ["shell"],
    });
  }
  return profiles;
}

async function detectMakefileProfiles(
  workspacePath: string,
  files: Set<string>,
  secretRefs: string[],
): Promise<DetectedProfileInput[]> {
  const makefileName = ["Makefile", "makefile"].find((candidate) => files.has(candidate));
  if (!makefileName) return [];
  const text = await readSmallText(path.join(workspacePath, makefileName), 128_000);
  const targets = new Set<string>();
  for (const match of text.matchAll(/^([A-Za-z0-9_.-]+)\s*:(?![=])/gm)) {
    targets.add(match[1] ?? "");
  }
  return makeTargets
    .filter((target) => targets.has(target))
    .map((target) => ({
      id: stableId("file", makefileName, target),
      name: `make: ${target}`,
      description: `Detected Makefile target ${target}.`,
      source: "file",
      runtime: "shell",
      command: ["make", target],
      secretRefs,
      timeoutSeconds: defaultTimeoutSeconds(target),
      requiresConfirmation: requiresConfirmation(target),
      tags: ["makefile", target],
    }));
}

async function detectN8nWorkflowProfiles(
  workspacePath: string,
  files: WorkspaceFile[],
  secretRefs: string[],
): Promise<DetectedProfileInput[]> {
  const workflowFiles: string[] = [];
  for (const file of files) {
    if (!file.relativePath.endsWith(".json")) continue;
    const json = await readJsonIfExists(path.join(workspacePath, file.relativePath));
    if (isN8nWorkflowJson(json)) workflowFiles.push(file.relativePath);
  }
  return workflowFiles.sort((a, b) => a.localeCompare(b)).flatMap((workflowPath) => [
    {
      id: stableId("file", workflowPath, "n8n-validate-metadata"),
      name: "n8n: validate workflow metadata",
      description: "Validate imported n8n workflow JSON metadata without executing n8n.",
      source: "file" as const,
      runtime: "workflow" as const,
      command: ["botblade", "workflow", "validate", workflowPath],
      secretRefs,
      timeoutSeconds: 300,
      requiresConfirmation: false,
      tags: ["n8n", "metadata", "validate"],
    },
    {
      id: stableId("file", workflowPath, "n8n-export-metadata"),
      name: "n8n: export workflow metadata",
      description: "Export BotBlade metadata for imported n8n workflow JSON without executing n8n.",
      source: "file" as const,
      runtime: "workflow" as const,
      command: ["botblade", "workflow", "export-metadata", workflowPath],
      secretRefs,
      timeoutSeconds: 300,
      requiresConfirmation: true,
      tags: ["n8n", "metadata", "export"],
    },
  ]);
}

function toBotProfileScriptProfile(
  profile: DetectedProfileInput,
  timestamp: string,
): BotProfileScriptProfile {
  return {
    id: profile.id,
    name: profile.name,
    description: profile.description,
    source: profile.source,
    runtime: profile.runtime,
    command: profile.command,
    workingDirectory: profile.workingDirectory ?? ".",
    envRefs: [],
    secretRefs: profile.secretRefs ?? [],
    timeoutSeconds: profile.timeoutSeconds ?? 300,
    requiresConfirmation: profile.requiresConfirmation ?? false,
    tags: profile.tags,
    createdAt: timestamp,
    updatedAt: timestamp,
  };
}

async function listWorkspaceFiles(workspacePath: string): Promise<WorkspaceFile[]> {
  try {
    await fs.access(workspacePath);
  } catch {
    return [];
  }
  const files: WorkspaceFile[] = [];
  await walk(workspacePath, workspacePath, files);
  return files;
}

async function walk(root: string, current: string, files: WorkspaceFile[]): Promise<void> {
  let entries: Array<{ name: string; isDirectory: () => boolean; isFile: () => boolean }>;
  try {
    entries = await fs.readdir(current, { withFileTypes: true });
  } catch {
    return;
  }
  entries.sort((a, b) => a.name.localeCompare(b.name));
  for (const entry of entries) {
    if ([".git", "node_modules", ".venv", "dist", "build"].includes(entry.name)) continue;
    const absolutePath = path.join(current, entry.name);
    const relativePath = toProjectRelativePath(root, absolutePath);
    if (entry.isDirectory()) {
      await walk(root, absolutePath, files);
    } else if (entry.isFile()) {
      files.push({ relativePath, absolutePath });
    }
  }
}

async function isShellProfileCandidate(file: WorkspaceFile): Promise<boolean> {
  if (/\.(?:sh|bash)$/i.test(file.relativePath)) return true;
  if (file.relativePath.startsWith("scripts/")) {
    if (!file.relativePath.includes(".")) return (await readSmallText(file.absolutePath, 256)).startsWith("#!");
    return /\.(?:sh|bash)$/i.test(file.relativePath);
  }
  return false;
}

function isN8nWorkflowJson(value: unknown): boolean {
  if (!value || typeof value !== "object" || Array.isArray(value)) return false;
  const object = value as Record<string, unknown>;
  if (!("nodes" in object) || !("connections" in object)) return false;
  return true;
}

async function readJsonIfExists(filePath: string): Promise<Record<string, unknown> | null> {
  try {
    const parsed = JSON.parse(await fs.readFile(filePath, "utf8")) as unknown;
    return parsed && typeof parsed === "object" && !Array.isArray(parsed)
      ? (parsed as Record<string, unknown>)
      : null;
  } catch {
    return null;
  }
}

async function readSmallText(filePath: string, limit = 4096): Promise<string> {
  try {
    return (await fs.readFile(filePath, "utf8")).slice(0, limit);
  } catch {
    return "";
  }
}

function packageManagerForNodeCommand(packageManager: PackageManager): "npm" | "pnpm" | "yarn" {
  if (packageManager === "pnpm" || packageManager === "yarn") return packageManager;
  return "npm";
}

function packageScriptCommand(packageManager: PackageManager, scriptName: string): string[] {
  const nodePackageManager = packageManagerForNodeCommand(packageManager);
  if (nodePackageManager === "yarn") return ["yarn", "run", scriptName];
  if (nodePackageManager === "pnpm") return ["pnpm", "run", scriptName];
  return ["npm", "run", scriptName];
}

function normalizeCommand(command: string): string[] {
  const trimmed = command.trim();
  if (!trimmed) return [];
  const normalized = normalizeKnownCommand(trimmed);
  if (normalized) return normalized;
  return splitCommand(trimmed);
}

function normalizeKnownCommand(command: string): string[] | null {
  const parts = splitCommand(command);
  if (parts[0] === "npm" && parts[1] === "install" && parts.length === 2) return ["npm", "install"];
  if (parts[0] === "npm" && parts[1] === "start" && parts.length === 2) return ["npm", "start"];
  if (parts[0] === "npm" && parts[1] === "test" && parts.length === 2) return ["npm", "test"];
  if (parts[0] === "npm" && parts[1] === "run" && parts[2] && parts.length === 3) return ["npm", "run", parts[2]];
  if (parts[0] === "pip" && parts[1] === "install") return ["python", "-m", "pip", ...parts.slice(1)];
  if (parts[0] === "python" || parts[0] === "python3") return ["python", ...parts.slice(1)];
  if (parts[0] === "botblade") return parts;
  return null;
}

function splitCommand(command: string): string[] {
  const tokens = command.match(/(?:[^\s"']+|"[^"]*"|'[^']*')+/g) ?? [];
  return tokens.map((token) => token.replace(/^(["'])(.*)\1$/, "$2"));
}

function mapScriptRuntime(runtime: string | undefined): ScriptProfileRuntime {
  if (runtime === "node" || runtime === "python" || runtime === "workflow") return runtime;
  return "custom";
}

function defaultTimeoutSeconds(action: string): number {
  const lowerAction = action.toLowerCase();
  if (["install", "build", "test", "validate"].includes(lowerAction)) return 600;
  if (writeOrExternalSideEffectKeywords.some((keyword) => lowerAction.includes(keyword))) return 900;
  return 300;
}

function requiresConfirmation(name: string, command = ""): boolean {
  const haystack = `${name} ${command}`.toLowerCase();
  return writeOrExternalSideEffectKeywords.some((keyword) => new RegExp(`(^|[^a-z0-9])${escapeRegExp(keyword)}([^a-z0-9]|$)`).test(haystack));
}

function stableId(source: string, relativePath: string, name: string): string {
  return [source, relativePath, name].map(slugSegment).join(":");
}

function slugSegment(value: string): string {
  return value
    .replace(/\\/g, "/")
    .toLowerCase()
    .replace(/[^a-z0-9._/-]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .replace(/\/+$/g, "");
}

function toProjectRelativePath(root: string, absolutePath: string): string {
  return path.relative(root, absolutePath).replace(/\\/g, "/");
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
