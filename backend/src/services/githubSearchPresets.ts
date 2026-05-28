export interface GitHubSearchPreset {
  id: string;
  label: string;
  description: string;
  query: string;
  url: string;
  tags: string[];
}

const SEARCH_BASE = "https://github.com/search";

const definitions: Array<Omit<GitHubSearchPreset, "url">> = [
  { id: "discord-js", label: "Discord.js", description: "Slash-command and Node bot repositories.", query: "discord.js slash commands language:TypeScript", tags: ["discord", "node", "typescript"] },
  { id: "telegram-telegraf", label: "Telegram / Telegraf", description: "Telegram bot repositories using Telegraf or similar frameworks.", query: "telegraf telegram bot language:TypeScript", tags: ["telegram", "telegraf", "node"] },
  { id: "slack-bolt", label: "Slack Bolt", description: "Slack Bolt app and bot repositories.", query: "slack bolt bot language:TypeScript", tags: ["slack", "bolt", "node"] },
  { id: "n8n-workflow", label: "n8n workflows", description: "Workflow JSON examples for import-only inspection.", query: "n8n workflow json", tags: ["n8n", "workflow", "json"] },
  { id: "botpress", label: "Botpress", description: "Botpress-style bot-as-code projects and templates.", query: "botpress bot workflow", tags: ["botpress", "workflow"] },
  { id: "webhook-worker", label: "Webhook worker", description: "Small HTTP worker and automation bot repositories.", query: "webhook worker fastify language:TypeScript", tags: ["webhook", "worker", "http"] },
  { id: "scheduled-worker", label: "Scheduled worker", description: "Cron and scheduler automation repositories.", query: "scheduled worker cron bot", tags: ["scheduled", "cron", "automation"] },
  { id: "python-bot", label: "Python automation", description: "Python bot repositories with requirements or pyproject metadata.", query: "python bot automation requirements.txt", tags: ["python", "automation"] },
];

export const GITHUB_SEARCH_PRESETS: GitHubSearchPreset[] = definitions.map((preset) => ({
  ...preset,
  url: `${SEARCH_BASE}?type=repositories&q=${encodeURIComponent(preset.query)}`,
}));
