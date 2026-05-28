package com.princess.botblade

import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.princess.botblade.diagnostics.DownloadsLogMirror

class BotBladeApp : Application() {
    private var downloadsLogMirror: DownloadsLogMirror? = null
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("botblade_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("use_dynamic_color", false)) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        StartupDiagnostics.install(this, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE, BuildConfig.GIT_SHA)
        StartupDiagnostics.mark("di_init")
        StartupDiagnostics.mark("application_on_create_end")
        downloadsLogMirror = DownloadsLogMirror(this).also { mirror ->
            mirror.start()
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                mirror.captureNow("app_crashed")
                previous?.uncaughtException(thread, throwable) ?: throw throwable
            }
        }
    }

    override fun onTerminate() {
        downloadsLogMirror?.stop()
        downloadsLogMirror = null
        super.onTerminate()
    }
}
