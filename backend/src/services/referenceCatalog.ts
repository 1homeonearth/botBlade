export type ReferenceCatalogEntry = {
  id: string;
  label: string;
  category: string;
  homepage?: string;
  docsUrl?: string;
  repoUrl?: string;
  languageHints: string[];
  packageHints: string[];
  entrypointHints: string[];
  secretHints: string[];
  importDetectorHints: string[];
  status: "seed" | "current" | "legacy" | "needs-review";
  licenseNotes: string;
};

export const referenceCatalog: ReferenceCatalogEntry[] = [
  {
    id: "platform-discord",
    label: "Discord",
    category: "platform",
    docsUrl: "https://discord.com/developers/docs/intro",
    languageHints: ["javascript", "typescript", "python", "java", "rust", "go", "csharp", "lua", "c"],
    packageHints: ["discord.js", "eris", "discord.py", "jda", "serenity", "discordgo", "dsharpplus", "discord.net", "discordia", "concord"],
    entrypointHints: ["index.js", "src/index.ts", "bot.js", "bot.py", "main.py", "Program.cs", "main.rs"],
    secretHints: ["DISCORD_TOKEN", "DISCORD_CLIENT_ID", "DISCORD_GUILD_ID", "DISCORD_PUBLIC_KEY"],
    importDetectorHints: ["discord", "discord.js", "Client({ intents", "GatewayIntentBits", "commands-deploy", "slash command"],
    status: "current",
    licenseNotes: "Platform seed derived from CC0 hackerkid/bots taxonomy; docs URL updated for current Discord developer documentation.",
  },
  {
    id: "platform-telegram",
    label: "Telegram",
    category: "platform",
    docsUrl: "https://core.telegram.org/bots/api",
    languageHints: ["javascript", "typescript", "python", "php", "csharp", "go", "java"],
    packageHints: ["telegraf", "grammy", "node-telegram-bot-api", "python-telegram-bot", "telegram-bot-sdk", "telebot"],
    entrypointHints: ["index.js", "src/index.ts", "bot.js", "bot.py", "main.py"],
    secretHints: ["TELEGRAM_BOT_TOKEN", "BOT_TOKEN"],
    importDetectorHints: ["telegram", "telegraf", "grammy", "node-telegram-bot-api", "python-telegram-bot", "bot.telegram"],
    status: "current",
    licenseNotes: "Platform seed derived from CC0 hackerkid/bots taxonomy.",
  },
  {
    id: "platform-slack",
    label: "Slack",
    category: "platform",
    docsUrl: "https://api.slack.com/bot-users",
    languageHints: ["javascript", "typescript", "python"],
    packageHints: ["@slack/bolt", "@slack/web-api", "slack_bolt", "slack-sdk", "node-slack-sdk"],
    entrypointHints: ["index.js", "src/index.ts", "app.js", "bot.py", "main.py"],
    secretHints: ["SLACK_BOT_TOKEN", "SLACK_SIGNING_SECRET", "SLACK_APP_TOKEN"],
    importDetectorHints: ["@slack/bolt", "App({ token", "signingSecret", "slack_bolt", "xapp-"],
    status: "current",
    licenseNotes: "Platform seed derived from CC0 hackerkid/bots taxonomy.",
  },
  {
    id: "platform-zulip",
    label: "Zulip",
    category: "platform",
    docsUrl: "https://zulip.com/integrations/",
    languageHints: ["python", "javascript", "typescript"],
    packageHints: ["zulip", "zulip-js"],
    entrypointHints: ["bot.py", "main.py", "index.js"],
    secretHints: ["ZULIP_EMAIL", "ZULIP_API_KEY", "ZULIP_SITE"],
    importDetectorHints: ["zulip", "ZULIP_API_KEY", "zuliprc"],
    status: "needs-review",
    licenseNotes: "Platform seed derived from CC0 hackerkid/bots taxonomy; review current SDK status before first-class support.",
  },
  {
    id: "platform-microsoft-bot-framework",
    label: "Microsoft Bot Framework",
    category: "platform",
    homepage: "https://dev.botframework.com/",
    languageHints: ["javascript", "typescript", "csharp", "python"],
    packageHints: ["botbuilder", "Microsoft.Bot.Builder"],
    entrypointHints: ["index.js", "src/index.ts", "Program.cs", "app.py"],
    secretHints: ["MicrosoftAppId", "MicrosoftAppPassword", "MICROSOFT_APP_ID", "MICROSOFT_APP_PASSWORD"],
    importDetectorHints: ["botbuilder", "ActivityHandler", "CloudAdapter", "BotFrameworkAdapter"],
    status: "needs-review",
    licenseNotes: "Platform seed derived from CC0 hackerkid/bots taxonomy; enterprise pack belongs after Discord, Telegram, Slack, and generic webhook support.",
  },
  {
    id: "framework-discord-js",
    label: "discord.js",
    category: "node-framework",
    repoUrl: "https://github.com/discordjs/discord.js",
    docsUrl: "https://discord.js.org/",
    languageHints: ["javascript", "typescript"],
    packageHints: ["discord.js"],
    entrypointHints: ["index.js", "src/index.ts", "src/bot.ts", "commands-deploy.js"],
    secretHints: ["DISCORD_TOKEN", "DISCORD_CLIENT_ID", "DISCORD_GUILD_ID"],
    importDetectorHints: ["discord.js", "GatewayIntentBits", "SlashCommandBuilder", "REST({ version", "Routes.applicationCommands"],
    status: "current",
    licenseNotes: "Discord library seed transformed from CC0 hackerkid/bots list; repo URL updated to current organization.",
  },
  {
    id: "framework-eris",
    label: "Eris",
    category: "node-framework",
    repoUrl: "https://github.com/abalabahaha/eris",
    languageHints: ["javascript", "typescript"],
    packageHints: ["eris"],
    entrypointHints: ["index.js", "bot.js", "src/index.ts"],
    secretHints: ["DISCORD_TOKEN"],
    importDetectorHints: ["eris", "new Eris", "Eris.Client"],
    status: "needs-review",
    licenseNotes: "Discord library seed derived from CC0 hackerkid/bots list; verify maintenance status before recommending new projects.",
  },
  {
    id: "framework-dsharpplus",
    label: "DSharpPlus",
    category: "dotnet-framework",
    repoUrl: "https://github.com/DSharpPlus/DSharpPlus",
    languageHints: ["csharp"],
    packageHints: ["DSharpPlus"],
    entrypointHints: ["Program.cs", "Bot.cs"],
    secretHints: ["DISCORD_TOKEN"],
    importDetectorHints: ["DSharpPlus", "DiscordClient", "DiscordConfiguration"],
    status: "current",
    licenseNotes: "Discord library seed transformed from CC0 hackerkid/bots list; repo URL updated from legacy namespace.",
  },
  {
    id: "framework-serenity",
    label: "Serenity",
    category: "rust-framework",
    repoUrl: "https://github.com/serenity-rs/serenity",
    languageHints: ["rust"],
    packageHints: ["serenity"],
    entrypointHints: ["src/main.rs"],
    secretHints: ["DISCORD_TOKEN"],
    importDetectorHints: ["serenity", "Client::builder", "GatewayIntents"],
    status: "current",
    licenseNotes: "Discord library seed transformed from CC0 hackerkid/bots list; repo URL updated to current organization.",
  },
  {
    id: "framework-discordgo",
    label: "DiscordGo",
    category: "go-framework",
    repoUrl: "https://github.com/bwmarrin/discordgo",
    languageHints: ["go"],
    packageHints: ["github.com/bwmarrin/discordgo"],
    entrypointHints: ["main.go", "cmd/bot/main.go"],
    secretHints: ["DISCORD_TOKEN"],
    importDetectorHints: ["discordgo", "discordgo.New", "AddHandler"],
    status: "current",
    licenseNotes: "Discord library seed derived from CC0 hackerkid/bots list.",
  },
  {
    id: "tool-chatbottest",
    label: "ChatbotTest",
    category: "testing-tool",
    homepage: "http://chatbottest.com/",
    languageHints: [],
    packageHints: [],
    entrypointHints: [],
    secretHints: [],
    importDetectorHints: ["chatbot test", "conversation quality", "bot qa"],
    status: "legacy",
    licenseNotes: "Testing category seed derived from CC0 hackerkid/bots list; keep as historical QA taxonomy clue, not as a hard dependency.",
  },
];

export function findReferenceCatalogEntriesByPackage(packageName: string): ReferenceCatalogEntry[] {
  const normalizedPackageName = packageName.trim().toLowerCase();

  if (normalizedPackageName.length === 0) {
    return [];
  }

  return referenceCatalog.filter((entry) =>
    entry.packageHints.some((hint) => hint.toLowerCase() === normalizedPackageName),
  );
}

export function findReferenceCatalogEntriesByHint(text: string): ReferenceCatalogEntry[] {
  const normalizedText = text.toLowerCase();

  if (normalizedText.trim().length === 0) {
    return [];
  }

  return referenceCatalog.filter((entry) =>
    entry.importDetectorHints.some((hint) => normalizedText.includes(hint.toLowerCase())),
  );
}
