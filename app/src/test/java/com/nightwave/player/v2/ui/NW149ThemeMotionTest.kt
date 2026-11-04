package com.nightwave.player.v2.ui

import com.nightwave.player.v2.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class NW149ThemeMotionTest {
    @Test fun inverseActionsUseTheSelectedPaletteWithNormalTextContrast() {
        for (theme in NightWaveTheme.entries) {
            val p = NightWaveDesign.palette(theme)
            val action = NightWaveThemeMotionPolicy.inverseAction(p)
            assertEquals(p.accentSoft, action)
            assertTrue(NightWaveContrastPolicy.ratio(action, p.foreground) >= 4.5)
            if (p.isDark) assertTrue(NightWaveContrastPolicy.ratio(p.accent, p.foreground) < 4.5)
        }
    }
    @Test fun actualPaletteTextAndStatePairsMeetNormalTextContrast() {
        for (theme in NightWaveTheme.entries) {
            val p = NightWaveDesign.palette(theme)
            val pairs = listOf(
                Triple("body_canvas", p.foreground, p.background),
                Triple("body_card", p.foreground, p.surface),
                Triple("secondary_card", p.secondary, p.surface),
                Triple("secondary_raised", p.secondary, p.raised),
                Triple("action_raised", p.accent, p.raised),
                Triple("selected_primary", p.foreground, p.accentSoft),
                Triple("selected_secondary", p.foreground, p.secondarySoft),
                Triple("secondary_caption", p.secondary, p.secondarySoft),
                Triple("filled_primary", p.contrastOnAccent(), p.accent),
                Triple("filled_tertiary", p.contrastOnAccent(), p.secondaryAccent),
                Triple("error_text", p.danger, p.surface),
                Triple("error_action", p.background, p.danger),
                Triple("inverse_text", p.background, p.foreground),
                Triple("inverse_action", NightWaveThemeMotionPolicy.inverseAction(p), p.foreground)
            )
            for ((role, fg, bg) in pairs) {
                val ratio = NightWaveContrastPolicy.ratio(fg, bg)
                assertTrue("$theme $role $ratio", ratio >= 4.5)
                println(String.format(Locale.ROOT, "NW149_CONTRAST %s %s %.6f", theme, role, ratio))
            }
        }
    }
    @Test fun systemDisabledAndUnreadableScalesSuppressAllOptionalMotion() {
        for (scale in listOf(null, 0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            val reduced = NightWaveThemeMotionPolicy.reduceMotion(false, true, scale)
            assertTrue(reduced)
            assertFalse(NightWaveMotionPolicy.animateTabChange(0, 1, 4, reduced))
            assertFalse(NightWaveMotionPolicy.tabHaptic(0, 1, 4, reduced))
            assertEquals(0, NightWaveMotionPolicy.duration(reduced, NightWaveMotionPolicy.PLAYER_ENTER_MS))
            assertEquals(1f, NightWaveMotionPolicy.artworkScale(false, reduced))
        }
    }
    @Test fun validSystemScalesAndExplicitOverridesRetainTheirIntent() {
        for (scale in listOf(.1f, .5f, 1f, 10f, Float.MAX_VALUE))
            assertFalse(NightWaveThemeMotionPolicy.reduceMotion(false, true, scale))
        for (scale in listOf(null, 0f, 1f)) {
            assertTrue(NightWaveThemeMotionPolicy.reduceMotion(true, true, scale))
            assertTrue(NightWaveThemeMotionPolicy.reduceMotion(true, false, scale))
            assertFalse(NightWaveThemeMotionPolicy.reduceMotion(false, false, scale))
        }
    }
    @Test fun lifecycleSubscriptionIsIdempotentAndCannotPublishAfterStop() {
        var registrations = 0; var removals = 0; var reads = 0; var scale = 1f
        val values = mutableListOf<Float?>()
        val o = NightWaveMotionObservation({ reads++; scale }, { registrations++ }, { removals++ }, values::add)
        o.refresh(); assertEquals(0, reads)
        o.start(); o.start(); assertEquals(1, registrations); assertEquals(listOf(1f), values)
        scale = 0f; o.refresh(); assertEquals(listOf(1f, 0f), values)
        o.stop(); o.stop(); scale = .5f; o.refresh()
        assertEquals(1, removals); assertEquals(listOf(1f, 0f), values)
        o.start(); assertEquals(listOf(1f, 0f, .5f), values); o.stop()
        assertEquals(2, registrations); assertEquals(2, removals)
    }
    @Test fun foregroundReturnRereadsChangesMadeWhileStopped() {
        var scale = 1f; val values = mutableListOf<Float?>()
        val o = NightWaveMotionObservation({ scale }, {}, {}, values::add)
        repeat(100) {
            o.start(); o.stop(); scale = if (scale == 0f) 1f else 0f
            o.refresh()
        }
        assertEquals(100, values.size)
        assertEquals(listOf(1f, 0f, 1f, 0f), values.take(4))
    }
    @Test fun inaccessibleRegistrationFailsSafelyAndCanRetryOnNextStart() {
        var failed = true; var removals = 0; val values = mutableListOf<Float?>()
        val o = NightWaveMotionObservation({ 1f }, { if (failed) error("Unavailable") }, { removals++ }, values::add)
        o.start(); o.refresh(); assertEquals(listOf<Float?>(null), values); assertEquals(1, removals)
        failed = false; o.start(); assertEquals(listOf(null, 1f), values); o.stop(); assertEquals(2, removals)
    }
    @Test fun readAndCleanupFailuresDoNotEscapeOrPublishAfterDisposal() {
        val values = mutableListOf<Float?>()
        val o = NightWaveMotionObservation({ error("Unreadable") }, {}, { error("Already detached") }, values::add)
        o.start(); assertEquals(listOf<Float?>(null), values)
        o.stop(); o.refresh(); o.stop(); assertEquals(1, values.size)
    }
}
