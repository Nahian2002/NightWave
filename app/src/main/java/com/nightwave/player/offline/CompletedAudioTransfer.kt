package com.nightwave.player.offline

import java.util.Locale

/** Optional numeric receipt, never a readiness decision or a live ETA.
 * Bytes are this attempt's received wire bytes, excluding retained bytes.
 * Elapsed is monotonic wall time through transfer completion, before import.
 */
internal data class CompletedAudioTransfer(val receivedBytes: Long, val elapsedMs: Long) {
    fun label(): String? {
        // Treat absent, corrupt or implausible persisted values as unknown.
        if (receivedBytes !in 1..4_294_967_296L || elapsedMs !in 1..604_800_000L) return null
        val mbps = receivedBytes.toDouble() * 0.008 / elapsedMs.toDouble()
        return String.format(Locale.US, "Transfer %.2f s · %.2f Mbps avg",
            elapsedMs / 1000.0, mbps)
    }
}
