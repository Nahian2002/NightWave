package com.nightwave.player.v2.lyrics

/**
 * On-device lyric timing controls for user-imported offline LRC.
 * A positive offset delays the displayed line relative to playback.
 */
object LyricsSyncPolicy {
    const val MAX_OFFSET_MS = 10_000
    const val STEP_MS = 250

    fun validOffset(ms: Int): Boolean =
        ms in -MAX_OFFSET_MS..MAX_OFFSET_MS && ms % STEP_MS == 0

    fun adjust(ms: Int, steps: Int): Int =
        (ms.toLong() + steps.toLong() * STEP_MS)
            .coerceIn(-MAX_OFFSET_MS.toLong(), MAX_OFFSET_MS.toLong())
            .toInt()

    fun activeIndex(lyrics: TimedLyrics, positionMs: Long, offsetMs: Int): Int {
        require(validOffset(offsetMs)) { "Unsupported lyric offset" }
        // Clamp impossible clock values before subtracting a negative offset.
        // Do not clamp the result to zero: silence before the first cue is -1.
        val clock = positionMs.coerceIn(0L, Long.MAX_VALUE - MAX_OFFSET_MS)
        return lyrics.activeIndex(clock - offsetMs)
    }

    fun seekPosition(cue: LyricCue, offsetMs: Int, durationMs: Long): Long {
        require(validOffset(offsetMs)) { "Unsupported lyric offset" }
        val candidate = (cue.timeMs + offsetMs.toLong()).coerceAtLeast(0L)
        return if (durationMs > 0L) candidate.coerceAtMost(durationMs) else candidate
    }

    fun label(ms: Int): String {
        require(validOffset(ms)) { "Unsupported lyric offset" }
        return if (ms == 0) "0.00s" else
            (if (ms > 0) "+" else "-") +
                "%d.%02ds".format(java.util.Locale.ROOT,
                    kotlin.math.abs(ms) / 1000, kotlin.math.abs(ms) % 1000 / 10)
    }
}
