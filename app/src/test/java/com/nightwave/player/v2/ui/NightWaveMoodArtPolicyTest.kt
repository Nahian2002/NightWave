package com.nightwave.player.v2.ui

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class NightWaveMoodArtPolicyTest {
    private val titles = listOf("After hours", "Deep focus", "Desi pulse", "Unplugged", "Momentum", "Slow mornings")
    @Test fun everyShortcutHasItsOwnCompleteMotif() {
        val motifs = titles.map(NightWaveMoodArtPolicy::motif)
        assertEquals(6, motifs.distinct().size)
        assertTrue(motifs.all { it.lines.isNotEmpty() || it.rings.isNotEmpty() })
    }
    @Test fun everyStrokeAndCircleFitsTheNormalizedPaintArea() {
        titles.forEach { title ->
            val motif = NightWaveMoodArtPolicy.motif(title)
            motif.lines.forEach { line ->
                listOf(line.x1, line.y1, line.x2, line.y2).forEach { assertTrue("$title: $it", it in .08f.. .92f) }
                assertTrue(line.strokeDp in 1f..4f)
            }
            motif.rings.forEach { ring ->
                assertTrue(ring.radius > 0f)
                assertTrue(ring.x - ring.radius >= .079f && ring.x + ring.radius <= .921f)
                assertTrue(ring.y - ring.radius >= .079f && ring.y + ring.radius <= .921f)
            }
        }
    }
    @Test fun paintViewportPreservesStrokeMarginForNarrowAndWideHeaders() {
        for (width in listOf(24f, 48f, 76f, 96f, 178f, 740f)) {
            for (height in listOf(24f, 56f, 72f)) {
                val v = requireNotNull(NightWaveMoodArtPolicy.viewport(width, height, 4f))
                assertTrue(v.left >= 4f && v.top >= 4f)
                assertTrue(v.left + v.side <= width - 4f)
                assertTrue(v.top + v.side <= height - 4f)
            }
        }
    }
    @Test fun invalidOrTinyPaintBoundsDrawNothing() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f, 8f)) {
            assertNull(NightWaveMoodArtPolicy.viewport(value, 56f, 4f))
            assertNull(NightWaveMoodArtPolicy.viewport(56f, value, 4f))
        }
        assertNull(NightWaveMoodArtPolicy.viewport(56f, 56f, -1f))
    }
    @Test fun titleMatchingIsStableAcrossDeviceLocales() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(NightWaveMoodArtPolicy.motif("Desi pulse"), NightWaveMoodArtPolicy.motif(" DESI PULSE "))
        } finally { Locale.setDefault(old) }
    }
    @Test fun unknownMoodKeepsTheSafeMorningFallback() {
        assertEquals(NightWaveMoodArtPolicy.motif("Slow mornings"), NightWaveMoodArtPolicy.motif("unknown"))
    }
}
