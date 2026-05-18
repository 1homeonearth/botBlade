package com.princess.royalscepter.data.api

import com.princess.royalscepter.data.model.ApiErrorResponse
import com.princess.royalscepter.data.model.BotProject
import com.princess.royalscepter.data.model.BotStatusResponse
import com.princess.royalscepter.data.model.BotToggleRequest
import com.princess.royalscepter.data.model.BotToggleResponse
import com.princess.royalscepter.data.model.DiscordProjectConfig
import com.princess.royalscepter.data.model.HealthResponse
import com.princess.royalscepter.data.model.ProjectCreateRequest
import com.princess.royalscepter.data.model.ProjectDeployment
import com.princess.royalscepter.data.model.ProjectPermissions
import com.princess.royalscepter.data.model.ProjectUpdateRequest
import com.princess.royalscepter.data.model.SecretSummary
import com.princess.royalscepter.data.model.SecretCreateRequest
import com.princess.royalscepter.data.model.ProjectFileSummary
import com.princess.royalscepter.data.model.ProjectFileContent
import com.princess.royalscepter.data.model.BuildSummary
import com.princess.royalscepter.data.model.BuildRequest
import com.princess.royalscepter.data.model.RuntimeStatusResponse
import com.princess.royalscepter.data.model.DeploymentTargetSummary
import com.princess.royalscepter.data.model.DeploymentTargetCreateRequest
import com.princess.royalscepter.data.model.DeploymentTargetTestResponse
import com.princess.royalscepter.data.model.DeploymentJobSummary
import com.princess.royalscepter.data.model.DeploymentCreateRequest
import com.princess.royalscepter.data.model.DeploymentActionResponse
import com.princess.royalscepter.data.model.DeploymentAdapterCapabilities
import com.princess.royalscepter.data.model.GitHubStatusResponse
import com.princess.royalscepter.data.model.BotCommand
import com.princess.royalscepter.data.model.BotCommandHandler
import com.princess.royalscepter.data.model.BotCommandPermissions
import com.princess.royalscepter.data.model.CommandCreateRequest
import com.princess.royalscepter.data.model.GitHubConnectRequest
import com.princess.royalscepter.data.model.GitHubLinkRepoRequest
import com.princess.royalscepter.data.model.GitHubProjectConfig
import com.princess.royalscepter.data.model.GitHubWorkflowResponse
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class RoyalScepterApiClient(
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
        request(path = "/api/projects/${projectId.urlPathSegment()}", method = "GET").toProjectResponse()

    @Throws(IOException::class)
    fun updateProject(projectId: String, request: ProjectUpdateRequest): BotProject {
        val payload = JSONObject().apply {
            request.name?.let { put("name", it) }
            request.description?.let { put("description", it) }
            request.templateId?.let { put("templateId", it) }
        }.toString()
        return request(path = "/api/projects/${projectId.urlPathSegment()}", method = "PATCH", requestBody = payload).toProjectResponse()
    }

    @Throws(IOException::class)
    fun archiveProject(projectId: String): BotProject =
        request(path = "/api/projects/${projectId.urlPathSegment()}/archive", method = "POST", requestBody = "{}").toProjectResponse()

    @Throws(IOException::class)
    fun cloneProject(projectId: String): BotProject =
        request(path = "/api/projects/${projectId.urlPathSegment()}/clone", method = "POST", requestBody = "{}").toProjectResponse()

    @Throws(IOException::class)
    fun listCommands(projectId: String): List<BotCommand> {
        val body = request(path = "/api/projects/${projectId.urlPathSegment()}/commands", method = "GET")
        val commands = requireNotNull(body.asJsonOrNull()) { "Invalid commands response." }.optJSONArray("commands") ?: JSONArray()
        return (0 until commands.length()).map { index -> commands.getJSONObject(index).toBotCommand() }
    }

    @Throws(IOException::class)
    fun createCommand(projectId: String, command: CommandCreateRequest): BotCommand =
        request(path = "/api/projects/${projectId.urlPathSegment()}/commands", method = "POST", requestBody = command.toCommandPayload()).toCommandResponse()

    @Throws(IOException::class)
    fun updateCommand(projectId: String, commandId: String, command: CommandCreateRequest): BotCommand =
        request(path = "/api/projects/${projectId.urlPathSegment()}/commands/${commandId.urlPathSegment()}", method = "PATCH", requestBody = command.toCommandPayload()).toCommandResponse()

    @Throws(IOException::class)
    fun deleteCommand(projectId: String, commandId: String) {
        request(path = "/api/projects/${projectId.urlPathSegment()}/commands/${commandId.urlPathSegment()}", method = "DELETE")
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
        return request(path = "/api/projects/${projectId.urlPathSegment()}/github/create-repo", method = "POST", requestBody = payload).toProjectResponse()
    }

    @Throws(IOException::class)
    fun pushGitHub(projectId: String): String =
        request(path = "/api/projects/${projectId.urlPathSegment()}/github/push", method = "POST", requestBody = "{}")

    @Throws(IOException::class)
    fun createGitHubWorkflow(projectId: String): GitHubWorkflowResponse {
        val json = requireNotNull(request(path = "/api/projects/${projectId.urlPathSegment()}/github/create-workflow", method = "POST", requestBody = "{}").asJsonOrNull()) { "Invalid workflow response." }
        return GitHubWorkflowResponse(path = json.optString("path"), content = json.optString("content"))
    }


    @Throws(IOException::class)
    fun generateProject(projectId: String): List<ProjectFileSummary> {
        val body = request(path = "/api/projects/${projectId.urlPathSegment()}/generate", method = "POST", requestBody = "{}")
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid generate response." }
        return json.optJSONArray("files").toFileSummaries()
    }

    @Throws(IOException::class)
    fun listProjectFiles(projectId: String): List<ProjectFileSummary> {
        val body = request(path = "/api/projects/${projectId.urlPathSegment()}/files", method = "GET")
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid files response." }
        return json.optJSONArray("files").toFileSummaries()
    }

    @Throws(IOException::class)
    fun getProjectFile(projectId: String, filePath: String): ProjectFileContent {
        val body = request(path = "/api/projects/${projectId.urlPathSegment()}/files/${filePath.urlFilePath()}", method = "GET")
        return requireNotNull(body.asJsonOrNull()) { "Invalid file response." }.toProjectFileContent()
    }

    @Throws(IOException::class)
    fun saveProjectFile(projectId: String, filePath: String, content: String): ProjectFileContent {
        val payload = JSONObject().put("content", content).toString()
        val body = request(path = "/api/projects/${projectId.urlPathSegment()}/files/${filePath.urlFilePath()}", method = "PUT", requestBody = payload)
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
        return request(path = "/api/secrets/${secretId.urlPathSegment()}/rotate", method = "POST", requestBody = payload).toSecretResponse()
    }

    @Throws(IOException::class)
    fun deleteSecret(secretId: String) {
        request(path = "/api/secrets/${secretId.urlPathSegment()}", method = "DELETE")
    }

    @Throws(IOException::class)
    fun createBuild(projectId: String, buildRequest: BuildRequest = BuildRequest()): BuildSummary {
        val payload = JSONObject()
            .put("source", buildRequest.source)
            .put("clean", buildRequest.clean)
            .put("runTests", buildRequest.runTests)
            .put("createDockerImage", buildRequest.createDockerImage)
            .toString()
        return request(path = "/api/projects/${projectId.urlPathSegment()}/builds", method = "POST", requestBody = payload).toBuildSummary()
    }

    @Throws(IOException::class)
    fun listBuilds(projectId: String): List<BuildSummary> {
        val body = request(path = "/api/projects/${projectId.urlPathSegment()}/builds", method = "GET")
        val json = requireNotNull(body.asJsonOrNull()) { "Invalid builds response." }
        val builds = json.optJSONArray("builds") ?: JSONArray()
        return (0 until builds.length()).map { index -> builds.getJSONObject(index).toBuildSummary() }
    }

    @Throws(IOException::class)
    fun getBuild(projectId: String, buildId: String): BuildSummary =
        request(path = "/api/projects/${projectId.urlPathSegment()}/builds/${buildId.urlPathSegment()}", method = "GET").toBuildSummary()

    @Throws(IOException::class)
    fun getBuildLogs(projectId: String, buildId: String): String {
        val body = request(path = "/api/projects/${projectId.urlPathSegment()}/builds/${buildId.urlPathSegment()}/logs", method = "GET")
        return body.asJsonOrNull()?.optionalString("logs") ?: body
    }


    @Throws(IOException::class)
    fun getProjectRuntimeStatus(projectId: String): RuntimeStatusResponse =
        request(path = "/api/projects/${projectId.urlPathSegment()}/runtime/status", method = "GET").toRuntimeStatusResponse()

    @Throws(IOException::class)
    fun runtimeAction(projectId: String, action: String): RuntimeStatusResponse =
        request(path = "/api/projects/${projectId.urlPathSegment()}/runtime/$action", method = "POST", requestBody = "{}").toRuntimeStatusResponse()

    @Throws(IOException::class)
    fun getProjectRuntimeLogs(projectId: String): String {
        val body = request(path = "/api/projects/${projectId.urlPathSegment()}/runtime/logs", method = "GET")
        return body.asJsonOrNull()?.optionalString("logs") ?: body
    }

    @Throws(IOException::class)
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
        request(path = "/api/deployment-targets/${targetId.urlPathSegment()}/test", method = "POST", requestBody = "{}").toDeploymentTargetTestResponse()

    @Throws(IOException::class)
    fun createDeployment(projectId: String, request: DeploymentCreateRequest): DeploymentJobSummary {
        val payload = JSONObject().put("targetId", request.targetId).put("buildId", request.buildId).toString()
        return this.request(path = "/api/projects/${projectId.urlPathSegment()}/deployments", method = "POST", requestBody = payload).toDeploymentJobSummary()
    }

    @Throws(IOException::class)
    fun listDeployments(projectId: String): List<DeploymentJobSummary> {
        val body = request(path = "/api/projects/${projectId.urlPathSegment()}/deployments", method = "GET")
        val deployments = requireNotNull(body.asJsonOrNull()) { "Invalid deployments response." }.optJSONArray("deployments") ?: JSONArray()
        return (0 until deployments.length()).map { index -> deployments.getJSONObject(index).toDeploymentJobSummary() }
    }

    @Throws(IOException::class)
    fun getDeploymentLogs(projectId: String, deploymentId: String): String {
        val body = request(path = "/api/projects/${projectId.urlPathSegment()}/deployments/${deploymentId.urlPathSegment()}/logs", method = "GET")
        return body.asJsonOrNull()?.optionalString("logs") ?: body
    }

    @Throws(IOException::class)
    fun getDeploymentStatus(projectId: String, deploymentId: String): DeploymentActionResponse =
        request(path = "/api/projects/${projectId.urlPathSegment()}/deployments/${deploymentId.urlPathSegment()}/status", method = "GET").toDeploymentActionResponse()

    @Throws(IOException::class)
    fun deploymentAction(projectId: String, deploymentId: String, action: String): DeploymentActionResponse =
        request(path = "/api/projects/${projectId.urlPathSegment()}/deployments/${deploymentId.urlPathSegment()}/${action.urlPathSegment()}", method = "POST", requestBody = "{}").toDeploymentActionResponse()

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
            sessionToken.isNotBlank() -> connection.setRequestProperty("Cookie", "royalScepterSession=${sessionToken.cookieValue()}")
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

    private fun String.asJsonOrNull(): JSONObject? = runCatching { JSONObject(this) }.getOrNull()

    private fun JSONObject.optionalString(name: String): String? = if (has(name) && !isNull(name)) {
        optString(name).takeIf { it.isNotBlank() }
    } else {
        null
    }

    private fun JSONObject.optStringArray(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
    }

    private fun String.urlPathSegment(): String = replace("/", "")

    private fun String.urlFilePath(): String = split("/").joinToString("/") { it.urlPathSegment() }

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
