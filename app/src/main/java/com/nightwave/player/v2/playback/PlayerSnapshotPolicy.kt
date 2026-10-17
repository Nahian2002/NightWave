package com.nightwave.player.v2.playback

/**
 * An inexpensive, predictable decision about when an active controller must
 * refresh the persisted queue. The playhead can update without reparsing disk
 * snapshots every 750 ms; authoritative Media3 events still refresh instantly.
 */
internal object PlayerSnapshotPolicy {
    const val RECONCILE_TICKS = 8

    fun needsFullRefresh(
        currentMediaId: String?,
        shownMediaId: String,
        ticksSinceReconcile: Int,
        previouslyPlaying: Boolean
    ): Boolean {
        if (ticksSinceReconcile >= RECONCILE_TICKS - 1) return true
        // A different active MediaItem means title/artwork/queue have changed.
        if (currentMediaId != null && currentMediaId != shownMediaId) return true
        // No MediaItem after a playing track may mean a service restart.
        if (currentMediaId == null && previouslyPlaying) return true
        return false
    }
}
