package com.nightwave.player.v2.lyrics

import java.text.Normalizer
import java.util.Locale

/**
 * Local, non-persistent search over the current unsaved LRC draft.
 * Returns cue indices, not transformed or exported lyric content.
 */
object LyricsEditorSearchPolicy {
    const val MAX_QUERY_LENGTH = 80

    private val whitespace = Regex("""\s+""")

    private fun normalized(value: String): String =
        whitespace.replace(
            Normalizer.normalize(value, Normalizer.Form.NFC)
                .lowercase(Locale.ROOT).trim(),
            " "
        )

    /**
     * Empty query intentionally means no highlighted matches.
     * The result indexes always refer to the current sorted draft,
     * so editing, undo and timestamp reordering cannot use stale indexes.
     */
    fun matches(draft: LyricsEditorDraft, query: String): List<Int> {
        require(query.length <= MAX_QUERY_LENGTH) { "Search is limited to 80 characters" }
        val needle = normalized(query)
        if (needle.isEmpty()) return emptyList()
        val searchTime = ':' in needle
        return draft.cues.mapIndexedNotNull { index, cue ->
            val found = normalized(cue.text).contains(needle) ||
                (searchTime && LyricsEditorPolicy.timeLabel(cue.timeMs).contains(needle))
            index.takeIf { found }
        }
    }

    /**
     * Strict next/previous match navigation with wrapping.
     * Null on no matches; no scrolling or player seeking is performed here.
     */
    fun nearest(indices: List<Int>, selectedIndex: Int, forward: Boolean): Int? {
        if (indices.isEmpty()) return null
        require(indices.zipWithNext().all { (a, b) -> a < b } &&
            indices.first() >= 0) { "Match indices must be ascending and unique" }
        return if (forward) {
            indices.firstOrNull { it > selectedIndex } ?: indices.first()
        } else {
            indices.lastOrNull { it < selectedIndex } ?: indices.last()
        }
    }

    fun ordinal(indices: List<Int>, selectedIndex: Int): Int? =
        indices.indexOf(selectedIndex).takeIf { it >= 0 }?.plus(1)
}
