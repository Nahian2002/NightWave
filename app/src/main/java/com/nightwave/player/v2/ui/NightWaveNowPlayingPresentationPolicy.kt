package com.nightwave.player.v2.ui

/**
 * NW85 — truthful compact player presentation. Pure Kotlin / read-only.
 * Navigation and Media3 remain owned by their existing playback engine.
 */
object NightWaveNowPlayingPresentationPolicy {
    fun statusLabel(health: com.nightwave.player.v2.playback.PlaybackHealth): String = when (health.phase) {
        com.nightwave.player.v2.playback.PlaybackPhase.PAUSED -> "Paused"
        com.nightwave.player.v2.playback.PlaybackPhase.ERROR -> "Playback unavailable"
        else -> health.title
    }

    fun canSeek(track: String, connected: Boolean, durationMs: Long, resolving: Boolean, error: String?): Boolean =
        track.isNotBlank() && connected && durationMs > 0L && !resolving && error.isNullOrBlank()

    fun seekPosition(expected: String, current: String, connected: Boolean, durationMs: Long,
        resolving: Boolean, error: String?, fraction: Float): Long? =
        if (expected != current || !canSeek(current, connected, durationMs, resolving, error)) null
        else PlayerProgressRules.requestedPosition(durationMs, fraction)

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
        !hasTrack -> "Choose a song"
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

    /**
     * Full-width title/artist metadata beats a permanent trailing Like button
     * on narrow windows or enlarged Android text. Keep the existing one-row
     * look on ordinary 360/390/412dp phone windows at 1.0x font scale.
     * Pure presentation decision: no Like data or player state is modified.
     */
    fun useFullWidthMetadata(widthDp: Float, fontScale: Float): Boolean {
        val width = widthDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val scale = fontScale.takeIf { it.isFinite() }?.coerceIn(0.7f, 3f) ?: 1f
        val available = (width -
            NightWaveAdaptiveChromePolicy.horizontalPaddingDp(width) * 2)
            .coerceAtLeast(0f)
        return available < 320f || scale >= 1.6f
    }

    fun likeStatusLabel(liked: Boolean, offline: Boolean): String = when {
        offline -> "Likes unavailable for offline tracks"
        liked -> "In Liked Songs"
        else -> "Add to Liked Songs"
    }

    /** Large text gets enough rows before any title/creator is truncated. */
    fun titleLines(fontScale: Float, widthDp: Float): Int = when {
        fontScale.isFinite() && fontScale >= 2f -> 4
        (fontScale.isFinite() && fontScale >= 1.3f) ||
            (widthDp.isFinite() && widthDp >= 500f) -> 3
        else -> 2
    }

    fun artistLines(fontScale: Float): Int = when {
        fontScale.isFinite() && fontScale >= 2.2f -> 3
        fontScale.isFinite() && fontScale >= 1.5f -> 2
        else -> 1
    }
}
