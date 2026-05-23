// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.deployments  // line 7: executes this statement as part of this file's behavior

import android.graphics.Color  // line 9: executes this statement as part of this file's behavior
import android.os.Bundle  // line 10: executes this statement as part of this file's behavior
import android.view.LayoutInflater  // line 11: executes this statement as part of this file's behavior
import android.view.View  // line 12: executes this statement as part of this file's behavior
import android.view.ViewGroup  // line 13: executes this statement as part of this file's behavior
import android.widget.Button  // line 14: executes this statement as part of this file's behavior
import android.widget.EditText  // line 15: executes this statement as part of this file's behavior
import android.widget.LinearLayout  // line 16: executes this statement as part of this file's behavior
import android.widget.TextView  // line 17: executes this statement as part of this file's behavior
import androidx.fragment.app.Fragment  // line 18: executes this statement as part of this file's behavior
import androidx.lifecycle.lifecycleScope  // line 19: executes this statement as part of this file's behavior
import com.princess.botblade.R  // line 20: executes this statement as part of this file's behavior
import com.princess.botblade.data.api.ApiResult  // line 21: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BuildSummary  // line 22: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentJobSummary  // line 23: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.DeploymentTargetSummary  // line 24: executes this statement as part of this file's behavior
import com.princess.botblade.data.repository.BuildRepository  // line 25: executes this statement as part of this file's behavior
import com.princess.botblade.data.repository.DeploymentRepository  // line 26: executes this statement as part of this file's behavior
import com.princess.botblade.data.store.ActiveProjectStore  // line 27: executes this statement as part of this file's behavior
import kotlinx.coroutines.launch  // line 28: executes this statement as part of this file's behavior

class DeploymentsFragment : Fragment() {  // line 30: executes this statement as part of this file's behavior
    private val buildRepository = BuildRepository()  // line 31: executes this statement as part of this file's behavior
    private val deploymentRepository = DeploymentRepository()  // line 32: executes this statement as part of this file's behavior
    private var projectId: String? = null  // line 33: executes this statement as part of this file's behavior
    private var latestSuccessfulBuild: BuildSummary? = null  // line 34: executes this statement as part of this file's behavior
    private var selectedTarget: DeploymentTargetSummary? = null  // line 35: executes this statement as part of this file's behavior
    private var targetsById: Map<String, DeploymentTargetSummary> = emptyMap()  // line 36: executes this statement as part of this file's behavior
    private lateinit var status: TextView  // line 37: executes this statement as part of this file's behavior
    private lateinit var list: LinearLayout  // line 38: executes this statement as part of this file's behavior
    private lateinit var logs: TextView  // line 39: executes this statement as part of this file's behavior
    private lateinit var targetName: EditText  // line 40: executes this statement as part of this file's behavior
    private lateinit var targetType: EditText  // line 41: executes this statement as part of this file's behavior
    private lateinit var deployButton: Button  // line 42: executes this statement as part of this file's behavior

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =  // line 44: executes this statement as part of this file's behavior
        inflater.inflate(R.layout.fragment_deployments, container, false)  // line 45: executes this statement as part of this file's behavior

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {  // line 47: executes this statement as part of this file's behavior
        super.onViewCreated(view, savedInstanceState)  // line 48: executes this statement as part of this file's behavior
        val store = ActiveProjectStore(requireContext())  // line 49: executes this statement as part of this file's behavior
        projectId = store.getActiveProjectId()  // line 50: executes this statement as part of this file's behavior
        status = view.findViewById(R.id.deployments_status)  // line 51: executes this statement as part of this file's behavior
        list = view.findViewById(R.id.build_list_container)  // line 52: executes this statement as part of this file's behavior
        logs = view.findViewById(R.id.build_logs_text)  // line 53: executes this statement as part of this file's behavior
        targetName = view.findViewById(R.id.deployment_target_name)  // line 54: executes this statement as part of this file's behavior
        targetType = view.findViewById(R.id.deployment_target_type)  // line 55: executes this statement as part of this file's behavior
        deployButton = view.findViewById(R.id.deploy_latest_build_button)  // line 56: executes this statement as part of this file's behavior
        view.findViewById<TextView>(R.id.deployments_active_project).text = if (projectId == null) getString(R.string.active_project_none) else getString(R.string.active_project_value, store.getActiveProjectName() ?: projectId)  // line 57: executes this statement as part of this file's behavior

        view.findViewById<Button>(R.id.create_deployment_button).setOnClickListener { startBuild() }  // line 59: executes this statement as part of this file's behavior
        view.findViewById<Button>(R.id.create_target_button).setOnClickListener { createTarget() }  // line 60: executes this statement as part of this file's behavior
        deployButton.setOnClickListener { deployLatest() }  // line 61: executes this statement as part of this file's behavior
        view.findViewById<Button>(R.id.refresh_deployments_button).setOnClickListener { loadAll() }  // line 62: executes this statement as part of this file's behavior
        updateDeployButton()  // line 63: executes this statement as part of this file's behavior
        loadAll()  // line 64: executes this statement as part of this file's behavior
    }  // line 65: executes this statement as part of this file's behavior

    private fun loadAll() {  // line 67: executes this statement as part of this file's behavior
        if (projectId == null) {  // line 68: executes this statement as part of this file's behavior
            status.text = getString(R.string.select_project_first)  // line 69: executes this statement as part of this file's behavior
        }  // line 70: executes this statement as part of this file's behavior
        list.removeAllViews()  // line 71: executes this statement as part of this file's behavior
        loadBuilds()  // line 72: executes this statement as part of this file's behavior
        loadTargets()  // line 73: executes this statement as part of this file's behavior
        loadDeployments()  // line 74: executes this statement as part of this file's behavior
        loadGitHubStatus()  // line 75: executes this statement as part of this file's behavior
    }  // line 76: executes this statement as part of this file's behavior

    private fun startBuild() = lifecycleScope.launch {  // line 78: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 79: executes this statement as part of this file's behavior
        status.text = "Starting build…"  // line 80: executes this statement as part of this file's behavior
        when (val result = buildRepository.createBuild(id)) {  // line 81: executes this statement as part of this file's behavior
            is ApiResult.Success -> { status.text = getString(R.string.build_started, result.data.buildId, result.data.status); loadBuilds() }  // line 82: executes this statement as part of this file's behavior
            is ApiResult.Error -> status.text = "Error: ${result.message}"  // line 83: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 84: executes this statement as part of this file's behavior
        }  // line 85: executes this statement as part of this file's behavior
    }  // line 86: executes this statement as part of this file's behavior

    private fun loadBuilds() = lifecycleScope.launch {  // line 88: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 89: executes this statement as part of this file's behavior
        when (val result = buildRepository.listBuilds(id)) {  // line 90: executes this statement as part of this file's behavior
            is ApiResult.Success -> { latestSuccessfulBuild = result.data.firstOrNull { it.status == "succeeded" }; updateDeployButton() }  // line 91: executes this statement as part of this file's behavior
            is ApiResult.Error -> status.text = "Build error: ${result.message}"  // line 92: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 93: executes this statement as part of this file's behavior
        }  // line 94: executes this statement as part of this file's behavior
    }  // line 95: executes this statement as part of this file's behavior

    private fun loadTargets() = lifecycleScope.launch {  // line 97: executes this statement as part of this file's behavior
        when (val result = deploymentRepository.listTargets()) {  // line 98: executes this statement as part of this file's behavior
            is ApiResult.Success -> { targetsById = result.data.associateBy { it.id }; selectedTarget = result.data.firstOrNull(); renderTargets(result.data); updateDeployButton() }  // line 99: executes this statement as part of this file's behavior
            is ApiResult.Error -> status.text = "Target error: ${result.message}"  // line 100: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 101: executes this statement as part of this file's behavior
        }  // line 102: executes this statement as part of this file's behavior
    }  // line 103: executes this statement as part of this file's behavior

    private fun loadDeployments() = lifecycleScope.launch {  // line 105: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 106: executes this statement as part of this file's behavior
        when (val result = deploymentRepository.listDeployments(id)) {  // line 107: executes this statement as part of this file's behavior
            is ApiResult.Success -> renderDeployments(result.data)  // line 108: executes this statement as part of this file's behavior
            is ApiResult.Error -> status.text = "Deployment error: ${result.message}"  // line 109: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 110: executes this statement as part of this file's behavior
        }  // line 111: executes this statement as part of this file's behavior
    }  // line 112: executes this statement as part of this file's behavior

    private fun loadGitHubStatus() = lifecycleScope.launch {  // line 114: executes this statement as part of this file's behavior
        when (val result = deploymentRepository.getGitHubStatus()) {  // line 115: executes this statement as part of this file's behavior
            is ApiResult.Success -> logs.text = "GitHub: ${if (result.data.connected) "connected" else "disconnected"}. ${result.data.message.orEmpty()}"  // line 116: executes this statement as part of this file's behavior
            is ApiResult.Error -> logs.text = "GitHub status unavailable: ${result.message}"  // line 117: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 118: executes this statement as part of this file's behavior
        }  // line 119: executes this statement as part of this file's behavior
    }  // line 120: executes this statement as part of this file's behavior

    private fun createTarget() = lifecycleScope.launch {  // line 122: executes this statement as part of this file's behavior
        val name = targetName.text.toString().ifBlank { "Local Process" }  // line 123: executes this statement as part of this file's behavior
        val type = targetType.text.toString().ifBlank { "local_process" }  // line 124: executes this statement as part of this file's behavior
        when (val result = deploymentRepository.createTarget(name, type)) {  // line 125: executes this statement as part of this file's behavior
            is ApiResult.Success -> { status.text = "Created target ${result.data.name}."; loadTargets() }  // line 126: executes this statement as part of this file's behavior
            is ApiResult.Error -> status.text = "Error: ${result.message}"  // line 127: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 128: executes this statement as part of this file's behavior
        }  // line 129: executes this statement as part of this file's behavior
    }  // line 130: executes this statement as part of this file's behavior

    private fun deployLatest() = lifecycleScope.launch {  // line 132: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 133: executes this statement as part of this file's behavior
        val build = latestSuccessfulBuild ?: return@launch  // line 134: executes this statement as part of this file's behavior
        val target = selectedTarget ?: return@launch  // line 135: executes this statement as part of this file's behavior
        when (val result = deploymentRepository.createDeployment(id, target.id, build.buildId)) {  // line 136: executes this statement as part of this file's behavior
            is ApiResult.Success -> { status.text = "Deployment ${result.data.deploymentId}: ${result.data.status}"; loadDeployments() }  // line 137: executes this statement as part of this file's behavior
            is ApiResult.Error -> status.text = "Error: ${result.message}"  // line 138: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 139: executes this statement as part of this file's behavior
        }  // line 140: executes this statement as part of this file's behavior
    }  // line 141: executes this statement as part of this file's behavior

    private fun renderTargets(targets: List<DeploymentTargetSummary>) {  // line 143: executes this statement as part of this file's behavior
        targets.forEach { target ->  // line 144: executes this statement as part of this file's behavior
            val row = Button(requireContext()).apply {  // line 145: executes this statement as part of this file's behavior
                val unsupported = target.capabilities.actions.filterValues { supported -> !supported }.keys.sorted().joinToString(", ").ifBlank { "none" }  // line 146: executes this statement as part of this file's behavior
                val supported = target.capabilities.actions.filterValues { supported -> supported }.keys.sorted().joinToString(", ").ifBlank { "none" }  // line 147: executes this statement as part of this file's behavior
                text = "Target: ${target.name} (${target.type})\nSupported: $supported\nUnsupported: $unsupported\nTap to test/select"  // line 148: executes this statement as part of this file's behavior
                isAllCaps = false  // line 149: executes this statement as part of this file's behavior
                setOnClickListener { selectedTarget = target; updateDeployButton(); testTarget(target) }  // line 150: executes this statement as part of this file's behavior
            }  // line 151: executes this statement as part of this file's behavior
            list.addView(row)  // line 152: executes this statement as part of this file's behavior
        }  // line 153: executes this statement as part of this file's behavior
    }  // line 154: executes this statement as part of this file's behavior

    private fun testTarget(target: DeploymentTargetSummary) = lifecycleScope.launch {  // line 156: executes this statement as part of this file's behavior
        when (val result = deploymentRepository.testTarget(target.id)) {  // line 157: executes this statement as part of this file's behavior
            is ApiResult.Success -> status.text = "Target test: ${result.data.status} - ${result.data.message}"  // line 158: executes this statement as part of this file's behavior
            is ApiResult.Error -> status.text = "Target test failed: ${result.message}"  // line 159: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 160: executes this statement as part of this file's behavior
        }  // line 161: executes this statement as part of this file's behavior
    }  // line 162: executes this statement as part of this file's behavior

    private fun renderDeployments(deployments: List<DeploymentJobSummary>) {  // line 164: executes this statement as part of this file's behavior
        deployments.forEach { deployment ->  // line 165: executes this statement as part of this file's behavior
            val row = Button(requireContext()).apply {  // line 166: executes this statement as part of this file's behavior
                val target = targetsById[deployment.targetId]  // line 167: executes this statement as part of this file's behavior
                val caps = target?.capabilities?.actions.orEmpty()  // line 168: executes this statement as part of this file's behavior
                val restartSupport = if (caps["restart"] == true) "Restart supported" else "Restart unsupported"  // line 169: executes this statement as part of this file's behavior
                val rollbackSupport = if (caps["rollback"] == true) "Rollback supported" else "Rollback unsupported"  // line 170: executes this statement as part of this file's behavior
                text = "Deployment: ${deployment.deploymentId}\nStatus: ${deployment.status}\nBuild: ${deployment.buildId}\n$restartSupport · $rollbackSupport\nTap for logs/status"  // line 171: executes this statement as part of this file's behavior
                isAllCaps = false  // line 172: executes this statement as part of this file's behavior
                if (deployment.status == "failed") setTextColor(Color.RED)  // line 173: executes this statement as part of this file's behavior
                setOnClickListener { loadDeploymentDetails(deployment) }  // line 174: executes this statement as part of this file's behavior
            }  // line 175: executes this statement as part of this file's behavior
            list.addView(row)  // line 176: executes this statement as part of this file's behavior
        }  // line 177: executes this statement as part of this file's behavior
        status.text = "Loaded deployment data. Latest successful build: ${latestSuccessfulBuild?.buildId ?: "none"}."  // line 178: executes this statement as part of this file's behavior
    }  // line 179: executes this statement as part of this file's behavior

    private fun loadDeploymentDetails(deployment: DeploymentJobSummary) = lifecycleScope.launch {  // line 181: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 182: executes this statement as part of this file's behavior
        val target = targetsById[deployment.targetId]  // line 183: executes this statement as part of this file's behavior
        val canRestart = target?.capabilities?.actions?.get("restart") == true  // line 184: executes this statement as part of this file's behavior
        val canRollback = target?.capabilities?.actions?.get("rollback") == true  // line 185: executes this statement as part of this file's behavior
        val statusResult = deploymentRepository.getDeploymentStatus(id, deployment.deploymentId)  // line 186: executes this statement as part of this file's behavior
        val logResult = deploymentRepository.getDeploymentLogs(id, deployment.deploymentId)  // line 187: executes this statement as part of this file's behavior
        val statusText = when (statusResult) {  // line 188: executes this statement as part of this file's behavior
            is ApiResult.Success -> "Adapter status: ${statusResult.data.status ?: "unknown"} ${statusResult.data.message.orEmpty()}"  // line 189: executes this statement as part of this file's behavior
            is ApiResult.Error -> "Adapter status unavailable: ${statusResult.message}"  // line 190: executes this statement as part of this file's behavior
            ApiResult.Loading -> "Adapter status loading…"  // line 191: executes this statement as part of this file's behavior
        }  // line 192: executes this statement as part of this file's behavior
        val logText = when (logResult) {  // line 193: executes this statement as part of this file's behavior
            is ApiResult.Success -> logResult.data.ifBlank { "No logs captured." }  // line 194: executes this statement as part of this file's behavior
            is ApiResult.Error -> "Error: ${logResult.message}"  // line 195: executes this statement as part of this file's behavior
            ApiResult.Loading -> "Logs loading…"  // line 196: executes this statement as part of this file's behavior
        }  // line 197: executes this statement as part of this file's behavior
        logs.text = "$statusText\nActions: restart=${if (canRestart) "supported" else "unsupported"}, rollback=${if (canRollback) "supported" else "unsupported"}\n\n$logText"  // line 198: executes this statement as part of this file's behavior
        if (!canRestart || !canRollback) status.text = "Unsupported deployment actions are disabled by adapter capabilities."  // line 199: executes this statement as part of this file's behavior
    }  // line 200: executes this statement as part of this file's behavior

    private fun restartDeployment(deployment: DeploymentJobSummary) = lifecycleScope.launch {  // line 202: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 203: executes this statement as part of this file's behavior
        if (targetsById[deployment.targetId]?.capabilities?.actions?.get("restart") != true) { status.text = "Restart is unsupported by this adapter."; return@launch }  // line 204: executes this statement as part of this file's behavior
        when (val result = deploymentRepository.restartDeployment(id, deployment.deploymentId)) {  // line 205: executes this statement as part of this file's behavior
            is ApiResult.Success -> status.text = result.data.message ?: "Restart requested."  // line 206: executes this statement as part of this file's behavior
            is ApiResult.Error -> status.text = "Restart failed: ${result.message}"  // line 207: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 208: executes this statement as part of this file's behavior
        }  // line 209: executes this statement as part of this file's behavior
    }  // line 210: executes this statement as part of this file's behavior

    private fun rollbackDeployment(deployment: DeploymentJobSummary) = lifecycleScope.launch {  // line 212: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 213: executes this statement as part of this file's behavior
        if (targetsById[deployment.targetId]?.capabilities?.actions?.get("rollback") != true) { status.text = "Rollback is unsupported by this adapter."; return@launch }  // line 214: executes this statement as part of this file's behavior
        when (val result = deploymentRepository.rollbackDeployment(id, deployment.deploymentId)) {  // line 215: executes this statement as part of this file's behavior
            is ApiResult.Success -> { status.text = "Rollback result: ${result.data.status}"; loadDeployments() }  // line 216: executes this statement as part of this file's behavior
            is ApiResult.Error -> status.text = "Rollback failed: ${result.message}"  // line 217: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 218: executes this statement as part of this file's behavior
        }  // line 219: executes this statement as part of this file's behavior
    }  // line 220: executes this statement as part of this file's behavior

    private fun updateDeployButton() {  // line 222: executes this statement as part of this file's behavior
        deployButton.isEnabled = projectId != null && latestSuccessfulBuild != null && selectedTarget != null  // line 223: executes this statement as part of this file's behavior
    }  // line 224: executes this statement as part of this file's behavior
}  // line 225: executes this statement as part of this file's behavior
