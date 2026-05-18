package com.princess.royalscepter.data.api

import com.princess.royalscepter.data.model.ApiErrorResponse
import com.princess.royalscepter.data.model.BotStatusResponse
import com.princess.royalscepter.data.model.BotToggleRequest
import com.princess.royalscepter.data.model.BotToggleResponse
import com.princess.royalscepter.data.model.HealthResponse
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class RoyalScepterApiClient(
    private val baseUrl: String = ApiConfig.DEFAULT_BASE_URL,
) {
    @Throws(IOException::class)
    fun getHealth(): HealthResponse {
        val body = request(path = "/api/health", method = "GET")
        val json = body.asJsonOrNull()
        return HealthResponse(
            status = json?.optionalString("status") ?: body.ifBlank { "unknown" },
            message = json?.optionalString("message"),
        )
    }

    @Throws(IOException::class)
    fun getBotStatus(): BotStatusResponse {
        val body = request(path = "/api/bot-status/", method = "GET")
        val json = body.asJsonOrNull()
        return BotStatusResponse(
            status = json?.optionalString("status")
                ?: json?.optionalString("bot_status")
                ?: json?.optionalString("state")
                ?: body.normalizePlainStatus(),
            message = json?.optionalString("message"),
        )
    }

    @Throws(IOException::class)
    fun toggleBot(request: BotToggleRequest): BotToggleResponse {
        val payload = JSONObject().put("action", request.action).toString()
        val body = request(path = "/api/bot-toggle/", method = "POST", requestBody = payload)
        val json = body.asJsonOrNull()
        return BotToggleResponse(
            status = json?.optionalString("status")
                ?: json?.optionalString("bot_status")
                ?: body.normalizePlainStatus().takeIf { json == null },
            action = json?.optionalString("action") ?: request.action,
            message = json?.optionalString("message") ?: body.takeIf { it.isNotBlank() && json == null },
        )
    }

    @Throws(IOException::class)
    private fun request(
        path: String,
        method: String,
        requestBody: String? = null,
    ): String {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 2_000
        connection.readTimeout = 2_000
        connection.doInput = true
        connection.setRequestProperty("Accept", "application/json")

        if (requestBody != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { output -> output.write(requestBody.toByteArray()) }
        }

        return connection.use { http ->
            val responseCode = http.responseCode
            val responseBody = (if (responseCode in 200..299) http.inputStream else http.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (responseCode !in 200..299) {
                val error = parseError(responseBody, responseCode)
                throw IOException(error.message ?: error.error ?: "HTTP $responseCode")
            }

            responseBody
        }
    }

    private fun parseError(body: String, statusCode: Int): ApiErrorResponse {
        val json = body.asJsonOrNull()
        return ApiErrorResponse(
            error = json?.optionalString("error"),
            message = json?.optionalString("message"),
            statusCode = statusCode,
        )
    }

    private fun String.asJsonOrNull(): JSONObject? = runCatching { JSONObject(this) }.getOrNull()

    private fun JSONObject.optionalString(name: String): String? = if (has(name) && !isNull(name)) {
        optString(name).takeIf { it.isNotBlank() }
    } else {
        null
    }

    private fun String.normalizePlainStatus(): String = replace(Regex("[{}\\\"]"), "")
        .replace(',', ' ')
        .trim()
        .ifBlank { "Unknown" }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T = try {
        block(this)
    } finally {
        disconnect()
    }
}
