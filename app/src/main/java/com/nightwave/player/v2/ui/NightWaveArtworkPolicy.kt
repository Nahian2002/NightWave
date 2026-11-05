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

    /** Public thumbnail metadata is eligible only with an independently matching canonical track.
     * The stored queried address is never modified and remains the final fallback.
     */
    fun trackCandidates(
        original: String, canonicalTrack: String, large: Boolean, sharperTile: Boolean = false
    ): List<String> {
        val unchanged = requestCandidates(original, large)
        if (!large && !sharperTile) return unchanged
        return runCatching {
            val uri = URI(original)
            val query = uri.rawQuery ?: return unchanged
            val fields = query.split('&').map { it.split('=', limit = 2) }
            if (fields.size != 2 || fields.any { it.size != 2 ||
                    !Regex("^[A-Za-z0-9_-]+={0,2}$").matches(it[1]) } ||
                fields.map { it[0] }.toSet() != setOf("sqp", "rs")) return unchanged
            // Reuse the strict unsigned-path parser only after validating the exact public keys.
            // User info, fragments, encoded paths and foreign hosts remain ineligible.
            if (uri.rawFragment != null) return unchanged
            val unsigned = original.substringBefore('?')
            val source = still(unsigned) ?: return unchanged
            if (canonicalTrack != "https://www.youtube.com/watch?v=${source.first}") return unchanged
            val sizes = if (large) listOf("maxresdefault.jpg", "sddefault.jpg", "hqdefault.jpg")
                else listOf("hqdefault.jpg")
            (sizes.map { "https://i.ytimg.com/vi/${source.first}/$it" } + original).distinct()
        }.getOrDefault(unchanged)
    }

    /** Some missing large stills return a 120x90 placeholder with HTTP 200. */
    fun isUnavailableLargeStill(url: String, width: Int, height: Int): Boolean {
        val name = still(url)?.second ?: return false
        return name in setOf("maxresdefault.jpg", "sddefault.jpg") &&
            width in 1..120 && height in 1..90
    }
}
