package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

class QueueRestorePolicyTest {
    private val a = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val b = "https://www.youtube.com/watch?v=J---aiyznGQ"
    private val c = "https://www.youtube.com/watch?v=9bZkp7q19f0"
    private fun entry(url: String) = QueueEntry(url, "Song", "Artist", "")

    @Test fun legacySelectionSurvivesInvalidPrecedingRow() {
        val original = listOf(a, "https://evil.example/audio?sig=secret", b)
        assertEquals(1, QueueRestorePolicy.selectedIndex(
            original, 2, null, listOf(entry(a), entry(b))))
    }

    @Test fun duplicateRemovalCannotMoveSelectedSong() {
        assertEquals(1, QueueRestorePolicy.selectedIndex(
            listOf(a, a, b), 2, b, listOf(entry(a), entry(b))))
    }

    @Test fun legacySnapshotCanRestoreByOriginalIndex() {
        assertEquals(2, QueueRestorePolicy.selectedIndex(
            listOf(a, b, c), 2, null, listOf(entry(a), entry(b), entry(c))))
    }

    @Test fun mismatchedSavedSelectedUrlFailsClosed() {
        assertNull(QueueRestorePolicy.selectedIndex(
            listOf(a, b), 0, b, listOf(entry(a), entry(b))))
    }

    @Test fun missingOrMalformedSelectedSongFailsClosed() {
        assertNull(QueueRestorePolicy.selectedIndex(
            listOf(a, "https://www.youtube.com/watch?v=invalid"), 1, null,
            listOf(entry(a))))
        assertNull(QueueRestorePolicy.selectedIndex(
            listOf(a, b), 3, b, listOf(entry(a), entry(b))))
        assertNull(QueueRestorePolicy.selectedIndex(
            listOf(a, b), -1, b, listOf(entry(a), entry(b))))
        assertNull(QueueRestorePolicy.selectedIndex(
            listOf(a, b), 1, b, listOf(entry(a))))
    }

    @Test fun onlyCanonicalPublicVideoIdsAreSafeToPersistAndRestore() {
        assertTrue(QueueRestorePolicy.isCanonicalPublicWatchUrl(a))
        assertFalse(QueueRestorePolicy.isCanonicalPublicWatchUrl(a + "&sig=private"))
        assertFalse(QueueRestorePolicy.isCanonicalPublicWatchUrl(
            "https://rr.example.googlevideo.com/audio?sig=private"))
        assertFalse(QueueRestorePolicy.isCanonicalPublicWatchUrl(
            "https://www.youtube.com/watch?v=short"))
    }
}
