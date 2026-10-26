package com.nightwave.player.v2.ui

/**
 * NW85 — truthful compact player presentation. Pure Kotlin / read-only.
 * Navigation and Media3 remain owned by their existing playback engine.
 */
object NightWaveNowPlayingPresentationPolicy {
    data class QueuePreview(
        val heading: String,
        val subtitle: String,
        val sourceSlot: Int?
    )

    /**
     * Do not claim a sequential "up next" track under shuffle: that next
     * track has not been selected yet. Explicit user "Play next" entries
     * DO have priority and can be shown without predicting random choices.
     */
    fun queuePreview(
        urls: List<String>,
        titles: List<String>,
        currentIndex: Int,
        manualNextUrls: List<String>,
        shuffled: Boolean
    ): QueuePreview {
        if (urls.isEmpty() || currentIndex !in urls.indices)
            return QueuePreview("Up next", "Queue is empty", null)

        for (requested in manualNextUrls) {
            val at = urls.indices.firstOrNull {
                it != currentIndex && urls[it] == requested
            } ?: continue
            return QueuePreview("Play next", titles.getOrNull(at)
                ?.takeIf { it.isNotBlank() } ?: "Queued song", at)
        }

        if (shuffled) {
            val remaining = (urls.size - 1).coerceAtLeast(0)
            return QueuePreview("Shuffle queue",
                if (remaining == 0) "No other tracks" else
                    "$remaining possible next " + if (remaining == 1) "track" else "tracks",
                null)
        }

        val next = currentIndex + 1
        return if (next in urls.indices) QueuePreview("Up next",
            titles.getOrNull(next)?.takeIf(String::isNotBlank) ?: "Queued song", next)
        else QueuePreview("Up next", "End of current queue", null)
    }

    fun offlineActionLabel(hasTrack: Boolean, offline: Boolean): String = when {
        !hasTrack -> "Offline library"
        offline -> "Open offline library"
        else -> "Offline options"
    }

    fun artworkGestureHint(canSwipe: Boolean): String? =
        if (canSwipe) "Swipe artwork to skip" else null

    /** Lightweight visual hierarchy; large text uses more vertical room. */
    fun titleLines(fontScale: Float, widthDp: Float): Int =
        if ((fontScale.isFinite() && fontScale >= 1.3f) ||
            (widthDp.isFinite() && widthDp >= 500f)) 3 else 2

    fun artistLines(fontScale: Float): Int =
        if (fontScale.isFinite() && fontScale >= 1.5f) 2 else 1
}
