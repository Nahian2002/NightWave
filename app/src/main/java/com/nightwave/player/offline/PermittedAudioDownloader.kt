package com.nightwave.player.offline

import android.content.Context
import okhttp3.Call
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bounded, foreground user-initiated downloader for direct permitted audio
 * files. No YouTube extractor, stream URL caching, cookies or DRM workarounds.
 */
class PermittedAudioDownloader(private val context: Context) {
    private val cancelled = AtomicBoolean(false)
    @Volatile private var currentCall: Call? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.MINUTES)
        .followRedirects(false)
        .followSslRedirects(false)
        .dns(Dns { hostname ->
            val resolved = Dns.SYSTEM.lookup(hostname)
            require(resolved.isNotEmpty() && resolved.all(::isPublicAddress)) {
                "Private or unsafe network address blocked"
            }
            resolved
        })
        .build()

    fun cancel() {
        cancelled.set(true)
        currentCall?.cancel()
    }

    fun download(
        source: PermittedAudioSource,
        progress: (downloaded: Long, total: Long?) -> Unit
    ): OfflineSong {
        check(!cancelled.get()) { "Download was cancelled" }
        val home = context.applicationContext.filesDir.canonicalFile
        val staging = File(home, "permitted_download_staging")
        require(!Files.isSymbolicLink(staging.toPath())) { "Unsafe temporary folder" }
        require(staging.isDirectory || staging.mkdirs()) { "Cannot create download folder" }
        require(staging.canonicalFile.parentFile == home) { "Unsafe download path" }
        val pending = File(staging, UUID.randomUUID().toString() + ".pending")
        try {
            val request = Request.Builder().url(source.url)
                .header("Accept", "audio/*,application/octet-stream")
                .header("Cache-Control", "no-store")
                .get().build()
            val call = client.newCall(request)
            currentCall = call
            call.execute().use { response ->
                require(response.code == 200) {
                    if (response.isRedirect) "Redirected audio URLs are not supported."
                    else "Audio server returned HTTP ${response.code}."
                }
                require(PermittedAudioDownloadPolicy.supportedContentType(
                    response.header("Content-Type"), source.extension)) {
                    "The link did not return the expected audio format."
                }
                val body = requireNotNull(response.body) { "Empty download response" }
                val length = body.contentLength()
                require(length <= PermittedAudioDownloadPolicy.MAX_BYTES) {
                    "File is larger than the 250 MB song limit."
                }
                var received = 0L
                val prefix = ByteArray(16)
                var prefixCount = 0
                body.byteStream().use { stream ->
                    FileOutputStream(pending).use { output ->
                        val buffer = ByteArray(32_768)
                        while (true) {
                            check(!cancelled.get()) { "Download cancelled" }
                            val n = stream.read(buffer)
                            if (n < 0) break
                            received += n
                            require(received <= PermittedAudioDownloadPolicy.MAX_BYTES) {
                                "File exceeds the 250 MB song limit."
                            }
                            if (prefixCount < prefix.size) {
                                val bytes = minOf(n, prefix.size - prefixCount)
                                buffer.copyInto(prefix, prefixCount, 0, bytes)
                                prefixCount += bytes
                            }
                            output.write(buffer, 0, n)
                            progress(received, length.takeIf { it > 0 })
                        }
                        output.fd.sync()
                    }
                }
                check(!cancelled.get()) { "Download cancelled" }
                require(received > 0 && (length < 0 || received == length)) {
                    "Audio download was incomplete."
                }
                require(PermittedAudioDownloadPolicy.audioSignatureMatches(
                    prefix.copyOf(prefixCount), source.extension)) {
                    "The downloaded file did not match the declared audio format."
                }
            }
            check(!cancelled.get()) { "Download cancelled" }
            return OfflineAudioLibrary(context).importPermittedStagedAudio(pending, source.fileName)
        } finally {
            currentCall = null
            // Only this download's private staging file is removed. Existing
            // imported songs, other downloads and personal files are untouched.
            pending.delete()
        }
    }

    private fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress ||
            address.isLinkLocalAddress || address.isSiteLocalAddress ||
            address.isMulticastAddress) return false
        val octets = address.address
        // IPv6 unique local (fc00::/7), documentation (2001:db8::/32),
        // and IPv4 carrier-grade NAT / test networks are not remote media.
        if (octets.size == 16) {
            if ((octets[0].toInt() and 0xFE) == 0xFC) return false
            if ((octets[0].toInt() and 0xFF) == 0x20 &&
                (octets[1].toInt() and 0xFF) == 0x01 &&
                (octets[2].toInt() and 0xFF) == 0x0D &&
                (octets[3].toInt() and 0xFF) == 0xB8) return false
        }
        if (octets.size == 4) {
            val a = octets[0].toInt() and 255
            val b = octets[1].toInt() and 255
            if (a == 0 || a >= 224 || a == 100 && b in 64..127 ||
                a == 169 && b == 254 || a == 192 && b == 0 ||
                a == 198 && b in 18..19 || a == 198 && b == 51 ||
                a == 203 && b == 0) return false
        }
        return true
    }
}
