package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDecodeRecoveryPolicyTest {
    private val p = OfflineDecodeRecoveryPolicy
    private val queue = listOf("a.mp3", "b.mp3", "c.mp3")

    @Test fun onlyTheActualSelectedPrivateTrackCanTriggerRecovery() {
        assertTrue(p.belongsToSelection("offline:a.mp3", "a.mp3", queue, emptySet()))
        assertFalse(p.belongsToSelection("https://example.com/audio", "a.mp3", queue, emptySet()))
        assertFalse(p.belongsToSelection("offline:b.mp3", "a.mp3", queue, emptySet()))
        assertFalse(p.belongsToSelection("offline:missing", "missing", queue, emptySet()))
        assertFalse(p.belongsToSelection("offline:a.mp3", "a.mp3", queue, setOf("a.mp3")))
        assertFalse(p.belongsToSelection(null, "a.mp3", queue, emptySet()))
    }

    @Test fun linearRecoverySkipsBrokenTracksAndNeverRevisitsFailedIds() {
        assertEquals(1, p.nextLinearIndex(queue, 0, setOf("a.mp3"), false) ?: -1)
        assertEquals(2, p.nextLinearIndex(queue, 0, setOf("a.mp3", "b.mp3"), false) ?: -1)
        assertNull(p.nextLinearIndex(queue, 1, setOf("a.mp3", "b.mp3", "c.mp3"), false))
        assertNull(p.nextLinearIndex(queue, 0, setOf("a.mp3", "b.mp3", "c.mp3"), true))
    }

    @Test fun repeatAllCanWrapButNeverReplayBrokenSource() {
        assertEquals(0, p.nextLinearIndex(queue, 2, setOf("c.mp3"), true) ?: -1)
        assertEquals(1, p.nextLinearIndex(queue, 2, setOf("a.mp3", "c.mp3"), true) ?: -1)
        assertNull(p.nextLinearIndex(queue, 2, setOf("c.mp3"), false))
        assertNull(p.nextLinearIndex(listOf("only.mp3"), 0, setOf("only.mp3"), true))
    }

    @Test fun shuffleOnlyUsesMedia3OwnedNextSlot() {
        assertEquals(2, p.safeShuffledIndex(queue, 0, 2, setOf("a.mp3")) ?: -1)
        assertNull(p.safeShuffledIndex(queue, 0, 0, setOf("a.mp3")))
        assertNull(p.safeShuffledIndex(queue, 0, -1, setOf("a.mp3")))
        assertNull(p.safeShuffledIndex(queue, 0, 2, setOf("a.mp3", "c.mp3")))
    }

    @Test fun maliciousOrOverlargeQueueFailsClosed() {
        val many = List(p.MAX_QUEUE_SIZE + 1) { "id$it" }
        assertFalse(p.belongsToSelection("offline:id0", "id0", many, emptySet()))
        assertNull(p.nextLinearIndex(many, 0, emptySet(), true))
        assertNull(p.safeShuffledIndex(many, 0, 1, emptySet()))
        assertNull(p.nextLinearIndex(queue, -1, emptySet(), true))
        assertNull(p.nextLinearIndex(queue, 3, emptySet(), true))
    }
}
