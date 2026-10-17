package com.nightwave.player.v2.playback

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.*
import org.junit.Test

class QueueSelectionRulesTest {
    private fun track(index: Int): VideoPreview {
        val id = index.toString().padStart(11, '0')
        return VideoPreview("Song " + index, "Artist", "https://www.youtube.com/watch?v=" + id, "", 180)
    }

    @Test fun selectedTrackBeyondCapIsNeverLost() {
        val songs = (0..170).map(::track)
        val result = QueueSelectionRules.plan(songs.last(), songs)
        assertEquals(100, result.songs.size)
        assertTrue(result.selectedIndex in 0 until 100)
        assertEquals(songs.last().url, result.songs[result.selectedIndex].url)
    }

    @Test fun normalCollectionMaintainsOriginalOrder() {
        val songs = (1..4).map(::track)
        val result = QueueSelectionRules.plan(songs[2], songs)
        assertEquals(songs, result.songs)
        assertEquals(2, result.selectedIndex)
    }

    @Test fun duplicatesAndNonVideoLinksAreDropped() {
        val song = track(1)
        val invalid = song.copy(url = "https://youtube.com/playlist?list=abc")
        val result = QueueSelectionRules.plan(song, listOf(song, song, invalid))
        assertEquals(listOf(song), result.songs)
        assertEquals(0, result.selectedIndex)
    }

    @Test fun malformedSelectionDoesNotAutoplayUnrelatedTrack() {
        val invalid = track(3).copy(url = "https://evil.example/x")
        val result = QueueSelectionRules.plan(invalid, listOf(track(1)))
        assertTrue(result.songs.isEmpty())
        assertEquals(-1, result.selectedIndex)
    }

    @Test fun boundedQueueRespectsCapacity() {
        val songs = (1..10).map(::track)
        val result = QueueSelectionRules.plan(songs[0], songs, capacity = 3)
        assertEquals(3, result.songs.size)
        assertEquals(0, result.selectedIndex)
    }
}
