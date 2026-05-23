// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.api  // line 7: executes this statement as part of this file's behavior

import android.content.Context  // line 9: executes this statement as part of this file's behavior
import java.net.URI  // line 10: executes this statement as part of this file's behavior

object ApiConfig {  // line 12: executes this statement as part of this file's behavior
    val DEFAULT_BASE_URL: String = normalizeUrl(BackendConfig.baseUrl)  // line 13: executes this statement as part of this file's behavior
    const val DEFAULT_BEARER_TOKEN = ""  // line 14: executes this statement as part of this file's behavior
    const val DEFAULT_SESSION_TOKEN = ""  // line 15: executes this statement as part of this file's behavior

    val baseUrl: String  // line 17: executes this statement as part of this file's behavior
        get() = normalizeUrl(BackendConfig.baseUrl)  // line 18: executes this statement as part of this file's behavior

    fun initialize(context: Context) {  // line 20: executes this statement as part of this file's behavior
        context.applicationContext  // line 21: executes this statement as part of this file's behavior
    }  // line 22: executes this statement as part of this file's behavior

    fun saveBaseUrl(url: String): String {  // line 24: executes this statement as part of this file's behavior
        return normalizeUrl(baseUrl)  // line 25: executes this statement as part of this file's behavior
    }  // line 26: executes this statement as part of this file's behavior

    fun validateBaseUrl(url: String): String? = runCatching {  // line 28: executes this statement as part of this file's behavior
        normalizeUrl(url)  // line 29: executes this statement as part of this file's behavior
    }.exceptionOrNull()?.message  // line 30: executes this statement as part of this file's behavior

    fun normalizeUrl(url: String): String {  // line 32: executes this statement as part of this file's behavior
        val trimmed = url.trim()  // line 33: executes this statement as part of this file's behavior
        require(trimmed.isNotBlank()) { "Backend URL is required." }  // line 34: executes this statement as part of this file's behavior
        require(!trimmed.any { it.isWhitespace() }) { "Backend URL cannot contain spaces." }  // line 35: executes this statement as part of this file's behavior

        val uri = runCatching { URI(trimmed) }  // line 37: executes this statement as part of this file's behavior
            .getOrElse { throw IllegalArgumentException("Enter a valid backend URL.") }  // line 38: executes this statement as part of this file's behavior
        require(uri.scheme == "http" || uri.scheme == "https") { "Backend URL must start with http:// or https://." }  // line 39: executes this statement as part of this file's behavior
        require(!uri.host.isNullOrBlank()) { "Backend URL must include a host." }  // line 40: executes this statement as part of this file's behavior
        require(uri.userInfo.isNullOrBlank()) { "Do not include credentials in the backend URL." }  // line 41: executes this statement as part of this file's behavior
        require(uri.rawQuery.isNullOrBlank() && uri.rawFragment.isNullOrBlank()) { "Backend URL cannot include query parameters or fragments." }  // line 42: executes this statement as part of this file's behavior

        return trimmed.trimEnd('/')  // line 44: executes this statement as part of this file's behavior
    }  // line 45: executes this statement as part of this file's behavior
}  // line 46: executes this statement as part of this file's behavior
