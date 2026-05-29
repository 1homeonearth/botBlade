import { BladePack, BladePackImportantFilePattern, BladePackImportMode } from "./schema.js";

const sharedPanels = ["projectMap", "editor", "logs", "secrets", "git", "health"];
const repositoryImportModes: BladePackImportMode[] = ["repository", "zip", "folder", "template"];
const nodeImportantFilePatterns: BladePackImportantFilePattern[] = [
  { kind: "packageManifest", pattern: "package.json", label: "Node package manifest", required: true },
  { kind: "config", pattern: "tsconfig.json", label: "TypeScript configuration" },
  { kind: "config", pattern: ".env.example", label: "Environment example" },
  { kind: "entrypoint", pattern: "src/{index,main,bot,server}.{ts,js}", label: "Common Node entrypoints" }
];

export const BLADE_PACKS: BladePack[] = [
  {
    schemaVersion: "0.2.0",
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
    templates: ["discord-slash-bot", "discord-gpt-bot", "discord-gemini-bot"],
    templateOptions: [
      { id: "discord-slash-bot", label: "Discord slash bot", description: "Minimal Discord.js slash-command starter.", runtime: "node", backendTemplateRef: "discord-slash-bot", starterFiles: [{ path: "src/index.ts" }, { path: "src/commands/ping.ts" }] },
      { id: "discord-gpt-bot", label: "Discord GPT bot", description: "Discord.js starter prepared for an OpenAI-backed response command.", runtime: "node", backendTemplateRef: "discord-gpt-bot", starterFiles: [{ path: "src/index.ts" }, { path: "src/commands/ask.ts" }] },
      { id: "discord-gemini-bot", label: "Discord Gemini bot", description: "Discord.js starter prepared for a Gemini-backed response command.", runtime: "node", backendTemplateRef: "discord-gemini-bot", starterFiles: [{ path: "src/index.ts" }, { path: "src/commands/ask.ts" }] }
    ],
    commands: { install: "npm install", build: "npm run build", test: "npm test", validate: "npm run build", start: "npm start", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: [
      ...nodeImportantFilePatterns,
      { kind: "commandDirectory", pattern: "src/commands", label: "Discord slash command modules" },
      { kind: "config", pattern: "deploy-commands.{js,ts}", label: "Discord command registration script" }
    ],
    repairRules: [
      { id: "discord-missing-token", title: "Discord token reference is missing", severity: "error", evidencePattern: "DISCORD_TOKEN", safeAction: "Create a DISCORD_TOKEN secret reference before starting the bot.", commandHint: "start", affectedFiles: [".env.example", "src/index.ts"] },
      { id: "discord-register-commands", title: "Slash commands may need registration", severity: "warning", evidencePattern: "Unknown interaction|application command", safeAction: "Run the command registration script after reviewing target guild/global scope.", commandHint: "deploy", affectedFiles: ["deploy-commands.js", "src/register-commands.ts"] }
    ],
    secrets: [
      { name: "DISCORD_TOKEN", label: "Discord bot token", required: true, example: "Paste your Discord bot token" },
      { name: "DISCORD_CLIENT_ID", label: "Discord application client ID", required: false }
    ],
    secretDetectors: [
      { source: "envExample", names: ["DISCORD_TOKEN", "DISCORD_CLIENT_ID"], required: true, evidencePattern: "DISCORD_(TOKEN|CLIENT_ID)", description: "Detect Discord secret references declared in environment examples." },
      { source: "sourceReference", names: ["DISCORD_TOKEN"], required: true, evidencePattern: "process\\.env\\.DISCORD_TOKEN" },
      { source: "frameworkConvention", names: ["DISCORD_TOKEN"], required: true, description: "Discord.js clients require a bot token at login time." }
    ],
    diagnostics: [],
    panels: sharedPanels,
    docs: [{ label: "discord.js guide", url: "https://discord.js.org/docs" }],
    importModes: [...repositoryImportModes],
    supportedImports: [{ kind: "repository", notes: "TypeScript or JavaScript discord.js bots." }]
  },
  {
    schemaVersion: "0.2.0",
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
    templates: ["telegram-command-bot", "telegram-ai-bot"],
    templateOptions: [
      { id: "telegram-command-bot", label: "Telegram command bot", description: "Minimal Telegraf command bot starter.", runtime: "node", backendTemplateRef: "telegram-command-bot", starterFiles: [{ path: "src/index.ts" }] },
      { id: "telegram-ai-bot", label: "Telegram AI bot", description: "Telegraf starter structured for an AI response command.", runtime: "node", backendTemplateRef: "telegram-ai-bot", starterFiles: [{ path: "src/index.ts" }] }
    ],
    commands: { install: "npm install", build: "npm run build", test: "npm test", validate: "npm run build", start: "npm start", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: [...nodeImportantFilePatterns, { kind: "entrypoint", pattern: "src/{bot,index}.{ts,js}", label: "Telegraf bot entrypoint" }],
    repairRules: [{ id: "telegram-missing-token", title: "Telegram bot token reference is missing", severity: "error", evidencePattern: "TELEGRAM_BOT_TOKEN|BOT_TOKEN", safeAction: "Create a TELEGRAM_BOT_TOKEN secret reference before starting the bot.", commandHint: "start", affectedFiles: [".env.example", "src/index.ts"] }],
    secrets: [{ name: "TELEGRAM_BOT_TOKEN", label: "Telegram bot token", required: true }],
    secretDetectors: [
      { source: "envExample", names: ["TELEGRAM_BOT_TOKEN", "BOT_TOKEN"], required: true, evidencePattern: "TELEGRAM_BOT_TOKEN|BOT_TOKEN" },
      { source: "sourceReference", names: ["TELEGRAM_BOT_TOKEN"], required: true, evidencePattern: "process\\.env\\.(TELEGRAM_BOT_TOKEN|BOT_TOKEN)" }
    ],
    diagnostics: [],
    panels: sharedPanels,
    docs: [{ label: "Telegraf docs", url: "https://telegraf.js.org/" }],
    importModes: [...repositoryImportModes],
    supportedImports: [{ kind: "repository", notes: "Node Telegraf repositories." }]
  },
  {
    schemaVersion: "0.2.0",
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
    templates: ["slack-slash-command", "slack-ai-assistant"],
    templateOptions: [
      { id: "slack-slash-command", label: "Slack slash command", description: "Slack Bolt starter for slash commands.", runtime: "node", backendTemplateRef: "slack-slash-command", starterFiles: [{ path: "src/index.ts" }] },
      { id: "slack-ai-assistant", label: "Slack AI assistant", description: "Slack Bolt starter for assistant-style messages.", runtime: "node", backendTemplateRef: "slack-ai-assistant", starterFiles: [{ path: "src/index.ts" }] }
    ],
    commands: { install: "npm install", build: "npm run build", test: "npm test", validate: "npm run build", start: "npm start", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: [...nodeImportantFilePatterns, { kind: "config", pattern: "manifest.yml", label: "Slack app manifest" }],
    repairRules: [{ id: "slack-missing-signing-secret", title: "Slack signing secret reference is missing", severity: "error", evidencePattern: "SLACK_SIGNING_SECRET", safeAction: "Create a SLACK_SIGNING_SECRET secret reference before receiving Slack requests.", commandHint: "start", affectedFiles: [".env.example", "src/index.ts"] }],
    secrets: [
      { name: "SLACK_BOT_TOKEN", label: "Slack bot token", required: true },
      { name: "SLACK_SIGNING_SECRET", label: "Slack signing secret", required: true },
      { name: "SLACK_APP_TOKEN", label: "Slack app token (socket mode)", required: false }
    ],
    secretDetectors: [
      { source: "envExample", names: ["SLACK_BOT_TOKEN", "SLACK_SIGNING_SECRET", "SLACK_APP_TOKEN"], required: true, evidencePattern: "SLACK_(BOT_TOKEN|SIGNING_SECRET|APP_TOKEN)" },
      { source: "frameworkConvention", names: ["SLACK_BOT_TOKEN", "SLACK_SIGNING_SECRET"], required: true, description: "Slack Bolt apps conventionally require bot and signing secret references." }
    ],
    diagnostics: [],
    panels: sharedPanels,
    docs: [{ label: "Slack Bolt JS docs", url: "https://slack.dev/bolt-js" }],
    importModes: [...repositoryImportModes],
    supportedImports: [{ kind: "repository", notes: "Slack Bolt JavaScript repositories." }]
  },
  {
    schemaVersion: "0.2.0",
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
    templates: ["generic-webhook-receiver", "scheduled-worker"],
    templateOptions: [
      { id: "generic-webhook-receiver", label: "Webhook receiver", description: "Generic Node starter for receiving webhook events.", runtime: "node", backendTemplateRef: "generic-webhook-receiver", starterFiles: [{ path: "src/index.ts" }] },
      { id: "scheduled-worker", label: "Scheduled worker", description: "Generic Node starter for background jobs.", runtime: "node", backendTemplateRef: "scheduled-worker", starterFiles: [{ path: "src/index.ts" }] }
    ],
    commands: { install: "npm install", validate: "npm run build", start: "npm start", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: nodeImportantFilePatterns,
    repairRules: [{ id: "node-missing-start-script", title: "Start script is missing", severity: "warning", evidencePattern: "missing script: start|Missing script.*start", safeAction: "Add a package.json start script that launches the reviewed entrypoint.", commandHint: "start", affectedFiles: ["package.json"] }],
    secrets: [],
    secretDetectors: [{ source: "envExample", names: [], required: false, evidencePattern: "^[A-Z][A-Z0-9_]+=" }],
    diagnostics: [],
    panels: sharedPanels,
    docs: [],
    importModes: [...repositoryImportModes],
    supportedImports: [{ kind: "repository", notes: "Generic Node projects." }]
  },
  {
    schemaVersion: "0.2.0",
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
    templates: ["python-telegram-bot", "python-discord-bot"],
    templateOptions: [
      { id: "python-telegram-bot", label: "Python Telegram bot", description: "Python starter for a Telegram bot.", runtime: "python", backendTemplateRef: "python-telegram-bot", starterFiles: [{ path: "main.py" }, { path: "requirements.txt" }] },
      { id: "python-discord-bot", label: "Python Discord bot", description: "Python starter for a Discord bot.", runtime: "python", backendTemplateRef: "python-discord-bot", starterFiles: [{ path: "main.py" }, { path: "requirements.txt" }] }
    ],
    commands: { install: "pip install -r requirements.txt", validate: "python -m py_compile main.py", start: "python main.py", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: [
      { kind: "packageManifest", pattern: "requirements.txt", label: "pip requirements" },
      { kind: "packageManifest", pattern: "pyproject.toml", label: "Python project manifest" },
      { kind: "entrypoint", pattern: "{main,bot,app}.py", label: "Python entrypoint" },
      { kind: "config", pattern: ".env.example", label: "Environment example" }
    ],
    repairRules: [{ id: "python-missing-requirements", title: "Python dependency manifest is missing", severity: "warning", evidencePattern: "No module named|ModuleNotFoundError", safeAction: "Add missing dependencies to requirements.txt or pyproject.toml after reviewing imports.", commandHint: "install", affectedFiles: ["requirements.txt", "pyproject.toml"] }],
    secrets: [],
    secretDetectors: [{ source: "envExample", names: [], required: false, evidencePattern: "^[A-Z][A-Z0-9_]+=" }],
    diagnostics: [],
    panels: sharedPanels,
    docs: [],
    importModes: [...repositoryImportModes],
    supportedImports: [{ kind: "repository", notes: "Generic Python bots and scripts." }]
  },

  {
    schemaVersion: "0.2.0",
    id: "generic-shell",
    name: "Generic Shell Project",
    version: "0.1.0",
    license: "MIT",
    runtime: { type: "unknown" },
    detectors: [
      { kind: "knownDirectory", path: "scripts", weight: 20 },
      { kind: "knownFilename", path: "Makefile", weight: 40 },
      { kind: "knownFilename", path: "Taskfile.yml", weight: 40 },
      { kind: "knownFilename", path: "justfile", weight: 40 },
      { kind: "knownFilename", path: ".shellcheckrc", weight: 20 },
      { kind: "sourceImport", pattern: "(^|\\n)#!\\s*(?:/usr/bin/env\\s+bash|/bin/(?:ba)?sh)", weight: 25 }
    ],
    templates: [],
    templateOptions: [],
    commands: {},
    importantFilePatterns: [
      { kind: "commandDirectory", pattern: "scripts", label: "Shell script directory" },
      { kind: "config", pattern: "Makefile", label: "Make task file" },
      { kind: "config", pattern: "Taskfile.yml", label: "Task taskfile" },
      { kind: "config", pattern: "justfile", label: "Just command file" },
      { kind: "config", pattern: ".shellcheckrc", label: "ShellCheck configuration" }
    ],
    repairRules: [],
    secrets: [],
    secretDetectors: [{ source: "envExample", names: [], required: false, evidencePattern: "^[A-Z][A-Z0-9_]+=" }],
    diagnostics: [],
    panels: sharedPanels,
    docs: [],
    importModes: [...repositoryImportModes],
    supportedImports: [{ kind: "repository", notes: "Generic shell scripts and command metadata; no shell runtime execution is implied." }]
  },
  {
    schemaVersion: "0.2.0",
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
    templates: [],
    templateOptions: [],
    commands: { validate: "botblade workflow validate" },
    importantFilePatterns: [
      { kind: "workflow", pattern: "workflow.json", label: "n8n workflow JSON", required: true },
      { kind: "config", pattern: "credentials.example.json", label: "Credential reference example" }
    ],
    repairRules: [{ id: "n8n-credential-reference", title: "Workflow credential reference needs mapping", severity: "warning", evidencePattern: "credentials", safeAction: "Map workflow credential names to BotBlade secret references without importing credential values.", commandHint: "validate", affectedFiles: ["workflow.json"] }],
    secrets: [{ name: "WORKFLOW_CREDENTIALS", label: "Imported workflow credentials", required: false }],
    secretDetectors: [{ source: "workflowCredentialReference", names: ["WORKFLOW_CREDENTIALS"], required: false, evidencePattern: "\"credentials\"\\s*:" }],
    diagnostics: [],
    panels: ["projectMap", "logs", "health"],
    docs: [{ label: "n8n docs", url: "https://docs.n8n.io/" }],
    importModes: ["workflow_json", "template"],
    supportedImports: [{ kind: "workflow-json", notes: "Import-only parser for n8n workflow JSON; no embedded n8n runtime." }]
  },
  {
    schemaVersion: "0.2.0",
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
    templates: ["botpress-bot-as-code"],
    templateOptions: [{ id: "botpress-bot-as-code", label: "Botpress bot-as-code", description: "Botpress metadata/template adapter without vendoring upstream runtime code.", runtime: "node", backendTemplateRef: "botpress-bot-as-code", starterFiles: [{ path: "botpress.config.json" }, { path: "src/index.ts" }] }],
    commands: { install: "npm install", build: "npm run build", validate: "npm run build", start: "npm start", stop: "botblade runtime stop", restart: "botblade runtime restart" },
    importantFilePatterns: [...nodeImportantFilePatterns, { kind: "config", pattern: "botpress.config.json", label: "Botpress configuration", required: true }, { kind: "config", pattern: ".botpress", label: "Botpress metadata directory" }],
    repairRules: [{ id: "botpress-token-reference", title: "Botpress token reference may be required", severity: "info", evidencePattern: "BOTPRESS_TOKEN|botpress", safeAction: "Create a BOTPRESS_TOKEN secret reference only if this project uses hosted Botpress APIs.", commandHint: "validate", affectedFiles: [".env.example", "botpress.config.json"] }],
    secrets: [{ name: "BOTPRESS_TOKEN", label: "Botpress token", required: false }],
    secretDetectors: [{ source: "frameworkConvention", names: ["BOTPRESS_TOKEN"], required: false, evidencePattern: "BOTPRESS_TOKEN|botpress" }],
    diagnostics: [],
    panels: sharedPanels,
    docs: [{ label: "Botpress docs", url: "https://botpress.com/docs" }],
    importModes: [...repositoryImportModes],
    supportedImports: [{ kind: "repository", notes: "Import Botpress project metadata/templates without copying upstream code." }]
  }
];
