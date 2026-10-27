package com.nightwave.player.v2.ui

import com.nightwave.player.v2.model.NightWaveDesign
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Pure UI contracts; never touches network, media playback or saved data. */
class NightWaveAccessibilityPolicyTest {
    private val p = NightWaveAccessibilityPolicy

    @Test fun fullTabNamesSurviveVisualEllipsizing() {
        assertEquals("Home tab", p.navigationAnnouncement("Home"))
        assertEquals("Library tab", p.navigationAnnouncement("Library"))
        assertEquals("You tab", p.navigationAnnouncement("You"))
        assertEquals("Destination tab", p.navigationAnnouncement(""))
    }

    @Test fun banglaQueueNamesPreserveVowelMarksAndArtist() {
        val title = "ভালোবাসি ঝুম"
        val artist = "শিল্পী"
        assertEquals("Play track 8: " + title + ", " + artist,
            p.queueAnnouncement(8, title, artist, false))
        assertEquals("Playing track 8: " + title + ", " + artist,
            p.queueAnnouncement(8, title, artist, true))
    }

    @Test fun missingQueueFieldsHaveSafeTalkBackFallbacks() {
        assertEquals("Play track 1: Untitled track",
            p.queueAnnouncement(-17, "", "", false))
        assertEquals("Play track 1: Hello",
            p.queueAnnouncement(0, "Hello", "", false))
    }

    @Test fun editorialCardsGrowWithAccessibilityFontScale() {
        assertEquals(164, p.featureCardMinHeightDp(1f))
        assertEquals(214, p.featureCardMinHeightDp(1.3f))
        assertEquals(270, p.featureCardMinHeightDp(2f))
        assertEquals(324, p.featureCardMinHeightDp(3f))
        assertEquals(118, p.mixCardMinHeightDp(true, 1f))
        assertEquals(146, p.mixCardMinHeightDp(false, 1f))
        assertEquals(252, p.mixCardMinHeightDp(true, 3f))
        assertEquals(282, p.mixCardMinHeightDp(false, 3f))
        assertEquals(224, p.moodCardMinHeightDp(3f))
    }

    @Test fun corruptFontScalesNeverProduceUnboundedCardSizes() {
        for (scale in floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, -1f, 0f, 100f)) {
            assertTrue(p.featureCardMinHeightDp(scale) in 164..324)
            assertTrue(p.mixCardMinHeightDp(false, scale) in 146..282)
            assertTrue(p.moodCardMinHeightDp(scale) in 118..224)
        }
    }

    @Test fun supportingMetadataGetsTwoLinesAtLargeText() {
        assertEquals(1, NightWavePremiumDesignTokens.supportingLines(1f))
        assertEquals(1, NightWavePremiumDesignTokens.supportingLines(1.3f))
        assertEquals(2, NightWavePremiumDesignTokens.supportingLines(1.8f))
        assertEquals(2, NightWavePremiumDesignTokens.supportingLines(3f))
        assertEquals(1, NightWavePremiumDesignTokens.supportingLines(Float.NaN))
    }

    @Test fun allFourThemesRetainAccessibleTextContrast() {
        for (theme in NightWaveTheme.values()) {
            val palette = NightWaveDesign.palette(theme)
            assertTrue(theme.toString() + " main text",
                contrast(palette.foreground, palette.background) >= 4.5)
            assertTrue(theme.toString() + " secondary text",
                contrast(palette.secondary, palette.surface) >= 4.5)
            assertTrue(theme.toString() + " accent text",
                contrast(palette.accent, palette.surface) >= 4.5)
        }
    }

    private fun contrast(a: Long, b: Long): Double {
        val x = luminance(a)
        val y = luminance(b)
        return (maxOf(x, y) + .05) / (minOf(x, y) + .05)
    }

    private fun luminance(argb: Long): Double {
        fun linear(channel: Int): Double {
            val s = channel / 255.0
            return if (s <= .04045) s / 12.92 else ((s + .055) / 1.055).pow(2.4)
        }
        return .2126 * linear(((argb shr 16) and 255).toInt()) +
            .7152 * linear(((argb shr 8) and 255).toInt()) +
            .0722 * linear((argb and 255).toInt())
    }
}
