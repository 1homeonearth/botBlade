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

data class ProjectDetail(
    val id: String,
    val name: String,
    val description: String? = null,
    val runtimeStatus: String? = null,
    val build: BuildSummary? = null,
    val deployment: DeploymentSummary? = null,
    val secrets: List<SecretSummary> = emptyList(),
)

data class BuildSummary(
    val id: String,
    val status: String,
    val createdAt: String? = null,
)

data class DeploymentSummary(
    val id: String,
    val status: String,
    val environment: String? = null,
    val createdAt: String? = null,
)

data class SecretSummary(
    val key: String,
    val isConfigured: Boolean,
)
