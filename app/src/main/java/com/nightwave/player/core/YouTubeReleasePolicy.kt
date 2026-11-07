package com.nightwave.player.core

import com.nightwave.player.BuildConfig

/** The debug-only owner preview does not enable YouTube offline saving. */
object YouTubeReleasePolicy {
    const val OFFICIAL_ONLY = true
    const val UNOFFICIAL_SEARCH_ENABLED = true
    val ownerPlaybackEnabled: Boolean get() = BuildConfig.OWNER_YOUTUBE_PLAYBACK_PREVIEW
    const val OFFLINE_UNAVAILABLE = "YouTube tracks cannot be downloaded here. Import your own audio or choose a supported offline source."

    fun requireUnofficialSearch() {
        check(UNOFFICIAL_SEARCH_ENABLED) { "Native YouTube search is unavailable in this build." }
    }

    fun requireLegacyAccess() {
        check(!OFFICIAL_ONLY) { "Open YouTube to listen to this track." }
    }
}
