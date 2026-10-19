package com.nightwave.player.v2.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LyricsPresentationPolicyTest {
    private val lines = TimedLyrics(listOf(
        LyricCue(1_000L, "First"), LyricCue(3_000L, "Second"),
        LyricCue(5_000L, "Third")
    ))

    @Test fun scrollAnchorKeepsTwoContextRowsWithoutGoingNegative() {
        assertEquals(0, LyricsPresentationPolicy.scrollTarget(0))
        assertEquals(0, LyricsPresentationPolicy.scrollTarget(1))
        assertEquals(0, LyricsPresentationPolicy.scrollTarget(2))
        assertEquals(5, LyricsPresentationPolicy.scrollTarget(7))
        assertEquals(0, LyricsPresentationPolicy.scrollTarget(-1))
    }

    @Test fun progressIsLineLevelNotFabricatedWordTimestamps() {
        assertEquals(0f, LyricsPresentationPolicy.lineProgress(lines, 0, 1000L, 0), 0f)
        assertEquals(.5f, LyricsPresentationPolicy.lineProgress(lines, 0, 2000L, 0), .00001f)
        assertEquals(.9995f, LyricsPresentationPolicy.lineProgress(lines, 0, 2999L, 0), .00001f)
    }

    @Test fun delayedAndAdvancedOffsetsShiftVisibleLineProgress() {
        assertEquals(.25f, LyricsPresentationPolicy.lineProgress(lines, 0, 2000L, 500), .00001f)
        assertEquals(.75f, LyricsPresentationPolicy.lineProgress(lines, 0, 2000L, -500), .00001f)
    }

    @Test fun beforeFirstCueAndInvalidIndicesHaveZeroProgress() {
        assertEquals(0f, LyricsPresentationPolicy.lineProgress(lines, -1, 0L, 0), 0f)
        assertEquals(0f, LyricsPresentationPolicy.lineProgress(lines, 3, 9900L, 0), 0f)
        assertEquals(0f, LyricsPresentationPolicy.lineProgress(lines, 0, 250L, 0), 0f)
    }

    @Test fun finalCueOnlyCompletesOnceItBegins() {
        assertEquals(0f, LyricsPresentationPolicy.lineProgress(lines, 2, 4999L, 0), 0f)
        assertEquals(0f, LyricsPresentationPolicy.lineProgress(lines, 2, 5000L, 0), 0f)
        assertEquals(1f, LyricsPresentationPolicy.lineProgress(lines, 2, 5001L, 0), 0f)
    }

    @Test fun duplicateTimestampsFindTheNextDistinctTimestamp() {
        val lyrics = TimedLyrics(listOf(
            LyricCue(1000L, "one"), LyricCue(1000L, "another"),
            LyricCue(5000L, "two")
        ))
        assertEquals(.25f,
            LyricsPresentationPolicy.lineProgress(lyrics, 0, 2000L, 0), .00001f)
        assertEquals(.25f,
            LyricsPresentationPolicy.lineProgress(lyrics, 1, 2000L, 0), .00001f)
    }

    @Test fun extremeClocksCannotOverflowOrCreateInvalidProgress() {
        assertEquals(1f,
            LyricsPresentationPolicy.lineProgress(lines, 1, Long.MAX_VALUE, 0), 0f)
        assertEquals(0f,
            LyricsPresentationPolicy.lineProgress(lines, 1, Long.MIN_VALUE, 0), 0f)
    }

    @Test fun invalidOffsetIsRejectedRatherThanSilentlyFudged() {
        assertThrows(IllegalArgumentException::class.java) {
            LyricsPresentationPolicy.lineProgress(lines, 0, 1000L, 55)
        }
    }
}
