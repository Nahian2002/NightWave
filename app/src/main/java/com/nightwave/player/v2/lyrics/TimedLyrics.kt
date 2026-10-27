package com.nightwave.player.v2.lyrics

/** User-provided LRC only. No online lyrics fetching or embedded publisher catalog. */
data class LyricCue(val timeMs: Long, val text: String)
data class TimedLyrics(val cues: List<LyricCue>) {
    fun activeIndex(positionMs: Long): Int {
        if (cues.isEmpty() || positionMs < cues.first().timeMs) return -1
        var lo = 0
        var hi = cues.lastIndex
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (cues[mid].timeMs <= positionMs) lo = mid + 1 else hi = mid - 1
        }
        return hi
    }
}

/** Strict, bounded parser for imported [mm:ss.xx] synchronized LRC. */
object TimedLyricsCodec {
    const val MAX_BYTES = 64 * 1024
    const val MAX_LINES = 1200
    private val timestamp = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]""")

    /** A time embedded in metadata/prose is never a synchronized lyric. */
    internal fun rowCues(line: String): List<LyricCue> {
        val trimmed = line.trim()
        val times = mutableListOf<Long>()
        var end = 0
        while (end < trimmed.length) {
            val match = timestamp.find(trimmed, end)
                ?.takeIf { it.range.first == end } ?: break
            val sec = match.groupValues[2].toInt()
            if (sec > 59) return emptyList()
            val fraction = match.groupValues[3].padEnd(3, '0').ifEmpty { "000" }
            times += (match.groupValues[1].toLong() * 60L + sec) * 1000L +
                fraction.toLong()
            end = match.range.last + 1
        }
        if (times.isEmpty()) return emptyList()
        val words = trimmed.substring(end).trim()
        // Reject ambiguous rows instead of creating phantom cues or silently
        // deleting bracketed times from the user's words.
        if (words.isBlank() || timestamp.containsMatchIn(words) ||
            Regex("""^\[\d""").containsMatchIn(words)) return emptyList()
        require(words.length <= 220) { "Each lyric line must contain 1–220 characters" }
        return times.map { LyricCue(it, words) }
    }

    fun parse(bytes: ByteArray): TimedLyrics {
        require(bytes.size in 1..MAX_BYTES) { "Lyrics file exceeds 64 KB" }
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        val text = decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            .removePrefix("\uFEFF")
        val cues = mutableListOf<LyricCue>()
        for (line in text.lineSequence()) {
            cues += rowCues(line)
            require(cues.size <= MAX_LINES) { "Lyrics file has too many timed lines" }
        }
        require(cues.isNotEmpty()) { "No synchronized LRC timestamps were found" }
        // Stable timestamp ordering preserves user-authored sequence when
        // simultaneous cues have different text. Never alphabetize lyrics:
        // that changes the visible line and tap-to-seek ordering.
        return TimedLyrics(cues.distinct().sortedBy { it.timeMs })
    }
}
