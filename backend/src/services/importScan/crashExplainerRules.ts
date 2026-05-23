// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
export type CrashExplainerRule = {  // line 7: executes this statement as part of this file's behavior
  id: string;  // line 8: executes this statement as part of this file's behavior
  pattern: string;  // line 9: executes this statement as part of this file's behavior
  diagnosis: string;  // line 10: executes this statement as part of this file's behavior
  fix: string;  // line 11: executes this statement as part of this file's behavior
  action: string;  // line 12: executes this statement as part of this file's behavior
};  // line 13: executes this statement as part of this file's behavior

export const CRASH_EXPLAINER_RULES: CrashExplainerRule[] = [  // line 15: executes this statement as part of this file's behavior
  { id: "node-module-not-found", pattern: "Cannot find module|ERR_MODULE_NOT_FOUND", diagnosis: "A dependency is missing or an import path is wrong.", fix: "Install dependencies and verify the import path.", action: "Run install / Open file" },  // line 16: executes this statement as part of this file's behavior
  { id: "python-module-not-found", pattern: "ModuleNotFoundError: No module named", diagnosis: "A Python package is missing from the environment.", fix: "Add the package to requirements.txt or install dependencies.", action: "Open requirements.txt / Run install" },  // line 17: executes this statement as part of this file's behavior
  { id: "discord-token-invalid", pattern: "TOKEN_INVALID|401 Unauthorized|invalid token", diagnosis: "The Discord token is missing or invalid.", fix: "Update DISCORD_TOKEN.", action: "Open secrets" },  // line 18: executes this statement as part of this file's behavior
  { id: "discord-intents", pattern: "used disallowed intents|privileged intent", diagnosis: "The bot requests an intent that is disabled in Discord developer settings.", fix: "Enable required intents or adjust bot configuration.", action: "Show intent checklist" },  // line 19: executes this statement as part of this file's behavior
  { id: "telegram-token-failure", pattern: "401 Unauthorized.*Telegram", diagnosis: "The Telegram bot token is missing or invalid.", fix: "Update TELEGRAM_BOT_TOKEN.", action: "Open secrets" },  // line 20: executes this statement as part of this file's behavior
  { id: "slack-signing-secret", pattern: "signature verification failed", diagnosis: "Slack signing secret does not match app configuration.", fix: "Update SLACK_SIGNING_SECRET.", action: "Open secrets" },  // line 21: executes this statement as part of this file's behavior
  { id: "port-in-use", pattern: "EADDRINUSE", diagnosis: "Another process is already using the port.", fix: "Stop the existing process or configure a different port.", action: "Show running processes" },  // line 22: executes this statement as part of this file's behavior
  { id: "install-failure", pattern: "npm ERR|pnpm ERR|pip install", diagnosis: "Dependency installation failed.", fix: "Inspect package-manager output and fix the failing package, auth, or network issue.", action: "Open install log" },  // line 23: executes this statement as part of this file's behavior
  { id: "typescript-compile", pattern: "TS\\d{4}", diagnosis: "TypeScript compilation failed.", fix: "Open the first diagnostic and resolve the type or import issue.", action: "Open source" }  // line 24: executes this statement as part of this file's behavior
];  // line 25: executes this statement as part of this file's behavior
