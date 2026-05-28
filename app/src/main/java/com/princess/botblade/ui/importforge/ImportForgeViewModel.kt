package com.princess.botblade.ui.importforge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.model.ImportStartRequest
import com.princess.botblade.data.repository.ImportRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ImportForgeStep { SOURCE_PICKER, TIMELINE, PROFILE, MISSING_SECRETS, REPAIR_CARDS }
enum class ImportForgeBackendState { QUEUED, ANALYZING, PROFILE_READY, MISSING_SECRETS, BLOCKED_POLICY, REPAIR_SUGGESTED, COMPLETE, FAILED }

data class ImportForgeUiState(
    val step: ImportForgeStep = ImportForgeStep.SOURCE_PICKER,
    val backendState: ImportForgeBackendState? = null,
    val importId: String? = null,
    val blockedPolicyMessage: String? = null,
    val timelineEvents: List<String> = emptyList(),
)

class ImportForgeViewModel(
    private val importRepository: ImportRepository = ImportRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ImportForgeUiState())
    val uiState: StateFlow<ImportForgeUiState> = _uiState.asStateFlow()

    fun startImport(importId: String) {
        _uiState.value = _uiState.value.copy(step = ImportForgeStep.TIMELINE, importId = importId, backendState = ImportForgeBackendState.QUEUED, timelineEvents = listOf("Import queued"))
    }

    fun startImport(sourceType: String, source: String, workspacePath: String) {
        _uiState.value = _uiState.value.copy(step = ImportForgeStep.TIMELINE, backendState = ImportForgeBackendState.QUEUED, timelineEvents = _uiState.value.timelineEvents + "Starting $sourceType import")
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
            ImportForgeBackendState.QUEUED, ImportForgeBackendState.ANALYZING -> ImportForgeStep.TIMELINE
            ImportForgeBackendState.PROFILE_READY, ImportForgeBackendState.COMPLETE -> ImportForgeStep.PROFILE
            ImportForgeBackendState.MISSING_SECRETS -> ImportForgeStep.MISSING_SECRETS
            ImportForgeBackendState.BLOCKED_POLICY, ImportForgeBackendState.FAILED, ImportForgeBackendState.REPAIR_SUGGESTED -> ImportForgeStep.REPAIR_CARDS
        }
        val eventText = detail ?: state.name.lowercase().replace('_', ' ')
        _uiState.value = _uiState.value.copy(step = step, backendState = state, blockedPolicyMessage = if (state == ImportForgeBackendState.BLOCKED_POLICY) (detail ?: "Blocked by import policy") else _uiState.value.blockedPolicyMessage, timelineEvents = _uiState.value.timelineEvents + eventText)
    }
}
