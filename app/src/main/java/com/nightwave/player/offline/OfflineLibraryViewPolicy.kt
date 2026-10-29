package com.nightwave.player.offline

/**
 * NW48 read-only filter for indexed audio. A damaged/missing file remains
 * visible under "Needs attention" and is NEVER deleted or repaired by UI.
 */
object OfflineLibraryViewPolicy {
    enum class Filter { ALL, READY, ATTENTION }

    fun includes(filter: Filter, health: OfflineFileHealth): Boolean = when (filter) {
        Filter.ALL -> true
        Filter.READY -> health == OfflineFileHealth.AVAILABLE
        Filter.ATTENTION -> health != OfflineFileHealth.AVAILABLE
    }

    fun label(filter: Filter): String = when (filter) {
        Filter.ALL -> "All"
        Filter.READY -> "Available"
        Filter.ATTENTION -> "Issues"
    }

    /** Short on small screens; a full description remains available to TalkBack. */
    fun accessibilityLabel(filter: Filter): String = when (filter) {
        Filter.ALL -> "All"
        Filter.READY -> "Available local files (size checked)"
        Filter.ATTENTION -> "Needs attention"
    }

    fun summary(visible: Int, total: Int, playable: Int, issues: Int): String {
        val count = visible.coerceAtLeast(0)
        val totalCount = total.coerceAtLeast(0)
        val playableCount = playable.coerceAtLeast(0)
        val issueCount = issues.coerceAtLeast(0)
        return "$count of $totalCount tracks · $playableCount local files available" +
            if (issueCount > 0) " · $issueCount need attention" else ""
    }

    fun emptyMessage(
        filter: Filter,
        hasIndexedSongs: Boolean,
        hasQuery: Boolean
    ): String = when {
        !hasIndexedSongs -> "No offline songs yet. Import audio you own or are licensed to use."
        hasQuery -> "No tracks match your search and filter."
        filter == Filter.ATTENTION -> "All indexed tracks have available local audio."
        filter == Filter.READY -> "No available local audio. Check Needs attention."
        else -> "No matching songs. Try another search."
    }
}
