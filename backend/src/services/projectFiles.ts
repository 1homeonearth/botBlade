// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import fs from "node:fs/promises";  // line 7: executes this statement as part of this file's behavior
import path from "node:path";  // line 8: executes this statement as part of this file's behavior
import type { BotProject } from "../models/project.js";  // line 9: executes this statement as part of this file's behavior
import { RequestValidationError } from "./projectStore.js";  // line 10: executes this statement as part of this file's behavior
import { generatedBotPackageLock } from "./generatedBotLockfile.js";  // line 11: executes this statement as part of this file's behavior

export interface ProjectFileSummary {  // line 13: executes this statement as part of this file's behavior
  path: string;  // line 14: executes this statement as part of this file's behavior
  size: number;  // line 15: executes this statement as part of this file's behavior
  updatedAt: string;  // line 16: executes this statement as part of this file's behavior
  generated: boolean;  // line 17: executes this statement as part of this file's behavior
  editable: boolean;  // line 18: executes this statement as part of this file's behavior
}  // line 19: executes this statement as part of this file's behavior

export interface ProjectFileContent extends ProjectFileSummary {  // line 21: executes this statement as part of this file's behavior
  content: string;  // line 22: executes this statement as part of this file's behavior
}  // line 23: executes this statement as part of this file's behavior

export interface GeneratedWorkspacePaths {  // line 25: executes this statement as part of this file's behavior
  root: string;  // line 26: executes this statement as part of this file's behavior
  workspace: string;  // line 27: executes this statement as part of this file's behavior
}  // line 28: executes this statement as part of this file's behavior

export class ProjectFileService {  // line 30: executes this statement as part of this file's behavior
  constructor(private readonly rootDir = path.join(process.cwd(), "generated-projects")) {}  // line 31: executes this statement as part of this file's behavior

  async generate(project: BotProject, force = false): Promise<{ projectId: string; generatedAt: string; files: ProjectFileSummary[] }> {  // line 33: executes this statement as part of this file's behavior
    const workspace = this.workspace(project.id);  // line 34: executes this statement as part of this file's behavior
    await fs.mkdir(workspace, { recursive: true });  // line 35: executes this statement as part of this file's behavior
    const files = templateFiles(project);  // line 36: executes this statement as part of this file's behavior
    for (const [relativePath, content] of Object.entries(files)) {  // line 37: executes this statement as part of this file's behavior
      const target = this.safePath(project.id, relativePath);  // line 38: executes this statement as part of this file's behavior
      if (!force && await exists(target)) continue;  // line 39: executes this statement as part of this file's behavior
      await fs.mkdir(path.dirname(target), { recursive: true });  // line 40: executes this statement as part of this file's behavior
      await fs.writeFile(target, content, "utf8");  // line 41: executes this statement as part of this file's behavior
    }  // line 42: executes this statement as part of this file's behavior
    return { projectId: project.id, generatedAt: new Date().toISOString(), files: await this.list(project.id) };  // line 43: executes this statement as part of this file's behavior
  }  // line 44: executes this statement as part of this file's behavior

  async ensureGenerated(project: BotProject): Promise<void> {  // line 46: executes this statement as part of this file's behavior
    const workspace = this.workspace(project.id);  // line 47: executes this statement as part of this file's behavior
    if (!await exists(path.join(workspace, "package.json"))) await this.generate(project, false);  // line 48: executes this statement as part of this file's behavior
  }  // line 49: executes this statement as part of this file's behavior

  async hasGenerated(projectId: string): Promise<boolean> {  // line 51: executes this statement as part of this file's behavior
    return exists(path.join(this.workspace(projectId), "package.json"));  // line 52: executes this statement as part of this file's behavior
  }  // line 53: executes this statement as part of this file's behavior

  async list(projectId: string): Promise<ProjectFileSummary[]> {  // line 55: executes this statement as part of this file's behavior
    const workspace = this.workspace(projectId);  // line 56: executes this statement as part of this file's behavior
    if (!await exists(workspace)) return [];  // line 57: executes this statement as part of this file's behavior
    const output: ProjectFileSummary[] = [];  // line 58: executes this statement as part of this file's behavior
    await walk(workspace, workspace, output);  // line 59: executes this statement as part of this file's behavior
    return output.sort((a, b) => a.path.localeCompare(b.path));  // line 60: executes this statement as part of this file's behavior
  }  // line 61: executes this statement as part of this file's behavior

  async read(projectId: string, relativePath: string): Promise<ProjectFileContent> {  // line 63: executes this statement as part of this file's behavior
    const target = this.safePath(projectId, relativePath);  // line 64: executes this statement as part of this file's behavior
    const stat = await fs.stat(target).catch(() => undefined);  // line 65: executes this statement as part of this file's behavior
    if (!stat || !stat.isFile()) throw { statusCode: 404, code: "FILE_NOT_FOUND", message: "Project file was not found.", details: { path: relativePath } };  // line 66: executes this statement as part of this file's behavior
    const content = await fs.readFile(target, "utf8");  // line 67: executes this statement as part of this file's behavior
    return { path: normalizeRelativePath(relativePath), size: stat.size, updatedAt: stat.mtime.toISOString(), generated: isGeneratedPath(relativePath), editable: true, content };  // line 68: executes this statement as part of this file's behavior
  }  // line 69: executes this statement as part of this file's behavior

  async write(projectId: string, relativePath: string, content: string): Promise<ProjectFileContent> {  // line 71: executes this statement as part of this file's behavior
    if (typeof content !== "string") throw new RequestValidationError([{ field: "content", message: "File content must be a string." }]);  // line 72: executes this statement as part of this file's behavior
    const target = this.safePath(projectId, relativePath);  // line 73: executes this statement as part of this file's behavior
    await fs.mkdir(path.dirname(target), { recursive: true });  // line 74: executes this statement as part of this file's behavior
    await fs.writeFile(target, content, "utf8");  // line 75: executes this statement as part of this file's behavior
    return this.read(projectId, relativePath);  // line 76: executes this statement as part of this file's behavior
  }  // line 77: executes this statement as part of this file's behavior

  generatedRoot(): string {  // line 79: executes this statement as part of this file's behavior
    return path.resolve(this.rootDir);  // line 80: executes this statement as part of this file's behavior
  }  // line 81: executes this statement as part of this file's behavior

  workspace(projectId: string): string {  // line 83: executes this statement as part of this file's behavior
    return path.join(this.rootDir, sanitizeId(projectId));  // line 84: executes this statement as part of this file's behavior
  }  // line 85: executes this statement as part of this file's behavior

  resolveWorkspace(projectId: string): GeneratedWorkspacePaths {  // line 87: executes this statement as part of this file's behavior
    return { root: this.generatedRoot(), workspace: path.resolve(this.workspace(projectId)) };  // line 88: executes this statement as part of this file's behavior
  }  // line 89: executes this statement as part of this file's behavior

  safePath(projectId: string, relativePath: string): string {  // line 91: executes this statement as part of this file's behavior
    const normalized = normalizeRelativePath(relativePath);  // line 92: executes this statement as part of this file's behavior
    if (!normalized) throw new RequestValidationError([{ field: "path", message: "File path is required." }]);  // line 93: executes this statement as part of this file's behavior
    const workspace = this.workspace(projectId);  // line 94: executes this statement as part of this file's behavior
    const target = path.resolve(workspace, normalized);  // line 95: executes this statement as part of this file's behavior
    const workspaceResolved = path.resolve(workspace);  // line 96: executes this statement as part of this file's behavior
    if (!target.startsWith(workspaceResolved + path.sep) && target !== workspaceResolved) {  // line 97: executes this statement as part of this file's behavior
      throw { statusCode: 400, code: "INVALID_FILE_PATH", message: "File path must stay within the project workspace.", details: { path: relativePath } };  // line 98: executes this statement as part of this file's behavior
    }  // line 99: executes this statement as part of this file's behavior
    return target;  // line 100: executes this statement as part of this file's behavior
  }  // line 101: executes this statement as part of this file's behavior
}  // line 102: executes this statement as part of this file's behavior

export function parseFileWriteInput(value: unknown): { content: string } {  // line 104: executes this statement as part of this file's behavior
  const maxBytes = 512 * 1024;  // line 105: executes this statement as part of this file's behavior
  const object = value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};  // line 106: executes this statement as part of this file's behavior
  if (typeof object.content !== "string") throw new RequestValidationError([{ field: "content", message: "File content must be a string." }]);  // line 107: executes this statement as part of this file's behavior
  if (Buffer.byteLength(object.content, "utf8") > maxBytes) throw new RequestValidationError([{ field: "content", message: "File content exceeds 512KB limit." }]);  // line 108: executes this statement as part of this file's behavior
  return { content: object.content };  // line 109: executes this statement as part of this file's behavior
}  // line 110: executes this statement as part of this file's behavior

export function templateFiles(project: BotProject): Record<string, string> {  // line 112: executes this statement as part of this file's behavior
  const intents = project.permissions.intents.length > 0 ? project.permissions.intents : ["Guilds"];  // line 113: executes this statement as part of this file's behavior
  const commands = project.commands.length > 0 ? project.commands : [{  // line 114: executes this statement as part of this file's behavior
    id: "cmd_ping",  // line 115: executes this statement as part of this file's behavior
    name: "ping",  // line 116: executes this statement as part of this file's behavior
    description: "Replies with pong.",  // line 117: executes this statement as part of this file's behavior
    type: "chat_input" as const,  // line 118: executes this statement as part of this file's behavior
    options: [],  // line 119: executes this statement as part of this file's behavior
    permissions: { defaultMemberPermissions: null, dmPermission: false },  // line 120: executes this statement as part of this file's behavior
    handler: { kind: "static_response" as const, ephemeral: true, content: "Pong." },  // line 121: executes this statement as part of this file's behavior
  }];  // line 122: executes this statement as part of this file's behavior
  const commandImports = commands.map((command) => `import { ${commandExportName(command.name)} } from "./commands/${sanitizeCommandFile(command.name)}.js";`).join("\n");  // line 123: executes this statement as part of this file's behavior
  const commandTestImports = commands.map((command) => `import { ${commandExportName(command.name)} } from "./${sanitizeCommandFile(command.name)}.js";`).join("\n");  // line 124: executes this statement as part of this file's behavior
  const commandArray = commands.map((command) => commandExportName(command.name)).join(", ");  // line 125: executes this statement as part of this file's behavior
  const requireApplicationId = project.discord.commandRegistration === "global" || project.discord.commandRegistration === "guild";  // line 126: executes this statement as part of this file's behavior
  const requireGuildId = project.discord.commandRegistration === "guild";  // line 127: executes this statement as part of this file's behavior
  const registrationModeMessage = project.discord.commandRegistration === "guild" ? "guild" : "global";  // line 128: executes this statement as part of this file's behavior

  const files: Record<string, string> = {  // line 130: executes this statement as part of this file's behavior
    "package.json": JSON.stringify({  // line 131: executes this statement as part of this file's behavior
      name: project.slug,  // line 132: executes this statement as part of this file's behavior
      version: "0.1.0",  // line 133: executes this statement as part of this file's behavior
      private: true,  // line 134: executes this statement as part of this file's behavior
      type: "module",  // line 135: executes this statement as part of this file's behavior
      scripts: {  // line 136: executes this statement as part of this file's behavior
        build: "tsc -p tsconfig.json",  // line 137: executes this statement as part of this file's behavior
        start: "node dist/index.js",  // line 138: executes this statement as part of this file's behavior
        test: "node --test dist/**/*.test.js",  // line 139: executes this statement as part of this file's behavior
        "register:commands": "node dist/register-commands.js",  // line 140: executes this statement as part of this file's behavior
        "register:commands:guild": "DISCORD_COMMAND_REGISTRATION=guild node dist/register-commands.js",  // line 141: executes this statement as part of this file's behavior
        "register:commands:global": "DISCORD_COMMAND_REGISTRATION=global node dist/register-commands.js",  // line 142: executes this statement as part of this file's behavior
      },  // line 143: executes this statement as part of this file's behavior
      dependencies: { "discord.js": "14.15.3" },  // line 144: executes this statement as part of this file's behavior
      devDependencies: { typescript: "^5.4.5" },  // line 145: executes this statement as part of this file's behavior
    }, null, 2) + "\n",  // line 146: executes this statement as part of this file's behavior
    "package-lock.json": generatedBotPackageLock(project.slug),  // line 147: executes this statement as part of this file's behavior
    "tsconfig.json": JSON.stringify({ compilerOptions: { target: "ES2022", module: "NodeNext", moduleResolution: "NodeNext", outDir: "dist", rootDir: "src", strict: true, skipLibCheck: true }, include: ["src/**/*.ts"] }, null, 2) + "\n",  // line 148: executes this statement as part of this file's behavior
    "Dockerfile": `FROM node:22-alpine AS deps\nWORKDIR /app\nCOPY package*.json ./\nRUN npm ci --omit=dev || npm install --omit=dev\n\nFROM node:22-alpine AS build\nWORKDIR /app\nCOPY package*.json ./\nRUN npm ci || npm install\nCOPY tsconfig.json ./\nCOPY src ./src\nRUN npm run build\n\nFROM node:22-alpine AS runtime\nWORKDIR /app\nENV NODE_ENV=production\nCOPY --from=deps /app/node_modules ./node_modules\nCOPY --from=build /app/dist ./dist\nCOPY package.json ./package.json\nCMD ["node", "dist/index.js"]\n`,  // line 149: executes this statement as part of this file's behavior
    ".dockerignore": "node_modules\ndist\n.env\n*.log\n",  // line 150: executes this statement as part of this file's behavior
    "README.md": `# ${project.name}  // line 151: executes this statement as part of this file's behavior

Generated bot project for botBlade. Configure DISCORD_TOKEN through a secret reference or your local shell environment; no secret values are written here.  // line 153: executes this statement as part of this file's behavior

## Run manually  // line 155: executes this statement as part of this file's behavior

\`\`\`bash  // line 157: executes this statement as part of this file's behavior
npm ci  // line 158: executes this statement as part of this file's behavior
npm run build  // line 159: executes this statement as part of this file's behavior
DISCORD_TOKEN=<placeholder-from-secret-manager> npm start  // line 160: executes this statement as part of this file's behavior
\`\`\`  // line 161: executes this statement as part of this file's behavior

## Install through a registry mirror  // line 163: executes this statement as part of this file's behavior

This project includes a generated package-lock.json so npm can perform a reproducible install with \`npm ci\`. If the public npm registry returns \`403 Forbidden\` in your network, point npm at your approved corporate mirror before installing:  // line 165: executes this statement as part of this file's behavior

\`\`\`bash  // line 167: executes this statement as part of this file's behavior
NPM_CONFIG_REGISTRY=https://npm.example.corp/repository/npm/ npm ci  // line 168: executes this statement as part of this file's behavior
\`\`\`  // line 169: executes this statement as part of this file's behavior

You can also persist the mirror in a local, uncommitted \`.npmrc\` file:  // line 171: executes this statement as part of this file's behavior

\`\`\`ini  // line 173: executes this statement as part of this file's behavior
registry=https://npm.example.corp/repository/npm/  // line 174: executes this statement as part of this file's behavior
\`\`\`  // line 175: executes this statement as part of this file's behavior

Keep authentication tokens in your shell, CI secret store, or user-level npm config; do not commit registry credentials to this generated bot.  // line 177: executes this statement as part of this file's behavior

## Validate config  // line 179: executes this statement as part of this file's behavior

\`npm test\` includes a config validation test that confirms the bot fails clearly when DISCORD_TOKEN is missing.  // line 181: executes this statement as part of this file's behavior
`,  // line 182: executes this statement as part of this file's behavior
    ".env.example": `DISCORD_TOKEN=<secret reference resolved at runtime>\n${requireApplicationId ? "DISCORD_APPLICATION_ID=<discord app id>\n" : ""}${requireGuildId ? "DISCORD_GUILD_ID=<discord guild id>\n" : ""}`,  // line 183: executes this statement as part of this file's behavior
    "src/node-env.d.ts": `declare const process: { env: Record<string, string | undefined> };\n\ndeclare namespace NodeJS {\n  interface ProcessEnv {\n    [key: string]: string | undefined;\n  }\n}\n\ndeclare module "node:test" {\n  const test: (name: string, fn: () => void | Promise<void>) => void;\n  export default test;\n}\n\ndeclare module "node:assert/strict" {\n  const assert: {\n    ok(value: unknown, message?: string): void;\n    throws(fn: () => unknown, expected?: RegExp): void;\n  };\n  export default assert;\n}\n`,  // line 184: executes this statement as part of this file's behavior
    "src/config.ts": `export interface BotConfig {\n  discordToken: string;\n  discordApplicationId: string;\n  discordGuildId: string | null;\n}\n\nexport function loadConfig(env: NodeJS.ProcessEnv = process.env): BotConfig {\n  const discordToken = env.DISCORD_TOKEN;\n  if (!discordToken) {\n    throw new Error("DISCORD_TOKEN is required. Configure discord.tokenSecretRef in botBlade or set DISCORD_TOKEN before starting the bot.");\n  }\n  const discordApplicationId = env.DISCORD_APPLICATION_ID;\n  if (!discordApplicationId) {\n    throw new Error("DISCORD_APPLICATION_ID is required for command registration.");\n  }\n  const commandRegistration = env.DISCORD_COMMAND_REGISTRATION ?? ${JSON.stringify(project.discord.commandRegistration)};\n  const discordGuildId = env.DISCORD_GUILD_ID ?? null;\n  if (commandRegistration === "guild" && !discordGuildId) {\n    throw new Error("DISCORD_GUILD_ID is required when command registration mode is guild.");\n  }\n  return { discordToken, discordApplicationId, discordGuildId };\n}\n`,  // line 185: executes this statement as part of this file's behavior
    "src/config.test.ts": `import test from "node:test";\nimport assert from "node:assert/strict";\nimport { loadConfig } from "./config.js";\n\ntest("config validation fails without required env vars", () => {\n  assert.throws(() => loadConfig({}), /DISCORD_TOKEN is required/);\n});\n`,  // line 186: executes this statement as part of this file's behavior
    "src/commands/load.test.ts": `${commandTestImports}\nimport test from "node:test";\nimport assert from "node:assert/strict";\n\nconst commands = [${commandArray}];\n\ntest("command modules load", () => {\n  assert.ok(commands.length > 0);\n  assert.ok(commands.every((command) => command.data.name));\n});\n\ntest("command payloads are serializable for registration", () => {\n  const payload = commands.map((command) => command.data);\n  const serialized = JSON.stringify(payload);\n  assert.ok(serialized.includes(commands[0].data.name));\n});\n`,  // line 187: executes this statement as part of this file's behavior
    "src/register-commands.ts": `${commandImports}\nimport { REST, Routes } from "discord.js";\nimport { loadConfig } from "./config.js";\n\nconst commands = [${commandArray}].map((command) => command.data);\nconst config = loadConfig();\nconst registrationMode = process.env.DISCORD_COMMAND_REGISTRATION ?? ${JSON.stringify(project.discord.commandRegistration)};\n\nasync function registerCommands(): Promise<void> {\n  const rest = new REST({ version: "10" }).setToken(config.discordToken);\n  const route = registrationMode === "guild"\n    ? Routes.applicationGuildCommands(config.discordApplicationId, config.discordGuildId ?? "")\n    : Routes.applicationCommands(config.discordApplicationId);\n  await rest.put(route, { body: commands });\n  console.log(\`Registered \${commands.length} command(s) in ${registrationModeMessage} mode.\`);\n}\n\nregisterCommands().catch((error) => {\n  console.error("Failed to register commands", error);\n  process.exitCode = 1;\n});\n`,  // line 188: executes this statement as part of this file's behavior
    "src/index.ts": `${commandImports}\nimport { Client, GatewayIntentBits } from "discord.js";\nimport { loadConfig } from "./config.js";\n\nconst commands = [${commandArray}];\nconst config = loadConfig();\n\nconst client = new Client({ intents: [${intents.map((intent) => `GatewayIntentBits.${intent}`).join(", ")}] });\nclient.once("ready", () => {\n  console.log(\`Bot logged in with \${commands.length} command definition(s).\`);\n});\nclient.login(config.discordToken);\n`,  // line 189: executes this statement as part of this file's behavior
  };  // line 190: executes this statement as part of this file's behavior
  for (const command of commands) files[`src/commands/${sanitizeCommandFile(command.name)}.ts`] = commandFile(command);  // line 191: executes this statement as part of this file's behavior
  return files;  // line 192: executes this statement as part of this file's behavior
}  // line 193: executes this statement as part of this file's behavior

function commandFile(command: BotProject["commands"][number]): string {  // line 195: executes this statement as part of this file's behavior
  const response = typeof command.handler === "object" && command.handler.kind === "static_response" ? command.handler.content ?? "" : "TODO: implement custom TypeScript handler.";  // line 196: executes this statement as part of this file's behavior
  return `export const ${commandExportName(command.name)} = {\n  data: {\n    name: ${JSON.stringify(command.name)},\n    description: ${JSON.stringify(command.description)},\n    type: ${JSON.stringify(command.type ?? "chat_input")},\n    options: ${JSON.stringify(command.options ?? [])},\n    defaultMemberPermissions: ${JSON.stringify(command.permissions?.defaultMemberPermissions ?? null)},\n    dmPermission: ${JSON.stringify(command.permissions?.dmPermission ?? false)}\n  },\n  handler: {\n    kind: ${JSON.stringify(typeof command.handler === "object" ? command.handler.kind : "custom_typescript_placeholder")},\n    ephemeral: ${JSON.stringify(Boolean(typeof command.handler === "object" ? command.handler.ephemeral : false))},\n    content: ${JSON.stringify(response)}\n  }\n};\n`;  // line 197: executes this statement as part of this file's behavior
}  // line 198: executes this statement as part of this file's behavior

function sanitizeCommandFile(name: string): string {  // line 200: executes this statement as part of this file's behavior
  return name.replace(/[^a-z0-9_-]/g, "_");  // line 201: executes this statement as part of this file's behavior
}  // line 202: executes this statement as part of this file's behavior

function commandExportName(name: string): string {  // line 204: executes this statement as part of this file's behavior
  return `${sanitizeCommandFile(name).replace(/(^|[-_])(\w)/g, (_match, _prefix, letter: string) => letter.toUpperCase())}Command`;  // line 205: executes this statement as part of this file's behavior
}  // line 206: executes this statement as part of this file's behavior

async function walk(root: string, current: string, output: ProjectFileSummary[]): Promise<void> {  // line 208: executes this statement as part of this file's behavior
  const entries = await fs.readdir(current, { withFileTypes: true });  // line 209: executes this statement as part of this file's behavior
  for (const entry of entries) {  // line 210: executes this statement as part of this file's behavior
    if (entry.name === "node_modules" || entry.name === "dist") continue;  // line 211: executes this statement as part of this file's behavior
    const full = path.join(current, entry.name);  // line 212: executes this statement as part of this file's behavior
    if (entry.isDirectory()) {  // line 213: executes this statement as part of this file's behavior
      await walk(root, full, output);  // line 214: executes this statement as part of this file's behavior
    } else if (entry.isFile()) {  // line 215: executes this statement as part of this file's behavior
      const stat = await fs.stat(full);  // line 216: executes this statement as part of this file's behavior
      const relative = path.relative(root, full).split(path.sep).join("/");  // line 217: executes this statement as part of this file's behavior
      output.push({ path: relative, size: stat.size, updatedAt: stat.mtime.toISOString(), generated: isGeneratedPath(relative), editable: true });  // line 218: executes this statement as part of this file's behavior
    }  // line 219: executes this statement as part of this file's behavior
  }  // line 220: executes this statement as part of this file's behavior
}  // line 221: executes this statement as part of this file's behavior

async function exists(filePath: string): Promise<boolean> {  // line 223: executes this statement as part of this file's behavior
  return fs.access(filePath).then(() => true, () => false);  // line 224: executes this statement as part of this file's behavior
}  // line 225: executes this statement as part of this file's behavior

function normalizeRelativePath(value: string): string {  // line 227: executes this statement as part of this file's behavior
  return decodeURIComponent(value).replace(/\\/g, "/").replace(/^\/+/, "");  // line 228: executes this statement as part of this file's behavior
}  // line 229: executes this statement as part of this file's behavior

function sanitizeId(value: string): string {  // line 231: executes this statement as part of this file's behavior
  return value.replace(/[^A-Za-z0-9_-]/g, "_");  // line 232: executes this statement as part of this file's behavior
}  // line 233: executes this statement as part of this file's behavior

function isGeneratedPath(relativePath: string): boolean {  // line 235: executes this statement as part of this file's behavior
  return ["package.json", "package-lock.json", "tsconfig.json", "README.md", ".env.example", "Dockerfile", ".dockerignore"].includes(normalizeRelativePath(relativePath)) || normalizeRelativePath(relativePath).startsWith("src/");  // line 236: executes this statement as part of this file's behavior
}  // line 237: executes this statement as part of this file's behavior
