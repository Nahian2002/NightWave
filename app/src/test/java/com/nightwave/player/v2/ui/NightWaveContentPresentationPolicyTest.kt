package com.nightwave.player.v2.ui

import com.nightwave.player.v2.model.*
import org.junit.Assert.*
import org.junit.Test

class NightWaveContentPresentationPolicyTest {
    @Test fun errorContainerCannotAliasOrdinaryMusicOrSelectionSurfaces() {
        NightWaveTheme.entries.forEach { theme ->
            val p = NightWaveDesign.palette(theme)
            val error = NightWaveContentPresentationPolicy.errorSurface(p)
            assertEquals(255L, (error shr 24) and 255L)
            assertFalse(error in listOf(p.background, p.surface, p.raised, p.accentSoft, p.secondarySoft))
        }
    }
    @Test fun actualErrorTextRemainsReadableOnItsDistinctContainer() {
        NightWaveTheme.entries.forEach { theme ->
            val p = NightWaveDesign.palette(theme)
            assertTrue("$theme error contrast", NightWaveContrastPolicy.ratio(
                p.danger, NightWaveContentPresentationPolicy.errorSurface(p)) >= 4.5)
        }
    }
    @Test fun uniqueSongsAndRepeatedPlaysKeepTheirDifferentMeaning() {
        assertEquals("24 songs", NightWaveContentPresentationPolicy.historySummary(24, 24))
        assertEquals("24 songs · 58 plays", NightWaveContentPresentationPolicy.historySummary(24, 58))
        assertEquals("1 song · 3 plays", NightWaveContentPresentationPolicy.historySummary(1, 3))
    }
    @Test fun automaticHeadingAllowsShorterRowsWithoutDroppingMembershipEvidence() {
        assertEquals("24 songs · 24 in history", NightWaveContentPresentationPolicy.automaticSummary(
            "24 songs · Automatic collection · 24 in history"))
        assertEquals("9 songs · 3 liked · 2 in playlists · 7 in history", NightWaveContentPresentationPolicy.automaticSummary(
            "9 songs · Automatic collection · 3 liked · 2 in playlists · 7 in history"))
    }
    @Test fun emptyAndInconsistentCountsNeverInventAdditionalPlays() {
        assertEquals("0 songs", NightWaveContentPresentationPolicy.historySummary(-1, -1))
        assertEquals("1 song", NightWaveContentPresentationPolicy.historySummary(1, 0))
    }
}
