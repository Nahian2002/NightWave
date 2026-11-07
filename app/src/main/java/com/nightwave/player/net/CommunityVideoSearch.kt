package com.nightwave.player.net

import com.nightwave.player.core.VideoLink
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Retained search transport for offline fixtures. The app configures no remote instances.
 */
class CommunityVideoSearch(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .callTimeout(4, TimeUnit.SECONDS)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build(),
    private val instances: List<String> = emptyList()
) {

    data class Result(val videos: List<VideoPreview>, val server: String)

    @Volatile private var lastSuccessfulInstance: String? = null

    @Throws(IOException::class)
    fun search(query: String): Result {
        val failures = mutableListOf<String>()
        val preferred = lastSuccessfulInstance
        val ordered = if (preferred == null) instances else listOf(preferred) + (instances - preferred)
        val requestScope = PublicSearchRequest.current.get()
        for (base in ordered) {
            PublicSearchRequest.current.get()?.checkActive()
            try {
                val url = (base + "/api/v1/search").toHttpUrl().newBuilder()
                    .addQueryParameter("q", query)
                    .addQueryParameter("type", "video")
                    .addQueryParameter("sort", "relevance")
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", "NightWave/0.7 (Android)")
                    .get()
                    .build()
                val found = PublicSearchRequest.executeHttp(client.newCall(request)) { response ->
                    if (!response.isSuccessful) {
                        failures.add(base.substringAfter("://") + ": HTTP " + response.code)
                        return@executeHttp null
                    }
                    val source = response.body?.source() ?: return@executeHttp null
                    if (source.request(3_000_001L)) throw IOException("Search response exceeded limit")
                    val payload = source.buffer.readUtf8()
                    val parsed = InvidiousResults.parse(payload)
                    if (parsed.isNotEmpty()) return@executeHttp Result(parsed, base.substringAfter("://"))
                    failures.add(base.substringAfter("://") + ": empty response")
                    null
                }
                if (found != null) {
                    if (requestScope == null) lastSuccessfulInstance = base
                    else requestScope.commitIfActive { lastSuccessfulInstance = base }
                    return found
                }
            } catch (e: Exception) {
                PublicSearchRequest.current.get()?.checkActive()
                failures.add(base.substringAfter("://") + ": " + e.javaClass.simpleName)
            }
        }
        throw IOException("All independent search endpoints failed (" +
            failures.joinToString("; ").take(200) + ")")
    }
}

/** Parser is separated from HTTP so we can test it against fixed JSON fixtures. */
object InvidiousResults {
    fun parse(payload: String): List<VideoPreview> {
        val data = JSONArray(payload)
        val results = mutableListOf<VideoPreview>()
        val ids = hashSetOf<String>()
        for (i in 0 until minOf(data.length(), 100)) {
            val item = data.optJSONObject(i) ?: continue
            if (item.optString("type") != "video") continue
            if (item.optBoolean("paid") || item.optBoolean("premium")) continue
            val id = item.optString("videoId")
            val canonical = VideoLink.normalizeSearchResult(id) ?: continue
            if (!ids.add(canonical)) continue
            val title = item.optString("title").take(220).ifBlank { "Untitled video" }
            val author = item.optString("author").take(110).ifBlank { "YouTube creator" }
            results.add(
                VideoPreview(
                    title = title,
                    author = author,
                    url = canonical,
                    thumbnail = "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg",
                    durationSeconds = item.optLong("lengthSeconds", 0).coerceAtLeast(0),
                    viewCount = item.optLong("viewCount", -1).takeIf { it >= 0 }
                )
            )
            if (results.size == 45) break
        }
        return results
    }
}
