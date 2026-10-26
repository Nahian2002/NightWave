package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test fun collapsedTimelineCannotSeekToBeginningFromTapOrDrag() {
        assertNull(NightWaveSeekGesturePolicy.touchFractionOrNull(4f, 10f, 8f))
        assertNull(NightWaveSeekGesturePolicy.touchFractionOrNull(8f, 16f, 8f))
        assertNull(NightWaveSeekGesturePolicy.touchFractionOrNull(0f, 0f, 8f))
        assertNull(NightWaveSeekGesturePolicy.touchFractionOrNull(10f, 10f, 100f))
    }

    @Test fun unusableCoordinatesNeverCommitASilentSeek() {
        assertNull(NightWaveSeekGesturePolicy.touchFractionOrNull(
            Float.NaN, 108f, 8f))
        assertNull(NightWaveSeekGesturePolicy.touchFractionOrNull(
            Float.POSITIVE_INFINITY, 108f, 8f))
        assertNull(NightWaveSeekGesturePolicy.touchFractionOrNull(
            12f, Float.NaN, 8f))
        assertNull(NightWaveSeekGesturePolicy.touchFractionOrNull(
            12f, 108f, Float.NaN))
        assertNull(NightWaveSeekGesturePolicy.touchFractionOrNull(
            12f, 108f, -1f))
    }

    @Test fun accessibilityRejectsNonFiniteValuesRatherThanAcknowledgingThem() {
        assertNull(NightWaveSeekGesturePolicy.accessibilityFractionOrNull(Float.NaN))
        assertNull(NightWaveSeekGesturePolicy.accessibilityFractionOrNull(Float.NEGATIVE_INFINITY))
        assertNull(NightWaveSeekGesturePolicy.accessibilityFractionOrNull(Float.POSITIVE_INFINITY))
        assertEquals(0f, NightWaveSeekGesturePolicy.accessibilityFractionOrNull(-5f)!!, 0f)
        assertEquals(.5f, NightWaveSeekGesturePolicy.accessibilityFractionOrNull(.5f)!!, 0f)
        assertEquals(1f, NightWaveSeekGesturePolicy.accessibilityFractionOrNull(50f)!!, 0f)
    }

    @Test fun visualThumbAndTapMappingUseTheSameGeometry() {
        for (v in listOf(0f, .05f, .25f, .5f, .75f, .95f, 1f)) {
            val x = NightWaveSeekGesturePolicy.thumbX(v, 320f, 8f)
            assertEquals(v, NightWaveSeekGesturePolicy.touchFractionOrNull(
                x, 320f, 8f)!!, 0.00001f)
        }
    }

    @Test fun validTapsClampOutsideTheTrackWithoutCrossingTracks() {
        assertEquals(0f, NightWaveSeekGesturePolicy.touchFractionOrNull(
            -50f, 320f, 8f)!!, 0f)
        assertEquals(1f, NightWaveSeekGesturePolicy.touchFractionOrNull(
            900f, 320f, 8f)!!, 0f)
    }
}
