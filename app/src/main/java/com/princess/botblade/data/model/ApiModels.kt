package com.princess.botblade.data.model

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

data class DeploymentAdapterCapabilities(
    val supported: Boolean = false,
    val actions: Map<String, Boolean> = emptyMap(),
    val notes: List<String> = emptyList(),
)

data class DeploymentTargetSummary(
    val id: String,
    val name: String,
    val type: String,
    val capabilities: DeploymentAdapterCapabilities = DeploymentAdapterCapabilities(),
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

data class DeploymentActionResponse(
    val status: String? = null,
    val running: Boolean = false,
    val message: String? = null,
)

data class DeploymentCreateRequest(
    val targetId: String,
    val buildId: String,
)

data class ScriptEnvironmentRef(
    val name: String,
    val source: String? = null,
)

data class ScriptSecretRef(
    val id: String,
    val name: String? = null,
    val required: Boolean? = null,
    val configured: Boolean? = null,
)

data class ScriptProfileSummary(
    val id: String,
    val projectId: String? = null,
    val name: String,
    val description: String? = null,
    val source: String,
    val runtime: String,
    val command: List<String> = emptyList(),
    val workingDirectory: String = ".",
    val envRefs: List<ScriptEnvironmentRef> = emptyList(),
    val secretRefs: List<ScriptSecretRef> = emptyList(),
    val timeoutSeconds: Int = 300,
    val requiresConfirmation: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

data class RepairCard(
    val title: String,
    val evidence: String? = null,
    val safeAction: String,
)

data class ProjectProfileImportSource(
    val kind: String,
    val url: String? = null,
)

data class ProjectProfileProject(
    val id: String? = null,
    val name: String,
    val type: String,
    val root: String,
    val importSource: ProjectProfileImportSource? = null,
)

data class ProjectProfileRuntime(
    val type: String,
    val version: String,
    val packageManager: String = "unknown",
    val detectedLanguages: List<String> = emptyList(),
    val detectedFrameworks: List<String> = emptyList(),
)

data class ProjectProfilePackEvidence(
    val id: String,
    val name: String,
    val score: Int,
    val confidence: String,
    val matchedEvidence: List<String> = emptyList(),
)

data class ProjectProfileBladePack(
    val selected: String,
    val version: String,
    val detected: List<ProjectProfilePackEvidence> = emptyList(),
)

data class ProjectProfileCommandPlan(
    val install: List<String> = emptyList(),
    val build: List<String> = emptyList(),
    val test: List<String> = emptyList(),
    val validate: List<String> = emptyList(),
    val start: List<String> = emptyList(),
    val stop: List<String> = emptyList(),
    val restart: List<String> = emptyList(),
    val deploy: List<String> = emptyList(),
)

data class ProjectProfileSecrets(
    val required: List<ScriptSecretRef> = emptyList(),
    val optional: List<ScriptSecretRef> = emptyList(),
)

data class ProjectProfileGitRemote(
    val name: String,
    val url: String? = null,
)

data class ProjectProfileGit(
    val branch: String? = null,
    val status: String = "unknown",
    val dirtyFileCount: Int = 0,
    val remotes: List<ProjectProfileGitRemote> = emptyList(),
)

data class ProjectProfileResponse(
    val schemaVersion: String,
    val generatedBy: String,
    val generatedAt: String,
    val project: ProjectProfileProject? = null,
    val runtime: ProjectProfileRuntime? = null,
    val bladePack: ProjectProfileBladePack? = null,
    val commandPlan: ProjectProfileCommandPlan = ProjectProfileCommandPlan(),
    val scriptProfiles: List<ScriptProfileSummary> = emptyList(),
    val secrets: ProjectProfileSecrets = ProjectProfileSecrets(),
    val permissions: List<String> = emptyList(),
    val capabilities: List<String> = emptyList(),
    val importantFiles: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val repairCards: List<RepairCard> = emptyList(),
    val git: ProjectProfileGit? = null,
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

data class ScanDetectionMatch(
    val id: String,
    val name: String,
    val score: Int,
    val confidence: String,
    val requiredSecrets: List<String> = emptyList(),
)

data class ProjectScanResponse(
    val recommendedPackId: String,
    val matches: List<ScanDetectionMatch> = emptyList(),
    val warnings: List<String> = emptyList(),
)


data class ImportStartRequest(
    val sourceType: String,
    val source: String,
    val workspacePath: String,
)

data class ImportSummary(
    val id: String,
    val state: String,
    val profileId: String? = null,
    val blockedPolicy: String? = null,
)


data class GitRemoteSummary(
    val name: String,
    val url: String? = null,
)

data class GitChangedFileSummary(
    val path: String,
    val status: String,
)

data class GitStatusApiSummary(
    val available: Boolean = false,
    val branch: String? = null,
    val remotes: List<GitRemoteSummary> = emptyList(),
    val clean: Boolean = true,
    val dirtyFileCount: Int = 0,
    val changedFiles: List<GitChangedFileSummary> = emptyList(),
    val note: String? = null,
)
