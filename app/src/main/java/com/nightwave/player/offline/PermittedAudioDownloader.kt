package com.nightwave.player.offline

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import okhttp3.Call
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.URI
import java.io.IOException
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

    /**
     * Clean-start bounded retry rather than appending unknown partial ranges.
     * On any failure/cancellation, only this private staging file is deleted.
     */
    fun download(
        source: PermittedAudioSource,
        expectedSourceSha1: String? = null,
        expectedSourceBytes: Long? = null,
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
            repeat(3) { attempt ->
                check(!cancelled.get()) { "Download cancelled" }
                require(!pending.exists() || pending.delete()) {
                    "Could not reset partial audio transfer"
                }
                val fileName = try {
                    transferOnce(source, pending, progress)
                } catch (network: IOException) {
                    if (cancelled.get() || attempt == 2) throw network
                    progress(0, null)
                    return@repeat
                }
                check(!cancelled.get()) { "Download cancelled" }
                if (expectedSourceBytes != null) {
                    require(expectedSourceBytes in 1..PermittedAudioDownloadPolicy.MAX_BYTES &&
                        pending.length() == expectedSourceBytes) {
                        "The downloaded file size does not match the catalog. Search again."
                    }
                }
                if (expectedSourceSha1 != null) {
                    require(FreeAudioIntegrity.sourceMatches(pending, expectedSourceSha1)) {
                        "Audio bytes do not match the original catalog file. Nothing was saved."
                    }
                }
                inspectAudioOnly(pending)
                check(!cancelled.get()) { "Download cancelled" }
                return OfflineAudioLibrary(context).importPermittedStagedAudio(pending, fileName)
            }
            error("Download did not finish")
        } finally {
            currentCall?.cancel()
            currentCall = null
            pending.delete()
        }
    }

    /**
     * Follow a small number of fully validated HTTPS redirects. OkHttp never
     * follows any redirects automatically. Every hop is checked for URL
     * restrictions and DNS never connects to private/reserved IP addresses.
     */
    private fun transferOnce(
        source: PermittedAudioSource, pending: File,
        progress: (downloaded: Long, total: Long?) -> Unit
    ): String {
        var url = source.url
        repeat(5) { redirects ->
            check(!cancelled.get()) { "Download cancelled" }
            url = PermittedAudioDownloadPolicy.prepare(url, "").url
            val request = Request.Builder().url(url)
                .header("Accept", "audio/*,application/octet-stream")
                .header("Cache-Control", "no-store")
                .get().build()
            val call = client.newCall(request)
            currentCall = call
            val response = call.execute()
            if (response.code in 300..399) {
                val target = response.header("Location")
                response.close()
                require(redirects < 4 && !target.isNullOrBlank()) {
                    "Too many redirects or missing destination"
                }
                url = PermittedAudioDownloadPolicy.prepare(
                    URI(url).resolve(target).toString(), "").url
                return@repeat
            }
            response.use {
                require(response.code == 200) {
                    "Audio server returned HTTP " + response.code + "."
                }
                val body = requireNotNull(response.body) { "Empty download response" }
                val length = body.contentLength()
                require(length <= PermittedAudioDownloadPolicy.MAX_BYTES) {
                    "File is larger than the 250 MB song limit."
                }
                var received = 0L
                val prefix = ByteArray(64)
                var prefixCount = 0
                body.byteStream().use { input ->
                    FileOutputStream(pending).use { output ->
                        val buffer = ByteArray(32_768)
                        while (true) {
                            check(!cancelled.get()) { "Download cancelled" }
                            val n = input.read(buffer)
                            if (n < 0) break
                            if (n == 0) continue
                            received += n
                            require(received <= PermittedAudioDownloadPolicy.MAX_BYTES) {
                                "File exceeds the 250 MB song limit."
                            }
                            if (prefixCount < prefix.size) {
                                val copy = minOf(n, prefix.size - prefixCount)
                                buffer.copyInto(prefix, prefixCount, 0, copy)
                                prefixCount += copy
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
                val firstBytes = prefix.copyOf(prefixCount)
                val format = PermittedAudioDownloadPolicy.detectAudioExtension(
                    response.header("Content-Type"), firstBytes, source.extension)
                require(format != null &&
                    PermittedAudioDownloadPolicy.supportedContentType(
                        response.header("Content-Type"), format) &&
                    PermittedAudioDownloadPolicy.audioSignatureMatches(firstBytes, format)) {
                    "The link returned invalid audio or a different format."
                }
                val stem = if (source.extension.isBlank()) source.fileName
                    else source.fileName.removeSuffix("." + source.extension)
                return stem + "." + format
            }
        }
        error("Audio redirect limit exceeded")
    }

    /** Reject spoofed audio: Android must parse an audio track and no video. */
    private fun inspectAudioOnly(file: File) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            var audioTracks = 0
            var selectedAudioTrack = -1
            for (index in 0 until extractor.trackCount) {
                val track = extractor.getTrackFormat(index)
                val mime = track.getString(MediaFormat.KEY_MIME).orEmpty()
                require(!mime.startsWith("video/")) {
                    "This file contains video rather than audio only."
                }
                if (mime.startsWith("audio/")) {
                    audioTracks++
                    if (selectedAudioTrack < 0) selectedAudioTrack = index
                }
            }
            require(audioTracks > 0) {
                "Android could not identify any playable audio track."
            }
            // A valid container/header with no actual audio frames must never
            // become a "successful download" in the private library.
            extractor.selectTrack(selectedAudioTrack)
            val sampleBytes = extractor.readSampleData(
                java.nio.ByteBuffer.allocate(1_048_576), 0)
            require(sampleBytes > 0) {
                "The audio file has no readable audio samples."
            }
        } catch (error: RuntimeException) {
            throw IllegalArgumentException(
                "Android could not validate the audio file; nothing was saved.", error
            )
        } finally {
            extractor.release()
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
