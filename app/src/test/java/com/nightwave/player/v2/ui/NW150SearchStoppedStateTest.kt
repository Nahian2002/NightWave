package com.nightwave.player.v2.ui

import com.nightwave.player.net.SearchMode
import com.nightwave.player.net.VideoPreview
import org.junit.Assert.*
import org.junit.Test

class NW150SearchStoppedStateTest {
    private val videos = listOf(
        VideoPreview("Same title", "Fixture", "https://www.youtube.com/watch?v=00000000001", "", 120),
        VideoPreview("Same title (Live)", "Fixture", "https://www.youtube.com/watch?v=00000000002", "", 125))
    @Test fun cancelPreservesVersionsUrlsModeQueryAndDiagnostics() {
        val before = StudioSearchSnapshot(query = "same title", mode = SearchMode.VIDEOS, loading = true,
            videos = videos, provider = "Fixture", details = "Numeric phases", fromCache = true)
        val after = before.stop(SearchStopReason.CANCELLED)
        assertFalse(after.loading); assertEquals("", after.error)
        assertEquals(SearchStopReason.CANCELLED, after.stopped)
        assertEquals(before, after.copy(loading = true, stopped = null))
        assertEquals(videos.map { it.url }, after.videos.map { it.url })
    }
    @Test fun backgroundPauseIsDistinctFromFailureAndCancel() {
        val after = StudioSearchSnapshot(query = "fixture", loading = true).stop(SearchStopReason.BACKGROUNDED)
        assertEquals(SearchStopReason.BACKGROUNDED, after.stopped)
        assertTrue(after.error.isEmpty()); assertTrue(after.videos.isEmpty()); assertFalse(after.loading)
    }
    @Test fun alreadyCompletedOrFailedRequestIsNeverReclassifiedByStop() {
        for (before in listOf(StudioSearchSnapshot(videos = videos), StudioSearchSnapshot(error = "Failure"))) {
            assertSame(before, before.stop(SearchStopReason.CANCELLED))
            assertSame(before, before.stop(SearchStopReason.BACKGROUNDED))
        }
    }
    @Test fun retryStartsWithNoStoppedState() {
        val stopped = StudioSearchSnapshot(loading = true).stop(SearchStopReason.CANCELLED)
        val retry = StudioSearchSnapshot(query = stopped.query, loading = true, videos = stopped.videos)
        assertNull(retry.stopped); assertTrue(retry.loading)
    }
}
