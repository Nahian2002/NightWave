package com.nightwave.player.playback

import com.nightwave.player.core.VideoLink

/**
 * Restore the same public song the user selected, even when older queue rows
 * must be discarded or de-duplicated. Never silently shift to a different
 * track because a saved numeric index now points somewhere else.
 */
internal object QueueRestorePolicy {
    fun isCanonicalPublicWatchUrl(url: String): Boolean =
        url.startsWith("https://www.youtube.com/watch?v=") &&
            VideoLink.normalize(url) == url

    /**
     * [originalUrls] is the unfiltered serialized order, [restored] the safe
     * retained order. [selectedUrl] is optional for backwards compatibility
     * with old snapshots that recorded only an index.
     */
    fun selectedIndex(
        originalUrls: List<String?>,
        savedIndex: Int,
        selectedUrl: String?,
        restored: List<QueueEntry>
    ): Int? {
        if (savedIndex !in originalUrls.indices) return null
        val originallySelected = originalUrls[savedIndex] ?: return null
        if (!isCanonicalPublicWatchUrl(originallySelected)) return null
        if (selectedUrl != null && selectedUrl != originallySelected) return null
        return restored.indexOfFirst { it.url == originallySelected }
            .takeIf { it >= 0 }
    }
}
