// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.util  // line 7: executes this statement as part of this file's behavior

object SecretRedactor {  // line 9: executes this statement as part of this file's behavior
    private const val REDACTION = "[REDACTED]"  // line 10: executes this statement as part of this file's behavior
    private val sensitiveKeyPattern = Regex("""(?i)(token|secret|password|api[_-]?key|authorization|auth)""")  // line 11: executes this statement as part of this file's behavior
    private val tokenLikePattern = Regex("""[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{6,}\.[A-Za-z0-9_-]{10,}""")  // line 12: executes this statement as part of this file's behavior
    private val assignmentPattern = Regex("""(?i)(token|secret|password|api[_-]?key|authorization|auth)\s*[:=]\s*([^\s,;]+)""")  // line 13: executes this statement as part of this file's behavior

    fun redact(value: String?): String {  // line 15: executes this statement as part of this file's behavior
        if (value.isNullOrBlank()) return ""  // line 16: executes this statement as part of this file's behavior
        var output = value  // line 17: executes this statement as part of this file's behavior
        output = tokenLikePattern.replace(output, REDACTION)  // line 18: executes this statement as part of this file's behavior
        output = assignmentPattern.replace(output) { match -> "${match.groupValues[1]}=${REDACTION}" }  // line 19: executes this statement as part of this file's behavior
        return output  // line 20: executes this statement as part of this file's behavior
    }  // line 21: executes this statement as part of this file's behavior

    fun redactMap(values: Map<String, String?>): Map<String, String> = values.mapValues { (key, value) ->  // line 23: executes this statement as part of this file's behavior
        if (sensitiveKeyPattern.containsMatchIn(key)) REDACTION else redact(value)  // line 24: executes this statement as part of this file's behavior
    }  // line 25: executes this statement as part of this file's behavior
}  // line 26: executes this statement as part of this file's behavior
