// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.backend  // line 7: executes this statement as part of this file's behavior

import android.app.Notification  // line 9: executes this statement as part of this file's behavior
import android.app.NotificationChannel  // line 10: executes this statement as part of this file's behavior
import android.app.NotificationManager  // line 11: executes this statement as part of this file's behavior
import android.app.PendingIntent  // line 12: executes this statement as part of this file's behavior
import android.app.Service  // line 13: executes this statement as part of this file's behavior
import android.content.Context  // line 14: executes this statement as part of this file's behavior
import android.content.Intent  // line 15: executes this statement as part of this file's behavior
import android.net.wifi.WifiManager  // line 16: executes this statement as part of this file's behavior
import android.os.Binder  // line 17: executes this statement as part of this file's behavior
import android.os.Build  // line 18: executes this statement as part of this file's behavior
import android.os.IBinder  // line 19: executes this statement as part of this file's behavior
import android.os.PowerManager  // line 20: executes this statement as part of this file's behavior
import androidx.core.app.NotificationCompat  // line 21: executes this statement as part of this file's behavior
import com.princess.botblade.BuildConfig  // line 22: executes this statement as part of this file's behavior
import com.princess.botblade.MainActivity  // line 23: executes this statement as part of this file's behavior
import com.princess.botblade.R  // line 24: executes this statement as part of this file's behavior
import kotlinx.coroutines.CoroutineScope  // line 25: executes this statement as part of this file's behavior
import kotlinx.coroutines.Dispatchers  // line 26: executes this statement as part of this file's behavior
import kotlinx.coroutines.Job  // line 27: executes this statement as part of this file's behavior
import kotlinx.coroutines.delay  // line 28: executes this statement as part of this file's behavior
import kotlinx.coroutines.launch  // line 29: executes this statement as part of this file's behavior
import java.io.File  // line 30: executes this statement as part of this file's behavior
import java.util.ArrayDeque  // line 31: executes this statement as part of this file's behavior

class BotEngineService : Service() {  // line 33: executes this statement as part of this file's behavior
    inner class LocalBinder : Binder() {  // line 34: executes this statement as part of this file's behavior
        fun isRunning(): Boolean = isRunning  // line 35: executes this statement as part of this file's behavior
        fun stopEngine() {  // line 36: executes this statement as part of this file's behavior
            userInitiatedStop = true  // line 37: executes this statement as part of this file's behavior
            stopNode()  // line 38: executes this statement as part of this file's behavior
        }  // line 39: executes this statement as part of this file's behavior
        fun restartEngine() {  // line 40: executes this statement as part of this file's behavior
            userInitiatedStop = true  // line 41: executes this statement as part of this file's behavior
            stopNode()  // line 42: executes this statement as part of this file's behavior
            userInitiatedStop = false  // line 43: executes this statement as part of this file's behavior
            startNode()  // line 44: executes this statement as part of this file's behavior
        }  // line 45: executes this statement as part of this file's behavior
    }  // line 46: executes this statement as part of this file's behavior

    private val binder = LocalBinder()  // line 48: executes this statement as part of this file's behavior
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())  // line 49: executes this statement as part of this file's behavior
    private val crashTimestamps = ArrayDeque<Long>()  // line 50: executes this statement as part of this file's behavior
    private var monitorJob: Job? = null  // line 51: executes this statement as part of this file's behavior
    private var userInitiatedStop = false  // line 52: executes this statement as part of this file's behavior
    private var isRunning = false  // line 53: executes this statement as part of this file's behavior
    private var wakeLock: PowerManager.WakeLock? = null  // line 54: executes this statement as part of this file's behavior
    private var wifiLock: WifiManager.WifiLock? = null  // line 55: executes this statement as part of this file's behavior

    override fun onCreate() {  // line 57: executes this statement as part of this file's behavior
        super.onCreate()  // line 58: executes this statement as part of this file's behavior
        createNotificationChannel()  // line 59: executes this statement as part of this file's behavior
    }  // line 60: executes this statement as part of this file's behavior

    override fun onBind(intent: Intent?): IBinder = binder  // line 62: executes this statement as part of this file's behavior

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {  // line 64: executes this statement as part of this file's behavior
        startForeground(NOTIFICATION_ID, buildNotification(active = true))  // line 65: executes this statement as part of this file's behavior
        acquireLocks()  // line 66: executes this statement as part of this file's behavior
        userInitiatedStop = false  // line 67: executes this statement as part of this file's behavior
        startNode()  // line 68: executes this statement as part of this file's behavior
        return START_STICKY  // line 69: executes this statement as part of this file's behavior
    }  // line 70: executes this statement as part of this file's behavior

    override fun onDestroy() {  // line 72: executes this statement as part of this file's behavior
        userInitiatedStop = true  // line 73: executes this statement as part of this file's behavior
        stopNode()  // line 74: executes this statement as part of this file's behavior
        releaseLocks()  // line 75: executes this statement as part of this file's behavior
        super.onDestroy()  // line 76: executes this statement as part of this file's behavior
    }  // line 77: executes this statement as part of this file's behavior

    private fun startNode() {  // line 79: executes this statement as part of this file's behavior
        val entry = prepareBackendEntry()  // line 80: executes this statement as part of this file's behavior
        entry // keep asset preparation side effects  // line 81: executes this statement as part of this file's behavior
        // TODO(on-device-runtime): LiquidCore 0.6.2 does not exist on JitPack.
        // Replace with a verified Node.js runtime when available
        android.util.Log.w("BotEngineService", "Node.js runtime stub — LiquidCore removed")  // line 84: executes this statement as part of this file's behavior
        isRunning = false  // line 85: executes this statement as part of this file's behavior
        notifyState(true)  // line 86: executes this statement as part of this file's behavior
        startProcessMonitor()  // line 87: executes this statement as part of this file's behavior
    }  // line 88: executes this statement as part of this file's behavior

    private fun stopNode() {  // line 90: executes this statement as part of this file's behavior
        monitorJob?.cancel()  // line 91: executes this statement as part of this file's behavior
        monitorJob = null  // line 92: executes this statement as part of this file's behavior
        // TODO(on-device-runtime): LiquidCore 0.6.2 does not exist on JitPack.
        // Replace with a verified Node.js runtime when available
        android.util.Log.w("BotEngineService", "Node.js runtime stub — LiquidCore removed")  // line 95: executes this statement as part of this file's behavior
        isRunning = false  // line 96: executes this statement as part of this file's behavior
        notifyState(false)  // line 97: executes this statement as part of this file's behavior
    }  // line 98: executes this statement as part of this file's behavior

    private fun startProcessMonitor() {  // line 100: executes this statement as part of this file's behavior
        monitorJob?.cancel()  // line 101: executes this statement as part of this file's behavior
        monitorJob = serviceScope.launch {  // line 102: executes this statement as part of this file's behavior
            while (true) {  // line 103: executes this statement as part of this file's behavior
                delay(1000)  // line 104: executes this statement as part of this file's behavior
                // TODO(on-device-runtime): LiquidCore 0.6.2 does not exist on JitPack.
                // Replace with a verified Node.js runtime when available
                val running = false  // line 107: executes this statement as part of this file's behavior
                android.util.Log.w("BotEngineService", "Node.js runtime stub — LiquidCore removed")  // line 108: executes this statement as part of this file's behavior
                if (!running && isRunning) {  // line 109: executes this statement as part of this file's behavior
                    isRunning = false  // line 110: executes this statement as part of this file's behavior
                    val reason = "On-device Node.js runtime unavailable; service in stub mode."  // line 111: executes this statement as part of this file's behavior
                    appendLog(reason)  // line 112: executes this statement as part of this file's behavior
                    if (!userInitiatedStop && shouldRestartAfterCrash()) {  // line 113: executes this statement as part of this file's behavior
                        delay(5000)  // line 114: executes this statement as part of this file's behavior
                        startNode()  // line 115: executes this statement as part of this file's behavior
                    } else if (!userInitiatedStop) {  // line 116: executes this statement as part of this file's behavior
                        postCrashStopNotification()  // line 117: executes this statement as part of this file's behavior
                        stopSelf()  // line 118: executes this statement as part of this file's behavior
                    }  // line 119: executes this statement as part of this file's behavior
                    return@launch  // line 120: executes this statement as part of this file's behavior
                }  // line 121: executes this statement as part of this file's behavior
            }  // line 122: executes this statement as part of this file's behavior
        }  // line 123: executes this statement as part of this file's behavior
    }  // line 124: executes this statement as part of this file's behavior

    private fun shouldRestartAfterCrash(): Boolean {  // line 126: executes this statement as part of this file's behavior
        val now = System.currentTimeMillis()  // line 127: executes this statement as part of this file's behavior
        crashTimestamps.addLast(now)  // line 128: executes this statement as part of this file's behavior
        while (crashTimestamps.isNotEmpty() && now - crashTimestamps.first() > 60_000) crashTimestamps.removeFirst()  // line 129: executes this statement as part of this file's behavior
        return crashTimestamps.size <= 3  // line 130: executes this statement as part of this file's behavior
    }  // line 131: executes this statement as part of this file's behavior

    private fun appendLog(line: String) {  // line 133: executes this statement as part of this file's behavior
        val logDir = File(filesDir, "logs")  // line 134: executes this statement as part of this file's behavior
        val logFile = File(logDir, "engine.log")  // line 135: executes this statement as part of this file's behavior
        logDir.mkdirs()  // line 136: executes this statement as part of this file's behavior
        val lines = if (logFile.exists()) logFile.readLines().takeLast(499).toMutableList() else mutableListOf()  // line 137: executes this statement as part of this file's behavior
        lines.add("${System.currentTimeMillis()} $line")  // line 138: executes this statement as part of this file's behavior
        logFile.writeText(lines.joinToString("\n") + "\n")  // line 139: executes this statement as part of this file's behavior
    }  // line 140: executes this statement as part of this file's behavior

    private fun postCrashStopNotification() {  // line 142: executes this statement as part of this file's behavior
        val intent = Intent(this, MainActivity::class.java)  // line 143: executes this statement as part of this file's behavior
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)  // line 144: executes this statement as part of this file's behavior
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)  // line 145: executes this statement as part of this file's behavior
            .setSmallIcon(R.mipmap.ic_launcher)  // line 146: executes this statement as part of this file's behavior
            .setContentTitle(getString(R.string.app_name))  // line 147: executes this statement as part of this file's behavior
            .setContentText("BotBlade stopped unexpectedly — tap to view logs.")  // line 148: executes this statement as part of this file's behavior
            .setContentIntent(pendingIntent)  // line 149: executes this statement as part of this file's behavior
            .setAutoCancel(true)  // line 150: executes this statement as part of this file's behavior
            .build()  // line 151: executes this statement as part of this file's behavior
        getSystemService(NotificationManager::class.java).notify(CRASH_NOTIFICATION_ID, notification)  // line 152: executes this statement as part of this file's behavior
    }  // line 153: executes this statement as part of this file's behavior

    private fun prepareBackendEntry(): File {  // line 155: executes this statement as part of this file's behavior
        val targetRoot = File(filesDir, "backend")  // line 156: executes this statement as part of this file's behavior
        val savedVersion = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_ASSET_VERSION, -1)  // line 157: executes this statement as part of this file's behavior
        if (!targetRoot.exists() || savedVersion != BuildConfig.VERSION_CODE) {  // line 158: executes this statement as part of this file's behavior
            copyAssetDirectory("", targetRoot)  // line 159: executes this statement as part of this file's behavior
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_ASSET_VERSION, BuildConfig.VERSION_CODE).apply()  // line 160: executes this statement as part of this file's behavior
        }  // line 161: executes this statement as part of this file's behavior
        return File(targetRoot, "src/server.js")  // line 162: executes this statement as part of this file's behavior
    }  // line 163: executes this statement as part of this file's behavior

    private fun copyAssetDirectory(assetPath: String, destination: File) {  // line 165: executes this statement as part of this file's behavior
        val children = assets.list(assetPath).orEmpty()  // line 166: executes this statement as part of this file's behavior
        if (children.isEmpty()) {  // line 167: executes this statement as part of this file's behavior
            destination.parentFile?.mkdirs()  // line 168: executes this statement as part of this file's behavior
            assets.open(assetPath).use { input -> destination.outputStream().use { output -> input.copyTo(output) } }  // line 169: executes this statement as part of this file's behavior
            return  // line 170: executes this statement as part of this file's behavior
        }  // line 171: executes this statement as part of this file's behavior
        destination.mkdirs()  // line 172: executes this statement as part of this file's behavior
        for (child in children) {  // line 173: executes this statement as part of this file's behavior
            val childAssetPath = if (assetPath.isBlank()) child else "$assetPath/$child"  // line 174: executes this statement as part of this file's behavior
            copyAssetDirectory(childAssetPath, File(destination, child))  // line 175: executes this statement as part of this file's behavior
        }  // line 176: executes this statement as part of this file's behavior
    }  // line 177: executes this statement as part of this file's behavior

    private fun acquireLocks() { /* unchanged */  // line 179: executes this statement as part of this file's behavior
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager  // line 180: executes this statement as part of this file's behavior
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:BotEngineWakeLock").apply { setReferenceCounted(false); acquire() }  // line 181: executes this statement as part of this file's behavior
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager  // line 182: executes this statement as part of this file's behavior
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "$packageName:BotEngineWifiLock").apply { setReferenceCounted(false); acquire() }  // line 183: executes this statement as part of this file's behavior
    }  // line 184: executes this statement as part of this file's behavior

    private fun releaseLocks() {  // line 186: executes this statement as part of this file's behavior
        wakeLock?.takeIf { it.isHeld }?.release(); wakeLock = null  // line 187: executes this statement as part of this file's behavior
        wifiLock?.takeIf { it.isHeld }?.release(); wifiLock = null  // line 188: executes this statement as part of this file's behavior
    }  // line 189: executes this statement as part of this file's behavior

    private fun notifyState(active: Boolean) { getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(active)) }  // line 191: executes this statement as part of this file's behavior

    private fun buildNotification(active: Boolean): Notification = NotificationCompat.Builder(this, CHANNEL_ID)  // line 193: executes this statement as part of this file's behavior
        .setSmallIcon(R.mipmap.ic_launcher)  // line 194: executes this statement as part of this file's behavior
        .setContentTitle(getString(R.string.app_name))  // line 195: executes this statement as part of this file's behavior
        .setContentText(if (active) "BotBlade is running" else "BotBlade is stopped")  // line 196: executes this statement as part of this file's behavior
        .setOngoing(true)  // line 197: executes this statement as part of this file's behavior
        .build()  // line 198: executes this statement as part of this file's behavior

    private fun createNotificationChannel() {  // line 200: executes this statement as part of this file's behavior
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return  // line 201: executes this statement as part of this file's behavior
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID, "Bot Engine", NotificationManager.IMPORTANCE_LOW))  // line 202: executes this statement as part of this file's behavior
    }  // line 203: executes this statement as part of this file's behavior

    companion object {  // line 205: executes this statement as part of this file's behavior
        private const val PREFS_NAME = "bot_engine_service"  // line 206: executes this statement as part of this file's behavior
        private const val KEY_ASSET_VERSION = "asset_version"  // line 207: executes this statement as part of this file's behavior
        private const val CHANNEL_ID = "bot_engine"  // line 208: executes this statement as part of this file's behavior
        private const val NOTIFICATION_ID = 7432  // line 209: executes this statement as part of this file's behavior
        private const val CRASH_NOTIFICATION_ID = 7433  // line 210: executes this statement as part of this file's behavior
    }  // line 211: executes this statement as part of this file's behavior
}  // line 212: executes this statement as part of this file's behavior
