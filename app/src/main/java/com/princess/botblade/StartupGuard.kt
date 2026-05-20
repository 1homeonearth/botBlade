package com.princess.botblade

import android.app.Application
import android.content.Context
import org.json.JSONObject
import java.io.File
import java.time.Instant

class StartupGuard(private val application: Application) {
    data class State(
        val startupAttempts: Int,
        val consecutiveEarlyCrashes: Int,
        val safeModeEnabled: Boolean,
        val safeModeReason: String?
    )

    fun beginStartup(): State {
        val prefs = prefs()
        val attempts = prefs.getInt(KEY_STARTUP_ATTEMPTS, 0) + 1
        val crashes = prefs.getInt(KEY_CONSECUTIVE_EARLY_CRASHES, 0)
        prefs.edit().putInt(KEY_STARTUP_ATTEMPTS, attempts).apply()
        return State(attempts, crashes, crashes >= SAFE_MODE_THRESHOLD, if (crashes >= SAFE_MODE_THRESHOLD) "crash_loop" else null)
    }

    fun recordStartupSuccess() {
        val now = Instant.now().toString()
        prefs().edit()
            .putString(KEY_LAST_SUCCESSFUL_STARTUP_UTC, now)
            .putInt(KEY_CONSECUTIVE_EARLY_CRASHES, 0)
            .apply()
        appendTelemetry("safe_mode_exit", JSONObject().put("timestampUtc", now).put("reason", "startup_complete"))
    }

    fun recordEarlyCrash(milestone: String?) {
        val prefs = prefs()
        val crashes = prefs.getInt(KEY_CONSECUTIVE_EARLY_CRASHES, 0) + 1
        prefs.edit().putInt(KEY_CONSECUTIVE_EARLY_CRASHES, crashes).apply()
        if (crashes >= SAFE_MODE_THRESHOLD) {
            appendTelemetry("safe_mode_entry", JSONObject().put("consecutiveEarlyCrashes", crashes).put("milestone", milestone ?: "unknown"))
        }
    }

    fun isSafeModeEnabled(): Boolean = prefs().getInt(KEY_CONSECUTIVE_EARLY_CRASHES, 0) >= SAFE_MODE_THRESHOLD

    fun clearRuntimeCache() { File(application.filesDir, "backend").deleteRecursively() }

    fun resetIntegrationState() {
        prefs().edit().remove(KEY_CONSECUTIVE_EARLY_CRASHES).putInt(KEY_CONSECUTIVE_EARLY_CRASHES, 0).apply()
    }

    fun exportLogs(): File {
        val src = File(application.filesDir, "logs/engine.log")
        val dst = File(application.filesDir, "logs/startup-safe-mode-export.log")
        dst.parentFile?.mkdirs()
        if (src.exists()) dst.writeText(src.readText()) else dst.writeText("No runtime logs were available at export time.")
        return dst
    }

    private fun appendTelemetry(eventName: String, payload: JSONObject) {
        val file = File(application.filesDir, TELEMETRY_FILE)
        val base = if (file.exists()) file.readText() else ""
        file.writeText(base + JSONObject().put("event", eventName).put("payload", payload).put("timestampUtc", Instant.now().toString()).toString() + "\n")
    }

    private fun prefs() = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "startup_guard"
        private const val KEY_STARTUP_ATTEMPTS = "startup_attempts"
        private const val KEY_CONSECUTIVE_EARLY_CRASHES = "consecutive_early_crashes"
        private const val KEY_LAST_SUCCESSFUL_STARTUP_UTC = "last_successful_startup_utc"
        private const val SAFE_MODE_THRESHOLD = 3
        private const val TELEMETRY_FILE = "safe_mode_telemetry.jsonl"
    }
}
