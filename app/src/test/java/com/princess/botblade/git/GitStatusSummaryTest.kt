package com.princess.botblade.git

import kotlin.test.Test
import kotlin.test.assertEquals

class GitStatusSummaryTest {
    @Test
    fun dirtyCountUsesUniquePathsAcrossStatusBuckets() {
        val summary = GitStatusSummary(
            clean = false,
            added = listOf("src/A.kt"),
            changed = listOf("src/A.kt", "src/B.kt"),
            modified = listOf("src/B.kt"),
            missing = listOf("src/C.kt"),
            removed = listOf("src/C.kt"),
            untracked = listOf("README.md"),
            conflicting = listOf("README.md"),
        )

        assertEquals(4, summary.dirtyFileCount)
    }
}
