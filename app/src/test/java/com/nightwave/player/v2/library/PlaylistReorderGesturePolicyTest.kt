package com.nightwave.player.v2.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistReorderGesturePolicyTest {
    @Test fun dragTranslatesToNearestRowWithoutOverreactingToTinyMovements() {
        assertEquals(0, PlaylistReorderGesturePolicy.offsetToSteps(10f, 100))
        assertEquals(1, PlaylistReorderGesturePolicy.offsetToSteps(80f, 100))
        assertEquals(-2, PlaylistReorderGesturePolicy.offsetToSteps(-210f, 100))
    }

    @Test fun invalidSizeAndCoordinatesNeverGenerateMoves() {
        assertEquals(0, PlaylistReorderGesturePolicy.offsetToSteps(100f, 0))
        assertEquals(0, PlaylistReorderGesturePolicy.offsetToSteps(Float.NaN, 80))
        assertEquals(0, PlaylistReorderGesturePolicy.offsetToSteps(Float.POSITIVE_INFINITY, 80))
        assertNull(PlaylistReorderGesturePolicy.target(-1, 2, 3))
        assertNull(PlaylistReorderGesturePolicy.target(0, 1, 1))
        assertNull(PlaylistReorderGesturePolicy.target(0, 0, 3))
    }

    @Test fun dragIsClampedToPlaylistBoundaries() {
        assertEquals(2, PlaylistReorderGesturePolicy.target(0, 99, 3))
        assertEquals(0, PlaylistReorderGesturePolicy.target(2, -99, 3))
        assertEquals(299, PlaylistReorderGesturePolicy.offsetToSteps(100000f, 1))
    }
}
