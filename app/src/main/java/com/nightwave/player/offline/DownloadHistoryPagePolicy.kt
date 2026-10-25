package com.nightwave.player.offline

/**
 * NW63: bounded, deterministic browsing of WorkManager history.
 * The full queue remains stored by WorkManager; only PAGE_SIZE cards are
 * inflated for one page. No audio, cloud state or work records are mutated.
 */
internal object DownloadHistoryPagePolicy {
    const val PAGE_SIZE = 60

    fun pageCount(matchCount: Int): Int =
        if (matchCount <= 0) 0 else 1 + (matchCount - 1) / PAGE_SIZE

    fun normalizedPage(requestedPage: Int, matchCount: Int): Int =
        requestedPage.coerceIn(0, (pageCount(matchCount) - 1).coerceAtLeast(0))

    fun offset(page: Int, matchCount: Int): Int =
        normalizedPage(page, matchCount) * PAGE_SIZE

    fun hasPrevious(page: Int, matchCount: Int): Boolean =
        normalizedPage(page, matchCount) > 0

    fun hasNext(page: Int, matchCount: Int): Boolean =
        normalizedPage(page, matchCount) + 1 < pageCount(matchCount)

    fun summary(page: Int, matchCount: Int): String {
        if (matchCount <= 0) return "0 jobs"
        val start = offset(page, matchCount) + 1
        val end = minOf(start + PAGE_SIZE - 1, matchCount)
        return "Showing $start–$end of $matchCount jobs"
    }
}
