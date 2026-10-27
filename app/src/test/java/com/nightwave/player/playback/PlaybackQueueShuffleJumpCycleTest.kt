package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NW115: manual jumps do not mean the previous song was never heard.
 * Preserve the explicit user's selection, true Previous/Forward history,
 * the original URL queue and the current shuffle/repeat setting.
 */
class PlaybackQueueShuffleJumpCycleTest {
    private fun track(i: Int) = QueueEntry(
        "https://www.youtube.com/watch?v=" + i.toString().padStart(11, '0'),
        "Song $i", "Artist", ""
    )

    private fun queue(n: Int = 3): PlaybackQueue =
        PlaybackQueue(pick = { 0 }).apply {
            replace((1..n).map(::track), 0)
            shuffle = true
        }

    @Test fun manualShuffleJumpMarksDepartedSongSeenBeforeNextPick() {
        val q = queue()
        assertEquals(track(2), q.jump(1))
        assertEquals(listOf(track(1).url), q.shuffleRecoveryState().seen)
        assertEquals(track(3), q.next())
        assertFalse(q.hasNext())
        assertNull(q.next())
        assertEquals(track(3), q.current)
    }

    @Test fun restartingQueueDoesNotImmediatelyReplayTheJumpedFromSong() {
        val source = queue(4)
        assertEquals(track(3), source.jump(2))
        val savedHistory = source.shuffleRecoveryState()
        val restored = PlaybackQueue(pick = { 0 })
        restored.replace(source.snapshot(), source.index)
        restored.shuffle = true
        restored.restoreShuffleRecovery(savedHistory)
        assertEquals(track(3), restored.current)
        assertEquals(track(2), restored.next())
        assertEquals(track(3), restored.previous())
        assertEquals(track(1), restored.previous())
    }

    @Test fun invalidAndSameSongJumpsCannotInventShuffleListeningHistory() {
        val q = queue()
        assertNull(q.jump(-1))
        assertNull(q.jump(100))
        assertEquals(track(1), q.jump(0))
        assertEquals(ShuffleRecoverySnapshot(), q.shuffleRecoveryState())
        assertNull(q.previous())
        assertEquals(track(2), q.next())
    }

    @Test fun deliberateReturnToPreviouslyHeardSongStillWorks() {
        val q = queue()
        assertEquals(track(2), q.jump(1))
        assertEquals(track(1), q.jump(0))
        assertEquals(track(2), q.previous())
        assertEquals(track(1), q.next()) // exact forward retrace
        assertEquals(3, q.size)
    }

    @Test fun explicitModeToggleStartsANewShuffleCycleWithoutOldSeenIds() {
        val q = queue()
        assertEquals(track(2), q.jump(1))
        q.shuffle = false
        q.shuffle = true
        assertEquals(ShuffleRecoverySnapshot(), q.shuffleRecoveryState())
        assertEquals(track(1), q.next())
    }

    @Test fun playNextStillOutranksShuffleEvenAfterAJump() {
        val q = queue()
        assertEquals(track(2), q.jump(1))
        assertTrue(q.insertNext(track(9)))
        assertEquals(track(2), q.current)
        assertEquals(track(9), q.next())
        assertEquals(track(3), q.next())
        assertTrue(q.priorityNextUrls().isEmpty())
    }

    @Test fun twoTrackShuffleJumpMustNotAdvertiseAPhantomNext() {
        val q = queue(2)
        assertEquals(track(2), q.jump(1))
        assertFalse(q.hasNext())
        assertNull(q.next())
        q.repeat = PlaybackQueue.Repeat.ALL
        assertTrue(q.hasNext())
        assertEquals(track(1), q.next())
    }
}
