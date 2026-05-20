package com.princess.botblade.backend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.princess.botblade.R
import org.liquidplayer.nodejs.NodeJS
import java.io.File

class BotEngineService : Service() {
    inner class LocalBinder : Binder() {
        fun isRunning(): Boolean = isRunning
        fun stopEngine() = stopNode()
        fun restartEngine() {
            stopNode()
            startNode()
        }
    }

    private val binder = LocalBinder()
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
        startNode()
        return START_STICKY
    }

    override fun onDestroy() {
        stopNode()
        releaseLocks()
        super.onDestroy()
    }

    private fun startNode() {
        val entry = prepareBackendEntry()
        NodeJS.start(entry.absolutePath)
        isRunning = true
        notifyState(true)
    }

    private fun stopNode() {
        if (isRunning) {
            NodeJS.stop()
        }
        isRunning = false
        notifyState(false)
    }

    private fun prepareBackendEntry(): File {
        val targetRoot = File(filesDir, "backend")
        val savedVersion = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_ASSET_VERSION, -1)
        if (!targetRoot.exists() || savedVersion != BuildConfig.VERSION_CODE) {
            copyAssetDirectory("", targetRoot)
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_ASSET_VERSION, BuildConfig.VERSION_CODE)
                .apply()
        }
        return File(targetRoot, "src/server.js")
    }

    private fun copyAssetDirectory(assetPath: String, destination: File) {
        val children = assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            destination.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }
        destination.mkdirs()
        for (child in children) {
            val childAssetPath = if (assetPath.isBlank()) child else "$assetPath/$child"
            val childDestination = File(destination, child)
            copyAssetDirectory(childAssetPath, childDestination)
        }
    }

    private fun acquireLocks() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:BotEngineWakeLock").apply {
            setReferenceCounted(false)
            acquire()
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "$packageName:BotEngineWifiLock").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseLocks() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        wifiLock?.takeIf { it.isHeld }?.release()
        wifiLock = null
    }

    private fun notifyState(active: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(active))
    }

    private fun buildNotification(active: Boolean): Notification {
        val content = if (active) "BotBlade is running" else "BotBlade is stopped"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Bot Engine", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val PREFS_NAME = "bot_engine_service"
        private const val KEY_ASSET_VERSION = "asset_version"
        private const val CHANNEL_ID = "bot_engine"
        private const val NOTIFICATION_ID = 7432
    }
}
