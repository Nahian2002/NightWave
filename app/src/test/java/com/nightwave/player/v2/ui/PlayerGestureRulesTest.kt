package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class PlayerGestureRulesTest {
    @Test fun leftSwipeAdvances() {
        assertEquals(PlayerArtworkSwipe.NEXT,
            PlayerGestureRules.decideHorizontalSwipe(-110f, 88f, true, false, 4))
    }

    @Test fun rightSwipeGoesBack() {
        assertEquals(PlayerArtworkSwipe.PREVIOUS,
            PlayerGestureRules.decideHorizontalSwipe(110f, 88f, true, false, 4))
    }

    @Test fun smallAndThresholdEdgeSwipesAreDeliberate() {
        assertEquals(PlayerArtworkSwipe.NONE,
            PlayerGestureRules.decideHorizontalSwipe(-87.9f, 88f, true, false, 4))
        assertEquals(PlayerArtworkSwipe.NEXT,
            PlayerGestureRules.decideHorizontalSwipe(-88f, 88f, true, false, 4))
    }

    @Test fun disconnectedOrBufferingPlayerIgnoresSwipes() {
        assertEquals(PlayerArtworkSwipe.NONE,
            PlayerGestureRules.decideHorizontalSwipe(-200f, 88f, false, false, 4))
        assertEquals(PlayerArtworkSwipe.NONE,
            PlayerGestureRules.decideHorizontalSwipe(200f, 88f, true, true, 4))
    }

    @Test fun singleTrackQueueNeverSkips() {
        assertEquals(PlayerArtworkSwipe.NONE,
            PlayerGestureRules.decideHorizontalSwipe(-200f, 88f, true, false, 1))
        assertEquals(PlayerArtworkSwipe.NONE,
            PlayerGestureRules.decideHorizontalSwipe(200f, 88f, true, false, 0))
    }

    @Test fun rejectsInvalidDistancesAndThresholds() {
        assertEquals(PlayerArtworkSwipe.NONE,
            PlayerGestureRules.decideHorizontalSwipe(Float.NaN, 88f, true, false, 3))
        assertEquals(PlayerArtworkSwipe.NONE,
            PlayerGestureRules.decideHorizontalSwipe(Float.POSITIVE_INFINITY, 88f, true, false, 3))
        assertEquals(PlayerArtworkSwipe.NONE,
            PlayerGestureRules.decideHorizontalSwipe(-200f, 0f, true, false, 3))
    }

    @Test fun verticalGesturesUseDensityScaledThresholdAndBothDirections() {
        for (density in listOf(1f, 2f, 3.5f)) {
            val threshold = PlayerGestureRules.MIN_VERTICAL_SWIPE_DP * density
            assertEquals(PlayerVerticalSwipe.NONE,
                PlayerGestureRules.decideVerticalSwipe(threshold - 1f, threshold))
            assertEquals(PlayerVerticalSwipe.NONE,
                PlayerGestureRules.decideVerticalSwipe(-threshold + 1f, threshold))
            assertEquals(PlayerVerticalSwipe.MINIMIZE,
                PlayerGestureRules.decideVerticalSwipe(threshold, threshold))
            assertEquals(PlayerVerticalSwipe.EXPAND,
                PlayerGestureRules.decideVerticalSwipe(-threshold, threshold))
        }
    }

    @Test fun verticalSwipesRejectInvalidNumbersAndZeroThreshold() {
        assertEquals(PlayerVerticalSwipe.NONE,
            PlayerGestureRules.decideVerticalSwipe(Float.NaN, 72f))
        assertEquals(PlayerVerticalSwipe.NONE,
            PlayerGestureRules.decideVerticalSwipe(Float.NEGATIVE_INFINITY, 72f))
        assertEquals(PlayerVerticalSwipe.NONE,
            PlayerGestureRules.decideVerticalSwipe(100f, Float.POSITIVE_INFINITY))
        assertEquals(PlayerVerticalSwipe.NONE,
            PlayerGestureRules.decideVerticalSwipe(100f, 0f))
    }

    @Test fun verticalSwipeIsIndependentOfHorizontalPlaybackEligibility() {
        assertEquals(PlayerVerticalSwipe.MINIMIZE,
            PlayerGestureRules.decideVerticalSwipe(108f, 72f))
        assertEquals(PlayerArtworkSwipe.NONE,
            PlayerGestureRules.decideHorizontalSwipe(108f, 88f, false, false, 0))
    }
}
