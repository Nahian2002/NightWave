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

    @Test fun playbackAndSaveCannotResolveExtractedStreams() {
        assertThrows(IllegalStateException::class.java) { gateway.resolve(reference) }
        assertThrows(IllegalStateException::class.java) { gateway.resolveAudioOnlyForOffline(reference) }
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
