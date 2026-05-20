package com.princess.botblade

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.princess.botblade.backend.EnginePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import java.time.Instant

private val Context.startupGuardDataStore by preferencesDataStore(name = "startup_guard")

class StartupGuard(private val application: Application) {
    suspend fun onProcessStart() {
        application.startupGuardDataStore.edit { prefs ->
            val success = prefs[KEY_LAST_STARTUP_SUCCESS] ?: false
            val attempts = prefs[KEY_EARLY_CRASH_ATTEMPTS] ?: 0
            val nextAttempts = computeNextAttemptCount(success, attempts)
            prefs[KEY_EARLY_CRASH_ATTEMPTS] = nextAttempts
            prefs[KEY_LAST_STARTUP_SUCCESS] = false
            prefs[KEY_LAST_ATTEMPT_AT] = System.currentTimeMillis()
            val shouldEnableSafeMode = shouldEnableSafeMode(nextAttempts)
            val currentSafeMode = prefs[KEY_SAFE_MODE] ?: false
            if (shouldEnableSafeMode && !currentSafeMode) {
                prefs[KEY_SAFE_MODE] = true
                appendTelemetry("safe_mode_entry", nextAttempts)
            }
        }
    }

    suspend fun markStartupComplete() {
        application.startupGuardDataStore.edit { prefs ->
            val wasSafeMode = prefs[KEY_SAFE_MODE] ?: false
            prefs[KEY_LAST_STARTUP_SUCCESS] = true
            prefs[KEY_EARLY_CRASH_ATTEMPTS] = 0
            if (wasSafeMode) {
                prefs[KEY_SAFE_MODE] = false
                appendTelemetry("safe_mode_exit", 0)
            }
        }
    }

    fun isSafeModeEnabledBlocking(): Boolean = runBlocking { application.startupGuardDataStore.data.first()[KEY_SAFE_MODE] ?: false }

    suspend fun applySafeModeRuntimePolicies() {
        if (!isSafeModeEnabledBlocking()) return
        EnginePreferences.setTerminalAutoAttach(application, false)
    }

    suspend fun clearRuntimeCache() { File(application.filesDir, "backend").deleteRecursively() }

    suspend fun resetIntegrationState() {
        File(application.filesDir, "startup_crash_artifact.json").delete()
    }

    fun exportLogs(): File {
        val logDir = File(application.filesDir, "logs")
        val output = File(application.filesDir, "safe_mode_logs_export.txt")
        val content = logDir.walkTopDown().filter { it.isFile }.joinToString("\n\n") { file ->
            "# ${file.name}\n${file.readText()}"
        }
        output.writeText(content.ifBlank { "No logs available" })
        return output
    }

    private fun appendTelemetry(event: String, attempts: Int) {
        val file = File(application.filesDir, "safe_mode_telemetry.log")
        val payload = JSONObject()
            .put("timestampUtc", Instant.now().toString())
            .put("event", event)
            .put("earlyCrashAttempts", attempts)
        file.appendText(payload.toString() + "\n")
    }

    companion object {
        internal fun computeNextAttemptCount(lastStartupSucceeded: Boolean, earlyCrashAttempts: Int): Int =
            if (lastStartupSucceeded) 0 else earlyCrashAttempts + 1

        internal fun shouldEnableSafeMode(attempts: Int): Boolean = attempts >= SAFE_MODE_THRESHOLD

        private const val SAFE_MODE_THRESHOLD = 3
        private val KEY_EARLY_CRASH_ATTEMPTS = intPreferencesKey("early_crash_attempts")
        private val KEY_LAST_STARTUP_SUCCESS = booleanPreferencesKey("last_startup_success")
        private val KEY_LAST_ATTEMPT_AT = longPreferencesKey("last_attempt_at")
        private val KEY_SAFE_MODE = booleanPreferencesKey("safe_mode")
    }
}
