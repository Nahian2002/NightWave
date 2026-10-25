package com.nightwave.player.offline

import java.util.Locale

/** Small truthful summary: only health-checked, playable imported audio is counted. */
object OfflineStorageSummary {
    fun size(bytes: Long): String {
        if (bytes < 0L) return "Unknown"
        val mb = 1024.0 * 1024.0
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / mb)
            else -> String.format(Locale.US, "%.1f GB", bytes / (mb * 1024.0))
        }
    }

    fun label(playableCount: Int, playableBytes: Long, freeBytes: Long,
        attentionCount: Int): String {
        val available = playableCount.coerceAtLeast(0)
        val used = size(playableBytes.coerceAtLeast(0))
        val free = if (freeBytes < 0) "" else " · ${size(freeBytes)} device free"
        val attention = if (attentionCount <= 0) "" else
            " · $attentionCount entries need attention"
        return "$available playable · $used imported audio$free$attention"
    }
}
