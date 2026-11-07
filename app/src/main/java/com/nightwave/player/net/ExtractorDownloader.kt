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

    private val playbackClient = client.newBuilder().callTimeout(8, TimeUnit.SECONDS).build()

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val url = request.url().toHttpUrl()
        val method = request.httpMethod().uppercase(Locale.ROOT)
        val playback = playbackScope.get() == true
        if (playback) {
            check(YouTubeReleasePolicy.ownerPlaybackEnabled && allowsNewPipePlayback(url, method)) {
                "Unsupported YouTube playback lookup endpoint."
            }
            if (Thread.currentThread().isInterrupted) throw IOException("Audio lookup cancelled")
        } else {
            YouTubeReleasePolicy.requireUnofficialSearch()
            check(PublicSearchRequest.current.get() != null && allowsNewPipeSearch(url)) {
                "The NewPipe transport is only available for metadata search."
            }
        }
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
        val outgoing = requestBuilder.build()
        if (playback) return playbackClient.newCall(outgoing).execute().use(::extractorResponse)
        return PublicSearchRequest.executeHttp(client.newCall(outgoing), ::extractorResponse)
    }

    private fun extractorResponse(res: okhttp3.Response) = Response(
        res.code, res.message, res.headers.toMultimap(),
        res.body?.string() ?: "", res.request.url.toString()
    )

    companion object {
        private val playbackScope = ThreadLocal<Boolean>()

        internal fun <T> forPlayback(block: () -> T): T {
            check(YouTubeReleasePolicy.ownerPlaybackEnabled)
            check(playbackScope.get() != true)
            playbackScope.set(true)
            try {
                return block()
            } finally {
                playbackScope.remove()
            }
        }
    }
}


/** Search and its web-client metadata bootstrap only. Media endpoints are excluded. */
internal fun allowsNewPipeSearch(url: HttpUrl): Boolean =
    url.isHttps && url.port == 443 && url.username.isEmpty() && url.password.isEmpty() &&
        url.host in setOf("www.youtube.com", "m.youtube.com", "youtube.com", "www.youtube-nocookie.com") &&
        url.encodedPath in setOf("/", "/results", "/sw.js", "/youtubei/v1/guide", "/youtubei/v1/search")

private val playerPaths = setOf(
    "/youtubei/v1/visitor_id", "/youtubei/v1/reel/reel_item_watch",
    "/youtubei/v1/player", "/youtubei/v1/next"
)
private val playerScript = Regex("/s/player/[A-Za-z0-9_-]+/[A-Za-z0-9_./-]+/base\\.js")

internal fun allowsNewPipePlayback(url: HttpUrl, method: String): Boolean {
    if (!url.isHttps || url.port != 443 || url.username.isNotEmpty() ||
        url.password.isNotEmpty() || url.fragment != null) return false
    if (url.host != "www.youtube.com" && url.host != "youtubei.googleapis.com") return false
    return when (method) {
        "POST" -> url.encodedPath in playerPaths
        "GET" -> url.host == "www.youtube.com" &&
            (url.encodedPath in setOf("/watch", "/iframe_api") || playerScript.matches(url.encodedPath))
        else -> false
    }
}

internal fun isYouTubeAudioHost(url: HttpUrl): Boolean = url.host.endsWith(".googlevideo.com")

internal fun allowsYouTubeAudio(url: HttpUrl): Boolean =
    url.isHttps && url.port == 443 && url.username.isEmpty() && url.password.isEmpty() &&
        url.fragment == null && isYouTubeAudioHost(url) && url.encodedPath == "/videoplayback"
