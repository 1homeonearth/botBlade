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

data class BotCommandPermissions(
    val defaultMemberPermissions: String? = null,
    val dmPermission: Boolean = false,
)

data class BotCommandHandler(
    val kind: String = "static_response",
    val ephemeral: Boolean = true,
    val content: String? = null,
)

data class BotCommand(
    val id: String? = null,
    val name: String,
    val description: String,
    val type: String = "chat_input",
    val permissions: BotCommandPermissions = BotCommandPermissions(),
    val handler: BotCommandHandler = BotCommandHandler(),
)

data class GitHubProjectConfig(
    val owner: String? = null,
    val repo: String? = null,
    val defaultBranch: String = "main",
    val lastPushedAt: String? = null,
)

fun GitHubProjectConfig.displayNameOrNull(): String? {
    val displayOwner = owner?.trim()
    val displayRepo = repo?.trim()
    return if (!displayOwner.isNullOrBlank() && !displayRepo.isNullOrBlank()) "$displayOwner/$displayRepo" else null
}

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
    val commands: List<BotCommand> = emptyList(),
    val deployment: ProjectDeployment = ProjectDeployment(),
    val github: GitHubProjectConfig? = null,
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

data class ProjectFileSummary(
    val path: String,
    val size: Long,
    val updatedAt: String,
    val generated: Boolean,
    val editable: Boolean,
)

data class ProjectFileContent(
    val path: String,
    val size: Long,
    val updatedAt: String,
    val generated: Boolean,
    val editable: Boolean,
    val content: String,
)

data class SecretSummary(
    val id: String,
    val projectId: String?,
    val name: String,
    val type: String,
    val storageMode: String,
    val fingerprint: String,
    val createdAt: String,
    val updatedAt: String,
    val rotatedAt: String?,
)

data class SecretCreateRequest(
    val projectId: String? = null,
    val name: String,
    val type: String,
    val value: String,
)

data class BuildRequest(
    val source: String = "current_project",
    val clean: Boolean = true,
    val runTests: Boolean = true,
    val createDockerImage: Boolean = false,
)

data class BuildSummary(
    val buildId: String,
    val projectId: String,
    val status: String,
    val logUrl: String?,
    val auditEventId: String?,
    val startedAt: String?,
    val finishedAt: String?,
    val errorMessage: String?,
)

data class RuntimeStatusResponse(
    val projectId: String,
    val status: String,
    val running: Boolean = false,
    val message: String? = null,
)

data class DeploymentTargetSummary(
    val id: String,
    val name: String,
    val type: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class DeploymentTargetCreateRequest(
    val name: String,
    val type: String,
)

data class DeploymentTargetTestResponse(
    val ok: Boolean,
    val status: String,
    val message: String,
)

data class DeploymentJobSummary(
    val deploymentId: String,
    val projectId: String,
    val targetId: String,
    val buildId: String,
    val status: String,
    val createdAt: String? = null,
    val finishedAt: String? = null,
    val errorMessage: String? = null,
)

data class DeploymentCreateRequest(
    val targetId: String,
    val buildId: String,
)

data class GitHubStatusResponse(
    val connected: Boolean,
    val tokenSecretRef: String? = null,
    val message: String? = null,
)


data class CommandCreateRequest(
    val name: String,
    val description: String,
    val handlerKind: String = "static_response",
    val handlerContent: String = "",
    val ephemeral: Boolean = true,
)

data class GitHubConnectRequest(
    val tokenSecretRef: String,
)

data class GitHubLinkRepoRequest(
    val owner: String,
    val repo: String,
    val defaultBranch: String = "main",
)

data class GitHubWorkflowResponse(
    val path: String,
    val content: String,
)
