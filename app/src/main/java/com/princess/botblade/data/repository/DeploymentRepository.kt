// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.repository  // line 7: executes this statement as part of this file's behavior

import com.princess.botblade.data.api.ApiResult  // line 9: executes this statement as part of this file's behavior
import com.princess.botblade.data.api.BotBladeApiClient  // line 10: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentCreateRequest  // line 11: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentJobSummary  // line 12: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentTargetCreateRequest  // line 13: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentTargetSummary  // line 14: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentTargetTestResponse  // line 15: executes this statement as part of this file's behavior
import java.io.IOException  // line 16: executes this statement as part of this file's behavior
import kotlinx.coroutines.Dispatchers  // line 17: executes this statement as part of this file's behavior
import kotlinx.coroutines.withContext  // line 18: executes this statement as part of this file's behavior

class DeploymentRepository(  // line 20: executes this statement as part of this file's behavior
    private val apiClient: BotBladeApiClient = BotBladeApiClient(),  // line 21: executes this statement as part of this file's behavior
) {  // line 22: executes this statement as part of this file's behavior
    suspend fun listTargets(): ApiResult<List<DeploymentTargetSummary>> = apiCall { apiClient.listDeploymentTargets() }  // line 23: executes this statement as part of this file's behavior
    suspend fun createTarget(name: String, type: String): ApiResult<DeploymentTargetSummary> = apiCall { apiClient.createDeploymentTarget(DeploymentTargetCreateRequest(name, type)) }  // line 24: executes this statement as part of this file's behavior
    suspend fun testTarget(targetId: String): ApiResult<DeploymentTargetTestResponse> = apiCall { apiClient.testDeploymentTarget(targetId) }  // line 25: executes this statement as part of this file's behavior
    suspend fun createDeployment(projectId: String, targetId: String, buildId: String): ApiResult<DeploymentJobSummary> = apiCall { apiClient.createDeployment(projectId, DeploymentCreateRequest(targetId, buildId)) }  // line 26: executes this statement as part of this file's behavior
    suspend fun listDeployments(projectId: String): ApiResult<List<DeploymentJobSummary>> = apiCall { apiClient.listDeployments(projectId) }  // line 27: executes this statement as part of this file's behavior
    suspend fun getDeploymentLogs(projectId: String, deploymentId: String): ApiResult<String> = apiCall { apiClient.getDeploymentLogs(projectId, deploymentId) }  // line 28: executes this statement as part of this file's behavior
    suspend fun getDeploymentStatus(projectId: String, deploymentId: String) = apiCall { apiClient.getDeploymentStatus(projectId, deploymentId) }  // line 29: executes this statement as part of this file's behavior
    suspend fun restartDeployment(projectId: String, deploymentId: String) = apiCall { apiClient.deploymentAction(projectId, deploymentId, "restart") }  // line 30: executes this statement as part of this file's behavior
    suspend fun rollbackDeployment(projectId: String, deploymentId: String) = apiCall { apiClient.deploymentAction(projectId, deploymentId, "rollback") }  // line 31: executes this statement as part of this file's behavior
    suspend fun getGitHubStatus() = apiCall { apiClient.getGitHubStatus() }  // line 32: executes this statement as part of this file's behavior

    private suspend fun <T> apiCall(block: () -> T): ApiResult<T> = withContext(Dispatchers.IO) {  // line 34: executes this statement as part of this file's behavior
        runCatching { block() }.fold(  // line 35: executes this statement as part of this file's behavior
            onSuccess = { ApiResult.Success(it) },  // line 36: executes this statement as part of this file's behavior
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },  // line 37: executes this statement as part of this file's behavior
        )  // line 38: executes this statement as part of this file's behavior
    }  // line 39: executes this statement as part of this file's behavior

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {  // line 41: executes this statement as part of this file's behavior
        is IOException -> throwable.message ?: "Unable to reach the local botBlade backend."  // line 42: executes this statement as part of this file's behavior
        else -> throwable.message ?: "Unexpected deployment error."  // line 43: executes this statement as part of this file's behavior
    }  // line 44: executes this statement as part of this file's behavior
}  // line 45: executes this statement as part of this file's behavior
