// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.dashboard  // line 7: executes this statement as part of this file's behavior

import android.os.Bundle  // line 9: executes this statement as part of this file's behavior
import android.os.SystemClock  // line 10: executes this statement as part of this file's behavior
import android.view.View  // line 11: executes this statement as part of this file's behavior
import androidx.compose.foundation.background  // line 12: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Arrangement  // line 13: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Box  // line 14: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Column  // line 15: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Row  // line 16: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.fillMaxSize  // line 17: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.fillMaxWidth  // line 18: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.height  // line 19: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.padding  // line 20: executes this statement as part of this file's behavior
import androidx.compose.foundation.lazy.LazyColumn  // line 21: executes this statement as part of this file's behavior
import androidx.compose.foundation.lazy.items  // line 22: executes this statement as part of this file's behavior
import androidx.compose.material3.Button  // line 23: executes this statement as part of this file's behavior
import androidx.compose.material3.Text  // line 24: executes this statement as part of this file's behavior
import androidx.compose.runtime.Composable  // line 25: executes this statement as part of this file's behavior
import androidx.compose.runtime.collectAsState  // line 26: executes this statement as part of this file's behavior
import androidx.compose.runtime.getValue  // line 27: executes this statement as part of this file's behavior
import androidx.compose.ui.Alignment  // line 28: executes this statement as part of this file's behavior
import androidx.compose.ui.Modifier  // line 29: executes this statement as part of this file's behavior
import androidx.compose.ui.graphics.Color  // line 30: executes this statement as part of this file's behavior
import androidx.compose.ui.unit.dp  // line 31: executes this statement as part of this file's behavior
import androidx.fragment.app.Fragment  // line 32: executes this statement as part of this file's behavior
import androidx.fragment.app.viewModels  // line 33: executes this statement as part of this file's behavior
import androidx.lifecycle.lifecycleScope  // line 34: executes this statement as part of this file's behavior
import com.princess.botblade.data.store.ActiveProjectStore  // line 35: executes this statement as part of this file's behavior
import com.princess.botblade.MainActivity  // line 36: executes this statement as part of this file's behavior
import com.princess.botblade.ui.theme.BotBladeTheme  // line 37: executes this statement as part of this file's behavior
import com.princess.botblade.ui.theme.isDynamicColorEnabled  // line 38: executes this statement as part of this file's behavior
import kotlinx.coroutines.launch  // line 39: executes this statement as part of this file's behavior
import androidx.compose.ui.platform.ComposeView  // line 40: executes this statement as part of this file's behavior

class DashboardFragment : Fragment() {  // line 42: executes this statement as part of this file's behavior
    private val viewModel: DashboardViewModel by viewModels()  // line 43: executes this statement as part of this file's behavior
    private var activeProjectId: String? = null  // line 44: executes this statement as part of this file's behavior
    private lateinit var activeProjectStore: ActiveProjectStore  // line 45: executes this statement as part of this file's behavior
    private val started = SystemClock.elapsedRealtime()  // line 46: executes this statement as part of this file's behavior

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View {  // line 48: executes this statement as part of this file's behavior
        activeProjectStore = ActiveProjectStore(requireContext())  // line 49: executes this statement as part of this file's behavior
        return ComposeView(requireContext()).apply {  // line 50: executes this statement as part of this file's behavior
            setContent {  // line 51: executes this statement as part of this file's behavior
                BotBladeTheme(useDynamicColor = isDynamicColorEnabled(requireContext())) { DashboardScreen(viewModel, started) { (activity as? MainActivity)?.openLogsScreen() } }  // line 52: executes this statement as part of this file's behavior
            }  // line 53: executes this statement as part of this file's behavior
        }  // line 54: executes this statement as part of this file's behavior
    }  // line 55: executes this statement as part of this file's behavior

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {  // line 57: executes this statement as part of this file's behavior
        super.onViewCreated(view, savedInstanceState)  // line 58: executes this statement as part of this file's behavior
        updateActiveProject()  // line 59: executes this statement as part of this file's behavior
        viewLifecycleOwner.lifecycleScope.launch { viewModel.connectLogs(); viewModel.loadStatus(activeProjectId) }  // line 60: executes this statement as part of this file's behavior
    }  // line 61: executes this statement as part of this file's behavior

    override fun onResume() { super.onResume(); updateActiveProject(); viewModel.connectLogs(); viewModel.loadStatus(activeProjectId) }  // line 63: executes this statement as part of this file's behavior
    override fun onPause() { viewModel.disconnectLogs(); super.onPause() }  // line 64: executes this statement as part of this file's behavior
    private fun updateActiveProject() { activeProjectId = activeProjectStore.getActiveProjectId() }  // line 65: executes this statement as part of this file's behavior
}  // line 66: executes this statement as part of this file's behavior

@Composable  // line 68: executes this statement as part of this file's behavior
private fun DashboardScreen(vm: DashboardViewModel, started: Long, onLogs: () -> Unit) {  // line 69: executes this statement as part of this file's behavior
    val status by vm.status.collectAsState()  // line 70: executes this statement as part of this file's behavior
    val logs by vm.logs.collectAsState()  // line 71: executes this statement as part of this file's behavior
    val controls by vm.controls.collectAsState()  // line 72: executes this statement as part of this file's behavior
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {  // line 73: executes this statement as part of this file's behavior
        Row(verticalAlignment = Alignment.CenterVertically) {  // line 74: executes this statement as part of this file's behavior
            Box(Modifier.padding(end = 12.dp).height(14.dp).fillMaxWidth(0.03f).background(if (controls.running) Color(0xFF2ECC71) else Color(0xFFE74C3C)))  // line 75: executes this statement as part of this file's behavior
            Column { Text("BotBlade Bot"); Text("Uptime: ${(SystemClock.elapsedRealtime() - started) / 1000}s · ${status?.status ?: "unknown"}") }  // line 76: executes this statement as part of this file's behavior
        }  // line 77: executes this statement as part of this file's behavior
        LazyColumn(Modifier.weight(1f).fillMaxWidth().background(Color.Black).padding(8.dp)) {  // line 78: executes this statement as part of this file's behavior
            items(logs) { line -> Text(line, color = Color(0xFF00FF9C)) }  // line 79: executes this statement as part of this file's behavior
        }  // line 80: executes this statement as part of this file's behavior
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {  // line 81: executes this statement as part of this file's behavior
            Button(onClick = vm::start, enabled = controls.canStart) { Text("Start") }  // line 82: executes this statement as part of this file's behavior
            Button(onClick = vm::stop, enabled = controls.canStop) { Text("Stop") }  // line 83: executes this statement as part of this file's behavior
            Button(onClick = vm::restart, enabled = controls.canRestart) { Text("Restart") }  // line 84: executes this statement as part of this file's behavior
            Button(onClick = onLogs) { Text("Logs") }  // line 85: executes this statement as part of this file's behavior
        }  // line 86: executes this statement as part of this file's behavior
    }  // line 87: executes this statement as part of this file's behavior
}  // line 88: executes this statement as part of this file's behavior
