package com.princess.botblade.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.princess.botblade.BuildConfig
import com.princess.botblade.MainActivity
import com.princess.botblade.data.api.ApiConfig
import com.princess.botblade.data.repository.LocalProjectRepository
import com.princess.botblade.data.store.ActiveProjectStore
import com.princess.botblade.ui.components.BladeButton
import com.princess.botblade.ui.components.BladeOutlinedButton
import com.princess.botblade.ui.components.BotBladeTokens
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
    private var activeProjectId by mutableStateOf<String?>(null)
    private var activeProjectName by mutableStateOf<String?>(null)
    private var projectCount by mutableStateOf<Int?>(null)
    private lateinit var activeProjectStore: ActiveProjectStore

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View {
        activeProjectStore = ActiveProjectStore(requireContext())
        updateActiveProject()
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                BotBladeTheme(useDynamicColor = isDynamicColorEnabled(requireContext())) {
                    DashboardScreen(
                        vm = viewModel,
                        activeProjectId = activeProjectId,
                        activeProjectName = activeProjectName,
                        projectCount = projectCount,
                        openRuntimePanel = arguments?.getBoolean(ARG_OPEN_RUNTIME_PANEL) == true,
                        onCreateProject = ::openAddProjectFlow,
                        onOpenProjects = { navigateTo(BotBladeDestination.Projects) },
                        onOpenEditor = { navigateTo(BotBladeDestination.Editor) },
                        onOpenDeployments = { openDeploymentsForProject(activeProjectId) },
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
        activeProjectName = activeProjectStore.getActiveProjectName()
        projectCount = runCatching { LocalProjectRepository(requireContext()).listProjects().size }.getOrNull()
    }

    private fun openDeploymentsForProject(selectedProjectId: String?) {
        val activity = activity as? MainActivity
        if (activity != null) {
            activity.openDeploymentsForProject(selectedProjectId ?: activeProjectId, activeProjectStore.getActiveProjectName())
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

    companion object {
        private const val ARG_OPEN_RUNTIME_PANEL = "open_runtime_panel"
        private const val WORKSTATION_PREFS = "botblade_workstation_flow"
        private const val KEY_OPEN_ADD_PROJECT = "open_add_project"

        fun newInstance(openRuntimePanel: Boolean = false): DashboardFragment = DashboardFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_OPEN_RUNTIME_PANEL, openRuntimePanel) }
        }
    }
}

@Composable
private fun DashboardScreen(
    vm: DashboardViewModel,
    activeProjectId: String?,
    activeProjectName: String?,
    projectCount: Int?,
    openRuntimePanel: Boolean,
    onCreateProject: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenDeployments: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogs: () -> Unit,
) {
    val status by vm.status.collectAsState()
    val logs by vm.logs.collectAsState()
    val controls by vm.controls.collectAsState()
    val message by vm.message.collectAsState()
    val runtimeState = status?.status ?: "unknown"
    val backendTone = when (runtimeState.lowercase()) {
        "running", "stopped", "idle", "ready" -> StatusTone.Success
        "unknown" -> StatusTone.Neutral
        else -> StatusTone.Warning
    }
    val runtimeSource = when (status?.projectId) {
        "bound" -> "Bound Android service"
        "legacy" -> "Legacy backend API"
        null -> "Waiting for runtime status"
        else -> "Project runtime API"
    }
    val listState = rememberLazyListState()

    LaunchedEffect(openRuntimePanel) {
        if (openRuntimePanel) {
            vm.markOpenedFromNotification()
            listState.animateScrollToItem(3)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(BotBladeTokens.Black)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            AppIdentityHeader(runtimeState = runtimeState, running = controls.running)
        }

        item {
            ActiveWorkspaceCard(
                activeProjectId = activeProjectId,
                activeProjectName = activeProjectName,
                onOpenEditor = onOpenEditor,
                onOpenProjects = onOpenProjects,
                onCreateProject = onCreateProject,
            )
        }

        item {
            DashboardStatsCard(
                projectCount = projectCount,
                runningBots = if (controls.running) 1 else 0,
                gitState = if (activeProjectId == null) "Select workspace" else "Not checked",
                deployState = if (activeProjectId == null) "No workspace" else "Open deployments",
            )
        }

        item {
            RuntimeControlCard(
                runtimeState = runtimeState,
                message = message,
                controls = controls,
                onStart = { vm.start(activeProjectId) },
                onStop = { vm.stop(activeProjectId) },
                onRestart = { vm.restart(activeProjectId) },
                onOpenLogs = onLogs,
                onClearMessage = vm::clearMessage,
            )
        }

        item {
            BackendStatusCard(
                runtimeState = runtimeState,
                backendTone = backendTone,
                runtimeSource = runtimeSource,
            )
        }

        item {
            QuickToolsGrid(
                onOpenEditor = onOpenEditor,
                onOpenProjects = onOpenProjects,
                onOpenDeployments = onOpenDeployments,
                onOpenSettings = onOpenSettings,
                onOpenLogs = onLogs,
            )
        }

        item {
            LogsPreviewCard(logs = logs.takeLast(25), onOpenLogs = onLogs)
        }
    }
}

@Composable
private fun AppIdentityHeader(runtimeState: String, running: Boolean) {
    WorkstationCard(accent = BotBladeTokens.HotPink) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("BotBlade", color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    "Android-native bot forge and command center",
                    color = BotBladeTokens.Muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    color = BotBladeTokens.GlitterGold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            StatusChip(
                label = if (running) "Runtime running" else "Runtime $runtimeState",
                tone = if (running) StatusTone.Success else StatusTone.Warning,
            )
        }
    }
}

@Composable
private fun ActiveWorkspaceCard(
    activeProjectId: String?,
    activeProjectName: String?,
    onOpenEditor: () -> Unit,
    onOpenProjects: () -> Unit,
    onCreateProject: () -> Unit,
) {
    WorkstationCard(accent = BotBladeTokens.BabyBlue) {
        SectionTitle("Active workspace")
        Text(
            activeProjectName ?: "No active workspace",
            color = BotBladeTokens.BabyBlue,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            activeProjectId?.let { "Project id: $it" } ?: "Import or select a project to unlock project-scoped runtime controls.",
            color = BotBladeTokens.Muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
            BladeButton("Open Files / Editor", onClick = onOpenEditor, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BladeOutlinedButton("Projects", onClick = onOpenProjects, modifier = Modifier.weight(1f))
                BladeOutlinedButton("Import", onClick = onCreateProject, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DashboardStatsCard(
    projectCount: Int?,
    runningBots: Int,
    gitState: String,
    deployState: String,
) {
    WorkstationCard(accent = BotBladeTokens.GlitterGold) {
        SectionTitle("Command center stats")
        Spacer(Modifier.height(10.dp))
        StatLine("Projects", projectCount?.toString() ?: "Not loaded", "Local workspace records")
        StatLine("Running bots", runningBots.toString(), "Runtime services currently reported online")
        StatLine("Git state", gitState, "Dirty-file check will graduate into Files / Editor")
        StatLine("Latest deploy", deployState, "Deployment history opens from the Deployments tool")
    }
}

@Composable
private fun StatLine(label: String, value: String, detail: String) {
    Surface(
        color = BotBladeTokens.Ink,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BotBladeTokens.Stroke),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, color = BotBladeTokens.Muted, style = MaterialTheme.typography.labelMedium)
            Text(value, color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            Text(detail, color = BotBladeTokens.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun RuntimeControlCard(
    runtimeState: String,
    message: DashboardMessage?,
    controls: DashboardControlState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onOpenLogs: () -> Unit,
    onClearMessage: () -> Unit,
) {
    WorkstationCard(accent = BotBladeTokens.HotPink) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                SectionTitle("Runtime controls")
                Text("Status: $runtimeState", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 6.dp))
            }
            StatusChip(if (controls.running) "Online" else "Stopped", if (controls.running) StatusTone.Success else StatusTone.Warning)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BladeButton("Start runtime", onClick = onStart, enabled = controls.canStart, modifier = Modifier.weight(1f))
                BladeOutlinedButton("Stop runtime", onClick = onStop, enabled = controls.canStop, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BladeOutlinedButton("Restart runtime", onClick = onRestart, enabled = controls.canRestart, modifier = Modifier.weight(1f))
                BladeOutlinedButton("Open Logs", onClick = onOpenLogs, modifier = Modifier.weight(1f))
            }
        }
        if (message != null) {
            Surface(
                color = BotBladeTokens.Ink,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (message.isError) BotBladeTokens.Danger else BotBladeTokens.Stroke),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                    Text(message.text, color = if (message.isError) BotBladeTokens.Danger else BotBladeTokens.Success, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClearMessage) { Text("Clear") }
                }
            }
        }
    }
}

@Composable
private fun BackendStatusCard(runtimeState: String, backendTone: StatusTone, runtimeSource: String) {
    WorkstationCard(accent = BotBladeTokens.BabyBlue) {
        SectionTitle("Backend status")
        Text("Backend URL", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 8.dp))
        Text(ApiConfig.baseUrl, color = BotBladeTokens.BabyBlue, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            StatusChip(if (runtimeState == "unknown") "Connection unknown" else "Backend reachable", backendTone)
            StatusChip(runtimeSource, StatusTone.Info)
        }
    }
}

@Composable
private fun QuickToolsGrid(
    onOpenEditor: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenDeployments: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogs: () -> Unit,
) {
    WorkstationCard(accent = BotBladeTokens.HotPink) {
        SectionTitle("Quick tools")
        Spacer(Modifier.height(10.dp))
        ToolEntry("Files / Editor", "Manage workspace files and code.", onClick = onOpenEditor, primary = true)
        ToolEntry("GitHub Discovery / Projects", "Import, select, and inspect workspaces.", onClick = onOpenProjects)
        ToolEntry("Deployments", "Build, deploy, and review release jobs.", onClick = onOpenDeployments)
        ToolEntry("Logs", "Open the full runtime log surface.", onClick = onOpenLogs)
        ToolEntry("Settings", "Appearance • Backend • Permissions • Network binding", onClick = onOpenSettings)
        ToolEntry("Security / Scan", "Coming next: project scans and hardening checks.", onClick = {}, enabled = false)
    }
}

@Composable
private fun ToolEntry(title: String, subtitle: String, onClick: () -> Unit, primary: Boolean = false, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (primary) BotBladeTokens.RaisedPanel else BotBladeTokens.Ink,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (primary) BotBladeTokens.HotPink else BotBladeTokens.Stroke),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = if (enabled) BotBladeTokens.BabyBlue else BotBladeTokens.Muted, fontWeight = FontWeight.Bold)
            Text(subtitle, color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun LogsPreviewCard(logs: List<String>, onOpenLogs: () -> Unit) {
    WorkstationCard(accent = BotBladeTokens.BabyBlue) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Live logs preview", modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onOpenLogs) { Text("Open Logs") }
        }
        if (logs.isEmpty()) {
            Text("No live logs yet. Start the backend or runtime to stream activity here.", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 8.dp))
        } else {
            LazyColumn(modifier = Modifier.height(220.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(logs.takeLast(25)) { line ->
                    Text(
                        line,
                        color = BotBladeTokens.Success,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().background(BotBladeTokens.Ink, RoundedCornerShape(10.dp)).padding(8.dp),
                    )
                }
            }
        }
    }
}
