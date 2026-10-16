package com.nightwave.player.core

import org.junit.Assert.*
import org.junit.Test

class WebSearchRouteTest {
    @Test fun encodesRealSongNamesWithoutApiKeys() {
        val url = WebSearchRoute.searchUrl("  saiyara & baby shark  ")
        assertEquals("https://www.youtube.com/results?search_query=saiyara+%26+baby+shark", url)
    }
    @Test fun selectingYouTubeVideoRoutesIntoExistingPlayer() {
        val expected = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals(expected, WebSearchRoute.selectedVideo(
            "nightwave://play?url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DdQw4w9WgXcQ"
        ))
        assertEquals(expected, WebSearchRoute.selectedVideo(
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ"
        ))
        assertEquals(expected, WebSearchRoute.selectedVideo(
            "https://www.youtube.com/shorts/dQw4w9WgXcQ"
        ))
    }
    @Test fun blocksUnrelatedHostsAndDangerousUris() {
        assertNull(WebSearchRoute.selectedVideo(
            "nightwave://play?url=https%3A%2F%2Fevil.example%2Fwatch%3Fv%3DdQw4w9WgXcQ"
        ))
        assertFalse(WebSearchRoute.isAllowedWebsiteNavigation("https://youtube.com.fake.tld/results"))
        assertFalse(WebSearchRoute.isAllowedWebsiteNavigation("http://www.youtube.com/results"))
        assertFalse(WebSearchRoute.isAllowedWebsiteNavigation("javascript:alert(1)"))
        assertTrue(WebSearchRoute.isAllowedWebsiteNavigation("https://m.youtube.com/results?search_query=music"))
    }
}
