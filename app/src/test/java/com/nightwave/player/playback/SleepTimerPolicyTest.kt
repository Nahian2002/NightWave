package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure monotonic sleep timer rules. No device clock or Media3 dependencies. */
class SleepTimerPolicyTest {
    @Test fun presetsAreExplicitAndBounded() {
        assertEquals(listOf(15, 30, 45, 60), SleepTimerPolicy.choicesMinutes)
        for (minutes in SleepTimerPolicy.choicesMinutes) {
            assertEquals(minutes.toLong() * 60_000L,
                SleepTimerPolicy.deadlineMs(0L, minutes))
        }
    }

    @Test fun arbitraryIntentsCannotArmTimer() {
        for (minutes in listOf(-1, 0, 1, 14, 61, Int.MAX_VALUE)) {
            assertNull(SleepTimerPolicy.deadlineMs(123L, minutes))
        }
    }

    @Test fun invalidClockCannotArmTimer() {
        assertNull(SleepTimerPolicy.deadlineMs(-1L, 15))
    }

    @Test fun overflowCannotScheduleTimer() {
        assertNull(SleepTimerPolicy.deadlineMs(Long.MAX_VALUE, 15))
        assertNull(SleepTimerPolicy.deadlineMs(Long.MAX_VALUE - 59_999L, 15))
    }

    @Test fun deadlineUsesMonotonicElapsedTime() {
        val start = 100_000L
        val deadline = SleepTimerPolicy.deadlineMs(start, 30)!!
        assertEquals(start + 1_800_000L, deadline)
        assertEquals(1_800_000L, SleepTimerPolicy.remainingMs(start, deadline))
        assertEquals(900_000L, SleepTimerPolicy.remainingMs(start + 900_000L, deadline))
    }

    @Test fun expiredTimerHasNoNegativeRemainingTime() {
        val deadline = SleepTimerPolicy.deadlineMs(10L, 15)!!
        assertEquals(0L, SleepTimerPolicy.remainingMs(deadline, deadline))
        assertEquals(0L, SleepTimerPolicy.remainingMs(deadline + 1L, deadline))
        assertTrue(SleepTimerPolicy.expires(deadline, deadline))
        assertFalse(SleepTimerPolicy.expires(deadline - 1L, deadline))
    }

    @Test fun missingOrStaleDeadlineCannotExpire() {
        assertFalse(SleepTimerPolicy.expires(10L, 0L))
        assertFalse(SleepTimerPolicy.expires(10L, -1L))
        assertEquals(0L, SleepTimerPolicy.remainingMs(10L, 0L))
    }

    @Test fun remainingTimeLabelRoundsUpToAvoidPrematureZero() {
        assertEquals("Sleep timer", SleepTimerPolicy.label(0L))
        assertEquals("Sleep in 1 min", SleepTimerPolicy.label(1L))
        assertEquals("Sleep in 1 min", SleepTimerPolicy.label(60_000L))
        assertEquals("Sleep in 2 min", SleepTimerPolicy.label(60_001L))
    }
}
