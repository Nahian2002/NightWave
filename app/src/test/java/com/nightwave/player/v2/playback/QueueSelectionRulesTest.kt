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

    @Test fun largeCollectionKeepsRealNextAndPreviousAroundSelection() {
        val songs = (0..200).map(::track)
        val result = QueueSelectionRules.plan(songs[150], songs)
        assertEquals(100, result.songs.size)
        assertEquals(50, result.selectedIndex)
        assertEquals(songs[149], result.songs[result.selectedIndex - 1])
        assertEquals(songs[150], result.songs[result.selectedIndex])
        assertEquals(songs[151], result.songs[result.selectedIndex + 1])
    }

    @Test fun selectingLastSongKeepsItsPreviousNeighbour() {
        val songs = (0..170).map(::track)
        val result = QueueSelectionRules.plan(songs.last(), songs)
        assertEquals(99, result.selectedIndex)
        assertEquals(songs[169], result.songs[98])
        assertEquals(songs.last(), result.songs[99])
    }

    @Test fun earlySelectionKeepsOriginalQueueHeadAndOrder() {
        val songs = (0..200).map(::track)
        val result = QueueSelectionRules.plan(songs[30], songs)
        assertEquals(songs.take(100), result.songs)
        assertEquals(30, result.selectedIndex)
    }

    @Test fun selectedSongOutsideCollectionStaysFirstAndPlayable() {
        val songs = (0..170).map(::track)
        val outsider = track(999)
        val result = QueueSelectionRules.plan(outsider, songs)
        assertEquals(100, result.songs.size)
        assertEquals(0, result.selectedIndex)
        assertEquals(outsider, result.songs.first())
        assertEquals(songs.take(99), result.songs.drop(1))
    }

    @Test fun invalidAndDuplicateEntriesDoNotShiftSelectionToWrongSong() {
        val songs = (0..160).map(::track)
        val invalid = track(999).copy(url = "https://youtube.com/playlist?list=abc")
        val collection = listOf(invalid, songs.first(), songs.first()) + songs
        val result = QueueSelectionRules.plan(songs[125], collection)
        assertEquals(songs[125], result.songs[result.selectedIndex])
        assertEquals(songs[124], result.songs[result.selectedIndex - 1])
        assertEquals(songs[126], result.songs[result.selectedIndex + 1])
        assertEquals(100, result.songs.size)
    }

    @Test fun capacityOneStillSelectsRequestedTrackNotSomeOtherSong() {
        val songs = (0..150).map(::track)
        val result = QueueSelectionRules.plan(songs[120], songs, capacity = 1)
        assertEquals(listOf(songs[120]), result.songs)
        assertEquals(0, result.selectedIndex)
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
