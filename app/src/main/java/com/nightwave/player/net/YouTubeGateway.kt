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
    val note: String = "",
    val fromCache: Boolean = false
)

/** Public video search; never hides extraction failures as an ordinary empty result. */
class YouTubeGateway {
    private val maxResults = 55
    private val alternate = CommunityVideoSearch()
    private val memoryCache = PublicSearchResultCache()
    internal fun cachedSearch(query: String, mode: SearchMode): CachedPublicSearch? = memoryCache.get(query, mode)
    private fun remember(query: String, mode: SearchMode, result: VideoSearchResults, request: PublicSearchRequest?) {
        if (request == null) memoryCache.put(query, mode, result)
        else request.commitIfActive { memoryCache.put(query, mode, result) }
    }

    fun search(query: String, mode: SearchMode, forceRefresh: Boolean = false,
        onPhase: (PublicSearchPhase) -> Unit = {}): VideoSearchResults {
        val request = PublicSearchRequest.current.get()
        request?.checkActive()
        val text = query.trim().take(160)
        require(text.isNotBlank()) { "Please enter a song name or artist" }
        if (!forceRefresh) cachedSearch(text, mode)?.takeIf { it.fresh }?.let {
            request?.checkActive()
            request?.cacheHit()
            return it.result
        }
        request?.beginPhase(PublicSearchPhase.DIRECT)
        onPhase(PublicSearchPhase.DIRECT)

        val diagnostics = mutableListOf<String>()
        // Existing extractor first. If a source returns no items *and* parsing
        // exceptions, don't retry three equivalent queries against the same
        // failing backend. Use the independent provider instead.
        val filters = listOf(
            YoutubeSearchQueryHandlerFactory.VIDEOS,
            YoutubeSearchQueryHandlerFactory.ALL
        )
        for (filter in filters) {
            request?.checkActive()
            val attempt = runCatching { searchWithFilter(text, filter) }
            request?.checkActive()
            val data = attempt.getOrNull()
            if (data != null) {
                val (videos, report) = data
                diagnostics.add(report)
                if (videos.isNotEmpty()) {
                    val ranked = if (mode == SearchMode.SONGS) sortMusicMatches(videos, text) else videos
                    val result = VideoSearchResults(ranked.take(maxResults), false, "YouTube search")
                    request?.checkActive()
                    remember(text, mode, result, request)
                    return result
                }
                if (report.contains("raw=0") && !report.contains("errors=0")) {
                    android.util.Log.w("NightWaveSearch", "Extractor parsing failed: " + report)
                    break
                }
            } else {
                val error = attempt.exceptionOrNull()
                diagnostics.add(filter + ": " + error?.javaClass?.simpleName)
                // The ALL filter uses the same YouTube backend. A transport/
                // extractor exception isn't cured by issuing another nearly
                // identical 45-second request; fail over to the independent
                // public endpoint instead of leaving Search spinning.
                android.util.Log.w("NightWaveSearch",
                    "Direct extractor unavailable; trying independent public provider (" +
                        error?.javaClass?.simpleName + ")")
                break
            }
        }

        // Only public title/artist search text is sent to one of the
        // documented independent Invidious community servers. No login data.
        request?.checkActive()
        request?.beginPhase(PublicSearchPhase.COMMUNITY)
        onPhase(PublicSearchPhase.COMMUNITY)
        val alternateResult = runCatching { alternate.search(text) }
        request?.checkActive()
        alternateResult.onSuccess { fallback ->
            if (fallback.videos.isNotEmpty()) {
                val ranked = if (mode == SearchMode.SONGS) sortMusicMatches(fallback.videos, text)
                    else fallback.videos
                val result = VideoSearchResults(
                    ranked.take(maxResults), true,
                    "Results via independent community search (" + fallback.server + ")"
                )
                request?.checkActive()
                remember(text, mode, result, request)
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

    private fun sortMusicMatches(videos: List<VideoPreview>, query: String): List<VideoPreview> =
        SearchRelevancePolicy.rankPublic(query, videos)

    /**
     * Resolve only a distinct audio-only progressive HTTPS stream.
     * Never fall back to a video track, HLS/DASH manifest, DRM or account
     * cookies for an offline candidate. The download worker independently
     * validates bytes, media tracks and the temporary CDN endpoint.
     */
    fun resolveAudioOnlyForOffline(url: String): PlayableAudio {
        val canonical = VideoLink.normalize(url)
            ?: throw IllegalArgumentException("Invalid search track")
        val info = StreamInfo.getInfo(NewPipe.getService("YouTube"), canonical)
        val audio = info.audioStreams
            .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .maxByOrNull { it.averageBitrate.takeIf { rate -> rate > 0 } ?: 0 }
            ?: throw IllegalStateException("No independent audio-only stream is available")
        require(com.nightwave.player.offline.SearchTrackSavePolicy.safeAudioEndpoint(audio.content)) {
            "Audio source is unavailable for safe offline saving"
        }
        return PlayableAudio(
            VideoPreview(info.name, info.uploaderName ?: "Creator",
                canonical, info.thumbnails.firstOrNull()?.url ?: "", info.duration),
            audioUrl = audio.content
        )
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
            .filter { it.isUrl && !it.isVideoOnly() && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
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
