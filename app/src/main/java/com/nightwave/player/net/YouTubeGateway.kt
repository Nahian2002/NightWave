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
    private val alternate = CommunityVideoSearch()
    private val memoryCache = LinkedHashMap<String, Pair<Long, VideoSearchResults>>(12, .75f, true)

    @Synchronized private fun cached(key: String): VideoSearchResults? {
        val item = memoryCache[key] ?: return null
        return item.second.takeIf { android.os.SystemClock.elapsedRealtime() - item.first < 600_000 }
    }
    @Synchronized private fun remember(key: String, result: VideoSearchResults) {
        memoryCache[key] = android.os.SystemClock.elapsedRealtime() to result
        while (memoryCache.size > 12) memoryCache.remove(memoryCache.keys.first())
    }

    fun search(query: String, mode: SearchMode): VideoSearchResults {
        val text = query.trim().take(160)
        require(text.isNotBlank()) { "Please enter a song name or artist" }
        val key = mode.name + "|" + text.lowercase(java.util.Locale.ROOT)
        cached(key)?.let { return it }

        val diagnostics = mutableListOf<String>()
        // Existing extractor first. If a source returns no items *and* parsing
        // exceptions, don't retry three equivalent queries against the same
        // failing backend. Use the independent provider instead.
        val filters = listOf(
            YoutubeSearchQueryHandlerFactory.VIDEOS,
            YoutubeSearchQueryHandlerFactory.ALL
        )
        for (filter in filters) {
            val attempt = runCatching { searchWithFilter(text, filter) }
            val data = attempt.getOrNull()
            if (data != null) {
                val (videos, report) = data
                diagnostics.add(report)
                if (videos.isNotEmpty()) {
                    val ranked = if (mode == SearchMode.SONGS) sortMusicMatches(videos, text) else videos
                    val result = VideoSearchResults(ranked.take(maxResults), false, "YouTube search")
                    remember(key, result)
                    return result
                }
                if (report.contains("raw=0") && !report.contains("errors=0")) {
                    android.util.Log.w("NightWaveSearch", "Extractor parsing failed: " + report)
                    break
                }
            } else {
                val error = attempt.exceptionOrNull()
                diagnostics.add(filter + ": " + error?.javaClass?.simpleName)
                android.util.Log.w("NightWaveSearch", "Direct search failed; using alternate provider", error)
            }
        }

        // Only public title/artist search text is sent to one of the
        // documented independent Invidious community servers. No login data.
        val alternateResult = runCatching { alternate.search(text) }
        alternateResult.onSuccess { fallback ->
            if (fallback.videos.isNotEmpty()) {
                val ranked = if (mode == SearchMode.SONGS) sortMusicMatches(fallback.videos, text)
                    else fallback.videos
                val result = VideoSearchResults(
                    ranked.take(maxResults), true,
                    "Results via independent community search (" + fallback.server + ")"
                )
                remember(key, result)
                android.util.Log.i("NightWaveSearch", "Recovered " + ranked.size +
                    " search results through " + fallback.server)
                return result
            }
        }
        val failure = alternateResult.exceptionOrNull()
        android.util.Log.e("NightWaveSearch",
            "All search providers unavailable. Extractor: " + diagnostics.joinToString(" | "), failure)
        throw IllegalStateException(
            "Search providers are unavailable. Check your connection and retry. " +
                "Extractor: " + diagnostics.take(1).joinToString().take(105) +
                "; community: " + (failure?.message ?: "no results").take(125),
            failure
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
