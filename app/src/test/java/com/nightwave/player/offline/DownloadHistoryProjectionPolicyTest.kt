package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadHistoryProjectionPolicyTest {
    private val p = DownloadHistoryProjectionPolicy
    private val f = DownloadCenterViewPolicy.Filter

    private fun row(id: String, retryKey: String?, state: String,
                    available: Boolean = false, requested: Long = 0) =
        DownloadHistoryProjectionPolicy.Row(id, retryKey, state, available, requested)

    @Test fun laterFailedRetryCannotHideOlderVerifiedFileInReadyTab() {
        val history = listOf(
            row("older", "youtube-track-1", "SUCCEEDED", true, 100),
            row("newer", "youtube-track-1", "FAILED", false, 200)
        )
        assertEquals(listOf("newer"), p.select(history, f.ALL))
        assertEquals(listOf("older"), p.select(history, f.READY))
        assertEquals(listOf("newer"), p.select(history, f.ATTENTION))
        assertEquals(1, p.count(history, f.READY))
        assertEquals(1, p.count(history, f.ATTENTION))
    }

    @Test fun currentActiveAttemptDoesNotEraseValidEarlierSavedFile() {
        val history = listOf(
            row("prior", "youtube-track-1", "SUCCEEDED", true, 50),
            row("running", "youtube-track-1", "RUNNING", false, 90)
        )
        assertEquals(listOf("running"), p.select(history, f.ALL))
        assertEquals(listOf("prior"), p.select(history, f.READY))
        assertEquals(listOf("running"), p.select(history, f.ACTIVE))
    }

    @Test fun failuresWithSameSourceDoNotCreateDuplicateIssueRows() {
        val history = listOf(
            row("failed-1", "youtube-track-1", "FAILED", false, 1),
            row("failed-2", "youtube-track-1", "CANCELLED", false, 5),
            row("failed-3", "youtube-track-2", "FAILED", false, 3)
        )
        assertEquals(listOf("failed-2", "failed-3"), p.select(history, f.ATTENTION))
        assertEquals(2, p.count(history, f.ATTENTION))
    }

    @Test fun missingFileIsNotReadyEvenWhenWorkerSucceeded() {
        val history = listOf(row("missing", "youtube-track-1", "SUCCEEDED", false, 10))
        assertTrue(p.select(history, f.READY).isEmpty())
        assertEquals(listOf("missing"), p.select(history, f.ATTENTION))
    }

    @Test fun separateDirectAndCatalogRequestsKeepTheirOwnHistory() {
        val history = listOf(
            row("direct", null, "SUCCEEDED", true, 5),
            row("catalog", null, "SUCCEEDED", true, 5),
            row("untracked-old", null, "SUCCEEDED", true, 1)
        )
        assertEquals(listOf("catalog", "direct", "untracked-old"),
            p.select(history, f.READY))
    }

    @Test fun activeFirstThenTimestampThenIdIsDeterministic() {
        val history = listOf(
            row("z", null, "SUCCEEDED", true, 999),
            row("b", null, "BLOCKED", false, 20),
            row("a", null, "RUNNING", false, 20),
            row("c", null, "ENQUEUED", false, 30)
        )
        assertEquals(listOf("c", "a", "b", "z"), p.select(history, f.ALL))
    }

    @Test fun noHistoriesAndNoEligibleResultsStayEmpty() {
        assertTrue(p.select(emptyList(), f.ALL).isEmpty())
        assertEquals(0, p.count(emptyList(), f.ATTENTION))
        assertTrue(p.select(listOf(row("pending", null, "ENQUEUED")), f.READY).isEmpty())
    }
}
