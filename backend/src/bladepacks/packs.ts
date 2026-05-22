import { BladePack } from "./schema.js";

const sharedPanels = ["projectMap", "editor", "logs", "secrets", "git", "health"];

export const BLADE_PACKS: BladePack[] = [
  {
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
    commands: { install: "npm install", build: "npm run build", test: "npm test", run: "npm start" },
    secrets: [
      { name: "DISCORD_TOKEN", label: "Discord bot token", required: true, example: "Paste your Discord bot token" },
      { name: "DISCORD_CLIENT_ID", label: "Discord application client ID", required: false }
    ],
    diagnostics: [],
    panels: sharedPanels,
    docs: [{ label: "discord.js guide", url: "https://discord.js.org/docs" }],
    supportedImports: [{ kind: "repository", notes: "TypeScript or JavaScript discord.js bots." }]
  },
  {
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
    commands: { install: "npm install", build: "npm run build", test: "npm test", run: "npm start" },
    secrets: [{ name: "TELEGRAM_BOT_TOKEN", label: "Telegram bot token", required: true }],
    diagnostics: [],
    panels: sharedPanels,
    docs: [{ label: "Telegraf docs", url: "https://telegraf.js.org/" }],
    supportedImports: [{ kind: "repository", notes: "Node Telegraf repositories." }]
  },
  {
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
    commands: { install: "npm install", build: "npm run build", test: "npm test", run: "npm start" },
    secrets: [
      { name: "SLACK_BOT_TOKEN", label: "Slack bot token", required: true },
      { name: "SLACK_SIGNING_SECRET", label: "Slack signing secret", required: true },
      { name: "SLACK_APP_TOKEN", label: "Slack app token (socket mode)", required: false }
    ],
    diagnostics: [],
    panels: sharedPanels,
    docs: [{ label: "Slack Bolt JS docs", url: "https://slack.dev/bolt-js" }],
    supportedImports: [{ kind: "repository", notes: "Slack Bolt JavaScript repositories." }]
  },
  {
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
    commands: { install: "npm install", run: "npm start" },
    secrets: [], diagnostics: [], panels: sharedPanels, docs: [], supportedImports: [{ kind: "repository", notes: "Generic Node projects." }]
  },
  {
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
    commands: { install: "pip install -r requirements.txt", run: "python main.py" },
    secrets: [], diagnostics: [], panels: sharedPanels, docs: [], supportedImports: [{ kind: "repository", notes: "Generic Python bots and scripts." }]
  },
  {
    id: "n8n-workflow",
    name: "n8n Workflow Import",
    version: "0.1.0",
    license: "Sustainable-Use-reference",
    runtime: { type: "workflow" },
    detectors: [
      { kind: "jsonShape", file: "workflow.json", keys: ["nodes", "connections"], weight: 60 },
      { kind: "sourceImport", pattern: "\"type\"\s*:\s*\"n8n-nodes-base\\.", weight: 20 },
      { kind: "sourceImport", pattern: "credentials", weight: 10 },
      { kind: "sourceImport", pattern: "trigger", weight: 10 }
    ],
    templates: [],
    commands: {},
    secrets: [{ name: "WORKFLOW_CREDENTIALS", label: "Imported workflow credentials", required: false }],
    diagnostics: [],
    panels: ["projectMap", "logs", "health"],
    docs: [{ label: "n8n docs", url: "https://docs.n8n.io/" }],
    supportedImports: [{ kind: "workflow-json", notes: "Import-only parser for n8n workflow JSON; no embedded n8n runtime." }]
  },
  {
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
    commands: { install: "npm install", build: "npm run build", run: "npm start" },
    secrets: [{ name: "BOTPRESS_TOKEN", label: "Botpress token", required: false }],
    diagnostics: [], panels: sharedPanels,
    docs: [{ label: "Botpress docs", url: "https://botpress.com/docs" }],
    supportedImports: [{ kind: "repository", notes: "Import Botpress project metadata/templates without copying upstream code." }]
  }
];
