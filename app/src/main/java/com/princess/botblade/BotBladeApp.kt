package com.princess.botblade

import android.app.Application

class BotBladeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        StartupDiagnostics.install(this, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE, BuildConfig.GIT_SHA)
        StartupDiagnostics.mark("di_init")
        StartupDiagnostics.mark("application_on_create_end")
    }
}
