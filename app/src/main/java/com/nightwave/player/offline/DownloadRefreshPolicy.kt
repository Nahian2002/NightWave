package com.nightwave.player.offline

/**
 * NW64 — bound how often rapid WorkManager progress callbacks reconstruct the
 * Downloads screen. This is presentation-only; the queue and WorkManager
 * state remain authoritative and are never throttled or mutated.
 */
internal object DownloadRefreshPolicy {
    const val MIN_UI_INTERVAL_MS = 400L

    /**
     * A first paint and monotonic clock reset are immediate. Otherwise defer
     * just enough to guarantee spacing between full UI rebuilds. Never
     * postpone the latest terminal WorkManager state indefinitely.
     */
    fun delayMs(lastPaintedAtMs: Long, nowMs: Long): Long {
        if (lastPaintedAtMs < 0L || nowMs < lastPaintedAtMs) return 0L
        val elapsed = nowMs - lastPaintedAtMs
        return if (elapsed >= MIN_UI_INTERVAL_MS) 0L
               else MIN_UI_INTERVAL_MS - elapsed
    }
}
