package com.princess.royalscepter.data.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActiveProjectStoreTest {
    private lateinit var context: Context
    private lateinit var store: ActiveProjectStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = ActiveProjectStore(context)
        store.setActiveProject(null, null)
    }

    @Test
    fun setActiveProjectPersistsIdAndNameAcrossStoreInstances() {
        store.setActiveProject("project-123", "Princess Helper")

        val secondStore = ActiveProjectStore(context)

        assertEquals("project-123", secondStore.getActiveProjectId())
        assertEquals("Princess Helper", secondStore.getActiveProjectName())
    }

    @Test
    fun setActiveProjectWithBlankIdClearsIdAndName() {
        store.setActiveProject("project-123", "Princess Helper")

        store.setActiveProject(" ", "Ignored")

        assertNull(store.getActiveProjectId())
        assertNull(store.getActiveProjectName())
    }

    @Test
    fun setActiveProjectIdClearsIdAndKeepsNoStaleName() {
        store.setActiveProject("project-123", "Princess Helper")

        store.setActiveProjectId(null)

        assertNull(store.getActiveProjectId())
        assertNull(store.getActiveProjectName())
    }

    @Test
    fun setActiveProjectWithBlankNameClearsOnlyName() {
        store.setActiveProject("project-123", "Princess Helper")

        store.setActiveProject("project-456", "")

        assertEquals("project-456", store.getActiveProjectId())
        assertNull(store.getActiveProjectName())
    }
}
