package com.princess.botblade

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
import com.princess.botblade.ui.importforge.ImportForgeFragment
import com.princess.botblade.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {
    private var bound = false
    private var binder: BotEngineService.LocalBinder? = null

    private val runtimePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val denied = results.filterValues { granted -> !granted }.keys
        if (denied.isNotEmpty()) {
            Toast.makeText(this, "BotBlade could not enable Downloads log mirroring for: ${denied.joinToString()}", Toast.LENGTH_LONG).show()
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            StartupDiagnostics.mark("main_activity_on_create_start")
            ApiConfig.initialize(this)
            requestRuntimePermissionsIfNeeded()
            setContentView(R.layout.activity_main)
            setupBottomNavigation(savedInstanceState)
            window.decorView.post { StartupDiagnostics.mark("first_render") }
        }.onFailure { error ->
            showStartupFallback(error)
        }
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        if (needed.isNotEmpty()) {
            runtimePermissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun setupBottomNavigation(savedInstanceState: Bundle?) {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> showFragmentSafely { DashboardFragment() }
                R.id.navigation_projects -> showFragmentSafely { ImportForgeFragment() }
                R.id.navigation_editor -> showFragmentSafely { CodeEditorFragment() }
                R.id.navigation_deployments -> showFragmentSafely { DeploymentsFragment.newInstance() }
                R.id.navigation_settings -> showFragmentSafely { SettingsFragment() }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navigation_dashboard
        }
    }

    private fun showFragmentSafely(factory: () -> Fragment): Boolean =
        runCatching {
            showFragment(factory())
            true
        }.getOrElse { error ->
            showStartupFallback(error)
            true
        }

    private fun showFragment(fragment: Fragment) {
        val tx = supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
        if (supportFragmentManager.isStateSaved) tx.commitAllowingStateLoss() else tx.commit()
    }

    private fun showStartupFallback(error: Throwable) {
        val message = "BotBlade recovered from a startup screen crash.\n\n${error::class.java.simpleName}: ${error.message ?: "Unknown error"}\n\nOpen Dashboard, Projects, or Settings after updating."
        if (findViewById<ViewGroup?>(R.id.fragment_container) == null) {
            val fallback = TextView(this).apply {
                text = message
                gravity = Gravity.CENTER
                setPadding(32, 32, 32, 32)
            }
            setContentView(fallback)
            return
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, StartupFallbackFragment.newInstance(message))
            .commitAllowingStateLoss()
    }

    fun showEditorForProject(projectName: String) {
        val projectId = try {
            LocalProjectRepository(this).listProjects().firstOrNull { it.name == projectName }?.id
        } catch (_: Exception) {
            null
        }
        if (projectId != null) {
            ActiveProjectStore(this).setActiveProject(projectId, projectName)
        }
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.navigation_editor
    }


    fun openDeploymentsForProject(projectId: String?, projectName: String?) {
        val args = DeploymentsFragment.buildArgs(projectId, projectName)
        showFragmentSafely { DeploymentsFragment.newInstance(args) }
    }
    fun openLogsScreen() {
        showFragmentSafely { LogsFragment() }
    }

    override fun onStart() {
        super.onStart()
        runCatching {
            bindService(Intent(this, BotEngineService::class.java), connection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            runCatching { unbindService(connection) }
            bound = false
            binder = null
            BotEngineBindingState.serviceRunning.value = null
        }
    }
}
