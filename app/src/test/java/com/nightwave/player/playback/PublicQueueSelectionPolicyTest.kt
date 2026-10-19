package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PublicQueueSelectionPolicyTest {
    private fun entry(n: Int) = QueueEntry("https://www.youtube.com/watch?v=track$n", "Track $n", "Creator", "")

    @Test fun originalIndexResolvesAfterEarlierDuplicateRowsRemoved() {
        val original = listOf(entry(1), entry(1), entry(2), entry(3))
        val unique = original.distinctBy { it.url }
        assertEquals(1, PublicQueueSelectionPolicy.selectedIndex(original, unique, 2))
    }

    @Test fun duplicateSelectedTrackSelectsFirstRetainedOccurrence() {
        val original = listOf(entry(1), entry(2), entry(1))
        assertEquals(0, PublicQueueSelectionPolicy.selectedIndex(
            original, original.distinctBy { it.url }, 2))
    }

    @Test fun missingAndOutOfRangeIndicesUseSameSafeFallbackAsQueue() {
        val original = listOf(entry(1), entry(2), entry(3))
        assertEquals(0, PublicQueueSelectionPolicy.selectedIndex(original, original, -6))
        assertEquals(2, PublicQueueSelectionPolicy.selectedIndex(original, original, 99))
        assertEquals(-1, PublicQueueSelectionPolicy.selectedIndex(emptyList(), emptyList(), 0))
    }

    @Test fun alreadyDeduplicatedQueueKeepsRequestedIndex() {
        val original = listOf(entry(1), entry(2), entry(3))
        assertEquals(1, PublicQueueSelectionPolicy.selectedIndex(original, original, 1))
    }
}
