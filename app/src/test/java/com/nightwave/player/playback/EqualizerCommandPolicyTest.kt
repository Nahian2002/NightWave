package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

class EqualizerCommandPolicyTest {
    private val centers = listOf(60, 230, 910, 3600, 14000)
    private fun token(session: Int = 7, hz: List<Int> = centers,
        low: Int = -1500, high: Int = 1500, presets: List<String> = listOf("Flat", "Warm")) =
        EqualizerCommandPolicy.token(session, hz, low, high, presets)

    @Test fun nw120UnchangedHardwareIdentityAcceptsTheReviewedCommand() {
        val first = token()
        assertNotNull(first)
        assertEquals(64, first!!.length)
        assertTrue(EqualizerCommandPolicy.accepts(first, token()))
        assertFalse(EqualizerCommandPolicy.shouldResetDraft(first, token()))
    }

    @Test fun nw120SameCountWithDifferentFrequencyCentersRejectsOldDraft() {
        val replacement = token(hz = listOf(80, 300, 1000, 4000, 16000))
        assertFalse(EqualizerCommandPolicy.accepts(token(), replacement))
        assertTrue(EqualizerCommandPolicy.shouldResetDraft(token(), replacement))
        assertFalse(EqualizerCommandPolicy.accepts(token(), token(hz = centers.reversed())))
    }

    @Test fun nw120SessionRangeAndPresetTableChangesInvalidateCommands() {
        for (changed in listOf(token(session = 8), token(low = -1000), token(high = 1000),
            token(presets = listOf("Flat", "Vocal")), token(presets = listOf("Warm", "Flat")))) {
            assertFalse(EqualizerCommandPolicy.accepts(token(), changed))
            assertTrue(EqualizerCommandPolicy.shouldResetDraft(token(), changed))
        }
    }

    @Test fun nw120PresetNamesCannotAliasThroughSeparatorsOrUnicode() {
        assertNotEquals(token(presets = listOf("a", "bc")), token(presets = listOf("ab", "c")))
        assertNotEquals(token(presets = listOf("a|b", "c")), token(presets = listOf("a", "b|c")))
        assertNotEquals(token(presets = listOf("মালা")), token(presets = listOf("মেলা")))
    }

    @Test fun nw120UnavailableCorruptOrUnboundSnapshotCannotAuthorizeApply() {
        for (invalid in listOf(token(session = 0), token(session = -1), token(hz = emptyList()),
            token(hz = listOf(0)), token(hz = listOf(60, 60)), token(hz = listOf(100001)),
            token(low = 100, high = -100), token(low = Int.MIN_VALUE),
            token(hz = (1..33).toList()), token(presets = List(21) { "Preset $it" }),
            token(presets = listOf("X".repeat(61))))) assertNull(invalid)
        for (missing in listOf(null, "", "x".repeat(64), token()!!.uppercase()))
            assertFalse(EqualizerCommandPolicy.accepts(missing, token()))
        assertFalse(EqualizerCommandPolicy.accepts(token(), null))
        assertTrue(EqualizerCommandPolicy.shouldResetDraft(token(), null))
    }

    @Test fun nw120ManualOnlyAndFixedRangeDevicesHaveValidIdentity() {
        assertNotNull(token(presets = emptyList()))
        assertNotNull(token(low = 0, high = 0))
        assertNotNull(token(hz = (1..32).toList()))
    }

    @Test fun nw120IdentityAndDraftChecksNeverMutateInputs() {
        val mutable = centers.toMutableList()
        val presets = mutableListOf("Flat", "Warm")
        val original = token(hz = mutable, presets = presets)
        repeat(100) { assertTrue(EqualizerCommandPolicy.accepts(original, token(hz = mutable, presets = presets))) }
        assertEquals(centers, mutable)
        assertEquals(listOf("Flat", "Warm"), presets)
    }
}
