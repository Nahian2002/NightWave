package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineLibraryCapacityTest {
    private val mib = 1024L * 1024L

    @Test fun preservesLegacySizeAndExpandsWithoutUnlimitedIndex() {
        assertTrue(OfflineLibraryCapacity.MAX_ITEMS > 400)
        assertTrue(OfflineLibraryCapacity.MAX_LIBRARY_BYTES > 3L * 1024L * 1024L * 1024L)
        assertTrue(OfflineLibraryCapacity.acceptsNew(400, 3L * 1024L * 1024L * 1024L, 2 * mib))
        assertTrue(OfflineLibraryCapacity.acceptsNew(4999, 0L, mib))
        assertFalse(OfflineLibraryCapacity.acceptsNew(5000, 0L, mib))
        assertFalse(OfflineLibraryCapacity.acceptsNew(5001, 0L, mib))
        assertFalse(OfflineLibraryCapacity.acceptsNew(-1, 0L, mib))
        assertFalse(OfflineLibraryCapacity.acceptsNew(1, -1L, mib))
    }

    @Test fun limitsCannotOverflowWhenNearMaxCapacity() {
        val max = OfflineLibraryCapacity.MAX_LIBRARY_BYTES
        assertTrue(OfflineLibraryCapacity.acceptsNew(12, max - mib, mib))
        assertFalse(OfflineLibraryCapacity.acceptsNew(12, max - mib, mib + 1))
        assertFalse(OfflineLibraryCapacity.acceptsNew(12, max, mib))
        assertFalse(OfflineLibraryCapacity.acceptsNew(12, Long.MAX_VALUE, Long.MAX_VALUE))
        assertFalse(OfflineLibraryCapacity.acceptsNew(12, 12L, -1L))
        assertFalse(OfflineLibraryCapacity.acceptsNew(12, 12L, 0L))
    }

    @Test fun sourceAndIndexBounds() {
        assertTrue(OfflineLibraryCapacity.canStage(40 * mib, 20 * mib))
        assertFalse(OfflineLibraryCapacity.canStage(20 * mib, 20 * mib))
        assertFalse(OfflineLibraryCapacity.canStage(-1, mib))
        assertTrue(OfflineLibraryCapacity.safeIndex("[]"))
        assertTrue(OfflineLibraryCapacity.safeIndex("x".repeat(OfflineLibraryCapacity.MAX_INDEX_CHARS)))
        assertFalse(OfflineLibraryCapacity.safeIndex("x".repeat(OfflineLibraryCapacity.MAX_INDEX_CHARS + 1)))
    }

    @Test fun pagesCoverEveryTrackWithoutOverflow() {
        assertEquals(1, OfflineLibraryCapacity.pageCount(0))
        assertEquals(1, OfflineLibraryCapacity.pageCount(60))
        assertEquals(2, OfflineLibraryCapacity.pageCount(61))
        assertEquals(84, OfflineLibraryCapacity.pageCount(5000))
        assertEquals(0, OfflineLibraryCapacity.safePage(-10, 5000))
        assertEquals(83, OfflineLibraryCapacity.safePage(1000, 5000))
        assertEquals(0, OfflineLibraryCapacity.safePage(100, 0))
    }
}
