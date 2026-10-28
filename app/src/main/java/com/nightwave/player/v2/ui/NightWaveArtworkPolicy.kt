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

    /** Only known, unsigned YouTube still paths may gain same-video variants. */
    private fun still(original: String): Pair<String, String>? = runCatching {
        val uri = URI(original)
        if (uri.scheme?.lowercase(java.util.Locale.ROOT) != "https" ||
            uri.host?.lowercase(java.util.Locale.ROOT) !in setOf("i.ytimg.com", "img.youtube.com") ||
            uri.rawUserInfo != null || uri.port !in setOf(-1, 443) ||
            uri.rawQuery != null || uri.rawFragment != null) return null
        val parts = uri.rawPath.orEmpty().split('/')
        if (parts.size != 4 || parts[0] != "" || parts[1] != "vi" || !videoId.matches(parts[2]) ||
            parts[3] !in smallFiles + setOf("hqdefault.jpg", "sddefault.jpg", "maxresdefault.jpg", "0.jpg")) return null
        parts[2] to parts[3]
    }.getOrNull()

    fun requestCandidates(original: String, large: Boolean): List<String> {
        if (original.isBlank()) return emptyList()
        val source = still(original) ?: return listOf(original)
        if (!large) return listOf(preferredArtwork(original), original).distinct()
        return (listOf("maxresdefault.jpg", "sddefault.jpg", "hqdefault.jpg")
            .map { "https://i.ytimg.com/vi/${source.first}/$it" } + original).distinct()
    }

    fun requestPixels(width: Int, height: Int): Int =
        if (width <= 0 || height <= 0) 0 else minOf(width, height).coerceIn(48, 1280)

    /** Some missing large stills return a 120x90 placeholder with HTTP 200. */
    fun isUnavailableLargeStill(url: String, width: Int, height: Int): Boolean {
        val name = still(url)?.second ?: return false
        return name in setOf("maxresdefault.jpg", "sddefault.jpg") &&
            width in 1..120 && height in 1..90
    }
}
