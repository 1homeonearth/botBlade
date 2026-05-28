package com.princess.botblade.ui.importforge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportForgeViewModelTest {
    @Test
    fun transitionsToMissingSecretsWhenBackendReportsMissingSecrets() {
        val vm = ImportForgeViewModel()
        vm.startImport("import_1")
        vm.onBackendState(ImportForgeBackendState.MISSING_SECRETS, "missing DISCORD_TOKEN")
        val state = vm.uiState.value
        assertEquals(ImportForgeStep.MISSING_SECRETS, state.step)
        assertEquals(ImportForgeBackendState.MISSING_SECRETS, state.backendState)
        assertTrue(state.timelineEvents.last().contains("missing"))
    }

    @Test
    fun blockedPolicyMessageIsRetainedForRepairCardsRendering() {
        val vm = ImportForgeViewModel()
        vm.startImport("import_2")
        vm.onBackendState(ImportForgeBackendState.BLOCKED_POLICY, "blocked by policy: forbidden archive")
        val state = vm.uiState.value
        assertEquals(ImportForgeStep.REPAIR_CARDS, state.step)
        assertEquals("blocked by policy: forbidden archive", state.blockedPolicyMessage)
    }
}
