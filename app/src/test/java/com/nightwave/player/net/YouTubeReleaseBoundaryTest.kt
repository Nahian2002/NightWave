package com.nightwave.player.net

import com.nightwave.player.core.YouTubeReleasePolicy
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class YouTubeReleaseBoundaryTest {
    private val gateway = YouTubeGateway()
    private val reference = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

    @Test fun savingStaysBlockedAndNormalBuildsRejectPlayback() {
        assertThrows(IllegalStateException::class.java) { gateway.resolveAudioOnlyForOffline(reference) }
        if (YouTubeReleasePolicy.ownerPlaybackEnabled) {
            assertThrows(IllegalArgumentException::class.java) { gateway.resolve("https://example.com/watch") }
        } else {
            assertThrows(IllegalStateException::class.java) { gateway.resolve(reference) }
        }
    }

    @Test fun playbackLookupAllowsOnlyApprovedMethodsAndEndpoints() {
        assertTrue(allowsNewPipePlayback("https://youtubei.googleapis.com/youtubei/v1/player".toHttpUrl(), "POST"))
        assertTrue(allowsNewPipePlayback("https://www.youtube.com/youtubei/v1/next".toHttpUrl(), "POST"))
        assertTrue(allowsNewPipePlayback("https://www.youtube.com/s/player/abc/player_ias.vflset/en_US/base.js".toHttpUrl(), "GET"))
        assertFalse(allowsNewPipePlayback("https://www.youtube.com/youtubei/v1/search".toHttpUrl(), "POST"))
        assertFalse(allowsNewPipePlayback("https://www.youtube.com/youtubei/v1/player".toHttpUrl(), "GET"))
        assertFalse(allowsNewPipePlayback("https://user@www.youtube.com/youtubei/v1/player".toHttpUrl(), "POST"))
        assertFalse(allowsNewPipePlayback("https://www.youtube.com:8443/youtubei/v1/player".toHttpUrl(), "POST"))
    }

    @Test fun mediaEndpointRejectsNonAudioPathsAndOffsiteRedirectDestinations() {
        assertTrue(allowsYouTubeAudio("https://rr1.googlevideo.com/videoplayback?expire=123".toHttpUrl()))
        assertFalse(allowsYouTubeAudio("https://rr1.googlevideo.com/other".toHttpUrl()))
        assertFalse(allowsYouTubeAudio("http://rr1.googlevideo.com/videoplayback".toHttpUrl()))
        assertFalse(allowsYouTubeAudio("https://rr1.googlevideo.com.example.net/videoplayback".toHttpUrl()))
    }

    @Test fun privateSearchTransportAllowsMetadataAndRejectsMediaAndOffsiteUrls() {
        YouTubeReleasePolicy.requireUnofficialSearch()
        assertTrue(allowsNewPipeSearch("https://www.youtube.com/youtubei/v1/search".toHttpUrl()))
        assertTrue(allowsNewPipeSearch("https://www.youtube.com/youtubei/v1/guide".toHttpUrl()))
        assertFalse(allowsNewPipeSearch("https://www.youtube.com/youtubei/v1/player".toHttpUrl()))
        assertFalse(allowsNewPipeSearch("https://rr1.googlevideo.com/videoplayback".toHttpUrl()))
        assertFalse(allowsNewPipeSearch("https://www.youtube.com.example.net/results".toHttpUrl()))
        assertFalse(allowsNewPipeSearch("http://www.youtube.com/results".toHttpUrl()))
        assertFalse(allowsNewPipeSearch("https://www.youtube.com:8443/results".toHttpUrl()))
    }
}
