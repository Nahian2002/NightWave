package com.nightwave.player.offline

/** UI-only estimate derived from actual WorkManager bytes received over time. */
object OfflineTransferRatePolicy {
    /** Binary byte units plus decimal network units; no advertised line rate. */
    fun preciseDisplay(rate: Long?): String {
        val bytes = display(rate).replace(" MB/s", " MiB/s").replace(" KB/s", " KiB/s")
        return if (rate == null || rate <= 0) bytes else bytes + " · " +
            "%.2f Mbps".format(java.util.Locale.US, rate.toDouble() * 8.0 / 1_000_000.0)
    }
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
            rate < 1024L -> "$rate B/s"
            rate < 10L * 1024L -> "%.1f KB/s".format(java.util.Locale.US,
                rate.toDouble() / 1024.0)
            else -> "%d KB/s".format(java.util.Locale.US,
                rate / 1024L)
        }
    }

    fun etaSeconds(received: Long, expected: Long, rate: Long?): Long? {
        if (rate == null || rate <= 0L || expected <= received ||
            received < 0L || expected <= 0L) return null
        val remaining = expected - received
        // Round upward: a nonzero remaining byte count is never "0s left".
        return (remaining / rate) + if (remaining % rate > 0L) 1L else 0L
    }
}
