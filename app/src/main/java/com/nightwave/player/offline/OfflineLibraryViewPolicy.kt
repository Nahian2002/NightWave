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
        Filter.READY -> "Ready offline"
        Filter.ATTENTION -> "Needs attention"
    }

    fun emptyMessage(
        filter: Filter,
        hasIndexedSongs: Boolean,
        hasQuery: Boolean
    ): String = when {
        !hasIndexedSongs -> "No offline songs yet. Import audio you own or are licensed to use."
        hasQuery -> "No tracks match your search and filter."
        filter == Filter.ATTENTION -> "All indexed tracks have available local audio."
        filter == Filter.READY -> "No verified playable audio. Check Needs attention."
        else -> "No matching songs. Try another search."
    }
}
