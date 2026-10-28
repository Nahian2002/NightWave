package com.nightwave.player.offline

/** Prepared full-corpus metadata only; no file readiness or mutation claim. */
object OfflineTrackSearch {
    class Prepared internal constructor(private val songs: List<OfflineSong>,
        private val identities: List<OfflineSearchRanking.Prepared>) {
        fun match(query: String, checkpoint: () -> Unit = {}): List<OfflineSong> {
            val terms = OfflineSearchRanking.terms(query)
            if (terms.isEmpty()) return if (query.isBlank()) songs else emptyList()
            return songs.withIndex().mapNotNull { (i, song) ->
                if (i % 128 == 0) checkpoint()
                val score = OfflineSearchRanking.score(terms, identities[i])
                if (score < 0) null else Triple(song, score, i)
            }.sortedWith(compareByDescending<Triple<OfflineSong, Int, Int>> { it.second }
                .thenBy { it.third }).map { it.first }.also { checkpoint() }
        }
    }
    fun prepare(songs: List<OfflineSong>, checkpoint: () -> Unit = {}): Prepared {
        val copy = songs.toList()
        return Prepared(copy, copy.mapIndexed { i, song ->
            if (i % 128 == 0) checkpoint()
            OfflineSearchRanking.prepare(song.title, song.id.substringAfterLast('.', ""))
        })
    }
    fun match(query: String, songs: List<OfflineSong>): List<OfflineSong> {
        if (query.isBlank()) return songs
        return prepare(songs).match(query)
    }
}
