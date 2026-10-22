package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermittedAudioDownloadPolicyTest {
    private fun reject(url: String, name: String = "") {
        val result = runCatching { PermittedAudioDownloadPolicy.prepare(url, name) }
        assertTrue("Unsafe URL was accepted: $url", result.isFailure)
    }

    @Test fun acceptsDirectHttpsMp3() {
        val source = PermittedAudioDownloadPolicy.prepare(
            "https://artist.example.net/music/track.mp3", "")
        assertEquals("mp3", source.extension)
        assertEquals("track.mp3", source.fileName)
    }

    @Test fun keepsHttpsQueryForAuthorizedDirectDownloads() {
        val source = PermittedAudioDownloadPolicy.prepare(
            "https://artist.example.net/track.flac?download=1", "")
        assertEquals("track.flac", source.fileName)
        assertTrue(source.url.contains("download=1"))
    }

    @Test fun sanitizesUserSuppliedSongName() {
        val source = PermittedAudioDownloadPolicy.prepare(
            "https://artist.example.net/song.mp3", "My ? Song.mp3")
        assertEquals("My _ Song.mp3", source.fileName)
    }

    @Test fun optionalNameCannotSmuggleUnsafeExtension() {
        reject("https://artist.example.net/song.mp3", "music.exe")
        reject("https://artist.example.net/song.mp3", "archive.html")
    }

    @Test fun rejectsYoutubeAndStreamingHosts() {
        reject("https://www.youtube.com/watch?v=dQw4w9WgXcQ", "song.mp3")
        reject("https://youtu.be/dQw4w9WgXcQ", "song.mp3")
        reject("https://r2.googlevideo.com/audio.mp3")
        reject("https://open.spotify.com/track/12345", "song.mp3")
    }

    @Test fun rejectsNonHttpsAndEmbeddedCredentials() {
        reject("http://artist.example.net/song.mp3")
        reject("file:///sdcard/song.mp3")
        reject("https://user:password@artist.example.net/song.mp3")
    }

    @Test fun rejectsLocalAndIpLiteralLinks() {
        reject("https://localhost/song.mp3")
        reject("https://music.local/song.mp3")
        reject("https://internal.example.local/song.mp3")
        reject("https://127.0.0.1/song.mp3")
        reject("https://192.168.1.7/song.mp3")
    }

    @Test fun rejectsHtmlAndManifestLinks() {
        reject("https://artist.example.net/album")
        reject("https://artist.example.net/song.m3u8")
        reject("https://artist.example.net/song.html")
    }

    @Test fun verifiesHttpAudioContentType() {
        assertTrue(PermittedAudioDownloadPolicy.supportedContentType("audio/mpeg", "mp3"))
        assertTrue(PermittedAudioDownloadPolicy.supportedContentType(
            "audio/ogg; charset=binary", "opus"))
        assertFalse(PermittedAudioDownloadPolicy.supportedContentType("text/html", "mp3"))
        assertFalse(PermittedAudioDownloadPolicy.supportedContentType("application/json", "mp3"))
        assertFalse(PermittedAudioDownloadPolicy.supportedContentType("video/mp4", "m4a"))
        assertFalse(PermittedAudioDownloadPolicy.supportedContentType("audio/mpeg", "flac"))
    }

    @Test fun verifiesAudioMagicBytes() {
        assertTrue(PermittedAudioDownloadPolicy.audioSignatureMatches(
            "ID3\u0003\u0000\u0000".toByteArray(), "mp3"))
        assertTrue(PermittedAudioDownloadPolicy.audioSignatureMatches(
            "fLaC\u0000\u0000".toByteArray(), "flac"))
        assertTrue(PermittedAudioDownloadPolicy.audioSignatureMatches(
            "OggS\u0000".toByteArray(), "ogg"))
        assertFalse(PermittedAudioDownloadPolicy.audioSignatureMatches(
            "<html>not audio</html>".toByteArray(), "mp3"))
        assertFalse(PermittedAudioDownloadPolicy.audioSignatureMatches(
            "RIFFaaaaWEBP".toByteArray(), "wav"))
    }

    @Test fun rejectsMalformedAndFragmentedLinks() {
        reject("https://artist.example.net/song.mp3#anchor")
        reject("https://artist.example.net:0/song.mp3")
        reject("https://artist.example.net:bad/song.mp3")
    }

    @Test fun finiteDownloadLimitAlignedWithOfflineLibrary() {
        assertEquals(250L * 1024L * 1024L, PermittedAudioDownloadPolicy.MAX_BYTES)
        assertTrue(PermittedAudioDownloadPolicy.prepare(
            "https://artist.example.net/track.m4a", "Track.m4a").fileName.endsWith(".m4a"))
    }
}
