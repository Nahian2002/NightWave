package com.nightwave.player.v2.social

import com.nightwave.player.net.VideoPreview
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * User-selected SAF documents containing ONLY the existing, bounded public
 * playlist text-share format. Never serializes local audio, thumbnails,
 * accounts, signed streams or private backups.
 */
data class PreparedPlaylistDocument(val fileName: String, val bytes: ByteArray)

object PlaylistDocumentPolicy {
    /** Four UTF-8 bytes for every permitted input code point, bounded upstream. */
    const val MAX_BYTES = PlaylistShareParser.MAX_TEXT_CHARS * 4

    fun prepare(name: String, tracks: List<VideoPreview>): PreparedPlaylistDocument? {
        val text = PlaylistShareFormatter.format(name, tracks) ?: return null
        // Export only what the current strict importer can round-trip.
        if (PlaylistShareParser.parse(text) == null) return null
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return null
        // Preserve Bangla vowel/combining marks without allowing separators,
        // traversal punctuation or filesystem-control characters.
        val safeBase = name.replace(Regex("""[^\\p{L}\\p{M}\\p{N} _-]+"""), " ")
            .trim().take(55).trim().ifBlank { "NightWave playlist" }
        return PreparedPlaylistDocument("$safeBase.nightwave.txt", bytes)
    }

    /** Bounded, strict UTF-8 decode and parse; never modifies the library. */
    fun read(input: InputStream): SharedPlaylist? {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(4096)
        var consecutiveEmptyReads = 0
        while (true) {
            val count = input.read(chunk)
            if (count < 0) break
            if (count == 0) {
                // A broken content provider must not spin forever.
                if (++consecutiveEmptyReads > 8) return null
                continue
            }
            consecutiveEmptyReads = 0
            if (out.size() > MAX_BYTES - count) return null
            out.write(chunk, 0, count)
        }
        if (out.size() == 0) return null
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = runCatching {
            decoder.decode(ByteBuffer.wrap(out.toByteArray())).toString()
        }.getOrNull() ?: return null
        return PlaylistShareParser.parse(text)
    }
}
