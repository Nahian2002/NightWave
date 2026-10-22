package com.nightwave.player.offline

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * User-initiated Commons discovery. No account, scraper, paid API or server.
 * Search returns only original Wikimedia Commons files marked CC0 / public
 * domain; normal YouTube search and playback remain completely separate.
 */
class FreeAudioCatalogClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    fun search(input: String): List<FreeAudioCandidate> {
        val term = FreeAudioCatalogPolicy.safeSearchTerm(input)
        require(term.isNotBlank()) { "Enter a song, instrument or artist name." }
        val address = "https://commons.wikimedia.org/w/api.php".toHttpUrl()
            .newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("format", "json")
            .addQueryParameter("formatversion", "2")
            .addQueryParameter("generator", "search")
            .addQueryParameter("gsrsearch", "filetype:audio $term")
            .addQueryParameter("gsrnamespace", "6")
            .addQueryParameter("gsrlimit", "40")
            .addQueryParameter("prop", "imageinfo")
            .addQueryParameter("iiprop", "url|size|mime|extmetadata|sha1")
            .addQueryParameter("iiextmetadatafilter", "LicenseShortName")
            .addQueryParameter("maxlag", "5")
            .build()
        val request = Request.Builder().url(address)
            .header("User-Agent",
                "NightWave/2.0 (Android; free music search; https://github.com/Nahian2002/NightWave)")
            .header("Accept", "application/json")
            .get().build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) {
                "Open audio catalog unavailable (HTTP ${response.code}). Retry later."
            }
            val inputStream = requireNotNull(response.body) { "Empty catalog response" }.byteStream()
            val bytes = ByteArrayOutputStream()
            inputStream.use { stream ->
                val buffer = ByteArray(8192)
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    require(bytes.size() + n <= 750_000) { "Catalog response too large" }
                    bytes.write(buffer, 0, n)
                }
            }
            return parseResults(bytes.toString("UTF-8"))
        }
    }

    companion object {
        /** Pure mapping, unit-testable with synthetic JSON without network. */
        fun parseResults(payload: String): List<FreeAudioCandidate> {
            val root = JSONObject(payload)
            root.optJSONObject("error")?.let {
                error("Open audio catalog unavailable: " +
                    it.optString("code").take(40))
            }
            val pages = root.optJSONObject("query")?.optJSONArray("pages")
                ?: return emptyList()
            val result = mutableListOf<FreeAudioCandidate>()
            val seen = mutableSetOf<String>()
            for (i in 0 until minOf(pages.length(), 50)) {
                val page = pages.optJSONObject(i) ?: continue
                if (page.optInt("ns") != 6) continue
                val title = page.optString("title")
                val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                val license = info.optJSONObject("extmetadata")
                    ?.optJSONObject("LicenseShortName")?.optString("value").orEmpty()
                if (!FreeAudioCatalogPolicy.permittedLicense(license)) continue
                val url = info.optString("url")
                val description = info.optString("descriptionurl")
                if (!FreeAudioCatalogPolicy.safeMediaUrl(url) ||
                    !FreeAudioCatalogPolicy.safeDescriptionUrl(description)) continue
                val bytes = info.optLong("size", -1L)
                if (!FreeAudioCatalogPolicy.validSize(bytes)) continue
                val sha1 = info.optString("sha1")
                if (!FreeAudioIntegrity.isValidSourceHash(sha1)) continue
                val saveName = FreeAudioCatalogPolicy.saveName(title, info.optString("mime"))
                    ?: continue
                if (!seen.add(url)) continue
                result.add(FreeAudioCandidate(
                    saveName.substringBeforeLast('.'), url, description,
                    license.take(70), bytes, saveName, sha1
                ))
                if (result.size >= 20) break
            }
            return result
        }
    }
}
