package com.nightwave.player.net

import com.nightwave.player.core.VideoLink
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** Public, extraction-supported videos only. Private/DRM/region-restricted videos are not bypassed. */
data class VideoPreview(val title: String, val author: String, val url: String, val thumbnail: String, val durationSeconds: Long)
data class PlayableAudio(val video: VideoPreview, val audioUrl: String, val mimeType: String? = null)

enum class SearchMode { SONGS, VIDEOS }
data class VideoSearchResults(
    val videos: List<VideoPreview>,
    val usedVideoFallback: Boolean,
    val note: String = ""
)

/** Public video search; never hides extraction failures as an ordinary empty result. */
class YouTubeGateway {
    private val maxResults = 55

    fun search(query: String, mode: SearchMode): VideoSearchResults {
        val text = query.trim()
        require(text.isNotBlank()) { "Please enter a song name or artist" }
        val diagnostics = mutableListOf<String>()
        val errors = mutableListOf<Throwable>()

        // Normal video search is primary. YouTube Music filters may return no playable streams.
        val filters = listOf(
            YoutubeSearchQueryHandlerFactory.VIDEOS,
            YoutubeSearchQueryHandlerFactory.ALL,
            YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
        )
        var found = emptyList<VideoPreview>()
        var winningFilter = ""
        for (filter in filters) {
            val attempt = runCatching { searchWithFilter(text, filter) }
            attempt.onSuccess { (videos, report) ->
                diagnostics.add(report)
                if (videos.isNotEmpty()) {
                    found = videos
                    winningFilter = filter
                }
            }.onFailure { error ->
                errors.add(error)
                diagnostics.add(filter + ": " + error.javaClass.simpleName + ": " + error.message?.take(110))
            }
            if (found.isNotEmpty()) break
        }

        if (found.isEmpty()) {
            android.util.Log.w("NightWaveSearch", "No results for: " + text + " | " + diagnostics.joinToString(" | "))
            val reason = if (errors.size == filters.size) "YouTube search is unavailable right now."
                else "Search returned no playable video entries."
            throw IllegalStateException(reason + " Try again or update NightWave. Details: " +
                diagnostics.take(2).joinToString(" • ").take(185), errors.firstOrNull())
        }
        android.util.Log.i("NightWaveSearch", "Search success: " + text + " " + winningFilter + " | " + diagnostics.joinToString(" | "))
        val ranked = if (mode == SearchMode.SONGS) sortMusicMatches(found, text) else found
        return VideoSearchResults(
            ranked.take(maxResults),
            winningFilter != YoutubeSearchQueryHandlerFactory.MUSIC_SONGS,
            if (mode == SearchMode.SONGS) "Top song and video matches" else "Public YouTube videos"
        )
    }

    private fun searchWithFilter(query: String, filter: String): Pair<List<VideoPreview>, String> {
        val service = NewPipe.getService("YouTube")
        val extractor = service.getSearchExtractor(query, listOf(filter), "")
        val info = SearchInfo.getInfo(extractor)
        val raw = info.relatedItems
        val videos = raw.filterIsInstance<StreamInfoItem>()
        var skipped = 0
        val parsed = videos.mapNotNull { item ->
            val url = VideoLink.normalizeSearchResult(item.url)
            if (url == null) {
                skipped++
                return@mapNotNull null
            }
            VideoPreview(
                title = item.name.ifBlank { "Untitled video" },
                author = item.uploaderName ?: "YouTube creator",
                url = url,
                thumbnail = item.thumbnails.firstOrNull()?.url ?: "",
                durationSeconds = item.duration
            )
        }.distinctBy { it.url }
        val report = filter + ": raw=" + raw.size + ", streams=" + videos.size +
            ", linksAccepted=" + parsed.size + ", linksRejected=" + skipped +
            ", errors=" + info.errors.size
        android.util.Log.d("NightWaveSearch", report)
        return parsed to report
    }

    private fun sortMusicMatches(videos: List<VideoPreview>, query: String): List<VideoPreview> {
        val lowerQuery = query.lowercase(java.util.Locale.ROOT)
        val terms = lowerQuery.split(Regex("\\s+")).filter { it.length > 1 }
        fun score(item: VideoPreview): Int {
            val title = item.title.lowercase(java.util.Locale.ROOT)
            val author = item.author.lowercase(java.util.Locale.ROOT)
            var rank = terms.count { title.contains(it) } * 7 + terms.count { author.contains(it) } * 3
            if (title.contains(lowerQuery)) rank += 35
            if (title.contains("official audio") || title.contains("official music video")) rank += 7
            if (title.contains("lyrics") || title.contains("lyric video")) rank += 2
            if (item.durationSeconds in 90..660) rank += 4
            if (title.contains("reaction") || title.contains("tutorial") || title.contains("review")) rank -= 8
            return rank
        }
        return videos.withIndex().sortedWith(
            compareByDescending<IndexedValue<VideoPreview>> { score(it.value) }.thenBy { it.index }
        ).map { it.value }
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
