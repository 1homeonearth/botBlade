package com.princess.botblade.ui.editor

internal data class ForgeEditorLane(
    val title: String,
    val status: String,
    val detail: String,
)

internal object ForgeEditorDeck {
    val lanes = listOf(
        ForgeEditorLane(
            title = "Monaco-style models",
            status = "File identity",
            detail = "Treat every open file as a model with path, language, edit state, diagnostics, and view state.",
        ),
        ForgeEditorLane(
            title = "Command palette",
            status = "Mobile shortcut layer",
            detail = "Expose generate, scan, save, revert, build, repair, deploy, open logs, and container terminal actions.",
        ),
        ForgeEditorLane(
            title = "Language modes",
            status = "Bot project aware",
            detail = "Prioritize TypeScript, JavaScript, JSON, YAML, Markdown, shell, Kotlin, Java, Python, Rust, Dockerfile, and env files.",
        ),
        ForgeEditorLane(
            title = "Diagnostics rail",
            status = "Repair first",
            detail = "Surface package scripts, missing secrets, dependency issues, build failures, unsafe defaults, and project scan results.",
        ),
        ForgeEditorLane(
            title = "Bot map",
            status = "Project structure",
            detail = "Show commands, triggers, adapters, secrets, services, webhooks, deployment targets, and health checks.",
        ),
        ForgeEditorLane(
            title = "Container terminal",
            status = "Scoped shell",
            detail = "Open terminal sessions only inside the selected BotBlade-managed bot container.",
        ),
    )
}
