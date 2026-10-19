package com.nightwave.player.v2.ui

import java.net.URI

/**
 * Only upgrade explicitly low-resolution YouTube stills. Never rewrite
 * third-party artwork URLs, user-picked content URIs, or local audio art.
 * YouTube's hqdefault is usually more useful than its 120x90 default but
 * is not a guarantee of full-resolution album artwork.
 */
internal object NightWaveArtworkPolicy {
    private val smallFiles = setOf(
        "default.jpg", "mqdefault.jpg", "1.jpg", "2.jpg", "3.jpg"
    )
    private val videoId = Regex("^[A-Za-z0-9_-]{11}$")

    fun preferredArtwork(original: String): String {
        if (original.isBlank()) return original
        return runCatching {
            val uri = URI(original)
            val host = uri.host?.lowercase() ?: return original
            if (uri.scheme?.lowercase() != "https" ||
                host !in setOf("i.ytimg.com", "img.youtube.com") ||
                uri.query != null || uri.fragment != null
            ) return original
            val parts = uri.path.orEmpty().trim('/').split('/')
            if (parts.size != 3 || parts[0] != "vi" ||
                !videoId.matches(parts[1]) || parts[2] !in smallFiles
            ) return original
            "https://i.ytimg.com/vi/${parts[1]}/hqdefault.jpg"
        }.getOrDefault(original)
    }

    fun allowOriginalFallback(original: String, preferred: String): Boolean =
        original.isNotBlank() && original != preferred
}
