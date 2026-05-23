// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.settings  // line 7: executes this statement as part of this file's behavior

import android.content.ClipData  // line 9: executes this statement as part of this file's behavior
import android.content.ClipboardManager  // line 10: executes this statement as part of this file's behavior
import android.content.Intent  // line 11: executes this statement as part of this file's behavior
import android.content.Context  // line 12: executes this statement as part of this file's behavior
import android.os.Bundle  // line 13: executes this statement as part of this file's behavior
import android.text.InputType  // line 14: executes this statement as part of this file's behavior
import android.view.LayoutInflater  // line 15: executes this statement as part of this file's behavior
import android.view.View  // line 16: executes this statement as part of this file's behavior
import android.view.ViewGroup  // line 17: executes this statement as part of this file's behavior
import android.widget.Button  // line 18: executes this statement as part of this file's behavior
import android.widget.EditText  // line 19: executes this statement as part of this file's behavior
import android.widget.LinearLayout  // line 20: executes this statement as part of this file's behavior
import android.widget.TextView  // line 21: executes this statement as part of this file's behavior
import android.widget.Toast  // line 22: executes this statement as part of this file's behavior
import com.google.android.material.switchmaterial.SwitchMaterial  // line 23: executes this statement as part of this file's behavior
import androidx.appcompat.app.AlertDialog  // line 24: executes this statement as part of this file's behavior
import androidx.fragment.app.Fragment  // line 25: executes this statement as part of this file's behavior
import androidx.lifecycle.lifecycleScope  // line 26: executes this statement as part of this file's behavior
import com.princess.botblade.BuildConfig  // line 27: executes this statement as part of this file's behavior
import com.princess.botblade.R  // line 28: executes this statement as part of this file's behavior
import com.princess.botblade.StartupDiagnostics  // line 29: executes this statement as part of this file's behavior
import com.princess.botblade.data.api.ApiConfig  // line 30: executes this statement as part of this file's behavior
import com.princess.botblade.data.api.ApiResult  // line 31: executes this statement as part of this file's behavior
import com.princess.botblade.data.api.BotBladeApiClient  // line 32: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.BotProject  // line 33: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubConnectRequest  // line 34: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubLinkRepoRequest  // line 35: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.GitHubStatusResponse  // line 36: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.SecretCreateRequest  // line 37: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.SecretSummary  // line 38: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.displayNameOrNull  // line 39: executes this statement as part of this file's behavior
import com.princess.botblade.data.repository.ProjectRepository  // line 40: executes this statement as part of this file's behavior
import com.princess.botblade.data.repository.SecretRepository  // line 41: executes this statement as part of this file's behavior
import com.princess.botblade.backend.EnginePreferences  // line 42: executes this statement as part of this file's behavior
import com.princess.botblade.data.repository.TokenRepository  // line 43: executes this statement as part of this file's behavior
import com.princess.botblade.data.store.ActiveProjectStore  // line 44: executes this statement as part of this file's behavior
import com.princess.botblade.ui.theme.isDynamicColorEnabled  // line 45: executes this statement as part of this file's behavior
import kotlinx.coroutines.Dispatchers  // line 46: executes this statement as part of this file's behavior
import kotlinx.coroutines.launch  // line 47: executes this statement as part of this file's behavior
import kotlinx.coroutines.withContext  // line 48: executes this statement as part of this file's behavior

class SettingsFragment : Fragment() {  // line 50: executes this statement as part of this file's behavior
    private val secretRepository = SecretRepository()  // line 51: executes this statement as part of this file's behavior
    private val projectRepository = ProjectRepository()  // line 52: executes this statement as part of this file's behavior
    private lateinit var status: TextView  // line 53: executes this statement as part of this file's behavior
    private lateinit var apiStatus: TextView  // line 54: executes this statement as part of this file's behavior
    private lateinit var backendUrlInput: EditText  // line 55: executes this statement as part of this file's behavior
    private lateinit var testConnectionButton: Button  // line 56: executes this statement as part of this file's behavior
    private lateinit var list: LinearLayout  // line 57: executes this statement as part of this file's behavior
    private lateinit var nameInput: EditText  // line 58: executes this statement as part of this file's behavior
    private lateinit var typeInput: EditText  // line 59: executes this statement as part of this file's behavior
    private lateinit var projectInput: EditText  // line 60: executes this statement as part of this file's behavior
    private lateinit var valueInput: EditText  // line 61: executes this statement as part of this file's behavior
    private lateinit var githubStatus: TextView  // line 62: executes this statement as part of this file's behavior
    private lateinit var githubTokenRefInput: EditText  // line 63: executes this statement as part of this file's behavior
    private lateinit var githubOwnerInput: EditText  // line 64: executes this statement as part of this file's behavior
    private lateinit var githubRepoInput: EditText  // line 65: executes this statement as part of this file's behavior
    private lateinit var githubBranchInput: EditText  // line 66: executes this statement as part of this file's behavior
    private lateinit var pushGitHubButton: Button  // line 67: executes this statement as part of this file's behavior
    private lateinit var workflowButton: Button  // line 68: executes this statement as part of this file's behavior
    private lateinit var workflowHelpText: TextView  // line 69: executes this statement as part of this file's behavior
    private lateinit var copyWorkflowHelpButton: Button  // line 70: executes this statement as part of this file's behavior
    private lateinit var apkDownloadsText: TextView  // line 71: executes this statement as part of this file's behavior
    private var activeProject: BotProject? = null  // line 72: executes this statement as part of this file's behavior
    private var currentGitHubStatus: GitHubStatusResponse? = null  // line 73: executes this statement as part of this file's behavior
    private lateinit var tokenRepository: TokenRepository  // line 74: executes this statement as part of this file's behavior

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =  // line 76: executes this statement as part of this file's behavior
        inflater.inflate(R.layout.fragment_settings, container, false)  // line 77: executes this statement as part of this file's behavior

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {  // line 79: executes this statement as part of this file's behavior
        super.onViewCreated(view, savedInstanceState)  // line 80: executes this statement as part of this file's behavior
        tokenRepository = TokenRepository(requireContext())  // line 81: executes this statement as part of this file's behavior
        status = view.findViewById(R.id.secrets_status)  // line 82: executes this statement as part of this file's behavior
        apiStatus = view.findViewById(R.id.api_settings_status)  // line 83: executes this statement as part of this file's behavior
        backendUrlInput = view.findViewById(R.id.backend_url_input)  // line 84: executes this statement as part of this file's behavior
        testConnectionButton = view.findViewById(R.id.test_backend_connection_button)  // line 85: executes this statement as part of this file's behavior
        val shareDiagnosticsButton = view.findViewById<Button>(R.id.share_startup_diagnostics_button)  // line 86: executes this statement as part of this file's behavior
        list = view.findViewById(R.id.secrets_list_container)  // line 87: executes this statement as part of this file's behavior
        nameInput = view.findViewById(R.id.secret_name_input)  // line 88: executes this statement as part of this file's behavior
        typeInput = view.findViewById(R.id.secret_type_input)  // line 89: executes this statement as part of this file's behavior
        projectInput = view.findViewById(R.id.secret_project_input)  // line 90: executes this statement as part of this file's behavior
        valueInput = view.findViewById(R.id.secret_value_input)  // line 91: executes this statement as part of this file's behavior
        githubStatus = view.findViewById(R.id.github_status)  // line 92: executes this statement as part of this file's behavior
        githubTokenRefInput = view.findViewById(R.id.github_token_ref_input)  // line 93: executes this statement as part of this file's behavior
        githubOwnerInput = view.findViewById(R.id.github_owner_input)  // line 94: executes this statement as part of this file's behavior
        githubRepoInput = view.findViewById(R.id.github_repo_input)  // line 95: executes this statement as part of this file's behavior
        githubBranchInput = view.findViewById(R.id.github_branch_input)  // line 96: executes this statement as part of this file's behavior
        pushGitHubButton = view.findViewById(R.id.push_github_button)  // line 97: executes this statement as part of this file's behavior
        workflowButton = view.findViewById(R.id.create_github_workflow_button)  // line 98: executes this statement as part of this file's behavior
        workflowHelpText = view.findViewById(R.id.github_workflow_help_text)  // line 99: executes this statement as part of this file's behavior
        copyWorkflowHelpButton = view.findViewById(R.id.copy_github_workflow_help_button)  // line 100: executes this statement as part of this file's behavior
        apkDownloadsText = view.findViewById(R.id.apk_downloads_text)  // line 101: executes this statement as part of this file's behavior

        valueInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS  // line 103: executes this statement as part of this file's behavior
        backendUrlInput.setText(ApiConfig.baseUrl)  // line 104: executes this statement as part of this file's behavior
        apiStatus.text = getString(R.string.backend_url_current, ApiConfig.baseUrl)  // line 105: executes this statement as part of this file's behavior
        typeInput.setText("discord_bot_token")  // line 106: executes this statement as part of this file's behavior
        githubBranchInput.setText("main")  // line 107: executes this statement as part of this file's behavior
        view.findViewById<Button>(R.id.save_backend_url_button).setOnClickListener { saveBackendUrl() }  // line 108: executes this statement as part of this file's behavior
        testConnectionButton.setOnClickListener { testBackendConnection() }  // line 109: executes this statement as part of this file's behavior
        if (BuildConfig.DEBUG) {  // line 110: executes this statement as part of this file's behavior
            shareDiagnosticsButton.visibility = View.VISIBLE  // line 111: executes this statement as part of this file's behavior
            shareDiagnosticsButton.setOnClickListener { shareStartupDiagnostics() }  // line 112: executes this statement as part of this file's behavior
        }  // line 113: executes this statement as part of this file's behavior
        view.findViewById<Button>(R.id.create_secret_button).setOnClickListener { createSecret() }  // line 114: executes this statement as part of this file's behavior
        view.findViewById<Button>(R.id.connect_github_button).setOnClickListener { connectGitHub() }  // line 115: executes this statement as part of this file's behavior
        view.findViewById<Button>(R.id.link_github_repo_button).setOnClickListener { linkGitHubRepo() }  // line 116: executes this statement as part of this file's behavior
        workflowButton.setOnClickListener { createWorkflow() }  // line 117: executes this statement as part of this file's behavior
        copyWorkflowHelpButton.setOnClickListener { copyWorkflowInstructions() }  // line 118: executes this statement as part of this file's behavior
        pushGitHubButton.setOnClickListener { pushGitHub() }  // line 119: executes this statement as part of this file's behavior
        apkDownloadsText.text = getString(R.string.apk_download_links, "princessraven/botBlade")  // line 120: executes this statement as part of this file's behavior
        val dynamicColorSwitch = view.findViewById<SwitchMaterial>(R.id.dynamic_color_switch)  // line 121: executes this statement as part of this file's behavior
        val prefs = requireContext().getSharedPreferences("botblade_prefs", Context.MODE_PRIVATE)  // line 122: executes this statement as part of this file's behavior
        dynamicColorSwitch.isChecked = prefs.getBoolean("use_dynamic_color", false)  // line 123: executes this statement as part of this file's behavior
        dynamicColorSwitch.setOnCheckedChangeListener { _, checked ->  // line 124: executes this statement as part of this file's behavior
            prefs.edit().putBoolean("use_dynamic_color", checked).apply()  // line 125: executes this statement as part of this file's behavior
            requireActivity().recreate()  // line 126: executes this statement as part of this file's behavior
        }  // line 127: executes this statement as part of this file's behavior
        setupTokenSection(view)  // line 128: executes this statement as part of this file's behavior
        setupAutoStart(view)  // line 129: executes this statement as part of this file's behavior
        loadSecrets()  // line 130: executes this statement as part of this file's behavior
        loadGitHubSection()  // line 131: executes this statement as part of this file's behavior
    }  // line 132: executes this statement as part of this file's behavior

    private fun saveBackendUrl(): Boolean {  // line 134: executes this statement as part of this file's behavior
        val candidate = backendUrlInput.text.toString()  // line 135: executes this statement as part of this file's behavior
        val validationError = ApiConfig.validateBaseUrl(candidate)  // line 136: executes this statement as part of this file's behavior
        if (validationError != null) {  // line 137: executes this statement as part of this file's behavior
            backendUrlInput.error = validationError  // line 138: executes this statement as part of this file's behavior
            apiStatus.text = validationError  // line 139: executes this statement as part of this file's behavior
            return false  // line 140: executes this statement as part of this file's behavior
        }  // line 141: executes this statement as part of this file's behavior

        val savedUrl = ApiConfig.saveBaseUrl(candidate)  // line 143: executes this statement as part of this file's behavior
        backendUrlInput.setText(savedUrl)  // line 144: executes this statement as part of this file's behavior
        backendUrlInput.error = null  // line 145: executes this statement as part of this file's behavior
        apiStatus.text = getString(R.string.backend_url_saved, savedUrl)  // line 146: executes this statement as part of this file's behavior
        return true  // line 147: executes this statement as part of this file's behavior
    }  // line 148: executes this statement as part of this file's behavior

    private fun testBackendConnection() = lifecycleScope.launch {  // line 150: executes this statement as part of this file's behavior
        if (!saveBackendUrl()) return@launch  // line 151: executes this statement as part of this file's behavior
        val url = ApiConfig.baseUrl  // line 152: executes this statement as part of this file's behavior
        apiStatus.text = getString(R.string.backend_url_testing, url)  // line 153: executes this statement as part of this file's behavior
        testConnectionButton.isEnabled = false  // line 154: executes this statement as part of this file's behavior
        val result = withContext(Dispatchers.IO) {  // line 155: executes this statement as part of this file's behavior
            runCatching { BotBladeApiClient(baseUrl = url).getHealth() }  // line 156: executes this statement as part of this file's behavior
        }  // line 157: executes this statement as part of this file's behavior
        testConnectionButton.isEnabled = true  // line 158: executes this statement as part of this file's behavior
        apiStatus.text = result.fold(  // line 159: executes this statement as part of this file's behavior
            onSuccess = { health -> getString(R.string.backend_url_test_success, url, health.status) },  // line 160: executes this statement as part of this file's behavior
            onFailure = { error -> getString(R.string.backend_url_test_failed, url, error.message ?: getString(R.string.unknown)) },  // line 161: executes this statement as part of this file's behavior
        )  // line 162: executes this statement as part of this file's behavior
    }  // line 163: executes this statement as part of this file's behavior

    private fun loadSecrets() = lifecycleScope.launch {  // line 165: executes this statement as part of this file's behavior
        status.text = getString(R.string.secrets_loading)  // line 166: executes this statement as part of this file's behavior
        when (val result = secretRepository.listSecrets()) {  // line 167: executes this statement as part of this file's behavior
            is ApiResult.Success -> renderSecrets(result.data)  // line 168: executes this statement as part of this file's behavior
            is ApiResult.Error -> status.text = "Error: ${result.message}"  // line 169: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 170: executes this statement as part of this file's behavior
        }  // line 171: executes this statement as part of this file's behavior
    }  // line 172: executes this statement as part of this file's behavior

    private fun loadGitHubSection() = lifecycleScope.launch {  // line 174: executes this statement as part of this file's behavior
        githubStatus.text = getString(R.string.github_loading)  // line 175: executes this statement as part of this file's behavior
        val activeProjectId = context?.let { ActiveProjectStore(it).getActiveProjectId() }  // line 176: executes this statement as part of this file's behavior
        val statusResult = projectRepository.getGitHubStatus()  // line 177: executes this statement as part of this file's behavior
        if (statusResult is ApiResult.Success) currentGitHubStatus = statusResult.data  // line 178: executes this statement as part of this file's behavior
        if (activeProjectId != null) {  // line 179: executes this statement as part of this file's behavior
            when (val projectResult = projectRepository.getProject(activeProjectId)) {  // line 180: executes this statement as part of this file's behavior
                is ApiResult.Success -> activeProject = projectResult.data  // line 181: executes this statement as part of this file's behavior
                is ApiResult.Error -> githubStatus.text = getString(R.string.github_error, projectResult.message)  // line 182: executes this statement as part of this file's behavior
                ApiResult.Loading -> Unit  // line 183: executes this statement as part of this file's behavior
            }  // line 184: executes this statement as part of this file's behavior
        }  // line 185: executes this statement as part of this file's behavior
        renderGitHubSection()  // line 186: executes this statement as part of this file's behavior
    }  // line 187: executes this statement as part of this file's behavior

    private fun renderGitHubSection() {  // line 189: executes this statement as part of this file's behavior
        val statusValue = currentGitHubStatus  // line 190: executes this statement as part of this file's behavior
        val project = activeProject  // line 191: executes this statement as part of this file's behavior
        val github = project?.github  // line 192: executes this statement as part of this file's behavior
        if (statusValue?.tokenSecretRef != null) githubTokenRefInput.setText(statusValue.tokenSecretRef)  // line 193: executes this statement as part of this file's behavior
        if (github?.owner != null) githubOwnerInput.setText(github.owner)  // line 194: executes this statement as part of this file's behavior
        if (github?.repo != null) githubRepoInput.setText(github.repo)  // line 195: executes this statement as part of this file's behavior
        githubBranchInput.setText(github?.defaultBranch ?: githubBranchInput.text.toString().ifBlank { "main" })  // line 196: executes this statement as part of this file's behavior
        val connected = statusValue?.connected == true  // line 197: executes this statement as part of this file's behavior
        val repoDisplayName = github?.displayNameOrNull()  // line 198: executes this statement as part of this file's behavior
        val repoLinked = repoDisplayName != null  // line 199: executes this statement as part of this file's behavior
        pushGitHubButton.isEnabled = connected && repoLinked  // line 200: executes this statement as part of this file's behavior
        workflowButton.isEnabled = repoLinked  // line 201: executes this statement as part of this file's behavior
        workflowHelpText.visibility = if (pushGitHubButton.isEnabled) View.GONE else View.VISIBLE  // line 202: executes this statement as part of this file's behavior
        copyWorkflowHelpButton.visibility = if (pushGitHubButton.isEnabled) View.GONE else View.VISIBLE  // line 203: executes this statement as part of this file's behavior
        pushGitHubButton.text = getString(if (pushGitHubButton.isEnabled) R.string.push_github else R.string.push_github_disabled)  // line 204: executes this statement as part of this file's behavior
        githubStatus.text = getString(  // line 205: executes this statement as part of this file's behavior
            R.string.github_status_value,  // line 206: executes this statement as part of this file's behavior
            if (connected) getString(R.string.github_connected) else getString(R.string.github_not_connected),  // line 207: executes this statement as part of this file's behavior
            project?.name ?: getString(R.string.active_project_none),  // line 208: executes this statement as part of this file's behavior
            repoDisplayName ?: getString(R.string.github_not_linked),  // line 209: executes this statement as part of this file's behavior
        )  // line 210: executes this statement as part of this file's behavior
    }  // line 211: executes this statement as part of this file's behavior

    private fun connectGitHub() = lifecycleScope.launch {  // line 213: executes this statement as part of this file's behavior
        val tokenRef = githubTokenRefInput.text.toString().trim()  // line 214: executes this statement as part of this file's behavior
        if (tokenRef.isBlank()) {  // line 215: executes this statement as part of this file's behavior
            githubTokenRefInput.error = getString(R.string.github_token_ref_required)  // line 216: executes this statement as part of this file's behavior
            return@launch  // line 217: executes this statement as part of this file's behavior
        }  // line 218: executes this statement as part of this file's behavior
        githubStatus.text = getString(R.string.github_connecting)  // line 219: executes this statement as part of this file's behavior
        when (val result = projectRepository.connectGitHub(GitHubConnectRequest(tokenRef))) {  // line 220: executes this statement as part of this file's behavior
            is ApiResult.Success -> { currentGitHubStatus = result.data; renderGitHubSection() }  // line 221: executes this statement as part of this file's behavior
            is ApiResult.Error -> githubStatus.text = getString(R.string.github_error, result.message)  // line 222: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 223: executes this statement as part of this file's behavior
        }  // line 224: executes this statement as part of this file's behavior
    }  // line 225: executes this statement as part of this file's behavior

    private fun linkGitHubRepo() = lifecycleScope.launch {  // line 227: executes this statement as part of this file's behavior
        val projectId = context?.let { ActiveProjectStore(it).getActiveProjectId() }  // line 228: executes this statement as part of this file's behavior
        if (projectId == null) { githubStatus.text = getString(R.string.select_project_first); return@launch }  // line 229: executes this statement as part of this file's behavior
        val owner = githubOwnerInput.text.toString().trim()  // line 230: executes this statement as part of this file's behavior
        val repo = githubRepoInput.text.toString().trim()  // line 231: executes this statement as part of this file's behavior
        val branch = githubBranchInput.text.toString().trim().ifBlank { "main" }  // line 232: executes this statement as part of this file's behavior
        var valid = true  // line 233: executes this statement as part of this file's behavior
        if (owner.isBlank()) { githubOwnerInput.error = getString(R.string.github_owner_required); valid = false }  // line 234: executes this statement as part of this file's behavior
        if (repo.isBlank()) { githubRepoInput.error = getString(R.string.github_repo_required); valid = false }  // line 235: executes this statement as part of this file's behavior
        if (!valid) return@launch  // line 236: executes this statement as part of this file's behavior
        githubStatus.text = getString(R.string.github_linking)  // line 237: executes this statement as part of this file's behavior
        when (val result = projectRepository.linkGitHubRepo(projectId, GitHubLinkRepoRequest(owner, repo, branch))) {  // line 238: executes this statement as part of this file's behavior
            is ApiResult.Success -> { activeProject = result.data; renderGitHubSection() }  // line 239: executes this statement as part of this file's behavior
            is ApiResult.Error -> githubStatus.text = getString(R.string.github_error, result.message)  // line 240: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 241: executes this statement as part of this file's behavior
        }  // line 242: executes this statement as part of this file's behavior
    }  // line 243: executes this statement as part of this file's behavior

    private fun createWorkflow() = lifecycleScope.launch {  // line 245: executes this statement as part of this file's behavior
        val projectId = context?.let { ActiveProjectStore(it).getActiveProjectId() }  // line 246: executes this statement as part of this file's behavior
        if (projectId == null) { githubStatus.text = getString(R.string.select_project_first); return@launch }  // line 247: executes this statement as part of this file's behavior
        githubStatus.text = getString(R.string.github_workflow_creating)  // line 248: executes this statement as part of this file's behavior
        when (val result = projectRepository.createGitHubWorkflow(projectId)) {  // line 249: executes this statement as part of this file's behavior
            is ApiResult.Success -> githubStatus.text = getString(R.string.github_workflow_created, result.data.path)  // line 250: executes this statement as part of this file's behavior
            is ApiResult.Error -> githubStatus.text = getString(R.string.github_error, result.message)  // line 251: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 252: executes this statement as part of this file's behavior
        }  // line 253: executes this statement as part of this file's behavior
    }  // line 254: executes this statement as part of this file's behavior

    private fun pushGitHub() = lifecycleScope.launch {  // line 256: executes this statement as part of this file's behavior
        val projectId = context?.let { ActiveProjectStore(it).getActiveProjectId() }  // line 257: executes this statement as part of this file's behavior
        if (projectId == null) { githubStatus.text = getString(R.string.select_project_first); return@launch }  // line 258: executes this statement as part of this file's behavior
        if (!pushGitHubButton.isEnabled) return@launch  // line 259: executes this statement as part of this file's behavior
        githubStatus.text = getString(R.string.github_push_starting)  // line 260: executes this statement as part of this file's behavior
        when (val result = projectRepository.pushGitHub(projectId)) {  // line 261: executes this statement as part of this file's behavior
            is ApiResult.Success -> githubStatus.text = result.data  // line 262: executes this statement as part of this file's behavior
            is ApiResult.Error -> githubStatus.text = getString(R.string.github_error, result.message)  // line 263: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 264: executes this statement as part of this file's behavior
        }  // line 265: executes this statement as part of this file's behavior
    }  // line 266: executes this statement as part of this file's behavior


    private fun shareStartupDiagnostics() {  // line 269: executes this statement as part of this file's behavior
        val diagnostics = StartupDiagnostics.readLatest(requireActivity().application)  // line 270: executes this statement as part of this file's behavior
        if (diagnostics.isNullOrBlank()) {  // line 271: executes this statement as part of this file's behavior
            Toast.makeText(requireContext(), getString(R.string.startup_diagnostics_missing), Toast.LENGTH_SHORT).show()  // line 272: executes this statement as part of this file's behavior
            return  // line 273: executes this statement as part of this file's behavior
        }  // line 274: executes this statement as part of this file's behavior
        val intent = Intent(Intent.ACTION_SEND).apply {  // line 275: executes this statement as part of this file's behavior
            type = "text/plain"  // line 276: executes this statement as part of this file's behavior
            putExtra(Intent.EXTRA_SUBJECT, "botBlade startup diagnostics")  // line 277: executes this statement as part of this file's behavior
            putExtra(Intent.EXTRA_TEXT, diagnostics)  // line 278: executes this statement as part of this file's behavior
        }  // line 279: executes this statement as part of this file's behavior
        startActivity(Intent.createChooser(intent, getString(R.string.share_startup_diagnostics)))  // line 280: executes this statement as part of this file's behavior
    }  // line 281: executes this statement as part of this file's behavior

    private fun copyWorkflowInstructions() {  // line 283: executes this statement as part of this file's behavior
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager  // line 284: executes this statement as part of this file's behavior
        val instructions = buildString {  // line 285: executes this statement as part of this file's behavior
            append(getString(R.string.github_workflow_help))  // line 286: executes this statement as part of this file's behavior
            append("\n")  // line 287: executes this statement as part of this file's behavior
            append(getString(R.string.apk_download_links, "princessraven/botBlade").replace(Regex("<[^>]+>"), ""))  // line 288: executes this statement as part of this file's behavior
        }  // line 289: executes this statement as part of this file's behavior
        clipboard.setPrimaryClip(ClipData.newPlainText("workflow_help", instructions))  // line 290: executes this statement as part of this file's behavior
        Toast.makeText(requireContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()  // line 291: executes this statement as part of this file's behavior
    }  // line 292: executes this statement as part of this file's behavior

    private fun createSecret() = lifecycleScope.launch {  // line 294: executes this statement as part of this file's behavior
        val value = valueInput.text.toString()  // line 295: executes this statement as part of this file's behavior
        val name = nameInput.text.toString().trim()  // line 296: executes this statement as part of this file's behavior
        if (name.isBlank()) { status.text = "Secret name is required."; return@launch }  // line 297: executes this statement as part of this file's behavior
        if (value.isBlank()) { status.text = "Secret value is required."; return@launch }  // line 298: executes this statement as part of this file's behavior
        val request = SecretCreateRequest(projectId = projectInput.text.toString().takeIf { it.isNotBlank() }, name = name, type = typeInput.text.toString().ifBlank { "custom" }, value = value)  // line 299: executes this statement as part of this file's behavior
        status.text = "Saving secret reference…"  // line 300: executes this statement as part of this file's behavior
        when (val result = secretRepository.createSecret(request)) {  // line 301: executes this statement as part of this file's behavior
            is ApiResult.Success -> { valueInput.text?.clear(); status.text = getString(R.string.secret_saved_hidden); loadSecrets() }  // line 302: executes this statement as part of this file's behavior
            is ApiResult.Error -> { valueInput.text?.clear(); status.text = "Error: ${result.message}" }  // line 303: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 304: executes this statement as part of this file's behavior
        }  // line 305: executes this statement as part of this file's behavior
    }  // line 306: executes this statement as part of this file's behavior

    private fun rotateSecret(secret: SecretSummary) = lifecycleScope.launch {  // line 308: executes this statement as part of this file's behavior
        val value = valueInput.text.toString()  // line 309: executes this statement as part of this file's behavior
        if (value.isBlank()) { status.text = "Enter the new secret value in the secret value field first."; return@launch }  // line 310: executes this statement as part of this file's behavior
        status.text = "Rotating ${secret.name}…"  // line 311: executes this statement as part of this file's behavior
        when (val result = secretRepository.rotateSecret(secret.id, value)) {  // line 312: executes this statement as part of this file's behavior
            is ApiResult.Success -> { valueInput.text?.clear(); status.text = getString(R.string.secret_saved_hidden); loadSecrets() }  // line 313: executes this statement as part of this file's behavior
            is ApiResult.Error -> { valueInput.text?.clear(); status.text = "Error: ${result.message}" }  // line 314: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 315: executes this statement as part of this file's behavior
        }  // line 316: executes this statement as part of this file's behavior
    }  // line 317: executes this statement as part of this file's behavior

    private fun confirmDeleteSecret(secret: SecretSummary) {  // line 319: executes this statement as part of this file's behavior
        AlertDialog.Builder(requireContext())  // line 320: executes this statement as part of this file's behavior
            .setTitle("Delete secret reference?")  // line 321: executes this statement as part of this file's behavior
            .setMessage("Delete ${secret.name}? The full value will remain hidden and cannot be recovered from this app.")  // line 322: executes this statement as part of this file's behavior
            .setNegativeButton(android.R.string.cancel, null)  // line 323: executes this statement as part of this file's behavior
            .setPositiveButton(android.R.string.ok) { _, _ -> deleteSecret(secret) }  // line 324: executes this statement as part of this file's behavior
            .show()  // line 325: executes this statement as part of this file's behavior
    }  // line 326: executes this statement as part of this file's behavior

    private fun deleteSecret(secret: SecretSummary) = lifecycleScope.launch {  // line 328: executes this statement as part of this file's behavior
        status.text = "Deleting ${secret.name}…"  // line 329: executes this statement as part of this file's behavior
        when (val result = secretRepository.deleteSecret(secret.id)) {  // line 330: executes this statement as part of this file's behavior
            is ApiResult.Success -> loadSecrets()  // line 331: executes this statement as part of this file's behavior
            is ApiResult.Error -> status.text = "Error: ${result.message}"  // line 332: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 333: executes this statement as part of this file's behavior
        }  // line 334: executes this statement as part of this file's behavior
    }  // line 335: executes this statement as part of this file's behavior

    private fun renderSecrets(secrets: List<SecretSummary>) {  // line 337: executes this statement as part of this file's behavior
        list.removeAllViews()  // line 338: executes this statement as part of this file's behavior
        if (secrets.isEmpty()) { status.text = "No secrets saved."; return }  // line 339: executes this statement as part of this file's behavior
        secrets.forEach { secret ->  // line 340: executes this statement as part of this file's behavior
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 12, 0, 12) }  // line 341: executes this statement as part of this file's behavior
            row.addView(TextView(requireContext()).apply { text = "${secret.name} • ${secret.type}\n${secret.storageMode} • ${secret.fingerprint}\nUpdated: ${secret.updatedAt}\nProject: ${secret.projectId ?: "global"}" })  // line 342: executes this statement as part of this file's behavior
            row.addView(Button(requireContext()).apply { text = getString(R.string.rotate_secret); setOnClickListener { rotateSecret(secret) } })  // line 343: executes this statement as part of this file's behavior
            row.addView(Button(requireContext()).apply { text = getString(R.string.delete_secret); setOnClickListener { confirmDeleteSecret(secret) } })  // line 344: executes this statement as part of this file's behavior
            list.addView(row)  // line 345: executes this statement as part of this file's behavior
        }  // line 346: executes this statement as part of this file's behavior
        status.text = "Loaded ${secrets.size} secret reference(s). Values stay hidden."  // line 347: executes this statement as part of this file's behavior
    }  // line 348: executes this statement as part of this file's behavior

    private fun setupTokenSection(view: View) {  // line 350: executes this statement as part of this file's behavior
        val tokenMasked = view.findViewById<TextView>(R.id.bot_token_masked)  // line 351: executes this statement as part of this file's behavior
        val changeButton = view.findViewById<Button>(R.id.change_bot_token_button)  // line 352: executes this statement as part of this file's behavior
        val clearButton = view.findViewById<Button>(R.id.clear_bot_token_button)  // line 353: executes this statement as part of this file's behavior
        fun refresh() { tokenMasked.text = tokenRepository.getTokenMasked() ?: "Not set" }  // line 354: executes this statement as part of this file's behavior
        refresh()  // line 355: executes this statement as part of this file's behavior
        changeButton.setOnClickListener {  // line 356: executes this statement as part of this file's behavior
            val input = EditText(requireContext())  // line 357: executes this statement as part of this file's behavior
            AlertDialog.Builder(requireContext()).setTitle("Change Bot Token").setView(input).setPositiveButton("Save") { _, _ ->  // line 358: executes this statement as part of this file's behavior
                val token = input.text.toString().trim()  // line 359: executes this statement as part of this file's behavior
                if (token.isNotBlank()) tokenRepository.setToken(token)  // line 360: executes this statement as part of this file's behavior
                refresh()  // line 361: executes this statement as part of this file's behavior
            }.setNegativeButton("Cancel", null).show()  // line 362: executes this statement as part of this file's behavior
        }  // line 363: executes this statement as part of this file's behavior
        clearButton.setOnClickListener { tokenRepository.clearToken(); refresh() }  // line 364: executes this statement as part of this file's behavior
    }  // line 365: executes this statement as part of this file's behavior

    private fun setupAutoStart(view: View) {  // line 367: executes this statement as part of this file's behavior
        val autoStart = view.findViewById<android.widget.Switch>(R.id.auto_start_switch)  // line 368: executes this statement as part of this file's behavior
        val disclaimer = view.findViewById<TextView>(R.id.auto_start_disclaimer)  // line 369: executes this statement as part of this file's behavior
        lifecycleScope.launch {  // line 370: executes this statement as part of this file's behavior
            EnginePreferences.autoStartOnBoot(requireContext()).collect { enabled ->  // line 371: executes this statement as part of this file's behavior
                autoStart.isChecked = enabled  // line 372: executes this statement as part of this file's behavior
                disclaimer.visibility = if (enabled) View.VISIBLE else View.GONE  // line 373: executes this statement as part of this file's behavior
            }  // line 374: executes this statement as part of this file's behavior
        }  // line 375: executes this statement as part of this file's behavior
        autoStart.setOnCheckedChangeListener { _, checked -> lifecycleScope.launch { EnginePreferences.setAutoStartOnBoot(requireContext(), checked) } }  // line 376: executes this statement as part of this file's behavior
    }  // line 377: executes this statement as part of this file's behavior
}  // line 378: executes this statement as part of this file's behavior
