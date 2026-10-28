package com.nightwave.player.v2.ui

import com.nightwave.player.v2.model.*
import org.junit.Assert.*
import org.junit.Test

/** Real theme and interaction policies. Rendered Android acceptance is a separate matrix. */
class NightWaveMatteDesignTest {
    private val policy = NightWaveMattePolicy
    private fun palettes() = NightWaveTheme.entries.map { it to NightWaveDesign.palette(it) }
    private fun blend(fg: Long, bg: Long, alpha: Float): Long {
        var color = 0xFF000000L
        for (shift in listOf(16, 8, 0)) {
            val value = (((fg ushr shift) and 255) * alpha + ((bg ushr shift) and 255) * (1f - alpha)).toLong()
            color = color or (value shl shift)
        }
        return color
    }

    @Test fun allThemesKeepRequiredTextReadableOnEveryOpaqueSurface() {
        for ((theme, p) in palettes()) for (surface in listOf(p.background, p.surface, p.raised)) {
            for (text in listOf(p.foreground, p.secondary, p.accent))
                assertTrue("$theme text", NightWaveContrastPolicy.ratio(text, surface) >= 4.5)
        }
    }
    @Test fun transportHasStrongContrastInBothLightAndDarkThemes() {
        for ((theme, p) in palettes()) assertTrue("$theme transport",
            NightWaveContrastPolicy.ratio(policy.transportInk(p), policy.transportFill(p)) >= 7.0)
    }
    @Test fun semanticOutlinesRemainVisibleAndSeparateFromDecorativeStrokes() {
        for ((theme, p) in palettes()) for (surface in listOf(p.background, p.surface, p.raised))
            assertTrue("$theme outline", NightWaveContrastPolicy.ratio(p.outline, surface) >= 3.0)
        assertTrue(policy.BORDER_ALPHA < .1f)
    }
    @Test fun playerTextSurvivesWorstPermittedArtworkColors() {
        for ((theme, p) in palettes()) for (art in listOf(0xFFFFFFFF, 0xFF000000, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF)) {
            val backdrop = blend(art, p.background, policy.backdropAlpha(p.isDark))
            for (text in listOf(p.foreground, policy.playerSecondary(p), p.danger))
                assertTrue("$theme player $art", NightWaveContrastPolicy.ratio(text, backdrop) >= 4.5)
            assertTrue("$theme selected control", NightWaveContrastPolicy.ratio(p.accent, backdrop) >= 3.0)
        }
    }
    @Test fun nativeOrdinaryTextMatchesTheChosenTheme() {
        for ((_, p) in palettes()) {
            assertEquals(p.foreground, NightWaveNativeMattePolicy.text(-1L, p))
            assertEquals(p.secondary, NightWaveNativeMattePolicy.text(0xFFB5C8D8, p))
            for (source in listOf(-1L, 0xFFB5C8D8)) assertTrue(NightWaveContrastPolicy.ratio(
                NightWaveNativeMattePolicy.text(source, p), p.surface) >= 4.5)
        }
    }
    @Test fun nativeBrightActionInkAndFillRemainAPairedContract() {
        for ((theme, p) in palettes()) for (ink in listOf(0xFF09131B, 0xFF091B20, 0xFF0C1B20, 0xFF101A21))
            for (fill in listOf(0xFF91E2D7, 0xFF94E2D8, 0xFF98E5DB))
                assertTrue("$theme native button", NightWaveContrastPolicy.ratio(
                    NightWaveNativeMattePolicy.text(ink, p), NightWaveNativeMattePolicy.surface(fill, p)) >= 4.5)
    }
    @Test fun nativeWarningRolesStayReadableWithoutDeletingTheirMessages() {
        for ((_, p) in palettes()) for (warning in listOf(0xFFFFC07D, 0xFFF0B7B7, 0xFFF1BDBD, 0xFFE2C99A)) {
            assertEquals(p.danger, NightWaveNativeMattePolicy.text(warning, p))
            assertTrue(NightWaveContrastPolicy.ratio(NightWaveNativeMattePolicy.text(warning, p), p.surface) >= 4.5)
        }
    }
    @Test fun nativeCanvasAndCardsUseTheActualThemeSurfaces() {
        for ((_, p) in palettes()) {
            assertEquals(p.background, NightWaveNativeMattePolicy.surface(0xFF0C131B, p))
            assertEquals(p.background, NightWaveNativeMattePolicy.surface(0xFF090D14, p))
            assertEquals(p.surface, NightWaveNativeMattePolicy.surface(0xFF172432, p))
        }
    }
    @Test fun bentoUsesActualContentWidthAndStacksAtLargeType() {
        for (width in listOf(320f, 360f, 412f)) {
            val content = width - NightWavePremiumDesignTokens.pageGutterDp(width) * 2
            assertEquals(if (width == 320f) 1 else 2, policy.bentoColumns(content, 1f))
            for (scale in listOf(1.3f, 2f, 3f)) assertEquals(1, policy.bentoColumns(content, scale))
        }
    }
    @Test fun invalidBentoMeasurementsChooseSafeReadingOrder() {
        for (width in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f)) assertEquals(1, policy.bentoColumns(width, 1f))
        for (scale in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f)) assertEquals(1, policy.bentoColumns(412f, scale))
    }
    @Test fun artworkEffectsRespectOldAndroidAndBoundedOpacity() {
        for (sdk in listOf(26, 28, 30)) assertFalse(policy.supportsBlur(sdk))
        for (sdk in listOf(31, 33, 36)) assertTrue(policy.supportsBlur(sdk))
        assertTrue(policy.backdropAlpha(true) <= .15f)
        assertTrue(policy.backdropAlpha(false) < policy.backdropAlpha(true))
        assertTrue(policy.BACKDROP_BITMAP_PX <= 128)
    }
    @Test fun pressReleaseCancelAndReducedMotionHaveDeterministicFeedback() {
        assertEquals(.95f, policy.pressScale(true, false), 0f)
        assertEquals(1f, policy.pressScale(false, false), 0f)
        assertEquals(1f, policy.pressScale(true, true), 0f)
        assertEquals(1f, policy.pressScale(false, true), 0f)
        assertEquals(0, policy.pressDuration(true))
        assertEquals(100, policy.pressDuration(false))
    }
    @Test fun thumbIsQuietAtRestButVisibleForAllActiveInputModes() {
        assertFalse(policy.showSeekThumb(false, false, false))
        for (input in listOf(Triple(true, false, false), Triple(false, true, false),
                Triple(false, false, true), Triple(true, true, true)))
            assertTrue(policy.showSeekThumb(input.first, input.second, input.third))
    }
    @Test fun persistedThemeNamesAndAccessibleControlGeometryArePreserved() {
        assertEquals(setOf("MIDNIGHT", "AURORA", "PEARL", "AMOLED"), NightWaveTheme.entries.map { it.name }.toSet())
        for (scale in listOf(1f, 2f, 3f)) assertTrue(NightWavePremiumDesignTokens.controlTouchTargetDp(scale) >= 48)
        for (width in listOf(320f, 360f, 412f)) assertEquals(16,
            NightWaveNowPlayingLayoutPolicy.resolve(width, 800f, 1f).artworkCornerDp)
    }
}
