package com.nightwave.player.v2.lyrics

/** Stateless navigation shared by the editor UI and its JVM regressions. */
object LyricsEditorNavigationPolicy {
    fun clamp(index: Int, cueCount: Int): Int {
        require(cueCount in 1..TimedLyricsCodec.MAX_LINES) { "No valid lyric rows" }
        return index.coerceIn(0, cueCount - 1)
    }

    fun step(index: Int, cueCount: Int, change: Int): Int {
        require(cueCount in 1..TimedLyricsCodec.MAX_LINES) { "No valid lyric rows" }
        return (index.toLong() + change.toLong())
            .coerceIn(0L, cueCount.toLong() - 1L).toInt()
    }

    fun follow(draft: LyricsEditorDraft, positionMs: Long, offsetMs: Int): Int =
        LyricsSyncPolicy.activeIndex(TimedLyrics(draft.cues), positionMs, offsetMs)

    fun followOrKeep(
        draft: LyricsEditorDraft, positionMs: Long, offsetMs: Int, previous: Int
    ): Int {
        val target = follow(draft, positionMs, offsetMs)
        return if (target < 0) clamp(previous, draft.cues.size) else target
    }

    /** Follow the edited cue after a time change reorders the sorted LRC. */
    fun afterEdit(
        draft: LyricsEditorDraft, changedCue: LyricCue, previous: Int
    ): Int = draft.cues.indexOf(changedCue).takeIf { it >= 0 }
        ?: clamp(previous, draft.cues.size)
}
