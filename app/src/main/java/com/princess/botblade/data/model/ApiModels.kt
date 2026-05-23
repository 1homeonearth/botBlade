// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.model  // line 7: executes this statement as part of this file's behavior

data class HealthResponse(  // line 9: executes this statement as part of this file's behavior
    val status: String,  // line 10: executes this statement as part of this file's behavior
    val message: String? = null,  // line 11: executes this statement as part of this file's behavior
)  // line 12: executes this statement as part of this file's behavior

data class BotStatusResponse(  // line 14: executes this statement as part of this file's behavior
    val status: String,  // line 15: executes this statement as part of this file's behavior
    val message: String? = null,  // line 16: executes this statement as part of this file's behavior
)  // line 17: executes this statement as part of this file's behavior

data class BotToggleRequest(  // line 19: executes this statement as part of this file's behavior
    val action: String,  // line 20: executes this statement as part of this file's behavior
)  // line 21: executes this statement as part of this file's behavior

data class BotToggleResponse(  // line 23: executes this statement as part of this file's behavior
    val status: String? = null,  // line 24: executes this statement as part of this file's behavior
    val action: String? = null,  // line 25: executes this statement as part of this file's behavior
    val message: String? = null,  // line 26: executes this statement as part of this file's behavior
)  // line 27: executes this statement as part of this file's behavior

data class ApiErrorResponse(  // line 29: executes this statement as part of this file's behavior
    val error: String? = null,  // line 30: executes this statement as part of this file's behavior
    val message: String? = null,  // line 31: executes this statement as part of this file's behavior
    val statusCode: Int? = null,  // line 32: executes this statement as part of this file's behavior
)  // line 33: executes this statement as part of this file's behavior

data class ProjectSummary(  // line 35: executes this statement as part of this file's behavior
    val id: String,  // line 36: executes this statement as part of this file's behavior
    val name: String,  // line 37: executes this statement as part of this file's behavior
    val description: String? = null,  // line 38: executes this statement as part of this file's behavior
    val runtimeStatus: String? = null,  // line 39: executes this statement as part of this file's behavior
)  // line 40: executes this statement as part of this file's behavior

data class DiscordProjectConfig(  // line 42: executes this statement as part of this file's behavior
    val applicationId: String? = null,  // line 43: executes this statement as part of this file's behavior
    val clientId: String? = null,  // line 44: executes this statement as part of this file's behavior
    val defaultGuildId: String? = null,  // line 45: executes this statement as part of this file's behavior
    val tokenSecretRef: String? = null,  // line 46: executes this statement as part of this file's behavior
    val commandRegistration: String = "guild",  // line 47: executes this statement as part of this file's behavior
)  // line 48: executes this statement as part of this file's behavior

data class ProjectPermissions(  // line 50: executes this statement as part of this file's behavior
    val intents: List<String> = emptyList(),  // line 51: executes this statement as part of this file's behavior
    val botPermissions: List<String> = emptyList(),  // line 52: executes this statement as part of this file's behavior
)  // line 53: executes this statement as part of this file's behavior

data class ProjectDeployment(  // line 55: executes this statement as part of this file's behavior
    val targetId: String? = null,  // line 56: executes this statement as part of this file's behavior
    val lastDeploymentId: String? = null,  // line 57: executes this statement as part of this file's behavior
)  // line 58: executes this statement as part of this file's behavior

data class BotCommandPermissions(  // line 60: executes this statement as part of this file's behavior
    val defaultMemberPermissions: String? = null,  // line 61: executes this statement as part of this file's behavior
    val dmPermission: Boolean = false,  // line 62: executes this statement as part of this file's behavior
)  // line 63: executes this statement as part of this file's behavior

data class BotCommandHandler(  // line 65: executes this statement as part of this file's behavior
    val kind: String = "static_response",  // line 66: executes this statement as part of this file's behavior
    val ephemeral: Boolean = true,  // line 67: executes this statement as part of this file's behavior
    val content: String? = null,  // line 68: executes this statement as part of this file's behavior
)  // line 69: executes this statement as part of this file's behavior

data class BotCommand(  // line 71: executes this statement as part of this file's behavior
    val id: String? = null,  // line 72: executes this statement as part of this file's behavior
    val name: String,  // line 73: executes this statement as part of this file's behavior
    val description: String,  // line 74: executes this statement as part of this file's behavior
    val type: String = "chat_input",  // line 75: executes this statement as part of this file's behavior
    val permissions: BotCommandPermissions = BotCommandPermissions(),  // line 76: executes this statement as part of this file's behavior
    val handler: BotCommandHandler = BotCommandHandler(),  // line 77: executes this statement as part of this file's behavior
)  // line 78: executes this statement as part of this file's behavior

data class GitHubProjectConfig(  // line 80: executes this statement as part of this file's behavior
    val owner: String? = null,  // line 81: executes this statement as part of this file's behavior
    val repo: String? = null,  // line 82: executes this statement as part of this file's behavior
    val defaultBranch: String = "main",  // line 83: executes this statement as part of this file's behavior
    val lastPushedAt: String? = null,  // line 84: executes this statement as part of this file's behavior
)  // line 85: executes this statement as part of this file's behavior

fun GitHubProjectConfig.displayNameOrNull(): String? {  // line 87: executes this statement as part of this file's behavior
    val displayOwner = owner?.trim()  // line 88: executes this statement as part of this file's behavior
    val displayRepo = repo?.trim()  // line 89: executes this statement as part of this file's behavior
    return if (!displayOwner.isNullOrBlank() && !displayRepo.isNullOrBlank()) "$displayOwner/$displayRepo" else null  // line 90: executes this statement as part of this file's behavior
}  // line 91: executes this statement as part of this file's behavior

data class BotProject(  // line 93: executes this statement as part of this file's behavior
    val id: String,  // line 94: executes this statement as part of this file's behavior
    val name: String,  // line 95: executes this statement as part of this file's behavior
    val slug: String,  // line 96: executes this statement as part of this file's behavior
    val description: String,  // line 97: executes this statement as part of this file's behavior
    val templateId: String,  // line 98: executes this statement as part of this file's behavior
    val language: String,  // line 99: executes this statement as part of this file's behavior
    val runtime: String,  // line 100: executes this statement as part of this file's behavior
    val discord: DiscordProjectConfig,  // line 101: executes this statement as part of this file's behavior
    val permissions: ProjectPermissions,  // line 102: executes this statement as part of this file's behavior
    val commands: List<BotCommand> = emptyList(),  // line 103: executes this statement as part of this file's behavior
    val deployment: ProjectDeployment = ProjectDeployment(),  // line 104: executes this statement as part of this file's behavior
    val github: GitHubProjectConfig? = null,  // line 105: executes this statement as part of this file's behavior
    val archivedAt: String? = null,  // line 106: executes this statement as part of this file's behavior
    val createdAt: String,  // line 107: executes this statement as part of this file's behavior
    val updatedAt: String,  // line 108: executes this statement as part of this file's behavior
)  // line 109: executes this statement as part of this file's behavior

data class ProjectCreateRequest(  // line 111: executes this statement as part of this file's behavior
    val name: String,  // line 112: executes this statement as part of this file's behavior
    val description: String = "",  // line 113: executes this statement as part of this file's behavior
    val templateId: String = "template_blank_discord_ts",  // line 114: executes this statement as part of this file's behavior
    val runtime: String = "node22",  // line 115: executes this statement as part of this file's behavior
)  // line 116: executes this statement as part of this file's behavior

data class ProjectUpdateRequest(  // line 118: executes this statement as part of this file's behavior
    val name: String? = null,  // line 119: executes this statement as part of this file's behavior
    val description: String? = null,  // line 120: executes this statement as part of this file's behavior
    val templateId: String? = null,  // line 121: executes this statement as part of this file's behavior
)  // line 122: executes this statement as part of this file's behavior

data class ProjectFileSummary(  // line 124: executes this statement as part of this file's behavior
    val path: String,  // line 125: executes this statement as part of this file's behavior
    val size: Long,  // line 126: executes this statement as part of this file's behavior
    val updatedAt: String,  // line 127: executes this statement as part of this file's behavior
    val generated: Boolean,  // line 128: executes this statement as part of this file's behavior
    val editable: Boolean,  // line 129: executes this statement as part of this file's behavior
)  // line 130: executes this statement as part of this file's behavior

data class ProjectFileContent(  // line 132: executes this statement as part of this file's behavior
    val path: String,  // line 133: executes this statement as part of this file's behavior
    val size: Long,  // line 134: executes this statement as part of this file's behavior
    val updatedAt: String,  // line 135: executes this statement as part of this file's behavior
    val generated: Boolean,  // line 136: executes this statement as part of this file's behavior
    val editable: Boolean,  // line 137: executes this statement as part of this file's behavior
    val content: String,  // line 138: executes this statement as part of this file's behavior
)  // line 139: executes this statement as part of this file's behavior

data class SecretSummary(  // line 141: executes this statement as part of this file's behavior
    val id: String,  // line 142: executes this statement as part of this file's behavior
    val projectId: String?,  // line 143: executes this statement as part of this file's behavior
    val name: String,  // line 144: executes this statement as part of this file's behavior
    val type: String,  // line 145: executes this statement as part of this file's behavior
    val storageMode: String,  // line 146: executes this statement as part of this file's behavior
    val fingerprint: String,  // line 147: executes this statement as part of this file's behavior
    val createdAt: String,  // line 148: executes this statement as part of this file's behavior
    val updatedAt: String,  // line 149: executes this statement as part of this file's behavior
    val rotatedAt: String?,  // line 150: executes this statement as part of this file's behavior
)  // line 151: executes this statement as part of this file's behavior

data class SecretCreateRequest(  // line 153: executes this statement as part of this file's behavior
    val projectId: String? = null,  // line 154: executes this statement as part of this file's behavior
    val name: String,  // line 155: executes this statement as part of this file's behavior
    val type: String,  // line 156: executes this statement as part of this file's behavior
    val value: String,  // line 157: executes this statement as part of this file's behavior
)  // line 158: executes this statement as part of this file's behavior

data class BuildRequest(  // line 160: executes this statement as part of this file's behavior
    val source: String = "current_project",  // line 161: executes this statement as part of this file's behavior
    val clean: Boolean = true,  // line 162: executes this statement as part of this file's behavior
    val runTests: Boolean = true,  // line 163: executes this statement as part of this file's behavior
    val createDockerImage: Boolean = false,  // line 164: executes this statement as part of this file's behavior
)  // line 165: executes this statement as part of this file's behavior

data class BuildSummary(  // line 167: executes this statement as part of this file's behavior
    val buildId: String,  // line 168: executes this statement as part of this file's behavior
    val projectId: String,  // line 169: executes this statement as part of this file's behavior
    val status: String,  // line 170: executes this statement as part of this file's behavior
    val logUrl: String?,  // line 171: executes this statement as part of this file's behavior
    val auditEventId: String?,  // line 172: executes this statement as part of this file's behavior
    val startedAt: String?,  // line 173: executes this statement as part of this file's behavior
    val finishedAt: String?,  // line 174: executes this statement as part of this file's behavior
    val errorMessage: String?,  // line 175: executes this statement as part of this file's behavior
)  // line 176: executes this statement as part of this file's behavior

data class RuntimeStatusResponse(  // line 178: executes this statement as part of this file's behavior
    val projectId: String,  // line 179: executes this statement as part of this file's behavior
    val status: String,  // line 180: executes this statement as part of this file's behavior
    val running: Boolean = false,  // line 181: executes this statement as part of this file's behavior
    val message: String? = null,  // line 182: executes this statement as part of this file's behavior
)  // line 183: executes this statement as part of this file's behavior

data class DeploymentAdapterCapabilities(  // line 185: executes this statement as part of this file's behavior
    val supported: Boolean = false,  // line 186: executes this statement as part of this file's behavior
    val actions: Map<String, Boolean> = emptyMap(),  // line 187: executes this statement as part of this file's behavior
    val notes: List<String> = emptyList(),  // line 188: executes this statement as part of this file's behavior
)  // line 189: executes this statement as part of this file's behavior

data class DeploymentTargetSummary(  // line 191: executes this statement as part of this file's behavior
    val id: String,  // line 192: executes this statement as part of this file's behavior
    val name: String,  // line 193: executes this statement as part of this file's behavior
    val type: String,  // line 194: executes this statement as part of this file's behavior
    val capabilities: DeploymentAdapterCapabilities = DeploymentAdapterCapabilities(),  // line 195: executes this statement as part of this file's behavior
    val createdAt: String? = null,  // line 196: executes this statement as part of this file's behavior
    val updatedAt: String? = null,  // line 197: executes this statement as part of this file's behavior
)  // line 198: executes this statement as part of this file's behavior

data class DeploymentTargetCreateRequest(  // line 200: executes this statement as part of this file's behavior
    val name: String,  // line 201: executes this statement as part of this file's behavior
    val type: String,  // line 202: executes this statement as part of this file's behavior
)  // line 203: executes this statement as part of this file's behavior

data class DeploymentTargetTestResponse(  // line 205: executes this statement as part of this file's behavior
    val ok: Boolean,  // line 206: executes this statement as part of this file's behavior
    val status: String,  // line 207: executes this statement as part of this file's behavior
    val message: String,  // line 208: executes this statement as part of this file's behavior
)  // line 209: executes this statement as part of this file's behavior

data class DeploymentJobSummary(  // line 211: executes this statement as part of this file's behavior
    val deploymentId: String,  // line 212: executes this statement as part of this file's behavior
    val projectId: String,  // line 213: executes this statement as part of this file's behavior
    val targetId: String,  // line 214: executes this statement as part of this file's behavior
    val buildId: String,  // line 215: executes this statement as part of this file's behavior
    val status: String,  // line 216: executes this statement as part of this file's behavior
    val createdAt: String? = null,  // line 217: executes this statement as part of this file's behavior
    val finishedAt: String? = null,  // line 218: executes this statement as part of this file's behavior
    val errorMessage: String? = null,  // line 219: executes this statement as part of this file's behavior
)  // line 220: executes this statement as part of this file's behavior

data class DeploymentActionResponse(  // line 222: executes this statement as part of this file's behavior
    val status: String? = null,  // line 223: executes this statement as part of this file's behavior
    val running: Boolean = false,  // line 224: executes this statement as part of this file's behavior
    val message: String? = null,  // line 225: executes this statement as part of this file's behavior
)  // line 226: executes this statement as part of this file's behavior

data class DeploymentCreateRequest(  // line 228: executes this statement as part of this file's behavior
    val targetId: String,  // line 229: executes this statement as part of this file's behavior
    val buildId: String,  // line 230: executes this statement as part of this file's behavior
)  // line 231: executes this statement as part of this file's behavior

data class GitHubStatusResponse(  // line 233: executes this statement as part of this file's behavior
    val connected: Boolean,  // line 234: executes this statement as part of this file's behavior
    val tokenSecretRef: String? = null,  // line 235: executes this statement as part of this file's behavior
    val message: String? = null,  // line 236: executes this statement as part of this file's behavior
)  // line 237: executes this statement as part of this file's behavior


data class CommandCreateRequest(  // line 240: executes this statement as part of this file's behavior
    val name: String,  // line 241: executes this statement as part of this file's behavior
    val description: String,  // line 242: executes this statement as part of this file's behavior
    val handlerKind: String = "static_response",  // line 243: executes this statement as part of this file's behavior
    val handlerContent: String = "",  // line 244: executes this statement as part of this file's behavior
    val ephemeral: Boolean = true,  // line 245: executes this statement as part of this file's behavior
)  // line 246: executes this statement as part of this file's behavior

data class GitHubConnectRequest(  // line 248: executes this statement as part of this file's behavior
    val tokenSecretRef: String,  // line 249: executes this statement as part of this file's behavior
)  // line 250: executes this statement as part of this file's behavior

data class GitHubLinkRepoRequest(  // line 252: executes this statement as part of this file's behavior
    val owner: String,  // line 253: executes this statement as part of this file's behavior
    val repo: String,  // line 254: executes this statement as part of this file's behavior
    val defaultBranch: String = "main",  // line 255: executes this statement as part of this file's behavior
)  // line 256: executes this statement as part of this file's behavior

data class GitHubWorkflowResponse(  // line 258: executes this statement as part of this file's behavior
    val path: String,  // line 259: executes this statement as part of this file's behavior
    val content: String,  // line 260: executes this statement as part of this file's behavior
)  // line 261: executes this statement as part of this file's behavior

data class ScanDetectionMatch(  // line 263: executes this statement as part of this file's behavior
    val id: String,  // line 264: executes this statement as part of this file's behavior
    val name: String,  // line 265: executes this statement as part of this file's behavior
    val score: Int,  // line 266: executes this statement as part of this file's behavior
    val confidence: String,  // line 267: executes this statement as part of this file's behavior
    val requiredSecrets: List<String> = emptyList(),  // line 268: executes this statement as part of this file's behavior
)  // line 269: executes this statement as part of this file's behavior

data class ProjectScanResponse(  // line 271: executes this statement as part of this file's behavior
    val recommendedPackId: String,  // line 272: executes this statement as part of this file's behavior
    val matches: List<ScanDetectionMatch> = emptyList(),  // line 273: executes this statement as part of this file's behavior
    val warnings: List<String> = emptyList(),  // line 274: executes this statement as part of this file's behavior
)  // line 275: executes this statement as part of this file's behavior
