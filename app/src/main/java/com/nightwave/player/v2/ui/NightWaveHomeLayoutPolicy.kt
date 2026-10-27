package com.nightwave.player.v2.ui

/**
 * NW84: compact music-first Home presentation. Layout only: no user history,
 * autoplay, network access, service commands or saved preferences.
 */
object NightWaveHomeLayoutPolicy {
    fun mixColumns(windowWidthDp: Float, fontScale: Float): Int {
        val width = windowWidthDp.takeIf { it.isFinite() } ?: 0f
        val scale = fontScale.takeIf { it.isFinite() } ?: 1f
        // Two 146dp cards become illegible on small phones and with large text.
        return if (width < 390f || scale >= 1.3f) 1 else 2
    }

    fun mixCardCompact(columnCount: Int): Boolean = columnCount == 1

    /**
     * NW117: an empty listening shelf isn't an empty library. Suggest one
     * action grounded only in real saved references, with no inferred taste,
     * fabricated recommendation, autoplay or listening-history mutation.
     */
    enum class EmptyRecentDestination { SEARCH, LIKED_SONGS, LIBRARY }

    data class EmptyRecentState(
        val title: String,
        val detail: String,
        val actionLabel: String,
        val destination: EmptyRecentDestination
    )

    fun recentEmptyState(
        favoriteCount: Int,
        playlistTrackCount: Int
    ): EmptyRecentState = when {
        favoriteCount > 0 -> EmptyRecentState(
            title = "No recent songs to show",
            detail = "Your liked songs are ready whenever you want to listen.",
            actionLabel = "Open Liked Songs",
            destination = EmptyRecentDestination.LIKED_SONGS
        )
        playlistTrackCount > 0 -> EmptyRecentState(
            title = "No recent songs to show",
            detail = "Your saved playlists are a good place to start.",
            actionLabel = "Open Library",
            destination = EmptyRecentDestination.LIBRARY
        )
        else -> EmptyRecentState(
            title = "Your soundtrack starts here",
            detail = "Search for music to discover your next listen.",
            actionLabel = "Find music",
            destination = EmptyRecentDestination.SEARCH
        )
    }
}
