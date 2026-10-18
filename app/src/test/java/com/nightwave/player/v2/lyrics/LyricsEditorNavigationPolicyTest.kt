package com.nightwave.player.v2.lyrics

import org.junit.Assert.*
import org.junit.Test

class LyricsEditorNavigationPolicyTest {
    private val initial = LyricsEditorPolicy.begin(TimedLyricsCodec.parse(
        "[00:01.000]First\n[00:02.000]Second\n[00:03.000]Third".toByteArray()
    ))

    @Test fun previousAndNextStayInRange() {
        assertEquals(0, LyricsEditorNavigationPolicy.step(0, 3, -1))
        assertEquals(2, LyricsEditorNavigationPolicy.step(2, 3, 1))
        assertEquals(1, LyricsEditorNavigationPolicy.step(0, 3, 1))
    }

    @Test fun hugeNavigationStepsDoNotOverflow() {
        assertEquals(2, LyricsEditorNavigationPolicy.step(0, 3, Int.MAX_VALUE))
        assertEquals(0, LyricsEditorNavigationPolicy.step(2, 3, Int.MIN_VALUE))
    }

    @Test fun deletionAndUndoClampsOldSelection() {
        assertEquals(1, LyricsEditorNavigationPolicy.clamp(7, 2))
        assertEquals(0, LyricsEditorNavigationPolicy.clamp(-7, 2))
    }

    @Test fun noRowsAreNeverPresentedAsSelectable() {
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorNavigationPolicy.clamp(0, 0)
        }
    }

    @Test fun sameSongPlayingCueUsesTimingOffset() {
        assertEquals(1, LyricsEditorNavigationPolicy.follow(initial, 2200L, 0))
        assertEquals(0, LyricsEditorNavigationPolicy.follow(initial, 2200L, 250))
    }

    @Test fun silenceBeforeFirstCuePreservesPriorSelection() {
        assertEquals(2, LyricsEditorNavigationPolicy.followOrKeep(initial, 0, 0, 2))
    }

    @Test fun seekToPlayingLineChoosesAvailableCue() {
        assertEquals(2, LyricsEditorNavigationPolicy.followOrKeep(initial, 3500, 0, 0))
    }

    @Test fun reorderKeepsMovedCueSelected() {
        val moved = LyricsEditorPolicy.updateTime(initial, 2, "00:00.500")
        assertEquals(0, LyricsEditorNavigationPolicy.afterEdit(
            moved, LyricCue(500L, "Third"), 2))
    }

    @Test fun unknownTargetFallsBackToBoundedPriorSelection() {
        assertEquals(2, LyricsEditorNavigationPolicy.afterEdit(
            initial, LyricCue(9000L, "Missing"), 99))
    }

    @Test fun singleCueNavigationRemainsFixed() {
        assertEquals(0, LyricsEditorNavigationPolicy.step(0, 1, Int.MAX_VALUE))
    }
}
