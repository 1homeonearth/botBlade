package com.princess.royalscepter.data.repository

import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.api.RoyalScepterApiClient
import com.princess.royalscepter.data.model.BotProject
import com.princess.royalscepter.data.model.ProjectCreateRequest
import com.princess.royalscepter.data.model.ProjectUpdateRequest
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectRepository(
    private val apiClient: RoyalScepterApiClient = RoyalScepterApiClient(),
) {
    suspend fun listProjects(): ApiResult<List<BotProject>> = apiCall { apiClient.listProjects() }

    suspend fun createProject(request: ProjectCreateRequest): ApiResult<BotProject> = apiCall {
        apiClient.createProject(request)
    }

    suspend fun getProject(projectId: String): ApiResult<BotProject> = apiCall {
        apiClient.getProject(projectId)
    }

    suspend fun updateProject(projectId: String, request: ProjectUpdateRequest): ApiResult<BotProject> = apiCall {
        apiClient.updateProject(projectId, request)
    }

    suspend fun archiveProject(projectId: String): ApiResult<BotProject> = apiCall {
        apiClient.archiveProject(projectId)
    }

    suspend fun cloneProject(projectId: String): ApiResult<BotProject> = apiCall {
        apiClient.cloneProject(projectId)
    }

    private suspend fun <T> apiCall(block: () -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        runCatching { block() }
            .fold(
                onSuccess = { ApiResult.Success(it) },
                onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },
            )
    }

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {
        is IOException -> throwable.message ?: "Unable to reach the local royalScepter backend. Confirm it is running and reachable."
        else -> throwable.message ?: "Unexpected project error."
    }
}
