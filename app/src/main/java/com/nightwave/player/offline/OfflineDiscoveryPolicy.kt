package com.nightwave.player.offline

/**
 * Converts a user's song-title/artist request into a bounded search for
 * legally downloadable recordings. Never asserts a result is identical to a
 * YouTube upload; recordings and licenses must still be reviewed by the user.
 */
object OfflineDiscoveryPolicy {
    private val decoration = Regex(
        """(?i)[\[(]\s*(official\s+(music\s+)?(audio|video)|lyric\s+video|lyrics?|visualizer|4k|hd)\s*[\])]"""
    )
    private val topicSuffix = Regex("""(?i)\s*-\s*topic\s*$""")

    fun queries(title: String, artist: String = ""): List<String> {
        val cleanTitle = FreeAudioCatalogPolicy.safeSearchTerm(
            title.replace(decoration, " ").trim())
        val cleanArtist = FreeAudioCatalogPolicy.safeSearchTerm(
            artist.replace(topicSuffix, "").trim())
        if (cleanTitle.isBlank()) return emptyList()
        val joined = if (cleanArtist.isBlank() ||
            cleanTitle.contains(cleanArtist, ignoreCase = true)
        ) cleanTitle else FreeAudioCatalogPolicy.safeSearchTerm(
            "$cleanTitle $cleanArtist"
        )
        return listOf(joined, cleanTitle).filter { it.isNotBlank() }.distinct().take(2)
    }
}
