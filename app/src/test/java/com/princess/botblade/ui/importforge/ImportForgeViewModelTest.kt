package com.princess.botblade.ui.importforge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportForgeViewModelTest {
    @Test
    fun exposesAllWizardSourceOptionsWhenSafImportIsEnabled() {
        val vm = ImportForgeViewModel()
        val state = vm.uiState.value

        assertEquals(ImportForgeStep.SOURCE, state.step)
        assertEquals(
            listOf(
                ImportForgeSourceType.GIT_URL,
                ImportForgeSourceType.ZIP,
                ImportForgeSourceType.LOCAL_FOLDER,
                ImportForgeSourceType.WORKFLOW_JSON,
                ImportForgeSourceType.FIRST_PARTY_TEMPLATE,
            ),
            state.sourceOptions.map { it.type },
        )
        assertTrue(state.sourceOptions.first { it.type == ImportForgeSourceType.LOCAL_FOLDER }.enabled)
    }

    @Test
    fun disablesLocalFolderOptionWhenSafImportIsDisabled() {
        val vm = ImportForgeViewModel()

        vm.setSafImportEnabled(false)

        val localFolder = vm.uiState.value.sourceOptions.first { it.type == ImportForgeSourceType.LOCAL_FOLDER }
        assertFalse(localFolder.enabled)
    }

    @Test
    fun selectingWorkflowJsonUpdatesDetectorConfigureAndScriptPreview() {
        val vm = ImportForgeViewModel()

        vm.selectSource(ImportForgeSourceType.WORKFLOW_JSON)
        val detected = vm.uiState.value

        assertEquals(ImportForgeStep.DETECT, detected.step)
        assertEquals("workflow-json", detected.recommendedBladePack.id)
        assertTrue(detected.recommendedBladePack.matchedEvidence.any { it.contains("credential references") })
        assertTrue(detected.requiredSecrets.all { !it.configured })
        assertTrue(detected.commandPlan.any { it.source == "validate" })
        assertTrue(detected.scriptProfilePreview.any { it.source == "workflow JSON" })
    }

    @Test
    fun previewScriptProfileMovesToPhaseFourWithoutRunningAnything() {
        val vm = ImportForgeViewModel()

        vm.previewScriptProfile()

        val state = vm.uiState.value
        assertEquals(ImportForgeStep.BUILD_RUN_SCRIPT_PREVIEW, state.step)
        assertEquals(ImportForgeBackendState.PROFILE_READY, state.backendState)
        assertTrue(state.timelineEvents.last().contains("preview ready"))
    }

    @Test
    fun transitionsToConfigureWhenBackendReportsMissingSecrets() {
        val vm = ImportForgeViewModel()
        vm.startImport("import_1")
        vm.onBackendState(ImportForgeBackendState.MISSING_SECRETS, "missing DISCORD_TOKEN")
        val state = vm.uiState.value
        assertEquals(ImportForgeStep.CONFIGURE, state.step)
        assertEquals(ImportForgeBackendState.MISSING_SECRETS, state.backendState)
        assertTrue(state.timelineEvents.last().contains("missing"))
    }

    @Test
    fun blockedPolicyMessageIsRetainedForRepairCardsRendering() {
        val vm = ImportForgeViewModel()
        vm.startImport("import_2")
        vm.onBackendState(ImportForgeBackendState.BLOCKED_POLICY, "blocked by policy: forbidden archive")
        val state = vm.uiState.value
        assertEquals(ImportForgeStep.CONFIGURE, state.step)
        assertEquals("blocked by policy: forbidden archive", state.blockedPolicyMessage)
        assertTrue(state.repairCards.any { it.title == "Policy block" })
    }
}
