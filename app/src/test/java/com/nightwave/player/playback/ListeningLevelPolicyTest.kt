package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningLevelPolicyTest {
    @Test fun normalLevelIsTheDefault() {
        assertEquals(100, ListeningLevelPolicy.DEFAULT_PERCENT)
    }
    @Test fun supportedPresetsNeverAmplify() {
        for (percent in ListeningLevelPolicy.presetsPercent) {
            assertTrue(ListeningLevelPolicy.gain(percent) in 0f..1f)
        }
    }
    @Test fun exactlyThreePresets() {
        assertEquals(listOf(100, 85, 70), ListeningLevelPolicy.presetsPercent)
    }
    @Test fun fullIsUnityGain() {
        assertEquals(1f, ListeningLevelPolicy.gain(100), .0001f)
    }
    @Test fun comfortableIsPointEightFive() {
        assertEquals(.85f, ListeningLevelPolicy.gain(85), .0001f)
    }
    @Test fun quietIsPointSeven() {
        assertEquals(.7f, ListeningLevelPolicy.gain(70), .0001f)
    }
    @Test fun invalidValuesAreRejected() {
        listOf(-10, 0, 69, 71, 86, 101, 150, Int.MAX_VALUE).forEach {
            assertFalse(ListeningLevelPolicy.accepts(it))
        }
    }
    @Test fun invalidValuesReturnUnityInsteadOfBoost() {
        assertEquals(1f, ListeningLevelPolicy.gain(999), .0001f)
    }
    @Test fun labelsReflectPhysicalOutputTrim() {
        assertEquals("Full · 100%", ListeningLevelPolicy.label(100))
        assertEquals("Comfortable · 85%", ListeningLevelPolicy.label(85))
        assertEquals("Quiet · 70%", ListeningLevelPolicy.label(70))
    }
    @Test fun unknownLabelsDoNotClaimSpecialMode() {
        assertEquals("Full · 100%", ListeningLevelPolicy.label(900))
    }
}
