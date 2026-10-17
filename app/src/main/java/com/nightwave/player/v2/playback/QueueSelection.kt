package com.nightwave.player.v2.playback

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview

data class QueueSelection(val songs: List<VideoPreview>, val selectedIndex: Int)

/**
 * Validates and bounds a requested music collection before sending it to the
 * foreground service. Never chooses another song when the selected track is
 * past the 100-item service cap.
 */
object QueueSelectionRules {
    fun plan(selected: VideoPreview, collection: List<VideoPreview>, capacity: Int = 100): QueueSelection {
        require(capacity in 1..100)
        val selectedUrl = VideoLink.normalize(selected.url) ?: return QueueSelection(emptyList(), -1)
        val unique = (collection + selected).mapNotNull { song ->
            val url = VideoLink.normalize(song.url) ?: return@mapNotNull null
            song.copy(url = url)
        }.distinctBy { it.url }
        val bounded = if (unique.take(capacity).any { it.url == selectedUrl }) unique.take(capacity)
            else listOf(selected.copy(url = selectedUrl)) + unique.filter { it.url != selectedUrl }.take(capacity - 1)
        return QueueSelection(bounded, bounded.indexOfFirst { it.url == selectedUrl })
    }
}
