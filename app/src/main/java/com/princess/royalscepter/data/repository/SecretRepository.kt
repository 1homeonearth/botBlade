package com.princess.royalscepter.data.repository

import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.api.RoyalScepterApiClient
import com.princess.royalscepter.data.model.SecretCreateRequest
import com.princess.royalscepter.data.model.SecretSummary
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecretRepository(
    private val apiClient: RoyalScepterApiClient = RoyalScepterApiClient(),
) {
    suspend fun listSecrets(): ApiResult<List<SecretSummary>> = apiCall { apiClient.listSecrets() }
    suspend fun createSecret(request: SecretCreateRequest): ApiResult<SecretSummary> = apiCall { apiClient.createSecret(request) }
    suspend fun rotateSecret(secretId: String, value: String): ApiResult<SecretSummary> = apiCall { apiClient.rotateSecret(secretId, value) }
    suspend fun deleteSecret(secretId: String): ApiResult<Unit> = apiCall { apiClient.deleteSecret(secretId) }

    private suspend fun <T> apiCall(block: () -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        runCatching { block() }.fold(
            onSuccess = { ApiResult.Success(it) },
            onFailure = { ApiResult.Error(message = disconnectedMessage(it), cause = it) },
        )
    }

    private fun disconnectedMessage(throwable: Throwable): String = when (throwable) {
        is IOException -> throwable.message ?: "Unable to reach the local royalScepter backend."
        else -> throwable.message ?: "Unexpected secret error."
    }
}
