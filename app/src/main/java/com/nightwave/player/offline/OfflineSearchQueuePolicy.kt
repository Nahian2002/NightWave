package com.nightwave.player.offline

/**
 * Pure offline navigation policy. Select the requested verified song first,
 * then follow the original collection order, wrapping to earlier saved songs.
 * Never insert an online-only track into the private offline queue.
 */
object OfflineSearchQueuePolicy {
    const val MAX_IDS = 400

    fun plan(selectedId: String, orderedIds: List<String?>): List<String> {
        if (selectedId.isBlank()) return emptyList()
        val found = orderedIds.indexOf(selectedId)
        val rotated = if (found >= 0) {
            orderedIds.drop(found) + orderedIds.take(found)
        } else listOf(selectedId) + orderedIds
        val ids = LinkedHashSet<String>()
        ids.add(selectedId)
        for (id in rotated) {
            if (!id.isNullOrBlank()) ids.add(id)
            if (ids.size >= MAX_IDS) break
        }
        return ids.take(MAX_IDS)
    }
}
