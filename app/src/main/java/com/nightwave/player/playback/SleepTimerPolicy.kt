package com.nightwave.player.playback

/**
 * A time-limited sleep timer independent of song/queue identities.
 * Uses Android elapsedRealtime supplied by the caller (monotonic clock).
 * It never reads libraries, network data or listening history.
 */
object SleepTimerPolicy {
    const val MIN_MINUTES = 1
    const val MAX_MINUTES = 24 * 60
    const val DIAL_MAX_MINUTES = 120
    val choicesMinutes = listOf(15, 30, 45, 60)

    fun accepts(minutes: Int): Boolean = minutes in MIN_MINUTES..MAX_MINUTES

    fun deadlineMs(nowMs: Long, minutes: Int): Long? {
        if (!accepts(minutes) || nowMs < 0L) return null
        val duration = minutes.toLong() * 60_000L
        return if (nowMs <= Long.MAX_VALUE - duration) nowMs + duration else null
    }

    /** The dial maps one complete revolution to 1..120 minutes, clockwise from top. */
    fun dialMinutes(x: Float, y: Float, width: Float, height: Float): Int {
        if (!x.isFinite() || !y.isFinite() || !width.isFinite() || !height.isFinite() ||
            width <= 0f || height <= 0f) return MIN_MINUTES
        val angle = Math.toDegrees(Math.atan2(
            (y - height / 2f).toDouble(), (x - width / 2f).toDouble()))
        val fromTop = (angle + 90.0 + 360.0) % 360.0
        val minute = Math.round(fromTop * DIAL_MAX_MINUTES / 360.0).toInt()
        return if (minute == 0) DIAL_MAX_MINUTES else minute.coerceIn(1, DIAL_MAX_MINUTES)
    }

    fun remainingMs(nowMs: Long, deadlineMs: Long): Long =
        if (nowMs < 0L || deadlineMs <= 0L) 0L
        else (deadlineMs - nowMs).coerceAtLeast(0L)

    fun expires(nowMs: Long, deadlineMs: Long): Boolean =
        deadlineMs > 0L && nowMs >= deadlineMs

    fun label(remainingMs: Long): String {
        if (remainingMs <= 0L) return "Sleep timer"
        val totalMinutes = ((remainingMs - 1L) / 60_000L) + 1L
        return "Sleep in " + totalMinutes + " min"
    }
}
