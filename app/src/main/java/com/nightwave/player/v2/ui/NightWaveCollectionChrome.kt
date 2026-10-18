package com.nightwave.player.v2.ui

/** UI-only sizing and provenance policy. Never changes stored music or search ordering. */
enum class NightWaveTrackSurface { HOME, SEARCH, LIBRARY }

object NightWaveCollectionChrome {
    const val TRACK_ARTWORK_DP = 52
    const val COLLECTION_ARTWORK_DP = 56
    const val TRACK_MIN_HEIGHT_DP = 66
    const val COLLECTION_MIN_HEIGHT_DP = 78
    const val COLLECTION_MAX_WIDTH_DP = 740
    const val COMPACT_PADDING_DP = 14

    fun lineCount(availableWidthDp: Float): Int =
        if (availableWidthDp.isFinite() && availableWidthDp >= 430f) 2 else 1

    fun sectionSubtitle(count: Int, singular: String, plural: String): String {
        val safe = count.coerceAtLeast(0)
        return "$safe " + (if (safe == 1) singular else plural)
    }

    /** Does not claim a public search result is saved or available offline. */
    fun sourceLabel(surface: NightWaveTrackSurface): String = when (surface) {
        NightWaveTrackSurface.HOME -> "Recently heard"
        NightWaveTrackSurface.SEARCH -> "Search result"
        NightWaveTrackSurface.LIBRARY -> "Saved in your library"
    }
}
