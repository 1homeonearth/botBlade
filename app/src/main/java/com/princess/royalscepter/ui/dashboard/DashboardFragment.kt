package com.princess.royalscepter.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.royalscepter.R
import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.model.BotStatusResponse
import com.princess.royalscepter.data.model.BotToggleResponse
import com.princess.royalscepter.data.repository.DashboardRepository
import com.princess.royalscepter.data.store.ActiveProjectStore
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private val repository = DashboardRepository()
    private var currentBotStatus: String? = null

    private lateinit var activeProjectStore: ActiveProjectStore
    private lateinit var activeProjectText: TextView
    private lateinit var statusText: TextView
    private lateinit var messageText: TextView
    private lateinit var refreshButton: Button
    private lateinit var toggleButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activeProjectStore = ActiveProjectStore(requireContext())
        activeProjectText = view.findViewById(R.id.active_project_text)
        statusText = view.findViewById(R.id.bot_status_text)
        messageText = view.findViewById(R.id.dashboard_message_text)
        refreshButton = view.findViewById(R.id.refresh_status_button)
        toggleButton = view.findViewById(R.id.toggle_bot_button)

        refreshButton.setOnClickListener { loadBotStatus() }
        toggleButton.setOnClickListener { toggleBot() }

        showUnknownState()
        updateActiveProject()
        loadBotStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::activeProjectStore.isInitialized) {
            updateActiveProject()
        }
    }

    private fun showUnknownState() {
        currentBotStatus = null
        statusText.text = getString(R.string.bot_status_unknown)
        messageText.text = getString(R.string.dashboard_empty_message)
    }

    private fun updateActiveProject() {
        val activeProjectId = activeProjectStore.getActiveProjectId()
        activeProjectText.text = if (activeProjectId == null) {
            getString(R.string.active_project_none)
        } else {
            getString(R.string.active_project_value, activeProjectId)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        refreshButton.isEnabled = !isLoading
        toggleButton.isEnabled = !isLoading
        if (isLoading) {
            messageText.text = getString(R.string.dashboard_loading_message)
        }
    }

    private fun loadBotStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            renderLoading()
            when (val result = repository.getBotStatus()) {
                ApiResult.Loading -> renderLoading()
                is ApiResult.Success -> renderBotStatus(result.data)
                is ApiResult.Error -> renderError(result.message)
            }
        }
    }

    private fun toggleBot() {
        viewLifecycleOwner.lifecycleScope.launch {
            renderLoading()
            when (val result = repository.toggleBot(currentBotStatus)) {
                ApiResult.Loading -> renderLoading()
                is ApiResult.Success -> renderToggle(result.data)
                is ApiResult.Error -> renderError(result.message)
            }
        }
    }

    private fun renderLoading() {
        setLoading(true)
    }

    private fun renderBotStatus(response: BotStatusResponse) {
        setLoading(false)
        currentBotStatus = response.status
        statusText.text = getString(R.string.bot_status_value, response.status.ifBlank { getString(R.string.unknown) })
        messageText.text = response.message ?: getString(R.string.dashboard_status_loaded)
    }

    private fun renderToggle(response: BotToggleResponse) {
        setLoading(false)
        currentBotStatus = response.status ?: currentBotStatus
        statusText.text = getString(
            R.string.bot_status_value,
            currentBotStatus?.ifBlank { getString(R.string.unknown) } ?: getString(R.string.unknown),
        )
        messageText.text = response.message ?: getString(R.string.dashboard_toggle_sent, response.action.orEmpty())
    }

    private fun renderError(message: String) {
        setLoading(false)
        currentBotStatus = null
        statusText.text = getString(R.string.bot_status_unknown)
        messageText.text = getString(R.string.dashboard_error_message, message)
    }
}
