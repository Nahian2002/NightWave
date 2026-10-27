package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadCenterDashboardPolicyTest {
    private val policy = DownloadCenterDashboardPolicy

    @Test fun readyAndIssuesAreIndeterminateDuringVerification() {
        assertEquals("Ready · …", policy.tab(DownloadCenterViewPolicy.Filter.READY, 0, true))
        assertEquals("Issues · …", policy.tab(DownloadCenterViewPolicy.Filter.ATTENTION, 0, true))
        assertEquals("Active · 4", policy.tab(DownloadCenterViewPolicy.Filter.ACTIVE, 4, true))
        assertEquals("All · 19", policy.tab(DownloadCenterViewPolicy.Filter.ALL, 19, true))
    }

    @Test fun historyTabsBoundCountsWithoutHidingRecords() {
        assertEquals("All · 99+", policy.tab(DownloadCenterViewPolicy.Filter.ALL, 12_000, false))
        assertEquals("Ready · 0", policy.tab(DownloadCenterViewPolicy.Filter.READY, 0, false))
        assertEquals("Issues", policy.tab(DownloadCenterViewPolicy.Filter.ATTENTION, -1, false))
        assertEquals("Active", policy.tab(DownloadCenterViewPolicy.Filter.ACTIVE, null, false))
    }

    @Test fun transferOverviewSeparatesVerifiedAudioAndCurrentWork() {
        assertEquals("2 active · 4 ready offline · 1 need attention · 10 transfers",
            policy.overview(2, 4, 1, 10, false, false))
        assertEquals("Checking saved audio · 2 active · 10 transfer entries",
            policy.overview(2, 0, 0, 10, true, false))
        assertTrue(policy.overview(2, 0, 0, 10, false, true)
            .contains("verification unavailable"))
        assertFalse(policy.overview(2, 0, 0, 10, false, true).contains("0 ready"))
    }

    @Test fun sourceTruthDoesNotClaimByteResumeOrDeleteOnHide() {
        val guidance = policy.helpLine()
        assertTrue(guidance.contains("restarts from zero"))
        assertTrue(guidance.contains("not its offline file"))
        assertFalse(guidance.contains("partial transfer preserved"))
    }
}
