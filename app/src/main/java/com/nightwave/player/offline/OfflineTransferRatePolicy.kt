package com.nightwave.player.offline

/** UI-only estimate derived from actual WorkManager bytes received over time. */
object OfflineTransferRatePolicy {
    fun bytesPerSecond(deltaBytes: Long, elapsedMs: Long): Long? {
        if (deltaBytes <= 0L || elapsedMs < 250L || elapsedMs > 60_000L) return null
        return ((deltaBytes.toDouble() * 1000.0) / elapsedMs.toDouble())
            .toLong().coerceAtLeast(1L)
    }

    fun display(rate: Long?): String {
        if (rate == null || rate <= 0L) return "Speed calculating"
        return when {
            rate >= 1024L * 1024L -> "%.1f MB/s".format(java.util.Locale.US,
                rate.toDouble() / (1024.0 * 1024.0))
            else -> "%d KB/s".format(java.util.Locale.US,
                (rate / 1024L).coerceAtLeast(1L))
        }
    }

    fun etaSeconds(received: Long, expected: Long, rate: Long?): Long? {
        if (rate == null || rate <= 0L || expected <= received ||
            received < 0L || expected <= 0L) return null
        return (expected - received) / rate
    }
}
