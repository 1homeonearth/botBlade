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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.princess.botblade.MainActivity
import com.princess.botblade.R
import com.princess.botblade.data.store.ActiveProjectStore
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
                        onOpenEditor = { navigateTo(R.id.navigation_editor) },
                        onOpenOps = { navigateTo(R.id.navigation_deployments) },
                        onOpenSettings = { navigateTo(R.id.navigation_settings) },
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

    private fun openAddProjectFlow() {
        requireContext()
            .getSharedPreferences(WORKSTATION_PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OPEN_ADD_PROJECT, true)
            .apply()
        navigateTo(R.id.navigation_projects)
    }

    private fun navigateTo(itemId: Int) {
        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = itemId
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
    onOpenEditor: () -> Unit,
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
            .background(BotBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("BotBlade", color = BabyBlue, fontWeight = FontWeight.Bold)
                Text("Command Center", color = HotPink, fontWeight = FontWeight.Bold)
            }
            Text("Point A → Z bot workstation", color = Muted, modifier = Modifier.padding(top = 8.dp))
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
                Text("A-to-Z flow", color = BabyBlue, fontWeight = FontWeight.Bold)
                Text("Create/import → scan → configure secrets → edit → build → run → inspect → deploy → release", color = Muted, modifier = Modifier.padding(top = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 18.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onCreateProject, modifier = Modifier.weight(1f)) { Text("01 Create") }
                        OutlinedButton(onClick = onOpenEditor, modifier = Modifier.weight(1f)) { Text("02 Scan") }
                        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) { Text("03 Vault") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onOpenEditor, modifier = Modifier.weight(1f)) { Text("04 Edit") }
                        OutlinedButton(onClick = onOpenEditor, modifier = Modifier.weight(1f)) { Text("05 Build") }
                        OutlinedButton(onClick = vm::start, enabled = controls.canStart, modifier = Modifier.weight(1f)) { Text("06 Run") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onOpenOps, modifier = Modifier.weight(1f)) { Text("07 Inspect") }
                        OutlinedButton(onClick = onOpenOps, modifier = Modifier.weight(1f)) { Text("08 Deploy") }
                        OutlinedButton(onClick = onOpenOps, modifier = Modifier.weight(1f)) { Text("09 Release") }
                    }
                }
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

        item {
            Text("Live logs", color = HotPink, fontWeight = FontWeight.Bold)
        }

        items(logs.takeLast(25)) { line ->
            Text(line, color = Success, modifier = Modifier.fillMaxWidth().background(Ink).padding(8.dp))
        }
    }
}

@Composable
private fun WorkstationCard(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Panel,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Stroke, RoundedCornerShape(22.dp)),
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
