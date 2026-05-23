package com.princess.botblade.ui.settings

internal data class BotBladeFeatureCard(
    val title: String,
    val status: String,
    val detail: String,
)

internal object BotBladeFeatureDeck {
    val cards = listOf(
        BotBladeFeatureCard(
            title = "Forge Sync",
            status = "Git spine",
            detail = "Clone, import, diff, branch, commit, push, pull, conflict repair, and release workflow prep.",
        ),
        BotBladeFeatureCard(
            title = "Blade Packs",
            status = "Import catalog",
            detail = "Discord, Telegram, Slack, Matrix, Reddit, webhook, RSS, local task bots, and framework detection.",
        ),
        BotBladeFeatureCard(
            title = "Forge Editor",
            status = "Mobile IDE",
            detail = "Tabs, project tree, syntax hints, package scripts, diagnostics, previews, templates, and repair actions.",
        ),
        BotBladeFeatureCard(
            title = "Forge Files",
            status = "Android workspace",
            detail = "Local-first file browser, zip import/export, project backup, restore, move, rename, delete, and storage review.",
        ),
        BotBladeFeatureCard(
            title = "Runtime Deck",
            status = "Runner controls",
            detail = "Start, stop, restart, logs, local backend health, Docker targets, deployment history, and rollback lanes.",
        ),
        BotBladeFeatureCard(
            title = "Vault",
            status = "Secrets and audit",
            detail = "Encrypted secret references, GitHub token refs, project-scoped values, rotation, deletion, redaction, and audit events.",
        ),
        BotBladeFeatureCard(
            title = "Sentry Lane",
            status = "Reliability helpers",
            detail = "Startup diagnostics, build logs, crash traces, health checks, failure summaries, and repair prompts.",
        ),
        BotBladeFeatureCard(
            title = "Release Rail",
            status = "Shipping flow",
            detail = "APK links, GitHub workflow previews, package metadata, generated docs, deployment targets, and publish-readiness checks.",
        ),
    )
}
