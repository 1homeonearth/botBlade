package com.princess.royalscepter.data.repository

import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.api.RoyalScepterApiClient
import com.princess.royalscepter.data.model.DeploymentCreateRequest
import com.princess.royalscepter.data.model.DeploymentJobSummary
import com.princess.royalscepter.data.model.DeploymentTargetCreateRequest
import com.princess.royalscepter.data.model.DeploymentTargetSummary
import com.princess.royalscepter.data.model.DeploymentTargetTestResponse
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeploymentRepository(
    private val apiClient: RoyalScepterApiClient = RoyalScepterApiClient(),
) {
    suspend fun listTargets(): ApiResult<List<DeploymentTargetSummary>> = apiCall { apiClient.listDeploymentTargets() }
    suspend fun createTarget(name: String, type: String): ApiResult<DeploymentTargetSummary> = apiCall { apiClient.createDeploymentTarget(DeploymentTargetCreateRequest(name, type)) }
    suspend fun testTarget(targetId: String): ApiResult<DeploymentTargetTestResponse> = apiCall { apiClient.testDeploymentTarget(targetId) }
    suspend fun createDeployment(projectId: String, targetId: String, buildId: String): ApiResult<DeploymentJobSummary> = apiCall { apiClient.createDeployment(projectId, DeploymentCreateRequest(targetId, buildId)) }
    suspend fun listDeployments(projectId: String): ApiResult<List<DeploymentJobSummary>> = apiCall { apiClient.listDeployments(projectId) }
    suspend fun getDeploymentLogs(projectId: String, deploymentId: String): ApiResult<String> = apiCall { apiClient.getDeploymentLogs(projectId, deploymentId) }
    suspend fun getGitHubStatus() = apiCall { apiClient.getGitHubStatus() }

    private suspend fun <T> apiCall(block: () -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        runCatching { block() }.fold(
            onSuccess = { ApiResult.Success(it) },
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },
        )
    }

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {
        is IOException -> throwable.message ?: "Unable to reach the local royalScepter backend."
        else -> throwable.message ?: "Unexpected deployment error."
    }
}
