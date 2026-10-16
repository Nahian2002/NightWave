package com.nightwave.player.net

import com.nightwave.player.core.VideoLink
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Optional alternate public-video search when direct extractor results fail.
 *
 * Search text is sent to the public Invidious instance that responds.
 * Servers are from https://docs.invidious.io/instances/ (review/update periodically).
 * No Google login, cookies, tokens or stream URLs are sent. This is not a private backend.
 */
class CommunityVideoSearch(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .callTimeout(6, TimeUnit.SECONDS)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    private val instances = listOf(
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://invidious.tiekoetter.com",
        "https://yt.chocolatemoo53.com"
    )

    data class Result(val videos: List<VideoPreview>, val server: String)

    @Throws(IOException::class)
    fun search(query: String): Result {
        val failures = mutableListOf<String>()
        for (base in instances) {
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
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        failures.add(base.substringAfter("https://") + ": HTTP " + response.code)
                        return@use
                    }
                    val payload = response.body?.string().orEmpty()
                    if (payload.length > 3_000_000) throw IOException("Search response exceeded limit")
                    val parsed = InvidiousResults.parse(payload)
                    if (parsed.isNotEmpty()) return Result(parsed, base.substringAfter("https://"))
                    failures.add(base.substringAfter("https://") + ": empty response")
                }
            } catch (e: Exception) {
                failures.add(base.substringAfter("https://") + ": " + e.javaClass.simpleName)
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
                    durationSeconds = item.optLong("lengthSeconds", 0).coerceAtLeast(0)
                )
            )
            if (results.size == 45) break
        }
        return results
    }
}
