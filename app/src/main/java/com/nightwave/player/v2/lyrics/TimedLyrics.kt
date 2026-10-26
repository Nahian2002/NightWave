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

    fun parse(bytes: ByteArray): TimedLyrics {
        require(bytes.size in 1..MAX_BYTES) { "Lyrics file exceeds 64 KB" }
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        val text = decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            .removePrefix("\uFEFF")
        val cues = mutableListOf<LyricCue>()
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue
            val matches = timestamp.findAll(trimmed).toList()
            if (matches.isEmpty()) continue // Ignore standard [ar:] etc tags.
            val words = timestamp.replace(trimmed, "").trim()
                .replace('\n', ' ').replace('\r', ' ').take(220)
            if (words.isBlank()) continue
            for (match in matches) {
                val min = match.groupValues[1].toInt()
                val sec = match.groupValues[2].toInt()
                if (sec > 59) continue
                val fraction = match.groupValues[3]
                val ms = when (fraction.length) {
                    1 -> fraction.toLong() * 100
                    2 -> fraction.toLong() * 10
                    3 -> fraction.toLong()
                    else -> 0L
                }
                cues += LyricCue((min * 60L + sec) * 1000 + ms, words)
                require(cues.size <= MAX_LINES) { "Lyrics file has too many timed lines" }
            }
        }
        require(cues.isNotEmpty()) { "No synchronized LRC timestamps were found" }
        // Stable timestamp ordering preserves user-authored sequence when
        // simultaneous cues have different text. Never alphabetize lyrics:
        // that changes the visible line and tap-to-seek ordering.
        return TimedLyrics(cues.distinct().sortedBy { it.timeMs })
    }
}
