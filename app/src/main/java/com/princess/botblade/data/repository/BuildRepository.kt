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
import java.io.IOException  // line 13: executes this statement as part of this file's behavior
import kotlinx.coroutines.Dispatchers  // line 14: executes this statement as part of this file's behavior
import kotlinx.coroutines.withContext  // line 15: executes this statement as part of this file's behavior

class BuildRepository(  // line 17: executes this statement as part of this file's behavior
    private val apiClient: BotBladeApiClient = BotBladeApiClient(),  // line 18: executes this statement as part of this file's behavior
) {  // line 19: executes this statement as part of this file's behavior
    suspend fun createBuild(projectId: String): ApiResult<BuildSummary> = apiCall { apiClient.createBuild(projectId, BuildRequest()) }  // line 20: executes this statement as part of this file's behavior
    suspend fun listBuilds(projectId: String): ApiResult<List<BuildSummary>> = apiCall { apiClient.listBuilds(projectId) }  // line 21: executes this statement as part of this file's behavior
    suspend fun getBuild(projectId: String, buildId: String): ApiResult<BuildSummary> = apiCall { apiClient.getBuild(projectId, buildId) }  // line 22: executes this statement as part of this file's behavior
    suspend fun getBuildLogs(projectId: String, buildId: String): ApiResult<String> = apiCall { apiClient.getBuildLogs(projectId, buildId) }  // line 23: executes this statement as part of this file's behavior

    private suspend fun <T> apiCall(block: () -> T): ApiResult<T> = withContext(Dispatchers.IO) {  // line 25: executes this statement as part of this file's behavior
        runCatching { block() }.fold(  // line 26: executes this statement as part of this file's behavior
            onSuccess = { ApiResult.Success(it) },  // line 27: executes this statement as part of this file's behavior
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },  // line 28: executes this statement as part of this file's behavior
        )  // line 29: executes this statement as part of this file's behavior
    }  // line 30: executes this statement as part of this file's behavior

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {  // line 32: executes this statement as part of this file's behavior
        is IOException -> throwable.message ?: "Unable to reach the local botBlade backend."  // line 33: executes this statement as part of this file's behavior
        else -> throwable.message ?: "Unexpected build error."  // line 34: executes this statement as part of this file's behavior
    }  // line 35: executes this statement as part of this file's behavior
}  // line 36: executes this statement as part of this file's behavior
