package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** NW115: avoid unnecessary extraction without accepting stale stream identity. */
class PlaybackQueueJumpPolicyTest {
    private val first = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val second = "https://www.youtube.com/watch?v=9bZkp7q19f0"
    private val policy = PlaybackQueueJumpPolicy

    @Test fun exactLoadedPublicSongCanRestartImmediately() {
        assertTrue(policy.canRestartLoaded(first, first, first, false, false))
    }

    @Test fun jumpingToAnotherQueueSongStillResolvesItsOwnAudio() {
        assertFalse(policy.canRestartLoaded(first, second, first, false, false))
        assertFalse(policy.canRestartLoaded(first, second, second, false, false))
    }

    @Test fun staleLoadedMediaCannotSubstituteTheRequestedSong() {
        assertFalse(policy.canRestartLoaded(first, first, second, false, false))
        assertFalse(policy.canRestartLoaded(first, first, null, false, false))
        assertFalse(policy.canRestartLoaded(second, first, first, false, false))
    }

    @Test fun idleOrErroredPlayerCannotUseTheFastPath() {
        assertFalse(policy.canRestartLoaded(first, first, first, true, false))
        assertFalse(policy.canRestartLoaded(first, first, first, false, true))
        assertFalse(policy.canRestartLoaded(first, first, first, true, true))
    }

    @Test fun missingSignedOrNoncanonicalIdsAlwaysRequireSafeResolution() {
        assertFalse(policy.canRestartLoaded(null, first, first, false, false))
        assertFalse(policy.canRestartLoaded("", first, first, false, false))
        assertFalse(policy.canRestartLoaded(first, null, first, false, false))
        assertFalse(policy.canRestartLoaded(first, "", "", false, false))
        val signed = "https://rr.example.googlevideo.com/audio?sig=private"
        assertFalse(policy.canRestartLoaded(signed, signed, signed, false, false))
        assertFalse(policy.canRestartLoaded(first + "&token=private",
            first + "&token=private", first + "&token=private", false, false))
    }

    @Test fun sameQueueRowReplayDoesNotEraseShuffleHistoryOrManualNext() {
        val entries = listOf(first, second,
            "https://www.youtube.com/watch?v=J---aiyznGQ").map { id ->
            QueueEntry(id, "Song", "Artist", "")
        }
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(entries, 0)
        queue.shuffle = true
        assertEquals(entries[1], queue.next())
        assertTrue(queue.insertNext(entries[2]))
        val before = queue.snapshot()
        val history = queue.shuffleRecoveryState()
        val priorities = queue.priorityNextUrls()
        val selected = queue.current?.url
        assertEquals(entries[1], queue.jump(queue.index))
        assertTrue(policy.canRestartLoaded(selected, queue.current?.url,
            selected, false, false))
        assertEquals(before, queue.snapshot())
        assertEquals(history, queue.shuffleRecoveryState())
        assertEquals(priorities, queue.priorityNextUrls())
        assertEquals(entries[2], queue.next())
    }

    @Test fun newQueueJumpStillChangesSelectionAndDisablesLoadedReuse() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(listOf(
            QueueEntry(first, "First", "Artist", ""),
            QueueEntry(second, "Second", "Artist", "")
        ), 0)
        val previouslySelected = queue.current?.url
        assertEquals(second, queue.jump(1)?.url)
        assertFalse(policy.canRestartLoaded(previouslySelected,
            queue.current?.url, previouslySelected, false, false))
        assertEquals(second, queue.current?.url)
        assertEquals(1, queue.index)
    }
}
