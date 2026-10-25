package com.nightwave.player.playback

/**
 * NW51: listening insights are optional telemetry, never on the critical
 * audio path. A permanently stalled storage write cannot create an unbounded
 * executor backlog. Overflow drops an optional pulse, not music or consent.
 */
internal object ListeningDispatchPolicy {
    const val MAX_PENDING_WRITES = 32
    const val WARN_INTERVAL_MS = 60_000L

    fun shouldWarn(nowElapsedMs: Long, lastWarningElapsedMs: Long): Boolean {
        if (nowElapsedMs < 0L || lastWarningElapsedMs < 0L ||
            nowElapsedMs < lastWarningElapsedMs) return false
        return lastWarningElapsedMs == 0L ||
            nowElapsedMs - lastWarningElapsedMs >= WARN_INTERVAL_MS
    }
}
