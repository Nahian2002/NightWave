package com.nightwave.player.offline

/**
 * Bounded, pure selection policy for preparing a Media3 timeline out of
 * verified app-private local files. Never admits streaming URLs or bare paths.
 * Large or incomplete queues fall back to legacy per-track playback.
 */
object OfflineGaplessPolicy {
    const val MAX_PREPARED_ITEMS = 64

    fun selectedIndex(ids: List<String>, selectedId: String): Int? {
        if (ids.size !in 2..MAX_PREPARED_ITEMS ||
            ids.any { it.isBlank() || it.startsWith("offline:") ||
                '/' in it || '\\' in it } ||
            ids.distinct().size != ids.size) return null
        val index = ids.indexOf(selectedId)
        return index.takeIf { it >= 0 }
    }

    /** Auto transitions must be the next item; explicit seeks may select any slot. */
    fun transitionIndex(
        ids: List<String>, currentIndex: Int, mediaId: String?,
        automatic: Boolean, shuffled: Boolean = false,
        repeatAll: Boolean = false
    ): Int? {
        if (currentIndex !in ids.indices || mediaId == null ||
            !mediaId.startsWith("offline:") ||
            selectedIndex(ids, ids[currentIndex]) == null) return null
        val id = mediaId.removePrefix("offline:")
        val target = ids.indexOf(id)
        if (target < 0) return null
        if (automatic) {
            // Only live, selected timeline items can auto-advance. Shuffle
            // follows Media3's verified target; linear mode remains adjacent.
            if (target == currentIndex) return null
            if (!shuffled && target != currentIndex + 1 &&
                !(repeatAll && currentIndex == ids.lastIndex && target == 0)) return null
        }
        return target
    }
}
