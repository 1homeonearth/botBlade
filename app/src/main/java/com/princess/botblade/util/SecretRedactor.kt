package com.princess.botblade.util

object SecretRedactor {
    private const val REDACTION = "••••••••"
    private val sensitiveKeyPattern = Regex(
        pattern = "(?i)(token|secret|password|api[_-]?key|authorization|auth)",
    )

    fun redact(value: String?): String = if (value.isNullOrBlank()) "" else REDACTION

    fun redactMap(values: Map<String, String?>): Map<String, String> = values.mapValues { (key, value) ->
        if (sensitiveKeyPattern.containsMatchIn(key)) redact(value) else value.orEmpty()
    }
}
