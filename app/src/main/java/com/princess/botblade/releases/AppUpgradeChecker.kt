package com.princess.botblade.releases

import android.content.Context
import com.princess.botblade.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class AppUpgradeChecker(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun checkLatestRelease(): AppUpgradeInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/1homeonearth/botBlade/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val json = JSONObject(response.body?.string().orEmpty())
            val tag = json.optString("tag_name")
            val pageUrl = json.optString("html_url")
            val assets = json.optJSONArray("assets")
            var assetUrl: String? = null
            var assetName: String? = null
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk")) {
                        assetName = name
                        assetUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }
            if (versionRank(tag) <= versionRank(BuildConfig.VERSION_NAME)) return@withContext null
            AppUpgradeInfo(tagName = tag, pageUrl = pageUrl, assetUrl = assetUrl, assetName = assetName)
        }
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

    private fun versionRank(value: String): Int {
        val match = Regex("(\\d+)\\.(\\d+)").find(value) ?: return 0
        val major = match.groupValues[1].toIntOrNull() ?: 0
        val minor = match.groupValues[2].toIntOrNull() ?: 0
        return major * 100000 + minor
    }

    private companion object {
        const val KEY_AUTO_CHECK = "auto_check_enabled"
        const val KEY_LAST_CHECKED_AT = "last_checked_at"
        const val CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L
    }
}
