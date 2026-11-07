package com.nightwave.player.core

/** Public builds delegate YouTube access to YouTube. No runtime preference can enable extraction. */
object YouTubeReleasePolicy {
    const val OFFICIAL_ONLY = true
    const val OFFLINE_UNAVAILABLE = "YouTube tracks cannot be downloaded here. Import your own audio or choose a supported offline source."

    fun requireLegacyAccess() {
        check(!OFFICIAL_ONLY) { "Open YouTube to listen to this track." }
    }
}
