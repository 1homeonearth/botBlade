package com.princess.botblade

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import com.princess.botblade.util.SecretRedactor
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

object StartupDiagnostics {
    private const val ARTIFACT_FILE = "startup_crash_artifact.json"
    private val latestMilestone = AtomicReference("process_start")

    fun install(application: Application, appVersion: String, buildType: String, gitSha: String?) {
        mark("application_on_create_start")
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { mark("first_activity_launch") }
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeArtifact(application, thread, throwable, appVersion, buildType, gitSha) }
            previous?.uncaughtException(thread, throwable) ?: throw throwable
        }
    }

    fun mark(milestone: String) { latestMilestone.set("${milestone}@${Instant.now()}") }

    fun readLatest(application: Application): String? {
        val file = File(application.filesDir, ARTIFACT_FILE)
        return if (file.exists()) file.readText() else null
    }

    internal fun writeArtifactForTest(application: Application, throwable: Throwable) {
        writeArtifact(application, Thread.currentThread(), throwable, "test", "debug", "test")
    }

    private fun writeArtifact(application: Application, thread: Thread, throwable: Throwable, appVersion: String, buildType: String, gitSha: String?) {
        val artifact = JSONObject()
            .put("timestampUtc", Instant.now().toString())
            .put("appVersion", appVersion)
            .put("buildType", buildType)
            .put("gitSha", gitSha ?: "unknown")
            .put("deviceModel", SecretRedactor.redact(Build.MODEL ?: "unknown"))
            .put("androidVersion", Build.VERSION.RELEASE ?: "unknown")
            .put("thread", SecretRedactor.redact(thread.name))
            .put("exceptionClass", throwable::class.java.name)
            .put("sanitizedStacktrace", SecretRedactor.redact(throwable.stackTraceAsString()))
            .put("startupMilestoneReached", latestMilestone.get())
        File(application.filesDir, ARTIFACT_FILE).writeText(artifact.toString(2))
    }

    private fun Throwable.stackTraceAsString(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }
}
