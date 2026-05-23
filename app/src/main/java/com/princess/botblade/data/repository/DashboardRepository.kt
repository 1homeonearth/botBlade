// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.repository  // line 7: executes this statement as part of this file's behavior

import com.princess.botblade.data.api.ApiResult  // line 9: executes this statement as part of this file's behavior
import com.princess.botblade.data.api.BotBladeApiClient  // line 10: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotStatusResponse  // line 11: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotToggleRequest  // line 12: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotToggleResponse  // line 13: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.RuntimeStatusResponse  // line 14: executes this statement as part of this file's behavior
import java.io.IOException  // line 15: executes this statement as part of this file's behavior
import kotlinx.coroutines.Dispatchers  // line 16: executes this statement as part of this file's behavior
import kotlinx.coroutines.withContext  // line 17: executes this statement as part of this file's behavior

class DashboardRepository(  // line 19: executes this statement as part of this file's behavior
    private val apiClient: BotBladeApiClient = BotBladeApiClient(),  // line 20: executes this statement as part of this file's behavior
) {  // line 21: executes this statement as part of this file's behavior
    suspend fun getBotStatus(projectId: String?): ApiResult<RuntimeStatusResponse> = withContext(Dispatchers.IO) {  // line 22: executes this statement as part of this file's behavior
        runCatching {  // line 23: executes this statement as part of this file's behavior
            if (projectId != null) {  // line 24: executes this statement as part of this file's behavior
                apiClient.getProjectRuntimeStatus(projectId)  // line 25: executes this statement as part of this file's behavior
            } else {  // line 26: executes this statement as part of this file's behavior
                val legacy = apiClient.getBotStatus()  // line 27: executes this statement as part of this file's behavior
                RuntimeStatusResponse(projectId = "legacy", status = legacy.status, message = legacy.message)  // line 28: executes this statement as part of this file's behavior
            }  // line 29: executes this statement as part of this file's behavior
        }.fold(  // line 30: executes this statement as part of this file's behavior
            onSuccess = { ApiResult.Success(it) },  // line 31: executes this statement as part of this file's behavior
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },  // line 32: executes this statement as part of this file's behavior
        )  // line 33: executes this statement as part of this file's behavior
    }  // line 34: executes this statement as part of this file's behavior

    suspend fun runtimeAction(projectId: String?, action: String, currentStatus: String? = null): ApiResult<RuntimeStatusResponse> = withContext(Dispatchers.IO) {  // line 36: executes this statement as part of this file's behavior
        runCatching {  // line 37: executes this statement as part of this file's behavior
            if (projectId != null) {  // line 38: executes this statement as part of this file's behavior
                apiClient.runtimeAction(projectId, action)  // line 39: executes this statement as part of this file's behavior
            } else {  // line 40: executes this statement as part of this file's behavior
                val legacyAction = if (action == "stop" || currentStatus.equals("running", ignoreCase = true)) "stop" else "start"  // line 41: executes this statement as part of this file's behavior
                val response = apiClient.toggleBot(BotToggleRequest(action = legacyAction))  // line 42: executes this statement as part of this file's behavior
                RuntimeStatusResponse(projectId = "legacy", status = response.status ?: "unknown", message = response.message)  // line 43: executes this statement as part of this file's behavior
            }  // line 44: executes this statement as part of this file's behavior
        }.fold(  // line 45: executes this statement as part of this file's behavior
            onSuccess = { ApiResult.Success(it) },  // line 46: executes this statement as part of this file's behavior
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },  // line 47: executes this statement as part of this file's behavior
        )  // line 48: executes this statement as part of this file's behavior
    }  // line 49: executes this statement as part of this file's behavior

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {  // line 51: executes this statement as part of this file's behavior
        is IOException -> "Unable to reach the local botBlade backend. Confirm it is running and reachable."  // line 52: executes this statement as part of this file's behavior
        else -> throwable.message ?: "Unexpected dashboard error."  // line 53: executes this statement as part of this file's behavior
    }  // line 54: executes this statement as part of this file's behavior
}  // line 55: executes this statement as part of this file's behavior
