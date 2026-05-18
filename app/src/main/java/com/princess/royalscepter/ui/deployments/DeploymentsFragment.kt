package com.princess.royalscepter.ui.deployments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import com.princess.royalscepter.ui.common.addBodyText
import com.princess.royalscepter.ui.common.copyToClipboard
import com.princess.royalscepter.ui.common.materialListCard
import com.princess.royalscepter.ui.common.visibleWhen
import kotlinx.coroutines.launch

class DeploymentsFragment : Fragment() {
    private val buildRepository = BuildRepository()
    private val deploymentRepository = DeploymentRepository()
    private var projectId: String? = null
    private var latestSuccessfulBuild: BuildSummary? = null
    private var selectedTarget: DeploymentTargetSummary? = null
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var list: LinearLayout
    private lateinit var logs: TextView
    private lateinit var targetName: EditText
    private lateinit var targetType: EditText
    private lateinit var buildButton: Button
    private lateinit var targetButton: Button
    private lateinit var deployButton: Button
    private lateinit var refreshButton: Button
    private lateinit var copyLogsButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_deployments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val store = ActiveProjectStore(requireContext())
        projectId = store.getActiveProjectId()
        status = view.findViewById(R.id.deployments_status)
        progress = view.findViewById(R.id.deployments_progress)
        list = view.findViewById(R.id.build_list_container)
        logs = view.findViewById(R.id.build_logs_text)
        targetName = view.findViewById(R.id.deployment_target_name)
        targetType = view.findViewById(R.id.deployment_target_type)
        deployButton = view.findViewById(R.id.deploy_latest_build_button)
        buildButton = view.findViewById(R.id.create_deployment_button)
        targetButton = view.findViewById(R.id.create_target_button)
        refreshButton = view.findViewById(R.id.refresh_deployments_button)
        copyLogsButton = view.findViewById(R.id.copy_build_logs_button)
        view.findViewById<TextView>(R.id.deployments_active_project).text = if (projectId == null) getString(R.string.active_project_none) else getString(R.string.active_project_value, store.getActiveProjectName() ?: projectId)

        buildButton.setOnClickListener { startBuild() }
        targetButton.setOnClickListener { createTarget() }
        deployButton.setOnClickListener { deployLatest() }
        refreshButton.setOnClickListener { loadAll() }
        copyLogsButton.setOnClickListener { requireContext().copyToClipboard(getString(R.string.copy_logs), logs.text.toString()) }
        updateDeployButton()
        loadAll()
    }

    private fun loadAll() {
        list.removeAllViews()
        if (projectId == null) {
            status.text = getString(R.string.select_project_first)
            renderEmpty(getString(R.string.select_project_first))
            return
        }
        setBusy(getString(R.string.deployments_loading))
        loadBuilds()
        loadTargets()
        loadDeployments()
        loadGitHubStatus()
    }

    private fun startBuild() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy(getString(R.string.starting_build))
        when (val result = buildRepository.createBuild(id)) {
            is ApiResult.Success -> { setIdle(); status.text = getString(R.string.build_started, result.data.buildId, result.data.status); loadBuilds() }
            is ApiResult.Error -> setError(getString(R.string.error_value, result.message))
            ApiResult.Loading -> Unit
        }
    }

    private fun loadBuilds() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        when (val result = buildRepository.listBuilds(id)) {
            is ApiResult.Success -> { latestSuccessfulBuild = result.data.firstOrNull { it.status == "succeeded" }; setIdle(); updateDeployButton() }
            is ApiResult.Error -> setError(getString(R.string.build_error_value, result.message))
            ApiResult.Loading -> Unit
        }
    }

    private fun loadTargets() = lifecycleScope.launch {
        when (val result = deploymentRepository.listTargets()) {
            is ApiResult.Success -> { selectedTarget = result.data.firstOrNull(); renderTargets(result.data); updateDeployButton() }
            is ApiResult.Error -> setError(getString(R.string.target_error_value, result.message))
            ApiResult.Loading -> Unit
        }
    }

    private fun loadDeployments() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        when (val result = deploymentRepository.listDeployments(id)) {
            is ApiResult.Success -> renderDeployments(result.data)
            is ApiResult.Error -> setError(getString(R.string.deployment_error_value, result.message))
            ApiResult.Loading -> Unit
        }
    }

    private fun loadGitHubStatus() = lifecycleScope.launch {
        when (val result = deploymentRepository.getGitHubStatus()) {
            is ApiResult.Success -> logs.text = getString(R.string.github_status_line, if (result.data.connected) getString(R.string.connected_lower) else getString(R.string.disconnected_lower), result.data.message.orEmpty())
            is ApiResult.Error -> logs.text = getString(R.string.github_status_unavailable, result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun createTarget() = lifecycleScope.launch {
        setBusy(getString(R.string.settings_busy))
        val name = targetName.text.toString().ifBlank { getString(R.string.default_local_process_name) }
        val type = targetType.text.toString().ifBlank { getString(R.string.default_local_process_type) }
        when (val result = deploymentRepository.createTarget(name, type)) {
            is ApiResult.Success -> { setIdle(); status.text = getString(R.string.created_target, result.data.name); loadAll() }
            is ApiResult.Error -> setError(getString(R.string.error_value, result.message))
            ApiResult.Loading -> Unit
        }
    }

    private fun deployLatest() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        val build = latestSuccessfulBuild ?: return@launch
        val target = selectedTarget ?: return@launch
        setBusy(getString(R.string.settings_busy))
        when (val result = deploymentRepository.createDeployment(id, target.id, build.buildId)) {
            is ApiResult.Success -> { setIdle(); status.text = getString(R.string.deployment_started_value, result.data.deploymentId, result.data.status); loadAll() }
            is ApiResult.Error -> setError(getString(R.string.error_value, result.message))
            ApiResult.Loading -> Unit
        }
    }

    private fun renderTargets(targets: List<DeploymentTargetSummary>) {
        if (targets.isEmpty()) {
            list.addView(requireContext().materialListCard { addBodyText(getString(R.string.targets_empty_detail)) })
            return
        }
        targets.forEach { target ->
            list.addView(requireContext().materialListCard {
                addBodyText(getString(R.string.target_card_value, target.name, target.type))
                addBodyText(getString(R.string.tap_to_test_select))
                addView(Button(requireContext()).apply { text = getString(R.string.copy_id); isAllCaps = false; setOnClickListener { requireContext().copyToClipboard(getString(R.string.copy_id), target.id) } })
                setOnClickListener { selectedTarget = target; updateDeployButton(); testTarget(target) }
            })
        }
    }

    private fun testTarget(target: DeploymentTargetSummary) = lifecycleScope.launch {
        setBusy(getString(R.string.settings_busy))
        when (val result = deploymentRepository.testTarget(target.id)) {
            is ApiResult.Success -> { setIdle(); status.text = getString(R.string.target_test_value, result.data.status, result.data.message) }
            is ApiResult.Error -> setError(getString(R.string.target_test_failed, result.message))
            ApiResult.Loading -> Unit
        }
    }

    private fun renderDeployments(deployments: List<DeploymentJobSummary>) {
        if (deployments.isEmpty()) renderEmpty(getString(R.string.deployments_empty_detail))
        deployments.forEach { deployment ->
            list.addView(requireContext().materialListCard {
                val text = getString(R.string.deployment_card_value, deployment.deploymentId, deployment.status, deployment.buildId, deployment.errorMessage.orEmpty())
                addBodyText(text).apply { if (deployment.status == "failed") setTextColor(Color.RED) }
                addView(Button(requireContext()).apply { text = getString(R.string.copy_id); isAllCaps = false; setOnClickListener { requireContext().copyToClipboard(getString(R.string.copy_id), deployment.deploymentId) } })
                setOnClickListener { loadDeploymentLogs(deployment) }
            })
        }
        setIdle()
        status.text = getString(R.string.deployment_data_loaded, latestSuccessfulBuild?.buildId ?: getString(R.string.none))
    }

    private fun loadDeploymentLogs(deployment: DeploymentJobSummary) = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy(getString(R.string.settings_busy))
        when (val result = deploymentRepository.getDeploymentLogs(id, deployment.deploymentId)) {
            is ApiResult.Success -> { setIdle(); logs.text = result.data.ifBlank { getString(R.string.no_logs_captured) } }
            is ApiResult.Error -> { setIdle(); logs.text = getString(R.string.error_value, result.message) }
            ApiResult.Loading -> Unit
        }
    }

    private fun renderEmpty(message: String) {
        list.addView(requireContext().materialListCard {
            addBodyText(message)
            addView(Button(requireContext()).apply { text = getString(R.string.retry); setOnClickListener { loadAll() } })
        })
    }

    private fun setBusy(message: String) {
        progress.visibleWhen(true)
        buildButton.isEnabled = false
        targetButton.isEnabled = false
        refreshButton.isEnabled = false
        deployButton.isEnabled = false
        status.text = message
    }

    private fun setIdle() {
        progress.visibleWhen(false)
        buildButton.isEnabled = projectId != null
        targetButton.isEnabled = true
        refreshButton.isEnabled = true
        updateDeployButton()
    }

    private fun setError(message: String) {
        setIdle()
        status.text = message
        list.addView(requireContext().materialListCard {
            addBodyText(message)
            addView(Button(requireContext()).apply { text = getString(R.string.retry); setOnClickListener { loadAll() } })
        })
    }

    private fun updateDeployButton() {
        deployButton.isEnabled = projectId != null && latestSuccessfulBuild != null && selectedTarget != null && !progress.isShown
    }
}
