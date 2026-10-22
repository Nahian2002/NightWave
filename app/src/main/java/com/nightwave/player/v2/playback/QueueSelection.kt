package com.nightwave.player.v2.playback

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview

data class QueueSelection(val songs: List<VideoPreview>, val selectedIndex: Int)

/**
 * Validates and bounds a requested music collection before sending it to the
 * foreground service. Never chooses another song when the selected track is
 * past the 100-item service cap. Keep a contiguous window around the
 * selected track so Next and Previous still follow its actual neighbours.
 */
object QueueSelectionRules {
    fun plan(selected: VideoPreview, collection: List<VideoPreview>, capacity: Int = 100): QueueSelection {
        require(capacity in 1..100)
        val selectedUrl = VideoLink.normalize(selected.url) ?: return QueueSelection(emptyList(), -1)
        val unique = collection.mapNotNull { song ->
            val url = VideoLink.normalize(song.url) ?: return@mapNotNull null
            song.copy(url = url)
        }.distinctBy { it.url }
        val selectedAt = unique.indexOfFirst { it.url == selectedUrl }
        val bounded = when {
            // A song selected outside this collection must still play, but
            // has no meaningful neighbours from the supplied collection.
            selectedAt < 0 -> listOf(selected.copy(url = selectedUrl)) +
                unique.take(capacity - 1)
            unique.size <= capacity -> unique
            else -> {
                // A 100-track cap must not break Next/Previous when a listener
                // taps song 150 in a long playlist. Retain nearby neighbours
                // in their original order, instead of moving it to row zero.
                val start = (selectedAt - capacity / 2)
                    .coerceIn(0, unique.size - capacity)
                unique.subList(start, start + capacity)
            }
        }
        return QueueSelection(bounded, bounded.indexOfFirst { it.url == selectedUrl })
    }
}
