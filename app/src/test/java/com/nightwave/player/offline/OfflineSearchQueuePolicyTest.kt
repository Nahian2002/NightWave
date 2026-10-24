package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSearchQueuePolicyTest {
    @Test fun selectedOfflineTrackPlaysBeforeOtherSavedTracks() {
        assertEquals(listOf("song-b", "song-c", "song-a"),
            OfflineSearchQueuePolicy.plan("song-b",
                listOf("song-a", null, "song-b", "song-c", "song-a")))
    }

    @Test fun unsavedResultsNeverEnterOfflinePlayback() {
        assertEquals(listOf("saved-1", "saved-2"),
            OfflineSearchQueuePolicy.plan("saved-1",
                listOf(null, "saved-1", "", null, "saved-2")))
    }

    @Test fun currentTrackIsIncludedEvenIfNotInOriginalCollection() {
        assertEquals(listOf("selected", "before", "after"),
            OfflineSearchQueuePolicy.plan("selected", listOf("before", "after")))
    }

    @Test fun duplicateTracksDoNotBreakNavigation() {
        assertEquals(listOf("alpha", "beta"),
            OfflineSearchQueuePolicy.plan("alpha",
                listOf("alpha", "alpha", "beta", "beta", "alpha")))
    }

    @Test fun queueIsBoundedAndRejectsBlankSelection() {
        assertTrue(OfflineSearchQueuePolicy.plan("", listOf("a")).isEmpty())
        val large = (0..800).map { "song-$it" }
        val result = OfflineSearchQueuePolicy.plan("song-700", large)
        assertEquals("song-700", result.first())
        assertEquals(OfflineSearchQueuePolicy.MAX_IDS, result.size)
        assertTrue(result.distinct().size == result.size)
    }
}
