// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import { BladePack } from "./schema.js";  // line 7: executes this statement as part of this file's behavior

const sharedPanels = ["projectMap", "editor", "logs", "secrets", "git", "health"];  // line 9: executes this statement as part of this file's behavior

export const BLADE_PACKS: BladePack[] = [  // line 11: executes this statement as part of this file's behavior
  {  // line 12: executes this statement as part of this file's behavior
    id: "discord-js",  // line 13: executes this statement as part of this file's behavior
    name: "Discord.js Bot",  // line 14: executes this statement as part of this file's behavior
    version: "0.1.0",  // line 15: executes this statement as part of this file's behavior
    license: "Apache-2.0",  // line 16: executes this statement as part of this file's behavior
    runtime: { type: "node", versionRange: ">=22", packageManagers: ["npm", "pnpm", "yarn"] },  // line 17: executes this statement as part of this file's behavior
    detectors: [  // line 18: executes this statement as part of this file's behavior
      { kind: "packageDependency", name: "discord.js", weight: 50 },  // line 19: executes this statement as part of this file's behavior
      { kind: "sourceImport", pattern: "from 'discord.js'|require\\('discord.js'\\)", weight: 25 },  // line 20: executes this statement as part of this file's behavior
      { kind: "envKey", pattern: "DISCORD_TOKEN|DISCORD_CLIENT_ID", weight: 15 },  // line 21: executes this statement as part of this file's behavior
      { kind: "knownFilename", path: "deploy-commands.js", weight: 10 }  // line 22: executes this statement as part of this file's behavior
    ],  // line 23: executes this statement as part of this file's behavior
    templates: ["discord-slash-bot", "discord-gpt-bot", "discord-gemini-bot"],  // line 24: executes this statement as part of this file's behavior
    commands: { install: "npm install", build: "npm run build", test: "npm test", run: "npm start" },  // line 25: executes this statement as part of this file's behavior
    secrets: [  // line 26: executes this statement as part of this file's behavior
      { name: "DISCORD_TOKEN", label: "Discord bot token", required: true, example: "Paste your Discord bot token" },  // line 27: executes this statement as part of this file's behavior
      { name: "DISCORD_CLIENT_ID", label: "Discord application client ID", required: false }  // line 28: executes this statement as part of this file's behavior
    ],  // line 29: executes this statement as part of this file's behavior
    diagnostics: [],  // line 30: executes this statement as part of this file's behavior
    panels: sharedPanels,  // line 31: executes this statement as part of this file's behavior
    docs: [{ label: "discord.js guide", url: "https://discord.js.org/docs" }],  // line 32: executes this statement as part of this file's behavior
    supportedImports: [{ kind: "repository", notes: "TypeScript or JavaScript discord.js bots." }]  // line 33: executes this statement as part of this file's behavior
  },  // line 34: executes this statement as part of this file's behavior
  {  // line 35: executes this statement as part of this file's behavior
    id: "telegraf",  // line 36: executes this statement as part of this file's behavior
    name: "Telegraf Bot",  // line 37: executes this statement as part of this file's behavior
    version: "0.1.0",  // line 38: executes this statement as part of this file's behavior
    license: "MIT",  // line 39: executes this statement as part of this file's behavior
    runtime: { type: "node", versionRange: ">=22", packageManagers: ["npm", "pnpm", "yarn"] },  // line 40: executes this statement as part of this file's behavior
    detectors: [  // line 41: executes this statement as part of this file's behavior
      { kind: "packageDependency", name: "telegraf", weight: 50 },  // line 42: executes this statement as part of this file's behavior
      { kind: "sourceImport", pattern: "from 'telegraf'|require\\('telegraf'\\)", weight: 25 },  // line 43: executes this statement as part of this file's behavior
      { kind: "envKey", pattern: "TELEGRAM_BOT_TOKEN|BOT_TOKEN", weight: 15 },  // line 44: executes this statement as part of this file's behavior
      { kind: "sourceImport", pattern: "bot\\.launch|Composer", weight: 10 }  // line 45: executes this statement as part of this file's behavior
    ],  // line 46: executes this statement as part of this file's behavior
    templates: ["telegram-command-bot", "telegram-ai-bot"],  // line 47: executes this statement as part of this file's behavior
    commands: { install: "npm install", build: "npm run build", test: "npm test", run: "npm start" },  // line 48: executes this statement as part of this file's behavior
    secrets: [{ name: "TELEGRAM_BOT_TOKEN", label: "Telegram bot token", required: true }],  // line 49: executes this statement as part of this file's behavior
    diagnostics: [],  // line 50: executes this statement as part of this file's behavior
    panels: sharedPanels,  // line 51: executes this statement as part of this file's behavior
    docs: [{ label: "Telegraf docs", url: "https://telegraf.js.org/" }],  // line 52: executes this statement as part of this file's behavior
    supportedImports: [{ kind: "repository", notes: "Node Telegraf repositories." }]  // line 53: executes this statement as part of this file's behavior
  },  // line 54: executes this statement as part of this file's behavior
  {  // line 55: executes this statement as part of this file's behavior
    id: "slack-bolt",  // line 56: executes this statement as part of this file's behavior
    name: "Slack Bolt JS",  // line 57: executes this statement as part of this file's behavior
    version: "0.1.0",  // line 58: executes this statement as part of this file's behavior
    license: "MIT",  // line 59: executes this statement as part of this file's behavior
    runtime: { type: "node", versionRange: ">=22", packageManagers: ["npm", "pnpm", "yarn"] },  // line 60: executes this statement as part of this file's behavior
    detectors: [  // line 61: executes this statement as part of this file's behavior
      { kind: "packageDependency", name: "@slack/bolt", weight: 50 },  // line 62: executes this statement as part of this file's behavior
      { kind: "sourceImport", pattern: "from '@slack/bolt'|require\\('@slack/bolt'\\)", weight: 25 },  // line 63: executes this statement as part of this file's behavior
      { kind: "envKey", pattern: "SLACK_BOT_TOKEN", weight: 15 },  // line 64: executes this statement as part of this file's behavior
      { kind: "envKey", pattern: "SLACK_SIGNING_SECRET", weight: 15 }  // line 65: executes this statement as part of this file's behavior
    ],  // line 66: executes this statement as part of this file's behavior
    templates: ["slack-slash-command", "slack-ai-assistant"],  // line 67: executes this statement as part of this file's behavior
    commands: { install: "npm install", build: "npm run build", test: "npm test", run: "npm start" },  // line 68: executes this statement as part of this file's behavior
    secrets: [  // line 69: executes this statement as part of this file's behavior
      { name: "SLACK_BOT_TOKEN", label: "Slack bot token", required: true },  // line 70: executes this statement as part of this file's behavior
      { name: "SLACK_SIGNING_SECRET", label: "Slack signing secret", required: true },  // line 71: executes this statement as part of this file's behavior
      { name: "SLACK_APP_TOKEN", label: "Slack app token (socket mode)", required: false }  // line 72: executes this statement as part of this file's behavior
    ],  // line 73: executes this statement as part of this file's behavior
    diagnostics: [],  // line 74: executes this statement as part of this file's behavior
    panels: sharedPanels,  // line 75: executes this statement as part of this file's behavior
    docs: [{ label: "Slack Bolt JS docs", url: "https://slack.dev/bolt-js" }],  // line 76: executes this statement as part of this file's behavior
    supportedImports: [{ kind: "repository", notes: "Slack Bolt JavaScript repositories." }]  // line 77: executes this statement as part of this file's behavior
  },  // line 78: executes this statement as part of this file's behavior
  {  // line 79: executes this statement as part of this file's behavior
    id: "generic-node",  // line 80: executes this statement as part of this file's behavior
    name: "Generic Node Project",  // line 81: executes this statement as part of this file's behavior
    version: "0.1.0",  // line 82: executes this statement as part of this file's behavior
    license: "MIT",  // line 83: executes this statement as part of this file's behavior
    runtime: { type: "node", versionRange: ">=22", packageManagers: ["npm", "pnpm", "yarn"] },  // line 84: executes this statement as part of this file's behavior
    detectors: [  // line 85: executes this statement as part of this file's behavior
      { kind: "fileExists", path: "package.json", weight: 35 },  // line 86: executes this statement as part of this file's behavior
      { kind: "packageScript", name: "start", weight: 20 },  // line 87: executes this statement as part of this file's behavior
      { kind: "knownFilename", path: "package-lock.json", weight: 15 },  // line 88: executes this statement as part of this file's behavior
      { kind: "knownDirectory", path: "src", weight: 15 },  // line 89: executes this statement as part of this file's behavior
      { kind: "sourceImport", pattern: "node:|process\\.", weight: 15 }  // line 90: executes this statement as part of this file's behavior
    ],  // line 91: executes this statement as part of this file's behavior
    templates: ["generic-webhook-receiver", "scheduled-worker"],  // line 92: executes this statement as part of this file's behavior
    commands: { install: "npm install", run: "npm start" },  // line 93: executes this statement as part of this file's behavior
    secrets: [], diagnostics: [], panels: sharedPanels, docs: [], supportedImports: [{ kind: "repository", notes: "Generic Node projects." }]  // line 94: executes this statement as part of this file's behavior
  },  // line 95: executes this statement as part of this file's behavior
  {  // line 96: executes this statement as part of this file's behavior
    id: "generic-python",  // line 97: executes this statement as part of this file's behavior
    name: "Generic Python Project",  // line 98: executes this statement as part of this file's behavior
    version: "0.1.0",  // line 99: executes this statement as part of this file's behavior
    license: "MIT",  // line 100: executes this statement as part of this file's behavior
    runtime: { type: "python", packageManagers: ["pip"] },  // line 101: executes this statement as part of this file's behavior
    detectors: [  // line 102: executes this statement as part of this file's behavior
      { kind: "fileExists", path: "requirements.txt", weight: 30 },  // line 103: executes this statement as part of this file's behavior
      { kind: "fileExists", path: "pyproject.toml", weight: 30 },  // line 104: executes this statement as part of this file's behavior
      { kind: "knownFilename", path: "main.py", weight: 20 },  // line 105: executes this statement as part of this file's behavior
      { kind: "knownDirectory", path: ".venv", weight: 15 },  // line 106: executes this statement as part of this file's behavior
      { kind: "sourceImport", pattern: "aiogram|discord|telegram|openai", weight: 20 }  // line 107: executes this statement as part of this file's behavior
    ],  // line 108: executes this statement as part of this file's behavior
    templates: ["python-telegram-bot", "python-discord-bot"],  // line 109: executes this statement as part of this file's behavior
    commands: { install: "pip install -r requirements.txt", run: "python main.py" },  // line 110: executes this statement as part of this file's behavior
    secrets: [], diagnostics: [], panels: sharedPanels, docs: [], supportedImports: [{ kind: "repository", notes: "Generic Python bots and scripts." }]  // line 111: executes this statement as part of this file's behavior
  },  // line 112: executes this statement as part of this file's behavior
  {  // line 113: executes this statement as part of this file's behavior
    id: "n8n-workflow",  // line 114: executes this statement as part of this file's behavior
    name: "n8n Workflow Import",  // line 115: executes this statement as part of this file's behavior
    version: "0.1.0",  // line 116: executes this statement as part of this file's behavior
    license: "Sustainable-Use-reference",  // line 117: executes this statement as part of this file's behavior
    runtime: { type: "workflow" },  // line 118: executes this statement as part of this file's behavior
    detectors: [  // line 119: executes this statement as part of this file's behavior
      { kind: "jsonShape", file: "workflow.json", keys: ["nodes", "connections"], weight: 60 },  // line 120: executes this statement as part of this file's behavior
      { kind: "sourceImport", pattern: "\"type\"\\s*:\\s*\"n8n-nodes-base\\.", weight: 20 },  // line 121: executes this statement as part of this file's behavior
      { kind: "sourceImport", pattern: "credentials", weight: 10 },  // line 122: executes this statement as part of this file's behavior
      { kind: "sourceImport", pattern: "trigger", weight: 10 }  // line 123: executes this statement as part of this file's behavior
    ],  // line 124: executes this statement as part of this file's behavior
    templates: [],  // line 125: executes this statement as part of this file's behavior
    commands: {},  // line 126: executes this statement as part of this file's behavior
    secrets: [{ name: "WORKFLOW_CREDENTIALS", label: "Imported workflow credentials", required: false }],  // line 127: executes this statement as part of this file's behavior
    diagnostics: [],  // line 128: executes this statement as part of this file's behavior
    panels: ["projectMap", "logs", "health"],  // line 129: executes this statement as part of this file's behavior
    docs: [{ label: "n8n docs", url: "https://docs.n8n.io/" }],  // line 130: executes this statement as part of this file's behavior
    supportedImports: [{ kind: "workflow-json", notes: "Import-only parser for n8n workflow JSON; no embedded n8n runtime." }]  // line 131: executes this statement as part of this file's behavior
  },  // line 132: executes this statement as part of this file's behavior
  {  // line 133: executes this statement as part of this file's behavior
    id: "botpress",  // line 134: executes this statement as part of this file's behavior
    name: "Botpress Bot-as-Code",  // line 135: executes this statement as part of this file's behavior
    version: "0.1.0",  // line 136: executes this statement as part of this file's behavior
    license: "Botpress-reference",  // line 137: executes this statement as part of this file's behavior
    runtime: { type: "node", versionRange: ">=22", packageManagers: ["npm", "pnpm", "yarn"] },  // line 138: executes this statement as part of this file's behavior
    detectors: [  // line 139: executes this statement as part of this file's behavior
      { kind: "knownDirectory", path: ".botpress", weight: 50 },  // line 140: executes this statement as part of this file's behavior
      { kind: "packageDependency", name: "@botpress", weight: 25 },  // line 141: executes this statement as part of this file's behavior
      { kind: "knownFilename", path: "botpress.config.json", weight: 15 },  // line 142: executes this statement as part of this file's behavior
      { kind: "sourceImport", pattern: "botpress", weight: 10 }  // line 143: executes this statement as part of this file's behavior
    ],  // line 144: executes this statement as part of this file's behavior
    templates: ["botpress-bot-as-code"],  // line 145: executes this statement as part of this file's behavior
    commands: { install: "npm install", build: "npm run build", run: "npm start" },  // line 146: executes this statement as part of this file's behavior
    secrets: [{ name: "BOTPRESS_TOKEN", label: "Botpress token", required: false }],  // line 147: executes this statement as part of this file's behavior
    diagnostics: [], panels: sharedPanels,  // line 148: executes this statement as part of this file's behavior
    docs: [{ label: "Botpress docs", url: "https://botpress.com/docs" }],  // line 149: executes this statement as part of this file's behavior
    supportedImports: [{ kind: "repository", notes: "Import Botpress project metadata/templates without copying upstream code." }]  // line 150: executes this statement as part of this file's behavior
  }  // line 151: executes this statement as part of this file's behavior
];  // line 152: executes this statement as part of this file's behavior
