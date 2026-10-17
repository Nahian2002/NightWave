package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class PlayerProgressRulesTest {
    @Test fun unknownDurationMeansNoSeek() {
        assertEquals(0f, PlayerProgressRules.fraction(500L, 0L), 0f)
        assertNull(PlayerProgressRules.requestedPosition(0L, .5f))
        assertNull(PlayerProgressRules.requestedPosition(-1L, .5f))
    }

    @Test fun positionClampsToDuration() {
        assertEquals(0f, PlayerProgressRules.fraction(-5, 10), 0f)
        assertEquals(1f, PlayerProgressRules.fraction(999, 10), 0f)
        assertEquals(.5f, PlayerProgressRules.fraction(50, 100), .0001f)
    }

    @Test fun invalidFractionCannotSeek() {
        assertNull(PlayerProgressRules.requestedPosition(100, Float.NaN))
        assertNull(PlayerProgressRules.requestedPosition(100, Float.POSITIVE_INFINITY))
    }

    @Test fun fractionClampsWithoutOverflow() {
        assertEquals(0L, PlayerProgressRules.requestedPosition(100L, -10f))
        assertEquals(100L, PlayerProgressRules.requestedPosition(100L, 10f))
        assertEquals(50L, PlayerProgressRules.requestedPosition(100L, .5f))
        assertEquals(Long.MAX_VALUE, PlayerProgressRules.requestedPosition(Long.MAX_VALUE, 1f))
    }
}
