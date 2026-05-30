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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.princess.botblade.backend.BotEngineBindingState
import com.princess.botblade.backend.BotEngineService
import com.princess.botblade.data.api.ApiConfig
import com.princess.botblade.data.repository.LocalProjectRepository
import com.princess.botblade.data.store.ActiveProjectStore
import com.princess.botblade.ui.dashboard.DashboardFragment
import com.princess.botblade.ui.deployments.DeploymentsFragment
import com.princess.botblade.ui.logs.LogsFragment
import com.princess.botblade.ui.shell.BotBladeAppShell
import com.princess.botblade.ui.shell.BotBladeDestination
import com.princess.botblade.ui.theme.BotBladeTheme
import com.princess.botblade.ui.theme.isDynamicColorEnabled

class MainActivity : AppCompatActivity() {
    private var bound = false
    private var binder: BotEngineService.LocalBinder? = null
    private var selectedDestination by mutableStateOf(BotBladeDestination.Dashboard)
    private var shellReady = false

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
            installComposeShell(savedInstanceState)
            handleNavigationIntent(intent)
            window.decorView.post { StartupDiagnostics.mark("first_render") }
        }.onFailure { error ->
            showStartupFallback(error)
        }
    }

    private fun installComposeShell(savedInstanceState: Bundle?) {
        val shouldOpenDashboard = savedInstanceState == null
        setContent {
            val runtimeOnline by BotEngineBindingState.serviceRunning.collectAsState()
            BotBladeTheme(useDynamicColor = isDynamicColorEnabled(this)) {
                BotBladeAppShell(
                    selectedDestination = selectedDestination,
                    runtimeOnline = runtimeOnline,
                    fragmentContainerId = R.id.fragment_container,
                    onDestinationSelected = ::openDestination,
                    contentReady = {
                        shellReady = true
                        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
                            if (!handleNavigationIntent(intent) && shouldOpenDashboard) {
                                openDestination(selectedDestination)
                            }
                        }
                    },
                )
            }
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

    fun openDestination(destination: BotBladeDestination) {
        selectedDestination = destination
        if (!shellReady) return
        showFragmentSafely { destination.createFragment() }
    }

    fun handleNavigationIntent(intent: Intent?): Boolean {
        val launchIntent = intent ?: return false
        val destination = launchIntent.getStringExtra(EXTRA_DESTINATION)
        val opensDashboard = destination == DESTINATION_DASHBOARD || launchIntent.action == ACTION_OPEN_DASHBOARD
        if (!opensDashboard) return false

        val openRuntimePanel = launchIntent.getBooleanExtra(EXTRA_OPEN_RUNTIME_PANEL, false)
        selectedDestination = BotBladeDestination.Dashboard
        if (shellReady) {
            showFragmentSafely { DashboardFragment.newInstance(openRuntimePanel = openRuntimePanel) }
        }
        return true
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
        if (!shellReady) {
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
        openDestination(BotBladeDestination.Editor)
    }

    fun openDeploymentsForProject(projectId: String?, projectName: String?) {
        selectedDestination = BotBladeDestination.Deployments
        val args = DeploymentsFragment.buildArgs(projectId, projectName)
        showFragmentSafely { DeploymentsFragment.newInstance(args) }
    }

    fun openLogsScreen() {
        showFragmentSafely { LogsFragment() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        runCatching {
            bindService(Intent(this, BotEngineService::class.java), connection, BIND_AUTO_CREATE)
        }
    }

    companion object {
        const val ACTION_OPEN_DASHBOARD = "com.princess.botblade.action.OPEN_DASHBOARD"
        const val EXTRA_DESTINATION = "com.princess.botblade.extra.DESTINATION"
        const val EXTRA_OPEN_RUNTIME_PANEL = "com.princess.botblade.extra.OPEN_RUNTIME_PANEL"
        const val DESTINATION_DASHBOARD = "dashboard"
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
