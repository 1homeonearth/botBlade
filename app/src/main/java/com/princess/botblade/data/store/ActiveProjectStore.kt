package com.princess.botblade.data.store

import android.content.Context

class ActiveProjectStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun getActiveProjectId(): String? = preferences.getString(KEY_ACTIVE_PROJECT_ID, null)
        ?.takeIf { it.isNotBlank() }

    fun getActiveProjectName(): String? = preferences.getString(KEY_ACTIVE_PROJECT_NAME, null)
        ?.takeIf { it.isNotBlank() }

    fun setActiveProject(projectId: String?, projectName: String?) {
        preferences.edit().apply {
            if (projectId.isNullOrBlank()) {
                remove(KEY_ACTIVE_PROJECT_ID)
                remove(KEY_ACTIVE_PROJECT_NAME)
            } else {
                putString(KEY_ACTIVE_PROJECT_ID, projectId)
                if (projectName.isNullOrBlank()) {
                    remove(KEY_ACTIVE_PROJECT_NAME)
                } else {
                    putString(KEY_ACTIVE_PROJECT_NAME, projectName)
                }
            }
        }.apply()
    }

    fun setActiveProjectId(projectId: String?) {
        setActiveProject(projectId, getActiveProjectName())
    }

    companion object {
        private const val PREFS_NAME = "botblade_active_project"
        private const val KEY_ACTIVE_PROJECT_ID = "active_project_id"
        private const val KEY_ACTIVE_PROJECT_NAME = "active_project_name"
    }
}
