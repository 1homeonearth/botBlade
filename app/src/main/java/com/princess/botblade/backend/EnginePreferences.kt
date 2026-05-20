package com.princess.botblade.backend

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.engineDataStore by preferencesDataStore(name = "engine_prefs")

object EnginePreferences {
    val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")

    fun autoStartOnBoot(context: Context): Flow<Boolean> =
        context.applicationContext.engineDataStore.data.map { it[AUTO_START_ON_BOOT] ?: false }

    suspend fun setAutoStartOnBoot(context: Context, enabled: Boolean) {
        context.applicationContext.engineDataStore.edit { it[AUTO_START_ON_BOOT] = enabled }
    }
}
