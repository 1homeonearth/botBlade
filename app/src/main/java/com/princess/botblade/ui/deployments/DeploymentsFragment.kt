package com.princess.botblade.ui.deployments

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
import com.princess.botblade.R
import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.model.BuildSummary
import com.princess.botblade.data.model.DeploymentJobSummary
import com.princess.botblade.data.model.DeploymentTargetSummary
import com.princess.botblade.data.repository.BuildRepository
import com.princess.botblade.data.repository.DeploymentRepository
import com.princess.botblade.data.store.ActiveProjectStore
import kotlinx.coroutines.launch

class DeploymentsFragment : Fragment() {
    companion object {
        private const val ARG_PROJECT_ID = "project_id"
        private const val ARG_PROJECT_NAME = "project_name"

        fun buildArgs(projectId: String?, projectName: String?): Bundle = Bundle().apply {
            putString(ARG_PROJECT_ID, projectId)
            putString(ARG_PROJECT_NAME, projectName)
        }

        fun newInstance(args: Bundle = Bundle()): DeploymentsFragment = DeploymentsFragment().apply {
            arguments = args
        }
    }
    private val buildRepository = BuildRepository()
    private val deploymentRepository = DeploymentRepository()
    private var projectId: String? = null
    private var projectName: String? = null
    private var latestSuccessfulBuild: BuildSummary? = null
    private var selectedTarget: DeploymentTargetSummary? = null
    private var targetsById: Map<String, DeploymentTargetSummary> = emptyMap()
    private var availableTargets: List<DeploymentTargetSummary> = emptyList()
    private var recentDeployments: List<DeploymentJobSummary> = emptyList()
    private var hasBuilds: Boolean = false
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private lateinit var logs: TextView
    private var githubStatusLine: String = ""
    private lateinit var targetName: EditText
    private lateinit var targetType: EditText
    private lateinit var deployButton: Button
    private lateinit var createBuildButton: Button
    private lateinit var createTargetButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_deployments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val store = ActiveProjectStore(requireContext())
        projectId = arguments?.getString(ARG_PROJECT_ID) ?: store.getActiveProjectId()
        projectName = arguments?.getString(ARG_PROJECT_NAME) ?: store.getActiveProjectName()
        status = view.findViewById(R.id.deployments_status)
        list = view.findViewById(R.id.build_list_container)
        logs = view.findViewById(R.id.build_logs_text)
        targetName = view.findViewById(R.id.deployment_target_name)
        targetType = view.findViewById(R.id.deployment_target_type)
        deployButton = view.findViewById(R.id.deploy_latest_build_button)
        view.findViewById<TextView>(R.id.deployments_active_project).text = if (projectId == null) getString(R.string.active_project_none) else getString(R.string.active_project_value, projectName ?: projectId)

        createBuildButton = view.findViewById(R.id.create_deployment_button)
        createTargetButton = view.findViewById(R.id.create_target_button)
        createBuildButton.setOnClickListener { startBuild() }
        createTargetButton.setOnClickListener { createTarget() }
        deployButton.setOnClickListener { deployLatest() }
        view.findViewById<Button>(R.id.refresh_deployments_button).setOnClickListener { loadAll() }
        view.findViewById<Button>(R.id.release_check_button).setOnClickListener { renderReleaseChecklist() }
        updateDeployButton()
        refreshOnEntry()
    }

    private fun refreshOnEntry() {
        loadAll()
    }

    private fun loadAll() {
        githubStatusLine = ""
        if (projectId == null) {
            status.text = getString(R.string.select_project_first)
            list.removeAllViews()
            updateDeployButton()
            loadGitHubStatus()
            return
        }
        loadBuilds()
        loadTargets()
        loadDeployments()
        loadGitHubStatus()
    }

    private fun renderReleaseChecklist() {
        logs.text = buildString {
            appendLine("Release readiness")
            appendLine("Project: ${projectName ?: projectId ?: "none"}")
            appendLine("Active project: ${if (projectId != null) "yes" else "no"}")
            appendLine("Successful build: ${latestSuccessfulBuild?.buildId ?: "needed"}")
            appendLine("Deployment target: ${selectedTarget?.name ?: "needed"}")
            appendLine("Secrets: verify in Settings / Vault")
            appendLine("Logs: inspect this terminal pane after deploy")
            appendLine()
            appendLine("Ship order")
            appendLine("1. Scan and edit in Editor.")
            appendLine("2. Add missing secrets in Settings / Vault.")
            appendLine("3. Start a build and wait for success.")
            appendLine("4. Create or select a deployment target.")
            appendLine("5. Deploy latest successful build.")
            appendLine("6. Inspect logs and write rollback notes.")
        }
        status.text = if (projectId == null) getString(R.string.select_project_first) else "Release checklist generated."
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
            is ApiResult.Success -> {
                hasBuilds = result.data.isNotEmpty()
                latestSuccessfulBuild = result.data
                    .asSequence()
                    .filter { it.status == "succeeded" }
                    .maxByOrNull { it.finishedAt ?: it.startedAt ?: "" }
                if (!hasBuilds) {
                    status.text = "No builds found. Create build to start deployment flow."
                    logs.text = "No builds available. Tap \"Create build\" to generate a deployable artifact."
                }
                renderDeploymentList()
                updateDeployButton()
            }
            is ApiResult.Error -> status.text = "Build error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun loadTargets() = lifecycleScope.launch {
        when (val result = deploymentRepository.listTargets()) {
            is ApiResult.Success -> {
                availableTargets = result.data
                targetsById = result.data.associateBy { it.id }
                if (selectedTarget == null || targetsById[selectedTarget?.id] == null) {
                    selectedTarget = result.data.firstOrNull()
                }
                if (result.data.isEmpty()) {
                    selectedTarget = null
                    status.text = "No deployment targets found. Add target to continue."
                    logs.text = "No targets available. Tap \"Add target\" to configure a deployment destination."
                }
                renderDeploymentList()
                updateDeployButton()
            }
            is ApiResult.Error -> status.text = "Target error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun loadDeployments() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        when (val result = deploymentRepository.listDeployments(id)) {
            is ApiResult.Success -> {
                recentDeployments = result.data
                renderDeploymentList()
            }
            is ApiResult.Error -> status.text = "Deployment error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun loadGitHubStatus() = lifecycleScope.launch {
        githubStatusLine = when (val result = deploymentRepository.getGitHubStatus()) {
            is ApiResult.Success -> "GitHub: ${if (result.data.connected) "connected" else "disconnected"}. ${result.data.message.orEmpty()}"
            is ApiResult.Error -> "GitHub status unavailable: ${result.message}"
            ApiResult.Loading -> githubStatusLine
        }
        if (logs.text.isNullOrBlank()) {
            logs.text = githubStatusLine
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

    private fun renderDeploymentList() {
        if (!hasBuilds) {
            showEmptyStateAction(
                message = "No builds found for this project.",
                actionLabel = "Create build",
                action = ::startBuild,
            )
            return
        }
        if (availableTargets.isEmpty()) {
            showEmptyStateAction(
                message = "No deployment targets found.",
                actionLabel = "Add target",
                action = ::focusCreateTarget,
            )
            return
        }

        list.removeAllViews()
        availableTargets.forEach { target ->
            val row = Button(requireContext()).apply {
                val unsupported = target.capabilities.actions.filterValues { supported -> !supported }.keys.sorted().joinToString(", ").ifBlank { "none" }
                val supported = target.capabilities.actions.filterValues { supported -> supported }.keys.sorted().joinToString(", ").ifBlank { "none" }
                text = "Target: ${target.name} (${target.type})\nSupported: $supported\nUnsupported: $unsupported\nTap to test/select"
                isAllCaps = false
                setOnClickListener { selectedTarget = target; updateDeployButton(); testTarget(target) }
            }
            list.addView(row)
        }

        recentDeployments.forEach { deployment ->
            val row = Button(requireContext()).apply {
                val target = targetsById[deployment.targetId]
                val caps = target?.capabilities?.actions.orEmpty()
                val restartSupport = if (caps["restart"] == true) "Restart supported" else "Restart unsupported"
                val rollbackSupport = if (caps["rollback"] == true) "Rollback supported" else "Rollback unsupported"
                text = "Deployment: ${deployment.deploymentId}\nStatus: ${deployment.status}\nBuild: ${deployment.buildId}\n$restartSupport · $rollbackSupport\nTap for logs/status"
                isAllCaps = false
                if (deployment.status == "failed") setTextColor(Color.RED)
                setOnClickListener { loadDeploymentDetails(deployment) }
            }
            list.addView(row)
        }

        status.text = "Loaded deployment data. Latest successful build: ${latestSuccessfulBuild?.buildId ?: "none"}."
        recentDeployments.firstOrNull()?.let { loadDeploymentDetails(it) }
    }

    private fun loadDeploymentDetails(deployment: DeploymentJobSummary) = lifecycleScope.launch {
        val id = projectId ?: return@launch
        val target = targetsById[deployment.targetId]
        val canRestart = target?.capabilities?.actions?.get("restart") == true
        val canRollback = target?.capabilities?.actions?.get("rollback") == true
        val statusResult = deploymentRepository.getDeploymentStatus(id, deployment.deploymentId)
        val logResult = deploymentRepository.getDeploymentLogs(id, deployment.deploymentId)
        val statusText = when (statusResult) {
            is ApiResult.Success -> "Adapter status: ${statusResult.data.status ?: "unknown"} ${statusResult.data.message.orEmpty()}"
            is ApiResult.Error -> "Adapter status unavailable: ${statusResult.message}"
            ApiResult.Loading -> "Adapter status loading…"
        }
        val logText = when (logResult) {
            is ApiResult.Success -> logResult.data.ifBlank { "No logs captured." }
            is ApiResult.Error -> "Error: ${logResult.message}"
            ApiResult.Loading -> "Logs loading…"
        }
        val deploymentDetails = "$statusText\nActions: restart=${if (canRestart) "supported" else "unsupported"}, rollback=${if (canRollback) "supported" else "unsupported"}\n\n$logText"
        logs.text = if (githubStatusLine.isBlank()) deploymentDetails else "$githubStatusLine\n\n$deploymentDetails"
        if (!canRestart || !canRollback) status.text = "Unsupported deployment actions are disabled by adapter capabilities."
    }

    private fun restartDeployment(deployment: DeploymentJobSummary) = lifecycleScope.launch {
        val id = projectId ?: return@launch
        if (targetsById[deployment.targetId]?.capabilities?.actions?.get("restart") != true) { status.text = "Restart is unsupported by this adapter."; return@launch }
        when (val result = deploymentRepository.restartDeployment(id, deployment.deploymentId)) {
            is ApiResult.Success -> status.text = result.data.message ?: "Restart requested."
            is ApiResult.Error -> status.text = "Restart failed: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun rollbackDeployment(deployment: DeploymentJobSummary) = lifecycleScope.launch {
        val id = projectId ?: return@launch
        if (targetsById[deployment.targetId]?.capabilities?.actions?.get("rollback") != true) { status.text = "Rollback is unsupported by this adapter."; return@launch }
        when (val result = deploymentRepository.rollbackDeployment(id, deployment.deploymentId)) {
            is ApiResult.Success -> { status.text = "Rollback result: ${result.data.status}"; loadDeployments() }
            is ApiResult.Error -> status.text = "Rollback failed: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }


    private fun showEmptyStateAction(message: String, actionLabel: String, action: () -> Unit) {
        list.removeAllViews()
        val emptyMessage = TextView(requireContext()).apply {
            text = message
            setPadding(8, 8, 8, 12)
        }
        val actionButton = Button(requireContext()).apply {
            text = actionLabel
            setOnClickListener { action() }
        }
        list.addView(emptyMessage)
        list.addView(actionButton)
    }

    private fun focusCreateTarget() {
        targetName.requestFocus()
        targetType.requestFocus()
    }

    private fun updateDeployButton() {
        deployButton.isEnabled = projectId != null && latestSuccessfulBuild != null && selectedTarget != null
    }
}