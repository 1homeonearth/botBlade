package com.princess.botblade

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.princess.botblade.backend.BotEngineBindingState
import com.princess.botblade.backend.BotEngineService
import com.princess.botblade.data.api.ApiConfig
import com.princess.botblade.data.repository.LocalProjectRepository
import com.princess.botblade.data.store.ActiveProjectStore
import com.princess.botblade.ui.dashboard.DashboardFragment
import com.princess.botblade.ui.deployments.DeploymentsFragment
import com.princess.botblade.ui.editor.CodeEditorFragment
import com.princess.botblade.ui.logs.LogsFragment
import com.princess.botblade.ui.onboarding.OnboardingFragment
import com.princess.botblade.ui.projects.ProjectsFragment
import com.princess.botblade.ui.settings.SettingsFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("onboarding")

class MainActivity : AppCompatActivity() {
    private var bound = false
    private var binder: BotEngineService.LocalBinder? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { binder = service as? BotEngineService.LocalBinder; bound = true; BotEngineBindingState.serviceRunning.value = binder?.isRunning() }
        override fun onServiceDisconnected(name: ComponentName?) { bound = false; binder = null; BotEngineBindingState.serviceRunning.value = null }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiConfig.initialize(this)
        setContentView(R.layout.activity_main)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item -> when (item.itemId) {
            R.id.navigation_dashboard -> { showFragment(DashboardFragment()); true }
            R.id.navigation_projects -> { showFragment(ProjectsFragment()); true }
            R.id.navigation_editor -> { showFragment(CodeEditorFragment()); true }
            R.id.navigation_deployments -> { showFragment(DeploymentsFragment()); true }
            R.id.navigation_settings -> { showFragment(SettingsFragment()); true }
            else -> false
        } }
        lifecycleScope.launch {
            val done = dataStore.data.first()[booleanPreferencesKey("onboarding_complete")] == true
            if (!done) { bottomNavigation.visibility = android.view.View.GONE; showFragment(OnboardingFragment()) }
            else if (savedInstanceState == null) bottomNavigation.selectedItemId = R.id.navigation_dashboard
        }
    }
    fun finishOnboarding() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.visibility = android.view.View.VISIBLE
        showFragment(DashboardFragment())
        if (bottomNavigation.selectedItemId != R.id.navigation_dashboard) {
            bottomNavigation.selectedItemId = R.id.navigation_dashboard
        }
    }
    override fun onResume() { super.onResume(); bindService(Intent(this, BotEngineService::class.java), connection, Context.BIND_AUTO_CREATE) }
    override fun onPause() { if (bound) { unbindService(connection); bound = false; binder = null; BotEngineBindingState.serviceRunning.value = null }; super.onPause() }
    fun showEditorForProject(projectName: String) {
        val project = LocalProjectRepository(this).findProjectByName(projectName)
        ActiveProjectStore(this).setActiveProject(project?.id ?: projectName, projectName)
        showFragment(CodeEditorFragment())
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.navigation_editor
    }

    fun openLogsScreen() { showFragment(LogsFragment()) }

    private fun showFragment(fragment: Fragment) {
        val tx = supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
        if (supportFragmentManager.isStateSaved) tx.commitAllowingStateLoss() else tx.commit()
    }
}
