package com.princess.botblade.ui.settings

import android.content.Context
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.princess.botblade.BuildConfig
import com.princess.botblade.data.api.ApiConfig
import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.api.BotBladeApiClient
import com.princess.botblade.data.model.BotProject
import com.princess.botblade.data.model.GitHubConnectRequest
import com.princess.botblade.data.model.GitHubLinkRepoRequest
import com.princess.botblade.data.model.GitHubStatusResponse
import com.princess.botblade.data.model.SecretCreateRequest
import com.princess.botblade.data.model.SecretSummary
import com.princess.botblade.data.model.displayNameOrNull
import com.princess.botblade.data.repository.ProjectRepository
import com.princess.botblade.data.repository.SecretRepository
import com.princess.botblade.data.store.ActiveProjectStore
import com.princess.botblade.ui.components.BladeButton
import com.princess.botblade.ui.components.BotBladeTokens
import com.princess.botblade.ui.components.SectionTitle
import com.princess.botblade.ui.components.StatusChip
import com.princess.botblade.ui.components.StatusTone
import com.princess.botblade.ui.components.WorkstationCard
import com.princess.botblade.ui.theme.BotBladeTheme
import com.princess.botblade.ui.theme.isDynamicColorEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsComposeFragment : Fragment() {
    private val secretRepository = SecretRepository()
    private val projectRepository = ProjectRepository()

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            BotBladeTheme(useDynamicColor = isDynamicColorEnabled(requireContext())) {
                SettingsScreen()
            }
        }
    }

    @Composable
    private fun SettingsScreen() {
        val context = requireContext()
        val scope = rememberCoroutineScope()
        val activeProjectId = remember { ActiveProjectStore(context).getActiveProjectId() }
        var backendUrl by remember { mutableStateOf(ApiConfig.baseUrl) }
        var apiStatus by remember { mutableStateOf("Current backend: ${ApiConfig.baseUrl}") }
        var dynamicColor by remember { mutableStateOf(isDynamicColorEnabled(context)) }
        var secrets by remember { mutableStateOf<List<SecretSummary>>(emptyList()) }
        var secretStatus by remember { mutableStateOf("Secrets not loaded yet.") }
        var secretName by remember { mutableStateOf("") }
        var secretType by remember { mutableStateOf("discord_bot_token") }
        var secretProject by remember { mutableStateOf(activeProjectId.orEmpty()) }
        var secretValue by remember { mutableStateOf("") }
        var secretToDelete by remember { mutableStateOf<SecretSummary?>(null) }
        var githubStatus by remember { mutableStateOf<GitHubStatusResponse?>(null) }
        var activeProject by remember { mutableStateOf<BotProject?>(null) }
        var githubMessage by remember { mutableStateOf("GitHub status not loaded yet.") }
        var tokenRef by remember { mutableStateOf("") }
        var owner by remember { mutableStateOf("") }
        var repoName by remember { mutableStateOf("") }
        var branch by remember { mutableStateOf("main") }

        fun loadSecrets() = scope.launch {
            secretStatus = "Loading secrets…"
            when (val result = secretRepository.listSecrets()) {
                is ApiResult.Success -> {
                    secrets = result.data
                    secretStatus = "Loaded ${result.data.size} secret reference(s). Values stay hidden."
                }
                is ApiResult.Error -> secretStatus = "Secret error: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun loadGitHub() = scope.launch {
            githubMessage = "Loading GitHub status…"
            val statusResult = projectRepository.getGitHubStatus()
            if (statusResult is ApiResult.Success) githubStatus = statusResult.data
            if (activeProjectId != null) {
                when (val projectResult = projectRepository.getProject(activeProjectId)) {
                    is ApiResult.Success -> activeProject = projectResult.data
                    is ApiResult.Error -> githubMessage = "Project error: ${projectResult.message}"
                    ApiResult.Loading -> Unit
                }
            }
            val project = activeProject
            val gh = project?.github
            tokenRef = githubStatus?.tokenSecretRef.orEmpty()
            owner = gh?.owner.orEmpty()
            repoName = gh?.repo.orEmpty()
            branch = gh?.defaultBranch ?: "main"
            githubMessage = buildString {
                append(if (githubStatus?.connected == true) "GitHub connected" else "GitHub disconnected")
                append(" • Project: ${project?.name ?: "none"}")
                append(" • Repo: ${gh?.displayNameOrNull() ?: "not linked"}")
            }
        }

        fun saveBackend() {
            val validationError = ApiConfig.validateBaseUrl(backendUrl)
            if (validationError != null) {
                apiStatus = validationError
                return
            }
            val savedUrl = ApiConfig.saveBaseUrl(backendUrl)
            backendUrl = savedUrl
            apiStatus = "Saved backend URL: $savedUrl"
        }

        fun testBackend() = scope.launch {
            saveBackend()
            val url = ApiConfig.baseUrl
            apiStatus = "Testing $url…"
            val result = withContext(Dispatchers.IO) { runCatching { BotBladeApiClient(baseUrl = url).getHealth() } }
            apiStatus = result.fold(
                onSuccess = { health -> "Backend reachable at $url (${health.status})." },
                onFailure = { error -> "Backend test failed at $url: ${error.message ?: "unknown"}" },
            )
        }

        fun createSecret() = scope.launch {
            val name = secretName.trim()
            val value = secretValue
            if (name.isBlank()) {
                secretStatus = "Secret name is required."
                return@launch
            }
            if (value.isBlank()) {
                secretStatus = "Secret value is required."
                return@launch
            }
            secretStatus = "Saving secret reference…"
            val request = SecretCreateRequest(
                projectId = secretProject.trim().ifBlank { null },
                name = name,
                type = secretType.ifBlank { "custom" },
                value = value,
            )
            when (val result = secretRepository.createSecret(request)) {
                is ApiResult.Success -> {
                    secretValue = ""
                    secretStatus = "Secret saved. Value remains hidden."
                    loadSecrets()
                }
                is ApiResult.Error -> {
                    secretValue = ""
                    secretStatus = "Secret save failed: ${result.message}"
                }
                ApiResult.Loading -> Unit
            }
        }

        fun rotateSecret(secret: SecretSummary) = scope.launch {
            if (secretValue.isBlank()) {
                secretStatus = "Enter the new value in Secret value first."
                return@launch
            }
            when (val result = secretRepository.rotateSecret(secret.id, secretValue)) {
                is ApiResult.Success -> {
                    secretValue = ""
                    secretStatus = "Rotated ${result.data.name}."
                    loadSecrets()
                }
                is ApiResult.Error -> secretStatus = "Rotate failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun deleteSecret(secret: SecretSummary) = scope.launch {
            when (val result = secretRepository.deleteSecret(secret.id)) {
                is ApiResult.Success -> {
                    secretStatus = "Deleted ${secret.name}."
                    loadSecrets()
                }
                is ApiResult.Error -> secretStatus = "Delete failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun connectGitHub() = scope.launch {
            if (tokenRef.isBlank()) {
                githubMessage = "Token secret reference is required."
                return@launch
            }
            when (val result = projectRepository.connectGitHub(GitHubConnectRequest(tokenRef.trim()))) {
                is ApiResult.Success -> {
                    githubStatus = result.data
                    loadGitHub()
                }
                is ApiResult.Error -> githubMessage = "GitHub connect failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun linkGitHubRepo() = scope.launch {
            val projectId = activeProjectId
            if (projectId == null) {
                githubMessage = "Select a project first."
                return@launch
            }
            if (owner.isBlank() || repoName.isBlank()) {
                githubMessage = "GitHub owner and repo are required."
                return@launch
            }
            when (val result = projectRepository.linkGitHubRepo(projectId, GitHubLinkRepoRequest(owner.trim(), repoName.trim(), branch.trim().ifBlank { "main" }))) {
                is ApiResult.Success -> {
                    activeProject = result.data
                    loadGitHub()
                }
                is ApiResult.Error -> githubMessage = "GitHub link failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun createWorkflow() = scope.launch {
            val projectId = activeProjectId
            if (projectId == null) {
                githubMessage = "Select a project first."
                return@launch
            }
            when (val result = projectRepository.createGitHubWorkflow(projectId)) {
                is ApiResult.Success -> githubMessage = "Created workflow at ${result.data.path}."
                is ApiResult.Error -> githubMessage = "Workflow failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun pushGitHub() = scope.launch {
            val projectId = activeProjectId
            if (projectId == null) {
                githubMessage = "Select a project first."
                return@launch
            }
            when (val result = projectRepository.pushGitHub(projectId)) {
                is ApiResult.Success -> githubMessage = result.data
                is ApiResult.Error -> githubMessage = "Push failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        LaunchedEffect(Unit) {
            loadSecrets()
            loadGitHub()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(BotBladeTokens.Black).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                WorkstationCard(accent = BotBladeTokens.HotPink) {
                    Text("Settings", color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Backend, appearance, GitHub, secrets, and release metadata.", color = BotBladeTokens.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                        StatusChip("Backend", StatusTone.Info)
                        StatusChip("Vault", if (secrets.isEmpty()) StatusTone.Warning else StatusTone.Success)
                        StatusChip("GitHub", if (githubStatus?.connected == true) StatusTone.Success else StatusTone.Neutral)
                    }
                }
            }

            item { SectionTitle("Backend connection") }
            item {
                WorkstationCard(accent = BotBladeTokens.BabyBlue) {
                    OutlinedTextField(value = backendUrl, onValueChange = { backendUrl = it }, label = { Text("Backend URL") }, modifier = Modifier.fillMaxWidth())
                    Text(apiStatus, color = BotBladeTokens.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                        BladeButton("Save", onClick = ::saveBackend, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = ::testBackend, modifier = Modifier.weight(1f)) { Text("Test") }
                    }
                }
            }

            item { SectionTitle("Appearance") }
            item {
                WorkstationCard(accent = BotBladeTokens.GlitterGold) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Dynamic Material You color", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                            Text("Use Android dynamic color as the app base while retaining BotBlade neon accents.", color = BotBladeTokens.Muted)
                        }
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = { checked ->
                                dynamicColor = checked
                                context.getSharedPreferences("botblade_prefs", Context.MODE_PRIVATE).edit().putBoolean("use_dynamic_color", checked).apply()
                                requireActivity().recreate()
                            },
                        )
                    }
                }
            }

            item { SectionTitle("Secrets manager") }
            item {
                WorkstationCard(accent = BotBladeTokens.HotPink) {
                    Text(secretStatus, color = BotBladeTokens.Muted)
                    OutlinedTextField(value = secretName, onValueChange = { secretName = it }, label = { Text("Secret name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = secretType, onValueChange = { secretType = it }, label = { Text("Secret type") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = secretProject, onValueChange = { secretProject = it }, label = { Text("Project ID (blank for global)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = secretValue, onValueChange = { secretValue = it }, visualTransformation = PasswordVisualTransformation(), label = { Text("Secret value") }, modifier = Modifier.fillMaxWidth())
                    BladeButton("Save secret", onClick = ::createSecret, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                }
            }
            items(secrets, key = { it.id }) { secret ->
                WorkstationCard(accent = BotBladeTokens.BabyBlue) {
                    Text(secret.name, color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                    Text("${secret.type} • ${secret.storageMode} • ${secret.fingerprint}", color = BotBladeTokens.Muted)
                    Text("Project: ${secret.projectId ?: "global"} • Updated: ${secret.updatedAt}", color = BotBladeTokens.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = { rotateSecret(secret) }, modifier = Modifier.weight(1f)) { Text("Rotate") }
                        OutlinedButton(onClick = { secretToDelete = secret }, modifier = Modifier.weight(1f)) { Text("Delete") }
                    }
                }
            }

            item { SectionTitle("GitHub") }
            item {
                WorkstationCard(accent = BotBladeTokens.BabyBlue) {
                    Text(githubMessage, color = BotBladeTokens.Muted)
                    OutlinedTextField(value = tokenRef, onValueChange = { tokenRef = it }, label = { Text("GitHub token secret ref") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Owner") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = repoName, onValueChange = { repoName = it }, label = { Text("Repository") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = branch, onValueChange = { branch = it }, label = { Text("Branch") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                        BladeButton("Connect", onClick = ::connectGitHub, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = ::linkGitHubRepo, modifier = Modifier.weight(1f)) { Text("Link") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = ::createWorkflow, modifier = Modifier.weight(1f)) { Text("Workflow") }
                        OutlinedButton(onClick = ::pushGitHub, modifier = Modifier.weight(1f)) { Text("Push") }
                    }
                }
            }

            item { SectionTitle("About") }
            item {
                WorkstationCard(accent = BotBladeTokens.GlitterGold) {
                    Text("BotBlade ${BuildConfig.VERSION_NAME}", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                    Text("Git SHA: ${BuildConfig.GIT_SHA}", color = BotBladeTokens.Muted)
                    Text("APK downloads: princessraven/botBlade", color = BotBladeTokens.Muted)
                }
            }
        }

        secretToDelete?.let { secret ->
            AlertDialog(
                onDismissRequest = { secretToDelete = null },
                title = { Text("Delete secret reference?") },
                text = { Text("Delete ${secret.name}? The full value stays hidden and cannot be recovered from this app.") },
                confirmButton = {
                    TextButton(onClick = { secretToDelete = null; deleteSecret(secret) }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { secretToDelete = null }) { Text("Cancel") }
                },
            )
        }
    }
}
