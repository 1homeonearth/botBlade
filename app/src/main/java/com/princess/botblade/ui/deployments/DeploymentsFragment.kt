package com.princess.botblade.ui.deployments

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.model.BuildSummary
import com.princess.botblade.data.model.DeploymentJobSummary
import com.princess.botblade.data.model.DeploymentTargetSummary
import com.princess.botblade.data.repository.BuildRepository
import com.princess.botblade.data.repository.DeploymentRepository
import com.princess.botblade.data.store.ActiveProjectStore
import com.princess.botblade.ui.components.BladeButton
import com.princess.botblade.ui.components.BotBladeTokens
import com.princess.botblade.ui.components.FlowLane
import com.princess.botblade.ui.components.SectionTitle
import com.princess.botblade.ui.components.StatusChip
import com.princess.botblade.ui.components.StatusTone
import com.princess.botblade.ui.components.TerminalView
import com.princess.botblade.ui.components.WorkstationCard
import com.princess.botblade.ui.theme.BotBladeTheme
import com.princess.botblade.ui.theme.isDynamicColorEnabled
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

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        val store = ActiveProjectStore(requireContext())
        val projectId = arguments?.getString(ARG_PROJECT_ID) ?: store.getActiveProjectId()
        val projectName = arguments?.getString(ARG_PROJECT_NAME) ?: store.getActiveProjectName()
        setContent {
            BotBladeTheme(useDynamicColor = isDynamicColorEnabled(requireContext())) {
                DeploymentsScreen(projectId = projectId, projectName = projectName)
            }
        }
    }

    @Composable
    private fun DeploymentsScreen(projectId: String?, projectName: String?) {
        val scope = rememberCoroutineScope()
        var status by remember { mutableStateOf(if (projectId == null) "Select a project before deploying." else "Deployment console ready.") }
        var terminalLines by remember { mutableStateOf(listOf("Deployment terminal ready.")) }
        var targetName by remember { mutableStateOf("Local Process") }
        var targetType by remember { mutableStateOf("local_process") }
        var builds by remember { mutableStateOf<List<BuildSummary>>(emptyList()) }
        var targets by remember { mutableStateOf<List<DeploymentTargetSummary>>(emptyList()) }
        var deployments by remember { mutableStateOf<List<DeploymentJobSummary>>(emptyList()) }
        var selectedTarget by remember { mutableStateOf<DeploymentTargetSummary?>(null) }
        var githubStatus by remember { mutableStateOf("GitHub status not checked yet.") }

        val latestSuccessfulBuild = builds
            .asSequence()
            .filter { it.status == "succeeded" }
            .maxByOrNull { it.finishedAt ?: it.startedAt ?: "" }
        val canDeploy = projectId != null && latestSuccessfulBuild != null && selectedTarget != null
        val targetsById = targets.associateBy { it.id }

        fun appendTerminal(line: String) {
            terminalLines = (terminalLines + line).takeLast(250)
        }

        fun renderReleaseChecklist() {
            val checklist = listOf(
                "Release readiness",
                "Project: ${projectName ?: projectId ?: "none"}",
                "Active project: ${if (projectId != null) "yes" else "no"}",
                "Successful build: ${latestSuccessfulBuild?.buildId ?: "needed"}",
                "Deployment target: ${selectedTarget?.name ?: "needed"}",
                "Secrets: verify in Settings / Vault",
                "Ship order: scan, edit, add secrets, build, select target, deploy, inspect logs.",
            )
            terminalLines = checklist
            status = if (projectId == null) "Select a project before deploying." else "Release checklist generated."
        }

        fun loadBuilds() = scope.launch {
            val id = projectId ?: return@launch
            when (val result = buildRepository.listBuilds(id)) {
                is ApiResult.Success -> {
                    builds = result.data
                    if (result.data.isEmpty()) appendTerminal("No builds available. Create build to generate a deployable artifact.")
                    status = "Loaded ${result.data.size} build(s)."
                }
                is ApiResult.Error -> status = "Build error: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun loadTargets() = scope.launch {
            when (val result = deploymentRepository.listTargets()) {
                is ApiResult.Success -> {
                    targets = result.data
                    selectedTarget = selectedTarget?.takeIf { selected -> result.data.any { it.id == selected.id } } ?: result.data.firstOrNull()
                    if (result.data.isEmpty()) appendTerminal("No targets available. Add a deployment target to continue.")
                    status = "Loaded ${result.data.size} target(s)."
                }
                is ApiResult.Error -> status = "Target error: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun loadDeployments() = scope.launch {
            val id = projectId ?: return@launch
            when (val result = deploymentRepository.listDeployments(id)) {
                is ApiResult.Success -> {
                    deployments = result.data
                    status = "Loaded ${result.data.size} deployment job(s)."
                }
                is ApiResult.Error -> status = "Deployment error: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun loadGitHubStatus() = scope.launch {
            githubStatus = when (val result = deploymentRepository.getGitHubStatus()) {
                is ApiResult.Success -> "GitHub: ${if (result.data.connected) "connected" else "disconnected"}. ${result.data.message.orEmpty()}"
                is ApiResult.Error -> "GitHub status unavailable: ${result.message}"
                ApiResult.Loading -> githubStatus
            }
            appendTerminal(githubStatus)
        }

        fun loadAll() {
            if (projectId == null) {
                status = "Select a project before deploying."
                loadGitHubStatus()
                return
            }
            loadBuilds()
            loadTargets()
            loadDeployments()
            loadGitHubStatus()
        }

        fun createBuild() = scope.launch {
            val id = projectId ?: return@launch
            status = "Starting build…"
            when (val result = buildRepository.createBuild(id)) {
                is ApiResult.Success -> {
                    status = "Build ${result.data.buildId}: ${result.data.status}"
                    appendTerminal("Build ${result.data.buildId} started with status ${result.data.status}.")
                    loadBuilds()
                }
                is ApiResult.Error -> status = "Error: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun createTarget() = scope.launch {
            when (val result = deploymentRepository.createTarget(targetName.ifBlank { "Local Process" }, targetType.ifBlank { "local_process" })) {
                is ApiResult.Success -> {
                    status = "Created target ${result.data.name}."
                    appendTerminal("Created target ${result.data.name} (${result.data.type}).")
                    loadTargets()
                }
                is ApiResult.Error -> status = "Error: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun deployLatest() = scope.launch {
            val id = projectId ?: return@launch
            val build = latestSuccessfulBuild ?: return@launch
            val target = selectedTarget ?: return@launch
            when (val result = deploymentRepository.createDeployment(id, target.id, build.buildId)) {
                is ApiResult.Success -> {
                    status = "Deployment ${result.data.deploymentId}: ${result.data.status}"
                    appendTerminal("Deployment ${result.data.deploymentId} created for build ${build.buildId} on ${target.name}.")
                    loadDeployments()
                }
                is ApiResult.Error -> status = "Error: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun testTarget(target: DeploymentTargetSummary) = scope.launch {
            selectedTarget = target
            status = "Testing target ${target.name}…"
            when (val result = deploymentRepository.testTarget(target.id)) {
                is ApiResult.Success -> {
                    status = "Target ${target.name}: ${result.data.status}"
                    appendTerminal("${target.name}: ${result.data.message}")
                }
                is ApiResult.Error -> status = "Target test failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun loadDeploymentDetails(deployment: DeploymentJobSummary) = scope.launch {
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
            terminalLines = listOf(
                githubStatus,
                "Deployment ${deployment.deploymentId}",
                statusText,
                "Actions: restart=${if (canRestart) "supported" else "unsupported"}, rollback=${if (canRollback) "supported" else "unsupported"}",
            ) + logText.lines()
            status = "Loaded deployment ${deployment.deploymentId}."
        }

        LaunchedEffect(projectId) { loadAll() }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BotBladeTokens.Black)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                WorkstationCard(accent = BotBladeTokens.HotPink) {
                    Text("Deployment Pipeline", color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Project: ${projectName ?: projectId ?: "none selected"}", color = BotBladeTokens.Muted)
                    Text(status, color = BotBladeTokens.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                        StatusChip("Build", if (latestSuccessfulBuild != null) StatusTone.Success else StatusTone.Warning)
                        StatusChip("Target", if (selectedTarget != null) StatusTone.Success else StatusTone.Warning)
                        StatusChip("Deploy", if (canDeploy) StatusTone.Success else StatusTone.Neutral)
                    }
                }
            }

            item {
                FlowLane("Pipeline", "Build → Test → Package → Deploy → Verify", BotBladeTokens.BabyBlue) {
                    BladeButton("Create build", onClick = ::createBuild, enabled = projectId != null, modifier = Modifier.weight(1f))
                    BladeButton("Deploy latest", onClick = ::deployLatest, enabled = canDeploy, modifier = Modifier.weight(1f))
                }
            }

            item {
                WorkstationCard(accent = BotBladeTokens.GlitterGold) {
                    Text("Deployment target", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = targetName, onValueChange = { targetName = it }, label = { Text("Target name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = targetType, onValueChange = { targetType = it }, label = { Text("Target type") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                        BladeButton("Add target", onClick = ::createTarget, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = ::renderReleaseChecklist, modifier = Modifier.weight(1f)) { Text("Release check") }
                    }
                }
            }

            item { SectionTitle("Targets") }
            if (targets.isEmpty()) {
                item {
                    WorkstationCard(accent = BotBladeTokens.GlitterGold) {
                        Text("No deployment targets yet", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                        Text("Create a local process, Docker, or cloud target to continue.", color = BotBladeTokens.Muted)
                    }
                }
            } else {
                items(targets, key = { it.id }) { target ->
                    WorkstationCard(accent = if (selectedTarget?.id == target.id) BotBladeTokens.Success else BotBladeTokens.BabyBlue) {
                        Text(target.name, color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                        Text("Type: ${target.type}", color = BotBladeTokens.Muted)
                        Text("Supported: ${target.capabilities.actions.filterValues { it }.keys.sorted().joinToString(", ").ifBlank { "none" }}", color = BotBladeTokens.Muted)
                        Text("Unsupported: ${target.capabilities.actions.filterValues { !it }.keys.sorted().joinToString(", ").ifBlank { "none" }}", color = BotBladeTokens.Muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                            AssistChip(onClick = { selectedTarget = target }, label = { Text("Select") })
                            AssistChip(onClick = { testTarget(target) }, label = { Text("Test") })
                        }
                    }
                }
            }

            item { SectionTitle("Recent deployments") }
            if (deployments.isEmpty()) {
                item {
                    WorkstationCard(accent = BotBladeTokens.HotPink) {
                        Text("No deployment jobs yet", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                        Text("Deploy the latest successful build to create the first job.", color = BotBladeTokens.Muted)
                    }
                }
            } else {
                items(deployments, key = { it.deploymentId }) { deployment ->
                    val target = targetsById[deployment.targetId]
                    WorkstationCard(accent = if (deployment.status == "failed") BotBladeTokens.Danger else BotBladeTokens.HotPink) {
                        Text("Deployment ${deployment.deploymentId}", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                        Text("Status: ${deployment.status}", color = BotBladeTokens.Muted)
                        Text("Build: ${deployment.buildId}", color = BotBladeTokens.Muted)
                        Text("Target: ${target?.name ?: deployment.targetId}", color = BotBladeTokens.Muted)
                        AssistChip(onClick = { loadDeploymentDetails(deployment) }, label = { Text("Logs / status") })
                    }
                }
            }

            item {
                TerminalView(title = "Deployment logs", lines = terminalLines)
            }
        }
    }
}
