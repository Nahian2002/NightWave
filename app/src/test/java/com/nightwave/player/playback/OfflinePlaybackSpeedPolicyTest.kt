package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePlaybackSpeedPolicyTest {
    @Test fun normalIsDefault() {
        assertEquals(100, OfflinePlaybackSpeedPolicy.NORMAL_PERCENT)
        assertEquals(1f, OfflinePlaybackSpeedPolicy.speed(100), 0.0001f)
    }
    @Test fun allowedPresetsAreBounded() {
        assertEquals(listOf(75, 100, 125, 150, 200),
            OfflinePlaybackSpeedPolicy.presetsPercent)
    }
    @Test fun rejectsUnknownPercentages() {
        listOf(-10, 0, 50, 101, 175, 201, Int.MAX_VALUE).forEach {
            assertFalse(OfflinePlaybackSpeedPolicy.accepts(it))
        }
    }
    @Test fun invalidFallsBackToNormalSpeed() {
        assertEquals(1f, OfflinePlaybackSpeedPolicy.speed(999), 0.0001f)
    }
    @Test fun slowestIsPointSevenFive() {
        assertEquals(.75f, OfflinePlaybackSpeedPolicy.speed(75), 0.0001f)
    }
    @Test fun onePointTwoFiveIsSupported() {
        assertEquals(1.25f, OfflinePlaybackSpeedPolicy.speed(125), 0.0001f)
    }
    @Test fun onePointFiveIsSupported() {
        assertEquals(1.5f, OfflinePlaybackSpeedPolicy.speed(150), 0.0001f)
    }
    @Test fun maximumIsTwoTimes() {
        assertEquals(2f, OfflinePlaybackSpeedPolicy.speed(200), 0.0001f)
    }
    @Test fun labelsAreExactWithoutFloatRounding() {
        assertEquals(listOf("0.75×", "1×", "1.25×", "1.5×", "2×"),
            OfflinePlaybackSpeedPolicy.presetsPercent.map(OfflinePlaybackSpeedPolicy::label))
    }
    @Test fun unsupportedLabelDoesNotMislead() {
        assertEquals("1×", OfflinePlaybackSpeedPolicy.label(999))
    }
}
