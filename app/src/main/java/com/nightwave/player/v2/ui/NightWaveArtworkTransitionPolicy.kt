package com.nightwave.player.v2.ui

/**
 * NW116: art transitions belong to the selected track AND thumbnail, not the
 * image address alone. A temporary outgoing cover may remain visible during
 * a crossfade, but must never be announced as the newly selected song.
 *
 * Presentation only. IDs and thumbnails stay in memory; this policy never
 * fetches, stores, decodes or alters media and does not own playback.
 */
internal object NightWaveArtworkTransitionPolicy {
    data class Frame(val trackId: String, val thumbnail: String)

    fun frame(trackId: String, thumbnail: String): Frame =
        Frame(trackId, thumbnail)

    /**
     * Avoid two simultaneously announced artwork nodes when Compose draws
     * outgoing and incoming frames. The current cover alone is accessible.
     */
    fun accessibilityDescription(
        visible: Frame, current: Frame, currentTitle: String
    ): String? = when {
        visible != current -> null
        currentTitle.isBlank() -> "Song artwork"
        else -> "Artwork for " + currentTitle
    }

    fun shouldAnimate(from: Frame, to: Frame, reduceMotion: Boolean): Boolean =
        !reduceMotion && from != to
}
