import fs from "node:fs/promises";
import path from "node:path";
import type { BotProject } from "../models/project.js";
import { RequestValidationError } from "./projectStore.js";
import { generatedBotPackageLock } from "./generatedBotLockfile.js";

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

export interface GeneratedWorkspacePaths {
  root: string;
  workspace: string;
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

  generatedRoot(): string {
    return path.resolve(this.rootDir);
  }

  workspace(projectId: string): string {
    return path.join(this.rootDir, sanitizeId(projectId));
  }

  resolveWorkspace(projectId: string): GeneratedWorkspacePaths {
    return { root: this.generatedRoot(), workspace: path.resolve(this.workspace(projectId)) };
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
  const maxBytes = 512 * 1024;
  const object = value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
  if (typeof object.content !== "string") throw new RequestValidationError([{ field: "content", message: "File content must be a string." }]);
  if (object.content.length * 4 > maxBytes) throw new RequestValidationError([{ field: "content", message: "File content exceeds 512KB limit." }]);
  return { content: object.content };
}

export function templateFiles(project: BotProject): Record<string, string> {
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
  const commandTestImports = commands.map((command) => `import { ${commandExportName(command.name)} } from "./${sanitizeCommandFile(command.name)}.js";`).join("\n");
  const commandArray = commands.map((command) => commandExportName(command.name)).join(", ");
  const requireApplicationId = project.discord.commandRegistration === "global" || project.discord.commandRegistration === "guild";
  const requireGuildId = project.discord.commandRegistration === "guild";
  const registrationModeMessage = project.discord.commandRegistration === "guild" ? "guild" : "global";

  const files: Record<string, string> = {
    "package.json": JSON.stringify({
      name: project.slug,
      version: "0.1.0",
      private: true,
      type: "module",
      scripts: {
        build: "tsc -p tsconfig.json",
        start: "node dist/index.js",
        test: "node --test dist/**/*.test.js",
        "register:commands": "node dist/register-commands.js",
        "register:commands:guild": "DISCORD_COMMAND_REGISTRATION=guild node dist/register-commands.js",
        "register:commands:global": "DISCORD_COMMAND_REGISTRATION=global node dist/register-commands.js",
      },
      dependencies: { "discord.js": "14.15.3" },
      devDependencies: { typescript: "^5.4.5" },
    }, null, 2) + "\n",
    "package-lock.json": generatedBotPackageLock(project.slug),
    "tsconfig.json": JSON.stringify({ compilerOptions: { target: "ES2022", module: "NodeNext", moduleResolution: "NodeNext", outDir: "dist", rootDir: "src", strict: true, skipLibCheck: true }, include: ["src/**/*.ts"] }, null, 2) + "\n",
    "Dockerfile": `FROM node:22-alpine AS deps\nWORKDIR /app\nCOPY package*.json ./\nRUN npm ci --omit=dev || npm install --omit=dev\n\nFROM node:22-alpine AS build\nWORKDIR /app\nCOPY package*.json ./\nRUN npm ci || npm install\nCOPY tsconfig.json ./\nCOPY src ./src\nRUN npm run build\n\nFROM node:22-alpine AS runtime\nWORKDIR /app\nENV NODE_ENV=production\nCOPY --from=deps /app/node_modules ./node_modules\nCOPY --from=build /app/dist ./dist\nCOPY package.json ./package.json\nCMD ["node", "dist/index.js"]\n`,
    ".dockerignore": "node_modules\ndist\n.env\n*.log\n",
    "README.md": `# ${project.name}

Generated bot project for botBlade. Configure DISCORD_TOKEN through a secret reference or your local shell environment; no secret values are written here.

## Run manually

\`\`\`bash
npm ci
npm run build
DISCORD_TOKEN=<placeholder-from-secret-manager> npm start
\`\`\`

## Install through a registry mirror

This project includes a generated package-lock.json so npm can perform a reproducible install with \`npm ci\`. If the public npm registry returns \`403 Forbidden\` in your network, point npm at your approved corporate mirror before installing:

\`\`\`bash
NPM_CONFIG_REGISTRY=https://npm.example.corp/repository/npm/ npm ci
\`\`\`

You can also persist the mirror in a local, uncommitted \`.npmrc\` file:

\`\`\`ini
registry=https://npm.example.corp/repository/npm/
\`\`\`

Keep authentication tokens in your shell, CI secret store, or user-level npm config; do not commit registry credentials to this generated bot.

## Validate config

\`npm test\` includes a config validation test that confirms the bot fails clearly when DISCORD_TOKEN is missing.
`,
    ".env.example": `DISCORD_TOKEN=<secret reference resolved at runtime>\n${requireApplicationId ? "DISCORD_APPLICATION_ID=<discord app id>\n" : ""}${requireGuildId ? "DISCORD_GUILD_ID=<discord guild id>\n" : ""}`,
    "src/node-env.d.ts": `declare const process: { env: Record<string, string | undefined> };\n\ndeclare namespace NodeJS {\n  interface ProcessEnv {\n    [key: string]: string | undefined;\n  }\n}\n\ndeclare module "node:test" {\n  const test: (name: string, fn: () => void | Promise<void>) => void;\n  export default test;\n}\n\ndeclare module "node:assert/strict" {\n  const assert: {\n    ok(value: unknown, message?: string): void;\n    throws(fn: () => unknown, expected?: RegExp): void;\n  };\n  export default assert;\n}\n`,
    "src/config.ts": `export interface BotConfig {\n  discordToken: string;\n  discordApplicationId: string;\n  discordGuildId: string | null;\n}\n\nexport function loadConfig(env: NodeJS.ProcessEnv = process.env): BotConfig {\n  const discordToken = env.DISCORD_TOKEN;\n  if (!discordToken) {\n    throw new Error("DISCORD_TOKEN is required. Configure discord.tokenSecretRef in botBlade or set DISCORD_TOKEN before starting the bot.");\n  }\n  const discordApplicationId = env.DISCORD_APPLICATION_ID;\n  if (!discordApplicationId) {\n    throw new Error("DISCORD_APPLICATION_ID is required for command registration.");\n  }\n  const commandRegistration = env.DISCORD_COMMAND_REGISTRATION ?? ${JSON.stringify(project.discord.commandRegistration)};\n  const discordGuildId = env.DISCORD_GUILD_ID ?? null;\n  if (commandRegistration === "guild" && !discordGuildId) {\n    throw new Error("DISCORD_GUILD_ID is required when command registration mode is guild.");\n  }\n  return { discordToken, discordApplicationId, discordGuildId };\n}\n`,
    "src/config.test.ts": `import test from "node:test";\nimport assert from "node:assert/strict";\nimport { loadConfig } from "./config.js";\n\ntest("config validation fails without required env vars", () => {\n  assert.throws(() => loadConfig({}), /DISCORD_TOKEN is required/);\n});\n`,
    "src/commands/load.test.ts": `${commandTestImports}\nimport test from "node:test";\nimport assert from "node:assert/strict";\n\nconst commands = [${commandArray}];\n\ntest("command modules load", () => {\n  assert.ok(commands.length > 0);\n  assert.ok(commands.every((command) => command.data.name));\n});\n\ntest("command payloads are serializable for registration", () => {\n  const payload = commands.map((command) => command.data);\n  const serialized = JSON.stringify(payload);\n  assert.ok(serialized.includes(commands[0].data.name));\n});\n`,
    "src/register-commands.ts": `${commandImports}\nimport { REST, Routes } from "discord.js";\nimport { loadConfig } from "./config.js";\n\nconst commands = [${commandArray}].map((command) => command.data);\nconst config = loadConfig();\nconst registrationMode = process.env.DISCORD_COMMAND_REGISTRATION ?? ${JSON.stringify(project.discord.commandRegistration)};\n\nasync function registerCommands(): Promise<void> {\n  const rest = new REST({ version: "10" }).setToken(config.discordToken);\n  const route = registrationMode === "guild"\n    ? Routes.applicationGuildCommands(config.discordApplicationId, config.discordGuildId ?? "")\n    : Routes.applicationCommands(config.discordApplicationId);\n  await rest.put(route, { body: commands });\n  console.log(\`Registered \${commands.length} command(s) in ${registrationModeMessage} mode.\`);\n}\n\nregisterCommands().catch((error) => {\n  console.error("Failed to register commands", error);\n  process.exitCode = 1;\n});\n`,
    "src/index.ts": `${commandImports}\nimport { Client, GatewayIntentBits } from "discord.js";\nimport { loadConfig } from "./config.js";\n\nconst commands = [${commandArray}];\nconst config = loadConfig();\n\nconst client = new Client({ intents: [${intents.map((intent) => `GatewayIntentBits.${intent}`).join(", ")}] });\nclient.once("ready", () => {\n  console.log(\`Bot logged in with \${commands.length} command definition(s).\`);\n});\nclient.login(config.discordToken);\n`,
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
  return ["package.json", "package-lock.json", "tsconfig.json", "README.md", ".env.example", "Dockerfile", ".dockerignore"].includes(normalizeRelativePath(relativePath)) || normalizeRelativePath(relativePath).startsWith("src/");
}
