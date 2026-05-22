export type CrashExplainerRule = {
  id: string;
  pattern: string;
  diagnosis: string;
  fix: string;
  action: string;
};

export const CRASH_EXPLAINER_RULES: CrashExplainerRule[] = [
  { id: "node-module-not-found", pattern: "Cannot find module|ERR_MODULE_NOT_FOUND", diagnosis: "A dependency is missing or an import path is wrong.", fix: "Install dependencies and verify the import path.", action: "Run install / Open file" },
  { id: "python-module-not-found", pattern: "ModuleNotFoundError: No module named", diagnosis: "A Python package is missing from the environment.", fix: "Add the package to requirements.txt or install dependencies.", action: "Open requirements.txt / Run install" },
  { id: "discord-token-invalid", pattern: "TOKEN_INVALID|401 Unauthorized|invalid token", diagnosis: "The Discord token is missing or invalid.", fix: "Update DISCORD_TOKEN.", action: "Open secrets" },
  { id: "discord-intents", pattern: "used disallowed intents|privileged intent", diagnosis: "The bot requests an intent that is disabled in Discord developer settings.", fix: "Enable required intents or adjust bot configuration.", action: "Show intent checklist" },
  { id: "telegram-token-failure", pattern: "401 Unauthorized.*Telegram", diagnosis: "The Telegram bot token is missing or invalid.", fix: "Update TELEGRAM_BOT_TOKEN.", action: "Open secrets" },
  { id: "slack-signing-secret", pattern: "signature verification failed", diagnosis: "Slack signing secret does not match app configuration.", fix: "Update SLACK_SIGNING_SECRET.", action: "Open secrets" },
  { id: "port-in-use", pattern: "EADDRINUSE", diagnosis: "Another process is already using the port.", fix: "Stop the existing process or configure a different port.", action: "Show running processes" },
  { id: "install-failure", pattern: "npm ERR|pnpm ERR|pip install", diagnosis: "Dependency installation failed.", fix: "Inspect package-manager output and fix the failing package, auth, or network issue.", action: "Open install log" },
  { id: "typescript-compile", pattern: "TS\\d{4}", diagnosis: "TypeScript compilation failed.", fix: "Open the first diagnostic and resolve the type or import issue.", action: "Open source" }
];
