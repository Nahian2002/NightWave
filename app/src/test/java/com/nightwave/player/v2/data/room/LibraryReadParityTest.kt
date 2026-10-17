package com.nightwave.player.v2.data.room

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.*
import org.junit.Test

class LibraryReadParityTest {
    private val a = VideoPreview("Alpha", "Artist A", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", 90L)
    private val b = VideoPreview("Beta", "Artist B", "https://www.youtube.com/watch?v=J---aiyznGQ", "", 100L)
    private fun legacy() = LegacyLibrarySnapshot(
        favorites = listOf(a, b),
        recent = listOf(b, a),
        playlists = listOf("Focus" to listOf(b, a), "Travel" to listOf(a)),
        digest = "verified-digest"
    )
    private fun sameRoom() = LibraryReadSnapshot(
        favorites = legacy().favorites,
        recent = legacy().recent,
        playlists = legacy().playlists
    )
    private fun compare(room: LibraryReadSnapshot, checkpoint: String? = "verified-digest") =
        LibraryReadParity.evaluate(legacy(), room, checkpoint)

    @Test fun exactSnapshotAndMatchingCheckpointIsEligible() {
        assertEquals(ReadParity.VERIFIED, compare(sameRoom()))
    }

    @Test fun missingCheckpointIsNeverEligible() {
        assertEquals(ReadBlocker.MISSING_CHECKPOINT, compare(sameRoom(), null).blocker)
    }

    @Test fun staleCheckpointForcesLegacyFallback() {
        assertEquals(ReadBlocker.SOURCE_CHANGED, compare(sameRoom(), "stale").blocker)
    }

    @Test fun matchingFavoriteCountsWithDifferentOrderStillFails() {
        val changed = sameRoom().copy(favorites = listOf(b, a))
        assertEquals(ReadBlocker.FAVORITES_DIFFER, compare(changed).blocker)
    }

    @Test fun changedHistorySequenceForcesFallback() {
        val changed = sameRoom().copy(recent = listOf(a, b))
        assertEquals(ReadBlocker.HISTORY_DIFFER, compare(changed).blocker)
    }

    @Test fun playlistNameOrderMustMatchExactly() {
        val changed = sameRoom().copy(playlists = sameRoom().playlists.reversed())
        assertEquals(ReadBlocker.PLAYLIST_NAMES_DIFFER, compare(changed).blocker)
    }

    @Test fun samePlaylistCountsButDifferentTrackOrderFails() {
        val changed = sameRoom().copy(playlists = listOf(
            "Focus" to listOf(a, b), "Travel" to listOf(a)
        ))
        assertEquals(ReadBlocker.PLAYLIST_TRACKS_DIFFER, compare(changed).blocker)
    }

    @Test fun changedMetadataEvenForSameUrlIsNotDeclaredEqual() {
        val changed = sameRoom().copy(favorites = listOf(a.copy(title = "Altered"), b))
        assertEquals(ReadBlocker.FAVORITES_DIFFER, compare(changed).blocker)
    }

    @Test fun emptyLibraryCanBeVerifiedButOnlyWithCheckpoint() {
        val source = LegacyLibrarySnapshot(emptyList(), emptyList(), emptyList(), "empty")
        val rows = LibraryReadSnapshot(emptyList(), emptyList(), emptyList())
        assertTrue(LibraryReadParity.evaluate(source, rows, "empty").eligible)
        assertFalse(LibraryReadParity.evaluate(source, rows, null).eligible)
    }

    @Test fun duplicateSongMembershipDoesNotChangePlaylistIdentity() {
        val before = legacy().copy(playlists = listOf("Focus" to listOf(b, a), "Travel" to listOf(a)))
        assertTrue(LibraryReadParity.evaluate(before, sameRoom(), "verified-digest").eligible)
    }
}
