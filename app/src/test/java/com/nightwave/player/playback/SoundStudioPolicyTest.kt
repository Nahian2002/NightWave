package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundStudioPolicyTest {
    @Test fun flatUsesZeroWhenDeviceAllowsIt() {
        assertEquals(listOf(0, 0, 0), SoundStudioPolicy.flatLevels(3, -1200, 1200))
    }

    @Test fun flatClampsToDeviceRangeWhenZeroUnsupported() {
        assertEquals(listOf(100, 100), SoundStudioPolicy.flatLevels(2, 100, 900))
    }

    @Test fun manualEditsClampWithoutMutatingOriginal() {
        val original = listOf(0, 100, -100)
        val adjusted = SoundStudioPolicy.updateLevel(original, 1, 2500, -1500, 1500)
        assertEquals(listOf(0, 1500, -100), adjusted)
        assertEquals(listOf(0, 100, -100), original)
        assertEquals(original, SoundStudioPolicy.updateLevel(original, 5, 400, -1500, 1500))
    }

    @Test fun refusesMissingOrStaleBandShapes() {
        assertFalse(SoundStudioPolicy.validLevels(emptyList(), 0, -1000, 1000))
        assertFalse(SoundStudioPolicy.validLevels(listOf(0), 2, -1000, 1000))
        assertFalse(SoundStudioPolicy.validLevels(listOf(2000), 1, -1000, 1000))
        assertFalse(SoundStudioPolicy.validLevels(listOf(0), 1, 1000, -1000))
        assertTrue(SoundStudioPolicy.validLevels(listOf(-100, 0, 100), 3, -1000, 1000))
    }

    @Test fun presetIndexIsDeviceBounded() {
        assertTrue(SoundStudioPolicy.validPreset(0, 0))
        assertTrue(SoundStudioPolicy.validPreset(2, 3))
        assertFalse(SoundStudioPolicy.validPreset(4, 3))
        assertFalse(SoundStudioPolicy.validPreset(21, 30))
        assertFalse(SoundStudioPolicy.validPreset(-1, 3))
    }

    @Test fun labelsAreReadableAndStable() {
        assertEquals("80 Hz", SoundStudioPolicy.frequencyLabel(80))
        assertEquals("1 kHz", SoundStudioPolicy.frequencyLabel(1000))
        assertEquals("1.5 kHz", SoundStudioPolicy.frequencyLabel(1500))
        assertEquals("+3.5 dB", SoundStudioPolicy.gainLabel(350))
        assertEquals("-2.5 dB", SoundStudioPolicy.gainLabel(-250))
    }
}
