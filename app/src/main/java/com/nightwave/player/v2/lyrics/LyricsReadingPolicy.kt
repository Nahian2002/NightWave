package com.nightwave.player.v2.lyrics

/**
 * NW89 — cheap, accessible presentation for explicit user-imported LRC.
 * No transcription, network access, audio probes, persisted data or mutation.
 */
object LyricsReadingPolicy {
    const val MAX_ANIMATED_SCROLL_ROWS = 12

    /**
     * A far seek/skip should jump instead of running an animated lazy-list
     * scroll across hundreds of composed lyric rows. Near transitions keep
     * the restrained visual follow.
     */
    fun shouldAnimateScroll(currentFirstRow: Int, targetRow: Int, reduceMotion: Boolean): Boolean =
        !reduceMotion && currentFirstRow >= 0 && targetRow >= 0 &&
            kotlin.math.abs(currentFirstRow.toLong() - targetRow.toLong()) in
                1L..MAX_ANIMATED_SCROLL_ROWS.toLong()

    /** Describe the actual cue and offset-adjusted seek time to TalkBack. */
    fun cueDescription(cue: LyricCue, offsetMs: Int, durationMs: Long): String {
        val target = LyricsSyncPolicy.seekPosition(cue, offsetMs, durationMs)
        val totalSeconds = target / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val minuteUnit = if (minutes == 1L) "minute" else "minutes"
        val secondUnit = if (seconds == 1L) "second" else "seconds"
        return "Seek to $minutes $minuteUnit $seconds $secondUnit. " +
            cue.text.trim().replace(Regex("""\s+"""), " ").take(220)
    }

    fun currentLineState(current: Boolean): String =
        if (current) "Current synchronized lyric" else "Synchronized lyric"

    fun followHint(enabled: Boolean): String =
        if (enabled) "Following imported lyrics. Swipe to browse lines."
        else "Browsing lyrics. Select Follow to rejoin playback."

    /**
     * NW107: Follow is a real, reversible accessibility control. Leaving a
     * button disabled while follow was ON blocked TalkBack users from pausing
     * automatic scrolling without a touch gesture.
     */
    fun followActionLabel(following: Boolean): String =
        if (following) "Pause" else "Follow"

    fun followActionDescription(following: Boolean): String =
        if (following) "Pause automatic lyric scrolling"
        else "Resume automatic lyric scrolling"

    fun followStateDescription(following: Boolean): String =
        if (following) "Automatic lyric follow on"
        else "Automatic lyric follow off"

    fun toggledFollow(following: Boolean): Boolean = !following
}
