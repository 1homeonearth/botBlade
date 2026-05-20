package com.princess.botblade.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenRepository(context: Context) {
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "bot_token_secure_store",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getTokenMasked(): String? {
        val token = prefs.getString(KEY_BOT_TOKEN, null) ?: return null
        val suffix = token.takeLast(4)
        return "••••$suffix"
    }

    fun setToken(token: String) = prefs.edit().putString(KEY_BOT_TOKEN, token).apply()

    fun clearToken() = prefs.edit().remove(KEY_BOT_TOKEN).apply()

    companion object { private const val KEY_BOT_TOKEN = "discord_bot_token" }
}
