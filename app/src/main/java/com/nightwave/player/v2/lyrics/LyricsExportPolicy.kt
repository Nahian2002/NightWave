package com.nightwave.player.v2.lyrics

/**
 * Export is user-directed through Android's document picker. No file-system
 * permissions, upload, cloud sync or modification of NightWave's saved LRC.
 */
data class PreparedLyricsExport(
    val suggestedName: String,
    val bytes: ByteArray,
    val cueCount: Int,
    val unsavedDraft: Boolean
)

object LyricsExportPolicy {
    const val MAX_BASENAME_LENGTH = 64
    private val unsafe = Regex("""[\p{Cntrl}/\\:*?"<>|]""")
    private val spaces = Regex("""\s+""")

    fun suggestedFileName(title: String): String {
        val clean = unsafe.replace(title, " ")
            .replace(Regex("""(?i)\.lrc$"""), "")
            .trim().trimEnd('.').trim()
        val base = spaces.replace(clean, " ")
            .take(MAX_BASENAME_LENGTH).trim().trimEnd('.').trim()
            .takeIf { it.isNotEmpty() && it != "." && it != ".." }
            ?: "NightWave Lyrics"
        return "$base.lrc"
    }

    fun prepare(
        songTitle: String,
        baseline: LyricsEditorDraft,
        current: LyricsEditorDraft
    ): PreparedLyricsExport {
        val bytes = LyricsEditorPolicy.serialize(current)
        // Reject any encoding or parser divergence before the file picker
        // is launched; no silent removal of user-editable lyric lines.
        val verified = TimedLyricsCodec.parse(bytes)
        require(verified.cues == current.cues) {
            "LRC export would change the edited lyric cues"
        }
        return PreparedLyricsExport(
            suggestedName = suggestedFileName(songTitle),
            bytes = bytes,
            cueCount = verified.cues.size,
            unsavedDraft = LyricsEditorPolicy.changed(baseline, current)
        )
    }
}
