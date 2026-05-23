package com.princess.botblade.ui.dashboard

internal data class WorkstationStep(
    val index: String,
    val title: String,
    val state: String,
    val detail: String,
)

internal object BotWorkstationDeck {
    val flow = listOf(
        WorkstationStep("01", "Create or import", "Projects", "Start from a Discord TypeScript starter, moderation toolkit, blank workspace, Git repo, ZIP archive, folder, or repair-existing lane."),
        WorkstationStep("02", "Scan and classify", "Blade Packs", "Detect Discord.js, Telegraf, Slack Bolt, generic Node, generic Python, n8n workflow JSON, Botpress-style structure, and future bot packs."),
        WorkstationStep("03", "Configure secrets", "Vault", "Store Discord tokens, Slack app tokens, signing keys, GitHub refs, and per-project values as encrypted secret references."),
        WorkstationStep("04", "Edit and repair", "Forge Editor", "Edit files, inspect generated package files, run first-check diagnostics, review missing scripts, and prepare runtime metadata."),
        WorkstationStep("05", "Build", "Local backend", "Run validation and local builds from Android through token-protected backend routes."),
        WorkstationStep("06", "Run", "Runtime Deck", "Start, stop, restart, view logs, and inspect active runtime state for the selected bot system."),
        WorkstationStep("07", "Inspect", "Ops Deck", "Use deployment history, target status, runtime logs, audit events, and container-scoped terminal output."),
        WorkstationStep("08", "Deploy", "Release Rail", "Deploy to local process, local Docker, GitHub workflow, or a later remote target once credentials and target capabilities are ready."),
        WorkstationStep("09", "Release", "Publish Gate", "Check CI, versioning, signing, permissions, privacy text, release notes, install/update test, and rollback plan."),
    )

    val releasePaths = listOf(
        WorkstationStep("D", "Discord", "slash commands", "Create app in Discord Developer Portal, add bot token to Vault, configure intents/scopes, generate TypeScript commands, build, run, deploy, and register commands."),
        WorkstationStep("S", "Slack", "Bolt app", "Create Slack app, add bot/user tokens and signing secret to Vault, configure event subscriptions, build the workspace, run locally, then deploy the target."),
        WorkstationStep("W", "Webhooks", "generic adapter", "Import or create a webhook bot, configure env secrets, validate endpoints, build, run, inspect logs, then deploy behind the selected target."),
    )
}
