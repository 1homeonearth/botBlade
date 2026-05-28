package com.princess.botblade.diagnostics

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

class DownloadsLogMirror(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    fun start() {
        if (syncJob?.isActive == true) return
        captureNow("app_started")
        syncJob = scope.launch {
            while (isActive) {
                delay(FIVE_MINUTES_MS)
                captureNow("scheduled_5min")
            }
        }
    }

    fun captureNow(reason: String) {
        scope.launch {
            runCatching { mirrorNow(reason) }
        }
    }

    fun stop() {
        captureNow("app_stopped")
        syncJob?.cancel()
        syncJob = null
        scope.cancel()
    }

    private fun mirrorNow(reason: String) {
        val payload = buildPayload(reason)
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val existingId = resolver.query(collection, arrayOf(MediaStore.Downloads._ID), "${MediaStore.Downloads.DISPLAY_NAME}=?", arrayOf(FILE_NAME), null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

        val targetUri = if (existingId == null) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            resolver.insert(collection, values) ?: return
        } else ContentUris.withAppendedId(collection, existingId)

        resolver.openOutputStream(targetUri, "wt")?.bufferedWriter()?.use { it.write(payload) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) resolver.update(targetUri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
    }

    private fun buildPayload(reason: String): String {
        val logcat = runCatching {
            ProcessBuilder("logcat", "-d", "-v", "threadtime", "-T", "5m").start().inputStream.bufferedReader().use { it.readText() }
        }.getOrElse { "logcat_unavailable: ${it.message}" }

        return buildString {
            appendLine("BotBlade app log snapshot")
            appendLine("updatedAtUtc=${Instant.now()}")
            appendLine("reason=$reason")
            appendLine("note=Android app logcat snapshot written automatically to Downloads")
            appendLine()
            appendLine(logcat)
        }
    }

    companion object {
        private const val FILE_NAME = "botblade-app-logcat.txt"
        private const val FIVE_MINUTES_MS = 5 * 60 * 1000L
    }
}
