// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.api  // line 7: executes this statement as part of this file's behavior

import com.princess.botblade.data.model.ApiErrorResponse  // line 9: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotProject  // line 10: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotStatusResponse  // line 11: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotToggleRequest  // line 12: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotToggleResponse  // line 13: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DiscordProjectConfig  // line 14: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.HealthResponse  // line 15: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectCreateRequest  // line 16: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectDeployment  // line 17: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectPermissions  // line 18: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectUpdateRequest  // line 19: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.SecretSummary  // line 20: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.SecretCreateRequest  // line 21: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectFileSummary  // line 22: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectFileContent  // line 23: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BuildSummary  // line 24: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BuildRequest  // line 25: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.RuntimeStatusResponse  // line 26: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectScanResponse  // line 27: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ScanDetectionMatch  // line 28: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentTargetSummary  // line 29: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentTargetCreateRequest  // line 30: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentTargetTestResponse  // line 31: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentJobSummary  // line 32: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentCreateRequest  // line 33: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentActionResponse  // line 34: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentAdapterCapabilities  // line 35: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubStatusResponse  // line 36: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotCommand  // line 37: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotCommandHandler  // line 38: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotCommandPermissions  // line 39: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.CommandCreateRequest  // line 40: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubConnectRequest  // line 41: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubLinkRepoRequest  // line 42: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubProjectConfig  // line 43: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubWorkflowResponse  // line 44: executes this statement as part of this file's behavior
import java.io.IOException  // line 45: executes this statement as part of this file's behavior
import java.net.HttpURLConnection  // line 46: executes this statement as part of this file's behavior
import java.net.URL  // line 47: executes this statement as part of this file's behavior
import java.net.URLEncoder  // line 48: executes this statement as part of this file's behavior
import java.nio.charset.StandardCharsets  // line 49: executes this statement as part of this file's behavior
import org.json.JSONArray  // line 50: executes this statement as part of this file's behavior
import org.json.JSONObject  // line 51: executes this statement as part of this file's behavior

class BotBladeApiClient(  // line 53: executes this statement as part of this file's behavior
    private val baseUrl: String? = null,  // line 54: executes this statement as part of this file's behavior
    private val bearerToken: String = ApiConfig.DEFAULT_BEARER_TOKEN,  // line 55: executes this statement as part of this file's behavior
    private val sessionToken: String = ApiConfig.DEFAULT_SESSION_TOKEN,  // line 56: executes this statement as part of this file's behavior
) {  // line 57: executes this statement as part of this file's behavior
    @Throws(IOException::class)  // line 58: executes this statement as part of this file's behavior
    fun getHealth(): HealthResponse {  // line 59: executes this statement as part of this file's behavior
        val body = request(path = "/api/health", method = "GET")  // line 60: executes this statement as part of this file's behavior
        val json = body.asJsonOrNull()  // line 61: executes this statement as part of this file's behavior
        return HealthResponse(  // line 62: executes this statement as part of this file's behavior
            status = json?.optionalString("status") ?: json?.optionalString("service") ?: body.ifBlank { "unknown" },  // line 63: executes this statement as part of this file's behavior
            message = json?.optionalString("message"),  // line 64: executes this statement as part of this file's behavior
        )  // line 65: executes this statement as part of this file's behavior
    }  // line 66: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 68: executes this statement as part of this file's behavior
    fun getBotStatus(): BotStatusResponse {  // line 69: executes this statement as part of this file's behavior
        val body = request(path = "/api/bot-status/", method = "GET")  // line 70: executes this statement as part of this file's behavior
        val json = body.asJsonOrNull()  // line 71: executes this statement as part of this file's behavior
        return BotStatusResponse(  // line 72: executes this statement as part of this file's behavior
            status = json?.optionalString("status")  // line 73: executes this statement as part of this file's behavior
                ?: json?.optionalString("bot_status")  // line 74: executes this statement as part of this file's behavior
                ?: json?.optionalString("state")  // line 75: executes this statement as part of this file's behavior
                ?: body.normalizePlainStatus(),  // line 76: executes this statement as part of this file's behavior
            message = json?.optionalString("message"),  // line 77: executes this statement as part of this file's behavior
        )  // line 78: executes this statement as part of this file's behavior
    }  // line 79: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 81: executes this statement as part of this file's behavior
    fun toggleBot(request: BotToggleRequest): BotToggleResponse {  // line 82: executes this statement as part of this file's behavior
        val payload = JSONObject().put("action", request.action).toString()  // line 83: executes this statement as part of this file's behavior
        val body = request(path = "/api/bot-toggle/", method = "POST", requestBody = payload)  // line 84: executes this statement as part of this file's behavior
        val json = body.asJsonOrNull()  // line 85: executes this statement as part of this file's behavior
        return BotToggleResponse(  // line 86: executes this statement as part of this file's behavior
            status = json?.optionalString("status")  // line 87: executes this statement as part of this file's behavior
                ?: json?.optionalString("bot_status")  // line 88: executes this statement as part of this file's behavior
                ?: body.normalizePlainStatus().takeIf { json == null },  // line 89: executes this statement as part of this file's behavior
            action = json?.optionalString("action") ?: request.action,  // line 90: executes this statement as part of this file's behavior
            message = json?.optionalString("message") ?: body.takeIf { it.isNotBlank() && json == null },  // line 91: executes this statement as part of this file's behavior
        )  // line 92: executes this statement as part of this file's behavior
    }  // line 93: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 95: executes this statement as part of this file's behavior
    fun listProjects(): List<BotProject> {  // line 96: executes this statement as part of this file's behavior
        val body = request(path = "/api/projects", method = "GET")  // line 97: executes this statement as part of this file's behavior
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid projects response." }  // line 98: executes this statement as part of this file's behavior
        val projects = json.optJSONArray("projects") ?: JSONArray()  // line 99: executes this statement as part of this file's behavior
        return (0 until projects.length()).map { index -> projects.getJSONObject(index).toBotProject() }  // line 100: executes this statement as part of this file's behavior
    }  // line 101: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 103: executes this statement as part of this file's behavior
    fun createProject(request: ProjectCreateRequest): BotProject {  // line 104: executes this statement as part of this file's behavior
        val payload = JSONObject()  // line 105: executes this statement as part of this file's behavior
            .put("name", request.name)  // line 106: executes this statement as part of this file's behavior
            .put("description", request.description)  // line 107: executes this statement as part of this file's behavior
            .put("templateId", request.templateId)  // line 108: executes this statement as part of this file's behavior
            .put("runtime", request.runtime)  // line 109: executes this statement as part of this file's behavior
            .toString()  // line 110: executes this statement as part of this file's behavior
        return request(path = "/api/projects", method = "POST", requestBody = payload).toProjectResponse()  // line 111: executes this statement as part of this file's behavior
    }  // line 112: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 114: executes this statement as part of this file's behavior
    fun getProject(projectId: String): BotProject =  // line 115: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}", method = "GET").toProjectResponse()  // line 116: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 118: executes this statement as part of this file's behavior
    fun updateProject(projectId: String, request: ProjectUpdateRequest): BotProject {  // line 119: executes this statement as part of this file's behavior
        val payload = JSONObject().apply {  // line 120: executes this statement as part of this file's behavior
            request.name?.let { put("name", it) }  // line 121: executes this statement as part of this file's behavior
            request.description?.let { put("description", it) }  // line 122: executes this statement as part of this file's behavior
            request.templateId?.let { put("templateId", it) }  // line 123: executes this statement as part of this file's behavior
        }.toString()  // line 124: executes this statement as part of this file's behavior
        return request(path = "/api/projects/${projectId.encodedPathSegment()}", method = "PATCH", requestBody = payload).toProjectResponse()  // line 125: executes this statement as part of this file's behavior
    }  // line 126: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 128: executes this statement as part of this file's behavior
    fun archiveProject(projectId: String): BotProject =  // line 129: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/archive", method = "POST", requestBody = "{}").toProjectResponse()  // line 130: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 132: executes this statement as part of this file's behavior
    fun cloneProject(projectId: String): BotProject =  // line 133: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/clone", method = "POST", requestBody = "{}").toProjectResponse()  // line 134: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 136: executes this statement as part of this file's behavior
    fun listCommands(projectId: String): List<BotCommand> {  // line 137: executes this statement as part of this file's behavior
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/commands", method = "GET")  // line 138: executes this statement as part of this file's behavior
        val commands = requireNotNull(body.asJsonOrNull()) { "Invalid commands response." }.optJSONArray("commands") ?: JSONArray()  // line 139: executes this statement as part of this file's behavior
        return (0 until commands.length()).map { index -> commands.getJSONObject(index).toBotCommand() }  // line 140: executes this statement as part of this file's behavior
    }  // line 141: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 143: executes this statement as part of this file's behavior
    fun createCommand(projectId: String, command: CommandCreateRequest): BotCommand =  // line 144: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/commands", method = "POST", requestBody = command.toCommandPayload()).toCommandResponse()  // line 145: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 147: executes this statement as part of this file's behavior
    fun updateCommand(projectId: String, commandId: String, command: CommandCreateRequest): BotCommand =  // line 148: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/commands/${commandId.encodedPathSegment()}", method = "PATCH", requestBody = command.toCommandPayload()).toCommandResponse()  // line 149: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 151: executes this statement as part of this file's behavior
    fun deleteCommand(projectId: String, commandId: String) {  // line 152: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/commands/${commandId.encodedPathSegment()}", method = "DELETE")  // line 153: executes this statement as part of this file's behavior
    }  // line 154: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 156: executes this statement as part of this file's behavior
    fun connectGitHub(request: GitHubConnectRequest): GitHubStatusResponse {  // line 157: executes this statement as part of this file's behavior
        val payload = JSONObject().put("tokenSecretRef", request.tokenSecretRef).toString()  // line 158: executes this statement as part of this file's behavior
        return request(path = "/api/github/connect", method = "POST", requestBody = payload).toGitHubStatusResponse()  // line 159: executes this statement as part of this file's behavior
    }  // line 160: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 162: executes this statement as part of this file's behavior
    fun linkGitHubRepo(projectId: String, request: GitHubLinkRepoRequest): BotProject {  // line 163: executes this statement as part of this file's behavior
        val payload = JSONObject()  // line 164: executes this statement as part of this file's behavior
            .put("owner", request.owner)  // line 165: executes this statement as part of this file's behavior
            .put("repo", request.repo)  // line 166: executes this statement as part of this file's behavior
            .put("defaultBranch", request.defaultBranch)  // line 167: executes this statement as part of this file's behavior
            .toString()  // line 168: executes this statement as part of this file's behavior
        return request(path = "/api/projects/${projectId.encodedPathSegment()}/github/create-repo", method = "POST", requestBody = payload).toProjectResponse()  // line 169: executes this statement as part of this file's behavior
    }  // line 170: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 172: executes this statement as part of this file's behavior
    fun pushGitHub(projectId: String): String =  // line 173: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/github/push", method = "POST", requestBody = "{}")  // line 174: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 176: executes this statement as part of this file's behavior
    fun createGitHubWorkflow(projectId: String): GitHubWorkflowResponse {  // line 177: executes this statement as part of this file's behavior
        val json = requireNotNull(request(path = "/api/projects/${projectId.encodedPathSegment()}/github/create-workflow", method = "POST", requestBody = "{}").asJsonOrNull()) { "Invalid workflow response." }  // line 178: executes this statement as part of this file's behavior
        return GitHubWorkflowResponse(path = json.optString("path"), content = json.optString("content"))  // line 179: executes this statement as part of this file's behavior
    }  // line 180: executes this statement as part of this file's behavior


    @Throws(IOException::class)  // line 183: executes this statement as part of this file's behavior
    fun generateProject(projectId: String): List<ProjectFileSummary> {  // line 184: executes this statement as part of this file's behavior
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/generate", method = "POST", requestBody = "{}")  // line 185: executes this statement as part of this file's behavior
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid generate response." }  // line 186: executes this statement as part of this file's behavior
        return json.optJSONArray("files").toFileSummaries()  // line 187: executes this statement as part of this file's behavior
    }  // line 188: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 190: executes this statement as part of this file's behavior
    fun listProjectFiles(projectId: String): List<ProjectFileSummary> {  // line 191: executes this statement as part of this file's behavior
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/files", method = "GET")  // line 192: executes this statement as part of this file's behavior
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid files response." }  // line 193: executes this statement as part of this file's behavior
        return json.optJSONArray("files").toFileSummaries()  // line 194: executes this statement as part of this file's behavior
    }  // line 195: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 197: executes this statement as part of this file's behavior
    fun getProjectFile(projectId: String, filePath: String): ProjectFileContent {  // line 198: executes this statement as part of this file's behavior
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/files/${filePath.encodedFilePath()}", method = "GET")  // line 199: executes this statement as part of this file's behavior
        return requireNotNull(body.asJsonOrNull()) { "Invalid file response." }.toProjectFileContent()  // line 200: executes this statement as part of this file's behavior
    }  // line 201: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 203: executes this statement as part of this file's behavior
    fun saveProjectFile(projectId: String, filePath: String, content: String): ProjectFileContent {  // line 204: executes this statement as part of this file's behavior
        val payload = JSONObject().put("content", content).toString()  // line 205: executes this statement as part of this file's behavior
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/files/${filePath.encodedFilePath()}", method = "PUT", requestBody = payload)  // line 206: executes this statement as part of this file's behavior
        return requireNotNull(body.asJsonOrNull()) { "Invalid file response." }.toProjectFileContent()  // line 207: executes this statement as part of this file's behavior
    }  // line 208: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 210: executes this statement as part of this file's behavior
    fun listSecrets(): List<SecretSummary> {  // line 211: executes this statement as part of this file's behavior
        val body = request(path = "/api/secrets", method = "GET")  // line 212: executes this statement as part of this file's behavior
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid secrets response." }  // line 213: executes this statement as part of this file's behavior
        val secrets = json.optJSONArray("secrets") ?: JSONArray()  // line 214: executes this statement as part of this file's behavior
        return (0 until secrets.length()).map { index -> secrets.getJSONObject(index).toSecretSummary() }  // line 215: executes this statement as part of this file's behavior
    }  // line 216: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 218: executes this statement as part of this file's behavior
    fun createSecret(request: SecretCreateRequest): SecretSummary {  // line 219: executes this statement as part of this file's behavior
        val payload = JSONObject()  // line 220: executes this statement as part of this file's behavior
            .put("name", request.name)  // line 221: executes this statement as part of this file's behavior
            .put("type", request.type)  // line 222: executes this statement as part of this file's behavior
            .put("value", request.value)  // line 223: executes this statement as part of this file's behavior
            .apply { request.projectId?.let { put("projectId", it) } }  // line 224: executes this statement as part of this file's behavior
            .toString()  // line 225: executes this statement as part of this file's behavior
        return this.request(path = "/api/secrets", method = "POST", requestBody = payload).toSecretResponse()  // line 226: executes this statement as part of this file's behavior
    }  // line 227: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 229: executes this statement as part of this file's behavior
    fun rotateSecret(secretId: String, value: String): SecretSummary {  // line 230: executes this statement as part of this file's behavior
        val payload = JSONObject().put("value", value).toString()  // line 231: executes this statement as part of this file's behavior
        return request(path = "/api/secrets/${secretId.encodedPathSegment()}/rotate", method = "POST", requestBody = payload).toSecretResponse()  // line 232: executes this statement as part of this file's behavior
    }  // line 233: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 235: executes this statement as part of this file's behavior
    fun deleteSecret(secretId: String) {  // line 236: executes this statement as part of this file's behavior
        request(path = "/api/secrets/${secretId.encodedPathSegment()}", method = "DELETE")  // line 237: executes this statement as part of this file's behavior
    }  // line 238: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 240: executes this statement as part of this file's behavior
    fun createBuild(projectId: String, buildRequest: BuildRequest = BuildRequest()): BuildSummary {  // line 241: executes this statement as part of this file's behavior
        val payload = JSONObject()  // line 242: executes this statement as part of this file's behavior
            .put("source", buildRequest.source)  // line 243: executes this statement as part of this file's behavior
            .put("clean", buildRequest.clean)  // line 244: executes this statement as part of this file's behavior
            .put("runTests", buildRequest.runTests)  // line 245: executes this statement as part of this file's behavior
            .put("createDockerImage", buildRequest.createDockerImage)  // line 246: executes this statement as part of this file's behavior
            .toString()  // line 247: executes this statement as part of this file's behavior
        return request(path = "/api/projects/${projectId.encodedPathSegment()}/builds", method = "POST", requestBody = payload).toBuildSummary()  // line 248: executes this statement as part of this file's behavior
    }  // line 249: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 251: executes this statement as part of this file's behavior
    fun listBuilds(projectId: String): List<BuildSummary> {  // line 252: executes this statement as part of this file's behavior
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/builds", method = "GET")  // line 253: executes this statement as part of this file's behavior
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid builds response." }  // line 254: executes this statement as part of this file's behavior
        val builds = json.optJSONArray("builds") ?: JSONArray()  // line 255: executes this statement as part of this file's behavior
        return (0 until builds.length()).map { index -> builds.getJSONObject(index).toBuildSummary() }  // line 256: executes this statement as part of this file's behavior
    }  // line 257: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 259: executes this statement as part of this file's behavior
    fun getBuild(projectId: String, buildId: String): BuildSummary =  // line 260: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/builds/${buildId.encodedPathSegment()}", method = "GET").toBuildSummary()  // line 261: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 263: executes this statement as part of this file's behavior
    fun getBuildLogs(projectId: String, buildId: String): String {  // line 264: executes this statement as part of this file's behavior
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/builds/${buildId.encodedPathSegment()}/logs", method = "GET")  // line 265: executes this statement as part of this file's behavior
        return body.asJsonOrNull()?.optionalString("logs") ?: body  // line 266: executes this statement as part of this file's behavior
    }  // line 267: executes this statement as part of this file's behavior


    @Throws(IOException::class)  // line 270: executes this statement as part of this file's behavior
    fun getProjectRuntimeStatus(projectId: String): RuntimeStatusResponse =  // line 271: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/runtime/status", method = "GET").toRuntimeStatusResponse()  // line 272: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 274: executes this statement as part of this file's behavior
    fun runtimeAction(projectId: String, action: String): RuntimeStatusResponse =  // line 275: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/runtime/${action.encodedPathSegment()}", method = "POST", requestBody = "{}").toRuntimeStatusResponse()  // line 276: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 278: executes this statement as part of this file's behavior
    fun getProjectRuntimeLogs(projectId: String): String {  // line 279: executes this statement as part of this file's behavior
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/runtime/logs", method = "GET")  // line 280: executes this statement as part of this file's behavior
        return body.asJsonOrNull()?.optionalString("logs") ?: body  // line 281: executes this statement as part of this file's behavior
    }  // line 282: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 284: executes this statement as part of this file's behavior
    fun scanProject(projectId: String): ProjectScanResponse =  // line 285: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/scan", method = "POST", requestBody = "{}").toProjectScanResponse()  // line 286: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 288: executes this statement as part of this file's behavior
    fun listDeploymentTargets(): List<DeploymentTargetSummary> {  // line 289: executes this statement as part of this file's behavior
        val body = request(path = "/api/deployment-targets", method = "GET")  // line 290: executes this statement as part of this file's behavior
        val targets = requireNotNull(body.asJsonOrNull()) { "Invalid deployment targets response." }.optJSONArray("targets") ?: JSONArray()  // line 291: executes this statement as part of this file's behavior
        return (0 until targets.length()).map { index -> targets.getJSONObject(index).toDeploymentTargetSummary() }  // line 292: executes this statement as part of this file's behavior
    }  // line 293: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 295: executes this statement as part of this file's behavior
    fun createDeploymentTarget(request: DeploymentTargetCreateRequest): DeploymentTargetSummary {  // line 296: executes this statement as part of this file's behavior
        val payload = JSONObject().put("name", request.name).put("type", request.type).put("config", JSONObject()).put("secretRefs", JSONArray()).toString()  // line 297: executes this statement as part of this file's behavior
        return this.request(path = "/api/deployment-targets", method = "POST", requestBody = payload).toDeploymentTargetSummary()  // line 298: executes this statement as part of this file's behavior
    }  // line 299: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 301: executes this statement as part of this file's behavior
    fun testDeploymentTarget(targetId: String): DeploymentTargetTestResponse =  // line 302: executes this statement as part of this file's behavior
        request(path = "/api/deployment-targets/${targetId.encodedPathSegment()}/test", method = "POST", requestBody = "{}").toDeploymentTargetTestResponse()  // line 303: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 305: executes this statement as part of this file's behavior
    fun createDeployment(projectId: String, request: DeploymentCreateRequest): DeploymentJobSummary {  // line 306: executes this statement as part of this file's behavior
        val payload = JSONObject().put("targetId", request.targetId).put("buildId", request.buildId).toString()  // line 307: executes this statement as part of this file's behavior
        return this.request(path = "/api/projects/${projectId.encodedPathSegment()}/deployments", method = "POST", requestBody = payload).toDeploymentJobSummary()  // line 308: executes this statement as part of this file's behavior
    }  // line 309: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 311: executes this statement as part of this file's behavior
    fun listDeployments(projectId: String): List<DeploymentJobSummary> {  // line 312: executes this statement as part of this file's behavior
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/deployments", method = "GET")  // line 313: executes this statement as part of this file's behavior
        val deployments = requireNotNull(body.asJsonOrNull()) { "Invalid deployments response." }.optJSONArray("deployments") ?: JSONArray()  // line 314: executes this statement as part of this file's behavior
        return (0 until deployments.length()).map { index -> deployments.getJSONObject(index).toDeploymentJobSummary() }  // line 315: executes this statement as part of this file's behavior
    }  // line 316: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 318: executes this statement as part of this file's behavior
    fun getDeploymentLogs(projectId: String, deploymentId: String): String {  // line 319: executes this statement as part of this file's behavior
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/deployments/${deploymentId.encodedPathSegment()}/logs", method = "GET")  // line 320: executes this statement as part of this file's behavior
        return body.asJsonOrNull()?.optionalString("logs") ?: body  // line 321: executes this statement as part of this file's behavior
    }  // line 322: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 324: executes this statement as part of this file's behavior
    fun getDeploymentStatus(projectId: String, deploymentId: String): DeploymentActionResponse =  // line 325: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/deployments/${deploymentId.encodedPathSegment()}/status", method = "GET").toDeploymentActionResponse()  // line 326: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 328: executes this statement as part of this file's behavior
    fun deploymentAction(projectId: String, deploymentId: String, action: String): DeploymentActionResponse =  // line 329: executes this statement as part of this file's behavior
        request(path = "/api/projects/${projectId.encodedPathSegment()}/deployments/${deploymentId.encodedPathSegment()}/${action.encodedPathSegment()}", method = "POST", requestBody = "{}").toDeploymentActionResponse()  // line 330: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 332: executes this statement as part of this file's behavior
    fun getGitHubStatus(): GitHubStatusResponse =  // line 333: executes this statement as part of this file's behavior
        request(path = "/api/github/status", method = "GET").toGitHubStatusResponse()  // line 334: executes this statement as part of this file's behavior

    @Throws(IOException::class)  // line 336: executes this statement as part of this file's behavior
    private fun request(  // line 337: executes this statement as part of this file's behavior
        path: String,  // line 338: executes this statement as part of this file's behavior
        method: String,  // line 339: executes this statement as part of this file's behavior
        requestBody: String? = null,  // line 340: executes this statement as part of this file's behavior
    ): String {  // line 341: executes this statement as part of this file's behavior
        val requestBaseUrl = baseUrl?.let(ApiConfig::normalizeUrl) ?: ApiConfig.baseUrl  // line 342: executes this statement as part of this file's behavior
        val connection = URL(requestBaseUrl + path).openConnection() as HttpURLConnection  // line 343: executes this statement as part of this file's behavior
        connection.requestMethod = method  // line 344: executes this statement as part of this file's behavior
        connection.connectTimeout = 2_000  // line 345: executes this statement as part of this file's behavior
        connection.readTimeout = 2_000  // line 346: executes this statement as part of this file's behavior
        connection.doInput = true  // line 347: executes this statement as part of this file's behavior
        connection.setRequestProperty("Accept", "application/json")  // line 348: executes this statement as part of this file's behavior
        when {  // line 349: executes this statement as part of this file's behavior
            bearerToken.isNotBlank() -> connection.setRequestProperty("Authorization", "Bearer $bearerToken")  // line 350: executes this statement as part of this file's behavior
            sessionToken.isNotBlank() -> connection.setRequestProperty("Cookie", "botBladeSession=${sessionToken.cookieValue()}")  // line 351: executes this statement as part of this file's behavior
        }  // line 352: executes this statement as part of this file's behavior

        if (requestBody != null) {  // line 354: executes this statement as part of this file's behavior
            connection.doOutput = true  // line 355: executes this statement as part of this file's behavior
            connection.setRequestProperty("Content-Type", "application/json")  // line 356: executes this statement as part of this file's behavior
            connection.outputStream.use { output -> output.write(requestBody.toByteArray()) }  // line 357: executes this statement as part of this file's behavior
        }  // line 358: executes this statement as part of this file's behavior

        return connection.use { http ->  // line 360: executes this statement as part of this file's behavior
            val responseCode = http.responseCode  // line 361: executes this statement as part of this file's behavior
            val responseBody = (if (responseCode in 200..299) http.inputStream else http.errorStream)  // line 362: executes this statement as part of this file's behavior
                ?.bufferedReader()  // line 363: executes this statement as part of this file's behavior
                ?.use { it.readText() }  // line 364: executes this statement as part of this file's behavior
                .orEmpty()  // line 365: executes this statement as part of this file's behavior

            if (responseCode !in 200..299) {  // line 367: executes this statement as part of this file's behavior
                val error = parseError(responseBody, responseCode)  // line 368: executes this statement as part of this file's behavior
                throw IOException(error.message ?: error.error ?: "HTTP $responseCode")  // line 369: executes this statement as part of this file's behavior
            }  // line 370: executes this statement as part of this file's behavior

            responseBody  // line 372: executes this statement as part of this file's behavior
        }  // line 373: executes this statement as part of this file's behavior
    }  // line 374: executes this statement as part of this file's behavior

    private fun parseError(body: String, statusCode: Int): ApiErrorResponse {  // line 376: executes this statement as part of this file's behavior
        val json = body.asJsonOrNull()  // line 377: executes this statement as part of this file's behavior
        val nestedError = json?.optJSONObject("error")  // line 378: executes this statement as part of this file's behavior
        return ApiErrorResponse(  // line 379: executes this statement as part of this file's behavior
            error = nestedError?.optionalString("code") ?: json?.optionalString("error"),  // line 380: executes this statement as part of this file's behavior
            message = nestedError?.optionalString("message") ?: json?.optionalString("message"),  // line 381: executes this statement as part of this file's behavior
            statusCode = statusCode,  // line 382: executes this statement as part of this file's behavior
        )  // line 383: executes this statement as part of this file's behavior
    }  // line 384: executes this statement as part of this file's behavior

    private fun String.toProjectResponse(): BotProject {  // line 386: executes this statement as part of this file's behavior
        val json = requireNotNull(asJsonOrNull()) { "Invalid project response." }  // line 387: executes this statement as part of this file's behavior
        return json.toBotProject()  // line 388: executes this statement as part of this file's behavior
    }  // line 389: executes this statement as part of this file's behavior

    private fun JSONObject.toBotProject(): BotProject {  // line 391: executes this statement as part of this file's behavior
        val discordJson = optJSONObject("discord") ?: JSONObject()  // line 392: executes this statement as part of this file's behavior
        val permissionsJson = optJSONObject("permissions") ?: JSONObject()  // line 393: executes this statement as part of this file's behavior
        val deploymentJson = optJSONObject("deployment") ?: JSONObject()  // line 394: executes this statement as part of this file's behavior
        return BotProject(  // line 395: executes this statement as part of this file's behavior
            id = optString("id"),  // line 396: executes this statement as part of this file's behavior
            name = optString("name"),  // line 397: executes this statement as part of this file's behavior
            slug = optString("slug"),  // line 398: executes this statement as part of this file's behavior
            description = optString("description"),  // line 399: executes this statement as part of this file's behavior
            templateId = optString("templateId", "template_blank_discord_ts"),  // line 400: executes this statement as part of this file's behavior
            language = optString("language", "typescript"),  // line 401: executes this statement as part of this file's behavior
            runtime = optString("runtime", "node22"),  // line 402: executes this statement as part of this file's behavior
            discord = DiscordProjectConfig(  // line 403: executes this statement as part of this file's behavior
                applicationId = discordJson.optionalString("applicationId"),  // line 404: executes this statement as part of this file's behavior
                clientId = discordJson.optionalString("clientId"),  // line 405: executes this statement as part of this file's behavior
                defaultGuildId = discordJson.optionalString("defaultGuildId"),  // line 406: executes this statement as part of this file's behavior
                tokenSecretRef = discordJson.optionalString("tokenSecretRef"),  // line 407: executes this statement as part of this file's behavior
                commandRegistration = discordJson.optionalString("commandRegistration") ?: "guild",  // line 408: executes this statement as part of this file's behavior
            ),  // line 409: executes this statement as part of this file's behavior
            permissions = ProjectPermissions(  // line 410: executes this statement as part of this file's behavior
                intents = permissionsJson.optStringArray("intents"),  // line 411: executes this statement as part of this file's behavior
                botPermissions = permissionsJson.optStringArray("botPermissions"),  // line 412: executes this statement as part of this file's behavior
            ),  // line 413: executes this statement as part of this file's behavior
            commands = (optJSONArray("commands") ?: JSONArray()).let { commands ->  // line 414: executes this statement as part of this file's behavior
                (0 until commands.length()).map { index -> commands.getJSONObject(index).toBotCommand() }  // line 415: executes this statement as part of this file's behavior
            },  // line 416: executes this statement as part of this file's behavior
            deployment = ProjectDeployment(  // line 417: executes this statement as part of this file's behavior
                targetId = deploymentJson.optionalString("targetId"),  // line 418: executes this statement as part of this file's behavior
                lastDeploymentId = deploymentJson.optionalString("lastDeploymentId"),  // line 419: executes this statement as part of this file's behavior
            ),  // line 420: executes this statement as part of this file's behavior
            github = optJSONObject("github")?.let { githubJson ->  // line 421: executes this statement as part of this file's behavior
                GitHubProjectConfig(  // line 422: executes this statement as part of this file's behavior
                    owner = githubJson.optionalString("owner"),  // line 423: executes this statement as part of this file's behavior
                    repo = githubJson.optionalString("repo"),  // line 424: executes this statement as part of this file's behavior
                    defaultBranch = githubJson.optionalString("defaultBranch") ?: "main",  // line 425: executes this statement as part of this file's behavior
                    lastPushedAt = githubJson.optionalString("lastPushedAt"),  // line 426: executes this statement as part of this file's behavior
                )  // line 427: executes this statement as part of this file's behavior
            },  // line 428: executes this statement as part of this file's behavior
            archivedAt = optionalString("archivedAt"),  // line 429: executes this statement as part of this file's behavior
            createdAt = optString("createdAt"),  // line 430: executes this statement as part of this file's behavior
            updatedAt = optString("updatedAt"),  // line 431: executes this statement as part of this file's behavior
        )  // line 432: executes this statement as part of this file's behavior
    }  // line 433: executes this statement as part of this file's behavior


    private fun JSONArray?.toFileSummaries(): List<ProjectFileSummary> {  // line 436: executes this statement as part of this file's behavior
        val array = this ?: JSONArray()  // line 437: executes this statement as part of this file's behavior
        return (0 until array.length()).map { index -> array.getJSONObject(index).toProjectFileSummary() }  // line 438: executes this statement as part of this file's behavior
    }  // line 439: executes this statement as part of this file's behavior

    private fun JSONObject.toProjectFileSummary(): ProjectFileSummary = ProjectFileSummary(  // line 441: executes this statement as part of this file's behavior
        path = optString("path"),  // line 442: executes this statement as part of this file's behavior
        size = optLong("size"),  // line 443: executes this statement as part of this file's behavior
        updatedAt = optString("updatedAt"),  // line 444: executes this statement as part of this file's behavior
        generated = optBoolean("generated"),  // line 445: executes this statement as part of this file's behavior
        editable = optBoolean("editable"),  // line 446: executes this statement as part of this file's behavior
    )  // line 447: executes this statement as part of this file's behavior

    private fun JSONObject.toProjectFileContent(): ProjectFileContent = ProjectFileContent(  // line 449: executes this statement as part of this file's behavior
        path = optString("path"),  // line 450: executes this statement as part of this file's behavior
        size = optLong("size"),  // line 451: executes this statement as part of this file's behavior
        updatedAt = optString("updatedAt"),  // line 452: executes this statement as part of this file's behavior
        generated = optBoolean("generated"),  // line 453: executes this statement as part of this file's behavior
        editable = optBoolean("editable"),  // line 454: executes this statement as part of this file's behavior
        content = optString("content"),  // line 455: executes this statement as part of this file's behavior
    )  // line 456: executes this statement as part of this file's behavior

    private fun String.toSecretResponse(): SecretSummary = requireNotNull(asJsonOrNull()) { "Invalid secret response." }.toSecretSummary()  // line 458: executes this statement as part of this file's behavior

    private fun JSONObject.toSecretSummary(): SecretSummary = SecretSummary(  // line 460: executes this statement as part of this file's behavior
        id = optString("id"),  // line 461: executes this statement as part of this file's behavior
        projectId = optionalString("projectId"),  // line 462: executes this statement as part of this file's behavior
        name = optString("name"),  // line 463: executes this statement as part of this file's behavior
        type = optString("type"),  // line 464: executes this statement as part of this file's behavior
        storageMode = optString("storageMode"),  // line 465: executes this statement as part of this file's behavior
        fingerprint = optString("fingerprint"),  // line 466: executes this statement as part of this file's behavior
        createdAt = optString("createdAt"),  // line 467: executes this statement as part of this file's behavior
        updatedAt = optString("updatedAt"),  // line 468: executes this statement as part of this file's behavior
        rotatedAt = optionalString("rotatedAt"),  // line 469: executes this statement as part of this file's behavior
    )  // line 470: executes this statement as part of this file's behavior

    private fun String.toBuildSummary(): BuildSummary = requireNotNull(asJsonOrNull()) { "Invalid build response." }.toBuildSummary()  // line 472: executes this statement as part of this file's behavior

    private fun JSONObject.toBuildSummary(): BuildSummary = BuildSummary(  // line 474: executes this statement as part of this file's behavior
        buildId = optString("buildId"),  // line 475: executes this statement as part of this file's behavior
        projectId = optString("projectId"),  // line 476: executes this statement as part of this file's behavior
        status = optString("status"),  // line 477: executes this statement as part of this file's behavior
        logUrl = optionalString("logUrl"),  // line 478: executes this statement as part of this file's behavior
        auditEventId = optionalString("auditEventId"),  // line 479: executes this statement as part of this file's behavior
        startedAt = optionalString("startedAt"),  // line 480: executes this statement as part of this file's behavior
        finishedAt = optionalString("finishedAt"),  // line 481: executes this statement as part of this file's behavior
        errorMessage = optionalString("errorMessage"),  // line 482: executes this statement as part of this file's behavior
    )  // line 483: executes this statement as part of this file's behavior


    private fun String.toRuntimeStatusResponse(): RuntimeStatusResponse {  // line 486: executes this statement as part of this file's behavior
        val json = requireNotNull(asJsonOrNull()) { "Invalid runtime status response." }  // line 487: executes this statement as part of this file's behavior
        return RuntimeStatusResponse(  // line 488: executes this statement as part of this file's behavior
            projectId = json.optString("projectId"),  // line 489: executes this statement as part of this file's behavior
            status = json.optionalString("status") ?: "unknown",  // line 490: executes this statement as part of this file's behavior
            running = json.optBoolean("running"),  // line 491: executes this statement as part of this file's behavior
            message = json.optionalString("message"),  // line 492: executes this statement as part of this file's behavior
        )  // line 493: executes this statement as part of this file's behavior
    }  // line 494: executes this statement as part of this file's behavior

    private fun String.toDeploymentTargetSummary(): DeploymentTargetSummary = requireNotNull(asJsonOrNull()) { "Invalid deployment target response." }.toDeploymentTargetSummary()  // line 496: executes this statement as part of this file's behavior

    private fun JSONObject.toDeploymentTargetSummary(): DeploymentTargetSummary {  // line 498: executes this statement as part of this file's behavior
        val capabilitiesJson = optJSONObject("capabilities") ?: JSONObject()  // line 499: executes this statement as part of this file's behavior
        val actionsJson = capabilitiesJson.optJSONObject("actions") ?: JSONObject()  // line 500: executes this statement as part of this file's behavior
        val actions = actionsJson.keys().asSequence().associateWith { actionsJson.optBoolean(it) }  // line 501: executes this statement as part of this file's behavior
        val notesArray = capabilitiesJson.optJSONArray("notes") ?: JSONArray()  // line 502: executes this statement as part of this file's behavior
        val notes = (0 until notesArray.length()).map { index -> notesArray.optString(index) }  // line 503: executes this statement as part of this file's behavior
        return DeploymentTargetSummary(  // line 504: executes this statement as part of this file's behavior
            id = optString("id"),  // line 505: executes this statement as part of this file's behavior
            name = optString("name"),  // line 506: executes this statement as part of this file's behavior
            type = optString("type"),  // line 507: executes this statement as part of this file's behavior
            capabilities = DeploymentAdapterCapabilities(  // line 508: executes this statement as part of this file's behavior
                supported = capabilitiesJson.optBoolean("supported"),  // line 509: executes this statement as part of this file's behavior
                actions = actions,  // line 510: executes this statement as part of this file's behavior
                notes = notes,  // line 511: executes this statement as part of this file's behavior
            ),  // line 512: executes this statement as part of this file's behavior
            createdAt = optionalString("createdAt"),  // line 513: executes this statement as part of this file's behavior
            updatedAt = optionalString("updatedAt"),  // line 514: executes this statement as part of this file's behavior
        )  // line 515: executes this statement as part of this file's behavior
    }  // line 516: executes this statement as part of this file's behavior

    private fun String.toDeploymentTargetTestResponse(): DeploymentTargetTestResponse {  // line 518: executes this statement as part of this file's behavior
        val json = requireNotNull(asJsonOrNull()) { "Invalid deployment target test response." }  // line 519: executes this statement as part of this file's behavior
        return DeploymentTargetTestResponse(json.optBoolean("ok"), json.optString("status"), json.optString("message"))  // line 520: executes this statement as part of this file's behavior
    }  // line 521: executes this statement as part of this file's behavior

    private fun String.toDeploymentJobSummary(): DeploymentJobSummary = requireNotNull(asJsonOrNull()) { "Invalid deployment response." }.toDeploymentJobSummary()  // line 523: executes this statement as part of this file's behavior

    private fun String.toDeploymentActionResponse(): DeploymentActionResponse {  // line 525: executes this statement as part of this file's behavior
        val json = requireNotNull(asJsonOrNull()) { "Invalid deployment action response." }  // line 526: executes this statement as part of this file's behavior
        return DeploymentActionResponse(  // line 527: executes this statement as part of this file's behavior
            status = json.optionalString("status"),  // line 528: executes this statement as part of this file's behavior
            running = json.optBoolean("running"),  // line 529: executes this statement as part of this file's behavior
            message = json.optionalString("message"),  // line 530: executes this statement as part of this file's behavior
        )  // line 531: executes this statement as part of this file's behavior
    }  // line 532: executes this statement as part of this file's behavior

    private fun JSONObject.toDeploymentJobSummary(): DeploymentJobSummary = DeploymentJobSummary(  // line 534: executes this statement as part of this file's behavior
        deploymentId = optString("deploymentId"),  // line 535: executes this statement as part of this file's behavior
        projectId = optString("projectId"),  // line 536: executes this statement as part of this file's behavior
        targetId = optString("targetId"),  // line 537: executes this statement as part of this file's behavior
        buildId = optString("buildId"),  // line 538: executes this statement as part of this file's behavior
        status = optString("status"),  // line 539: executes this statement as part of this file's behavior
        createdAt = optionalString("createdAt"),  // line 540: executes this statement as part of this file's behavior
        finishedAt = optionalString("finishedAt"),  // line 541: executes this statement as part of this file's behavior
        errorMessage = optionalString("errorMessage"),  // line 542: executes this statement as part of this file's behavior
    )  // line 543: executes this statement as part of this file's behavior


    private fun CommandCreateRequest.toCommandPayload(): String = JSONObject()  // line 546: executes this statement as part of this file's behavior
        .put("name", name)  // line 547: executes this statement as part of this file's behavior
        .put("description", description)  // line 548: executes this statement as part of this file's behavior
        .put("type", "chat_input")  // line 549: executes this statement as part of this file's behavior
        .put("handler", JSONObject().put("kind", handlerKind).put("content", handlerContent).put("ephemeral", ephemeral))  // line 550: executes this statement as part of this file's behavior
        .toString()  // line 551: executes this statement as part of this file's behavior

    private fun String.toCommandResponse(): BotCommand = requireNotNull(asJsonOrNull()) { "Invalid command response." }.toBotCommand()  // line 553: executes this statement as part of this file's behavior

    private fun JSONObject.toBotCommand(): BotCommand {  // line 555: executes this statement as part of this file's behavior
        val permissionsJson = optJSONObject("permissions") ?: JSONObject()  // line 556: executes this statement as part of this file's behavior
        val handlerValue = opt("handler")  // line 557: executes this statement as part of this file's behavior
        val handlerJson = if (handlerValue is JSONObject) handlerValue else JSONObject().put("kind", "custom_typescript_placeholder")  // line 558: executes this statement as part of this file's behavior
        return BotCommand(  // line 559: executes this statement as part of this file's behavior
            id = optionalString("id"),  // line 560: executes this statement as part of this file's behavior
            name = optString("name"),  // line 561: executes this statement as part of this file's behavior
            description = optString("description"),  // line 562: executes this statement as part of this file's behavior
            type = optionalString("type") ?: "chat_input",  // line 563: executes this statement as part of this file's behavior
            permissions = BotCommandPermissions(  // line 564: executes this statement as part of this file's behavior
                defaultMemberPermissions = permissionsJson.optionalString("defaultMemberPermissions"),  // line 565: executes this statement as part of this file's behavior
                dmPermission = permissionsJson.optBoolean("dmPermission"),  // line 566: executes this statement as part of this file's behavior
            ),  // line 567: executes this statement as part of this file's behavior
            handler = BotCommandHandler(  // line 568: executes this statement as part of this file's behavior
                kind = handlerJson.optionalString("kind") ?: "static_response",  // line 569: executes this statement as part of this file's behavior
                ephemeral = handlerJson.optBoolean("ephemeral", true),  // line 570: executes this statement as part of this file's behavior
                content = handlerJson.optionalString("content"),  // line 571: executes this statement as part of this file's behavior
            ),  // line 572: executes this statement as part of this file's behavior
        )  // line 573: executes this statement as part of this file's behavior
    }  // line 574: executes this statement as part of this file's behavior

    private fun String.toGitHubStatusResponse(): GitHubStatusResponse {  // line 576: executes this statement as part of this file's behavior
        val json = requireNotNull(asJsonOrNull()) { "Invalid GitHub status response." }  // line 577: executes this statement as part of this file's behavior
        return GitHubStatusResponse(json.optBoolean("connected"), json.optionalString("tokenSecretRef"), json.optionalString("message"))  // line 578: executes this statement as part of this file's behavior
    }  // line 579: executes this statement as part of this file's behavior

    private fun String.toProjectScanResponse(): ProjectScanResponse {  // line 581: executes this statement as part of this file's behavior
        val json = requireNotNull(asJsonOrNull()) { "Invalid scan response." }  // line 582: executes this statement as part of this file's behavior
        val detection = json.optJSONObject("detection") ?: JSONObject()  // line 583: executes this statement as part of this file's behavior
        val matchesJson = detection.optJSONArray("matches") ?: JSONArray()  // line 584: executes this statement as part of this file's behavior
        val matches = (0 until matchesJson.length()).map { index ->  // line 585: executes this statement as part of this file's behavior
            val match = matchesJson.getJSONObject(index)  // line 586: executes this statement as part of this file's behavior
            val requiredSecrets = match.optJSONArray("requiredSecrets") ?: JSONArray()  // line 587: executes this statement as part of this file's behavior
            ScanDetectionMatch(  // line 588: executes this statement as part of this file's behavior
                id = match.optString("id"),  // line 589: executes this statement as part of this file's behavior
                name = match.optString("name"),  // line 590: executes this statement as part of this file's behavior
                score = match.optInt("score", 0),  // line 591: executes this statement as part of this file's behavior
                confidence = match.optString("confidence", "weak"),  // line 592: executes this statement as part of this file's behavior
                requiredSecrets = (0 until requiredSecrets.length()).mapNotNull { key -> requiredSecrets.optString(key).takeIf { it.isNotBlank() } },  // line 593: executes this statement as part of this file's behavior
            )  // line 594: executes this statement as part of this file's behavior
        }  // line 595: executes this statement as part of this file's behavior
        val warningsJson = detection.optJSONArray("warnings") ?: JSONArray()  // line 596: executes this statement as part of this file's behavior
        val warnings = (0 until warningsJson.length()).mapNotNull { index -> warningsJson.optString(index).takeIf { it.isNotBlank() } }  // line 597: executes this statement as part of this file's behavior
        return ProjectScanResponse(detection.optString("recommendedPackId", "unknown"), matches, warnings)  // line 598: executes this statement as part of this file's behavior
    }  // line 599: executes this statement as part of this file's behavior

    private fun String.asJsonOrNull(): JSONObject? = runCatching { JSONObject(this) }.getOrNull()  // line 601: executes this statement as part of this file's behavior

    private fun JSONObject.optionalString(name: String): String? = if (has(name) && !isNull(name)) {  // line 603: executes this statement as part of this file's behavior
        optString(name).takeIf { it.isNotBlank() }  // line 604: executes this statement as part of this file's behavior
    } else {  // line 605: executes this statement as part of this file's behavior
        null  // line 606: executes this statement as part of this file's behavior
    }  // line 607: executes this statement as part of this file's behavior

    private fun JSONObject.optStringArray(name: String): List<String> {  // line 609: executes this statement as part of this file's behavior
        val array = optJSONArray(name) ?: return emptyList()  // line 610: executes this statement as part of this file's behavior
        return (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }  // line 611: executes this statement as part of this file's behavior
    }  // line 612: executes this statement as part of this file's behavior

    private fun String.encodedPathSegment(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())  // line 614: executes this statement as part of this file's behavior
        .replace("+", "%20")  // line 615: executes this statement as part of this file's behavior

    private fun String.encodedFilePath(): String = split("/").joinToString("/") { it.encodedPathSegment() }  // line 617: executes this statement as part of this file's behavior

    private fun String.cookieValue(): String = replace(";", "").replace("\r", "").replace("\n", "")  // line 619: executes this statement as part of this file's behavior

    private fun String.normalizePlainStatus(): String = replace(Regex("[{}\\\"]"), "")  // line 621: executes this statement as part of this file's behavior
        .replace(',', ' ')  // line 622: executes this statement as part of this file's behavior
        .trim()  // line 623: executes this statement as part of this file's behavior
        .ifBlank { "Unknown" }  // line 624: executes this statement as part of this file's behavior

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T = try {  // line 626: executes this statement as part of this file's behavior
        block(this)  // line 627: executes this statement as part of this file's behavior
    } finally {  // line 628: executes this statement as part of this file's behavior
        disconnect()  // line 629: executes this statement as part of this file's behavior
    }  // line 630: executes this statement as part of this file's behavior
}  // line 631: executes this statement as part of this file's behavior
