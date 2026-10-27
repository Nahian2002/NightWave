package com.nightwave.player.v2.lyrics

import org.junit.Assert.*
import org.junit.Test

class LyricsSyncPolicyTest {
    @Test fun nw119ExtremeCueSeekSaturatesInsteadOfWrappingToStart() {
        val cue = LyricCue(Long.MAX_VALUE, "last")
        assertEquals(Long.MAX_VALUE, LyricsSyncPolicy.seekPosition(cue, 10000, 0))
        assertEquals(5000L, LyricsSyncPolicy.seekPosition(cue, 10000, 5000))
        assertEquals(Long.MAX_VALUE - 50, LyricsSyncPolicy.seekPosition(cue, -50, 0))
    }

    @Test fun nw119CoarseAdjustmentRejectsCorruptSavedOffsetLikeFineAdjustment() {
        for (offset in listOf(51, -51, Int.MAX_VALUE, Int.MIN_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) {
                LyricsSyncPolicy.adjust(offset, 1)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            LyricsSyncPolicy.seekPosition(LyricCue(-1L, "bad"), 0, 0)
        }
    }

    private val lrc = TimedLyricsCodec.parse(
        "[00:00.000]intro\n[00:01.000]line one\n[00:02.500]line two"
            .toByteArray(Charsets.UTF_8)
    )

    @Test fun zeroOffsetMaintainsExistingLrcBehavior() {
        assertEquals(0, LyricsSyncPolicy.activeIndex(lrc, -3L, 0))
        assertEquals(0, LyricsSyncPolicy.activeIndex(lrc, 250L, 0))
        assertEquals(1, LyricsSyncPolicy.activeIndex(lrc, 1_000L, 0))
        assertEquals(2, LyricsSyncPolicy.activeIndex(lrc, 2_500L, 0))
    }

    @Test fun positiveOffsetDelaysLineHighlight() {
        assertEquals(-1, LyricsSyncPolicy.activeIndex(lrc, 250L, 500))
        assertEquals(0, LyricsSyncPolicy.activeIndex(lrc, 999L, 500))
        assertEquals(1, LyricsSyncPolicy.activeIndex(lrc, 1_500L, 500))
    }

    @Test fun negativeOffsetAdvancesHighlight() {
        assertEquals(1, LyricsSyncPolicy.activeIndex(lrc, 500L, -500))
        assertEquals(2, LyricsSyncPolicy.activeIndex(lrc, 2_000L, -500))
    }

    @Test fun tappingCueSeeksWithOffsetAndClampsToDuration() {
        assertEquals(1_500L, LyricsSyncPolicy.seekPosition(lrc.cues[1], 500, 10_000L))
        assertEquals(500L, LyricsSyncPolicy.seekPosition(lrc.cues[1], -500, 10_000L))
        assertEquals(1_100L, LyricsSyncPolicy.seekPosition(lrc.cues[2], 500, 1_100L))
        assertEquals(3_000L, LyricsSyncPolicy.seekPosition(lrc.cues[2], 500, 0L))
    }

    @Test fun negativeSeekNeverGoesBeforeZero() {
        assertEquals(0L, LyricsSyncPolicy.seekPosition(lrc.cues[0], -10_000, 4_000))
    }

    @Test fun offsetsAreQuarterSecondStepsWithinTenSeconds() {
        assertTrue(LyricsSyncPolicy.validOffset(0))
        assertTrue(LyricsSyncPolicy.validOffset(10_000))
        assertTrue(LyricsSyncPolicy.validOffset(-10_000))
        assertFalse(LyricsSyncPolicy.validOffset(251))
        assertFalse(LyricsSyncPolicy.validOffset(10_250))
    }

    @Test fun repeatedAdjustmentNeverExceedsSafetyBounds() {
        assertEquals(10_000, LyricsSyncPolicy.adjust(9_750, 100))
        assertEquals(-10_000, LyricsSyncPolicy.adjust(-9_750, -100))
        assertEquals(0, LyricsSyncPolicy.adjust(500, -2))
    }

    @Test fun hugeStepArgumentDoesNotOverflow() {
        assertEquals(10_000, LyricsSyncPolicy.adjust(0, Int.MAX_VALUE))
        assertEquals(-10_000, LyricsSyncPolicy.adjust(0, Int.MIN_VALUE))
    }

    @Test fun offsetLabelsExplainDirection() {
        assertEquals("0.00s", LyricsSyncPolicy.label(0))
        assertEquals("+0.25s", LyricsSyncPolicy.label(250))
        assertEquals("-1.50s", LyricsSyncPolicy.label(-1_500))
    }

    @Test fun extremePlaybackClockDoesNotWrapBehindFirstCue() {
        assertEquals(lrc.cues.lastIndex,
            LyricsSyncPolicy.activeIndex(lrc, Long.MAX_VALUE, -10_000))
    }

    @Test fun invalidOffsetsAreRejectedBeforeUse() {
        assertThrows(IllegalArgumentException::class.java) {
            LyricsSyncPolicy.activeIndex(lrc, 10_000, 333)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LyricsSyncPolicy.seekPosition(lrc.cues[0], -11_000, 10_000)
        }
    }

    @Test fun fineOffsetsSupportFiftyMillisecondsWithoutChangingQuarterSecondControls() {
        assertEquals(50, LyricsSyncPolicy.fineAdjust(0, 1))
        assertEquals(-50, LyricsSyncPolicy.fineAdjust(0, -1))
        assertEquals(300, LyricsSyncPolicy.adjust(50, 1))
        assertTrue(LyricsSyncPolicy.validOffset(50))
        assertTrue(LyricsSyncPolicy.validOffset(-150))
        assertFalse(LyricsSyncPolicy.validOffset(51))
        assertEquals("+0.05s", LyricsSyncPolicy.label(50))
    }

    @Test fun fineOffsetsAffectHighlightAndTapSeekConsistently() {
        assertEquals(0, LyricsSyncPolicy.activeIndex(lrc, 1049L, 50))
        assertEquals(1, LyricsSyncPolicy.activeIndex(lrc, 1050L, 50))
        assertEquals(1050L, LyricsSyncPolicy.seekPosition(lrc.cues[1], 50, 6000L))
    }

    @Test fun fineOffsetsSaturateAtBoundsAndFailOnInvalidBase() {
        assertEquals(10000, LyricsSyncPolicy.fineAdjust(9950, Int.MAX_VALUE))
        assertEquals(-10000, LyricsSyncPolicy.fineAdjust(-9950, Int.MIN_VALUE))
        assertThrows(IllegalArgumentException::class.java) {
            LyricsSyncPolicy.fineAdjust(55, 1)
        }
    }

}
