package com.princess.royalscepter.data.model

data class HealthResponse(
    val status: String,
    val message: String? = null,
)

data class BotStatusResponse(
    val status: String,
    val message: String? = null,
)

data class BotToggleRequest(
    val action: String,
)

data class BotToggleResponse(
    val status: String? = null,
    val action: String? = null,
    val message: String? = null,
)

data class ApiErrorResponse(
    val error: String? = null,
    val message: String? = null,
    val statusCode: Int? = null,
)

data class ProjectSummary(
    val id: String,
    val name: String,
    val description: String? = null,
    val runtimeStatus: String? = null,
)

data class DiscordProjectConfig(
    val applicationId: String? = null,
    val clientId: String? = null,
    val defaultGuildId: String? = null,
    val tokenSecretRef: String? = null,
    val commandRegistration: String = "guild",
)

data class ProjectPermissions(
    val intents: List<String> = emptyList(),
    val botPermissions: List<String> = emptyList(),
)

data class ProjectDeployment(
    val targetId: String? = null,
    val lastDeploymentId: String? = null,
)

data class BotProject(
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val templateId: String,
    val language: String,
    val runtime: String,
    val discord: DiscordProjectConfig,
    val permissions: ProjectPermissions,
    val archivedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

data class ProjectCreateRequest(
    val name: String,
    val description: String = "",
    val templateId: String = "template_blank_discord_ts",
    val runtime: String = "node22",
)

data class ProjectUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val templateId: String? = null,
)
