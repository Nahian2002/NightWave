package com.nightwave.player.offline

/**
 * Device-only ranking across all imported/downloaded audio metadata.
 * No network access or index mutation; preserve stable saved order on ties.
 */
object OfflineTrackSearch {
    fun match(query: String, songs: List<OfflineSong>): List<OfflineSong> {
        val terms = OfflineSearchRanking.terms(query)
        if (terms.isEmpty()) return if (query.isBlank()) songs else emptyList()
        return songs.withIndex().mapNotNull { (index, song) ->
            val score = OfflineSearchRanking.score(terms, song.title,
                song.id.substringAfterLast('.', ""))
            if (score < 0) null else Triple(song, score, index)
        }.sortedWith(compareByDescending<Triple<OfflineSong, Int, Int>> { it.second }
            .thenBy { it.third }).map { it.first }
    }
}
