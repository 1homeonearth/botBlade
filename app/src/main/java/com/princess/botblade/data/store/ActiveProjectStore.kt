// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.store  // line 7: executes this statement as part of this file's behavior

import android.content.Context  // line 9: executes this statement as part of this file's behavior

class ActiveProjectStore(context: Context) {  // line 11: executes this statement as part of this file's behavior
    private val preferences = context.applicationContext.getSharedPreferences(  // line 12: executes this statement as part of this file's behavior
        PREFS_NAME,  // line 13: executes this statement as part of this file's behavior
        Context.MODE_PRIVATE,  // line 14: executes this statement as part of this file's behavior
    )  // line 15: executes this statement as part of this file's behavior

    fun getActiveProjectId(): String? = preferences.getString(KEY_ACTIVE_PROJECT_ID, null)  // line 17: executes this statement as part of this file's behavior
        ?.takeIf { it.isNotBlank() }  // line 18: executes this statement as part of this file's behavior

    fun getActiveProjectName(): String? = preferences.getString(KEY_ACTIVE_PROJECT_NAME, null)  // line 20: executes this statement as part of this file's behavior
        ?.takeIf { it.isNotBlank() }  // line 21: executes this statement as part of this file's behavior

    fun setActiveProject(projectId: String?, projectName: String?) {  // line 23: executes this statement as part of this file's behavior
        preferences.edit().apply {  // line 24: executes this statement as part of this file's behavior
            if (projectId.isNullOrBlank()) {  // line 25: executes this statement as part of this file's behavior
                remove(KEY_ACTIVE_PROJECT_ID)  // line 26: executes this statement as part of this file's behavior
                remove(KEY_ACTIVE_PROJECT_NAME)  // line 27: executes this statement as part of this file's behavior
            } else {  // line 28: executes this statement as part of this file's behavior
                putString(KEY_ACTIVE_PROJECT_ID, projectId)  // line 29: executes this statement as part of this file's behavior
                if (projectName.isNullOrBlank()) {  // line 30: executes this statement as part of this file's behavior
                    remove(KEY_ACTIVE_PROJECT_NAME)  // line 31: executes this statement as part of this file's behavior
                } else {  // line 32: executes this statement as part of this file's behavior
                    putString(KEY_ACTIVE_PROJECT_NAME, projectName)  // line 33: executes this statement as part of this file's behavior
                }  // line 34: executes this statement as part of this file's behavior
            }  // line 35: executes this statement as part of this file's behavior
        }.apply()  // line 36: executes this statement as part of this file's behavior
    }  // line 37: executes this statement as part of this file's behavior

    fun setActiveProjectId(projectId: String?) {  // line 39: executes this statement as part of this file's behavior
        setActiveProject(projectId, getActiveProjectName())  // line 40: executes this statement as part of this file's behavior
    }  // line 41: executes this statement as part of this file's behavior

    companion object {  // line 43: executes this statement as part of this file's behavior
        private const val PREFS_NAME = "botblade_active_project"  // line 44: executes this statement as part of this file's behavior
        private const val KEY_ACTIVE_PROJECT_ID = "active_project_id"  // line 45: executes this statement as part of this file's behavior
        private const val KEY_ACTIVE_PROJECT_NAME = "active_project_name"  // line 46: executes this statement as part of this file's behavior
    }  // line 47: executes this statement as part of this file's behavior
}  // line 48: executes this statement as part of this file's behavior
