// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.repository  // line 7: executes this statement as part of this file's behavior

import com.princess.botblade.data.api.ApiResult  // line 9: executes this statement as part of this file's behavior
import com.princess.botblade.data.api.BotBladeApiClient  // line 10: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BuildRequest  // line 11: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BuildSummary  // line 12: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectFileContent  // line 13: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectFileSummary  // line 14: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectScanResponse  // line 15: executes this statement as part of this file's behavior
import java.io.IOException  // line 16: executes this statement as part of this file's behavior
import kotlinx.coroutines.Dispatchers  // line 17: executes this statement as part of this file's behavior
import kotlinx.coroutines.withContext  // line 18: executes this statement as part of this file's behavior

class EditorRepository(  // line 20: executes this statement as part of this file's behavior
    private val apiClient: BotBladeApiClient = BotBladeApiClient(),  // line 21: executes this statement as part of this file's behavior
) {  // line 22: executes this statement as part of this file's behavior
    suspend fun generateProject(projectId: String): ApiResult<List<ProjectFileSummary>> = apiCall { apiClient.generateProject(projectId) }  // line 23: executes this statement as part of this file's behavior
    suspend fun listFiles(projectId: String): ApiResult<List<ProjectFileSummary>> = apiCall { apiClient.listProjectFiles(projectId) }  // line 24: executes this statement as part of this file's behavior
    suspend fun getFile(projectId: String, path: String): ApiResult<ProjectFileContent> = apiCall { apiClient.getProjectFile(projectId, path) }  // line 25: executes this statement as part of this file's behavior
    suspend fun saveFile(projectId: String, path: String, content: String): ApiResult<ProjectFileContent> = apiCall { apiClient.saveProjectFile(projectId, path, content) }  // line 26: executes this statement as part of this file's behavior
    suspend fun createBuild(projectId: String): ApiResult<BuildSummary> = apiCall { apiClient.createBuild(projectId, BuildRequest()) }  // line 27: executes this statement as part of this file's behavior
    suspend fun scanProject(projectId: String): ApiResult<ProjectScanResponse> = apiCall { apiClient.scanProject(projectId) }  // line 28: executes this statement as part of this file's behavior

    private suspend fun <T> apiCall(block: () -> T): ApiResult<T> = withContext(Dispatchers.IO) {  // line 30: executes this statement as part of this file's behavior
        runCatching { block() }.fold(  // line 31: executes this statement as part of this file's behavior
            onSuccess = { ApiResult.Success(it) },  // line 32: executes this statement as part of this file's behavior
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },  // line 33: executes this statement as part of this file's behavior
        )  // line 34: executes this statement as part of this file's behavior
    }  // line 35: executes this statement as part of this file's behavior

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {  // line 37: executes this statement as part of this file's behavior
        is IOException -> throwable.message ?: "Unable to reach the local botBlade backend."  // line 38: executes this statement as part of this file's behavior
        else -> throwable.message ?: "Unexpected editor error."  // line 39: executes this statement as part of this file's behavior
    }  // line 40: executes this statement as part of this file's behavior
}  // line 41: executes this statement as part of this file's behavior
