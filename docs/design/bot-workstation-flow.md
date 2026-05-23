# Bot Workstation Flow

BotBlade should function as a complete workstation for bots, from the first import or starter template through release. The UI must make the path visible and executable without making the user infer which tab owns each step.

## Point A to point Z

1. Create or import a bot project from a starter, Git repository, ZIP archive, folder, upstream reference snapshot, or repair-existing lane.
2. Scan the workspace with Blade Packs to classify the project and detect runtime, package manager, entrypoints, scripts, secrets, and missing metadata.
3. Configure encrypted secret references for platform tokens, signing secrets, GitHub tokens, environment variables, and project-scoped values.
4. Edit and repair files in the Forge Editor with project tree, generated files, diagnostics, language modes, command palette actions, and Bot Map context.
5. Validate and build through the local backend API.
6. Run the selected bot system with start, stop, restart, runtime status, logs, and health checks.
7. Inspect the running system through the Ops Deck, deployment history, target state, audit events, logs, and a container-scoped terminal.
8. Deploy through supported targets such as local process, local Docker, GitHub workflow, and later remote runners.
9. Release with CI status, versioning, signing, permissions, privacy copy, release notes, update/install testing, and rollback notes.

## Legitimate platform release paths

### Discord

The finished workflow should support: create or import a Discord.js project, configure Discord Developer Portal app/bot settings, store the Discord bot token in Vault, configure required gateway intents and OAuth scopes, generate slash command files, build the project, run locally, inspect logs, register commands, deploy to a target, and audit release state.

### Slack

The finished workflow should support: create or import a Slack Bolt app, configure the Slack app and event subscriptions, store the bot token, app token, and signing secret in Vault, build the project, run the app locally or in a container target, inspect logs, deploy to a target, and document required Slack settings.

### Similar bot platforms

The same flow should apply to Telegram, Matrix, Reddit, webhooks, RSS, and local automation bots: classify, configure credentials, validate entrypoints, build, run, inspect, deploy, and release.

## Conflict removal rules

Do not present a feature as fully available when the backend does not yet implement it. Use staged language for lanes such as Git import, ZIP import, folder import, writable container terminal, and remote deploy targets until endpoints are wired.

Do not route terminal access to the Android host shell. Terminal sessions belong inside the selected BotBlade-managed bot container.

Do not allow unreadable text. Black text on black, ink, dark cards, or dark chips is a release blocker. Compose uses the BotBlade dark color scheme; XML surfaces must set explicit light text colors where the inherited theme could be ambiguous.
