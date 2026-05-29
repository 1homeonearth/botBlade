package com.princess.botblade.data.repository

import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.api.BotBladeApiClient
import com.princess.botblade.data.model.ProjectProfileResponse
import com.princess.botblade.data.model.ScriptProfileSummary
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScriptProfileRepository(
    private val apiClient: BotBladeApiClient = BotBladeApiClient(),
) {
    suspend fun getProjectProfile(projectId: String): ApiResult<ProjectProfileResponse> = apiCall {
        apiClient.getProjectProfile(projectId)
    }

    suspend fun listScriptProfiles(projectId: String): ApiResult<List<ScriptProfileSummary>> = apiCall {
        apiClient.listScriptProfiles(projectId)
    }

    suspend fun getScriptProfile(projectId: String, scriptProfileId: String): ApiResult<ScriptProfileSummary> = apiCall {
        apiClient.getScriptProfile(projectId, scriptProfileId)
    }

    private suspend fun <T> apiCall(block: () -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        runCatching { block() }.fold(
            onSuccess = { ApiResult.Success(it) },
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },
        )
    }

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {
        is IOException -> throwable.message ?: "Unable to reach the local botBlade backend for script profiles."
        else -> throwable.message ?: "Unexpected script profile error."
    }
}
