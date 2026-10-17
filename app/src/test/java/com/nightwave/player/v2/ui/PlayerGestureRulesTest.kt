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
}
