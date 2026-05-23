package com.princess.botblade.ui.dashboard

internal data class CommercialHomeLane(
    val title: String,
    val tag: String,
    val detail: String,
)

internal object CommercialHomeDeck {
    val primaryActions = listOf(
        CommercialHomeLane(
            title = "Import",
            tag = "Git, zip, folder",
            detail = "Bring in bots from GitHub, local archives, workspace folders, and Blade Pack templates.",
        ),
        CommercialHomeLane(
            title = "Repair",
            tag = "First check",
            detail = "Detect package managers, runtimes, entrypoints, scripts, secrets, unsafe defaults, and missing metadata.",
        ),
        CommercialHomeLane(
            title = "Edit",
            tag = "Forge Editor",
            detail = "Use a mobile IDE surface inspired by Monaco, VS Code, CodeMirror, and Acode.",
        ),
        CommercialHomeLane(
            title = "Run",
            tag = "Container runtime",
            detail = "Start the selected bot system, stream logs, inspect health, and open a container-scoped terminal.",
        ),
        CommercialHomeLane(
            title = "Deploy",
            tag = "Release rail",
            detail = "Build artifacts, manage targets, publish APKs, push workflow previews, and prepare release notes.",
        ),
        CommercialHomeLane(
            title = "Observe",
            tag = "Sentry lane",
            detail = "Track startup diagnostics, build logs, audit events, crashes, warnings, and repair prompts.",
        ),
    )

    val workspaceHealth = listOf(
        CommercialHomeLane("Backend", "local API", "Connection, health, startup diagnostics, and selected runtime target."),
        CommercialHomeLane("Project", "active forge", "Current workspace, generated files, Git link, package manager, and build status."),
        CommercialHomeLane("Vault", "secret refs", "Encrypted values, required secrets, rotation, deletion, redaction, and audit trail."),
        CommercialHomeLane("Updates", "release channel", "GitHub release checks, APK asset discovery, signing guidance, and install handoff."),
    )
}
