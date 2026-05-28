package com.princess.botblade.ui.dashboard

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.princess.botblade.MainActivity
import com.princess.botblade.data.store.ActiveProjectStore
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
        modifier = Modifier.fillMaxSize().background(BotBlack).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("BotBlade", color = BabyBlue, fontWeight = FontWeight.Bold)
                Text("Command Center", color = HotPink, fontWeight = FontWeight.Bold)
            }
            Text("Build, run, and release bots from one Android workstation.", color = Muted, modifier = Modifier.padding(top = 8.dp))
        }

        item {
            WorkstationCard(accent = HotPink) {
                Text("Active workspace", color = BabyBlue, fontWeight = FontWeight.Bold)
                Text("Runtime: $runtimeState • uptime ${uptime}s", color = Muted, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 18.dp)) {
                    StatusChip("Backend", if (runtimeState == "unknown") Muted else Success)
                    StatusChip("Runtime", if (controls.running) Success else Warning)
                    StatusChip("Logs", BabyBlue)
                }
            }
        }

        item {
            WorkstationCard(accent = BabyBlue) {
                Text("Working order", color = BabyBlue, fontWeight = FontWeight.Bold)
                Text("Start with a project. Prepare it in Editor and Vault. Build and run it. Use Ops Deck to deploy and inspect release readiness.", color = Muted, modifier = Modifier.padding(top = 8.dp))
            }
        }

        item {
            FlowLane("Start", "Create a starter bot, import work, or open an existing workspace.", HotPink) {
                Button(onClick = onCreateProject, modifier = Modifier.weight(1f)) { Text("Create / Import") }
                OutlinedButton(onClick = onOpenProjects, modifier = Modifier.weight(1f)) { Text("Projects") }
            }
        }

        item {
            FlowLane("Prepare", "Scan the project, edit files, and add required secrets.", BabyBlue) {
                Button(onClick = onOpenEditor, modifier = Modifier.weight(1f)) { Text("Scan + Edit") }
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) { Text("Vault") }
            }
        }

        item {
            FlowLane("Build & Run", "Build from Editor, then control runtime and inspect logs.", HotPink) {
                Button(onClick = onOpenBuild, modifier = Modifier.weight(1f)) { Text("Builds") }
                OutlinedButton(onClick = vm::start, enabled = controls.canStart, modifier = Modifier.weight(1f)) { Text("Run") }
            }
        }

        item {
            FlowLane("Deploy", "Create targets, deploy the latest successful build, and inspect release readiness.", BabyBlue) {
                Button(onClick = onOpenOps, modifier = Modifier.weight(1f)) { Text("Ops Deck") }
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) { Text("Settings") }
            }
        }

        item {
            WorkstationCard(accent = HotPink) {
                Text("Runtime console", color = BabyBlue, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Button(onClick = vm::start, enabled = controls.canStart, modifier = Modifier.weight(1f)) { Text("Start") }
                    Button(onClick = vm::stop, enabled = controls.canStop, modifier = Modifier.weight(1f)) { Text("Stop") }
                    Button(onClick = vm::restart, enabled = controls.canRestart, modifier = Modifier.weight(1f)) { Text("Restart") }
                }
                Button(onClick = onLogs, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Open Logs") }
            }
        }

        if (logs.isNotEmpty()) {
            item { Text("Live logs", color = HotPink, fontWeight = FontWeight.Bold) }
            items(logs.takeLast(25)) { line ->
                Text(line, color = Success, modifier = Modifier.fillMaxWidth().background(Ink).padding(8.dp))
            }
        }
    }
}

@Composable
private fun FlowLane(
    title: String,
    detail: String,
    accent: Color,
    buttons: @Composable RowScope.() -> Unit,
) {
    WorkstationCard(accent = accent) {
        Text(title, color = BabyBlue, fontWeight = FontWeight.Bold)
        Text(detail, color = Muted, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            buttons()
        }
    }
}

@Composable
private fun WorkstationCard(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Panel,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Stroke, RoundedCornerShape(22.dp)),
    ) {
        Row {
            Surface(color = accent, modifier = Modifier.fillMaxWidth(0.02f)) {}
            Column(modifier = Modifier.padding(22.dp), content = content)
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Surface(color = Ink, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Stroke)) {
        Text(label, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

private val BotBlack = Color(0xFF05060A)
private val Ink = Color(0xFF090B12)
private val Panel = Color(0xFF101522)
private val Stroke = Color(0xFF2F405F)
private val BabyBlue = Color(0xFF8FD8FF)
private val HotPink = Color(0xFFFF3EA5)
private val Muted = Color(0xFFAAB8CC)
private val Success = Color(0xFF7CFFB2)
private val Warning = Color(0xFFFFD166)
