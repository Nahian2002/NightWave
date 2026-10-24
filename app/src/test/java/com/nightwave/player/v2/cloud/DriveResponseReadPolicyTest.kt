package com.nightwave.player.v2.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveResponseReadPolicyTest {
    @Test fun acceptsExactLimitAndRejectsOversize() {
        assertTrue(DriveResponseReadPolicy.canAppend(0, 8, 8))
        assertTrue(DriveResponseReadPolicy.canAppend(7, 1, 8))
        assertFalse(DriveResponseReadPolicy.canAppend(8, 1, 8))
        assertFalse(DriveResponseReadPolicy.canAppend(7, 2, 8))
    }

    @Test fun malformedAndOverflowingCountsFailClosed() {
        assertFalse(DriveResponseReadPolicy.canAppend(-1, 1, 8))
        assertFalse(DriveResponseReadPolicy.canAppend(1, 0, 8))
        assertFalse(DriveResponseReadPolicy.canAppend(1, -1, 8))
        assertFalse(DriveResponseReadPolicy.canAppend(Int.MAX_VALUE, 1, Int.MAX_VALUE))
        assertFalse(DriveResponseReadPolicy.canAppend(1, Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test fun emptyReadLoopIsBounded() {
        assertFalse(DriveResponseReadPolicy.stalled(0))
        assertFalse(DriveResponseReadPolicy.stalled(8))
        assertTrue(DriveResponseReadPolicy.stalled(9))
    }
}
