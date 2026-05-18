package com.princess.royalscepter.ui.deployments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.royalscepter.R
import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.model.BuildSummary
import com.princess.royalscepter.data.model.DeploymentJobSummary
import com.princess.royalscepter.data.model.DeploymentTargetSummary
import com.princess.royalscepter.data.repository.BuildRepository
import com.princess.royalscepter.data.repository.DeploymentRepository
import com.princess.royalscepter.data.store.ActiveProjectStore
import kotlinx.coroutines.launch

class DeploymentsFragment : Fragment() {
    private val buildRepository = BuildRepository()
    private val deploymentRepository = DeploymentRepository()
    private var projectId: String? = null
    private var latestSuccessfulBuild: BuildSummary? = null
    private var selectedTarget: DeploymentTargetSummary? = null
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private lateinit var logs: TextView
    private lateinit var targetName: EditText
    private lateinit var targetType: EditText
    private lateinit var deployButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_deployments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val store = ActiveProjectStore(requireContext())
        projectId = store.getActiveProjectId()
        status = view.findViewById(R.id.deployments_status)
        list = view.findViewById(R.id.build_list_container)
        logs = view.findViewById(R.id.build_logs_text)
        targetName = view.findViewById(R.id.deployment_target_name)
        targetType = view.findViewById(R.id.deployment_target_type)
        deployButton = view.findViewById(R.id.deploy_latest_build_button)
        view.findViewById<TextView>(R.id.deployments_active_project).text = if (projectId == null) getString(R.string.active_project_none) else getString(R.string.active_project_value, store.getActiveProjectName() ?: projectId)

        view.findViewById<Button>(R.id.create_deployment_button).setOnClickListener { startBuild() }
        view.findViewById<Button>(R.id.create_target_button).setOnClickListener { createTarget() }
        deployButton.setOnClickListener { deployLatest() }
        view.findViewById<Button>(R.id.refresh_deployments_button).setOnClickListener { loadAll() }
        updateDeployButton()
        loadAll()
    }

    private fun loadAll() {
        if (projectId == null) {
            status.text = getString(R.string.select_project_first)
        }
        loadBuilds()
        loadTargets()
        loadDeployments()
        loadGitHubStatus()
    }

    private fun startBuild() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        status.text = "Starting build…"
        when (val result = buildRepository.createBuild(id)) {
            is ApiResult.Success -> { status.text = getString(R.string.build_started, result.data.buildId, result.data.status); loadBuilds() }
            is ApiResult.Error -> status.text = "Error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun loadBuilds() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        when (val result = buildRepository.listBuilds(id)) {
            is ApiResult.Success -> { latestSuccessfulBuild = result.data.firstOrNull { it.status == "succeeded" }; updateDeployButton() }
            is ApiResult.Error -> status.text = "Build error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun loadTargets() = lifecycleScope.launch {
        when (val result = deploymentRepository.listTargets()) {
            is ApiResult.Success -> { selectedTarget = result.data.firstOrNull(); renderTargets(result.data); updateDeployButton() }
            is ApiResult.Error -> status.text = "Target error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun loadDeployments() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        when (val result = deploymentRepository.listDeployments(id)) {
            is ApiResult.Success -> renderDeployments(result.data)
            is ApiResult.Error -> status.text = "Deployment error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun loadGitHubStatus() = lifecycleScope.launch {
        when (val result = deploymentRepository.getGitHubStatus()) {
            is ApiResult.Success -> logs.text = "GitHub: ${if (result.data.connected) "connected" else "disconnected"}. ${result.data.message.orEmpty()}"
            is ApiResult.Error -> logs.text = "GitHub status unavailable: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun createTarget() = lifecycleScope.launch {
        val name = targetName.text.toString().ifBlank { "Local Process" }
        val type = targetType.text.toString().ifBlank { "local_process" }
        when (val result = deploymentRepository.createTarget(name, type)) {
            is ApiResult.Success -> { status.text = "Created target ${result.data.name}."; loadTargets() }
            is ApiResult.Error -> status.text = "Error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun deployLatest() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        val build = latestSuccessfulBuild ?: return@launch
        val target = selectedTarget ?: return@launch
        when (val result = deploymentRepository.createDeployment(id, target.id, build.buildId)) {
            is ApiResult.Success -> { status.text = "Deployment ${result.data.deploymentId}: ${result.data.status}"; loadDeployments() }
            is ApiResult.Error -> status.text = "Error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun renderTargets(targets: List<DeploymentTargetSummary>) {
        targets.forEach { target ->
            val row = Button(requireContext()).apply {
                text = "Target: ${target.name} (${target.type})\nTap to test/select"
                isAllCaps = false
                setOnClickListener { selectedTarget = target; updateDeployButton(); testTarget(target) }
            }
            list.addView(row)
        }
    }

    private fun testTarget(target: DeploymentTargetSummary) = lifecycleScope.launch {
        when (val result = deploymentRepository.testTarget(target.id)) {
            is ApiResult.Success -> status.text = "Target test: ${result.data.status} - ${result.data.message}"
            is ApiResult.Error -> status.text = "Target test failed: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun renderDeployments(deployments: List<DeploymentJobSummary>) {
        deployments.forEach { deployment ->
            val row = Button(requireContext()).apply {
                text = "Deployment: ${deployment.deploymentId}\nStatus: ${deployment.status}\nBuild: ${deployment.buildId}\n${deployment.errorMessage ?: ""}"
                isAllCaps = false
                if (deployment.status == "failed") setTextColor(Color.RED)
                setOnClickListener { loadDeploymentLogs(deployment) }
            }
            list.addView(row)
        }
        status.text = "Loaded deployment data. Latest successful build: ${latestSuccessfulBuild?.buildId ?: "none"}."
    }

    private fun loadDeploymentLogs(deployment: DeploymentJobSummary) = lifecycleScope.launch {
        val id = projectId ?: return@launch
        when (val result = deploymentRepository.getDeploymentLogs(id, deployment.deploymentId)) {
            is ApiResult.Success -> logs.text = result.data.ifBlank { "No logs captured." }
            is ApiResult.Error -> logs.text = "Error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun updateDeployButton() {
        deployButton.isEnabled = projectId != null && latestSuccessfulBuild != null && selectedTarget != null
    }
}
