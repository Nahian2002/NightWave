package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure policy regression coverage; no phone, Media3 instance or network needed. */
class OfflineGaplessPolicyTest {
    @Test fun albumWithinBoundHasStableSelectedSlot() {
        val ids = listOf("first.mp3", "second.mp3", "third.mp3")
        assertEquals(1, OfflineGaplessPolicy.selectedIndex(ids, "second.mp3"))
        assertEquals(0, OfflineGaplessPolicy.selectedIndex(ids, "first.mp3"))
        assertNull(OfflineGaplessPolicy.selectedIndex(ids, "missing.mp3"))
    }

    @Test fun neverPreloadsSingleOrOversizedQueues() {
        assertNull(OfflineGaplessPolicy.selectedIndex(listOf("one.mp3"), "one.mp3"))
        val limit = (0 until OfflineGaplessPolicy.MAX_PREPARED_ITEMS).map { "$it.mp3" }
        assertEquals(0, OfflineGaplessPolicy.selectedIndex(limit, "0.mp3"))
        val over = limit + "extra.mp3"
        assertNull(OfflineGaplessPolicy.selectedIndex(over, "0.mp3"))
    }

    @Test fun repeatedOrUnsafeIdsAreNotPrepared() {
        assertNull(OfflineGaplessPolicy.selectedIndex(
            listOf("same.mp3", "same.mp3"), "same.mp3"))
        assertNull(OfflineGaplessPolicy.selectedIndex(
            listOf("safe.mp3", "../unsafe.mp3"), "safe.mp3"))
        assertNull(OfflineGaplessPolicy.selectedIndex(
            listOf("safe.mp3", "offline:other.mp3"), "safe.mp3"))
        assertNull(OfflineGaplessPolicy.selectedIndex(
            listOf("safe.mp3", ""), "safe.mp3"))
    }

    @Test fun automaticTransitionMayOnlyAdvanceOneValidSlot() {
        val ids = listOf("a.mp3", "b.mp3", "c.mp3")
        assertEquals(1, OfflineGaplessPolicy.transitionIndex(
            ids, 0, "offline:b.mp3", automatic = true))
        assertNull(OfflineGaplessPolicy.transitionIndex(
            ids, 0, "offline:c.mp3", automatic = true))
        assertNull(OfflineGaplessPolicy.transitionIndex(
            ids, 2, "offline:a.mp3", automatic = true))
    }

    @Test fun explicitMediaSeekCanJumpBackwardOrForward() {
        val ids = listOf("a.mp3", "b.mp3", "c.mp3")
        assertEquals(2, OfflineGaplessPolicy.transitionIndex(
            ids, 0, "offline:c.mp3", automatic = false))
        assertEquals(0, OfflineGaplessPolicy.transitionIndex(
            ids, 2, "offline:a.mp3", automatic = false))
    }

    @Test fun publicOrMissingMediaNeverSelectsOfflineSlot() {
        val ids = listOf("a.mp3", "b.mp3")
        assertNull(OfflineGaplessPolicy.transitionIndex(
            ids, 0, null, automatic = true))
        assertNull(OfflineGaplessPolicy.transitionIndex(
            ids, 0, "https://example.test/b.mp3", automatic = true))
        assertNull(OfflineGaplessPolicy.transitionIndex(
            ids, 0, "offline:deleted.mp3", automatic = false))
    }
}
