package com.princess.botblade.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.botblade.BuildConfig
import com.princess.botblade.R
import com.princess.botblade.StartupDiagnostics
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
import com.princess.botblade.backend.EnginePreferences
import com.princess.botblade.data.repository.TokenRepository
import com.princess.botblade.data.store.ActiveProjectStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {
    private val secretRepository = SecretRepository()
    private val projectRepository = ProjectRepository()
    private lateinit var status: TextView
    private lateinit var apiStatus: TextView
    private lateinit var backendUrlInput: EditText
    private lateinit var testConnectionButton: Button
    private lateinit var list: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var typeInput: EditText
    private lateinit var projectInput: EditText
    private lateinit var valueInput: EditText
    private lateinit var githubStatus: TextView
    private lateinit var githubTokenRefInput: EditText
    private lateinit var githubOwnerInput: EditText
    private lateinit var githubRepoInput: EditText
    private lateinit var githubBranchInput: EditText
    private lateinit var pushGitHubButton: Button
    private lateinit var workflowButton: Button
    private lateinit var workflowHelpText: TextView
    private lateinit var copyWorkflowHelpButton: Button
    private lateinit var apkDownloadsText: TextView
    private var activeProject: BotProject? = null
    private var currentGitHubStatus: GitHubStatusResponse? = null
    private lateinit var tokenRepository: TokenRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onResume() {
        super.onResume()
        requireActivity().window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onPause() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tokenRepository = TokenRepository(requireContext())
        status = view.findViewById(R.id.secrets_status)
        apiStatus = view.findViewById(R.id.api_settings_status)
        backendUrlInput = view.findViewById(R.id.backend_url_input)
        testConnectionButton = view.findViewById(R.id.test_backend_connection_button)
        val shareDiagnosticsButton = view.findViewById<Button>(R.id.share_startup_diagnostics_button)
        list = view.findViewById(R.id.secrets_list_container)
        nameInput = view.findViewById(R.id.secret_name_input)
        typeInput = view.findViewById(R.id.secret_type_input)
        projectInput = view.findViewById(R.id.secret_project_input)
        valueInput = view.findViewById(R.id.secret_value_input)
        githubStatus = view.findViewById(R.id.github_status)
        githubTokenRefInput = view.findViewById(R.id.github_token_ref_input)
        githubOwnerInput = view.findViewById(R.id.github_owner_input)
        githubRepoInput = view.findViewById(R.id.github_repo_input)
        githubBranchInput = view.findViewById(R.id.github_branch_input)
        pushGitHubButton = view.findViewById(R.id.push_github_button)
        workflowButton = view.findViewById(R.id.create_github_workflow_button)
        workflowHelpText = view.findViewById(R.id.github_workflow_help_text)
        copyWorkflowHelpButton = view.findViewById(R.id.copy_github_workflow_help_button)
        apkDownloadsText = view.findViewById(R.id.apk_downloads_text)

        valueInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        backendUrlInput.setText(ApiConfig.baseUrl)
        apiStatus.text = getString(R.string.backend_url_current, ApiConfig.baseUrl)
        typeInput.setText("discord_bot_token")
        githubBranchInput.setText("main")
        view.findViewById<Button>(R.id.save_backend_url_button).setOnClickListener { saveBackendUrl() }
        testConnectionButton.setOnClickListener { testBackendConnection() }
        if (BuildConfig.DEBUG) {
            shareDiagnosticsButton.visibility = View.VISIBLE
            shareDiagnosticsButton.setOnClickListener { shareStartupDiagnostics() }
        }
        view.findViewById<Button>(R.id.create_secret_button).setOnClickListener { createSecret() }
        view.findViewById<Button>(R.id.connect_github_button).setOnClickListener { connectGitHub() }
        view.findViewById<Button>(R.id.link_github_repo_button).setOnClickListener { linkGitHubRepo() }
        workflowButton.setOnClickListener { createWorkflow() }
        copyWorkflowHelpButton.setOnClickListener { copyWorkflowInstructions() }
        pushGitHubButton.setOnClickListener { pushGitHub() }
        apkDownloadsText.text = getString(R.string.apk_download_links, "princessraven/botBlade")
        setupTokenSection(view)
        setupAutoStart(view)
        loadSecrets()
        loadGitHubSection()
    }

    private fun saveBackendUrl(): Boolean {
        val candidate = backendUrlInput.text.toString()
        val validationError = ApiConfig.validateBaseUrl(candidate)
        if (validationError != null) {
            backendUrlInput.error = validationError
            apiStatus.text = validationError
            return false
        }

        val savedUrl = ApiConfig.saveBaseUrl(candidate)
        backendUrlInput.setText(savedUrl)
        backendUrlInput.error = null
        apiStatus.text = getString(R.string.backend_url_saved, savedUrl)
        return true
    }

    private fun testBackendConnection() = lifecycleScope.launch {
        if (!saveBackendUrl()) return@launch
        val url = ApiConfig.baseUrl
        apiStatus.text = getString(R.string.backend_url_testing, url)
        testConnectionButton.isEnabled = false
        val result = withContext(Dispatchers.IO) {
            runCatching { BotBladeApiClient(baseUrl = url).getHealth() }
        }
        testConnectionButton.isEnabled = true
        apiStatus.text = result.fold(
            onSuccess = { health -> getString(R.string.backend_url_test_success, url, health.status) },
            onFailure = { error -> getString(R.string.backend_url_test_failed, url, error.message ?: getString(R.string.unknown)) },
        )
    }

    private fun loadSecrets() = lifecycleScope.launch {
        status.text = getString(R.string.secrets_loading)
        when (val result = secretRepository.listSecrets()) {
            is ApiResult.Success -> renderSecrets(result.data)
            is ApiResult.Error -> status.text = "Error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun loadGitHubSection() = lifecycleScope.launch {
        githubStatus.text = getString(R.string.github_loading)
        val activeProjectId = ActiveProjectStore(requireContext()).getActiveProjectId()
        val statusResult = projectRepository.getGitHubStatus()
        if (statusResult is ApiResult.Success) currentGitHubStatus = statusResult.data
        if (activeProjectId != null) {
            when (val projectResult = projectRepository.getProject(activeProjectId)) {
                is ApiResult.Success -> activeProject = projectResult.data
                is ApiResult.Error -> githubStatus.text = getString(R.string.github_error, projectResult.message)
                ApiResult.Loading -> Unit
            }
        }
        renderGitHubSection()
    }

    private fun renderGitHubSection() {
        val statusValue = currentGitHubStatus
        val project = activeProject
        val github = project?.github
        if (statusValue?.tokenSecretRef != null) githubTokenRefInput.setText(statusValue.tokenSecretRef)
        if (github?.owner != null) githubOwnerInput.setText(github.owner)
        if (github?.repo != null) githubRepoInput.setText(github.repo)
        githubBranchInput.setText(github?.defaultBranch ?: githubBranchInput.text.toString().ifBlank { "main" })
        val connected = statusValue?.connected == true
        val repoDisplayName = github?.displayNameOrNull()
        val repoLinked = repoDisplayName != null
        pushGitHubButton.isEnabled = connected && repoLinked
        workflowButton.isEnabled = repoLinked
        workflowHelpText.visibility = if (pushGitHubButton.isEnabled) View.GONE else View.VISIBLE
        copyWorkflowHelpButton.visibility = if (pushGitHubButton.isEnabled) View.GONE else View.VISIBLE
        pushGitHubButton.text = getString(if (pushGitHubButton.isEnabled) R.string.push_github else R.string.push_github_disabled)
        githubStatus.text = getString(
            R.string.github_status_value,
            if (connected) getString(R.string.github_connected) else getString(R.string.github_not_connected),
            project?.name ?: getString(R.string.active_project_none),
            repoDisplayName ?: getString(R.string.github_not_linked),
        )
    }

    private fun connectGitHub() = lifecycleScope.launch {
        val tokenRef = githubTokenRefInput.text.toString().trim()
        if (tokenRef.isBlank()) {
            githubTokenRefInput.error = getString(R.string.github_token_ref_required)
            return@launch
        }
        githubStatus.text = getString(R.string.github_connecting)
        when (val result = projectRepository.connectGitHub(GitHubConnectRequest(tokenRef))) {
            is ApiResult.Success -> { currentGitHubStatus = result.data; renderGitHubSection() }
            is ApiResult.Error -> githubStatus.text = getString(R.string.github_error, result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun linkGitHubRepo() = lifecycleScope.launch {
        val projectId = ActiveProjectStore(requireContext()).getActiveProjectId()
        if (projectId == null) { githubStatus.text = getString(R.string.select_project_first); return@launch }
        val owner = githubOwnerInput.text.toString().trim()
        val repo = githubRepoInput.text.toString().trim()
        val branch = githubBranchInput.text.toString().trim().ifBlank { "main" }
        var valid = true
        if (owner.isBlank()) { githubOwnerInput.error = getString(R.string.github_owner_required); valid = false }
        if (repo.isBlank()) { githubRepoInput.error = getString(R.string.github_repo_required); valid = false }
        if (!valid) return@launch
        githubStatus.text = getString(R.string.github_linking)
        when (val result = projectRepository.linkGitHubRepo(projectId, GitHubLinkRepoRequest(owner, repo, branch))) {
            is ApiResult.Success -> { activeProject = result.data; renderGitHubSection() }
            is ApiResult.Error -> githubStatus.text = getString(R.string.github_error, result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun createWorkflow() = lifecycleScope.launch {
        val projectId = ActiveProjectStore(requireContext()).getActiveProjectId() ?: return@launch
        githubStatus.text = getString(R.string.github_workflow_creating)
        when (val result = projectRepository.createGitHubWorkflow(projectId)) {
            is ApiResult.Success -> githubStatus.text = getString(R.string.github_workflow_created, result.data.path)
            is ApiResult.Error -> githubStatus.text = getString(R.string.github_error, result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun pushGitHub() = lifecycleScope.launch {
        val projectId = ActiveProjectStore(requireContext()).getActiveProjectId() ?: return@launch
        if (!pushGitHubButton.isEnabled) return@launch
        githubStatus.text = getString(R.string.github_push_starting)
        when (val result = projectRepository.pushGitHub(projectId)) {
            is ApiResult.Success -> githubStatus.text = result.data
            is ApiResult.Error -> githubStatus.text = getString(R.string.github_error, result.message)
            ApiResult.Loading -> Unit
        }
    }


    private fun shareStartupDiagnostics() {
        val diagnostics = StartupDiagnostics.readLatest(requireActivity().application)
        if (diagnostics.isNullOrBlank()) {
            Toast.makeText(requireContext(), getString(R.string.startup_diagnostics_missing), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "botBlade startup diagnostics")
            putExtra(Intent.EXTRA_TEXT, diagnostics)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_startup_diagnostics)))
    }

    private fun copyWorkflowInstructions() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val instructions = buildString {
            append(getString(R.string.github_workflow_help))
            append("\n")
            append(getString(R.string.apk_download_links, "princessraven/botBlade").replace(Regex("<[^>]+>"), ""))
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("workflow_help", instructions))
        Toast.makeText(requireContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    private fun createSecret() = lifecycleScope.launch {
        val value = valueInput.text.toString()
        val request = SecretCreateRequest(projectId = projectInput.text.toString().takeIf { it.isNotBlank() }, name = nameInput.text.toString(), type = typeInput.text.toString().ifBlank { "custom" }, value = value)
        status.text = "Saving secret reference…"
        when (val result = secretRepository.createSecret(request)) {
            is ApiResult.Success -> { valueInput.text?.clear(); status.text = getString(R.string.secret_saved_hidden); loadSecrets() }
            is ApiResult.Error -> { valueInput.text?.clear(); status.text = "Error: ${result.message}" }
            ApiResult.Loading -> Unit
        }
    }

    private fun rotateSecret(secret: SecretSummary) = lifecycleScope.launch {
        val value = valueInput.text.toString()
        if (value.isBlank()) { status.text = "Enter the new secret value in the secret value field first."; return@launch }
        status.text = "Rotating ${secret.name}…"
        when (val result = secretRepository.rotateSecret(secret.id, value)) {
            is ApiResult.Success -> { valueInput.text?.clear(); status.text = getString(R.string.secret_saved_hidden); loadSecrets() }
            is ApiResult.Error -> { valueInput.text?.clear(); status.text = "Error: ${result.message}" }
            ApiResult.Loading -> Unit
        }
    }

    private fun confirmDeleteSecret(secret: SecretSummary) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete secret reference?")
            .setMessage("Delete ${secret.name}? The full value will remain hidden and cannot be recovered from this app.")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> deleteSecret(secret) }
            .show()
    }

    private fun deleteSecret(secret: SecretSummary) = lifecycleScope.launch {
        status.text = "Deleting ${secret.name}…"
        when (val result = secretRepository.deleteSecret(secret.id)) {
            is ApiResult.Success -> loadSecrets()
            is ApiResult.Error -> status.text = "Error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun renderSecrets(secrets: List<SecretSummary>) {
        list.removeAllViews()
        if (secrets.isEmpty()) { status.text = "No secrets saved."; return }
        secrets.forEach { secret ->
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 12, 0, 12) }
            row.addView(TextView(requireContext()).apply { text = "${secret.name} • ${secret.type}\n${secret.storageMode} • ${secret.fingerprint}\nUpdated: ${secret.updatedAt}\nProject: ${secret.projectId ?: "global"}" })
            row.addView(Button(requireContext()).apply { text = getString(R.string.rotate_secret); setOnClickListener { rotateSecret(secret) } })
            row.addView(Button(requireContext()).apply { text = getString(R.string.delete_secret); setOnClickListener { confirmDeleteSecret(secret) } })
            list.addView(row)
        }
        status.text = "Loaded ${secrets.size} secret reference(s). Values stay hidden."
    }

    private fun setupTokenSection(view: View) {
        val tokenMasked = view.findViewById<TextView>(R.id.bot_token_masked)
        val changeButton = view.findViewById<Button>(R.id.change_bot_token_button)
        val clearButton = view.findViewById<Button>(R.id.clear_bot_token_button)
        fun refresh() { tokenMasked.text = tokenRepository.getTokenMasked() ?: "Not set" }
        refresh()
        changeButton.setOnClickListener {
            val input = EditText(requireContext())
            AlertDialog.Builder(requireContext()).setTitle("Change Bot Token").setView(input).setPositiveButton("Save") { _, _ ->
                val token = input.text.toString().trim()
                if (token.isNotBlank()) tokenRepository.setToken(token)
                refresh()
            }.setNegativeButton("Cancel", null).show()
        }
        clearButton.setOnClickListener { tokenRepository.clearToken(); refresh() }
    }

    private fun setupAutoStart(view: View) {
        val autoStart = view.findViewById<android.widget.Switch>(R.id.auto_start_switch)
        val disclaimer = view.findViewById<TextView>(R.id.auto_start_disclaimer)
        lifecycleScope.launch {
            EnginePreferences.autoStartOnBoot(requireContext()).collect { enabled ->
                autoStart.isChecked = enabled
                disclaimer.visibility = if (enabled) View.VISIBLE else View.GONE
            }
        }
        autoStart.setOnCheckedChangeListener { _, checked -> lifecycleScope.launch { EnginePreferences.setAutoStartOnBoot(requireContext(), checked) } }
    }
}
