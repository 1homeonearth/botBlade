import fs from "node:fs/promises";
import path from "node:path";
import type { BotProject } from "../models/project.js";
import { RequestValidationError } from "./projectStore.js";

export interface ProjectFileSummary {
  path: string;
  size: number;
  updatedAt: string;
  generated: boolean;
  editable: boolean;
}

export interface ProjectFileContent extends ProjectFileSummary {
  content: string;
}

export class ProjectFileService {
  constructor(private readonly rootDir = path.join(process.cwd(), "generated-projects")) {}

  async generate(project: BotProject, force = false): Promise<{ projectId: string; generatedAt: string; files: ProjectFileSummary[] }> {
    const workspace = this.workspace(project.id);
    await fs.mkdir(workspace, { recursive: true });
    const files = templateFiles(project);
    for (const [relativePath, content] of Object.entries(files)) {
      const target = this.safePath(project.id, relativePath);
      if (!force && await exists(target)) continue;
      await fs.mkdir(path.dirname(target), { recursive: true });
      await fs.writeFile(target, content, "utf8");
    }
    return { projectId: project.id, generatedAt: new Date().toISOString(), files: await this.list(project.id) };
  }

  async ensureGenerated(project: BotProject): Promise<void> {
    const workspace = this.workspace(project.id);
    if (!await exists(path.join(workspace, "package.json"))) await this.generate(project, false);
  }

  async hasGenerated(projectId: string): Promise<boolean> {
    return exists(path.join(this.workspace(projectId), "package.json"));
  }

  async list(projectId: string): Promise<ProjectFileSummary[]> {
    const workspace = this.workspace(projectId);
    if (!await exists(workspace)) return [];
    const output: ProjectFileSummary[] = [];
    await walk(workspace, workspace, output);
    return output.sort((a, b) => a.path.localeCompare(b.path));
  }

  async read(projectId: string, relativePath: string): Promise<ProjectFileContent> {
    const target = this.safePath(projectId, relativePath);
    const stat = await fs.stat(target).catch(() => undefined);
    if (!stat || !stat.isFile()) throw { statusCode: 404, code: "FILE_NOT_FOUND", message: "Project file was not found.", details: { path: relativePath } };
    const content = await fs.readFile(target, "utf8");
    return { path: normalizeRelativePath(relativePath), size: stat.size, updatedAt: stat.mtime.toISOString(), generated: isGeneratedPath(relativePath), editable: true, content };
  }

  async write(projectId: string, relativePath: string, content: string): Promise<ProjectFileContent> {
    if (typeof content !== "string") throw new RequestValidationError([{ field: "content", message: "File content must be a string." }]);
    const target = this.safePath(projectId, relativePath);
    await fs.mkdir(path.dirname(target), { recursive: true });
    await fs.writeFile(target, content, "utf8");
    return this.read(projectId, relativePath);
  }

  workspace(projectId: string): string {
    return path.join(this.rootDir, sanitizeId(projectId));
  }

  safePath(projectId: string, relativePath: string): string {
    const normalized = normalizeRelativePath(relativePath);
    if (!normalized) throw new RequestValidationError([{ field: "path", message: "File path is required." }]);
    const workspace = this.workspace(projectId);
    const target = path.resolve(workspace, normalized);
    const workspaceResolved = path.resolve(workspace);
    if (!target.startsWith(workspaceResolved + path.sep) && target !== workspaceResolved) {
      throw { statusCode: 400, code: "INVALID_FILE_PATH", message: "File path must stay within the project workspace.", details: { path: relativePath } };
    }
    return target;
  }
}

export function parseFileWriteInput(value: unknown): { content: string } {
  const object = value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
  if (typeof object.content !== "string") throw new RequestValidationError([{ field: "content", message: "File content must be a string." }]);
  return { content: object.content };
}

function templateFiles(project: BotProject): Record<string, string> {
  const intents = project.permissions.intents.length > 0 ? project.permissions.intents : ["Guilds"];
  const commands = project.commands.length > 0 ? project.commands : [{
    id: "cmd_ping",
    name: "ping",
    description: "Replies with pong.",
    type: "chat_input" as const,
    options: [],
    permissions: { defaultMemberPermissions: null, dmPermission: false },
    handler: { kind: "static_response" as const, ephemeral: true, content: "Pong." },
  }];
  const commandImports = commands.map((command) => `import { ${commandExportName(command.name)} } from "./commands/${sanitizeCommandFile(command.name)}.js";`).join("\n");
  const commandArray = commands.map((command) => commandExportName(command.name)).join(", ");
  const files: Record<string, string> = {
    "package.json": JSON.stringify({
      scripts: { build: "tsc -p tsconfig.json", start: "node dist/index.js", test: "node --test dist/**/*.test.js" },
      dependencies: { "discord.js": "^14.15.3" },
      devDependencies: { typescript: "^5.4.5" },
    }, null, 2) + "\n",
    "tsconfig.json": JSON.stringify({ compilerOptions: { target: "ES2022", module: "NodeNext", moduleResolution: "NodeNext", outDir: "dist", rootDir: "src", strict: true, skipLibCheck: true }, include: ["src/**/*.ts"] }, null, 2) + "\n",
    "README.md": `# ${project.name}\n\nGenerated bot project for royalScepter. Configure DISCORD_TOKEN through a secret reference; no secret values are written here.\n`,
    ".env.example": "DISCORD_TOKEN=<secret reference resolved at runtime>\n",
    "src/index.ts": `${commandImports}\nimport { Client, GatewayIntentBits } from "discord.js";\n\nconst commands = [${commandArray}];\n\nconst token = process.env.DISCORD_TOKEN;\nif (!token) {\n  throw new Error("DISCORD_TOKEN is required. Configure discord.tokenSecretRef on the project.");\n}\n\nconst client = new Client({ intents: [${intents.map((intent) => `GatewayIntentBits.${intent}`).join(", ")}] });\nclient.once("ready", () => {\n  console.log(\`Bot logged in with \${commands.length} command definition(s).\`);\n});\nclient.login(token);\n`,
  };
  for (const command of commands) files[`src/commands/${sanitizeCommandFile(command.name)}.ts`] = commandFile(command);
  return files;
}

function commandFile(command: BotProject["commands"][number]): string {
  const response = typeof command.handler === "object" && command.handler.kind === "static_response" ? command.handler.content ?? "" : "TODO: implement custom TypeScript handler.";
  return `export const ${commandExportName(command.name)} = {\n  data: {\n    name: ${JSON.stringify(command.name)},\n    description: ${JSON.stringify(command.description)},\n    type: ${JSON.stringify(command.type ?? "chat_input")},\n    options: ${JSON.stringify(command.options ?? [])},\n    defaultMemberPermissions: ${JSON.stringify(command.permissions?.defaultMemberPermissions ?? null)},\n    dmPermission: ${JSON.stringify(command.permissions?.dmPermission ?? false)}\n  },\n  handler: {\n    kind: ${JSON.stringify(typeof command.handler === "object" ? command.handler.kind : "custom_typescript_placeholder")},\n    ephemeral: ${JSON.stringify(Boolean(typeof command.handler === "object" ? command.handler.ephemeral : false))},\n    content: ${JSON.stringify(response)}\n  }\n};\n`;
}

function sanitizeCommandFile(name: string): string {
  return name.replace(/[^a-z0-9_-]/g, "_");
}

function commandExportName(name: string): string {
  return `${sanitizeCommandFile(name).replace(/(^|[-_])(\w)/g, (_match, _prefix, letter: string) => letter.toUpperCase())}Command`;
}

async function walk(root: string, current: string, output: ProjectFileSummary[]): Promise<void> {
  const entries = await fs.readdir(current, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.name === "node_modules" || entry.name === "dist") continue;
    const full = path.join(current, entry.name);
    if (entry.isDirectory()) {
      await walk(root, full, output);
    } else if (entry.isFile()) {
      const stat = await fs.stat(full);
      const relative = path.relative(root, full).split(path.sep).join("/");
      output.push({ path: relative, size: stat.size, updatedAt: stat.mtime.toISOString(), generated: isGeneratedPath(relative), editable: true });
    }
  }
}

async function exists(filePath: string): Promise<boolean> {
  return fs.access(filePath).then(() => true, () => false);
}

function normalizeRelativePath(value: string): string {
  return decodeURIComponent(value).replace(/\\/g, "/").replace(/^\/+/, "");
}

function sanitizeId(value: string): string {
  return value.replace(/[^A-Za-z0-9_-]/g, "_");
}

function isGeneratedPath(relativePath: string): boolean {
  return ["package.json", "tsconfig.json", "README.md", ".env.example"].includes(normalizeRelativePath(relativePath)) || normalizeRelativePath(relativePath).startsWith("src/");
}
