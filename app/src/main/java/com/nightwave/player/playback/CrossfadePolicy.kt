package com.nightwave.player.playback

/**
 * A user-opted local audio crossfade. Zero means strict Media3 gapless playback.
 * Linear complementary gains avoid output amplification during the overlap.
 */
object CrossfadePolicy {
    const val DEFAULT_SECONDS = 0
    const val MAX_SECONDS = 10

    fun accepts(seconds: Int): Boolean = seconds in 0..MAX_SECONDS

    /** Both overlapping decks must pass through the same requested session EQ. */
    fun compatibleEffectSession(requestedPreset: Int, outgoingSession: Int, incomingSession: Int): Boolean =
        requestedPreset == 0 || (outgoingSession > 0 && incomingSession == outgoingSession)

    fun canPrepare(
        seconds: Int, offline: Boolean, gaplessTimeline: Boolean,
        normalSpeed: Boolean, index: Int, count: Int,
        durationMs: Long, positionMs: Long
    ): Boolean {
        if (seconds !in 1..MAX_SECONDS || !offline || !gaplessTimeline ||
            !normalSpeed || index < 0 || index + 1 >= count) return false
        val overlapMs = seconds * 1_000L
        return durationMs > overlapMs + 1_000L &&
            positionMs >= 0L && positionMs < durationMs
    }

    fun startThresholdMs(seconds: Int): Long = (seconds.coerceIn(1, MAX_SECONDS) * 1_000L)
    fun progress(elapsedMs: Long, seconds: Int): Float =
        if (seconds !in 1..MAX_SECONDS) 0f
        else (elapsedMs.toFloat() / (seconds * 1_000f)).coerceIn(0f, 1f)

    /** Index 0 = outgoing, index 1 = incoming. Never increases master gain. */
    fun gains(progress: Float, master: Float): Pair<Float, Float> {
        val t = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
        val gain = if (master.isFinite()) master.coerceIn(0f, 1f) else 0f
        return gain * (1f - t) to gain * t
    }
}
