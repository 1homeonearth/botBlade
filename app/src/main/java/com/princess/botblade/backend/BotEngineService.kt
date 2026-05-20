package com.princess.botblade.backend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.princess.botblade.BuildConfig
import com.princess.botblade.MainActivity
import com.princess.botblade.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.ArrayDeque

class BotEngineService : Service() {
    inner class LocalBinder : Binder() {
        fun isRunning(): Boolean = isRunning
        fun stopEngine() {
            userInitiatedStop = true
            stopNode()
        }
        fun restartEngine() {
            userInitiatedStop = true
            stopNode()
            userInitiatedStop = false
            startNode()
        }
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val crashTimestamps = ArrayDeque<Long>()
    private var monitorJob: Job? = null
    private var userInitiatedStop = false
    private var isRunning = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(active = true))
        acquireLocks()
        userInitiatedStop = false
        startNode()
        return START_STICKY
    }

    override fun onDestroy() {
        userInitiatedStop = true
        stopNode()
        releaseLocks()
        super.onDestroy()
    }

    private fun startNode() {
        val entry = prepareBackendEntry()
        entry // keep asset preparation side effects
        // TODO(on-device-runtime): Replace with working Node.js runtime library.
        // LiquidCore 0.6.2 does not exist on JitPack. Evaluate:
        // - J2V8 (eclipsesource/j2v8) - V8 bindings, no full Node.js
        // - Capacitor/Ionic approach - WebView-based JS execution
        // - Chaquopy - Python alternative if bot logic is rewritten
        // - Self-hosted Node.js binary bundled as an asset (advanced)
        android.util.Log.w("BotEngineService", "Node.js runtime not yet available — LiquidCore removed pending replacement")
        isRunning = false
        notifyState(true)
        startProcessMonitor()
    }

    private fun stopNode() {
        monitorJob?.cancel()
        monitorJob = null
        // TODO(on-device-runtime): Replace with working Node.js runtime library.
        // LiquidCore 0.6.2 does not exist on JitPack. Evaluate:
        // - J2V8 (eclipsesource/j2v8) - V8 bindings, no full Node.js
        // - Capacitor/Ionic approach - WebView-based JS execution
        // - Chaquopy - Python alternative if bot logic is rewritten
        // - Self-hosted Node.js binary bundled as an asset (advanced)
        android.util.Log.w("BotEngineService", "Node.js runtime not yet available — LiquidCore removed pending replacement")
        isRunning = false
        notifyState(false)
    }

    private fun startProcessMonitor() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (true) {
                delay(1000)
                // TODO(on-device-runtime): Replace with working Node.js runtime library.
                // LiquidCore 0.6.2 does not exist on JitPack. Evaluate:
                // - J2V8 (eclipsesource/j2v8) - V8 bindings, no full Node.js
                // - Capacitor/Ionic approach - WebView-based JS execution
                // - Chaquopy - Python alternative if bot logic is rewritten
                // - Self-hosted Node.js binary bundled as an asset (advanced)
                val running = false
                android.util.Log.w("BotEngineService", "Node.js runtime not yet available — LiquidCore removed pending replacement")
                if (!running && isRunning) {
                    isRunning = false
                    val reason = "On-device Node.js runtime unavailable; service in stub mode."
                    appendLog(reason)
                    if (!userInitiatedStop && shouldRestartAfterCrash()) {
                        delay(5000)
                        startNode()
                    } else if (!userInitiatedStop) {
                        postCrashStopNotification()
                        stopSelf()
                    }
                    return@launch
                }
            }
        }
    }

    private fun shouldRestartAfterCrash(): Boolean {
        val now = System.currentTimeMillis()
        crashTimestamps.addLast(now)
        while (crashTimestamps.isNotEmpty() && now - crashTimestamps.first() > 60_000) crashTimestamps.removeFirst()
        return crashTimestamps.size <= 3
    }

    private fun appendLog(line: String) {
        val logDir = File(filesDir, "logs")
        val logFile = File(logDir, "engine.log")
        logDir.mkdirs()
        val lines = if (logFile.exists()) logFile.readLines().takeLast(499).toMutableList() else mutableListOf()
        lines.add("${System.currentTimeMillis()} $line")
        logFile.writeText(lines.joinToString("\n") + "\n")
    }

    private fun postCrashStopNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("BotBlade stopped unexpectedly — tap to view logs.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(CRASH_NOTIFICATION_ID, notification)
    }

    private fun prepareBackendEntry(): File {
        val targetRoot = File(filesDir, "backend")
        val savedVersion = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_ASSET_VERSION, -1)
        if (!targetRoot.exists() || savedVersion != BuildConfig.VERSION_CODE) {
            copyAssetDirectory("", targetRoot)
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_ASSET_VERSION, BuildConfig.VERSION_CODE).apply()
        }
        return File(targetRoot, "src/server.js")
    }

    private fun copyAssetDirectory(assetPath: String, destination: File) {
        val children = assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            destination.parentFile?.mkdirs()
            assets.open(assetPath).use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
            return
        }
        destination.mkdirs()
        for (child in children) {
            val childAssetPath = if (assetPath.isBlank()) child else "$assetPath/$child"
            copyAssetDirectory(childAssetPath, File(destination, child))
        }
    }

    private fun acquireLocks() { /* unchanged */
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:BotEngineWakeLock").apply { setReferenceCounted(false); acquire() }
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "$packageName:BotEngineWifiLock").apply { setReferenceCounted(false); acquire() }
    }

    private fun releaseLocks() {
        wakeLock?.takeIf { it.isHeld }?.release(); wakeLock = null
        wifiLock?.takeIf { it.isHeld }?.release(); wifiLock = null
    }

    private fun notifyState(active: Boolean) { getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(active)) }

    private fun buildNotification(active: Boolean): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(if (active) "BotBlade is running" else "BotBlade is stopped")
        .setOngoing(true)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID, "Bot Engine", NotificationManager.IMPORTANCE_LOW))
    }

    companion object {
        private const val PREFS_NAME = "bot_engine_service"
        private const val KEY_ASSET_VERSION = "asset_version"
        private const val CHANNEL_ID = "bot_engine"
        private const val NOTIFICATION_ID = 7432
        private const val CRASH_NOTIFICATION_ID = 7433
    }
}
