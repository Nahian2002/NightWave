package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

class SoundStudioDraftPolicyTest {
    private fun token(session: Int = 7, centers: List<Int> = listOf(60, 1000),
        min: Int = -1500, max: Int = 1500, presets: List<String> = listOf("Flat", "Warm")) =
        EqualizerCommandPolicy.token(session, centers, min, max, presets)

    private fun adopt(requestedToken: String?, currentToken: String?, selected: Int, active: Boolean,
        requested: List<Int>, actual: List<Int>) = SoundStudioDraftPolicy.adoptManualReadback(
            requestedToken, currentToken, selected, active, requested, actual, committedRequested = true)

    @Test fun failedDurableSaveCannotMasqueradeAsDeviceRounding() {
        assertFalse(SoundStudioDraftPolicy.adoptManualReadback(token(), token(), 21, true,
            listOf(250, 0), listOf(200, 0), committedRequested = false))
    }

    @Test fun acknowledgedExactOrDeviceRoundedCurveAdoptsCurrentReadback() {
        for (actual in listOf(listOf(250, -50), listOf(200, 0))) {
            assertTrue(adopt(token(), token(),
                21, true, listOf(250, -50), actual))
        }
    }

    @Test fun failedOrInactiveManualApplyKeepsDraftForRetry() {
        assertFalse(adopt(token(), token(), 21,
            false, listOf(250, -50), listOf(250, -50)))
        assertFalse(adopt(token(), token(), 21,
            true, listOf(300, 0), listOf(0, 0)))
        assertFalse(adopt(token(), token(), 2,
            true, listOf(0, 0), listOf(0, 0)))
    }

    @Test fun changedSessionSameCountFrequenciesRangeAndPresetTableRejectReadback() {
        for (changed in listOf(token(8), token(centers = listOf(80, 1200)),
            token(min = -1000), token(max = 1000), token(presets = listOf("Warm", "Flat")))) {
            assertFalse(adopt(token(), changed,
                21, true, listOf(0, 0), listOf(0, 0)))
            assertTrue(EqualizerCommandPolicy.shouldResetDraft(token(), changed))
        }
    }

    @Test fun lostControlMissingOrMalformedReceiptsCannotClaimManualSuccess() {
        for (changed in listOf(null, "", "x".repeat(64))) {
            assertFalse(adopt(token(), changed,
                21, true, listOf(0, 0), listOf(0, 0)))
        }
        for (actual in listOf(emptyList(), listOf(0), listOf(1501, 0))) {
            assertFalse(adopt(token(), token(),
                21, true, listOf(0, 0), actual))
        }
    }

    @Test fun cachedStateNeverAppearsActiveAndSavedUnavailableIsExplicit() {
        val names = listOf("Flat", "Warm")
        assertEquals("Checking current equalizer", SoundStudioDraftPolicy.activeLabel(false, true, true, 2, names))
        assertEquals("Equalizer unavailable", SoundStudioDraftPolicy.activeLabel(true, false, true, 2, names))
        assertEquals("Saved choice · not active", SoundStudioDraftPolicy.activeLabel(true, true, false, 2, names))
        assertEquals("Equalizer off", SoundStudioDraftPolicy.activeLabel(true, true, false, 0, names))
        assertEquals("Equalizer off · no active session", SoundStudioDraftPolicy.activeLabel(true, false, false, 0, names))
    }

    @Test fun activeLabelUsesExactDevicePresetOrManualIdentity() {
        assertEquals("Active · Warm", SoundStudioDraftPolicy.activeLabel(true, true, true, 2, listOf("Flat", "Warm")))
        assertEquals("Active · Manual EQ", SoundStudioDraftPolicy.activeLabel(true, true, true, 21, emptyList()))
        assertEquals("Current equalizer needs refresh", SoundStudioDraftPolicy.activeLabel(true, true, true, 2, emptyList()))
    }

    @Test fun accessibleStepsClampToActualSafeRangeWithoutChangingOtherBands() {
        assertEquals(150, SoundStudioDraftPolicy.adjust(100, 100, -150, 150))
        assertEquals(-150, SoundStudioDraftPolicy.adjust(-100, -100, -150, 150))
        assertEquals(150, SoundStudioDraftPolicy.adjust(150, 100, -150, 150))
        assertEquals(0, SoundStudioDraftPolicy.adjust(0, 100, 0, 0))
        val original = listOf(0, 50)
        val updated = SoundStudioPolicy.updateLevel(original, 1,
            SoundStudioDraftPolicy.adjust(original[1], -100, -150, 150)!!, -150, 150)
        assertEquals(listOf(0, -50), updated)
        assertEquals(listOf(0, 50), original)
    }

    @Test fun unsafeOrUnsupportedStepIsRejectedInsteadOfCoercingBadState() {
        assertNull(SoundStudioDraftPolicy.adjust(1600, -100, -1500, 1500))
        assertNull(SoundStudioDraftPolicy.adjust(0, 100, 100, -100))
        assertNull(SoundStudioDraftPolicy.adjust(0, Int.MAX_VALUE, -1500, 1500))
        assertNull(SoundStudioDraftPolicy.adjust(0, 0, -1500, 1500))
    }
}
