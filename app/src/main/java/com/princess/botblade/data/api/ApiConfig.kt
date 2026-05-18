package com.princess.botblade.data.api

import android.content.Context
import android.content.SharedPreferences
import com.princess.botblade.BuildConfig
import java.net.URI

object ApiConfig {
    private const val PREFERENCES_NAME = "botblade_api_config"
    private const val BACKEND_URL_KEY = "backend_url"

    val DEFAULT_BASE_URL: String = normalizeUrl(BuildConfig.API_BASE_URL)
    const val DEFAULT_BEARER_TOKEN = ""
    const val DEFAULT_SESSION_TOKEN = ""

    private var preferences: SharedPreferences? = null

    val baseUrl: String
        get() = preferences
            ?.getString(BACKEND_URL_KEY, null)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_BASE_URL

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun saveBaseUrl(url: String): String {
        val normalizedUrl = normalizeUrl(url)
        preferences?.edit()?.putString(BACKEND_URL_KEY, normalizedUrl)?.apply()
        return normalizedUrl
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
