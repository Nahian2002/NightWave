package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM policy tests; no Android device, network, media or WorkManager needed. */
class DownloadHistoryPagePolicyTest {
    private val p = DownloadHistoryPagePolicy

    @Test fun pageCountHandlesBoundariesWithoutOverflow() {
        assertEquals(0, p.pageCount(-1))
        assertEquals(0, p.pageCount(0))
        assertEquals(1, p.pageCount(1))
        assertEquals(1, p.pageCount(60))
        assertEquals(2, p.pageCount(61))
        assertEquals(3, p.pageCount(121))
        assertTrue(p.pageCount(Int.MAX_VALUE) > 0)
    }

    @Test fun changingFiltersOrShrinkingHistoryClampsOutdatedPage() {
        assertEquals(0, p.normalizedPage(-100, 62))
        assertEquals(1, p.normalizedPage(200, 62))
        assertEquals(0, p.normalizedPage(5, 0))
        assertEquals(0, p.normalizedPage(Int.MAX_VALUE, 60))
        assertEquals(0, p.offset(0, 125))
        assertEquals(60, p.offset(1, 125))
        assertEquals(120, p.offset(99, 125))
    }

    @Test fun navigationCannotGoPastHistoryEnds() {
        assertFalse(p.hasPrevious(0, 125))
        assertTrue(p.hasNext(0, 125))
        assertTrue(p.hasPrevious(2, 125))
        assertFalse(p.hasNext(2, 125))
        assertFalse(p.hasNext(5, 0))
        assertFalse(p.hasPrevious(-4, 60))
    }

    @Test fun captionsAreAccurateForPartialFinalPage() {
        assertEquals("0 jobs", p.summary(12, 0))
        assertEquals("Showing 1–60 of 60 jobs", p.summary(0, 60))
        assertEquals("Showing 1–60 of 61 jobs", p.summary(0, 61))
        assertEquals("Showing 61–61 of 61 jobs", p.summary(1, 61))
        assertEquals("Showing 121–125 of 125 jobs", p.summary(22, 125))
    }
}
