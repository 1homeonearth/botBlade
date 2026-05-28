package com.princess.botblade.ui.dashboard

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.princess.botblade.MainActivity
import com.princess.botblade.data.store.ActiveProjectStore
import com.princess.botblade.ui.components.BladeButton
import com.princess.botblade.ui.components.BotBladeTokens
import com.princess.botblade.ui.components.FlowLane
import com.princess.botblade.ui.components.SectionTitle
import com.princess.botblade.ui.components.StatusChip
import com.princess.botblade.ui.components.StatusTone
import com.princess.botblade.ui.components.WorkstationCard
import com.princess.botblade.ui.shell.BotBladeDestination
import com.princess.botblade.ui.theme.BotBladeTheme
import com.princess.botblade.ui.theme.isDynamicColorEnabled
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private val viewModel: DashboardViewModel by viewModels()
    private var activeProjectId: String? = null
    private lateinit var activeProjectStore: ActiveProjectStore
    private val started = SystemClock.elapsedRealtime()

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View {
        activeProjectStore = ActiveProjectStore(requireContext())
        return ComposeView(requireContext()).apply {
            setContent {
                BotBladeTheme(useDynamicColor = isDynamicColorEnabled(requireContext())) {
                    DashboardScreen(
                        vm = viewModel,
                        started = started,
                        onCreateProject = ::openAddProjectFlow,
                        onOpenProjects = { navigateTo(BotBladeDestination.Projects) },
                        onOpenEditor = { navigateTo(BotBladeDestination.Editor) },
                        onOpenBuild = { navigateTo(BotBladeDestination.Deployments) },
                        onOpenOps = { openOpsForProject(activeProjectId) },
                        onOpenSettings = { navigateTo(BotBladeDestination.Settings) },
                        onLogs = { (activity as? MainActivity)?.openLogsScreen() },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateActiveProject()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectLogs()
            viewModel.loadStatus(activeProjectId)
        }
    }

    override fun onResume() {
        super.onResume()
        updateActiveProject()
        viewModel.connectLogs()
        viewModel.loadStatus(activeProjectId)
    }

    override fun onPause() {
        viewModel.disconnectLogs()
        super.onPause()
    }

    private fun updateActiveProject() {
        activeProjectId = activeProjectStore.getActiveProjectId()
    }

    private fun openOpsForProject(selectedProjectId: String?) {
        val resolvedProjectId = selectedProjectId ?: activeProjectId
        val activity = activity as? MainActivity
        if (activity != null) {
            activity.openDeploymentsForProject(resolvedProjectId, activeProjectStore.getActiveProjectName())
        } else {
            navigateTo(BotBladeDestination.Deployments)
        }
    }

    private fun openAddProjectFlow() {
        requireContext()
            .getSharedPreferences(WORKSTATION_PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OPEN_ADD_PROJECT, true)
            .apply()
        navigateTo(BotBladeDestination.Projects)
    }

    private fun navigateTo(destination: BotBladeDestination) {
        (activity as? MainActivity)?.openDestination(destination)
    }

    private companion object {
        const val WORKSTATION_PREFS = "botblade_workstation_flow"
        const val KEY_OPEN_ADD_PROJECT = "open_add_project"
    }
}

@Composable
private fun DashboardScreen(
    vm: DashboardViewModel,
    started: Long,
    onCreateProject: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenBuild: () -> Unit,
    onOpenOps: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogs: () -> Unit,
) {
    val status by vm.status.collectAsState()
    val logs by vm.logs.collectAsState()
    val controls by vm.controls.collectAsState()
    val runtimeState = status?.status ?: "unknown"
    val uptime = (SystemClock.elapsedRealtime() - started) / 1000

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BotBladeTokens.Black)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("BotBlade", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                Text("Command Center", color = BotBladeTokens.HotPink, fontWeight = FontWeight.Bold)
            }
            Text(
                "Build, run, and release bots from one Android workstation.",
                color = BotBladeTokens.Muted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            WorkstationCard(accent = BotBladeTokens.HotPink) {
                Text("Active workspace", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                Text(
                    "Runtime: $runtimeState • uptime ${uptime}s",
                    color = BotBladeTokens.Muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 18.dp)) {
                    StatusChip("Backend", if (runtimeState == "unknown") StatusTone.Neutral else StatusTone.Success)
                    StatusChip("Runtime", if (controls.running) StatusTone.Success else StatusTone.Warning)
                    StatusChip("Logs", StatusTone.Info)
                }
            }
        }

        item {
            WorkstationCard(accent = BotBladeTokens.BabyBlue) {
                Text("Working order", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                Text(
                    "Start with a project. Prepare it in Editor and Vault. Build and run it. Use Ops Deck to deploy and inspect release readiness.",
                    color = BotBladeTokens.Muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item {
            FlowLane("Start", "Create a starter bot, import work, or open an existing workspace.", BotBladeTokens.HotPink) {
                BladeButton("Create / Import", onClick = onCreateProject, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onOpenProjects, modifier = Modifier.weight(1f)) { Text("Projects") }
            }
        }

        item {
            FlowLane("Prepare", "Scan the project, edit files, and add required secrets.", BotBladeTokens.BabyBlue) {
                BladeButton("Scan + Edit", onClick = onOpenEditor, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) { Text("Vault") }
            }
        }

        item {
            FlowLane("Build & Run", "Build from Editor, then control runtime and inspect logs.", BotBladeTokens.HotPink) {
                BladeButton("Builds", onClick = onOpenBuild, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = vm::start, enabled = controls.canStart, modifier = Modifier.weight(1f)) { Text("Run") }
            }
        }

        item {
            FlowLane("Deploy", "Create targets, deploy the latest successful build, and inspect release readiness.", BotBladeTokens.BabyBlue) {
                BladeButton("Ops Deck", onClick = onOpenOps, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) { Text("Settings") }
            }
        }

        item {
            WorkstationCard(accent = BotBladeTokens.HotPink) {
                Text("Runtime console", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    BladeButton("Start", onClick = vm::start, enabled = controls.canStart, modifier = Modifier.weight(1f))
                    BladeButton("Stop", onClick = vm::stop, enabled = controls.canStop, modifier = Modifier.weight(1f))
                    BladeButton("Restart", onClick = vm::restart, enabled = controls.canRestart, modifier = Modifier.weight(1f))
                }
                BladeButton("Open Logs", onClick = onLogs, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }

        if (logs.isNotEmpty()) {
            item { SectionTitle("Live logs") }
            items(logs.takeLast(25)) { line ->
                Text(
                    line,
                    color = BotBladeTokens.Success,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BotBladeTokens.Ink)
                        .padding(8.dp),
                )
            }
        }
    }
}
