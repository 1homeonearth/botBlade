// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.backend  // line 7: executes this statement as part of this file's behavior

import android.content.Context  // line 9: executes this statement as part of this file's behavior
import androidx.datastore.preferences.core.booleanPreferencesKey  // line 10: executes this statement as part of this file's behavior
import androidx.datastore.preferences.core.edit  // line 11: executes this statement as part of this file's behavior
import androidx.datastore.preferences.preferencesDataStore  // line 12: executes this statement as part of this file's behavior
import kotlinx.coroutines.flow.Flow  // line 13: executes this statement as part of this file's behavior
import kotlinx.coroutines.flow.map  // line 14: executes this statement as part of this file's behavior

private val Context.engineDataStore by preferencesDataStore(name = "engine_prefs")  // line 16: executes this statement as part of this file's behavior

object EnginePreferences {  // line 18: executes this statement as part of this file's behavior
    val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")  // line 19: executes this statement as part of this file's behavior

    fun autoStartOnBoot(context: Context): Flow<Boolean> =  // line 21: executes this statement as part of this file's behavior
        context.applicationContext.engineDataStore.data.map { it[AUTO_START_ON_BOOT] ?: false }  // line 22: executes this statement as part of this file's behavior

    suspend fun setAutoStartOnBoot(context: Context, enabled: Boolean) {  // line 24: executes this statement as part of this file's behavior
        context.applicationContext.engineDataStore.edit { it[AUTO_START_ON_BOOT] = enabled }  // line 25: executes this statement as part of this file's behavior
    }  // line 26: executes this statement as part of this file's behavior
}  // line 27: executes this statement as part of this file's behavior
