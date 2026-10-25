package com.nightwave.player.v2.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure test: no Android storage, music files, Activity or Media3 player. */
class LibraryUiRefreshPolicyTest {
    @Test fun onlyCurrentJobMayPublishUiSnapshot() {
        assertTrue(LibraryUiRefreshPolicy.accepts(1, 1, false, false))
        assertTrue(LibraryUiRefreshPolicy.accepts(90, 90, false, false))
        assertFalse(LibraryUiRefreshPolicy.accepts(1, 2, false, false))
        assertFalse(LibraryUiRefreshPolicy.accepts(2, 1, false, false))
        assertFalse(LibraryUiRefreshPolicy.accepts(-1, -1, false, false))
    }

    @Test fun stoppedOrDestroyedScreensRejectLateIoResults() {
        assertFalse(LibraryUiRefreshPolicy.accepts(5, 5, true, false))
        assertFalse(LibraryUiRefreshPolicy.accepts(5, 5, false, true))
        assertFalse(LibraryUiRefreshPolicy.accepts(5, 5, true, true))
    }
}
