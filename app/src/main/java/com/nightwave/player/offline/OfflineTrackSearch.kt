package com.nightwave.player.offline

import java.text.Normalizer
import java.util.Locale

/**
 * Device-only search for imported and downloaded audio. The old public-video
 * quick results do not contain these tracks. No network requests or index edits.
 */
object OfflineTrackSearch {
    private val separators = Regex("[^\\p{L}\\p{M}\\p{N}]+")
    private fun normalized(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
            .replace(separators, " ").trim()

    fun match(query: String, songs: List<OfflineSong>): List<OfflineSong> {
        val clean = normalized(query.take(160))
        if (clean.isBlank()) return songs
        val terms = clean.split(' ').filter { it.isNotBlank() }.take(12)
        if (terms.isEmpty()) return songs
        return songs.withIndex().mapNotNull { indexed ->
            val title = normalized(indexed.value.title)
            // Include original file extension as a secondary lookup field.
            val extension = indexed.value.id.substringAfterLast('.', "")
            val searchable = "$title $extension"
            if (!terms.all { it in searchable }) null
            else {
                val rank = when {
                    title == clean -> 1000
                    title.startsWith(clean) -> 800
                    title.contains(clean) -> 600
                    else -> terms.count { title.startsWith(it) } * 30
                }
                Triple(indexed.value, rank, indexed.index)
            }
        }.sortedWith(compareByDescending<Triple<OfflineSong, Int, Int>> { it.second }
            .thenBy { it.third }).map { it.first }
    }
}
