package com.princess.botblade.data.repository

import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.api.BotBladeApiClient
import com.princess.botblade.data.model.BotToggleRequest
import com.princess.botblade.data.model.RuntimeStatusResponse
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DashboardRepository(
    private val apiClient: BotBladeApiClient = BotBladeApiClient(),
) {
    suspend fun getBotStatus(projectId: String?): ApiResult<RuntimeStatusResponse> = withContext(Dispatchers.IO) {
        runCatching {
            if (projectId != null) {
                apiClient.getProjectRuntimeStatus(projectId)
            } else {
                val legacy = apiClient.getBotStatus()
                RuntimeStatusResponse(projectId = "legacy", status = legacy.status, message = legacy.message)
            }
        }.fold(
            onSuccess = { ApiResult.Success(it) },
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },
        )
    }

    suspend fun runtimeAction(projectId: String?, action: String, currentStatus: String? = null): ApiResult<RuntimeStatusResponse> = withContext(Dispatchers.IO) {
        runCatching {
            if (projectId != null) {
                apiClient.runtimeAction(projectId, action)
            } else {
                runLegacyRuntimeAction(action, currentStatus)
            }
        }.fold(
            onSuccess = { ApiResult.Success(it) },
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },
        )
    }

    private fun runLegacyRuntimeAction(action: String, currentStatus: String?): RuntimeStatusResponse {
        if (action == "restart") {
            if (currentStatus.equals("running", ignoreCase = true)) {
                apiClient.toggleBot(BotToggleRequest(action = "stop"))
            }
            val response = apiClient.toggleBot(BotToggleRequest(action = "start"))
            return RuntimeStatusResponse(projectId = "legacy", status = response.status ?: "unknown", message = response.message ?: "Restart requested through legacy start/stop controls.")
        }
        val legacyAction = when (action) {
            "stop" -> "stop"
            "start" -> "start"
            else -> if (currentStatus.equals("running", ignoreCase = true)) "stop" else "start"
        }
        val response = apiClient.toggleBot(BotToggleRequest(action = legacyAction))
        return RuntimeStatusResponse(projectId = "legacy", status = response.status ?: "unknown", message = response.message)
    }

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {
        is IOException -> "Unable to reach the local botBlade backend. Confirm it is running and reachable."
        else -> throwable.message ?: "Unexpected dashboard error."
    }
}
