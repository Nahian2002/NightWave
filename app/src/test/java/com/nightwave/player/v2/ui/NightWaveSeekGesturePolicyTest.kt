package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NightWaveSeekGesturePolicyTest {
    @Test fun touchFractionsRespectBothEndsAndMiddle() {
        assertEquals(0f, NightWaveSeekGesturePolicy.touchFraction(8f, 108f, 8f), 0.00001f)
        assertEquals(.5f, NightWaveSeekGesturePolicy.touchFraction(54f, 108f, 8f), 0.00001f)
        assertEquals(1f, NightWaveSeekGesturePolicy.touchFraction(100f, 108f, 8f), 0.00001f)
    }

    @Test fun outOfBoundsCoordinatesNeverProduceInvalidSeek() {
        assertEquals(0f, NightWaveSeekGesturePolicy.touchFraction(-999f, 108f, 8f), 0f)
        assertEquals(1f, NightWaveSeekGesturePolicy.touchFraction(999f, 108f, 8f), 0f)
        assertEquals(0f, NightWaveSeekGesturePolicy.touchFraction(Float.NaN, 108f, 8f), 0f)
        assertEquals(0f, NightWaveSeekGesturePolicy.touchFraction(10f, Float.POSITIVE_INFINITY, 8f), 0f)
    }

    @Test fun thumbStaysInsideTheVisualTrack() {
        assertEquals(8f, NightWaveSeekGesturePolicy.thumbX(-2f, 108f, 8f), 0f)
        assertEquals(54f, NightWaveSeekGesturePolicy.thumbX(.5f, 108f, 8f), 0f)
        assertEquals(100f, NightWaveSeekGesturePolicy.thumbX(2f, 108f, 8f), 0f)
    }

    @Test fun narrowAndInvalidTracksStayBounded() {
        assertEquals(5f, NightWaveSeekGesturePolicy.thumbX(1f, 10f, 8f), 0f)
        assertEquals(0f, NightWaveSeekGesturePolicy.thumbX(.5f, -3f, 8f), 0f)
        assertEquals(0f, NightWaveSeekGesturePolicy.touchFraction(0f, 0f, 8f), 0f)
    }
}
