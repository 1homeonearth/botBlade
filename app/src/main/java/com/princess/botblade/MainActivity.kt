package com.princess.botblade

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.princess.botblade.backend.BotEngineBindingState
import com.princess.botblade.backend.BotEngineService
import com.princess.botblade.data.api.ApiConfig
import com.princess.botblade.ui.dashboard.DashboardFragment
import com.princess.botblade.ui.deployments.DeploymentsFragment
import com.princess.botblade.ui.editor.CodeEditorFragment
import com.princess.botblade.ui.projects.ProjectsFragment
import com.princess.botblade.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {
    private var bound = false
    private var binder: BotEngineService.LocalBinder? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as? BotEngineService.LocalBinder
            bound = true
            BotEngineBindingState.serviceRunning.value = binder?.isRunning()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            binder = null
            BotEngineBindingState.serviceRunning.value = null
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) { /* existing */
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
        if (savedInstanceState == null) bottomNavigation.selectedItemId = R.id.navigation_dashboard
    }

    override fun onResume() {
        super.onResume()
        bindService(Intent(this, BotEngineService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        if (bound) {
            unbindService(connection)
            bound = false
            binder = null
            BotEngineBindingState.serviceRunning.value = null
        }
        super.onPause()
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}
