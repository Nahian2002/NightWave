package com.nightwave.player.v2.lyrics

import java.util.Locale

/** Unsaved per-track editing buffer. No changes are written until explicit Save. */
data class LyricsEditorDraft(val cues: List<LyricCue>)

object LyricsEditorPolicy {
    const val MAX_TIME_MS = 99L * 60_000L + 59_999L
    const val MAX_TEXT_LENGTH = 220
    const val MAX_UNDO = 15
    const val NUDGE_MS = 250L
    const val FINE_NUDGE_MS = 50L
    private val timeCode = Regex("""^(\d{1,2}):([0-5]\d)\.(\d{1,3})$""")
    private val order = compareBy<LyricCue> { it.timeMs }.thenBy { it.text }

    fun begin(lyrics: TimedLyrics): LyricsEditorDraft =
        checked(lyrics.cues)

    fun timeLabel(ms: Long): String {
        require(ms in 0..MAX_TIME_MS) { "Timestamp is outside the supported LRC range" }
        val min = ms / 60_000L
        val sec = (ms / 1_000L) % 60L
        val milli = ms % 1_000L
        return "%02d:%02d.%03d".format(Locale.ROOT, min, sec, milli)
    }

    fun parseTime(input: String): Long {
        val m = timeCode.matchEntire(input.trim())
            ?: throw IllegalArgumentException("Use mm:ss.SSS, for example 01:23.500")
        val fraction = m.groupValues[3]
        val ms = when (fraction.length) {
            1 -> fraction.toLong() * 100L
            2 -> fraction.toLong() * 10L
            else -> fraction.toLong()
        }
        return (m.groupValues[1].toLong() * 60L +
            m.groupValues[2].toLong()) * 1_000L + ms
    }

    fun updateText(draft: LyricsEditorDraft, index: Int, text: String): LyricsEditorDraft {
        val newText = text.replace('\n', ' ').replace('\r', ' ').trim()
        require(newText.isNotEmpty() && newText.length <= MAX_TEXT_LENGTH) {
            "Each lyric line must contain 1–220 characters"
        }
        return modify(draft, index) { it.copy(text = newText) }
    }

    fun updateTime(draft: LyricsEditorDraft, index: Int, input: String): LyricsEditorDraft =
        modify(draft, index) { it.copy(timeMs = parseTime(input)) }

    fun nudge(draft: LyricsEditorDraft, index: Int, steps: Int,
        stepMs: Long = NUDGE_MS): LyricsEditorDraft {
        require(stepMs == NUDGE_MS || stepMs == FINE_NUDGE_MS) {
            "Unsupported lyric timing step"
        }
        return modify(draft, index) { cue ->
            cue.copy(timeMs = (cue.timeMs + steps.toLong() * stepMs)
                .coerceIn(0L, MAX_TIME_MS))
        }
    }

    fun add(draft: LyricsEditorDraft, playbackMs: Long): LyricsEditorDraft {
        require(draft.cues.size < TimedLyricsCodec.MAX_LINES) {
            "The LRC has reached its 1,200-line limit"
        }
        val at = playbackMs.coerceIn(0L, MAX_TIME_MS)
        return checked(draft.cues + LyricCue(at, "New lyric line"))
    }

    fun delete(draft: LyricsEditorDraft, index: Int): LyricsEditorDraft {
        require(index in draft.cues.indices) { "Lyric line no longer exists" }
        require(draft.cues.size > 1) { "The final lyric line cannot be removed" }
        return checked(draft.cues.filterIndexed { i, _ -> i != index })
    }

    fun serialize(draft: LyricsEditorDraft): ByteArray =
        validatedBytes(draft.cues)

    fun changed(baseline: LyricsEditorDraft, edited: LyricsEditorDraft): Boolean =
        baseline.cues != edited.cues

    private fun modify(
        draft: LyricsEditorDraft, index: Int, map: (LyricCue) -> LyricCue
    ): LyricsEditorDraft {
        require(index in draft.cues.indices) { "Lyric line no longer exists" }
        return checked(draft.cues.mapIndexed { i, cue ->
            if (i == index) map(cue) else cue
        })
    }

    private fun checked(cues: List<LyricCue>): LyricsEditorDraft {
        validatedBytes(cues)
        return LyricsEditorDraft(cues.sortedWith(order))
    }

    private fun validatedBytes(cues: List<LyricCue>): ByteArray {
        require(cues.isNotEmpty() && cues.size <= TimedLyricsCodec.MAX_LINES) {
            "Lyrics require 1–1,200 timed lines"
        }
        require(cues.all { cue ->
            cue.timeMs in 0..MAX_TIME_MS &&
                cue.text.isNotBlank() && cue.text.length <= MAX_TEXT_LENGTH &&
                !cue.text.contains('\n') && !cue.text.contains('\r')
        }) { "Lyric line or timestamp is invalid" }
        require(cues.distinct().size == cues.size) {
            "Identical timestamp and lyric text would be duplicated"
        }
        val ordered = cues.sortedWith(order)
        val text = ordered.joinToString("\n") {
            "[${timeLabel(it.timeMs)}] ${it.text}"
        }
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size in 1..TimedLyricsCodec.MAX_BYTES) {
            "The edited LRC exceeds the 64 KB safety limit"
        }
        // Protect every editor action from changing the original parser's
        // meaning (for example bracketed timestamps typed into lyric text).
        val parsed = TimedLyricsCodec.parse(bytes)
        require(parsed.cues == ordered) {
            "This lyric text contains an ambiguous timestamp. Edit the line."
        }
        return bytes
    }
}
