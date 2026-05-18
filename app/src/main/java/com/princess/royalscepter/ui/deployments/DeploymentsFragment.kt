package com.princess.royalscepter.ui.deployments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.royalscepter.R
import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.model.BuildSummary
import com.princess.royalscepter.data.repository.BuildRepository
import com.princess.royalscepter.data.store.ActiveProjectStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeploymentsFragment : Fragment() {
    private val repository = BuildRepository()
    private var projectId: String? = null
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private lateinit var logs: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_deployments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        projectId = ActiveProjectStore(requireContext()).getActiveProjectId()
        status = view.findViewById(R.id.deployments_status)
        list = view.findViewById(R.id.build_list_container)
        logs = view.findViewById(R.id.build_logs_text)
        val button = view.findViewById<Button>(R.id.create_deployment_button)
        button.isEnabled = projectId != null
        if (projectId == null) {
            status.text = getString(R.string.select_project_first)
            return
        }
        button.setOnClickListener { startBuild() }
        loadBuilds()
    }

    private fun startBuild() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        status.text = "Starting build…"
        when (val result = repository.createBuild(id)) {
            is ApiResult.Success -> {
                status.text = getString(R.string.build_started, result.data.buildId, result.data.status)
                renderBuilds(listOf(result.data))
                pollBuild(result.data.buildId)
            }
            is ApiResult.Error -> status.text = "Error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun loadBuilds() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        status.text = "Loading builds…"
        when (val result = repository.listBuilds(id)) {
            is ApiResult.Success -> renderBuilds(result.data)
            is ApiResult.Error -> status.text = "Error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun pollBuild(buildId: String) = lifecycleScope.launch {
        val id = projectId ?: return@launch
        repeat(60) {
            delay(2_000)
            when (val result = repository.getBuild(id, buildId)) {
                is ApiResult.Success -> {
                    status.text = "Build ${result.data.buildId}: ${result.data.status}"
                    loadBuilds()
                    if (result.data.status in terminalStatuses) return@launch
                }
                is ApiResult.Error -> {
                    status.text = "Error: ${result.message}"
                    return@launch
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private fun loadLogs(build: BuildSummary) = lifecycleScope.launch {
        val id = projectId ?: return@launch
        logs.text = "Loading logs…"
        when (val result = repository.getBuildLogs(id, build.buildId)) {
            is ApiResult.Success -> logs.text = result.data.ifBlank { "No logs captured." }
            is ApiResult.Error -> logs.text = "Error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun renderBuilds(builds: List<BuildSummary>) {
        list.removeAllViews()
        if (builds.isEmpty()) {
            status.text = getString(R.string.deployments_empty_message)
            return
        }
        builds.forEach { build ->
            val row = Button(requireContext()).apply {
                text = "${build.buildId}\nStatus: ${build.status}\nStarted: ${build.startedAt ?: "unknown"}\nFinished: ${build.finishedAt ?: "active"}\n${build.errorMessage ?: ""}"
                isAllCaps = false
                if (build.status == "failed") setTextColor(Color.RED)
                setOnClickListener { loadLogs(build) }
            }
            list.addView(row)
        }
        status.text = "Loaded ${builds.size} build(s). Tap a build for logs."
    }

    private companion object {
        val terminalStatuses = setOf("succeeded", "failed", "canceled")
    }
}
