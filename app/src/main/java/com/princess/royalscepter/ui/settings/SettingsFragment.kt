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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.royalscepter.R
import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.model.BotProject
import com.princess.royalscepter.data.model.GitHubConnectRequest
import com.princess.royalscepter.data.model.GitHubLinkRepoRequest
import com.princess.royalscepter.data.model.GitHubStatusResponse
import com.princess.royalscepter.data.model.SecretCreateRequest
import com.princess.royalscepter.data.model.SecretSummary
import com.princess.royalscepter.data.repository.ProjectRepository
import com.princess.royalscepter.data.repository.SecretRepository
import com.princess.royalscepter.data.store.ActiveProjectStore
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private val secretRepository = SecretRepository()
    private val projectRepository = ProjectRepository()
    private lateinit var status: TextView
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
        typeInput.setText("discord_bot_token")
        githubBranchInput.setText("main")
        view.findViewById<Button>(R.id.create_secret_button).setOnClickListener { createSecret() }
        view.findViewById<Button>(R.id.connect_github_button).setOnClickListener { connectGitHub() }
        view.findViewById<Button>(R.id.link_github_repo_button).setOnClickListener { linkGitHubRepo() }
        workflowButton.setOnClickListener { createWorkflow() }
        pushGitHubButton.setOnClickListener { pushGitHub() }
        loadSecrets()
        loadGitHubSection()
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
}
