package com.nightwave.player.v2.lyrics

import org.junit.Assert.*
import org.junit.Test

class OwnedLyricsBindingTest {
    private val local = "00000000-0000-4000-8000-000000000001.mp3"
    private val publicMedia = "https://www.youtube.com/watch?v=abcdefghijk"
    @Test fun localIdsKeepTheirOriginalStorageIdentity() {
        assertEquals(local, LyricsTrackBindingPolicy.storageId("offline:$local"))
        assertEquals("offline:$local", LyricsTrackBindingPolicy.mediaId(local))
        assertTrue(LyricsTrackBindingPolicy.isLocal(local))
        assertFalse(LyricsTrackBindingPolicy.isPublic(local))
    }
    @Test fun publicBindingRoundTripsWithoutAStreamAddress() {
        assertEquals("youtube:abcdefghijk", LyricsTrackBindingPolicy.storageId(publicMedia))
        assertEquals(publicMedia, LyricsTrackBindingPolicy.mediaId("youtube:abcdefghijk"))
        assertFalse(LyricsTrackBindingPolicy.isLocal("youtube:abcdefghijk"))
    }
    @Test fun differentRecordingsWithIdenticalTitlesNeverShareAKey() {
        val second = "https://www.youtube.com/watch?v=12345678901"
        assertNotEquals(LyricsTrackBindingPolicy.storageId(publicMedia), LyricsTrackBindingPolicy.storageId(second))
        assertFalse(LyricsTrackBindingPolicy.matches("youtube:abcdefghijk", second))
        assertFalse(LyricsTrackBindingPolicy.matches(local, publicMedia))
    }
    @Test fun signedUrlsFilenamesAndNoncanonicalReferencesAreRejected() {
        for (id in listOf("", "Blue.lrc", "abcdefghijk", "https://media.example/audio?token=fixture",
            "https://www.youtube.com/watch?v=abcdefghijk&t=30", "https://youtu.be/abcdefghijk",
            "offline:../../audio.mp3", "https://www.youtube.com.evil.example/watch?v=abcdefghijk"))
            assertNull(id, LyricsTrackBindingPolicy.storageId(id))
    }
    @Test fun unsupportedStoredKeysCannotBecomeAPlaybackBinding() {
        for (id in listOf("youtube:short", "youtube:../../foo", publicMedia, "offline:$local", "Title match"))
            assertNull(LyricsTrackBindingPolicy.mediaId(id))
    }
    @Test fun ownedNormalizationPreservesBanglaMarksAndSimultaneousCueOrder() {
        val source = "[00:01.00] মেলা\n[00:01.00] মালা\n[00:02.00] আবার"
        val before = TimedLyricsCodec.parse(source.toByteArray())
        val saved = OwnedLyricsContentPolicy.normalized(source.toByteArray())
        assertEquals(before.cues, TimedLyricsCodec.parse(saved.toByteArray()).cues)
        assertEquals(listOf("মেলা", "মালা", "আবার"), before.cues.map { it.text })
    }
    @Test fun malformedUtf8AndOversizedContentFailBeforeMutation() {
        for (bytes in listOf(byteArrayOf(0xc3.toByte(), 0x28), ByteArray(TimedLyricsCodec.MAX_BYTES + 1) { 65 })) {
            try { OwnedLyricsContentPolicy.normalized(bytes); fail("Invalid content was accepted") }
            catch (_: IllegalArgumentException) { }
            catch (_: java.nio.charset.CharacterCodingException) { }
        }
    }
    @Test fun fingerprintsTrackContentRatherThanTitles() {
        val text = OwnedLyricsContentPolicy.normalized("[00:01.00] Original".toByteArray())
        val fp = OwnedLyricsContentPolicy.fingerprint(text)
        assertEquals(64, fp.length)
        assertEquals(fp, OwnedLyricsContentPolicy.fingerprint(text))
        assertNotEquals(fp, OwnedLyricsContentPolicy.fingerprint(text + " changed"))
    }
    @Test fun publicQuotaAllowsReplacementAtCapacityButNeverAnExtraTrack() {
        val entries = (0 until 128).associate { "youtube:%011d".format(it) to "small" }
        assertTrue(OwnedLyricsContentPolicy.withinQuota(entries, entries.keys.first(), "replacement"))
        assertFalse(OwnedLyricsContentPolicy.withinQuota(entries, "youtube:abcdefghijk", "extra"))
        assertFalse(OwnedLyricsContentPolicy.withinQuota(emptyMap(), local, "not public"))
    }
    @Test fun quotaCountsUtf8BytesAndSubtractsTheReplacedValue() {
        val full = "a".repeat(OwnedLyricsContentPolicy.MAX_PUBLIC_BYTES)
        val entries = mapOf("youtube:abcdefghijk" to full)
        assertTrue(OwnedLyricsContentPolicy.withinQuota(entries, "youtube:abcdefghijk", "ছোট"))
        assertFalse(OwnedLyricsContentPolicy.withinQuota(entries, "youtube:12345678901", "আর"))
    }
    @Test fun sourceLabelsDisclosePublicDeviceOnlyScope() {
        assertTrue(LyricsTrackBindingPolicy.sourceLabel("youtube:abcdefghijk").contains("Stored only on this device"))
        assertEquals("This on-device audio copy", LyricsTrackBindingPolicy.sourceLabel(local))
    }
    @Test fun importedPublicCuesUseTheExistingSeekAndOffsetRules() {
        val lyrics = TimedLyricsCodec.parse("[00:01.00] One\n[00:02.00] Two".toByteArray())
        assertEquals(-1, LyricsSyncPolicy.activeIndex(lyrics, 500, 0))
        assertEquals(0, LyricsSyncPolicy.activeIndex(lyrics, 1200, 0))
        assertEquals(1250L, LyricsSyncPolicy.seekPosition(lyrics.cues.first(), 250, 5000))
        assertEquals(1, LyricsSyncPolicy.activeIndex(lyrics, 2250, 250))
    }
}
