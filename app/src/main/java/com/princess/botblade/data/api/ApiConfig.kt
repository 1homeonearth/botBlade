package com.princess.botblade.data.api

import android.content.Context
import java.net.URI

object ApiConfig {
    val DEFAULT_BASE_URL: String = normalizeUrl(BackendConfig.baseUrl)
    const val DEFAULT_BEARER_TOKEN = ""
    const val DEFAULT_SESSION_TOKEN = ""

    val baseUrl: String
        get() = normalizeUrl(BackendConfig.baseUrl)

    fun initialize(context: Context) {
        context.applicationContext
    }

    fun saveBaseUrl(url: String): String {
        return normalizeUrl(baseUrl)
    }

    fun validateBaseUrl(url: String): String? = runCatching {
        normalizeUrl(url)
    }.exceptionOrNull()?.message

    fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        require(trimmed.isNotBlank()) { "Backend URL is required." }
        require(!trimmed.any { it.isWhitespace() }) { "Backend URL cannot contain spaces." }

        val uri = runCatching { URI(trimmed) }
            .getOrElse { throw IllegalArgumentException("Enter a valid backend URL.") }
        require(uri.scheme == "http" || uri.scheme == "https") { "Backend URL must start with http:// or https://." }
        require(!uri.host.isNullOrBlank()) { "Backend URL must include a host." }
        require(uri.userInfo.isNullOrBlank()) { "Do not include credentials in the backend URL." }
        require(uri.rawQuery.isNullOrBlank() && uri.rawFragment.isNullOrBlank()) { "Backend URL cannot include query parameters or fragments." }

        return trimmed.trimEnd('/')
    }
}
