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
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class RoyalScepterApiClient(
    private val baseUrl: String = ApiConfig.DEFAULT_BASE_URL,
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
    private fun request(
        path: String,
        method: String,
        requestBody: String? = null,
    ): String {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 2_000
        connection.readTimeout = 2_000
        connection.doInput = true
        connection.setRequestProperty("Accept", "application/json")

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
            deployment = ProjectDeployment(
                targetId = deploymentJson.optionalString("targetId"),
                lastDeploymentId = deploymentJson.optionalString("lastDeploymentId"),
            ),
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
