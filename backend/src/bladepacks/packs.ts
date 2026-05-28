import { BladePack, BladePackImportMode, BladePackImportantFilePattern, BladePackRepairRule } from "./schema.js";

const schemaVersion = "2026-05-phase4";
const sharedPanels = ["projectMap", "editor", "logs", "secrets", "git", "health"];
const repositoryImportModes: BladePackImportMode[] = [
  { kind: "repository", notes: "Clone or scan an existing Git repository using static inspection." },
  { kind: "zip", notes: "Import a ZIP archive after path, size, and file-type policy checks." },
  { kind: "folder", notes: "Scan an already-unpacked workspace folder." },
  { kind: "template", notes: "Create a new workspace from a BotBlade starter template." }
];
const nodeImportantFiles: BladePackImportantFilePattern[] = [
  { kind: "packageManifest", pattern: "package.json", label: "Node package manifest", required: true },
  { kind: "config", pattern: "tsconfig.json", label: "TypeScript config" },
  { kind: "config", pattern: ".env.example", label: "Environment example" },
  { kind: "entrypoint", pattern: "src/index.{ts,js}", label: "Primary source entrypoint" }
];
const nodeRepairRules: BladePackRepairRule[] = [
  {
    id: "missing-node-install",
    title: "Install dependencies before running Node bot commands",
    severity: "warning",
    evidencePattern: "Cannot find module|ERR_MODULE_NOT_FOUND|node_modules missing",
    safeAction: "Run the pack install command from the workspace root, then retry the failed command.",
    commandHint: "install",
    affectedFiles: ["package.json", "package-lock.json", "pnpm-lock.yaml", "yarn.lock"]
  }
];

export const BLADE_PACKS: BladePack[] = [
  {
    schemaVersion,
    id: "discord-js",
    name: "Discord.js Bot",
    version: "0.1.0",
    license: "Apache-2.0",
    runtime: { type: "node", versionRange: ">=22", packageManagers: ["npm", "pnpm", "yarn"] },
    detectors: [
      { kind: "packageDependency", name: "discord.js", weight: 50 },
      { kind: "sourceImport", pattern: "from 'discord.js'|require\\('discord.js'\\)", weight: 25 },
      { kind: "envKey", pattern: "DISCORD_TOKEN|DISCORD_CLIENT_ID", weight: 15 },
      { kind: "knownFilename", path: "deploy-commands.js", weight: 10 }
    ],
    templateOptions: [
      { id: "discord-slash-bot", label: "Discord slash bot", description: "Minimal Discord.js slash-command bot.", runtime: { type: "node", versionRange: ">=22" }, backendTemplateRef: "discord-slash-bot", starterFiles: ["package.json", "src/index.ts", "src/register-commands.ts", ".env.example"] },
      { id: "discord-gpt-bot", label: "Discord GPT bot", description: "Discord.js starter wired for an OpenAI-backed response command.", runtime: { type: "node", versionRange: ">=22" }, backendTemplateRef: "discord-gpt-bot", starterFiles: ["package.json", "src/index.ts", ".env.example"] },
      { id: "discord-gemini-bot", label: "Discord Gemini bot", description: "Discord.js starter prepared for Gemini API calls.", runtime: { type: "node", versionRange: ">=22" }, backendTemplateRef: "discord-gemini-bot", starterFiles: ["package.json", "src/index.ts", ".env.example"] }
    ],
    templates: ["discord-slash-bot", "discord-gpt-bot", "discord-gemini-bot"],
    commands: { install: "npm install", build: "npm run build", test: "npm test", validate: "botblade validate", start: "npm start", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: [...nodeImportantFiles, { kind: "commandDirectory", pattern: "src/commands", label: "Slash command modules" }, { kind: "config", pattern: "discord.config.{json,ts,js}", label: "Discord app config" }],
    repairRules: [...nodeRepairRules, { id: "missing-discord-token", title: "Discord token is not configured", severity: "error", evidencePattern: "DISCORD_TOKEN|401 Unauthorized|TOKEN_INVALID", safeAction: "Create a secret reference named DISCORD_TOKEN and rerun validation without writing the token to source files.", commandHint: "validate", affectedFiles: [".env.example", "src/index.ts"] }],
    secretDetectors: [
      { name: "DISCORD_TOKEN", label: "Discord bot token", required: true, sources: ["envExample", "sourceReference", "frameworkConvention"], patterns: ["DISCORD_TOKEN", "process\\.env\\.DISCORD_TOKEN"] },
      { name: "DISCORD_CLIENT_ID", label: "Discord application client ID", required: false, sources: ["envExample", "sourceReference", "frameworkConvention"], patterns: ["DISCORD_CLIENT_ID", "CLIENT_ID"] }
    ],
    secrets: [
      { name: "DISCORD_TOKEN", label: "Discord bot token", required: true, example: "Paste your Discord bot token" },
      { name: "DISCORD_CLIENT_ID", label: "Discord application client ID", required: false }
    ],
    diagnostics: [],
    panels: sharedPanels,
    docs: [{ label: "discord.js guide", url: "https://discord.js.org/docs" }],
    importModes: repositoryImportModes,
    supportedImports: [{ kind: "repository", notes: "TypeScript or JavaScript discord.js bots." }]
  },
  {
    schemaVersion,
    id: "telegraf",
    name: "Telegraf Bot",
    version: "0.1.0",
    license: "MIT",
    runtime: { type: "node", versionRange: ">=22", packageManagers: ["npm", "pnpm", "yarn"] },
    detectors: [
      { kind: "packageDependency", name: "telegraf", weight: 50 },
      { kind: "sourceImport", pattern: "from 'telegraf'|require\\('telegraf'\\)", weight: 25 },
      { kind: "envKey", pattern: "TELEGRAM_BOT_TOKEN|BOT_TOKEN", weight: 15 },
      { kind: "sourceImport", pattern: "bot\\.launch|Composer", weight: 10 }
    ],
    templateOptions: [
      { id: "telegram-command-bot", label: "Telegram command bot", description: "Telegraf command bot starter.", runtime: { type: "node", versionRange: ">=22" }, backendTemplateRef: "telegram-command-bot", starterFiles: ["package.json", "src/index.ts", ".env.example"] },
      { id: "telegram-ai-bot", label: "Telegram AI bot", description: "Telegraf bot starter with an AI response command shape.", runtime: { type: "node", versionRange: ">=22" }, backendTemplateRef: "telegram-ai-bot", starterFiles: ["package.json", "src/index.ts", ".env.example"] }
    ],
    templates: ["telegram-command-bot", "telegram-ai-bot"],
    commands: { install: "npm install", build: "npm run build", test: "npm test", validate: "botblade validate", start: "npm start", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: [...nodeImportantFiles, { kind: "entrypoint", pattern: "bot.{ts,js}", label: "Telegraf bot entrypoint" }],
    repairRules: [...nodeRepairRules, { id: "missing-telegram-token", title: "Telegram token is not configured", severity: "error", evidencePattern: "TELEGRAM_BOT_TOKEN|BOT_TOKEN|401 Unauthorized", safeAction: "Create a secret reference named TELEGRAM_BOT_TOKEN before starting the bot.", commandHint: "validate", affectedFiles: [".env.example", "src/index.ts"] }],
    secretDetectors: [{ name: "TELEGRAM_BOT_TOKEN", label: "Telegram bot token", required: true, sources: ["envExample", "sourceReference", "frameworkConvention"], patterns: ["TELEGRAM_BOT_TOKEN", "BOT_TOKEN", "process\\.env\\.TELEGRAM_BOT_TOKEN"] }],
    secrets: [{ name: "TELEGRAM_BOT_TOKEN", label: "Telegram bot token", required: true }],
    diagnostics: [],
    panels: sharedPanels,
    docs: [{ label: "Telegraf docs", url: "https://telegraf.js.org/" }],
    importModes: repositoryImportModes,
    supportedImports: [{ kind: "repository", notes: "Node Telegraf repositories." }]
  },
  {
    schemaVersion,
    id: "slack-bolt",
    name: "Slack Bolt JS",
    version: "0.1.0",
    license: "MIT",
    runtime: { type: "node", versionRange: ">=22", packageManagers: ["npm", "pnpm", "yarn"] },
    detectors: [
      { kind: "packageDependency", name: "@slack/bolt", weight: 50 },
      { kind: "sourceImport", pattern: "from '@slack/bolt'|require\\('@slack/bolt'\\)", weight: 25 },
      { kind: "envKey", pattern: "SLACK_BOT_TOKEN", weight: 15 },
      { kind: "envKey", pattern: "SLACK_SIGNING_SECRET", weight: 15 }
    ],
    templateOptions: [
      { id: "slack-slash-command", label: "Slack slash command", description: "Slack Bolt slash command starter.", runtime: { type: "node", versionRange: ">=22" }, backendTemplateRef: "slack-slash-command", starterFiles: ["package.json", "src/index.ts", ".env.example"] },
      { id: "slack-ai-assistant", label: "Slack AI assistant", description: "Slack Bolt assistant starter with AI command wiring.", runtime: { type: "node", versionRange: ">=22" }, backendTemplateRef: "slack-ai-assistant", starterFiles: ["package.json", "src/index.ts", ".env.example"] }
    ],
    templates: ["slack-slash-command", "slack-ai-assistant"],
    commands: { install: "npm install", build: "npm run build", test: "npm test", validate: "botblade validate", start: "npm start", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: [...nodeImportantFiles, { kind: "config", pattern: "manifest.{json,yml,yaml}", label: "Slack app manifest" }],
    repairRules: [...nodeRepairRules, { id: "missing-slack-secrets", title: "Slack credentials are not configured", severity: "error", evidencePattern: "SLACK_BOT_TOKEN|SLACK_SIGNING_SECRET|not_authed", safeAction: "Create secret references for Slack credentials and keep raw values out of source files.", commandHint: "validate", affectedFiles: [".env.example", "src/index.ts"] }],
    secretDetectors: [
      { name: "SLACK_BOT_TOKEN", label: "Slack bot token", required: true, sources: ["envExample", "sourceReference", "frameworkConvention"], patterns: ["SLACK_BOT_TOKEN", "process\\.env\\.SLACK_BOT_TOKEN"] },
      { name: "SLACK_SIGNING_SECRET", label: "Slack signing secret", required: true, sources: ["envExample", "sourceReference", "frameworkConvention"], patterns: ["SLACK_SIGNING_SECRET"] },
      { name: "SLACK_APP_TOKEN", label: "Slack app token (socket mode)", required: false, sources: ["envExample", "sourceReference", "frameworkConvention"], patterns: ["SLACK_APP_TOKEN"] }
    ],
    secrets: [
      { name: "SLACK_BOT_TOKEN", label: "Slack bot token", required: true },
      { name: "SLACK_SIGNING_SECRET", label: "Slack signing secret", required: true },
      { name: "SLACK_APP_TOKEN", label: "Slack app token (socket mode)", required: false }
    ],
    diagnostics: [],
    panels: sharedPanels,
    docs: [{ label: "Slack Bolt JS docs", url: "https://slack.dev/bolt-js" }],
    importModes: repositoryImportModes,
    supportedImports: [{ kind: "repository", notes: "Slack Bolt JavaScript repositories." }]
  },
  {
    schemaVersion,
    id: "generic-node",
    name: "Generic Node Project",
    version: "0.1.0",
    license: "MIT",
    runtime: { type: "node", versionRange: ">=22", packageManagers: ["npm", "pnpm", "yarn"] },
    detectors: [
      { kind: "fileExists", path: "package.json", weight: 35 },
      { kind: "packageScript", name: "start", weight: 20 },
      { kind: "knownFilename", path: "package-lock.json", weight: 15 },
      { kind: "knownDirectory", path: "src", weight: 15 },
      { kind: "sourceImport", pattern: "node:|process\\.", weight: 15 }
    ],
    templateOptions: [
      { id: "generic-webhook-receiver", label: "Webhook receiver", description: "Generic Node webhook receiver starter.", runtime: { type: "node", versionRange: ">=22" }, backendTemplateRef: "generic-webhook-receiver", starterFiles: ["package.json", "src/index.ts", ".env.example"] },
      { id: "scheduled-worker", label: "Scheduled worker", description: "Generic Node scheduled worker starter.", runtime: { type: "node", versionRange: ">=22" }, backendTemplateRef: "scheduled-worker", starterFiles: ["package.json", "src/index.ts"] }
    ],
    templates: ["generic-webhook-receiver", "scheduled-worker"],
    commands: { install: "npm install", validate: "botblade validate", start: "npm start", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: nodeImportantFiles,
    repairRules: nodeRepairRules,
    secretDetectors: [],
    secrets: [], diagnostics: [], panels: sharedPanels, docs: [], importModes: repositoryImportModes, supportedImports: [{ kind: "repository", notes: "Generic Node projects." }]
  },
  {
    schemaVersion,
    id: "generic-python",
    name: "Generic Python Project",
    version: "0.1.0",
    license: "MIT",
    runtime: { type: "python", packageManagers: ["pip"] },
    detectors: [
      { kind: "fileExists", path: "requirements.txt", weight: 30 },
      { kind: "fileExists", path: "pyproject.toml", weight: 30 },
      { kind: "knownFilename", path: "main.py", weight: 20 },
      { kind: "knownDirectory", path: ".venv", weight: 15 },
      { kind: "sourceImport", pattern: "aiogram|discord|telegram|openai", weight: 20 }
    ],
    templateOptions: [
      { id: "python-telegram-bot", label: "Python Telegram bot", description: "Python bot starter using a Telegram framework shape.", runtime: { type: "python" }, backendTemplateRef: "python-telegram-bot", starterFiles: ["requirements.txt", "main.py", ".env.example"] },
      { id: "python-discord-bot", label: "Python Discord bot", description: "Python Discord bot starter.", runtime: { type: "python" }, backendTemplateRef: "python-discord-bot", starterFiles: ["requirements.txt", "main.py", ".env.example"] }
    ],
    templates: ["python-telegram-bot", "python-discord-bot"],
    commands: { install: "pip install -r requirements.txt", validate: "botblade validate", start: "python main.py", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: [
      { kind: "packageManifest", pattern: "requirements.txt", label: "pip requirements" },
      { kind: "packageManifest", pattern: "pyproject.toml", label: "Python project manifest" },
      { kind: "entrypoint", pattern: "main.py", label: "Python entrypoint" },
      { kind: "entrypoint", pattern: "src/**/*.py", label: "Python source files" },
      { kind: "config", pattern: ".env.example", label: "Environment example" }
    ],
    repairRules: [{ id: "missing-python-deps", title: "Install Python dependencies before starting", severity: "warning", evidencePattern: "ModuleNotFoundError|No module named", safeAction: "Run the pack install command from the workspace root, then retry the failed command.", commandHint: "install", affectedFiles: ["requirements.txt", "pyproject.toml"] }],
    secretDetectors: [],
    secrets: [], diagnostics: [], panels: sharedPanels, docs: [], importModes: repositoryImportModes, supportedImports: [{ kind: "repository", notes: "Generic Python bots and scripts." }]
  },
  {
    schemaVersion,
    id: "n8n-workflow",
    name: "n8n Workflow Import",
    version: "0.1.0",
    license: "Sustainable-Use-reference",
    runtime: { type: "workflow" },
    detectors: [
      { kind: "jsonShape", file: "workflow.json", keys: ["nodes", "connections"], weight: 60 },
      { kind: "sourceImport", pattern: "\"type\"\\s*:\\s*\"n8n-nodes-base\\.", weight: 20 },
      { kind: "sourceImport", pattern: "credentials", weight: 10 },
      { kind: "sourceImport", pattern: "trigger", weight: 10 }
    ],
    templateOptions: [],
    templates: [],
    commands: { validate: "botblade validate" },
    importantFilePatterns: [
      { kind: "workflow", pattern: "workflow.json", label: "n8n workflow JSON", required: true },
      { kind: "workflow", pattern: "*.workflow.json", label: "Workflow JSON export" },
      { kind: "config", pattern: ".env.example", label: "Environment example" }
    ],
    repairRules: [{ id: "missing-workflow-credentials", title: "Workflow credential references need secret mapping", severity: "warning", evidencePattern: "credentials", safeAction: "Map workflow credential names to BotBlade secret references; do not paste credential values into workflow JSON.", commandHint: "validate", affectedFiles: ["workflow.json"] }],
    secretDetectors: [{ name: "WORKFLOW_CREDENTIALS", label: "Imported workflow credentials", required: false, sources: ["workflowCredentialReference"], patterns: ["credentials", "credentialId", "credentialName"] }],
    secrets: [{ name: "WORKFLOW_CREDENTIALS", label: "Imported workflow credentials", required: false }],
    diagnostics: [],
    panels: ["projectMap", "logs", "health"],
    docs: [{ label: "n8n docs", url: "https://docs.n8n.io/" }],
    importModes: [{ kind: "workflow_json", notes: "Import-only parser for n8n workflow JSON; no embedded n8n runtime." }, { kind: "zip", notes: "Import workflow exports bundled in a ZIP archive after policy checks." }, { kind: "folder", notes: "Scan a folder containing workflow JSON exports." }],
    supportedImports: [{ kind: "workflow-json", notes: "Import-only parser for n8n workflow JSON; no embedded n8n runtime." }]
  },
  {
    schemaVersion,
    id: "botpress",
    name: "Botpress Bot-as-Code",
    version: "0.1.0",
    license: "Botpress-reference",
    runtime: { type: "node", versionRange: ">=22", packageManagers: ["npm", "pnpm", "yarn"] },
    detectors: [
      { kind: "knownDirectory", path: ".botpress", weight: 50 },
      { kind: "packageDependency", name: "@botpress", weight: 25 },
      { kind: "knownFilename", path: "botpress.config.json", weight: 15 },
      { kind: "sourceImport", pattern: "botpress", weight: 10 }
    ],
    templateOptions: [{ id: "botpress-bot-as-code", label: "Botpress bot-as-code", description: "Botpress-style project shell that references user-authored code and config.", runtime: { type: "node", versionRange: ">=22" }, backendTemplateRef: "botpress-bot-as-code", starterFiles: ["package.json", "botpress.config.json", ".env.example"] }],
    templates: ["botpress-bot-as-code"],
    commands: { install: "npm install", build: "npm run build", validate: "botblade validate", start: "npm start", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: [...nodeImportantFiles, { kind: "config", pattern: "botpress.config.json", label: "Botpress config", required: true }, { kind: "workflow", pattern: ".botpress/**/*", label: "Botpress workspace metadata" }],
    repairRules: [...nodeRepairRules, { id: "missing-botpress-token", title: "Botpress token is not configured", severity: "warning", evidencePattern: "BOTPRESS_TOKEN|Unauthorized", safeAction: "Create a BOTPRESS_TOKEN secret reference only if the imported bot requires Botpress cloud access.", commandHint: "validate", affectedFiles: [".env.example", "botpress.config.json"] }],
    secretDetectors: [{ name: "BOTPRESS_TOKEN", label: "Botpress token", required: false, sources: ["envExample", "sourceReference", "frameworkConvention"], patterns: ["BOTPRESS_TOKEN", "process\\.env\\.BOTPRESS_TOKEN"] }],
    secrets: [{ name: "BOTPRESS_TOKEN", label: "Botpress token", required: false }],
    diagnostics: [], panels: sharedPanels,
    docs: [{ label: "Botpress docs", url: "https://botpress.com/docs" }],
    importModes: repositoryImportModes,
    supportedImports: [{ kind: "repository", notes: "Import Botpress project metadata/templates without copying upstream code." }]
  }
];
