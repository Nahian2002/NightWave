package com.nightwave.player.v2.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistPinPolicyTest {
    @Test fun pinnedAreFirstWithoutChangingWithinGroupSortOrder() {
        val items = listOf("B", "A", "C", "D")
        assertEquals(listOf("A", "D", "B", "C"),
            PlaylistPinPolicy.pinnedFirst(items, listOf("D", "A")) { it })
        assertEquals(items, PlaylistPinPolicy.pinnedFirst(items, emptyList()) { it })
    }

    @Test fun togglingPinsDoesNotChangeUnrelatedNames() {
        val known = listOf("Night", "Drive", "Focus")
        assertEquals(listOf("Focus", "Night"), PlaylistPinPolicy.toggle(
            listOf("Focus"), "Night", known))
        assertEquals(emptyList<String>(), PlaylistPinPolicy.toggle(
            listOf("Night"), "Night", known))
        assertNull(PlaylistPinPolicy.toggle(emptyList(), "Missing", known))
    }

    @Test fun staleNamesAreNotPromotedOrDeletedFromLibrary() {
        val known = listOf("Night", "Drive")
        assertEquals(listOf("Drive", "Night"), PlaylistPinPolicy.normalize(
            listOf("Missing", "Drive", "Night", "Drive"), known))
    }

    @Test fun renameAndDeleteMaintainPinMetadata() {
        assertEquals(listOf("Renamed", "Other"), PlaylistPinPolicy.renamed(
            listOf("Old", "Other"), "Old", "Renamed"))
        assertEquals(listOf("Other"), PlaylistPinPolicy.deleted(
            listOf("Old", "Other"), "Old"))
    }

    @Test fun capacityIsBounded() {
        val known = (0..50).map { "list$it" }
        assertNull(PlaylistPinPolicy.toggle(known.take(50), "list50", known))
        assertTrue(PlaylistPinPolicy.normalize(known, known).size <= 50)
    }
}
