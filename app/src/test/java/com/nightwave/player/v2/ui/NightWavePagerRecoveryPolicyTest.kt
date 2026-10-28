package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class NightWavePagerRecoveryPolicyTest {
    private val policy = NightWavePagerRecoveryPolicy
    @Test fun idlePartialPagesRecoverToTheirNearestCurrentPage() {
        assertEquals(0, policy.repairPage(0, .35f, false, false, 4))
        assertEquals(1, policy.repairPage(1, -.35f, false, false, 4))
    }
    @Test fun centeredAndRoundingOnlyOffsetsDoNotStartRepair() {
        for (offset in listOf(0f, .0001f, -.0001f))
            assertNull(policy.repairPage(1, offset, false, false, 4))
    }
    @Test fun activeDraggingOrAnimationIsNeverInterrupted() {
        assertNull(policy.repairPage(1, .4f, true, false, 4))
        assertNull(policy.repairPage(1, .4f, false, true, 4))
    }
    @Test fun invalidOffsetsAndEmptyPagersDoNotIssueScrolls() {
        for (offset in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY))
            assertNull(policy.repairPage(1, offset, false, false, 4))
        assertNull(policy.repairPage(1, .4f, false, false, 0))
    }
    @Test fun restoredPageIndicesStayInsideCurrentDestinations() {
        assertEquals(0, policy.repairPage(-1, .2f, false, false, 4))
        assertEquals(3, policy.repairPage(9, -.2f, false, false, 4))
    }
    @Test fun reselectingTheCurrentTabStillRecentersItsPartialOrMovingPage() {
        assertTrue(policy.needsNavigation(1, .3f, false, 1))
        assertTrue(policy.needsNavigation(1, 0f, true, 1))
        assertFalse(policy.needsNavigation(1, 0f, false, 1))
    }
    @Test fun aDifferentDestinationAlwaysNavigatesAndInvalidOffsetsFailSafe() {
        assertTrue(policy.needsNavigation(0, 0f, false, 1))
        assertTrue(policy.needsNavigation(0, Float.NaN, false, 0))
    }
}
