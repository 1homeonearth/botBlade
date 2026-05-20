package com.princess.botblade.util

object SecretRedactor {
    private const val REDACTION = "[REDACTED]"
    private val sensitiveKeyPattern = Regex("(?i)(token|secret|password|api[_-]?key|authorization|auth)")
    private val tokenLikePattern = Regex("[A-Za-z0-9_\-]{20,}\.[A-Za-z0-9_\-]{6,}\.[A-Za-z0-9_\-]{10,}")
    private val assignmentPattern = Regex("(?i)(token|secret|password|api[_-]?key|authorization|auth)\\s*[:=]\\s*([^\\s,;]+)")

    fun redact(value: String?): String {
        if (value.isNullOrBlank()) return ""
        var output = value
        output = tokenLikePattern.replace(output, REDACTION)
        output = assignmentPattern.replace(output) { match -> "${match.groupValues[1]}=${REDACTION}" }
        return output
    }

    fun redactMap(values: Map<String, String?>): Map<String, String> = values.mapValues { (key, value) ->
        if (sensitiveKeyPattern.containsMatchIn(key)) REDACTION else redact(value)
    }
}
