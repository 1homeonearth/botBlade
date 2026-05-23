// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.repository  // line 7: executes this statement as part of this file's behavior

import com.princess.botblade.data.api.ApiResult  // line 9: executes this statement as part of this file's behavior
import com.princess.botblade.data.api.BotBladeApiClient  // line 10: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotProject  // line 11: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotCommand  // line 12: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.CommandCreateRequest  // line 13: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubConnectRequest  // line 14: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubLinkRepoRequest  // line 15: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubStatusResponse  // line 16: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubWorkflowResponse  // line 17: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectCreateRequest  // line 18: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectUpdateRequest  // line 19: executes this statement as part of this file's behavior
import java.io.IOException  // line 20: executes this statement as part of this file's behavior
import kotlinx.coroutines.Dispatchers  // line 21: executes this statement as part of this file's behavior
import kotlinx.coroutines.withContext  // line 22: executes this statement as part of this file's behavior

class ProjectRepository(  // line 24: executes this statement as part of this file's behavior
    private val apiClient: BotBladeApiClient = BotBladeApiClient(),  // line 25: executes this statement as part of this file's behavior
) {  // line 26: executes this statement as part of this file's behavior
    suspend fun listProjects(): ApiResult<List<BotProject>> = apiCall { apiClient.listProjects() }  // line 27: executes this statement as part of this file's behavior

    suspend fun createProject(request: ProjectCreateRequest): ApiResult<BotProject> = apiCall {  // line 29: executes this statement as part of this file's behavior
        apiClient.createProject(request)  // line 30: executes this statement as part of this file's behavior
    }  // line 31: executes this statement as part of this file's behavior

    suspend fun getProject(projectId: String): ApiResult<BotProject> = apiCall {  // line 33: executes this statement as part of this file's behavior
        apiClient.getProject(projectId)  // line 34: executes this statement as part of this file's behavior
    }  // line 35: executes this statement as part of this file's behavior

    suspend fun updateProject(projectId: String, request: ProjectUpdateRequest): ApiResult<BotProject> = apiCall {  // line 37: executes this statement as part of this file's behavior
        apiClient.updateProject(projectId, request)  // line 38: executes this statement as part of this file's behavior
    }  // line 39: executes this statement as part of this file's behavior

    suspend fun archiveProject(projectId: String): ApiResult<BotProject> = apiCall {  // line 41: executes this statement as part of this file's behavior
        apiClient.archiveProject(projectId)  // line 42: executes this statement as part of this file's behavior
    }  // line 43: executes this statement as part of this file's behavior

    suspend fun cloneProject(projectId: String): ApiResult<BotProject> = apiCall {  // line 45: executes this statement as part of this file's behavior
        apiClient.cloneProject(projectId)  // line 46: executes this statement as part of this file's behavior
    }  // line 47: executes this statement as part of this file's behavior

    suspend fun listCommands(projectId: String): ApiResult<List<BotCommand>> = apiCall {  // line 49: executes this statement as part of this file's behavior
        apiClient.listCommands(projectId)  // line 50: executes this statement as part of this file's behavior
    }  // line 51: executes this statement as part of this file's behavior

    suspend fun createCommand(projectId: String, request: CommandCreateRequest): ApiResult<BotCommand> = apiCall {  // line 53: executes this statement as part of this file's behavior
        apiClient.createCommand(projectId, request)  // line 54: executes this statement as part of this file's behavior
    }  // line 55: executes this statement as part of this file's behavior

    suspend fun updateCommand(projectId: String, commandId: String, request: CommandCreateRequest): ApiResult<BotCommand> = apiCall {  // line 57: executes this statement as part of this file's behavior
        apiClient.updateCommand(projectId, commandId, request)  // line 58: executes this statement as part of this file's behavior
    }  // line 59: executes this statement as part of this file's behavior

    suspend fun deleteCommand(projectId: String, commandId: String): ApiResult<Unit> = apiCall {  // line 61: executes this statement as part of this file's behavior
        apiClient.deleteCommand(projectId, commandId)  // line 62: executes this statement as part of this file's behavior
    }  // line 63: executes this statement as part of this file's behavior

    suspend fun getGitHubStatus(): ApiResult<GitHubStatusResponse> = apiCall {  // line 65: executes this statement as part of this file's behavior
        apiClient.getGitHubStatus()  // line 66: executes this statement as part of this file's behavior
    }  // line 67: executes this statement as part of this file's behavior

    suspend fun connectGitHub(request: GitHubConnectRequest): ApiResult<GitHubStatusResponse> = apiCall {  // line 69: executes this statement as part of this file's behavior
        apiClient.connectGitHub(request)  // line 70: executes this statement as part of this file's behavior
    }  // line 71: executes this statement as part of this file's behavior

    suspend fun linkGitHubRepo(projectId: String, request: GitHubLinkRepoRequest): ApiResult<BotProject> = apiCall {  // line 73: executes this statement as part of this file's behavior
        apiClient.linkGitHubRepo(projectId, request)  // line 74: executes this statement as part of this file's behavior
    }  // line 75: executes this statement as part of this file's behavior

    suspend fun pushGitHub(projectId: String): ApiResult<String> = apiCall {  // line 77: executes this statement as part of this file's behavior
        apiClient.pushGitHub(projectId)  // line 78: executes this statement as part of this file's behavior
    }  // line 79: executes this statement as part of this file's behavior

    suspend fun createGitHubWorkflow(projectId: String): ApiResult<GitHubWorkflowResponse> = apiCall {  // line 81: executes this statement as part of this file's behavior
        apiClient.createGitHubWorkflow(projectId)  // line 82: executes this statement as part of this file's behavior
    }  // line 83: executes this statement as part of this file's behavior

    private suspend fun <T> apiCall(block: () -> T): ApiResult<T> = withContext(Dispatchers.IO) {  // line 85: executes this statement as part of this file's behavior
        runCatching { block() }  // line 86: executes this statement as part of this file's behavior
            .fold(  // line 87: executes this statement as part of this file's behavior
                onSuccess = { ApiResult.Success(it) },  // line 88: executes this statement as part of this file's behavior
                onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },  // line 89: executes this statement as part of this file's behavior
            )  // line 90: executes this statement as part of this file's behavior
    }  // line 91: executes this statement as part of this file's behavior

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {  // line 93: executes this statement as part of this file's behavior
        is IOException -> throwable.message ?: "Unable to reach the local botBlade backend. Confirm it is running and reachable."  // line 94: executes this statement as part of this file's behavior
        else -> throwable.message ?: "Unexpected project error."  // line 95: executes this statement as part of this file's behavior
    }  // line 96: executes this statement as part of this file's behavior
}  // line 97: executes this statement as part of this file's behavior
