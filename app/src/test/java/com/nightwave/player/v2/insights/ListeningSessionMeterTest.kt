package com.nightwave.player.v2.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningSessionMeterTest {
    private val a = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val b = "https://www.youtube.com/watch?v=9bZkp7q19f0"

    @Test fun playIsNotCreditedOnSelectionOrBuffering() {
        val meter = ListeningSessionMeter()
        assertTrue(meter.observe(a, "A", "Artist", false, 0, 1000).isEmpty())
        assertTrue(meter.observe(a, "A", "Artist", false, 2000, 3000).isEmpty())
        assertTrue(meter.finish(3000, 4000).isEmpty())
    }

    @Test fun aShortAudibleSessionDoesNotQualifyAsAPlay() {
        val meter = ListeningSessionMeter()
        meter.observe(a, "A", "Artist", true, 0, 0)
        val output = meter.observe(a, "A", "Artist", false, 4000, 4000)
        assertEquals(4000L, output.single().listenedMs)
        assertFalse(output.single().qualifyingPlay)
    }

    @Test fun thresholdCreditsExactlyOneQualifiedSession() {
        val meter = ListeningSessionMeter()
        meter.observe(a, "A", "Artist", true, 0, 0)
        val pulses = mutableListOf<ListeningPulse>()
        for (second in 1..14) {
            pulses += meter.observe(a, "A", "Artist", true, second * 2500L,
                second * 2500L)
        }
        pulses += meter.finish(37500L, 37500L)
        assertEquals(1, pulses.count { it.qualifyingPlay })
        assertEquals(37500L, pulses.sumOf { it.listenedMs })
    }

    @Test fun pauseNeverChargesPausedTime() {
        val meter = ListeningSessionMeter()
        meter.observe(a, "A", "Artist", true, 1000, 1000)
        val first = meter.observe(a, "A", "Artist", false, 4000, 4000)
        assertEquals(3000L, first.single().listenedMs)
        meter.observe(a, "A", "Artist", false, 400_000, 400_000)
        meter.observe(a, "A", "Artist", true, 410_000, 410_000)
        val later = meter.observe(a, "A", "Artist", false, 413_000, 413_000)
        assertEquals(3000L, later.single().listenedMs)
    }

    @Test fun largeElapsedGapIsCappedAfterAppSuspension() {
        val meter = ListeningSessionMeter()
        meter.observe(a, "A", "Artist", true, 0, 0)
        val pulses = meter.observe(a, "A", "Artist", false, 3_600_000, 3_600_000)
        assertEquals(ListeningSessionMeter.MAX_OBSERVED_GAP_MS,
            pulses.single().listenedMs)
    }

    @Test fun monotonicClockGoingBackwardsNeverCreatesNegativeTime() {
        val meter = ListeningSessionMeter()
        meter.observe(a, "A", "Artist", true, 2000, 2000)
        assertTrue(meter.observe(a, "A", "Artist", false, 1000, 1000).isEmpty())
    }

    @Test fun trackTransitionAttributesPreviousTailToPreviousSong() {
        val meter = ListeningSessionMeter()
        meter.observe(a, "A", "Artist", true, 0, 0)
        val out = meter.observe(b, "B", "Singer", true, 3500, 3500)
        assertEquals(a, out.single().trackId)
        assertEquals(3500L, out.single().listenedMs)
        val bPulse = meter.finish(6500, 6500)
        assertEquals(b, bPulse.single().trackId)
        assertEquals(3000L, bPulse.single().listenedMs)
    }

    @Test fun explicitRepeatFinishesOneSessionBeforeTheNext() {
        val meter = ListeningSessionMeter()
        meter.observe(a, "A", "Artist", true, 0, 0)
        val first = meter.finish(2500, 2500)
        assertEquals(2500L, first.single().listenedMs)
        meter.observe(a, "A", "Artist", true, 3000, 3000)
        val second = meter.finish(5500, 5500)
        assertEquals(2500L, second.single().listenedMs)
    }

    @Test fun resetDropsUnflushedEventsAfterConsentRevocation() {
        val meter = ListeningSessionMeter()
        meter.observe(a, "A", "Artist", true, 0, 0)
        meter.observe(a, "A", "Artist", true, 3000, 3000)
        meter.reset()
        assertTrue(meter.finish(5000, 5000).isEmpty())
    }

    @Test fun stateDoesNotCreditPlayTwiceAfterLongPauseWithinSameSession() {
        val meter = ListeningSessionMeter()
        meter.observe(a, "A", "Artist", true, 0, 0)
        val pulses = mutableListOf<ListeningPulse>()
        for (i in 1..12) pulses += meter.observe(a, "A", "Artist", true, i * 2500L, i * 2500L)
        meter.observe(a, "A", "Artist", false, 30_100, 30_100).let(pulses::addAll)
        meter.observe(a, "A", "Artist", true, 5_000_000, 5_000_000)
        meter.observe(a, "A", "Artist", false, 5_002_000, 5_002_000).let(pulses::addAll)
        assertEquals(1, pulses.count { it.qualifyingPlay })
    }
}
