package com.nightwave.player.v2.ui

/** NW54: short, honest consumer copy without surfacing scraper endpoints.
 * Provider/error telemetry remains available to source diagnostics, not in
 * consumer-facing discovery headers. Never imply verified artist identity.
 */
object SearchConsumerCopyPolicy {
    enum class Mode { SONGS, VIDEOS }

    fun resultsTitle(mode: Mode): String =
        if (mode == Mode.VIDEOS) "Videos" else "Songs & music"

    fun resultCount(count: Int): String =
        when (count.coerceAtLeast(0)) {
            0 -> "No online matches yet"
            1 -> "1 online result"
            else -> "${count.coerceAtLeast(0)} online results"
        }

    fun bestMatchesSubtitle(): String = "Closest to your search"
    fun moreResultsSubtitle(): String = "More recordings, covers & remixes"

    fun savedCreatorSubtitle(count: Int): String =
        "${count.coerceAtLeast(0)} saved tracks"

    fun savedCreatorNotice(): String =
        "Creator names come from your saved tracks and may differ from official artist pages."

    fun loadingHint(hasLocal: Boolean): String =
        if (hasLocal) "Your saved music is ready while results load."
        else "Searching for songs and videos."

    fun unavailableTitle(): String = "Couldn't load online music"

    fun fallbackHint(hasLocal: Boolean): String =
        if (hasLocal) "Your saved tracks are still here. Retry or use YouTube."
        else "Try again, change the search words, or use YouTube."
}
