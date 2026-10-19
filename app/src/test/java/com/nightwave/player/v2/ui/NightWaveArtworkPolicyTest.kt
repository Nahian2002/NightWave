package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightWaveArtworkPolicyTest {
    private val id = "dQw4w9WgXcQ"

    @Test fun upgradesKnownLowResYouTubeThumbnails() {
        assertEquals("https://i.ytimg.com/vi/$id/hqdefault.jpg",
            NightWaveArtworkPolicy.preferredArtwork(
                "https://i.ytimg.com/vi/$id/default.jpg"))
        assertEquals("https://i.ytimg.com/vi/$id/hqdefault.jpg",
            NightWaveArtworkPolicy.preferredArtwork(
                "https://img.youtube.com/vi/$id/mqdefault.jpg"))
    }

    @Test fun preservesHigherQualityAndUnrelatedArtwork() {
        for (source in listOf(
            "https://i.ytimg.com/vi/$id/hqdefault.jpg",
            "https://i.ytimg.com/vi/$id/maxresdefault.jpg",
            "https://art.example.org/album.jpg",
            "content://media/external/audio/27",
            "file:///storage/emulated/0/Music/cover.png",
            ""
        )) assertEquals(source, NightWaveArtworkPolicy.preferredArtwork(source))
    }

    @Test fun refusesExternalHostSpoofingAndInvalidIds() {
        for (url in listOf(
            "https://i.ytimg.com.attacker.test/vi/$id/default.jpg",
            "http://i.ytimg.com/vi/$id/default.jpg",
            "https://i.ytimg.com/vi/notvalid/default.jpg",
            "https://i.ytimg.com/vi/$id/default.jpg?token=secret",
            "invalid-uri"
        )) assertEquals(url, NightWaveArtworkPolicy.preferredArtwork(url))
    }

    @Test fun fallbackOnlyNeededWhenUrlWasActuallyUpgraded() {
        val original = "https://i.ytimg.com/vi/$id/default.jpg"
        assertTrue(NightWaveArtworkPolicy.allowOriginalFallback(
            original, NightWaveArtworkPolicy.preferredArtwork(original)))
        assertFalse(NightWaveArtworkPolicy.allowOriginalFallback(original, original))
        assertFalse(NightWaveArtworkPolicy.allowOriginalFallback("", ""))
    }
}
