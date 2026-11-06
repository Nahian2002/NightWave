package com.nightwave.player.offline

/**
 * NW48 display-only status policy. WorkManager state and a verified local file
 * are separate facts: SUCCEEDED must never by itself mean "ready offline".
 * No persisted library/audio state is read or written here.
 */
object DownloadCenterViewPolicy {
    enum class Filter { ALL, ACTIVE, READY, ATTENTION }

    fun isActive(workState: String): Boolean = when (workState) {
        "ENQUEUED", "RUNNING", "BLOCKED" -> true
        else -> false
    }

    /** Network bytes received is not the same as playable indexed audio. */
    fun isVerifying(workState: String, verifyingFlag: Boolean): Boolean =
        workState == "RUNNING" && verifyingFlag

    fun isReady(workState: String, availableFile: Boolean): Boolean =
        workState == "SUCCEEDED" && availableFile

    fun needsAttention(workState: String, availableFile: Boolean): Boolean =
        workState == "FAILED" || workState == "CANCELLED" ||
            (workState == "SUCCEEDED" && !availableFile)

    fun matches(filter: Filter, workState: String, availableFile: Boolean): Boolean =
        when (filter) {
            Filter.ALL -> true
            Filter.ACTIVE -> isActive(workState)
            Filter.READY -> isReady(workState, availableFile)
            Filter.ATTENTION -> needsAttention(workState, availableFile)
        }

    /** Double avoids overflowing 100 * received on large/corrupt counters. */
    fun percent(receivedBytes: Long, expectedBytes: Long): Int? {
        if (expectedBytes <= 0L || receivedBytes < 0L) return null
        return (receivedBytes.toDouble() * 100.0 / expectedBytes.toDouble())
            .coerceIn(0.0, 100.0).toInt()
    }

    /** Hide stale throughput/ETAs rather than presenting a frozen speed. */
    fun recentRate(rate: Long?, lastByteChangeMs: Long?, nowMs: Long): Long? {
        if (rate == null || rate <= 0L || lastByteChangeMs == null ||
            nowMs < lastByteChangeMs || nowMs - lastByteChangeMs > 8_000L)
            return null
        return rate
    }

    fun label(filter: Filter): String = when (filter) {
        Filter.ALL -> "All"
        Filter.ACTIVE -> "Active"
        Filter.READY -> "Ready"
        Filter.ATTENTION -> "Issues"
    }

    fun emptyMessage(filter: Filter): String = when (filter) {
        Filter.ALL -> "No downloads to show. Find saved music in Offline music."
        Filter.ACTIVE -> "No active downloads. Start one from Find audio or Audio link."
        Filter.READY -> "No downloads are ready offline in this view. Check Offline music."
        Filter.ATTENTION -> "No downloads need attention in this view."
    }
}
