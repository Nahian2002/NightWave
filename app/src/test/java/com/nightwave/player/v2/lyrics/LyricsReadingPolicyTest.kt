package com.nightwave.player.v2.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsReadingPolicyTest {
    private val policy = LyricsReadingPolicy

    @Test fun nw89OnlyNearbyLinesAnimateAndReducedMotionAlwaysJumps() {
        assertTrue(policy.shouldAnimateScroll(0, 12, false))
        assertTrue(policy.shouldAnimateScroll(50, 45, false))
        assertFalse(policy.shouldAnimateScroll(0, 13, false))
        assertFalse(policy.shouldAnimateScroll(1150, 8, false))
        assertFalse(policy.shouldAnimateScroll(8, 8, false))
        assertFalse(policy.shouldAnimateScroll(8, 9, true))
    }

    @Test fun nw89InvalidAndHugeIndexJumpsNeverOverflow() {
        assertFalse(policy.shouldAnimateScroll(-1, 20, false))
        assertFalse(policy.shouldAnimateScroll(1, -1, false))
        assertFalse(policy.shouldAnimateScroll(Int.MAX_VALUE, 1, false))
        assertFalse(policy.shouldAnimateScroll(1, Int.MAX_VALUE, false))
        assertEquals(12, policy.MAX_ANIMATED_SCROLL_ROWS)
    }

    @Test fun nw89ScreenReaderUsesRealLineAndOffsetAdjustedSeekTime() {
        val description = policy.cueDescription(
            LyricCue(62_000, "  আলো   জ্বলে  "), 500, 90_000L)
        assertEquals("Seek to 1 minute 2 seconds. আলো জ্বলে", description)
        assertEquals("Seek to 0 minutes 9 seconds. Last line",
            policy.cueDescription(LyricCue(25_000, "Last line"), 0, 9_000L))
        assertEquals("Seek to 0 minutes 0 seconds. First",
            policy.cueDescription(LyricCue(1_000, "First"), -10_000, 20_000L))
    }

    @Test fun nw89AccessibleCurrentStateAndFollowHintTellTheTruth() {
        assertEquals("Current synchronized lyric", policy.currentLineState(true))
        assertEquals("Synchronized lyric", policy.currentLineState(false))
        assertTrue(policy.followHint(false).contains("Select Follow"))
        assertTrue(policy.followHint(true).contains("Following imported lyrics"))
    }
}
