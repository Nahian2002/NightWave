package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioUiStateSafetyPolicyTest {
    private val p = StudioUiStateSafetyPolicy

    @Test fun validLibraryTabsSurviveRecreation() {
        val names = listOf("Night rides", "Favorites mix")
        val smart = listOf("nightwave://smart/recent")
        assertEquals("Night rides", p.resolveCollection("Night rides", names, smart,
            "nightwave://liked", "nightwave://recent"))
        assertEquals("nightwave://liked", p.resolveCollection(
            "nightwave://liked", names, smart, "nightwave://liked", "nightwave://recent"))
        assertEquals("nightwave://smart/recent", p.resolveCollection(
            "nightwave://smart/recent", names, smart, "nightwave://liked", "nightwave://recent"))
    }

    @Test fun removedPlaylistOrSmartCollectionGoesBackToLibraryOverview() {
        assertNull(p.resolveCollection("Deleted playlist", listOf("Different"), emptyList(),
            "liked", "history"))
        assertNull(p.resolveCollection("smart:expired", emptyList(), listOf("smart:new"),
            "liked", "history"))
        assertNull(p.resolveCollection(null, emptyList(), emptyList(), "liked", "history"))
    }

    @Test fun delayedNativeDialogIndicesNeverThrowOrChooseUnrelatedItems() {
        val options = listOf("A", "B", "C")
        assertEquals("B", p.itemAt(options, 1))
        assertNull(p.itemAt(options, -1))
        assertNull(p.itemAt(options, 3))
        assertNull(p.itemAt(options, Int.MAX_VALUE))
        assertNull(p.itemAt(emptyList<String>(), 0))
    }

    @Test fun createdPlaylistNameComesFromActualNewSnapshotRatherThanLastRow() {
        val before = listOf("Existing", "Archived")
        assertEquals("Shared", p.createdPlaylistName(before,
            listOf("Existing", "Shared", "Archived")))
        assertNull(p.createdPlaylistName(before, listOf("Existing", "Archived")))
        assertNull(p.createdPlaylistName(before, emptyList()))
        assertEquals("Newer", p.createdPlaylistName(before,
            listOf("Shared", "Newer", "Existing")))
    }

    @Test fun selectionPolicyDoesNotMutateInputs() {
        val names = mutableListOf("Current")
        val smart = mutableListOf("auto")
        p.resolveCollection("Missing", names, smart, "liked", "recent")
        p.createdPlaylistName(names, smart)
        assertEquals(listOf("Current"), names)
        assertEquals(listOf("auto"), smart)
        assertTrue(p.itemAt(names, 0) == "Current")
    }
}
