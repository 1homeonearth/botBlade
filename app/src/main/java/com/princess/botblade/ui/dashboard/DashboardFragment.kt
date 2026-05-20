package com.princess.botblade.ui.dashboard

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.princess.botblade.data.store.ActiveProjectStore
import com.princess.botblade.MainActivity
import com.princess.botblade.ui.theme.BotBladeTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.ComposeView

class DashboardFragment : Fragment() {
    private val viewModel: DashboardViewModel by viewModels()
    private var activeProjectId: String? = null
    private lateinit var activeProjectStore: ActiveProjectStore
    private val started = SystemClock.elapsedRealtime()

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View {
        activeProjectStore = ActiveProjectStore(requireContext())
        return ComposeView(requireContext()).apply {
            setContent {
                BotBladeTheme { DashboardScreen(viewModel, started) { (activity as? MainActivity)?.openLogsScreen() } }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateActiveProject()
        viewLifecycleOwner.lifecycleScope.launch { viewModel.connectLogs(); viewModel.loadStatus(activeProjectId) }
    }

    override fun onResume() { super.onResume(); updateActiveProject(); viewModel.connectLogs(); viewModel.loadStatus(activeProjectId) }
    override fun onPause() { viewModel.disconnectLogs(); super.onPause() }
    private fun updateActiveProject() { activeProjectId = activeProjectStore.getActiveProjectId() }
}

@Composable
private fun DashboardScreen(vm: DashboardViewModel, started: Long, onLogs: () -> Unit) {
    val status by vm.status.collectAsState()
    val logs by vm.logs.collectAsState()
    val controls by vm.controls.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(end = 12.dp).height(14.dp).fillMaxWidth(0.03f).background(if (controls.running) Color(0xFF2ECC71) else Color(0xFFE74C3C)))
            Column { Text("BotBlade Bot"); Text("Uptime: ${(SystemClock.elapsedRealtime() - started) / 1000}s · ${status?.status ?: "unknown"}") }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().background(Color.Black).padding(8.dp)) {
            items(logs) { line -> Text(line, color = Color(0xFF00FF9C)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = vm::start, enabled = controls.canStart) { Text("Start") }
            Button(onClick = vm::stop, enabled = controls.canStop) { Text("Stop") }
            Button(onClick = vm::restart, enabled = controls.canRestart) { Text("Restart") }
            Button(onClick = onLogs) { Text("Logs") }
        }
    }
}
