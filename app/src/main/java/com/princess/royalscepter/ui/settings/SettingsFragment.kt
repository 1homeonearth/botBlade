package com.princess.royalscepter.ui.settings

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
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.royalscepter.R
import com.princess.royalscepter.data.api.ApiConfig
import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.api.RoyalScepterApiClient
import com.princess.royalscepter.data.model.BotProject
import com.princess.royalscepter.data.model.GitHubConnectRequest
import com.princess.royalscepter.data.model.GitHubLinkRepoRequest
import com.princess.royalscepter.data.model.GitHubStatusResponse
import com.princess.royalscepter.data.model.SecretCreateRequest
import com.princess.royalscepter.data.model.SecretSummary
import com.princess.royalscepter.data.repository.ProjectRepository
import com.princess.royalscepter.data.repository.SecretRepository
import com.princess.royalscepter.data.store.ActiveProjectStore
import com.princess.royalscepter.ui.common.addBodyText
import com.princess.royalscepter.ui.common.copyToClipboard
import com.princess.royalscepter.ui.common.materialListCard
import com.princess.royalscepter.ui.common.visibleWhen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {
    private val secretRepository = SecretRepository()
    private val projectRepository = ProjectRepository()
    private lateinit var status: TextView
    private lateinit var apiStatus: TextView
    private lateinit var progress: ProgressBar
    private lateinit var backendUrlInput: EditText
    private lateinit var saveBackendButton: Button
    private lateinit var testConnectionButton: Button
    private lateinit var createSecretButton: Button
    private lateinit var connectGitHubButton: Button
    private lateinit var linkGitHubButton: Button
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
    private var activeProject: BotProject? = null
    private var currentGitHubStatus: GitHubStatusResponse? = null

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
        status = view.findViewById(R.id.secrets_status)
        apiStatus = view.findViewById(R.id.api_settings_status)
        progress = view.findViewById(R.id.settings_progress)
        backendUrlInput = view.findViewById(R.id.backend_url_input)
        saveBackendButton = view.findViewById(R.id.save_backend_url_button)
        testConnectionButton = view.findViewById(R.id.test_backend_connection_button)
        createSecretButton = view.findViewById(R.id.create_secret_button)
        connectGitHubButton = view.findViewById(R.id.connect_github_button)
        linkGitHubButton = view.findViewById(R.id.link_github_repo_button)
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

        valueInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        backendUrlInput.setText(ApiConfig.baseUrl)
        apiStatus.text = getString(R.string.backend_url_current, ApiConfig.baseUrl)
        typeInput.setText(getString(R.string.default_discord_token_type))
        githubBranchInput.setText(getString(R.string.default_github_branch))
        saveBackendButton.setOnClickListener { saveBackendUrl() }
        testConnectionButton.setOnClickListener { testBackendConnection() }
        createSecretButton.setOnClickListener { createSecret() }
        connectGitHubButton.setOnClickListener { connectGitHub() }
        linkGitHubButton.setOnClickListener { linkGitHubRepo() }
        workflowButton.setOnClickListener { createWorkflow() }
        pushGitHubButton.setOnClickListener { pushGitHub() }
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
        setBusy(true)
        val result = withContext(Dispatchers.IO) {
            runCatching { RoyalScepterApiClient(baseUrl = url).getHealth() }
        }
        setBusy(false)
        apiStatus.text = result.fold(
            onSuccess = { health -> getString(R.string.backend_url_test_success, url, health.status) },
            onFailure = { error -> getString(R.string.backend_url_test_failed, url, error.message ?: getString(R.string.unknown)) },
        )
    }

    private fun loadSecrets() = lifecycleScope.launch {
        status.text = getString(R.string.secrets_loading)
        when (val result = secretRepository.listSecrets()) {
            is ApiResult.Success -> renderSecrets(result.data)
            is ApiResult.Error -> status.text = getString(R.string.error_value, result.message)
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
        githubBranchInput.setText(github?.defaultBranch ?: githubBranchInput.text.toString().ifBlank { getString(R.string.default_github_branch) })
        val connected = statusValue?.connected == true
        val repoLinked = !github?.owner.isNullOrBlank() && !github?.repo.isNullOrBlank()
        pushGitHubButton.isEnabled = connected && repoLinked
        workflowButton.isEnabled = repoLinked
        pushGitHubButton.text = getString(if (pushGitHubButton.isEnabled) R.string.push_github else R.string.push_github_disabled)
        githubStatus.text = getString(
            R.string.github_status_value,
            if (connected) getString(R.string.github_connected) else getString(R.string.github_not_connected),
            project?.name ?: getString(R.string.active_project_none),
            github?.let { "${it.owner}/${it.repo}" } ?: getString(R.string.github_not_linked),
        )
    }

    private fun connectGitHub() = lifecycleScope.launch {
        val tokenRef = githubTokenRefInput.text.toString().trim()
        if (tokenRef.isBlank()) {
            githubTokenRefInput.error = getString(R.string.github_token_ref_required)
            return@launch
        }
        setBusy(true)
        githubStatus.text = getString(R.string.github_connecting)
        when (val result = projectRepository.connectGitHub(GitHubConnectRequest(tokenRef))) {
            is ApiResult.Success -> { setBusy(false); currentGitHubStatus = result.data; renderGitHubSection() }
            is ApiResult.Error -> { setBusy(false); githubStatus.text = getString(R.string.github_error, result.message) }
            ApiResult.Loading -> Unit
        }
    }

    private fun linkGitHubRepo() = lifecycleScope.launch {
        val projectId = ActiveProjectStore(requireContext()).getActiveProjectId()
        if (projectId == null) { githubStatus.text = getString(R.string.select_project_first); return@launch }
        val owner = githubOwnerInput.text.toString().trim()
        val repo = githubRepoInput.text.toString().trim()
        val branch = githubBranchInput.text.toString().trim().ifBlank { getString(R.string.default_github_branch) }
        var valid = true
        if (owner.isBlank()) { githubOwnerInput.error = getString(R.string.github_owner_required); valid = false }
        if (repo.isBlank()) { githubRepoInput.error = getString(R.string.github_repo_required); valid = false }
        if (!valid) return@launch
        setBusy(true)
        githubStatus.text = getString(R.string.github_linking)
        when (val result = projectRepository.linkGitHubRepo(projectId, GitHubLinkRepoRequest(owner, repo, branch))) {
            is ApiResult.Success -> { setBusy(false); activeProject = result.data; renderGitHubSection() }
            is ApiResult.Error -> { setBusy(false); githubStatus.text = getString(R.string.github_error, result.message) }
            ApiResult.Loading -> Unit
        }
    }

    private fun createWorkflow() = lifecycleScope.launch {
        val projectId = ActiveProjectStore(requireContext()).getActiveProjectId() ?: return@launch
        setBusy(true)
        githubStatus.text = getString(R.string.github_workflow_creating)
        when (val result = projectRepository.createGitHubWorkflow(projectId)) {
            is ApiResult.Success -> { setBusy(false); githubStatus.text = getString(R.string.github_workflow_created, result.data.path); requireContext().copyToClipboard(getString(R.string.copy_workflow), result.data.content) }
            is ApiResult.Error -> { setBusy(false); githubStatus.text = getString(R.string.github_error, result.message) }
            ApiResult.Loading -> Unit
        }
    }

    private fun pushGitHub() = lifecycleScope.launch {
        val projectId = ActiveProjectStore(requireContext()).getActiveProjectId() ?: return@launch
        if (!pushGitHubButton.isEnabled) return@launch
        setBusy(true)
        githubStatus.text = getString(R.string.github_push_starting)
        when (val result = projectRepository.pushGitHub(projectId)) {
            is ApiResult.Success -> { setBusy(false); githubStatus.text = result.data }
            is ApiResult.Error -> { setBusy(false); githubStatus.text = getString(R.string.github_error, result.message) }
            ApiResult.Loading -> Unit
        }
    }

    private fun createSecret() = lifecycleScope.launch {
        val value = valueInput.text.toString()
        val request = SecretCreateRequest(projectId = projectInput.text.toString().takeIf { it.isNotBlank() }, name = nameInput.text.toString(), type = typeInput.text.toString().ifBlank { getString(R.string.default_custom_secret_type) }, value = value)
        setBusy(true)
        status.text = getString(R.string.save_secret_progress)
        when (val result = secretRepository.createSecret(request)) {
            is ApiResult.Success -> { setBusy(false); valueInput.text?.clear(); status.text = getString(R.string.secret_saved_hidden); loadSecrets() }
            is ApiResult.Error -> { setBusy(false); valueInput.text?.clear(); status.text = getString(R.string.error_value, result.message) }
            ApiResult.Loading -> Unit
        }
    }

    private fun rotateSecret(secret: SecretSummary) = lifecycleScope.launch {
        val value = valueInput.text.toString()
        if (value.isBlank()) { status.text = getString(R.string.enter_new_secret_value); return@launch }
        setBusy(true)
        status.text = getString(R.string.rotating_secret, secret.name)
        when (val result = secretRepository.rotateSecret(secret.id, value)) {
            is ApiResult.Success -> { setBusy(false); valueInput.text?.clear(); status.text = getString(R.string.secret_saved_hidden); loadSecrets() }
            is ApiResult.Error -> { setBusy(false); valueInput.text?.clear(); status.text = getString(R.string.error_value, result.message) }
            ApiResult.Loading -> Unit
        }
    }

    private fun confirmDeleteSecret(secret: SecretSummary) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_secret_reference_title)
            .setMessage(getString(R.string.delete_secret_reference_message, secret.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> deleteSecret(secret) }
            .show()
    }

    private fun deleteSecret(secret: SecretSummary) = lifecycleScope.launch {
        setBusy(true)
        status.text = getString(R.string.deleting_secret, secret.name)
        when (val result = secretRepository.deleteSecret(secret.id)) {
            is ApiResult.Success -> { setBusy(false); loadSecrets() }
            is ApiResult.Error -> { setBusy(false); status.text = getString(R.string.error_value, result.message) }
            ApiResult.Loading -> Unit
        }
    }

    private fun renderSecrets(secrets: List<SecretSummary>) {
        list.removeAllViews()
        if (secrets.isEmpty()) {
            status.text = getString(R.string.secrets_empty_detail)
            list.addView(requireContext().materialListCard {
                addBodyText(getString(R.string.secrets_empty_detail))
                addView(Button(requireContext()).apply { text = getString(R.string.retry); setOnClickListener { loadSecrets() } })
            })
            return
        }
        secrets.forEach { secret ->
            list.addView(requireContext().materialListCard {
                addBodyText(getString(R.string.secret_card_value, secret.name, secret.type, secret.storageMode, secret.fingerprint, secret.updatedAt, secret.projectId ?: getString(R.string.global)))
                addView(Button(requireContext()).apply { text = getString(R.string.copy_id); setOnClickListener { requireContext().copyToClipboard(getString(R.string.copy_id), secret.id) } })
                addView(Button(requireContext()).apply { text = getString(R.string.rotate_secret); setOnClickListener { rotateSecret(secret) } })
                addView(Button(requireContext()).apply { text = getString(R.string.delete_secret); setOnClickListener { confirmDeleteSecret(secret) } })
            })
        }
        status.text = getString(R.string.secrets_loaded_message, secrets.size)
    }

    private fun setBusy(isBusy: Boolean) {
        progress.visibleWhen(isBusy)
        saveBackendButton.isEnabled = !isBusy
        testConnectionButton.isEnabled = !isBusy
        createSecretButton.isEnabled = !isBusy
        connectGitHubButton.isEnabled = !isBusy
        linkGitHubButton.isEnabled = !isBusy
        val connected = currentGitHubStatus?.connected == true
        val repoLinked = !activeProject?.github?.owner.isNullOrBlank() && !activeProject?.github?.repo.isNullOrBlank()
        workflowButton.isEnabled = !isBusy && repoLinked
        pushGitHubButton.isEnabled = !isBusy && connected && repoLinked
        pushGitHubButton.text = getString(if (pushGitHubButton.isEnabled) R.string.push_github else R.string.push_github_disabled)
    }
}
