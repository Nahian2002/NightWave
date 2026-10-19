package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineGaplessRepeatShuffleTest {
    private val ids = listOf("a.mp3", "b.mp3", "c.mp3")

    @Test fun naturalSequentialMovementIsStrictWithoutShuffle() {
        assertEquals(1, OfflineGaplessPolicy.transitionIndex(
            ids, 0, "offline:b.mp3", automatic = true))
        assertNull(OfflineGaplessPolicy.transitionIndex(
            ids, 0, "offline:c.mp3", automatic = true))
    }

    @Test fun randomMedia3OrderIsAllowedOnlyWhenShuffleEnabled() {
        assertEquals(2, OfflineGaplessPolicy.transitionIndex(
            ids, 0, "offline:c.mp3", automatic = true, shuffled = true))
        assertNull(OfflineGaplessPolicy.transitionIndex(
            ids, 0, "offline:a.mp3", automatic = true, shuffled = true))
    }

    @Test fun repeatAllMayWrapAtEndOnly() {
        assertEquals(0, OfflineGaplessPolicy.transitionIndex(
            ids, 2, "offline:a.mp3", automatic = true, repeatAll = true))
        assertNull(OfflineGaplessPolicy.transitionIndex(
            ids, 2, "offline:a.mp3", automatic = true, repeatAll = false))
        assertNull(OfflineGaplessPolicy.transitionIndex(
            ids, 1, "offline:a.mp3", automatic = true, repeatAll = true))
    }

    @Test fun unknownAndUnownedMediaIdsNeverAccepted() {
        assertNull(OfflineGaplessPolicy.transitionIndex(
            ids, 0, "https://example.com", automatic = true, shuffled = true))
        assertNull(OfflineGaplessPolicy.transitionIndex(
            ids, 0, "offline:wrong.mp3", automatic = true, shuffled = true))
    }
}
