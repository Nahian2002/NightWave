package com.nightwave.player.v2.lyrics

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Locale

/** Advisory only: filename metadata is not evidence that the words match the song. */
enum class LyricsImportMatch { LIKELY, POSSIBLE, DIFFERENT, UNKNOWN }

data class LyricsImportReview(
    val lyrics: TimedLyrics,
    val match: LyricsImportMatch,
    val sourceLabel: String,
    val declaredTitle: String?,
    val ignoredTimestampRows: Int,
    val repeatedCues: Int,
    val outOfOrderCues: Int,
    val durationWarning: Boolean,
    val warnings: List<String>
) {
    val cueCount: Int get() = lyrics.cues.size
    val lastCueMs: Long get() = lyrics.cues.lastOrNull()?.timeMs ?: 0L
}

/**
 * Preflight user-selected LRC in memory before confirmation and storage.
 * Do not search files, guess an offline song ID, transcribe audio, or write
 * imported lyrics from an advisory filename match.
 */
object LyricsImportReviewPolicy {
    private val stamp = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]""")
    private val likelyTimedRow = Regex("""^\[\d""")
    private val declaredTitle = Regex("""(?im)^\s*\[ti:([^\]\r\n]{1,150})\]\s*$""")
    private val nameClean = Regex("""(?iu)\s*(?:[\[(]\s*(?:official\s+)?(?:sync(?:ed|hronized)?\s+)?lyrics?\s*[\])])\s*""")
    // Keep dependent vowel signs and combining marks in Bangla and other scripts.
    // Dropping them would make মেলা and মালা indistinguishable in import review.
    private val separators = Regex("""[^\p{L}\p{M}\p{N}]+""")

    fun normalizedName(value: String): String =
        separators.replace(nameClean.replace(value, " "), " ")
            .lowercase(Locale.ROOT).trim().replace(Regex("""\s+"""), " ")

    fun compare(sourceLabel: String, songTitle: String, tagTitle: String?): LyricsImportMatch {
        val target = normalizedName(songTitle)
        if (target.isEmpty()) return LyricsImportMatch.UNKNOWN
        val file = sourceLabel.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("""(?i)\.lrc$"""), "")
        val fileName = normalizedName(file)
        val declared = tagTitle?.let(::normalizedName).orEmpty()
        // Conflicting explicit [ti:] metadata is a warning, never a silent match.
        if (declared.isNotEmpty() && declared != target &&
            !(target.length >= 5 && declared.contains(target))) {
            return LyricsImportMatch.DIFFERENT
        }
        if (fileName == target || declared == target) return LyricsImportMatch.LIKELY
        // Whole normalized word-sequence boundaries avoid accidental
        // subword matches (e.g. "alo" should not match "alone").
        val targetPhrase = " $target "
        if (target.length >= 5 &&
            ((" $fileName ").contains(targetPhrase) ||
                (" $declared ").contains(targetPhrase))) {
            return LyricsImportMatch.POSSIBLE
        }
        return if (fileName.isEmpty() && declared.isEmpty()) LyricsImportMatch.UNKNOWN
        else LyricsImportMatch.DIFFERENT
    }

    /** Fixed user-facing reasons; never echo provider errors or private file paths. */
    fun failureHint(error: Throwable): String = when {
        error is java.nio.charset.CharacterCodingException ->
            "The selected LRC is not valid UTF-8. Convert it to UTF-8 and retry."
        error.message?.contains("64 KB") == true ->
            "The selected lyrics exceed 64 KB. Choose a smaller timestamped LRC."
        error.message?.contains("too many timed lines") == true ->
            "This LRC contains more than 1,200 timestamped lines."
        error.message?.contains("No synchronized LRC timestamps") == true ->
            "No valid timestamps were found. Use [mm:ss.xx] before each lyric."
        else ->
            "The file could not be reviewed. Choose a UTF-8 LRC file under 64 KB."
    }

    fun inspect(
        bytes: ByteArray,
        sourceLabel: String,
        targetTitle: String,
        trackDurationMs: Long
    ): LyricsImportReview {
        // The original production parser is the sole authority for accepting
        // cues. Analysis never relaxes byte, cue, timestamp, or UTF-8 bounds.
        val lyrics = TimedLyricsCodec.parse(bytes)
        val text = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        val tag = declaredTitle.find(text)?.groupValues?.getOrNull(1)?.trim()
        val name = sourceLabel.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("""[\p{Cntrl}]"""), " ")
            .take(100).trim().ifBlank { "Selected LRC" }
        var invalid = 0
        var repeats = 0
        var unordered = 0
        var priorMs = -1L
        val observed = mutableSetOf<Pair<Long, String>>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (!likelyTimedRow.containsMatchIn(line)) continue
            val matches = stamp.findAll(line).toList()
            val words = stamp.replace(line, "").trim().take(220)
            val acceptable = matches.filter { it.groupValues[2].toInt() <= 59 }
            if (acceptable.isEmpty() || words.isBlank() ||
                acceptable.size != matches.size) {
                invalid++
            }
            for (match in acceptable) {
                if (words.isBlank()) continue
                val min = match.groupValues[1].toLong()
                val second = match.groupValues[2].toLong()
                val part = match.groupValues[3]
                val fraction = when (part.length) {
                    1 -> part.toLong() * 100L
                    2 -> part.toLong() * 10L
                    3 -> part.toLong()
                    else -> 0L
                }
                val ms = (min * 60L + second) * 1000L + fraction
                if (priorMs > ms) unordered++
                priorMs = ms
                if (!observed.add(ms to words)) repeats++
            }
        }
        val matchGrade = compare(name, targetTitle, tag)
        val durationWarning = trackDurationMs > 0L &&
            lyrics.cues.last().timeMs > trackDurationMs + 5_000L
        val warnings = buildList {
            when (matchGrade) {
                LyricsImportMatch.DIFFERENT ->
                    add("Filename or [ti:] title does not match the selected song. Review manually.")
                LyricsImportMatch.POSSIBLE ->
                    add("Title appears related, but the correct song is not verified.")
                LyricsImportMatch.UNKNOWN ->
                    add("Song identity cannot be verified from this filename.")
                LyricsImportMatch.LIKELY ->
                    add("Name looks similar; this is not audio-content verification.")
            }
            if (!name.endsWith(".lrc", ignoreCase = true)) {
                add("Selected filename is not .lrc; verify the file contains timed lyrics.")
            }
            if (invalid > 0) add("$invalid malformed or empty timestamp row(s) ignored.")
            if (repeats > 0) add("$repeats duplicate timestamp/text cue(s) collapsed.")
            if (unordered > 0) add("$unordered timestamp(s) out of order; sorted for playback.")
            if (durationWarning) add("Last lyric timestamp exceeds the track duration.")
        }
        return LyricsImportReview(lyrics, matchGrade, name, tag, invalid,
            repeats, unordered, durationWarning, warnings)
    }
}
