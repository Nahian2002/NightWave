package com.nightwave.player.offline

import com.nightwave.player.core.VideoLink
import java.net.URI
import java.util.Locale

/**
 * Strict safety boundary for user-consented, non-DRM, audio-only streams.
 * A play URL is NOT an offline asset. Readiness requires verified local audio.
 * No cookies, credential forwarding, private networks or manifest downloads.
 */
object SearchTrackSavePolicy {
    fun canonicalTrack(input: String): String? = VideoLink.normalize(input)

    fun safeAudioEndpoint(raw: String): Boolean {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return false
        val host = uri.host?.trimEnd('.')?.lowercase(Locale.ROOT) ?: return false
        return uri.scheme.equals("https", true) &&
            uri.userInfo == null && uri.rawFragment == null &&
            uri.port in listOf(-1, 443) &&
            (host == "googlevideo.com" || host.endsWith(".googlevideo.com")) &&
            uri.rawPath.orEmpty().startsWith("/") &&
            raw.length in 25..8192
    }

    fun safeTitle(title: String): String {
        // Private file names are labels, not paths. Preserve Bangla/Unicode
        // letters instead of replacing every non-ASCII word with underscores.
        return title.replace(Regex("[\\p{Cntrl}/\\\\:<>\"|?*]"), " ")
            .replace(Regex("\\s+"), " ").trim(' ', '.').take(105)
            .ifBlank { "Saved audio" }
    }

    fun validContainer(extension: String): Boolean =
        extension in setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "webm")

    fun resultStatus(savedId: String?, isReadable: Boolean): String =
        if (savedId != null && isReadable) "READY_OFFLINE" else "NOT_AVAILABLE_OFFLINE"
}
