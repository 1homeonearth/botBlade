package com.princess.royalscepter.data.repository

import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.api.RoyalScepterApiClient
import com.princess.royalscepter.data.model.BotStatusResponse
import com.princess.royalscepter.data.model.BotToggleRequest
import com.princess.royalscepter.data.model.BotToggleResponse
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DashboardRepository(
    private val apiClient: RoyalScepterApiClient = RoyalScepterApiClient(),
) {
    suspend fun getBotStatus(): ApiResult<BotStatusResponse> = withContext(Dispatchers.IO) {
        runCatching { apiClient.getBotStatus() }
            .fold(
                onSuccess = { ApiResult.Success(it) },
                onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },
            )
    }

    suspend fun toggleBot(currentStatus: String?): ApiResult<BotToggleResponse> = withContext(Dispatchers.IO) {
        val action = if (currentStatus.equals("running", ignoreCase = true) ||
            currentStatus.equals("started", ignoreCase = true) ||
            currentStatus.equals("online", ignoreCase = true)
        ) {
            "stop"
        } else {
            "start"
        }

        runCatching { apiClient.toggleBot(BotToggleRequest(action = action)) }
            .fold(
                onSuccess = { ApiResult.Success(it) },
                onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },
            )
    }

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {
        is IOException -> "Unable to reach the local royalScepter backend. Confirm it is running and reachable."
        else -> throwable.message ?: "Unexpected dashboard error."
    }
}
