package com.princess.botblade.releases

import android.content.Context
import com.princess.botblade.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class AppUpgradeChecker(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun latestReleaseForChannel(): AppUpgradeInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/princessraven/botBlade/releases")
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub release check failed with HTTP ${response.code}.")
            }

            val releases = JSONArray(response.body?.string().orEmpty())
            for (index in 0 until releases.length()) {
                val release = releases.optJSONObject(index) ?: continue
                if (release.optBoolean("draft", false)) continue
                if (!releaseMatchesCurrentChannel(release)) continue

                val asset = firstInstallableApkAsset(release) ?: continue
                return@withContext AppUpgradeInfo(
                    tagName = release.optString("tag_name"),
                    pageUrl = release.optString("html_url"),
                    assetUrl = asset.optString("browser_download_url"),
                    assetName = asset.optString("name"),
                )
            }

            null
        }
    }

    suspend fun checkLatestRelease(): AppUpgradeInfo? = withContext(Dispatchers.IO) {
        val latest = latestReleaseForChannel() ?: return@withContext null
        if (versionRank(latest.tagName) > versionRank(BuildConfig.VERSION_NAME)) latest else null
    }


    fun autoCheckEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_CHECK, true)

    fun setAutoCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK, enabled).apply()
    }

    fun shouldCheckNow(): Boolean {
        if (!autoCheckEnabled()) return false
        val lastCheckedAt = prefs.getLong(KEY_LAST_CHECKED_AT, 0L)
        return System.currentTimeMillis() - lastCheckedAt > CHECK_INTERVAL_MS
    }

    fun markChecked() {
        prefs.edit().putLong(KEY_LAST_CHECKED_AT, System.currentTimeMillis()).apply()
    }

    private val prefs by lazy {
        context.applicationContext.getSharedPreferences("botblade_upgrade_prefs", Context.MODE_PRIVATE)
    }

    private fun firstInstallableApkAsset(release: JSONObject): JSONObject? {
        val assets = release.optJSONArray("assets") ?: return null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            if (name.endsWith(".apk") && assetMatchesCurrentChannel(name)) return asset
        }
        return null
    }

    private fun assetMatchesCurrentChannel(assetName: String): Boolean {
        val packageName = BuildConfig.APPLICATION_ID
        val name = assetName.lowercase()
        return when {
            packageName.endsWith(".ci") -> name.contains("-ci-") || name.contains("-ci.")
            packageName.endsWith(".debug") -> name.contains("-debug-") || name.contains("-debug.")
            packageName.contains(".localdev") -> name.contains("local-dev") || name.contains("localdev")
            else -> !name.contains("-ci-") &&
                !name.contains("-ci.") &&
                !name.contains("-debug-") &&
                !name.contains("-debug.") &&
                !name.contains("local-dev") &&
                !name.contains("localdev")
        }
    }

    private fun versionRank(value: String): Int {
        val normalized = value.trim().removePrefix("v")
        val parts = normalized.split(".")
        if (parts.size < 2) return 0

        val major = parts[0].toIntOrNull() ?: return 0
        val minorDigits = buildString {
            for (ch in parts[1]) {
                if (ch.isDigit()) append(ch) else break
            }
        }
        val minor = minorDigits.toIntOrNull() ?: 0
        return major * 100000 + minor
    }

    private fun releaseMatchesCurrentChannel(release: JSONObject): Boolean {
        val packageName = BuildConfig.APPLICATION_ID
        val isPrerelease = release.optBoolean("prerelease", false)
        return when {
            packageName.endsWith(".ci") -> true
            packageName.endsWith(".debug") -> isPrerelease
            packageName.contains(".localdev") -> isPrerelease
            else -> !isPrerelease
        }
    }

    private companion object {
        const val KEY_AUTO_CHECK = "auto_check_enabled"
        const val KEY_LAST_CHECKED_AT = "last_checked_at"
        const val CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L
    }
}
