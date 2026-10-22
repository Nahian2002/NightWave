package com.nightwave.player.offline

import java.net.URI
import java.util.Locale

/** Only files clearly marked CC0 or public domain in Commons metadata. */
data class FreeAudioCandidate(
    val title: String,
    val sourceUrl: String,
    val descriptionUrl: String,
    val licenseName: String,
    val bytes: Long,
    val saveName: String,
    val sha1Base36: String
)

object FreeAudioCatalogPolicy {
    private val formats = mapOf(
        "mp3" to "mp3", "flac" to "flac", "wav" to "wav",
        "m4a" to "m4a", "aac" to "aac", "ogg" to "ogg",
        "oga" to "ogg", "opus" to "opus", "webm" to "webm"
    )
    private val allowedWords = Regex("[^\\p{L}\\p{M}\\p{N} ._()'\\-]+")

    fun safeSearchTerm(text: String): String = text.trim().take(100)
        .replace(Regex("[^\\p{L}\\p{M}\\p{N} '\\-]+"), " ")
        .replace(Regex("\\s+"), " ").trim().take(80)

    fun permittedLicense(shortName: String): Boolean {
        val label = shortName.trim().lowercase(Locale.ROOT)
        return label == "cc0" || label.startsWith("cc0 ") ||
            label == "public domain" || label.startsWith("public domain ") ||
            label == "pd" // Commons short-name alias only
    }

    /** Do not permit arbitrary media hosts or a URL supplied from an untrusted page. */
    fun safeMediaUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme.equals("https", true) &&
            uri.host.equals("upload.wikimedia.org", true) &&
            uri.userInfo == null && uri.rawFragment == null &&
            uri.port == -1 && uri.rawPath?.startsWith("/wikipedia/commons/") == true
    }

    fun safeDescriptionUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme.equals("https", true) &&
            uri.host.equals("commons.wikimedia.org", true) &&
            uri.userInfo == null && uri.port == -1 &&
            uri.path?.startsWith("/wiki/File:") == true
    }

    fun saveName(fileTitle: String, contentType: String): String? {
        val raw = fileTitle.removePrefix("File:").trim()
        val originalExt = raw.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val targetExt = formats[originalExt] ?: return null
        if (!PermittedAudioDownloadPolicy.supportedContentType(contentType, targetExt)) {
            return null
        }
        val clean = raw.substringBeforeLast('.').replace('_', ' ')
            .replace(allowedWords, " ").replace(Regex("\\s+"), " ")
            .trim(' ', '.', '_').take(90)
        if (clean.isBlank()) return null
        return "$clean.$targetExt"
    }

    fun validSize(bytes: Long): Boolean = bytes in 1..OfflineDownloadLimits.MAX_AUDIO_BYTES
}
