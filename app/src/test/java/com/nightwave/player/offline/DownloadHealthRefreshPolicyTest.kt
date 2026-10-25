package com.nightwave.player.offline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadHealthRefreshPolicyTest {
    @Test fun onlyLatestDiskHealthSnapshotMayUpdateScreen() {
        assertTrue(DownloadHealthRefreshPolicy.mayPublish(7L, 7L, false, false, false))
        assertFalse(DownloadHealthRefreshPolicy.mayPublish(6L, 7L, false, false, false))
        assertFalse(DownloadHealthRefreshPolicy.mayPublish(8L, 7L, false, false, false))
        assertFalse(DownloadHealthRefreshPolicy.mayPublish(0L, 0L, false, false, false))
    }

    @Test fun pausedOrDestroyedScreenRejectsLateFileResults() {
        assertFalse(DownloadHealthRefreshPolicy.mayPublish(4L, 4L, true, false, false))
        assertFalse(DownloadHealthRefreshPolicy.mayPublish(4L, 4L, false, true, false))
        assertFalse(DownloadHealthRefreshPolicy.mayPublish(4L, 4L, false, false, true))
    }

    @Test fun resumeMustUseNewGenerationRatherThanOldCachedRead() {
        assertFalse(DownloadHealthRefreshPolicy.mayPublish(12L, 13L, false, false, false))
        assertTrue(DownloadHealthRefreshPolicy.mayPublish(13L, 13L, false, false, false))
    }
}
