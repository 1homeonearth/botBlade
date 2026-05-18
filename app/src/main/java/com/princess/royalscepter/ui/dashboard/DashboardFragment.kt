package com.princess.royalscepter.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.royalscepter.R
import com.princess.royalscepter.data.api.ApiConfig
import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.model.RuntimeStatusResponse
import com.princess.royalscepter.data.repository.DashboardRepository
import com.princess.royalscepter.data.store.ActiveProjectStore
import com.princess.royalscepter.ui.common.copyToClipboard
import com.princess.royalscepter.ui.common.visibleWhen
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private val repository = DashboardRepository()
    private var currentBotStatus: String? = null
    private var activeProjectId: String? = null

    private lateinit var activeProjectStore: ActiveProjectStore
    private lateinit var activeProjectText: TextView
    private lateinit var statusText: TextView
    private lateinit var messageText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var refreshButton: Button
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var restartButton: Button
    private lateinit var logsButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activeProjectStore = ActiveProjectStore(requireContext())
        activeProjectText = view.findViewById(R.id.active_project_text)
        statusText = view.findViewById(R.id.bot_status_text)
        messageText = view.findViewById(R.id.dashboard_message_text)
        progress = view.findViewById(R.id.dashboard_progress)
        refreshButton = view.findViewById(R.id.refresh_status_button)
        startButton = view.findViewById(R.id.start_bot_button)
        stopButton = view.findViewById(R.id.stop_bot_button)
        restartButton = view.findViewById(R.id.restart_bot_button)
        logsButton = view.findViewById(R.id.view_runtime_logs_button)

        refreshButton.setOnClickListener { loadBotStatus() }
        startButton.setOnClickListener { runtimeAction("start") }
        stopButton.setOnClickListener { runtimeAction("stop") }
        restartButton.setOnClickListener { runtimeAction("restart") }
        logsButton.setOnClickListener {
            activeProjectId?.let { requireContext().copyToClipboard(getString(R.string.dashboard_copy_active_project_id), it) }
            messageText.text = getString(R.string.open_deployments_for_logs)
        }

        showUnknownState()
        updateActiveProject()
        loadBotStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::activeProjectStore.isInitialized) {
            updateActiveProject()
            loadBotStatus()
        }
    }

    private fun showUnknownState() {
        currentBotStatus = null
        statusText.text = getString(R.string.bot_status_unknown)
        messageText.text = checklistText(getString(R.string.dashboard_empty_message))
        updateActionButtons(false)
    }

    private fun updateActiveProject() {
        activeProjectId = activeProjectStore.getActiveProjectId()
        val activeProjectName = activeProjectStore.getActiveProjectName()
        activeProjectText.text = if (activeProjectId == null) getString(R.string.active_project_none) else getString(R.string.active_project_value, activeProjectName ?: activeProjectId)
    }

    private fun setLoading(isLoading: Boolean) {
        progress.visibleWhen(isLoading)
        refreshButton.isEnabled = !isLoading
        if (isLoading) {
            startButton.isEnabled = false
            stopButton.isEnabled = false
            restartButton.isEnabled = false
            logsButton.isEnabled = false
            messageText.text = checklistText(getString(R.string.dashboard_loading_message))
        } else {
            updateActionButtons(true)
        }
    }

    private fun loadBotStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            renderLoading()
            when (val result = repository.getBotStatus(activeProjectId)) {
                ApiResult.Loading -> renderLoading()
                is ApiResult.Success -> renderBotStatus(result.data)
                is ApiResult.Error -> renderError(result.message)
            }
        }
    }

    private fun runtimeAction(action: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            renderLoading()
            when (val result = repository.runtimeAction(activeProjectId, action, currentBotStatus)) {
                ApiResult.Loading -> renderLoading()
                is ApiResult.Success -> renderBotStatus(result.data)
                is ApiResult.Error -> renderError(result.message)
            }
        }
    }

    private fun renderLoading() = setLoading(true)

    private fun renderBotStatus(response: RuntimeStatusResponse) {
        setLoading(false)
        currentBotStatus = normalizeStatus(response.status)
        statusText.text = getString(R.string.bot_status_value, currentBotStatus ?: getString(R.string.unknown))
        val message = response.message ?: if (activeProjectId == null) getString(R.string.dashboard_legacy_status_loaded) else getString(R.string.dashboard_project_status_loaded)
        messageText.text = checklistText(message)
        updateActionButtons(true)
    }

    private fun renderError(message: String) {
        setLoading(false)
        currentBotStatus = "unknown"
        statusText.text = getString(R.string.bot_status_unknown)
        messageText.text = checklistText(getString(R.string.dashboard_error_message, message) + "\n" + getString(R.string.dashboard_retry_detail))
        updateActionButtons(false)
    }

    private fun updateActionButtons(backendConnected: Boolean) {
        val status = currentBotStatus
        val canAct = backendConnected && status != null && status != "unknown"
        startButton.isEnabled = canAct && status == "stopped"
        stopButton.isEnabled = canAct && status == "running"
        restartButton.isEnabled = canAct && status == "running"
        logsButton.isEnabled = activeProjectId != null
    }

    private fun checklistText(prefix: String): String {
        val projectName = activeProjectStore.getActiveProjectName() ?: activeProjectId.orEmpty()
        return listOf(
            prefix,
            "",
            getString(R.string.first_run_checklist_title),
            if (ApiConfig.baseUrl.isNotBlank()) getString(R.string.first_run_backend_url_done) else getString(R.string.first_run_backend_url_todo),
            if (activeProjectId != null) getString(R.string.first_run_project_done, projectName) else getString(R.string.first_run_project_todo),
            getString(R.string.first_run_secret_todo),
            getString(R.string.first_run_discord_ids_todo),
            getString(R.string.first_run_generate_todo),
            getString(R.string.first_run_build_todo),
            getString(R.string.first_run_run_deploy_todo),
        ).joinToString("\n")
    }

    private fun normalizeStatus(status: String?): String = status?.lowercase()?.ifBlank { "unknown" } ?: "unknown"
}
