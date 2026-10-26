package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NightWaveNowPlayingPresentationPolicyTest {
    private val p = NightWaveNowPlayingPresentationPolicy
    private val urls = listOf("song-a", "song-b", "song-c")
    private val names = listOf("Alo Alo", "Moonlight", "Night Drive")

    @Test fun regularQueueShowsExactlyTheNextSavedSong() {
        val next = p.queuePreview(urls, names, 0, emptyList(), false)
        assertEquals("Up next", next.heading)
        assertEquals("Moonlight", next.subtitle)
        assertEquals(1, next.sourceSlot)
        assertEquals("Night Drive", p.queuePreview(
            urls, names, 1, emptyList(), false).subtitle)
        assertNull(p.queuePreview(urls, names, 2, emptyList(), false).sourceSlot)
    }

    @Test fun priorityPlayNextUsesExplicitSelectionWithoutReplacingSourceIndex() {
        val next = p.queuePreview(urls, names, 0, listOf("song-c"), false)
        assertEquals("Play next", next.heading)
        assertEquals("Night Drive", next.subtitle)
        assertEquals(2, next.sourceSlot)
        assertEquals(1, p.queuePreview(urls, names, 0,
            listOf("missing-url", "song-b"), false).sourceSlot)
        assertEquals(1, p.queuePreview(urls, names, 0,
            listOf("song-a", "song-b"), false).sourceSlot)
    }

    @Test fun shuffleWithoutManualPriorityDoesNotPredictRandomNext() {
        val shuffled = p.queuePreview(urls, names, 0, emptyList(), true)
        assertEquals("Shuffle queue", shuffled.heading)
        assertEquals("2 possible next tracks", shuffled.subtitle)
        assertNull(shuffled.sourceSlot)
        val explicit = p.queuePreview(urls, names, 0, listOf("song-c"), true)
        assertEquals("Play next", explicit.heading)
        assertEquals(2, explicit.sourceSlot)
    }

    @Test fun repeatsAreExplainedTruthfullyIncludingLastSongWrap() {
        val one = p.queuePreview(urls, names, 0, listOf("song-c"),
            false, repeatOne = true)
        assertEquals("Repeat one", one.heading)
        assertEquals("Current song will replay", one.subtitle)
        assertNull(one.sourceSlot)
        val all = p.queuePreview(urls, names, 2, emptyList(),
            false, repeatAll = true)
        assertEquals("Repeat all", all.heading)
        assertEquals("Alo Alo", all.subtitle)
        assertEquals(0, all.sourceSlot)
    }

    @Test fun invalidStateAndMissingMetadataNeverCrashOrInventTracks() {
        val empty = p.queuePreview(emptyList(), emptyList(), -1, emptyList(), true)
        assertEquals("Queue is empty", empty.subtitle)
        assertNull(empty.sourceSlot)
        val unresolved = p.queuePreview(urls, names, -1, emptyList(), false)
        assertEquals("Waiting for queue selection", unresolved.subtitle)
        assertNull(unresolved.sourceSlot)
        assertEquals("Queued song", p.queuePreview(urls, emptyList(),
            0, emptyList(), false).subtitle)
        val zero = p.queuePreview(listOf("a"), listOf("Only"), 0, emptyList(), true)
        assertEquals("No other tracks", zero.subtitle)
    }

    @Test fun honestLyricsAndOfflineDescriptionsNeverPromiseRestrictedDownloads() {
        assertEquals("Offline options", p.offlineActionLabel(true, false))
        assertEquals("Open offline library", p.offlineActionLabel(true, true))
        assertEquals("Choose a song", p.offlineActionLabel(false, false))
        assertEquals("Synchronized lyrics require local audio",
            p.lyricsSubtitle(false, false))
        assertEquals("Your synchronized lyrics · ready",
            p.lyricsSubtitle(true, true))
        assertEquals("Import your own LRC lyrics",
            p.lyricsSubtitle(true, false))
    }

    @Test fun textHierarchySupportsLargeFontsAndGestureTruth() {
        assertEquals(2, p.titleLines(1f, 390f))
        assertEquals(3, p.titleLines(1.3f, 390f))
        assertEquals(3, p.titleLines(1f, 500f))
        assertEquals(1, p.artistLines(1.2f))
        assertEquals(2, p.artistLines(1.8f))
        assertEquals("Swipe artwork to skip", p.artworkGestureHint(true))
        assertNull(p.artworkGestureHint(false))
        assertTrue(p.titleLines(Float.NaN, 390f) >= 2)
        assertFalse(p.offlineActionLabel(true, false).contains("Save for"))
    }
}
