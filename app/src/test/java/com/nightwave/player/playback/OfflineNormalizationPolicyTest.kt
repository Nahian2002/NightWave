package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure local gain decisions: no cloud, media file, or Android decoder required. */
class OfflineNormalizationPolicyTest {
    @Test fun quietTracksAreNotArtificiallyBoosted() {
        assertEquals(1f, OfflineNormalizationPolicy.attenuation(.08f, .70f)!!)
    }

    @Test fun loudTracksAreReducedToTarget() {
        val gain = OfflineNormalizationPolicy.attenuation(.26f, .80f)!!
        assertEquals(.5f, gain, .00001f)
    }

    @Test fun aHotPeakSetsAClippingSafetyCeiling() {
        assertEquals(.98f, OfflineNormalizationPolicy.attenuation(.06f, 1f)!!, .00001f)
    }

    @Test fun invalidMeasurementsAreRejected() {
        assertNull(OfflineNormalizationPolicy.attenuation(0f, 0f))
        assertNull(OfflineNormalizationPolicy.attenuation(-.1f, .4f))
        assertNull(OfflineNormalizationPolicy.attenuation(Float.NaN, .4f))
        assertNull(OfflineNormalizationPolicy.attenuation(.1f, Float.POSITIVE_INFINITY))
        assertNull(OfflineNormalizationPolicy.attenuation(.5f, .1f))
    }

    @Test fun normalizationIsOffByDefaultAndRejectedGainsStayUnity() {
        assertEquals(1f, OfflineNormalizationPolicy.gain(false, .5f))
        assertEquals(1f, OfflineNormalizationPolicy.gain(true, null))
        assertEquals(1f, OfflineNormalizationPolicy.gain(true, Float.NaN))
        assertEquals(1f, OfflineNormalizationPolicy.gain(true, 1.2f))
        assertEquals(.5f, OfflineNormalizationPolicy.gain(true, .5f))
    }

    @Test fun listeningLevelAndNormalizationNeverAmplify() {
        assertEquals(.35f, OfflineNormalizationPolicy.combined(.7f, .5f), .00001f)
        assertEquals(1f, OfflineNormalizationPolicy.combined(2f, 1.2f))
        assertEquals(0f, OfflineNormalizationPolicy.combined(Float.NaN, .8f))
        assertEquals(.7f, OfflineNormalizationPolicy.combined(.7f, Float.NaN), .00001f)
    }

    @Test fun crossfadeDecksKeepIndividualTrackReductions() {
        val (outgoing, incoming) = CrossfadePolicy.gains(.5f, 1f)
        val old = outgoing * OfflineNormalizationPolicy.combined(.85f, .5f)
        val next = incoming * OfflineNormalizationPolicy.combined(.85f, .8f)
        assertTrue(old + next <= .85f)
        assertEquals(.5525f, old + next, .00001f)
    }

    @Test fun analysisNeverExceedsBoundedWork() {
        assertEquals(12_000L, OfflineNormalizationPolicy.MAX_SAMPLED_MS)
        assertEquals(3_000L, OfflineNormalizationPolicy.MAX_ANALYSIS_WALL_MS)
        assertTrue(OfflineNormalizationPolicy.MAX_SAMPLES <= 1_500_000L)
    }
}
