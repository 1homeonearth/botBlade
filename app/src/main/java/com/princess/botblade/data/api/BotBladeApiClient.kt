package com.princess.botblade.data.api

import com.princess.botblade.data.model.ApiErrorResponse
import com.princess.botblade.data.model.BotProject
import com.princess.botblade.data.model.BotStatusResponse
import com.princess.botblade.data.model.BotToggleRequest
import com.princess.botblade.data.model.BotToggleResponse
import com.princess.botblade.data.model.DiscordProjectConfig
import com.princess.botblade.data.model.HealthResponse
import com.princess.botblade.data.model.ProjectCreateRequest
import com.princess.botblade.data.model.ProjectDeployment
import com.princess.botblade.data.model.ProjectPermissions
import com.princess.botblade.data.model.ProjectUpdateRequest
import com.princess.botblade.data.model.SecretSummary
import com.princess.botblade.data.model.SecretCreateRequest
import com.princess.botblade.data.model.ProjectFileSummary
import com.princess.botblade.data.model.ProjectFileContent
import com.princess.botblade.data.model.BuildSummary
import com.princess.botblade.data.model.BuildRequest
import com.princess.botblade.data.model.RuntimeStatusResponse
import com.princess.botblade.data.model.ProjectScanResponse
import com.princess.botblade.data.model.ScanDetectionMatch
import com.princess.botblade.data.model.DeploymentTargetSummary
import com.princess.botblade.data.model.DeploymentTargetCreateRequest
import com.princess.botblade.data.model.DeploymentTargetTestResponse
import com.princess.botblade.data.model.DeploymentJobSummary
import com.princess.botblade.data.model.DeploymentCreateRequest
import com.princess.botblade.data.model.DeploymentActionResponse
import com.princess.botblade.data.model.DeploymentAdapterCapabilities
import com.princess.botblade.data.model.GitHubStatusResponse
import com.princess.botblade.data.model.BotCommand
import com.princess.botblade.data.model.BotCommandHandler
import com.princess.botblade.data.model.BotCommandPermissions
import com.princess.botblade.data.model.CommandCreateRequest
import com.princess.botblade.data.model.GitHubConnectRequest
import com.princess.botblade.data.model.GitHubLinkRepoRequest
import com.princess.botblade.data.model.GitHubProjectConfig
import com.princess.botblade.data.model.GitHubWorkflowResponse
import com.princess.botblade.data.model.ImportStartRequest
import com.princess.botblade.data.model.ImportSummary
import com.princess.botblade.data.model.GitStatusApiSummary
import com.princess.botblade.data.model.GitRemoteSummary
import com.princess.botblade.data.model.GitChangedFileSummary
import com.princess.botblade.data.model.ProjectProfileBladePack
import com.princess.botblade.data.model.ProjectProfileCommandPlan
import com.princess.botblade.data.model.ProjectProfileGit
import com.princess.botblade.data.model.ProjectProfileGitRemote
import com.princess.botblade.data.model.ProjectProfileImportSource
import com.princess.botblade.data.model.ProjectProfilePackEvidence
import com.princess.botblade.data.model.ProjectProfileProject
import com.princess.botblade.data.model.ProjectProfileResponse
import com.princess.botblade.data.model.ProjectProfileRuntime
import com.princess.botblade.data.model.ProjectProfileSecrets
import com.princess.botblade.data.model.RepairCard
import com.princess.botblade.data.model.ScriptEnvironmentRef
import com.princess.botblade.data.model.ScriptProfileSummary
import com.princess.botblade.data.model.ScriptSecretRef
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

class BotBladeApiClient(
    private val baseUrl: String? = null,
    private val bearerToken: String = ApiConfig.DEFAULT_BEARER_TOKEN,
    private val sessionToken: String = ApiConfig.DEFAULT_SESSION_TOKEN,
) {
    @Throws(IOException::class)
    fun getHealth(): HealthResponse {
        val body = request(path = "/api/health", method = "GET")
        val json = body.asJsonOrNull()
        return HealthResponse(
            status = json?.optionalString("status") ?: json?.optionalString("service") ?: body.ifBlank { "unknown" },
            message = json?.optionalString("message"),
        )
    }

    @Throws(IOException::class)
    fun getBotStatus(): BotStatusResponse {
        val body = request(path = "/api/bot-status/", method = "GET")
        val json = body.asJsonOrNull()
        return BotStatusResponse(
            status = json?.optionalString("status")
                ?: json?.optionalString("bot_status")
                ?: json?.optionalString("state")
                ?: body.normalizePlainStatus(),
            message = json?.optionalString("message"),
        )
    }

    @Throws(IOException::class)
    fun toggleBot(request: BotToggleRequest): BotToggleResponse {
        val payload = JSONObject().put("action", request.action).toString()
        val body = request(path = "/api/bot-toggle/", method = "POST", requestBody = payload)
        val json = body.asJsonOrNull()
        return BotToggleResponse(
            status = json?.optionalString("status")
                ?: json?.optionalString("bot_status")
                ?: body.normalizePlainStatus().takeIf { json == null },
            action = json?.optionalString("action") ?: request.action,
            message = json?.optionalString("message") ?: body.takeIf { it.isNotBlank() && json == null },
        )
    }


    @Throws(IOException::class)
    fun getProjectGitStatus(projectId: String): GitStatusApiSummary {
        val json = requireNotNull(request(path = "/api/projects/${projectId.encodedPathSegment()}/git/status", method = "GET").asJsonOrNull()) { "Invalid git status response." }
        val remotesJson = json.optJSONArray("remotes") ?: JSONArray()
        val remotes = (0 until remotesJson.length()).map { index ->
            val remote = remotesJson.getJSONObject(index)
            GitRemoteSummary(name = remote.optString("name"), url = remote.optionalString("url"))
        }
        val changedJson = json.optJSONArray("changedFiles") ?: JSONArray()
        val changed = (0 until changedJson.length()).map { index ->
            val file = changedJson.getJSONObject(index)
            GitChangedFileSummary(path = file.optString("path"), status = file.optString("status"))
        }
        return GitStatusApiSummary(
            available = json.optBoolean("available", false),
            branch = json.optionalString("branch"),
            remotes = remotes,
            clean = json.optBoolean("clean", true),
            dirtyFileCount = json.optInt("dirtyFileCount", changed.size),
            changedFiles = changed,
            note = json.optionalString("note"),
        )
    }

    @Throws(IOException::class)
    fun listProjects(): List<BotProject> {
        val body = request(path = "/api/projects", method = "GET")
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid projects response." }
        val projects = json.optJSONArray("projects") ?: JSONArray()
        return (0 until projects.length()).map { index -> projects.getJSONObject(index).toBotProject() }
    }

    @Throws(IOException::class)
    fun createProject(request: ProjectCreateRequest): BotProject {
        val payload = JSONObject()
            .put("name", request.name)
            .put("description", request.description)
            .put("templateId", request.templateId)
            .put("runtime", request.runtime)
            .toString()
        return request(path = "/api/projects", method = "POST", requestBody = payload).toProjectResponse()
    }

    @Throws(IOException::class)
    fun getProject(projectId: String): BotProject =
        request(path = "/api/projects/${projectId.encodedPathSegment()}", method = "GET").toProjectResponse()

    @Throws(IOException::class)
    fun getProjectProfile(projectId: String): ProjectProfileResponse =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/profile", method = "GET").toProjectProfileResponse()

    @Throws(IOException::class)
    fun listScriptProfiles(projectId: String): List<ScriptProfileSummary> {
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/script-profiles", method = "GET")
        val profiles = requireNotNull(body.asJsonOrNull()) { "Invalid script profiles response." }.optJSONArray("scriptProfiles") ?: JSONArray()
        return profiles.toScriptProfileSummaries()
    }

    @Throws(IOException::class)
    fun getScriptProfile(projectId: String, scriptProfileId: String): ScriptProfileSummary =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/script-profiles/${scriptProfileId.encodedPathSegment()}", method = "GET").toScriptProfileResponse()

    @Throws(IOException::class)
    fun updateProject(projectId: String, request: ProjectUpdateRequest): BotProject {
        val payload = JSONObject().apply {
            request.name?.let { put("name", it) }
            request.description?.let { put("description", it) }
            request.templateId?.let { put("templateId", it) }
        }.toString()
        return request(path = "/api/projects/${projectId.encodedPathSegment()}", method = "PATCH", requestBody = payload).toProjectResponse()
    }

    @Throws(IOException::class)
    fun archiveProject(projectId: String): BotProject =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/archive", method = "POST", requestBody = "{}").toProjectResponse()

    @Throws(IOException::class)
    fun cloneProject(projectId: String): BotProject =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/clone", method = "POST", requestBody = "{}").toProjectResponse()

    @Throws(IOException::class)
    fun listCommands(projectId: String): List<BotCommand> {
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/commands", method = "GET")
        val commands = requireNotNull(body.asJsonOrNull()) { "Invalid commands response." }.optJSONArray("commands") ?: JSONArray()
        return (0 until commands.length()).map { index -> commands.getJSONObject(index).toBotCommand() }
    }

    @Throws(IOException::class)
    fun createCommand(projectId: String, command: CommandCreateRequest): BotCommand =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/commands", method = "POST", requestBody = command.toCommandPayload()).toCommandResponse()

    @Throws(IOException::class)
    fun updateCommand(projectId: String, commandId: String, command: CommandCreateRequest): BotCommand =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/commands/${commandId.encodedPathSegment()}", method = "PATCH", requestBody = command.toCommandPayload()).toCommandResponse()

    @Throws(IOException::class)
    fun deleteCommand(projectId: String, commandId: String) {
        request(path = "/api/projects/${projectId.encodedPathSegment()}/commands/${commandId.encodedPathSegment()}", method = "DELETE")
    }

    @Throws(IOException::class)
    fun connectGitHub(request: GitHubConnectRequest): GitHubStatusResponse {
        val payload = JSONObject().put("tokenSecretRef", request.tokenSecretRef).toString()
        return request(path = "/api/github/connect", method = "POST", requestBody = payload).toGitHubStatusResponse()
    }

    @Throws(IOException::class)
    fun linkGitHubRepo(projectId: String, request: GitHubLinkRepoRequest): BotProject {
        val payload = JSONObject()
            .put("owner", request.owner)
            .put("repo", request.repo)
            .put("defaultBranch", request.defaultBranch)
            .toString()
        return request(path = "/api/projects/${projectId.encodedPathSegment()}/github/create-repo", method = "POST", requestBody = payload).toProjectResponse()
    }

    @Throws(IOException::class)
    fun pushGitHub(projectId: String): String =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/github/push", method = "POST", requestBody = "{}")

    @Throws(IOException::class)
    fun createGitHubWorkflow(projectId: String): GitHubWorkflowResponse {
        val json = requireNotNull(request(path = "/api/projects/${projectId.encodedPathSegment()}/github/create-workflow", method = "POST", requestBody = "{}").asJsonOrNull()) { "Invalid workflow response." }
        return GitHubWorkflowResponse(path = json.optString("path"), content = json.optString("content"))
    }


    @Throws(IOException::class)
    fun generateProject(projectId: String): List<ProjectFileSummary> {
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/generate", method = "POST", requestBody = "{}")
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid generate response." }
        return json.optJSONArray("files").toFileSummaries()
    }

    @Throws(IOException::class)
    fun listProjectFiles(projectId: String): List<ProjectFileSummary> {
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/files", method = "GET")
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid files response." }
        return json.optJSONArray("files").toFileSummaries()
    }

    @Throws(IOException::class)
    fun getProjectFile(projectId: String, filePath: String): ProjectFileContent {
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/files/${filePath.encodedFilePath()}", method = "GET")
        return requireNotNull(body.asJsonOrNull()) { "Invalid file response." }.toProjectFileContent()
    }

    @Throws(IOException::class)
    fun saveProjectFile(projectId: String, filePath: String, content: String): ProjectFileContent {
        val payload = JSONObject().put("content", content).toString()
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/files/${filePath.encodedFilePath()}", method = "PUT", requestBody = payload)
        return requireNotNull(body.asJsonOrNull()) { "Invalid file response." }.toProjectFileContent()
    }

    @Throws(IOException::class)
    fun listSecrets(): List<SecretSummary> {
        val body = request(path = "/api/secrets", method = "GET")
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid secrets response." }
        val secrets = json.optJSONArray("secrets") ?: JSONArray()
        return (0 until secrets.length()).map { index -> secrets.getJSONObject(index).toSecretSummary() }
    }

    @Throws(IOException::class)
    fun createSecret(request: SecretCreateRequest): SecretSummary {
        val payload = JSONObject()
            .put("name", request.name)
            .put("type", request.type)
            .put("value", request.value)
            .apply { request.projectId?.let { put("projectId", it) } }
            .toString()
        return this.request(path = "/api/secrets", method = "POST", requestBody = payload).toSecretResponse()
    }

    @Throws(IOException::class)
    fun rotateSecret(secretId: String, value: String): SecretSummary {
        val payload = JSONObject().put("value", value).toString()
        return request(path = "/api/secrets/${secretId.encodedPathSegment()}/rotate", method = "POST", requestBody = payload).toSecretResponse()
    }

    @Throws(IOException::class)
    fun deleteSecret(secretId: String) {
        request(path = "/api/secrets/${secretId.encodedPathSegment()}", method = "DELETE")
    }

    @Throws(IOException::class)
    fun createBuild(projectId: String, buildRequest: BuildRequest = BuildRequest()): BuildSummary {
        val payload = JSONObject()
            .put("source", buildRequest.source)
            .put("clean", buildRequest.clean)
            .put("runTests", buildRequest.runTests)
            .put("createDockerImage", buildRequest.createDockerImage)
            .toString()
        return request(path = "/api/projects/${projectId.encodedPathSegment()}/builds", method = "POST", requestBody = payload).toBuildSummary()
    }

    @Throws(IOException::class)
    fun listBuilds(projectId: String): List<BuildSummary> {
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/builds", method = "GET")
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid builds response." }
        val builds = json.optJSONArray("builds") ?: JSONArray()
        return (0 until builds.length()).map { index -> builds.getJSONObject(index).toBuildSummary() }
    }

    @Throws(IOException::class)
    fun getBuild(projectId: String, buildId: String): BuildSummary =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/builds/${buildId.encodedPathSegment()}", method = "GET").toBuildSummary()

    @Throws(IOException::class)
    fun getBuildLogs(projectId: String, buildId: String): String {
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/builds/${buildId.encodedPathSegment()}/logs", method = "GET")
        return body.asJsonOrNull()?.optionalString("logs") ?: body
    }


    @Throws(IOException::class)
    fun getProjectRuntimeStatus(projectId: String): RuntimeStatusResponse =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/runtime/status", method = "GET").toRuntimeStatusResponse()

    @Throws(IOException::class)
    fun runtimeAction(projectId: String, action: String): RuntimeStatusResponse =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/runtime/${action.encodedPathSegment()}", method = "POST", requestBody = "{}").toRuntimeStatusResponse()

    @Throws(IOException::class)
    fun getProjectRuntimeLogs(projectId: String): String {
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/runtime/logs", method = "GET")
        return body.asJsonOrNull()?.optionalString("logs") ?: body
    }

    @Throws(IOException::class)
    fun scanProject(projectId: String): ProjectScanResponse =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/scan", method = "POST", requestBody = "{}").toProjectScanResponse()

    @Throws(IOException::class)
    fun startImport(request: ImportStartRequest): ImportSummary = when (request.sourceType.lowercase()) {
        "git" -> {
            val payload = JSONObject()
                .put("repoUrl", request.source)
                .put("workspacePath", request.workspacePath)
                .toString()
            this.request(path = "/api/imports/git", method = "POST", requestBody = payload).toImportSummary()
        }
        "zip" -> {
            val payload = JSONObject()
                .put("archivePath", request.source)
                .put("workspacePath", request.workspacePath)
                .toString()
            this.request(path = "/api/imports/zip", method = "POST", requestBody = payload).toImportSummary()
        }
        "folder" -> {
            val payload = JSONObject()
                .put("folderPath", request.source)
                .put("workspacePath", request.workspacePath)
                .toString()
            this.request(path = "/api/imports/folder", method = "POST", requestBody = payload).toImportSummary()
        }
        else -> throw IOException("Unsupported import source type: ${request.sourceType}")
    }

    @Throws(IOException::class)
    fun getImport(importId: String): ImportSummary =
        request(path = "/api/imports/${importId.encodedPathSegment()}", method = "GET").toImportSummary()

    fun listDeploymentTargets(): List<DeploymentTargetSummary> {
        val body = request(path = "/api/deployment-targets", method = "GET")
        val targets = requireNotNull(body.asJsonOrNull()) { "Invalid deployment targets response." }.optJSONArray("targets") ?: JSONArray()
        return (0 until targets.length()).map { index -> targets.getJSONObject(index).toDeploymentTargetSummary() }
    }

    @Throws(IOException::class)
    fun createDeploymentTarget(request: DeploymentTargetCreateRequest): DeploymentTargetSummary {
        val payload = JSONObject().put("name", request.name).put("type", request.type).put("config", JSONObject()).put("secretRefs", JSONArray()).toString()
        return this.request(path = "/api/deployment-targets", method = "POST", requestBody = payload).toDeploymentTargetSummary()
    }

    @Throws(IOException::class)
    fun testDeploymentTarget(targetId: String): DeploymentTargetTestResponse =
        request(path = "/api/deployment-targets/${targetId.encodedPathSegment()}/test", method = "POST", requestBody = "{}").toDeploymentTargetTestResponse()

    @Throws(IOException::class)
    fun createDeployment(projectId: String, request: DeploymentCreateRequest): DeploymentJobSummary {
        val payload = JSONObject().put("targetId", request.targetId).put("buildId", request.buildId).toString()
        return this.request(path = "/api/projects/${projectId.encodedPathSegment()}/deployments", method = "POST", requestBody = payload).toDeploymentJobSummary()
    }

    @Throws(IOException::class)
    fun listDeployments(projectId: String): List<DeploymentJobSummary> {
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/deployments", method = "GET")
        val deployments = requireNotNull(body.asJsonOrNull()) { "Invalid deployments response." }.optJSONArray("deployments") ?: JSONArray()
        return (0 until deployments.length()).map { index -> deployments.getJSONObject(index).toDeploymentJobSummary() }
    }

    @Throws(IOException::class)
    fun getDeploymentLogs(projectId: String, deploymentId: String): String {
        val body = request(path = "/api/projects/${projectId.encodedPathSegment()}/deployments/${deploymentId.encodedPathSegment()}/logs", method = "GET")
        return body.asJsonOrNull()?.optionalString("logs") ?: body
    }

    @Throws(IOException::class)
    fun getDeploymentStatus(projectId: String, deploymentId: String): DeploymentActionResponse =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/deployments/${deploymentId.encodedPathSegment()}/status", method = "GET").toDeploymentActionResponse()

    @Throws(IOException::class)
    fun deploymentAction(projectId: String, deploymentId: String, action: String): DeploymentActionResponse =
        request(path = "/api/projects/${projectId.encodedPathSegment()}/deployments/${deploymentId.encodedPathSegment()}/${action.encodedPathSegment()}", method = "POST", requestBody = "{}").toDeploymentActionResponse()

    @Throws(IOException::class)
    fun getGitHubStatus(): GitHubStatusResponse =
        request(path = "/api/github/status", method = "GET").toGitHubStatusResponse()

    @Throws(IOException::class)
    private fun request(
        path: String,
        method: String,
        requestBody: String? = null,
    ): String {
        val requestBaseUrl = baseUrl?.let(ApiConfig::normalizeUrl) ?: ApiConfig.baseUrl
        val connection = URL(requestBaseUrl + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 2_000
        connection.readTimeout = 2_000
        connection.doInput = true
        connection.setRequestProperty("Accept", "application/json")
        when {
            bearerToken.isNotBlank() -> connection.setRequestProperty("Authorization", "Bearer $bearerToken")
            sessionToken.isNotBlank() -> connection.setRequestProperty("Cookie", "botBladeSession=${sessionToken.cookieValue()}")
        }

        if (requestBody != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { output -> output.write(requestBody.toByteArray()) }
        }

        return connection.use { http ->
            val responseCode = http.responseCode
            val responseBody = (if (responseCode in 200..299) http.inputStream else http.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (responseCode !in 200..299) {
                val error = parseError(responseBody, responseCode)
                throw IOException(error.message ?: error.error ?: "HTTP $responseCode")
            }

            responseBody
        }
    }

    private fun parseError(body: String, statusCode: Int): ApiErrorResponse {
        val json = body.asJsonOrNull()
        val nestedError = json?.optJSONObject("error")
        return ApiErrorResponse(
            error = nestedError?.optionalString("code") ?: json?.optionalString("error"),
            message = nestedError?.optionalString("message") ?: json?.optionalString("message"),
            statusCode = statusCode,
        )
    }

    private fun String.toProjectResponse(): BotProject {
        val json = requireNotNull(asJsonOrNull()) { "Invalid project response." }
        return json.toBotProject()
    }

    private fun JSONObject.toBotProject(): BotProject {
        val discordJson = optJSONObject("discord") ?: JSONObject()
        val permissionsJson = optJSONObject("permissions") ?: JSONObject()
        val deploymentJson = optJSONObject("deployment") ?: JSONObject()
        return BotProject(
            id = optString("id"),
            name = optString("name"),
            slug = optString("slug"),
            description = optString("description"),
            templateId = optString("templateId", "template_blank_discord_ts"),
            language = optString("language", "typescript"),
            runtime = optString("runtime", "node22"),
            discord = DiscordProjectConfig(
                applicationId = discordJson.optionalString("applicationId"),
                clientId = discordJson.optionalString("clientId"),
                defaultGuildId = discordJson.optionalString("defaultGuildId"),
                tokenSecretRef = discordJson.optionalString("tokenSecretRef"),
                commandRegistration = discordJson.optionalString("commandRegistration") ?: "guild",
            ),
            permissions = ProjectPermissions(
                intents = permissionsJson.optStringArray("intents"),
                botPermissions = permissionsJson.optStringArray("botPermissions"),
            ),
            commands = (optJSONArray("commands") ?: JSONArray()).let { commands ->
                (0 until commands.length()).map { index -> commands.getJSONObject(index).toBotCommand() }
            },
            deployment = ProjectDeployment(
                targetId = deploymentJson.optionalString("targetId"),
                lastDeploymentId = deploymentJson.optionalString("lastDeploymentId"),
            ),
            github = optJSONObject("github")?.let { githubJson ->
                GitHubProjectConfig(
                    owner = githubJson.optionalString("owner"),
                    repo = githubJson.optionalString("repo"),
                    defaultBranch = githubJson.optionalString("defaultBranch") ?: "main",
                    lastPushedAt = githubJson.optionalString("lastPushedAt"),
                )
            },
            archivedAt = optionalString("archivedAt"),
            createdAt = optString("createdAt"),
            updatedAt = optString("updatedAt"),
        )
    }


    private fun JSONArray?.toFileSummaries(): List<ProjectFileSummary> {
        val array = this ?: JSONArray()
        return (0 until array.length()).map { index -> array.getJSONObject(index).toProjectFileSummary() }
    }

    private fun JSONObject.toProjectFileSummary(): ProjectFileSummary = ProjectFileSummary(
        path = optString("path"),
        size = optLong("size"),
        updatedAt = optString("updatedAt"),
        generated = optBoolean("generated"),
        editable = optBoolean("editable"),
    )

    private fun JSONObject.toProjectFileContent(): ProjectFileContent = ProjectFileContent(
        path = optString("path"),
        size = optLong("size"),
        updatedAt = optString("updatedAt"),
        generated = optBoolean("generated"),
        editable = optBoolean("editable"),
        content = optString("content"),
    )

    private fun String.toSecretResponse(): SecretSummary = requireNotNull(asJsonOrNull()) { "Invalid secret response." }.toSecretSummary()

    private fun JSONObject.toSecretSummary(): SecretSummary = SecretSummary(
        id = optString("id"),
        projectId = optionalString("projectId"),
        name = optString("name"),
        type = optString("type"),
        storageMode = optString("storageMode"),
        fingerprint = optString("fingerprint"),
        createdAt = optString("createdAt"),
        updatedAt = optString("updatedAt"),
        rotatedAt = optionalString("rotatedAt"),
    )

    private fun String.toBuildSummary(): BuildSummary = requireNotNull(asJsonOrNull()) { "Invalid build response." }.toBuildSummary()

    private fun JSONObject.toBuildSummary(): BuildSummary = BuildSummary(
        buildId = optString("buildId"),
        projectId = optString("projectId"),
        status = optString("status"),
        logUrl = optionalString("logUrl"),
        auditEventId = optionalString("auditEventId"),
        startedAt = optionalString("startedAt"),
        finishedAt = optionalString("finishedAt"),
        errorMessage = optionalString("errorMessage"),
    )


    private fun String.toRuntimeStatusResponse(): RuntimeStatusResponse {
        val json = requireNotNull(asJsonOrNull()) { "Invalid runtime status response." }
        return RuntimeStatusResponse(
            projectId = json.optString("projectId"),
            status = json.optionalString("status") ?: "unknown",
            running = json.optBoolean("running"),
            message = json.optionalString("message"),
        )
    }

    private fun String.toDeploymentTargetSummary(): DeploymentTargetSummary = requireNotNull(asJsonOrNull()) { "Invalid deployment target response." }.toDeploymentTargetSummary()

    private fun JSONObject.toDeploymentTargetSummary(): DeploymentTargetSummary {
        val capabilitiesJson = optJSONObject("capabilities") ?: JSONObject()
        val actionsJson = capabilitiesJson.optJSONObject("actions") ?: JSONObject()
        val actions = actionsJson.keys().asSequence().associateWith { actionsJson.optBoolean(it) }
        val notesArray = capabilitiesJson.optJSONArray("notes") ?: JSONArray()
        val notes = (0 until notesArray.length()).map { index -> notesArray.optString(index) }
        return DeploymentTargetSummary(
            id = optString("id"),
            name = optString("name"),
            type = optString("type"),
            capabilities = DeploymentAdapterCapabilities(
                supported = capabilitiesJson.optBoolean("supported"),
                actions = actions,
                notes = notes,
            ),
            createdAt = optionalString("createdAt"),
            updatedAt = optionalString("updatedAt"),
        )
    }

    private fun String.toDeploymentTargetTestResponse(): DeploymentTargetTestResponse {
        val json = requireNotNull(asJsonOrNull()) { "Invalid deployment target test response." }
        return DeploymentTargetTestResponse(json.optBoolean("ok"), json.optString("status"), json.optString("message"))
    }

    private fun String.toDeploymentJobSummary(): DeploymentJobSummary = requireNotNull(asJsonOrNull()) { "Invalid deployment response." }.toDeploymentJobSummary()

    private fun String.toDeploymentActionResponse(): DeploymentActionResponse {
        val json = requireNotNull(asJsonOrNull()) { "Invalid deployment action response." }
        return DeploymentActionResponse(
            status = json.optionalString("status"),
            running = json.optBoolean("running"),
            message = json.optionalString("message"),
        )
    }

    private fun JSONObject.toDeploymentJobSummary(): DeploymentJobSummary = DeploymentJobSummary(
        deploymentId = optString("deploymentId"),
        projectId = optString("projectId"),
        targetId = optString("targetId"),
        buildId = optString("buildId"),
        status = optString("status"),
        createdAt = optionalString("createdAt"),
        finishedAt = optionalString("finishedAt"),
        errorMessage = optionalString("errorMessage"),
    )


    private fun CommandCreateRequest.toCommandPayload(): String = JSONObject()
        .put("name", name)
        .put("description", description)
        .put("type", "chat_input")
        .put("handler", JSONObject().put("kind", handlerKind).put("content", handlerContent).put("ephemeral", ephemeral))
        .toString()

    private fun String.toCommandResponse(): BotCommand = requireNotNull(asJsonOrNull()) { "Invalid command response." }.toBotCommand()

    private fun JSONObject.toBotCommand(): BotCommand {
        val permissionsJson = optJSONObject("permissions") ?: JSONObject()
        val handlerValue = opt("handler")
        val handlerJson = if (handlerValue is JSONObject) handlerValue else JSONObject().put("kind", "custom_typescript_placeholder")
        return BotCommand(
            id = optionalString("id"),
            name = optString("name"),
            description = optString("description"),
            type = optionalString("type") ?: "chat_input",
            permissions = BotCommandPermissions(
                defaultMemberPermissions = permissionsJson.optionalString("defaultMemberPermissions"),
                dmPermission = permissionsJson.optBoolean("dmPermission"),
            ),
            handler = BotCommandHandler(
                kind = handlerJson.optionalString("kind") ?: "static_response",
                ephemeral = handlerJson.optBoolean("ephemeral", true),
                content = handlerJson.optionalString("content"),
            ),
        )
    }

    private fun String.toGitHubStatusResponse(): GitHubStatusResponse {
        val json = requireNotNull(asJsonOrNull()) { "Invalid GitHub status response." }
        return GitHubStatusResponse(json.optBoolean("connected"), json.optionalString("tokenSecretRef"), json.optionalString("message"))
    }

    private fun String.toProjectScanResponse(): ProjectScanResponse {
        val json = requireNotNull(asJsonOrNull()) { "Invalid scan response." }
        val detection = json.optJSONObject("detection") ?: JSONObject()
        val matchesJson = detection.optJSONArray("matches") ?: JSONArray()
        val matches = (0 until matchesJson.length()).map { index ->
            val match = matchesJson.getJSONObject(index)
            val requiredSecrets = match.optJSONArray("requiredSecrets") ?: JSONArray()
            ScanDetectionMatch(
                id = match.optString("id"),
                name = match.optString("name"),
                score = match.optInt("score", 0),
                confidence = match.optString("confidence", "weak"),
                requiredSecrets = (0 until requiredSecrets.length()).mapNotNull { key -> requiredSecrets.optString(key).takeIf { it.isNotBlank() } },
            )
        }
        val warningsJson = detection.optJSONArray("warnings") ?: JSONArray()
        val warnings = (0 until warningsJson.length()).mapNotNull { index -> warningsJson.optString(index).takeIf { it.isNotBlank() } }
        return ProjectScanResponse(detection.optString("recommendedPackId", "unknown"), matches, warnings)
    }

    private fun String.toProjectProfileResponse(): ProjectProfileResponse = requireNotNull(asJsonOrNull()) { "Invalid project profile response." }.toProjectProfileResponse()

    private fun JSONObject.toProjectProfileResponse(): ProjectProfileResponse {
        val projectJson = optJSONObject("project")
        val importSourceJson = projectJson?.optJSONObject("importSource")
        val runtimeJson = optJSONObject("runtime")
        val bladePackJson = optJSONObject("bladePack")
        val detectedPacks = bladePackJson?.optJSONArray("detected") ?: JSONArray()
        val secretsJson = optJSONObject("secrets") ?: JSONObject()
        val gitJson = optJSONObject("git")
        return ProjectProfileResponse(
            schemaVersion = optString("schemaVersion"),
            generatedBy = optString("generatedBy"),
            generatedAt = optString("generatedAt"),
            project = projectJson?.let {
                ProjectProfileProject(
                    id = it.optionalString("id"),
                    name = it.optString("name"),
                    type = it.optString("type"),
                    root = it.optString("root"),
                    importSource = importSourceJson?.let { source ->
                        ProjectProfileImportSource(
                            kind = source.optString("kind"),
                            url = source.optionalString("url"),
                        )
                    },
                )
            },
            runtime = runtimeJson?.let {
                ProjectProfileRuntime(
                    type = it.optString("type"),
                    version = it.optString("version"),
                    packageManager = it.optionalString("packageManager") ?: "unknown",
                    detectedLanguages = it.optStringArray("detectedLanguages"),
                    detectedFrameworks = it.optStringArray("detectedFrameworks"),
                )
            },
            bladePack = bladePackJson?.let {
                ProjectProfileBladePack(
                    selected = it.optString("selected"),
                    version = it.optString("version"),
                    detected = (0 until detectedPacks.length()).map { index -> detectedPacks.getJSONObject(index).toProjectProfilePackEvidence() },
                )
            },
            commandPlan = (optJSONObject("commandPlan") ?: JSONObject()).toProjectProfileCommandPlan(),
            scriptProfiles = (optJSONArray("scriptProfiles") ?: JSONArray()).toScriptProfileSummaries(),
            secrets = ProjectProfileSecrets(
                required = (secretsJson.optJSONArray("required") ?: JSONArray()).toScriptSecretRefs(),
                optional = (secretsJson.optJSONArray("optional") ?: JSONArray()).toScriptSecretRefs(),
            ),
            permissions = optStringArray("permissions"),
            capabilities = optStringArray("capabilities"),
            importantFiles = optStringArray("importantFiles"),
            warnings = optStringArray("warnings"),
            repairCards = (optJSONArray("repairCards") ?: JSONArray()).toRepairCards(),
            git = gitJson?.toProjectProfileGit(),
        )
    }

    private fun JSONObject.toProjectProfilePackEvidence(): ProjectProfilePackEvidence = ProjectProfilePackEvidence(
        id = optString("id"),
        name = optString("name"),
        score = optInt("score"),
        confidence = optString("confidence"),
        matchedEvidence = optStringArray("matchedEvidence"),
    )

    private fun JSONObject.toProjectProfileCommandPlan(): ProjectProfileCommandPlan = ProjectProfileCommandPlan(
        install = optStringArray("install"),
        build = optStringArray("build"),
        test = optStringArray("test"),
        validate = optStringArray("validate"),
        start = optStringArray("start"),
        stop = optStringArray("stop"),
        restart = optStringArray("restart"),
        deploy = optStringArray("deploy"),
    )

    private fun String.toScriptProfileResponse(): ScriptProfileSummary = requireNotNull(asJsonOrNull()) { "Invalid script profile response." }.toScriptProfileSummary()

    private fun JSONArray.toScriptProfileSummaries(): List<ScriptProfileSummary> =
        (0 until length()).map { index -> getJSONObject(index).toScriptProfileSummary() }

    private fun JSONObject.toScriptProfileSummary(): ScriptProfileSummary = ScriptProfileSummary(
        id = optString("id"),
        projectId = optionalString("projectId"),
        name = optString("name"),
        description = optionalString("description"),
        source = optString("source"),
        runtime = optString("runtime"),
        command = optStringArray("command"),
        workingDirectory = optionalString("workingDirectory") ?: ".",
        envRefs = (optJSONArray("envRefs") ?: JSONArray()).toScriptEnvironmentRefs(),
        secretRefs = (optJSONArray("secretRefs") ?: JSONArray()).toScriptSecretRefs(),
        timeoutSeconds = optInt("timeoutSeconds", 300),
        requiresConfirmation = optBoolean("requiresConfirmation", false),
        tags = optStringArray("tags"),
        createdAt = optString("createdAt"),
        updatedAt = optString("updatedAt"),
    )

    private fun JSONArray.toScriptEnvironmentRefs(): List<ScriptEnvironmentRef> = (0 until length()).mapNotNull { index ->
        when (val value = opt(index)) {
            is JSONObject -> {
                val name = value.optionalString("name") ?: value.optionalString("key")
                name?.let { ScriptEnvironmentRef(name = it, source = value.optionalString("source")) }
            }
            is String -> value.takeIf { it.isNotBlank() }?.let { ScriptEnvironmentRef(name = it) }
            else -> null
        }
    }

    private fun JSONArray.toScriptSecretRefs(): List<ScriptSecretRef> = (0 until length()).mapNotNull { index ->
        when (val value = opt(index)) {
            is JSONObject -> {
                val id = value.optionalString("id") ?: value.optionalString("secretRef") ?: value.optionalString("name")
                id?.let {
                    ScriptSecretRef(
                        id = it,
                        name = value.optionalString("name"),
                        required = value.optionalBoolean("required"),
                        configured = value.optionalBoolean("configured"),
                    )
                }
            }
            is String -> value.takeIf { it.isNotBlank() }?.let { ScriptSecretRef(id = it) }
            else -> null
        }
    }

    private fun JSONArray.toRepairCards(): List<RepairCard> = (0 until length()).map { index ->
        val card = getJSONObject(index)
        RepairCard(
            title = card.optString("title"),
            evidence = card.optionalString("evidence"),
            safeAction = card.optString("safeAction"),
        )
    }

    private fun JSONObject.toProjectProfileGit(): ProjectProfileGit {
        val remotesJson = optJSONArray("remotes") ?: JSONArray()
        return ProjectProfileGit(
            branch = optionalString("branch"),
            status = optionalString("status") ?: "unknown",
            dirtyFileCount = optInt("dirtyFileCount", 0),
            remotes = (0 until remotesJson.length()).map { index ->
                val remote = remotesJson.getJSONObject(index)
                ProjectProfileGitRemote(name = remote.optString("name"), url = remote.optionalString("url"))
            },
        )
    }

    private fun String.asJsonOrNull(): JSONObject? = runCatching { JSONObject(this) }.getOrNull()

    private fun JSONObject.optionalString(name: String): String? = if (has(name) && !isNull(name)) {
        optString(name).takeIf { it.isNotBlank() }
    } else {
        null
    }

    private fun JSONObject.optionalBoolean(name: String): Boolean? = if (has(name) && !isNull(name)) {
        optBoolean(name)
    } else {
        null
    }

    private fun JSONObject.optStringArray(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
    }

    private fun String.encodedPathSegment(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())
        .replace("+", "%20")

    private fun String.encodedFilePath(): String = split("/").joinToString("/") { it.encodedPathSegment() }

    private fun String.cookieValue(): String = replace(";", "").replace("\r", "").replace("\n", "")

    private fun String.normalizePlainStatus(): String = replace(Regex("[{}\\\"]"), "")
        .replace(',', ' ')
        .trim()
        .ifBlank { "Unknown" }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T = try {
        block(this)
    } finally {
        disconnect()
    }
}


private fun String.toImportSummary(): ImportSummary {
    val json = runCatching { JSONObject(this) }.getOrNull()
        ?: throw IllegalArgumentException("Invalid import response.")
    val import = json.optJSONObject("import") ?: json
    val profileId = import.optString("profileId").takeIf { it.isNotBlank() }
    val blockedPolicy = import.optString("blockedPolicy").takeIf { it.isNotBlank() }
    return ImportSummary(
        id = import.optString("id"),
        state = import.optString("state"),
        profileId = profileId,
        blockedPolicy = blockedPolicy,
    )
}
