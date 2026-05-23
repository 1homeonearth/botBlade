// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade  // line 7: executes this statement as part of this file's behavior

import android.content.ComponentName  // line 9: executes this statement as part of this file's behavior
import android.content.Context  // line 10: executes this statement as part of this file's behavior
import android.content.Intent  // line 11: executes this statement as part of this file's behavior
import android.content.ServiceConnection  // line 12: executes this statement as part of this file's behavior
import android.os.Bundle  // line 13: executes this statement as part of this file's behavior
import android.os.IBinder  // line 14: executes this statement as part of this file's behavior
import androidx.appcompat.app.AppCompatActivity  // line 15: executes this statement as part of this file's behavior
import androidx.datastore.preferences.core.booleanPreferencesKey  // line 16: executes this statement as part of this file's behavior
import androidx.datastore.preferences.core.edit  // line 17: executes this statement as part of this file's behavior
import androidx.datastore.preferences.preferencesDataStore  // line 18: executes this statement as part of this file's behavior
import androidx.fragment.app.Fragment  // line 19: executes this statement as part of this file's behavior
import androidx.lifecycle.lifecycleScope  // line 20: executes this statement as part of this file's behavior
import com.google.android.material.bottomnavigation.BottomNavigationView  // line 21: executes this statement as part of this file's behavior
import com.princess.botblade.backend.BotEngineBindingState  // line 22: executes this statement as part of this file's behavior
import com.princess.botblade.backend.BotEngineService  // line 23: executes this statement as part of this file's behavior
import com.princess.botblade.data.api.ApiConfig  // line 24: executes this statement as part of this file's behavior
import com.princess.botblade.data.repository.LocalProjectRepository  // line 25: executes this statement as part of this file's behavior
import com.princess.botblade.data.store.ActiveProjectStore  // line 26: executes this statement as part of this file's behavior
import com.princess.botblade.ui.dashboard.DashboardFragment  // line 27: executes this statement as part of this file's behavior
import com.princess.botblade.ui.deployments.DeploymentsFragment  // line 28: executes this statement as part of this file's behavior
import com.princess.botblade.ui.editor.CodeEditorFragment  // line 29: executes this statement as part of this file's behavior
import com.princess.botblade.ui.logs.LogsFragment  // line 30: executes this statement as part of this file's behavior
import com.princess.botblade.ui.onboarding.OnboardingFragment  // line 31: executes this statement as part of this file's behavior
import com.princess.botblade.ui.projects.ProjectsFragment  // line 32: executes this statement as part of this file's behavior
import com.princess.botblade.ui.settings.SettingsFragment  // line 33: executes this statement as part of this file's behavior
import kotlinx.coroutines.flow.first  // line 34: executes this statement as part of this file's behavior
import kotlinx.coroutines.launch  // line 35: executes this statement as part of this file's behavior

private val Context.dataStore by preferencesDataStore("onboarding")  // line 37: executes this statement as part of this file's behavior
private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")  // line 38: executes this statement as part of this file's behavior

class MainActivity : AppCompatActivity() {  // line 40: executes this statement as part of this file's behavior
    private var bound = false  // line 41: executes this statement as part of this file's behavior
    private var binder: BotEngineService.LocalBinder? = null  // line 42: executes this statement as part of this file's behavior
    private val connection = object : ServiceConnection {  // line 43: executes this statement as part of this file's behavior
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { binder = service as? BotEngineService.LocalBinder; bound = true; BotEngineBindingState.serviceRunning.value = binder?.isRunning() }  // line 44: executes this statement as part of this file's behavior
        override fun onServiceDisconnected(name: ComponentName?) { bound = false; binder = null; BotEngineBindingState.serviceRunning.value = null }  // line 45: executes this statement as part of this file's behavior
    }  // line 46: executes this statement as part of this file's behavior
    override fun onCreate(savedInstanceState: Bundle?) {  // line 47: executes this statement as part of this file's behavior
        super.onCreate(savedInstanceState)  // line 48: executes this statement as part of this file's behavior
        StartupDiagnostics.mark("main_activity_on_create_start")  // line 49: executes this statement as part of this file's behavior
        ApiConfig.initialize(this)  // line 50: executes this statement as part of this file's behavior
        setContentView(R.layout.activity_main)  // line 51: executes this statement as part of this file's behavior
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)  // line 52: executes this statement as part of this file's behavior
        bottomNavigation.setOnItemSelectedListener { item -> when (item.itemId) {  // line 53: executes this statement as part of this file's behavior
            R.id.navigation_dashboard -> { showFragment(DashboardFragment()); true }  // line 54: executes this statement as part of this file's behavior
            R.id.navigation_projects -> { showFragment(ProjectsFragment()); true }  // line 55: executes this statement as part of this file's behavior
            R.id.navigation_editor -> { showFragment(CodeEditorFragment()); true }  // line 56: executes this statement as part of this file's behavior
            R.id.navigation_deployments -> { showFragment(DeploymentsFragment()); true }  // line 57: executes this statement as part of this file's behavior
            R.id.navigation_settings -> { showFragment(SettingsFragment()); true }  // line 58: executes this statement as part of this file's behavior
            else -> false  // line 59: executes this statement as part of this file's behavior
        } }  // line 60: executes this statement as part of this file's behavior
        lifecycleScope.launch {  // line 61: executes this statement as part of this file's behavior
            val done = dataStore.data.first()[onboardingCompleteKey] == true  // line 62: executes this statement as part of this file's behavior
            if (!done) { bottomNavigation.visibility = android.view.View.GONE; showFragment(OnboardingFragment()) }  // line 63: executes this statement as part of this file's behavior
            else if (savedInstanceState == null) bottomNavigation.selectedItemId = R.id.navigation_dashboard  // line 64: executes this statement as part of this file's behavior
        }  // line 65: executes this statement as part of this file's behavior
        window.decorView.post { StartupDiagnostics.mark("first_render") }  // line 66: executes this statement as part of this file's behavior
    }  // line 67: executes this statement as part of this file's behavior
    fun finishOnboarding() {  // line 68: executes this statement as part of this file's behavior
        if (isFinishing || isDestroyed) return  // line 69: executes this statement as part of this file's behavior
        lifecycleScope.launch {  // line 70: executes this statement as part of this file's behavior
            applicationContext.dataStore.edit { prefs -> prefs[onboardingCompleteKey] = true }  // line 71: executes this statement as part of this file's behavior
            showDashboardAfterOnboarding()  // line 72: executes this statement as part of this file's behavior
        }  // line 73: executes this statement as part of this file's behavior
    }  // line 74: executes this statement as part of this file's behavior

    private fun showDashboardAfterOnboarding() {  // line 76: executes this statement as part of this file's behavior
        if (isFinishing || isDestroyed) return  // line 77: executes this statement as part of this file's behavior
        val action = {  // line 78: executes this statement as part of this file's behavior
            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)  // line 79: executes this statement as part of this file's behavior
            bottomNavigation.visibility = android.view.View.VISIBLE  // line 80: executes this statement as part of this file's behavior
            if (supportFragmentManager.isStateSaved) {  // line 81: executes this statement as part of this file's behavior
                bottomNavigation.selectedItemId = R.id.navigation_dashboard  // line 82: executes this statement as part of this file's behavior
            } else {  // line 83: executes this statement as part of this file's behavior
                showFragment(DashboardFragment())  // line 84: executes this statement as part of this file's behavior
                if (bottomNavigation.selectedItemId != R.id.navigation_dashboard) {  // line 85: executes this statement as part of this file's behavior
                    bottomNavigation.selectedItemId = R.id.navigation_dashboard  // line 86: executes this statement as part of this file's behavior
                }  // line 87: executes this statement as part of this file's behavior
            }  // line 88: executes this statement as part of this file's behavior
        }  // line 89: executes this statement as part of this file's behavior
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) action() else runOnUiThread { action() }  // line 90: executes this statement as part of this file's behavior
    }  // line 91: executes this statement as part of this file's behavior
    override fun onResume() { super.onResume(); bindService(Intent(this, BotEngineService::class.java), connection, Context.BIND_AUTO_CREATE) }  // line 92: executes this statement as part of this file's behavior
    override fun onPause() { if (bound) { unbindService(connection); bound = false; binder = null; BotEngineBindingState.serviceRunning.value = null }; super.onPause() }  // line 93: executes this statement as part of this file's behavior
    fun showEditorForProject(projectName: String) {  // line 94: executes this statement as part of this file's behavior
        val project = LocalProjectRepository(this).findProjectByName(projectName)  // line 95: executes this statement as part of this file's behavior
        ActiveProjectStore(this).setActiveProject(project?.id ?: projectName, projectName)  // line 96: executes this statement as part of this file's behavior
        showFragment(CodeEditorFragment())  // line 97: executes this statement as part of this file's behavior
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.navigation_editor  // line 98: executes this statement as part of this file's behavior
    }  // line 99: executes this statement as part of this file's behavior

    fun openLogsScreen() { showFragment(LogsFragment()) }  // line 101: executes this statement as part of this file's behavior

    private fun showFragment(fragment: Fragment) {  // line 103: executes this statement as part of this file's behavior
        val tx = supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)  // line 104: executes this statement as part of this file's behavior
        if (supportFragmentManager.isStateSaved) tx.commitAllowingStateLoss() else tx.commit()  // line 105: executes this statement as part of this file's behavior
    }  // line 106: executes this statement as part of this file's behavior
}  // line 107: executes this statement as part of this file's behavior
