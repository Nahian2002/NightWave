package com.nightwave.player.net

import org.junit.Assert.assertThrows
import org.junit.Test

class YouTubeReleaseBoundaryTest {
    private val gateway = YouTubeGateway()
    private val reference = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

    @Test fun playbackAndSaveCannotResolveExtractedStreams() {
        assertThrows(IllegalStateException::class.java) { gateway.resolve(reference) }
        assertThrows(IllegalStateException::class.java) { gateway.resolveAudioOnlyForOffline(reference) }
    }

    @Test fun oldSearchGatewayCannotContactExtractorsOrProxies() {
        assertThrows(IllegalStateException::class.java) { gateway.search("music", SearchMode.SONGS) }
    }
}
