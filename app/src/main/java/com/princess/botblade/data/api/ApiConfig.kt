package com.princess.botblade.data.api

import android.content.Context
import java.net.URI

object ApiConfig {
    private const val PREFS_NAME = "botblade_api"
    private const val KEY_BASE_URL = "base_url"

    @Volatile
    private var configuredBaseUrl: String = normalizeUrl(BackendConfig.baseUrl)
    @Volatile
    private var appContext: Context? = null

    val DEFAULT_BASE_URL: String = normalizeUrl(BackendConfig.baseUrl)
    const val DEFAULT_BEARER_TOKEN = ""
    const val DEFAULT_SESSION_TOKEN = ""

    val baseUrl: String
        get() = configuredBaseUrl

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_BASE_URL, null)
        configuredBaseUrl = stored?.let(::normalizeUrl) ?: normalizeUrl(BackendConfig.baseUrl)
    }

    fun saveBaseUrl(url: String): String {
        val normalized = normalizeUrl(url)
        configuredBaseUrl = normalized
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_BASE_URL, normalized)
            ?.apply()
        return normalized
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
