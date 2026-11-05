package com.nightwave.player.offline

/** Navigation metadata only: selecting a search entry never starts playback or changes the index. */
object OfflineSearchDestinationPolicy {
    fun entryId(raw: String?): String? = raw?.takeIf {
        it.isNotBlank() && it.length <= 128 && it.none { c -> c == '/' || c == '\\' || c.isISOControl() }
    }

    /** A missing exact entry must not silently fall back to a different recording with the same title. */
    fun select(query: String, songs: List<OfflineSong>, selectedId: String?): List<OfflineSong> =
        if (selectedId != null) songs.filter { it.id == selectedId }
        else OfflineTrackSearch.match(query, songs)
}
