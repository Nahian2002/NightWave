package com.nightwave.player.net

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
        .followRedirects(true)
        .build()

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val method = request.httpMethod().uppercase(Locale.ROOT)
        val bytes = request.dataToSend()
        val requestBuilder = HttpRequest.Builder().url(request.url())
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
