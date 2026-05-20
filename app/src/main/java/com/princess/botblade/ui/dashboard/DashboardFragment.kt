package com.princess.botblade.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.princess.botblade.R
import com.princess.botblade.data.store.ActiveProjectStore
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private val viewModel: DashboardViewModel by viewModels()
    private var activeProjectId: String? = null
    private lateinit var activeProjectStore: ActiveProjectStore
    private lateinit var activeProjectText: TextView
    private lateinit var statusText: TextView
    private lateinit var messageText: TextView
    private lateinit var terminalText: TextView
    private lateinit var refreshButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activeProjectStore = ActiveProjectStore(requireContext())
        activeProjectText = view.findViewById(R.id.active_project_text)
        statusText = view.findViewById(R.id.bot_status_text)
        messageText = view.findViewById(R.id.dashboard_message_text)
        terminalText = view.findViewById(R.id.runtime_terminal_text)
        refreshButton = view.findViewById(R.id.refresh_status_button)
        refreshButton.setOnClickListener { viewModel.loadStatus(activeProjectId) }
        updateActiveProject()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.status.collect { it?.let { s -> statusText.text = getString(R.string.bot_status_value, s.status); messageText.text = s.message ?: "" } }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logs.collect { terminalText.text = it.joinToString("\n") }
        }
        viewModel.connectLogs()
        viewModel.loadStatus(activeProjectId)
    }

    override fun onResume() { super.onResume(); updateActiveProject(); viewModel.connectLogs(); viewModel.loadStatus(activeProjectId) }
    override fun onPause() { viewModel.disconnectLogs(); super.onPause() }

    private fun updateActiveProject() {
        activeProjectId = activeProjectStore.getActiveProjectId()
        val n = activeProjectStore.getActiveProjectName()
        activeProjectText.text = if (activeProjectId == null) getString(R.string.active_project_none) else getString(R.string.active_project_value, n ?: activeProjectId)
    }
}
