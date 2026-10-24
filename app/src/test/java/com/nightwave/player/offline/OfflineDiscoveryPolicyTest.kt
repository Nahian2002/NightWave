package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class OfflineDiscoveryPolicyTest {
    @Test fun songAndArtistSearchFallsBackToTitle() {
        assertEquals(listOf("Moonlight Sonata Beethoven", "Moonlight Sonata"),
            OfflineDiscoveryPolicy.queries("Moonlight Sonata (Official Audio)", "Beethoven - Topic"))
    }

    @Test fun preservedVersionAndNoFalseExactMatchGuarantee() {
        val searches = OfflineDiscoveryPolicy.queries(
            "Song Title (Live 1998) [Official Video]", "Artist")
        assertTrue(searches.first().contains("Live 1998"))
        assertFalse(searches.any { it.contains("Official", ignoreCase = true) })
        assertEquals(2, searches.size)
    }

    @Test fun emptyAndRepeatedArtistAreHandledSafely() {
        assertTrue(OfflineDiscoveryPolicy.queries(" ", "Artist").isEmpty())
        assertEquals(listOf("Artist Name Song"),
            OfflineDiscoveryPolicy.queries("Artist Name Song", "Artist Name"))
        assertTrue(OfflineDiscoveryPolicy.queries("N".repeat(500), "X".repeat(500))
            .all { it.length <= 80 })
    }
}
