package com.nightwave.player.offline

import java.io.File

/** Production-policy JVM proof: no Android SDK, network, app accounts, or user files. */
object NW22AudioCoreSmoke {
    @JvmStatic fun main(args: Array<String>) {
        var checked = 0
        fun verify(label: String, condition: Boolean) {
            check(condition) { "NW22 audio core: " + label }
            checked++
        }
        fun reject(label: String, block: () -> Unit) {
            verify(label, runCatching(block).isFailure)
        }

        val mp3 = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0xC4.toByte(), 0, 0, 0x14)
        val aac = byteArrayOf(0xFF.toByte(), 0xF1.toByte(), 0x50, 0x40, 0x28, 0x3F, 0xFC.toByte())
        verify("MP3 frame", AudioFrameSignature.isMp3Frame(mp3))
        verify("AAC not MP3", !AudioFrameSignature.isMp3Frame(aac))
        verify("MP3 not AAC", !AudioFrameSignature.isAacAdts(mp3))
        verify("AAC frame", AudioFrameSignature.isAacAdts(aac))
        verify("MP3 extension", PermittedAudioDownloadPolicy.detectAudioExtension("audio/mpeg", mp3, "") == "mp3")
        verify("AAC extension", PermittedAudioDownloadPolicy.detectAudioExtension("audio/aac", aac, "") == "aac")
        verify("HTML not MP3", PermittedAudioDownloadPolicy.detectAudioExtension(
            "text/html", "<html>Not audio</html>".toByteArray(), "") == null)
        verify("no fake MP3 labeling", PermittedAudioDownloadPolicy.detectAudioExtension("audio/mp4", byteArrayOf(0,0,0,20,102,116,121,112,77,52,65,32), "mp3") == null)
        verify("authorized HTTPS link", PermittedAudioDownloadPolicy.prepare(
            "https://creator.example.org/music/song.mp3", "").extension == "mp3")
        reject("no HTTP URLs") { PermittedAudioDownloadPolicy.prepare("http://creator.example.org/song.mp3", "") }
        reject("no YouTube video") { PermittedAudioDownloadPolicy.prepare("https://youtube.com/watch?v=example", "") }
        reject("no local host") { PermittedAudioDownloadPolicy.prepare("https://192.168.0.1/music.mp3", "") }
        reject("no streaming manifest") { PermittedAudioDownloadPolicy.prepare("https://creator.example.org/stream.m3u8", "") }
        verify("CC0 only", FreeAudioCatalogPolicy.permittedLicense("CC0 1.0"))
        verify("public-domain allowed", FreeAudioCatalogPolicy.permittedLicense("Public domain"))
        verify("CC BY-NC filtered", !FreeAudioCatalogPolicy.permittedLicense("CC BY-NC 4.0"))
        verify("fake host filtered", !FreeAudioCatalogPolicy.safeMediaUrl(
            "https://upload.wikimedia.org.attacker.invalid/wikipedia/commons/f.ogg"))
        verify("Commons host allowed", FreeAudioCatalogPolicy.safeMediaUrl(
            "https://upload.wikimedia.org/wikipedia/commons/a/ab/track.ogg"))
        verify("source format preserved", FreeAudioCatalogPolicy.saveName("File:Ocean.oga", "audio/ogg") == "Ocean.ogg")
        verify("video MIME filtered", FreeAudioCatalogPolicy.saveName("File:Movie.webm", "video/webm") == null)
        verify("large file filtered", !FreeAudioCatalogPolicy.validSize(251L * 1024L * 1024L))
        val scratch = File.createTempFile("nightwave-nw22-synthetic-", ".bin")
        try {
            scratch.writeBytes(ByteArray(65_555) { (it * 37).toByte() })
            val expected = FreeAudioIntegrity.sourceSha1Base36(scratch)
            verify("base36 shape", FreeAudioIntegrity.isValidSourceHash(expected))
            verify("source hash matches", FreeAudioIntegrity.sourceMatches(scratch, expected))
            scratch.appendBytes(byteArrayOf(1))
            verify("corrupt content rejected", !FreeAudioIntegrity.sourceMatches(scratch, expected))
        } finally {
            scratch.delete()
        }
        println("PASS: NW22 " + checked + " production Kotlin audio policy assertions")
        println("LIMIT: Android compilation, downloads and APK signing not tested")
    }
}
