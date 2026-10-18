package com.nightwave.player.v2.ui

/**
 * Presentation-only browse policy. Does not identify online videos as
 * locally downloaded audio or alter the library/search data layer.
 */
object NightWaveBrowseChromePolicy {
    const val MAX_HEADER_TITLE = 80
    const val COMPACT_WIDTH_DP = 440f
    const val TRACK_ART_DP = 55
    const val COLLECTION_ART_DP = 54
    const val MIN_TILE_HEIGHT_DP = 72

    enum class Source { SAVED_LIBRARY, PUBLIC_VIDEO }

    fun countLabel(count: Int, singular: String, plural: String): String {
        require(count >= 0) { "Count must be nonnegative" }
        return "$count " + (if (count == 1) singular else plural)
    }

    fun sourceDetail(source: Source): String = when (source) {
        Source.SAVED_LIBRARY -> "Saved in your library · playback may need internet"
        Source.PUBLIC_VIDEO -> "Public YouTube results · internet required"
    }

    fun title(value: String, fallback: String): String =
        value.trim().take(MAX_HEADER_TITLE).ifEmpty { fallback }

    fun showCompactCollections(availableWidthDp: Float): Boolean =
        !availableWidthDp.isFinite() || availableWidthDp < COMPACT_WIDTH_DP

    fun libraryCounts(liked: Int, playlists: Int): String =
        countLabel(liked, "liked track", "liked tracks") + "  ·  " +
            countLabel(playlists, "playlist", "playlists")
}
