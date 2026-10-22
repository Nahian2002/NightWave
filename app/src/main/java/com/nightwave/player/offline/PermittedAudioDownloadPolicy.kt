package com.nightwave.player.offline

import java.net.URI
import java.util.Locale

/** Direct HTTPS AUDIO FILE downloads only; never video pages or signed streaming manifests. */
data class PermittedAudioSource(val url: String, val fileName: String, val extension: String)

object PermittedAudioDownloadPolicy {
    const val MAX_BYTES = OfflineDownloadLimits.MAX_AUDIO_BYTES
    private val formats = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "webm")
    private val forbiddenHosts = setOf(
        "youtube.com", "youtu.be", "youtube-nocookie.com", "googlevideo.com",
        "ytimg.com", "spotify.com", "soundcloud.com"
    )
    private val manifestExtensions = setOf("m3u8", "mpd", "m3u", "pls")
    private val fileNameAllowed = Regex("[^a-zA-Z0-9 ._()\\-]")
    private val ipv4Literal = Regex("[0-9.]+")
    private val hostLabels = Regex("[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+")

    fun prepare(rawUrl: String, rawName: String): PermittedAudioSource {
        require(rawUrl.length in 12..2048) { "Enter a direct HTTPS audio file link." }
        val uri = runCatching { URI(rawUrl.trim()) }.getOrNull()
            ?: throw IllegalArgumentException("Invalid download URL.")
        require(uri.scheme.equals("https", true) &&
            uri.userInfo == null && uri.host != null && uri.rawFragment == null &&
            uri.port in -1..65535 && uri.port != 0) {
            "Only direct HTTPS audio links are accepted."
        }
        val host = uri.host!!.trimEnd('.').lowercase(Locale.ROOT)
        require(host.length in 4..253 && hostLabels.matches(host) &&
            !ipv4Literal.matches(host) && host != "localhost" &&
            !host.endsWith(".localhost") && !host.endsWith(".local") &&
            !host.endsWith(".internal") &&
            forbiddenHosts.none { host == it || host.endsWith(".$it") }) {
            "That site is not a direct downloadable audio source."
        }
        val pathName = uri.path.orEmpty().substringAfterLast('/')
        val pathExtension = pathName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        require(pathExtension !in manifestExtensions) {
            "A streaming playlist cannot be downloaded as a single song."
        }
        val suppliedName = rawName.trim()
        val candidate = suppliedName.ifBlank { pathName }.ifBlank { "Downloaded audio" }
        val fromName = candidate.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val fromPath = pathName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val extension = when {
            fromName in formats -> fromName
            suppliedName.isNotBlank() && '.' in suppliedName ->
                throw IllegalArgumentException("Choose MP3, M4A, AAC, FLAC, WAV, OGG, OPUS or WebM.")
            fromPath in formats -> fromPath
            else -> ""  // For extensionless CDN links, validate actual bytes after download.
        }
        val rawStem = if (extension.isNotBlank() && fromName == extension)
            candidate.substringBeforeLast('.') else candidate
        val stem = rawStem.replace(fileNameAllowed, "_").trim(' ', '.', '_')
            .take(90).ifBlank { "Downloaded audio" }
        return PermittedAudioSource(uri.toASCIIString(),
            if (extension.isBlank()) stem else "$stem.$extension", extension)
    }

    fun supportedContentType(header: String?, extension: String): Boolean {
        val mime = header.orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
        return when (mime) {
            "audio/mpeg", "audio/mp3", "audio/x-mpeg" -> extension == "mp3"
            "audio/mp4", "audio/x-m4a" -> extension == "m4a"
            "audio/aac", "audio/aacp", "audio/x-aac" -> extension == "aac"
            "audio/flac", "audio/x-flac" -> extension == "flac"
            "audio/wav", "audio/wave", "audio/x-wav" -> extension == "wav"
            "audio/ogg", "application/ogg", "audio/opus", "audio/x-opus+ogg" ->
                extension in setOf("ogg", "opus")
            "audio/webm" -> extension == "webm"
            "application/octet-stream", "binary/octet-stream" -> extension in formats
            else -> false
        }
    }

    /**
     * Resolve audio format only when content MIME and actual file signature
     * agree. If the URL had no extension, infer from bytes; never relabel an
     * M4A, OGG or WebM file as MP3.
     */
    fun detectAudioExtension(mime: String?, prefix: ByteArray, hinted: String): String? {
        val signature = when {
            audioSignatureMatches(prefix, "flac") -> "flac"
            audioSignatureMatches(prefix, "wav") -> "wav"
            audioSignatureMatches(prefix, "m4a") -> "m4a"
            audioSignatureMatches(prefix, "webm") -> "webm"
            audioSignatureMatches(prefix, "ogg") -> {
                // The Ogg container may carry Opus or Vorbis.
                val opus = prefix.size >= 36 && (28..(prefix.size - 8)).any { start ->
                    "OpusHead".indices.all { offset ->
                        prefix[start + offset] == "OpusHead"[offset].code.toByte()
                    }
                }
                if (opus) "opus" else "ogg"
            }
            audioSignatureMatches(prefix, "aac") -> "aac"
            audioSignatureMatches(prefix, "mp3") -> "mp3"
            else -> return null
        }
        val chosen = hinted.lowercase(Locale.ROOT).ifBlank { signature }
        if (chosen != signature &&
            !(chosen in setOf("opus", "ogg") && signature in setOf("opus", "ogg"))) return null
        return chosen.takeIf { supportedContentType(mime, it) }
    }

    /** Reject an HTML/error page disguised as an audio download. */
    fun audioSignatureMatches(firstBytes: ByteArray, extension: String): Boolean {
        if (firstBytes.size < 4) return false
        fun ascii(offset: Int, text: String): Boolean =
            firstBytes.size >= offset + text.length &&
                text.indices.all { i -> firstBytes[offset + i] == text[i].code.toByte() }
        return when (extension) {
            "mp3" -> ascii(0, "ID3") ||
                ((firstBytes[0].toInt() and 0xFF) == 0xFF &&
                    (firstBytes[1].toInt() and 0xE0) == 0xE0)
            "flac" -> ascii(0, "fLaC")
            "wav" -> (ascii(0, "RIFF") || ascii(0, "RF64")) && ascii(8, "WAVE")
            "ogg", "opus" -> ascii(0, "OggS")
            "webm" -> firstBytes.take(4).map { it.toInt() and 0xFF } ==
                listOf(0x1A, 0x45, 0xDF, 0xA3)
            "m4a" -> ascii(4, "ftyp")
            "aac" -> (firstBytes[0].toInt() and 0xFF) == 0xFF &&
                (firstBytes[1].toInt() and 0xF0) == 0xF0
            else -> false
        }
    }
}

object OfflineDownloadLimits {
    const val MAX_AUDIO_BYTES = 250L * 1024L * 1024L
}
