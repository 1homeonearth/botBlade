package com.princess.botblade.data.settings

import java.net.URI

object SettingsValidation {
    private val proxyBackedRoutes = setOf(
        RouteProfile.SOCKS5_PROXY,
        RouteProfile.HTTP_PROXY,
        RouteProfile.TOR_ORBOT,
    )

    fun validateBackendHost(host: String): String? {
        val trimmed = host.trim()
        if (trimmed.isBlank()) return "Backend host is required."
        if (trimmed.any { it.isWhitespace() }) return "Backend host cannot contain spaces."
        if (trimmed.any { it == '/' || it == '?' || it == '#' }) return "Backend host cannot contain a path, query, or fragment."
        return null
    }

    fun validatePort(portText: String): String? {
        val port = portText.trim().toIntOrNull() ?: return "Port must be a number."
        if (port !in 1..65_535) return "Port must be between 1 and 65535."
        return null
    }

    fun validateProxy(routeProfile: RouteProfile, host: String, portText: String): String? {
        if (routeProfile !in proxyBackedRoutes) return null
        val trimmedHost = host.trim()
        if (trimmedHost.isBlank()) return "Proxy host is required for ${routeProfile.label}."
        if (trimmedHost.any { it.isWhitespace() }) return "Proxy host cannot contain spaces."
        return validatePort(portText)
    }

    fun buildBaseUrl(host: String, portText: String, useHttps: Boolean = false): String {
        validateBackendHost(host)?.let { throw IllegalArgumentException(it) }
        validatePort(portText)?.let { throw IllegalArgumentException(it) }
        val scheme = if (useHttps) "https" else "http"
        return "$scheme://${host.trim()}:${portText.trim().toInt()}"
    }

    fun hostFromBaseUrl(baseUrl: String): String = runCatching {
        URI(baseUrl.trim()).host.orEmpty()
    }.getOrDefault("")

    fun portFromBaseUrl(baseUrl: String, fallback: Int = 8000): String = runCatching {
        val uri = URI(baseUrl.trim())
        val explicitPort = uri.port
        when {
            explicitPort > 0 -> explicitPort
            uri.scheme == "https" -> 443
            uri.scheme == "http" -> 80
            else -> fallback
        }.toString()
    }.getOrDefault(fallback.toString())

    fun usesHttps(baseUrl: String): Boolean = runCatching {
        URI(baseUrl.trim()).scheme.equals("https", ignoreCase = true)
    }.getOrDefault(false)

    fun isLoopbackHost(host: String): Boolean {
        val normalized = host.trim().lowercase()
        return normalized == "localhost" ||
            normalized == "127.0.0.1" ||
            normalized == "::1" ||
            normalized.startsWith("127.")
    }
}

enum class RouteProfile(val label: String, val detail: String) {
    DIRECT("Direct", "Use the configured backend route without a proxy."),
    SYSTEM_PROXY("System proxy", "Respect Android or network-level proxy behavior where supported."),
    SOCKS5_PROXY("SOCKS5 proxy", "Save a SOCKS5 route profile for backend/runtime plumbing."),
    HTTP_PROXY("HTTP proxy", "Save an HTTP proxy route profile for backend/runtime plumbing."),
    TOR_ORBOT("Tor / Orbot", "Save an Orbot-style SOCKS route profile; runtime routing support is explicit and separate."),
}
