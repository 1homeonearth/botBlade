package com.princess.botblade.ui.importforge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.model.ImportStartRequest
import com.princess.botblade.data.repository.ImportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ImportForgeStep(val title: String) {
    SOURCE("Source"),
    DETECT("Detect"),
    CONFIGURE("Configure"),
    BUILD_RUN_SCRIPT_PREVIEW("Build, Run & Script preview"),
}

enum class ImportForgeBackendState { QUEUED, ANALYZING, PROFILE_READY, MISSING_SECRETS, BLOCKED_POLICY, REPAIR_SUGGESTED, COMPLETE, FAILED }

enum class ImportForgeSourceType(val label: String, val apiType: String, val workspacePrefix: String) {
    GIT_URL("Git URL", "git", "imports/git"),
    ZIP("ZIP", "zip", "imports/zip"),
    LOCAL_FOLDER("Local folder", "folder", "imports/folder"),
    WORKFLOW_JSON("workflow JSON", "workflow_json", "imports/workflow-json"),
    FIRST_PARTY_TEMPLATE("first-party template", "template", "imports/templates"),
}

data class ImportForgeSourceOption(
    val type: ImportForgeSourceType,
    val title: String,
    val detail: String,
    val enabled: Boolean = true,
)

data class ImportForgeSecretPreview(
    val name: String,
    val configured: Boolean,
)

data class ImportForgeBladePackPreview(
    val id: String,
    val name: String,
    val confidenceScore: Int,
    val matchedEvidence: List<String>,
)

data class ImportForgeCommandPlanPreview(
    val source: String,
    val commands: List<String>,
)

data class ImportForgeRepairCardPreview(
    val title: String,
    val evidence: String,
    val safeAction: String,
)

data class ImportForgeScriptProfilePreview(
    val source: String,
    val profiles: List<String>,
)

data class ImportForgeUiState(
    val step: ImportForgeStep = ImportForgeStep.SOURCE,
    val backendState: ImportForgeBackendState? = null,
    val importId: String? = null,
    val blockedPolicyMessage: String? = null,
    val timelineEvents: List<String> = emptyList(),
    val safImportEnabled: Boolean = true,
    val selectedSourceType: ImportForgeSourceType = ImportForgeSourceType.GIT_URL,
    val sourceOptions: List<ImportForgeSourceOption> = defaultSourceOptions(),
    val recommendedBladePack: ImportForgeBladePackPreview = previewFor(ImportForgeSourceType.GIT_URL),
    val requiredSecrets: List<ImportForgeSecretPreview> = defaultRequiredSecrets(ImportForgeSourceType.GIT_URL),
    val optionalSecrets: List<ImportForgeSecretPreview> = defaultOptionalSecrets(ImportForgeSourceType.GIT_URL),
    val commandPlan: List<ImportForgeCommandPlanPreview> = defaultCommandPlan(ImportForgeSourceType.GIT_URL),
    val repairCards: List<ImportForgeRepairCardPreview> = defaultRepairCards(),
    val scriptProfilePreview: List<ImportForgeScriptProfilePreview> = defaultScriptProfilePreview(ImportForgeSourceType.GIT_URL),
)

class ImportForgeViewModel(
    private val importRepository: ImportRepository = ImportRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ImportForgeUiState())
    val uiState: StateFlow<ImportForgeUiState> = _uiState.asStateFlow()

    fun setSafImportEnabled(enabled: Boolean) {
        val nextOptions = defaultSourceOptions(enabled)
        val nextSource = if (!enabled && _uiState.value.selectedSourceType == ImportForgeSourceType.LOCAL_FOLDER) ImportForgeSourceType.GIT_URL else _uiState.value.selectedSourceType
        _uiState.value = _uiState.value.copy(
            safImportEnabled = enabled,
            sourceOptions = nextOptions,
            selectedSourceType = nextSource,
        ).withPreview(nextSource)
    }

    fun selectSource(type: ImportForgeSourceType) {
        val option = _uiState.value.sourceOptions.firstOrNull { it.type == type }
        if (option?.enabled == false) return
        _uiState.value = _uiState.value.copy(
            selectedSourceType = type,
            step = ImportForgeStep.DETECT,
            backendState = ImportForgeBackendState.ANALYZING,
            timelineEvents = _uiState.value.timelineEvents + "Detecting ${type.label} source",
        ).withPreview(type)
    }

    fun continueToConfigure() {
        _uiState.value = _uiState.value.copy(step = ImportForgeStep.CONFIGURE)
    }

    fun previewScriptProfile() {
        _uiState.value = _uiState.value.copy(
            step = ImportForgeStep.BUILD_RUN_SCRIPT_PREVIEW,
            backendState = ImportForgeBackendState.PROFILE_READY,
            timelineEvents = _uiState.value.timelineEvents + "Script profile preview ready",
        )
    }

    fun saveProfile() {
        _uiState.value = _uiState.value.copy(
            step = ImportForgeStep.BUILD_RUN_SCRIPT_PREVIEW,
            backendState = ImportForgeBackendState.COMPLETE,
            timelineEvents = _uiState.value.timelineEvents + "Profile saved",
        )
    }

    fun startImport(importId: String) {
        _uiState.value = _uiState.value.copy(
            step = ImportForgeStep.DETECT,
            importId = importId,
            backendState = ImportForgeBackendState.QUEUED,
            timelineEvents = listOf("Import queued"),
        )
    }

    fun startImport(sourceType: String, source: String, workspacePath: String) {
        val selectedType = sourceType.toSourceType() ?: _uiState.value.selectedSourceType
        _uiState.value = _uiState.value.copy(
            selectedSourceType = selectedType,
            step = ImportForgeStep.DETECT,
            backendState = ImportForgeBackendState.QUEUED,
            timelineEvents = _uiState.value.timelineEvents + "Starting ${selectedType.label} import",
        ).withPreview(selectedType)
        viewModelScope.launch {
            when (val result = importRepository.startImport(ImportStartRequest(sourceType = sourceType, source = source, workspacePath = workspacePath))) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(importId = result.data.id)
                    onBackendState(mapBackendState(result.data.state), result.data.blockedPolicy)
                }
                is ApiResult.Error -> onBackendState(ImportForgeBackendState.FAILED, result.message)
                ApiResult.Loading -> Unit
            }
        }
    }

    private fun mapBackendState(raw: String): ImportForgeBackendState = when (raw.trim().lowercase()) {
        "pending", "queued" -> ImportForgeBackendState.QUEUED
        "scanning", "detecting", "analyzing" -> ImportForgeBackendState.ANALYZING
        "profile_ready" -> ImportForgeBackendState.PROFILE_READY
        "needs_secrets", "missing_secrets" -> ImportForgeBackendState.MISSING_SECRETS
        "blocked_by_policy", "blocked_policy" -> ImportForgeBackendState.BLOCKED_POLICY
        "repair_suggested", "blocked" -> ImportForgeBackendState.REPAIR_SUGGESTED
        "ready", "completed", "complete" -> ImportForgeBackendState.COMPLETE
        "failed" -> ImportForgeBackendState.FAILED
        else -> ImportForgeBackendState.FAILED
    }

    fun onBackendState(state: ImportForgeBackendState, detail: String? = null) {
        val step = when (state) {
            ImportForgeBackendState.QUEUED, ImportForgeBackendState.ANALYZING -> ImportForgeStep.DETECT
            ImportForgeBackendState.MISSING_SECRETS -> ImportForgeStep.CONFIGURE
            ImportForgeBackendState.PROFILE_READY, ImportForgeBackendState.COMPLETE -> ImportForgeStep.BUILD_RUN_SCRIPT_PREVIEW
            ImportForgeBackendState.BLOCKED_POLICY, ImportForgeBackendState.FAILED, ImportForgeBackendState.REPAIR_SUGGESTED -> ImportForgeStep.CONFIGURE
        }
        val eventText = detail ?: state.name.lowercase().replace('_', ' ')
        val repairCards = if (state == ImportForgeBackendState.BLOCKED_POLICY && detail != null) {
            defaultRepairCards() + ImportForgeRepairCardPreview(
                title = "Policy block",
                evidence = detail,
                safeAction = "Review the source, remove blocked content, then import again.",
            )
        } else {
            _uiState.value.repairCards
        }
        _uiState.value = _uiState.value.copy(
            step = step,
            backendState = state,
            blockedPolicyMessage = if (state == ImportForgeBackendState.BLOCKED_POLICY) (detail ?: "Blocked by import policy") else _uiState.value.blockedPolicyMessage,
            timelineEvents = _uiState.value.timelineEvents + eventText,
            repairCards = repairCards,
        )
    }

    private fun ImportForgeUiState.withPreview(type: ImportForgeSourceType): ImportForgeUiState = copy(
        recommendedBladePack = previewFor(type),
        requiredSecrets = defaultRequiredSecrets(type),
        optionalSecrets = defaultOptionalSecrets(type),
        commandPlan = defaultCommandPlan(type),
        scriptProfilePreview = defaultScriptProfilePreview(type),
    )
}

private fun String.toSourceType(): ImportForgeSourceType? = ImportForgeSourceType.entries.firstOrNull { sourceType ->
    equals(sourceType.apiType, ignoreCase = true) || equals(sourceType.name, ignoreCase = true)
}

private fun defaultSourceOptions(safImportEnabled: Boolean = true): List<ImportForgeSourceOption> = listOf(
    ImportForgeSourceOption(ImportForgeSourceType.GIT_URL, "Git URL", "Clone a repository with static detection before any build command is suggested."),
    ImportForgeSourceOption(ImportForgeSourceType.ZIP, "ZIP", "Inspect an on-device archive through the ZIP import safety gate."),
    ImportForgeSourceOption(ImportForgeSourceType.LOCAL_FOLDER, "Local folder", "Import a Storage Access Framework folder without raw path guessing.", enabled = safImportEnabled),
    ImportForgeSourceOption(ImportForgeSourceType.WORKFLOW_JSON, "workflow JSON", "Preview a workflow importer for n8n-style JSON without treating secrets as values."),
    ImportForgeSourceOption(ImportForgeSourceType.FIRST_PARTY_TEMPLATE, "first-party template", "Start from a reviewed BotBlade template and save a profile."),
)

private fun previewFor(type: ImportForgeSourceType): ImportForgeBladePackPreview = when (type) {
    ImportForgeSourceType.GIT_URL -> ImportForgeBladePackPreview("discord-js", "Discord.js Blade Pack", 91, listOf("package.json depends on discord.js", "src/commands contains slash-command modules", "Node runtime scripts detected"))
    ImportForgeSourceType.ZIP -> ImportForgeBladePackPreview("generic-node", "Generic Node Blade Pack", 78, listOf("package.json found inside archive", "npm scripts present", "archive entries passed path policy"))
    ImportForgeSourceType.LOCAL_FOLDER -> ImportForgeBladePackPreview("generic-python", "Generic Python Blade Pack", 74, listOf("requirements.txt detected", "bot.py entrypoint candidate", "folder selected through SAF"))
    ImportForgeSourceType.WORKFLOW_JSON -> ImportForgeBladePackPreview("workflow-json", "Workflow JSON Blade Pack", 88, listOf("nodes array detected", "credential references found without values", "workflow trigger node present"))
    ImportForgeSourceType.FIRST_PARTY_TEMPLATE -> ImportForgeBladePackPreview("botblade-template", "BotBlade First-party Template Pack", 96, listOf("template manifest is first-party", "runtime profile included", "starter commands are generated"))
}

private fun defaultRequiredSecrets(type: ImportForgeSourceType): List<ImportForgeSecretPreview> = when (type) {
    ImportForgeSourceType.GIT_URL -> listOf(ImportForgeSecretPreview("DISCORD_TOKEN", false))
    ImportForgeSourceType.ZIP -> listOf(ImportForgeSecretPreview("BOT_TOKEN", false))
    ImportForgeSourceType.LOCAL_FOLDER -> listOf(ImportForgeSecretPreview("PYTHON_BOT_TOKEN", false))
    ImportForgeSourceType.WORKFLOW_JSON -> listOf(ImportForgeSecretPreview("WORKFLOW_CREDENTIAL_REF", false))
    ImportForgeSourceType.FIRST_PARTY_TEMPLATE -> listOf(ImportForgeSecretPreview("DISCORD_TOKEN", false))
}

private fun defaultOptionalSecrets(type: ImportForgeSourceType): List<ImportForgeSecretPreview> = when (type) {
    ImportForgeSourceType.GIT_URL -> listOf(ImportForgeSecretPreview("GITHUB_TOKEN_REF", true), ImportForgeSecretPreview("DISCORD_CLIENT_ID", false))
    ImportForgeSourceType.ZIP -> listOf(ImportForgeSecretPreview("NPM_TOKEN_REF", false))
    ImportForgeSourceType.LOCAL_FOLDER -> listOf(ImportForgeSecretPreview("DATABASE_URL_REF", true))
    ImportForgeSourceType.WORKFLOW_JSON -> listOf(ImportForgeSecretPreview("WEBHOOK_SECRET_REF", false))
    ImportForgeSourceType.FIRST_PARTY_TEMPLATE -> listOf(ImportForgeSecretPreview("DISCORD_GUILD_ID", false))
}

private fun defaultCommandPlan(type: ImportForgeSourceType): List<ImportForgeCommandPlanPreview> = when (type) {
    ImportForgeSourceType.GIT_URL -> listOf(
        ImportForgeCommandPlanPreview("install", listOf("npm ci")),
        ImportForgeCommandPlanPreview("build", listOf("npm run build")),
        ImportForgeCommandPlanPreview("validate", listOf("npm test -- --runInBand")),
        ImportForgeCommandPlanPreview("start", listOf("npm start")),
    )
    ImportForgeSourceType.ZIP -> listOf(
        ImportForgeCommandPlanPreview("extract", listOf("validate archive entries", "materialize safe workspace")),
        ImportForgeCommandPlanPreview("install", listOf("npm install")),
        ImportForgeCommandPlanPreview("build", listOf("npm run build --if-present")),
    )
    ImportForgeSourceType.LOCAL_FOLDER -> listOf(
        ImportForgeCommandPlanPreview("inspect", listOf("scan SAF tree metadata")),
        ImportForgeCommandPlanPreview("install", listOf("python -m pip install -r requirements.txt")),
        ImportForgeCommandPlanPreview("start", listOf("python bot.py")),
    )
    ImportForgeSourceType.WORKFLOW_JSON -> listOf(
        ImportForgeCommandPlanPreview("validate", listOf("parse workflow JSON", "map credential references")),
        ImportForgeCommandPlanPreview("export", listOf("save BotBlade workflow profile")),
    )
    ImportForgeSourceType.FIRST_PARTY_TEMPLATE -> listOf(
        ImportForgeCommandPlanPreview("generate", listOf("copy reviewed template files")),
        ImportForgeCommandPlanPreview("install", listOf("npm ci")),
        ImportForgeCommandPlanPreview("build", listOf("npm run build")),
    )
}

private fun defaultRepairCards(): List<ImportForgeRepairCardPreview> = listOf(
    ImportForgeRepairCardPreview("Missing secret reference", "Required secret is not configured.", "Open Configure and bind an existing Vault reference."),
    ImportForgeRepairCardPreview("Command needs review", "Start command was detected from package metadata.", "Preview the script profile before saving it."),
)

private fun defaultScriptProfilePreview(type: ImportForgeSourceType): List<ImportForgeScriptProfilePreview> = when (type) {
    ImportForgeSourceType.GIT_URL -> listOf(
        ImportForgeScriptProfilePreview("package.json", listOf("build: npm run build", "start: npm start")),
        ImportForgeScriptProfilePreview("Blade Pack", listOf("validate: npm test -- --runInBand")),
    )
    ImportForgeSourceType.ZIP -> listOf(
        ImportForgeScriptProfilePreview("archive manifest", listOf("extract: validate archive entries", "build: npm run build --if-present")),
    )
    ImportForgeSourceType.LOCAL_FOLDER -> listOf(
        ImportForgeScriptProfilePreview("SAF folder", listOf("install: python -m pip install -r requirements.txt", "start: python bot.py")),
    )
    ImportForgeSourceType.WORKFLOW_JSON -> listOf(
        ImportForgeScriptProfilePreview("workflow JSON", listOf("validate: parse workflow JSON", "save: BotBlade workflow profile")),
    )
    ImportForgeSourceType.FIRST_PARTY_TEMPLATE -> listOf(
        ImportForgeScriptProfilePreview("template manifest", listOf("generate: copy reviewed files", "build: npm run build")),
        ImportForgeScriptProfilePreview("Blade Pack", listOf("save: template runtime profile")),
    )
}
