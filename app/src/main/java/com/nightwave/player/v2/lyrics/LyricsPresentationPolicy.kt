package com.nightwave.player.v2.lyrics

/**
 * Display-only helpers for user-imported, timestamped LRC. They do not infer
 * word-level karaoke timing or modify the saved source / player timeline.
 */
object LyricsPresentationPolicy {
    /**
     * Leave some preceding context above the currently highlighted cue.
     * A tap-to-seek/auto-follow uses the same stable scroll anchor.
     */
    fun scrollTarget(activeIndex: Int, contextLines: Int = 2): Int =
        (activeIndex - contextLines.coerceIn(0, 5)).coerceAtLeast(0)

    /**
     * Elapsed fraction between this cue and the next distinct timestamp.
     * No false word-by-word synchronization: a progress bar is line-level only.
     * Last cue stays fully active. Before the cue begins, progress is zero.
     */
    fun lineProgress(
        lyrics: TimedLyrics, activeIndex: Int, positionMs: Long, offsetMs: Int
    ): Float {
        require(LyricsSyncPolicy.validOffset(offsetMs)) { "Unsupported lyric offset" }
        if (activeIndex !in lyrics.cues.indices) return 0f
        val start = lyrics.cues[activeIndex].timeMs
        val next = lyrics.cues.drop(activeIndex + 1)
            .firstOrNull { it.timeMs > start }?.timeMs ?: return 1f
        val clock = positionMs.coerceIn(0L, Long.MAX_VALUE - LyricsSyncPolicy.MAX_OFFSET_MS) -
            offsetMs.toLong()
        if (clock <= start) return 0f
        val span = (next - start).coerceAtLeast(1L)
        return ((clock - start).toDouble() / span.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }
}
