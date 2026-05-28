package com.princess.botblade.ui.importforge

import androidx.lifecycle.ViewModel
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

class ImportForgeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ImportForgeUiState())
    val uiState: StateFlow<ImportForgeUiState> = _uiState.asStateFlow()

    fun startImport(importId: String) {
        _uiState.value = _uiState.value.copy(step = ImportForgeStep.TIMELINE, importId = importId, backendState = ImportForgeBackendState.QUEUED, timelineEvents = listOf("Import queued"))
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
