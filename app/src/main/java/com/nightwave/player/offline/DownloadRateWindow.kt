package com.nightwave.player.offline

/** Observed bytes only; restored bytes and render cadence never become speed. */
internal class DownloadRateWindow {
    private data class Sample(val bytes: Long, val at: Long)
    private val samples = ArrayDeque<Sample>()
    private var startedAt = -1L
    fun observe(bytes: Long, now: Long, restored: Long = 0L): Long? {
        val last = samples.lastOrNull()
        if (last == null || bytes < last.bytes || now < last.at ||
            last.bytes < restored || now - last.at > 8_000L) {
            samples.clear(); samples.add(Sample(bytes, now)); startedAt = now
            return null
        }
        if (bytes > last.bytes) samples.add(Sample(bytes, now))
        while (samples.size > 2 && now - samples.elementAt(1).at > 8_000L) samples.removeFirst()
        while (samples.size > 64) samples.removeFirst()
        val first = samples.first()
        val latest = samples.last()
        if (now - latest.at > 8_000L || latest.at - first.at < 3_000L) return null
        return OfflineTransferRatePolicy.bytesPerSecond(latest.bytes - first.bytes, latest.at - first.at)
    }
    fun canEstimate(now: Long): Boolean = startedAt >= 0L && now - startedAt >= 10_000L &&
        samples.lastOrNull()?.bytes?.minus(samples.first().bytes)?.let { it >= 65_536L } == true
}
