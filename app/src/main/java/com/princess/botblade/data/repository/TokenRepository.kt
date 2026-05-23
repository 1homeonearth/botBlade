// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.repository  // line 7: executes this statement as part of this file's behavior

import android.content.Context  // line 9: executes this statement as part of this file's behavior
import androidx.security.crypto.EncryptedSharedPreferences  // line 10: executes this statement as part of this file's behavior
import androidx.security.crypto.MasterKey  // line 11: executes this statement as part of this file's behavior

class TokenRepository(context: Context) {  // line 13: executes this statement as part of this file's behavior
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()  // line 14: executes this statement as part of this file's behavior
    private val prefs = EncryptedSharedPreferences.create(  // line 15: executes this statement as part of this file's behavior
        context,  // line 16: executes this statement as part of this file's behavior
        "bot_token_secure_store",  // line 17: executes this statement as part of this file's behavior
        masterKey,  // line 18: executes this statement as part of this file's behavior
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,  // line 19: executes this statement as part of this file's behavior
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,  // line 20: executes this statement as part of this file's behavior
    )  // line 21: executes this statement as part of this file's behavior

    fun getTokenMasked(): String? {  // line 23: executes this statement as part of this file's behavior
        val token = prefs.getString(KEY_BOT_TOKEN, null) ?: return null  // line 24: executes this statement as part of this file's behavior
        val suffix = token.takeLast(4)  // line 25: executes this statement as part of this file's behavior
        return "••••$suffix"  // line 26: executes this statement as part of this file's behavior
    }  // line 27: executes this statement as part of this file's behavior

    fun setToken(token: String) = prefs.edit().putString(KEY_BOT_TOKEN, token).apply()  // line 29: executes this statement as part of this file's behavior

    fun clearToken() = prefs.edit().remove(KEY_BOT_TOKEN).apply()  // line 31: executes this statement as part of this file's behavior

    companion object { private const val KEY_BOT_TOKEN = "discord_bot_token" }  // line 33: executes this statement as part of this file's behavior
}  // line 34: executes this statement as part of this file's behavior
