package com.nightwave.player.playback

/**
 * A time-limited sleep timer independent of song/queue identities.
 * Uses Android elapsedRealtime supplied by the caller (monotonic clock).
 * It never reads libraries, network data or listening history.
 */
object SleepTimerPolicy {
    val choicesMinutes = listOf(15, 30, 45, 60)

    fun deadlineMs(nowMs: Long, minutes: Int): Long? {
        if (minutes !in choicesMinutes || nowMs < 0L) return null
        val duration = minutes.toLong() * 60_000L
        return if (nowMs <= Long.MAX_VALUE - duration) nowMs + duration else null
    }

    fun remainingMs(nowMs: Long, deadlineMs: Long): Long =
        if (nowMs < 0L || deadlineMs <= 0L) 0L
        else (deadlineMs - nowMs).coerceAtLeast(0L)

    fun expires(nowMs: Long, deadlineMs: Long): Boolean =
        deadlineMs > 0L && nowMs >= deadlineMs

    fun label(remainingMs: Long): String {
        if (remainingMs <= 0L) return "Sleep timer"
        val totalMinutes = (remainingMs + 59_999L) / 60_000L
        return "Sleep in " + totalMinutes + " min"
    }
}
