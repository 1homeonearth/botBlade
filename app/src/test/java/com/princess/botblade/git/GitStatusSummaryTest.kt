// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.git  // line 7: executes this statement as part of this file's behavior

import kotlin.test.Test  // line 9: executes this statement as part of this file's behavior
import kotlin.test.assertEquals  // line 10: executes this statement as part of this file's behavior

class GitStatusSummaryTest {  // line 12: executes this statement as part of this file's behavior
    @Test  // line 13: executes this statement as part of this file's behavior
    fun dirtyCountUsesUniquePathsAcrossStatusBuckets() {  // line 14: executes this statement as part of this file's behavior
        val summary = GitStatusSummary(  // line 15: executes this statement as part of this file's behavior
            clean = false,  // line 16: executes this statement as part of this file's behavior
            added = listOf("src/A.kt"),  // line 17: executes this statement as part of this file's behavior
            changed = listOf("src/A.kt", "src/B.kt"),  // line 18: executes this statement as part of this file's behavior
            modified = listOf("src/B.kt"),  // line 19: executes this statement as part of this file's behavior
            missing = listOf("src/C.kt"),  // line 20: executes this statement as part of this file's behavior
            removed = listOf("src/C.kt"),  // line 21: executes this statement as part of this file's behavior
            untracked = listOf("README.md"),  // line 22: executes this statement as part of this file's behavior
            conflicting = listOf("README.md"),  // line 23: executes this statement as part of this file's behavior
        )  // line 24: executes this statement as part of this file's behavior

        assertEquals(4, summary.dirtyFileCount)  // line 26: executes this statement as part of this file's behavior
    }  // line 27: executes this statement as part of this file's behavior
}  // line 28: executes this statement as part of this file's behavior
