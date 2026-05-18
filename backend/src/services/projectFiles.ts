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
  const defaultRegistrationMode = project.discord.commandRegistration === "global" ? "global" : "guild";
  const envExample = [
    "DISCORD_TOKEN=<secret reference resolved at runtime>",
    "DISCORD_APPLICATION_ID=<your Discord application ID>",
    ...(defaultRegistrationMode === "guild" ? ["DISCORD_GUILD_ID=<guild ID for guild command registration>"] : ["# DISCORD_GUILD_ID=<optional guild ID for register:guild>"]),
  ].join("\n") + "\n";
  const files: Record<string, string> = {
    "package.json": JSON.stringify({
      name: project.slug,
      version: "0.1.0",
      private: true,
      type: "module",
      scripts: { build: "tsc -p tsconfig.json", start: "node dist/index.js", test: "node --test dist/*.test.js dist/**/*.test.js", register: "npm run build && node dist/register-commands.js", "register:guild": "npm run build && COMMAND_REGISTRATION=guild node dist/register-commands.js", "register:global": "npm run build && COMMAND_REGISTRATION=global node dist/register-commands.js" },
      dependencies: { "discord.js": "14.15.3" },
      devDependencies: { typescript: "^5.4.5" },
    }, null, 2) + "\n",
    "package-lock.json": generatedBotPackageLock(project.slug),
    "tsconfig.json": JSON.stringify({ compilerOptions: { target: "ES2022", module: "NodeNext", moduleResolution: "NodeNext", outDir: "dist", rootDir: "src", strict: true, skipLibCheck: true }, include: ["src/**/*.ts"] }, null, 2) + "\n",
    "Dockerfile": `FROM node:22-alpine AS deps\nWORKDIR /app\nCOPY package*.json ./\nRUN npm ci --omit=dev || npm install --omit=dev\n\nFROM node:22-alpine AS build\nWORKDIR /app\nCOPY package*.json ./\nRUN npm ci || npm install\nCOPY tsconfig.json ./\nCOPY src ./src\nRUN npm run build\n\nFROM node:22-alpine AS runtime\nWORKDIR /app\nENV NODE_ENV=production\nCOPY --from=deps /app/node_modules ./node_modules\nCOPY --from=build /app/dist ./dist\nCOPY package.json ./package.json\nCMD ["node", "dist/index.js"]\n`,
    ".dockerignore": "node_modules\ndist\n.env\n*.log\n",
    "README.md": `# ${project.name}

Generated bot project for royalScepter. Configure DISCORD_TOKEN through a secret reference or your local shell environment; no secret values are written here.

## Run manually

\`\`\`bash
npm ci
npm run build
DISCORD_TOKEN=<placeholder-from-secret-manager> npm start
\`\`\`

## Register slash commands

This project includes a Discord REST registration entrypoint generated from the project command list. The default \`npm run register\` mode is \`${defaultRegistrationMode}\`, matching this project's royalScepter command registration setting.

\`\`\`bash
DISCORD_TOKEN=<placeholder-from-secret-manager> \
DISCORD_APPLICATION_ID=<discord-application-id> \
${defaultRegistrationMode === "guild" ? "DISCORD_GUILD_ID=<discord-guild-id> \
" : ""}npm run register
\`\`\`

Use \`npm run register:guild\` for fast guild registration; it requires \`DISCORD_GUILD_ID\`. Use \`npm run register:global\` for global registration; global Discord commands can take longer to propagate.

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

\`npm test\` includes config validation and command serialization tests so command JSON problems are caught before registration.
`,
    ".env.example": envExample,
    "src/node-env.d.ts": `declare const process: { env: Record<string, string | undefined> };\n\ndeclare namespace NodeJS {\n  interface ProcessEnv {\n    [key: string]: string | undefined;\n  }\n}\n\ndeclare module "node:test" {\n  const test: (name: string, fn: () => void | Promise<void>) => void;\n  export default test;\n}\n\ndeclare module "node:assert/strict" {\n  const assert: {\n    ok(value: unknown, message?: string): void;\n    equal(actual: unknown, expected: unknown, message?: string): void;\n    throws(fn: () => unknown, expected?: RegExp): void;\n  };\n  export default assert;\n}\n`,
    "src/config.ts": `export type CommandRegistrationMode = "guild" | "global";

export interface BotConfig {
  discordToken: string;
  applicationId?: string;
  guildId?: string;
  commandRegistration: CommandRegistrationMode;
}

export function loadConfig(env: NodeJS.ProcessEnv = process.env, registrationOverride?: string, requireCommandRegistrationConfig = false): BotConfig {
  const discordToken = env.DISCORD_TOKEN;
  if (!discordToken) {
    throw new Error("DISCORD_TOKEN is required. Configure discord.tokenSecretRef in royalScepter or set DISCORD_TOKEN before starting the bot.");
  }
  const commandRegistration = parseCommandRegistration(registrationOverride ?? env.COMMAND_REGISTRATION ?? "${defaultRegistrationMode}");
  const applicationId = env.DISCORD_APPLICATION_ID;
  if (requireCommandRegistrationConfig && !applicationId) {
    throw new Error("DISCORD_APPLICATION_ID is required before registering commands.");
  }
  const guildId = env.DISCORD_GUILD_ID;
  if (requireCommandRegistrationConfig && commandRegistration === "guild" && !guildId) {
    throw new Error("DISCORD_GUILD_ID is required for guild command registration.");
  }
  return { discordToken, applicationId, guildId, commandRegistration };
}

function parseCommandRegistration(value: string): CommandRegistrationMode {
  if (value === "guild" || value === "global") return value;
  throw new Error("COMMAND_REGISTRATION must be guild or global.");
}
`,
    "src/config.test.ts": `import test from "node:test";
import assert from "node:assert/strict";
import { loadConfig } from "./config.js";

test("config validation fails without required env vars", () => {
  assert.throws(() => loadConfig({}), /DISCORD_TOKEN is required/);
});

test("config validation requires Discord application and guild IDs for guild registration", () => {
  assert.throws(() => loadConfig({ DISCORD_TOKEN: "token" }, undefined, true), /DISCORD_APPLICATION_ID is required/);
  assert.throws(() => loadConfig({ DISCORD_TOKEN: "token", DISCORD_APPLICATION_ID: "123" }, "guild", true), /DISCORD_GUILD_ID is required/);
});

test("config validation allows global registration without a guild ID", () => {
  const config = loadConfig({ DISCORD_TOKEN: "token", DISCORD_APPLICATION_ID: "123" }, "global", true);
  assert.equal(config.commandRegistration, "global");
});
`,
    "src/commands/load.test.ts": `${commandTestImports}
import test from "node:test";
import assert from "node:assert/strict";

const commands = [${commandArray}];

test("command modules load", () => {
  assert.ok(commands.length > 0);
  assert.ok(commands.every((command) => command.data.name));
});

test("command JSON can be serialized", () => {
  const json = commands.map((command) => command.data);
  const serialized = JSON.stringify(json);
  const parsed = JSON.parse(serialized) as Array<{ name: string }>;
  assert.equal(parsed.length, commands.length);
  assert.ok(parsed.every((command) => command.name));
});
`,
    "src/index.ts": `${commandImports}
import { Client, GatewayIntentBits } from "discord.js";
import { loadConfig } from "./config.js";

const commands = [${commandArray}];
const config = loadConfig();

const client = new Client({ intents: [${intents.map((intent) => `GatewayIntentBits.${intent}`).join(", ")}] });
client.once("ready", () => {
  console.log(\`Bot logged in with \${commands.length} command definition(s).\`);
});
client.login(config.discordToken);
`,
    "src/register-commands.ts": `${commandImports}
import { ApplicationCommandOptionType, ApplicationCommandType, REST, Routes } from "discord.js";
import { loadConfig } from "./config.js";

const commands = [${commandArray}];
const config = loadConfig(process.env, undefined, true);
const rest = new REST({ version: "10" }).setToken(config.discordToken);

const commandJson = commands.map((command) => ({
  ...command.data,
  type: ApplicationCommandType.ChatInput,
  options: command.data.options.map((option) => ({
    ...option,
    type: optionType(option.type),
  })),
}));

const route = config.commandRegistration === "guild"
  ? Routes.applicationGuildCommands(config.applicationId ?? "", config.guildId ?? "")
  : Routes.applicationCommands(config.applicationId ?? "");

await rest.put(route, { body: commandJson });
console.log(\`Registered \${commandJson.length} \${config.commandRegistration} command(s).\`);

function optionType(type: string): ApplicationCommandOptionType {
  switch (type) {
    case "string": return ApplicationCommandOptionType.String;
    case "integer": return ApplicationCommandOptionType.Integer;
    case "boolean": return ApplicationCommandOptionType.Boolean;
    case "user": return ApplicationCommandOptionType.User;
    case "channel": return ApplicationCommandOptionType.Channel;
    case "role": return ApplicationCommandOptionType.Role;
    case "mentionable": return ApplicationCommandOptionType.Mentionable;
    case "number": return ApplicationCommandOptionType.Number;
    case "attachment": return ApplicationCommandOptionType.Attachment;
    default: throw new Error(\`Unsupported command option type: \${type}\`);
  }
}
`,
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
