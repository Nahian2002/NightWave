package com.nightwave.player.net

import com.nightwave.player.core.YouTubeReleasePolicy
import com.nightwave.player.core.VideoLink
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** Public, extraction-supported videos only. Private/DRM/region-restricted videos are not bypassed. */
data class VideoPreview(
    val title: String, val author: String, val url: String, val thumbnail: String,
    val durationSeconds: Long, val viewCount: Long? = null
)
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
    private val searchRouting = PublicSearchRouting()
    private val memoryCache = PublicSearchResultCache()
    internal fun cachedSearch(query: String, mode: SearchMode): CachedPublicSearch? = memoryCache.get(query, mode)
    private fun remember(query: String, mode: SearchMode, result: VideoSearchResults, request: PublicSearchRequest?) {
        if (request == null) memoryCache.put(query, mode, result)
        else request.commitIfActive { memoryCache.put(query, mode, result) }
    }

    fun search(query: String, mode: SearchMode, forceRefresh: Boolean = false,
        onPhase: (PublicSearchPhase) -> Unit = {}): VideoSearchResults {
        YouTubeReleasePolicy.requireLegacyAccess()
        val request = PublicSearchRequest.current.get()
        request?.checkActive()
        val text = query.trim().take(160)
        require(text.isNotBlank()) { "Please enter a song name or artist" }
        if (!forceRefresh) cachedSearch(text, mode)?.takeIf { it.fresh }?.let {
            request?.checkActive()
            request?.cacheHit()
            return it.result
        }
        val diagnostics = mutableListOf<String>()
        var directFailed = false
        var communityFailure: Throwable? = null
        for (phase in searchRouting.order()) {
            request?.checkActive()
            val response = when (phase) {
                PublicSearchPhase.DIRECT -> {
                    request?.beginPhase(PublicSearchPhase.DIRECT)
                    onPhase(PublicSearchPhase.DIRECT)
                    val direct = searchDirect(text, request, diagnostics)
                    directFailed = direct.unavailable
                    direct.videos.takeIf { it.isNotEmpty() }?.let { it to "YouTube search" }
                }
                PublicSearchPhase.COMMUNITY -> {
                    request?.beginPhase(PublicSearchPhase.COMMUNITY)
                    onPhase(PublicSearchPhase.COMMUNITY)
                    val attempt = runCatching { alternate.search(text) }
                    communityFailure = attempt.exceptionOrNull()
                    attempt.getOrNull()?.let { fallback ->
                        android.util.Log.i("NightWaveSearch", "Recovered " + fallback.videos.size +
                            " search results through " + fallback.server)
                        fallback.videos to "Results via independent community search (" + fallback.server + ")"
                    }
                }
            }
            request?.checkActive()
            if (response == null) continue
            val ranked = rankMatches(response.first, text, mode)
            val result = VideoSearchResults(ranked.take(maxResults), phase == PublicSearchPhase.COMMUNITY, response.second)
            if (request == null) searchRouting.recordSuccess(phase, directFailed)
            else request.commitIfActive { searchRouting.recordSuccess(phase, directFailed) }
            remember(text, mode, result, request)
            return result
        }

        android.util.Log.e("NightWaveSearch",
            "All search providers unavailable. Extractor: " + diagnostics.joinToString(" | "), communityFailure)
        throw IllegalStateException(
            "Search providers are unavailable. Check your connection and retry. " +
                "Extractor: " + diagnostics.take(1).joinToString().take(105) +
                "; community: " + (communityFailure?.message ?: "no results").take(125),
            communityFailure
        )
    }

    private data class DirectAttempt(val videos: List<VideoPreview>, val unavailable: Boolean = false)

    private fun searchDirect(
        text: String, request: PublicSearchRequest?, diagnostics: MutableList<String>
    ): DirectAttempt {
        val filters = listOf(YoutubeSearchQueryHandlerFactory.VIDEOS, YoutubeSearchQueryHandlerFactory.ALL)
        for (filter in filters) {
            request?.checkActive()
            val attempt = runCatching { searchWithFilter(text, filter) }
            request?.checkActive()
            val (videos, report) = attempt.getOrElse { error ->
                diagnostics.add(filter + ": " + error.javaClass.simpleName)
                android.util.Log.w("NightWaveSearch",
                    "Direct extractor unavailable (" + error.javaClass.simpleName + ")")
                return DirectAttempt(emptyList(), unavailable = true)
            }
            diagnostics.add(report)
            if (videos.isNotEmpty()) return DirectAttempt(videos)
            // An empty result alone is query-specific. Only parser or transport
            // failure is evidence that the route itself needs a short break.
            if (report.contains("raw=0") && !report.contains("errors=0")) {
                android.util.Log.w("NightWaveSearch", "Extractor parsing failed: " + report)
                return DirectAttempt(emptyList(), unavailable = true)
            }
        }
        return DirectAttempt(emptyList())
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
                durationSeconds = item.duration,
                viewCount = item.viewCount.takeIf { it >= 0 }
            )
        }.distinctBy { it.url }
        val report = filter + ": raw=" + raw.size + ", streams=" + videos.size +
            ", linksAccepted=" + parsed.size + ", linksRejected=" + skipped +
            ", errors=" + info.errors.size
        android.util.Log.d("NightWaveSearch", report)
        return parsed to report
    }

    private fun rankMatches(videos: List<VideoPreview>, query: String, mode: SearchMode): List<VideoPreview> =
        if (mode == SearchMode.SONGS) SearchRelevancePolicy.rankPublic(query, videos)
        else SearchRelevancePolicy.rankVideos(query, videos)

    /**
     * Resolve only a distinct audio-only progressive HTTPS stream.
     * Never fall back to a video track, HLS/DASH manifest, DRM or account
     * cookies for an offline candidate. The download worker independently
     * validates bytes, media tracks and the temporary CDN endpoint.
     */
    fun resolveAudioOnlyForOffline(url: String): PlayableAudio {
        YouTubeReleasePolicy.requireLegacyAccess()
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
        YouTubeReleasePolicy.requireLegacyAccess()
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
