package com.princess.royalscepter.data.repository

import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.api.RoyalScepterApiClient
import com.princess.royalscepter.data.model.BuildRequest
import com.princess.royalscepter.data.model.BuildSummary
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildRepository(
    private val apiClient: RoyalScepterApiClient = RoyalScepterApiClient(),
) {
    suspend fun createBuild(projectId: String): ApiResult<BuildSummary> = apiCall { apiClient.createBuild(projectId, BuildRequest()) }
    suspend fun listBuilds(projectId: String): ApiResult<List<BuildSummary>> = apiCall { apiClient.listBuilds(projectId) }
    suspend fun getBuild(projectId: String, buildId: String): ApiResult<BuildSummary> = apiCall { apiClient.getBuild(projectId, buildId) }
    suspend fun getBuildLogs(projectId: String, buildId: String): ApiResult<String> = apiCall { apiClient.getBuildLogs(projectId, buildId) }

    private suspend fun <T> apiCall(block: () -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        runCatching { block() }.fold(
            onSuccess = { ApiResult.Success(it) },
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },
        )
    }

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {
        is IOException -> throwable.message ?: "Unable to reach the local royalScepter backend."
        else -> throwable.message ?: "Unexpected build error."
    }
}
