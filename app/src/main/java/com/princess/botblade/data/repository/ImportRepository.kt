package com.princess.botblade.data.repository

import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.api.BotBladeApiClient
import com.princess.botblade.data.model.ImportStartRequest
import com.princess.botblade.data.model.ImportSummary
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportRepository(private val apiClient: BotBladeApiClient = BotBladeApiClient()) {
    suspend fun startImport(request: ImportStartRequest): ApiResult<ImportSummary> = apiCall { apiClient.startImport(request) }
    suspend fun pollImport(importId: String): ApiResult<ImportSummary> = apiCall { apiClient.getImport(importId) }

    private suspend fun <T> apiCall(block: () -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        runCatching { block() }.fold(onSuccess = { ApiResult.Success(it) }, onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) })
    }

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {
        is IOException -> throwable.message ?: "Unable to reach the local botBlade backend. Confirm it is running and reachable."
        else -> throwable.message ?: "Unexpected import error."
    }
}
