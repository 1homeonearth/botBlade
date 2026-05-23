// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade  // line 7: executes this statement as part of this file's behavior

import android.app.Activity  // line 9: executes this statement as part of this file's behavior
import android.app.Application  // line 10: executes this statement as part of this file's behavior
import android.os.Build  // line 11: executes this statement as part of this file's behavior
import android.os.Bundle  // line 12: executes this statement as part of this file's behavior
import com.princess.botblade.util.SecretRedactor  // line 13: executes this statement as part of this file's behavior
import org.json.JSONObject  // line 14: executes this statement as part of this file's behavior
import java.io.File  // line 15: executes this statement as part of this file's behavior
import java.io.PrintWriter  // line 16: executes this statement as part of this file's behavior
import java.io.StringWriter  // line 17: executes this statement as part of this file's behavior
import java.time.Instant  // line 18: executes this statement as part of this file's behavior
import java.util.concurrent.atomic.AtomicReference  // line 19: executes this statement as part of this file's behavior

object StartupDiagnostics {  // line 21: executes this statement as part of this file's behavior
    private const val ARTIFACT_FILE = "startup_crash_artifact.json"  // line 22: executes this statement as part of this file's behavior
    private val latestMilestone = AtomicReference("process_start")  // line 23: executes this statement as part of this file's behavior

    fun install(application: Application, appVersion: String, buildType: String, gitSha: String?) {  // line 25: executes this statement as part of this file's behavior
        mark("application_on_create_start")  // line 26: executes this statement as part of this file's behavior
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {  // line 27: executes this statement as part of this file's behavior
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { mark("first_activity_launch") }  // line 28: executes this statement as part of this file's behavior
            override fun onActivityStarted(activity: Activity) = Unit  // line 29: executes this statement as part of this file's behavior
            override fun onActivityResumed(activity: Activity) = Unit  // line 30: executes this statement as part of this file's behavior
            override fun onActivityPaused(activity: Activity) = Unit  // line 31: executes this statement as part of this file's behavior
            override fun onActivityStopped(activity: Activity) = Unit  // line 32: executes this statement as part of this file's behavior
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit  // line 33: executes this statement as part of this file's behavior
            override fun onActivityDestroyed(activity: Activity) = Unit  // line 34: executes this statement as part of this file's behavior
        })  // line 35: executes this statement as part of this file's behavior
        val previous = Thread.getDefaultUncaughtExceptionHandler()  // line 36: executes this statement as part of this file's behavior
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->  // line 37: executes this statement as part of this file's behavior
            runCatching { writeArtifact(application, thread, throwable, appVersion, buildType, gitSha) }  // line 38: executes this statement as part of this file's behavior
            previous?.uncaughtException(thread, throwable) ?: throw throwable  // line 39: executes this statement as part of this file's behavior
        }  // line 40: executes this statement as part of this file's behavior
    }  // line 41: executes this statement as part of this file's behavior

    fun mark(milestone: String) { latestMilestone.set("${milestone}@${Instant.now()}") }  // line 43: executes this statement as part of this file's behavior

    fun readLatest(application: Application): String? {  // line 45: executes this statement as part of this file's behavior
        val file = File(application.filesDir, ARTIFACT_FILE)  // line 46: executes this statement as part of this file's behavior
        return if (file.exists()) file.readText() else null  // line 47: executes this statement as part of this file's behavior
    }  // line 48: executes this statement as part of this file's behavior

    internal fun writeArtifactForTest(application: Application, throwable: Throwable) {  // line 50: executes this statement as part of this file's behavior
        writeArtifact(application, Thread.currentThread(), throwable, "test", "debug", "test")  // line 51: executes this statement as part of this file's behavior
    }  // line 52: executes this statement as part of this file's behavior

    private fun writeArtifact(application: Application, thread: Thread, throwable: Throwable, appVersion: String, buildType: String, gitSha: String?) {  // line 54: executes this statement as part of this file's behavior
        val artifact = JSONObject()  // line 55: executes this statement as part of this file's behavior
            .put("timestampUtc", Instant.now().toString())  // line 56: executes this statement as part of this file's behavior
            .put("appVersion", appVersion)  // line 57: executes this statement as part of this file's behavior
            .put("buildType", buildType)  // line 58: executes this statement as part of this file's behavior
            .put("gitSha", gitSha ?: "unknown")  // line 59: executes this statement as part of this file's behavior
            .put("deviceModel", SecretRedactor.redact(Build.MODEL ?: "unknown"))  // line 60: executes this statement as part of this file's behavior
            .put("androidVersion", Build.VERSION.RELEASE ?: "unknown")  // line 61: executes this statement as part of this file's behavior
            .put("thread", SecretRedactor.redact(thread.name))  // line 62: executes this statement as part of this file's behavior
            .put("exceptionClass", throwable::class.java.name)  // line 63: executes this statement as part of this file's behavior
            .put("sanitizedStacktrace", SecretRedactor.redact(throwable.stackTraceAsString()))  // line 64: executes this statement as part of this file's behavior
            .put("startupMilestoneReached", latestMilestone.get())  // line 65: executes this statement as part of this file's behavior
        File(application.filesDir, ARTIFACT_FILE).writeText(artifact.toString(2))  // line 66: executes this statement as part of this file's behavior
    }  // line 67: executes this statement as part of this file's behavior

    private fun Throwable.stackTraceAsString(): String {  // line 69: executes this statement as part of this file's behavior
        val writer = StringWriter()  // line 70: executes this statement as part of this file's behavior
        printStackTrace(PrintWriter(writer))  // line 71: executes this statement as part of this file's behavior
        return writer.toString()  // line 72: executes this statement as part of this file's behavior
    }  // line 73: executes this statement as part of this file's behavior
}  // line 74: executes this statement as part of this file's behavior
