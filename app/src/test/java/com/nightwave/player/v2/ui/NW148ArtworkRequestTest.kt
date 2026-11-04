package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class NW148ArtworkRequestTest {
    private val p = NightWaveArtworkRequestPolicy
    @Test fun landscapeViewportDoesNotBecomeAShortEdgeSquare() {
        assertEquals(NightWaveArtworkRequestPolicy.Pixels(1056, 594), p.pixels(1056, 594))
        assertEquals(NightWaveArtworkRequestPolicy.Pixels(594, 1056), p.pixels(594, 1056))
        assertEquals(NightWaveArtworkRequestPolicy.Pixels(182, 182), p.pixels(182, 182))
    }
    @Test fun capPreservesAspectAndHandlesHugeOrInvalidMeasurements() {
        assertEquals(NightWaveArtworkRequestPolicy.Pixels(1280, 720), p.pixels(3840, 2160))
        assertEquals(NightWaveArtworkRequestPolicy.Pixels(720, 1280), p.pixels(2160, 3840))
        for ((w, h) in listOf(0 to 720, 320 to -1, Int.MIN_VALUE to 1)) assertFalse(p.pixels(w, h).valid)
        for ((w, h) in listOf(Int.MAX_VALUE to Int.MAX_VALUE, Int.MAX_VALUE to 1, 1 to Int.MAX_VALUE, 1 to 1)) {
            val size = p.pixels(w, h)
            assertTrue(size.valid && size.width <= 1280 && size.height <= 1280)
            assertTrue(size.width.toLong() * size.height <= 1280L * 1280L)
        }
    }
    @Test fun completionCannotCrossTrackSourceCandidateOrResizeSession() {
        val a = ArtworkRequestSession(); val b = ArtworkRequestSession()
        val first = ArtworkRequestAttempt(a, 0, p.pixels(1056, 594))
        assertTrue(a.accepts(first, first))
        assertFalse(a.accepts(first, first.copy(session = b)))
        assertFalse(a.accepts(first, first.copy(candidate = 1)))
        assertFalse(a.accepts(first, first.copy(size = p.pixels(640, 360))))
        // Returning to the same source gets a new session, not the old request's authority.
        val returned = ArtworkRequestAttempt(ArtworkRequestSession(), 0, first.size)
        assertFalse(a.accepts(first, returned))
    }
    @Test fun disposedCompletionCannotResolveOrAdvanceFallback() {
        val session = ArtworkRequestSession()
        val ticket = ArtworkRequestAttempt(session, 0, p.pixels(500, 300))
        session.close()
        assertFalse(session.accepts(ticket, ticket))
    }
}
