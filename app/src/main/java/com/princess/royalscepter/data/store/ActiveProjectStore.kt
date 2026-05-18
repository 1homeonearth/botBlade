package com.princess.royalscepter.data.store

import android.content.Context

class ActiveProjectStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun getActiveProjectId(): String? = preferences.getString(KEY_ACTIVE_PROJECT_ID, null)
        ?.takeIf { it.isNotBlank() }

    fun setActiveProjectId(projectId: String?) {
        preferences.edit().apply {
            if (projectId.isNullOrBlank()) {
                remove(KEY_ACTIVE_PROJECT_ID)
            } else {
                putString(KEY_ACTIVE_PROJECT_ID, projectId)
            }
        }.apply()
    }

    companion object {
        private const val PREFS_NAME = "royal_scepter_active_project"
        private const val KEY_ACTIVE_PROJECT_ID = "active_project_id"
    }
}
