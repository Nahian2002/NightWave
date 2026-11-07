package com.nightwave.player.net

import com.nightwave.player.core.YouTubeReleasePolicy
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request as HttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Implements the downloader contract for NewPipeExtractor. Never runs on the UI thread. */
class ExtractorDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .callTimeout(45, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        YouTubeReleasePolicy.requireUnofficialSearch()
        val url = request.url().toHttpUrl()
        check(PublicSearchRequest.current.get() != null && allowsNewPipeSearch(url)) {
            "The NewPipe transport is only available for metadata search."
        }
        val method = request.httpMethod().uppercase(Locale.ROOT)
        val bytes = request.dataToSend()
        val requestBuilder = HttpRequest.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/121.0 Mobile Safari/537.36")
        request.headers()?.forEach { (name, values) ->
            requestBuilder.removeHeader(name)
            values.forEach { value -> requestBuilder.addHeader(name, value) }
        }
        val payload = bytes?.toRequestBody(null)
            ?: if (method in setOf("POST", "PUT", "PATCH")) ByteArray(0).toRequestBody(null) else null
        requestBuilder.method(method, payload)
        return PublicSearchRequest.executeHttp(client.newCall(requestBuilder.build())) { res ->
            Response(
                res.code, res.message, res.headers.toMultimap(),
                res.body?.string() ?: "", res.request.url.toString()
            )
        }
    }
}


/** Search and its web-client metadata bootstrap only. Media endpoints are excluded. */
internal fun allowsNewPipeSearch(url: HttpUrl): Boolean =
    url.isHttps && url.port == 443 && url.username.isEmpty() && url.password.isEmpty() &&
        url.host in setOf("www.youtube.com", "m.youtube.com", "youtube.com", "www.youtube-nocookie.com") &&
        url.encodedPath in setOf("/", "/results", "/sw.js", "/youtubei/v1/guide", "/youtubei/v1/search")
