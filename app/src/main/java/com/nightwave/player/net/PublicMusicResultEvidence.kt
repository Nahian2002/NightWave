package com.nightwave.player.net

import com.nightwave.player.core.VideoLink
import java.util.Locale

/** Display evidence only. Nothing here verifies a recording, owner or offline right. */
data class PublicMusicResultEvidence(
    val sourceAndDuration: String,
    val uploader: String,
    val titleLabels: String,
    val duplicateReference: String?
) {
    companion object {
        fun duration(seconds: Long): String = when {
            seconds <= 0 -> "Duration unavailable"
            seconds >= 3600 -> String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
            else -> String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
        }

        /** Compare visible evidence, so indistinguishable captions get an actual source ID. */
        fun forResults(videos: List<VideoPreview>, mode: SearchMode): Map<String, PublicMusicResultEvidence> {
            fun key(v: VideoPreview): List<String> {
                val identity = if (mode == SearchMode.VIDEOS) MusicIdentity.videoDisplay(v.title, v.author)
                    else MusicIdentity.display(v.title, v.author)
                return listOf(identity.title, v.author.trim(), duration(v.durationSeconds), labels(v))
                    .map { it.lowercase(Locale.ROOT) }
            }
            val collisions = videos.groupBy(::key)
            return videos.associate { video ->
                val source = if (VideoLink.normalize(video.url) != null) "YouTube video" else "Public video"
                val duplicate = collisions[key(video)].orEmpty().map { it.url }.distinct().size > 1
                val reference = if (duplicate) VideoLink.normalize(video.url)?.substringAfter("v=")
                    ?.let { "Video ID: $it" } ?: "Source: ${video.url}" else null
                video.url to PublicMusicResultEvidence(
                    "$source · ${duration(video.durationSeconds)}",
                    "Uploader: ${video.author.trim().ifEmpty { "Unknown uploader" }}",
                    labels(video), reference)
            }
        }

        private fun labels(video: VideoPreview): String = SearchVersionGroupingPolicy.titleLabels(video.title)
            .takeIf { it.isNotEmpty() }?.joinToString(prefix = "Title labels: ", separator = " · ").orEmpty()
    }
}
