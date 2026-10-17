package com.nightwave.player.net

import java.util.Locale

/**
 * Device-local matching only; does not contact a provider, inspect imported
 * audio file paths, or mutate playlists. Results are regular saved public
 * videos already playable through the existing NightWave queue.
 */
object SavedTrackSearch {
    fun match(query: String, tracks: List<VideoPreview>, limit: Int = 24): List<VideoPreview> {
        val text = query.trim().lowercase(Locale.ROOT).take(160)
        val terms = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        return tracks.withIndex()
            .filter { (_, track) ->
                val label = MusicIdentity.display(track.title, track.author)
                // Search both the original metadata and the safe display-only
                // labels. Never modify VideoPreview identity or saved queue.
                val haystack = (track.title + " " + track.author + " " +
                    label.title + " " + label.artistOrCreator).lowercase(Locale.ROOT)
                terms.all { term -> haystack.contains(term) }
            }
            .distinctBy { it.value.url }
            .sortedWith(
                compareByDescending<IndexedValue<VideoPreview>> {
                    val label = MusicIdentity.display(it.value.title, it.value.author)
                    val title = label.title.lowercase(Locale.ROOT)
                    val artist = label.artistOrCreator.lowercase(Locale.ROOT)
                    when {
                        title == text -> 4
                        title.startsWith(text) -> 3
                        title.contains(text) -> 2
                        artist.contains(text) -> 1
                        else -> 0
                    }
                }.thenBy { it.index }
            )
            .take(limit.coerceIn(1, 24))
            .map { it.value }
    }
}
