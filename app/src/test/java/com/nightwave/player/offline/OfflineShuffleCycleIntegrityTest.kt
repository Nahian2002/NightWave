package com.nightwave.player.offline

import com.nightwave.player.playback.PlaybackQueue
import org.junit.Assert.*
import org.junit.Test

/** Tests both fallback shuffle history and strict prepared Media3 handoff IDs. */
class OfflineShuffleCycleIntegrityTest {
    private fun queue() = OfflinePlaybackQueue(pick = { 0 }).apply {
        select(listOf("a", "b", "c", "d", "e"), "a")
        setShuffle(true)
    }

    @Test fun previousThenJumpCannotRandomlyReplayAlreadyHeardSong() {
        val q = queue()
        assertEquals("b", q.next())
        assertEquals("a", q.previous())
        assertEquals("d", q.jump(3))
        assertEquals("c", q.next())
        assertEquals("e", q.next())
        assertNull(q.next())
    }

    @Test fun previousAndForwardPreserveTheActualListeningTrail() {
        val q = queue()
        assertEquals("b", q.next())
        assertEquals("c", q.next())
        assertEquals("b", q.previous())
        assertEquals("a", q.previous())
        assertEquals("b", q.next())
        assertEquals("c", q.next())
        assertEquals("d", q.next())
        assertEquals("e", q.next())
        assertNull(q.next())
    }

    @Test fun jumpingOntoAnotherUnheardSongCannotResetSeenHistory() {
        val q = queue()
        q.next()  // heard b
        q.next()  // heard c
        q.previous() // replay b
        assertEquals("e", q.jump(4))
        assertEquals("d", q.next())
        assertNull(q.next())
    }

    @Test fun repeatOneReplaysWithoutConsumingForwardHistory() {
        val q = queue()
        q.next()
        q.previous()
        q.setRepeat(PlaybackQueue.Repeat.ONE)
        assertEquals("a", q.next(automatic = true))
        assertEquals("b", q.next(automatic = false))
    }

    @Test fun repeatAllStillStartsNewCycleOnlyAfterExhaustion() {
        val q = queue()
        q.setRepeat(PlaybackQueue.Repeat.ALL)
        assertEquals("b", q.next())
        assertEquals("c", q.next())
        assertEquals("d", q.next())
        assertEquals("e", q.next())
        assertEquals("a", q.next())
    }

    @Test fun gaplessExplicitSeekRejectsUnselectedQueueAndUnknownMedia() {
        val ids = listOf("a", "b", "c")
        assertNull(OfflineGaplessPolicy.transitionIndex(ids, -1, "offline:b", automatic = false))
        assertNull(OfflineGaplessPolicy.transitionIndex(ids, 99, "offline:b", automatic = false))
        assertNull(OfflineGaplessPolicy.transitionIndex(ids, 1, "offline:missing", automatic = false))
    }

    @Test fun gaplessAutoHonorsShuffleAndEndRepeatRules() {
        val ids = listOf("a", "b", "c")
        assertNull(OfflineGaplessPolicy.transitionIndex(ids, 0, "offline:c", automatic = true))
        assertEquals(2, OfflineGaplessPolicy.transitionIndex(ids, 0, "offline:c",
            automatic = true, shuffled = true))
        assertEquals(0, OfflineGaplessPolicy.transitionIndex(ids, 2, "offline:a",
            automatic = true, repeatAll = true))
        assertNull(OfflineGaplessPolicy.transitionIndex(ids, 2, "offline:a", automatic = true))
    }

    @Test fun gaplessRejectsInvalidOrDuplicatePreparedQueueIds() {
        assertNull(OfflineGaplessPolicy.transitionIndex(listOf("a", "a", "b"), 0,
            "offline:b", automatic = false))
        assertNull(OfflineGaplessPolicy.transitionIndex(listOf("a", "../unsafe"), 0,
            "offline:../unsafe", automatic = false))
    }
}
