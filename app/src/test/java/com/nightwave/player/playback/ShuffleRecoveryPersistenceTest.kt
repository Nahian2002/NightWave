package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

/** NW93 restart fidelity uses public watch IDs, never signed stream links. */
class ShuffleRecoveryPersistenceTest {
    private fun song(n: Int) = QueueEntry(
        "https://www.youtube.com/watch?v=" + n.toString().padStart(11, '0'),
        "Track $n", "Creator", "")
    private fun q() = PlaybackQueue(pick = { 0 }).apply {
        replace((1..5).map(::song), 0)
        shuffle = true
    }
    private fun clone(from: PlaybackQueue): PlaybackQueue =
        PlaybackQueue(pick = { 0 }).also { target ->
            val history = from.shuffleRecoveryState()
            target.replace(from.snapshot(), from.index)
            target.shuffle = from.shuffle
            target.repeat = from.repeat
            target.restoreShuffleRecovery(history)
            target.restoreNextPriorities(from.priorityNextUrls())
        }

    @Test fun previousAfterRestartRetracesActualListeningHistory() {
        val before = q()
        assertEquals(song(2), before.next())
        assertEquals(song(3), before.next())
        val restored = clone(before)
        assertEquals(song(3), restored.current)
        assertEquals(song(2), restored.previous())
        assertEquals(song(1), restored.previous())
    }

    @Test fun forwardAfterRestartRetracesPreviousInsteadOfRandomizing() {
        val before = q()
        before.next()
        before.next()
        assertEquals(song(2), before.previous())
        val restored = clone(before)
        assertEquals(song(2), restored.current)
        assertEquals(song(3), restored.next())
        assertEquals(song(2), restored.previous())
    }

    @Test fun seenCycleSurvivesRestartWithoutRepeatingOldSong() {
        val before = q()
        before.next()
        val restored = clone(before)
        assertEquals(song(3), restored.next())
    }

    @Test fun tamperedHistoryCannotChangeCurrentQueueSelection() {
        val before = q()
        before.next()
        val restored = clone(before)
        restored.restoreShuffleRecovery(ShuffleRecoverySnapshot(
            visited = listOf("https://signed.example/audio?token=secret")))
        assertEquals(song(2), restored.current)
        assertNull(restored.previous())
        assertEquals(5, restored.size)
        // Tampered input is discarded, including the previously imported seen-cycle.
        // After that reset, pick { 0 } may select Track 1 again. It cannot safely
        // infer Track 1 was previously heard from the rejected recovery state.
        assertEquals(ShuffleRecoverySnapshot(), restored.shuffleRecoveryState())
        assertEquals(song(1), restored.next())
    }

    @Test fun legacySnapshotWithoutHistoryRemainsPlayable() {
        val restored = q()
        restored.restoreShuffleRecovery(null)
        assertEquals(song(1), restored.current)
        assertNull(restored.previous())
        assertEquals(song(2), restored.next())
    }

    @Test fun turningShuffleOffErasesHistoryAndManualPriorityStillWorks() {
        val original = q()
        original.next()
        original.insertNext(song(5))
        val restored = clone(original)
        restored.shuffle = false
        assertEquals(0, restored.shuffleRecoveryState().visited.size)
        assertEquals(song(5), restored.next())
    }
}
