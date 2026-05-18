package com.princess.botblade.data.repository

import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.api.BotBladeApiClient
import com.princess.botblade.data.model.BuildRequest
import com.princess.botblade.data.model.BuildSummary
import com.princess.botblade.data.model.ProjectFileContent
import com.princess.botblade.data.model.ProjectFileSummary
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditorRepository(
    private val apiClient: BotBladeApiClient = BotBladeApiClient(),
) {
    suspend fun generateProject(projectId: String): ApiResult<List<ProjectFileSummary>> = apiCall { apiClient.generateProject(projectId) }
    suspend fun listFiles(projectId: String): ApiResult<List<ProjectFileSummary>> = apiCall { apiClient.listProjectFiles(projectId) }
    suspend fun getFile(projectId: String, path: String): ApiResult<ProjectFileContent> = apiCall { apiClient.getProjectFile(projectId, path) }
    suspend fun saveFile(projectId: String, path: String, content: String): ApiResult<ProjectFileContent> = apiCall { apiClient.saveProjectFile(projectId, path, content) }
    suspend fun createBuild(projectId: String): ApiResult<BuildSummary> = apiCall { apiClient.createBuild(projectId, BuildRequest()) }

    private suspend fun <T> apiCall(block: () -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        runCatching { block() }.fold(
            onSuccess = { ApiResult.Success(it) },
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },
        )
    }

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {
        is IOException -> throwable.message ?: "Unable to reach the local botBlade backend."
        else -> throwable.message ?: "Unexpected editor error."
    }
}
