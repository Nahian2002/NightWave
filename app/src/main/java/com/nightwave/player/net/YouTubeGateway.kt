package com.nightwave.player.net

import com.nightwave.player.core.VideoLink
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** Public, extraction-supported videos only. Private/DRM/region-restricted videos are not bypassed. */
data class VideoPreview(val title: String, val author: String, val url: String, val thumbnail: String, val durationSeconds: Long)
data class PlayableAudio(val video: VideoPreview, val audioUrl: String, val mimeType: String? = null)

class YouTubeGateway {
    fun search(query: String): List<VideoPreview> {
        val service = NewPipe.getService("YouTube")
        val result = SearchInfo.getInfo(service.getSearchExtractor(query))
        return result.relatedItems.filterIsInstance<StreamInfoItem>().map { item ->
            VideoPreview(
                title = item.name,
                author = item.uploaderName ?: "YouTube creator",
                url = item.url,
                thumbnail = item.thumbnails.firstOrNull()?.url ?: "",
                durationSeconds = item.duration
            )
        }
    }

    fun resolve(url: String): PlayableAudio {
        val canonical = VideoLink.normalize(url) ?: throw IllegalArgumentException("Not a supported YouTube video link")
        val info = StreamInfo.getInfo(NewPipe.getService("YouTube"), canonical)
        // Prefer separate audio (least data). Support combined video+audio and live manifests
        // when no direct audio-only variant is exposed. Never attempt to bypass DRM.
        val audio = info.audioStreams
            .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .maxByOrNull { it.averageBitrate.takeIf { br -> br >= 0 } ?: 0 }
        val combined = if (audio == null) info.videoStreams
            .filter { it.isUrl && !it.isVideoOnly && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .minByOrNull { if (it.height > 0) it.height else Int.MAX_VALUE } else null
        val playableUrl: String
        val mimeType: String?
        when {
            audio != null -> { playableUrl = audio.content; mimeType = null }
            combined != null -> { playableUrl = combined.content; mimeType = null }
            info.hlsUrl?.startsWith("https://") == true -> {
                playableUrl = info.hlsUrl; mimeType = "application/x-mpegURL"
            }
            info.dashMpdUrl?.startsWith("https://") == true -> {
                playableUrl = info.dashMpdUrl; mimeType = "application/dash+xml"
            }
            else -> throw IllegalStateException("No supported public audio stream for this video")
        }
        return PlayableAudio(
            VideoPreview(
                title = info.name,
                author = info.uploaderName ?: "YouTube creator",
                url = canonical,
                thumbnail = info.thumbnails.firstOrNull()?.url ?: "",
                durationSeconds = info.duration
            ),
            audioUrl = playableUrl,
            mimeType = mimeType
        )
    }
}
