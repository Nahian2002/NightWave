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
        shuffled: Boolean,
        repeatOne: Boolean = false,
        repeatAll: Boolean = false
    ): QueuePreview {
        if (urls.isEmpty())
            return QueuePreview("Up next", "Queue is empty", null)
        if (currentIndex !in urls.indices)
            return QueuePreview("Up next", "Waiting for queue selection", null)
        // Automatic transitions respect Repeat One even when manual Play Next
        // selections are queued; the user can still skip with Next.
        if (repeatOne)
            return QueuePreview("Repeat one", "Current song will replay", null)

        for (requested in manualNextUrls) {
            val at = urls.indices.firstOrNull {
                it != currentIndex && (shuffled || it > currentIndex) &&
                    urls[it] == requested
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
        return when {
            next in urls.indices -> QueuePreview("Up next",
                titles.getOrNull(next)?.takeIf(String::isNotBlank) ?: "Queued song", next)
            repeatAll && urls.isNotEmpty() -> QueuePreview("Repeat all",
                titles.firstOrNull()?.takeIf(String::isNotBlank) ?: "First queued song", 0)
            else -> QueuePreview("Up next", "End of current queue", null)
        }
    }

    fun offlineActionLabel(hasTrack: Boolean, offline: Boolean): String = when {
        !hasTrack -> "Offline library"
        offline -> "Open offline library"
        else -> "Offline options"
    }

    fun artworkGestureHint(canSwipe: Boolean): String? =
        if (canSwipe) "Swipe artwork to skip" else null

    fun lyricsSubtitle(offline: Boolean, importedLyrics: Boolean): String = when {
        !offline -> "Synchronized lyrics require local audio"
        importedLyrics -> "Your synchronized lyrics · ready"
        else -> "Import your own LRC lyrics"
    }

    /** Lightweight visual hierarchy; large text uses more vertical room. */
    fun titleLines(fontScale: Float, widthDp: Float): Int =
        if ((fontScale.isFinite() && fontScale >= 1.3f) ||
            (widthDp.isFinite() && widthDp >= 500f)) 3 else 2

    fun artistLines(fontScale: Float): Int =
        if (fontScale.isFinite() && fontScale >= 1.5f) 2 else 1
}
