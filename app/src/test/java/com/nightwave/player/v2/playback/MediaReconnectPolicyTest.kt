package com.nightwave.player.v2.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaReconnectPolicyTest {
    private fun allowed(
        released: Boolean = false,
        foreground: Boolean = true,
        budget: Boolean = true,
        attached: Boolean = false,
        pending: Boolean = false
    ) = MediaReconnectPolicy.mayAttempt(
        released, foreground, budget, attached, pending
    )

    @Test fun oneVisibleAttemptIsAllowedAfterDisconnection() {
        assertTrue(allowed())
    }

    @Test fun leavingScreenOrDestroyingActivityCancelsReconnect() {
        assertFalse(allowed(foreground = false))
        assertFalse(allowed(released = true))
    }

    @Test fun consumedOneShotBudgetCannotLoopInBackground() {
        assertFalse(allowed(budget = false))
    }

    @Test fun activeControllerOrPendingRequestBlocksDuplicates() {
        assertFalse(allowed(attached = true))
        assertFalse(allowed(pending = true))
        assertFalse(allowed(attached = true, pending = true))
    }
}
