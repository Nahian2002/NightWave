package com.nightwave.player.offline

import org.junit.Assert.*
import org.junit.Test

class NW150OfflineDestinationTest {
    private val first = OfflineSong("00000000-0000-4000-8000-000000000001.mp3", "Same title", "audio/mpeg", 1200)
    private val second = first.copy(id = "00000000-0000-4000-8000-000000000002.flac", mimeType = "audio/flac", bytes = 4500)

    @Test fun duplicateTitlesOpenOnlyTheChosenIdAndPreserveItsMetadata() {
        assertEquals(listOf(second), OfflineSearchDestinationPolicy.select("Same title", listOf(first, second), second.id))
    }
    @Test fun renamedEntryStillResolvesByIdentityInsteadOfOldCaption() {
        val renamed = second.copy(title = "New caption")
        assertEquals(listOf(renamed), OfflineSearchDestinationPolicy.select("Same title", listOf(first, renamed), second.id))
    }
    @Test fun missingEntryNeverFallsThroughToAnIdenticallyNamedFile() {
        assertTrue(OfflineSearchDestinationPolicy.select("Same title", listOf(first), second.id).isEmpty())
        assertTrue(OfflineSearchDestinationPolicy.select("", emptyList(), second.id).isEmpty())
    }
    @Test fun explicitEditingClearsSelectionAndReturnsToFullQueryMatching() {
        assertEquals(listOf(first, second), OfflineSearchDestinationPolicy.select("same", listOf(first, second), null))
        assertEquals(listOf(first, second), OfflineSearchDestinationPolicy.select("", listOf(first, second), null))
    }
    @Test fun navigationMetadataNeverAcceptsPathsOrControlCharacters() {
        for (raw in listOf("", " ", "../song.mp3", "folder\\song.mp3", "a\nb", "x".repeat(129)))
            assertNull(OfflineSearchDestinationPolicy.entryId(raw))
        assertEquals(second.id, OfflineSearchDestinationPolicy.entryId(second.id))
        assertNull(OfflineSearchDestinationPolicy.entryId(null))
    }
    @Test fun exactSelectionDoesNotMutateCallerListOrRequireTitleMatch() {
        val songs = mutableListOf(first, second)
        assertEquals(listOf(second), OfflineSearchDestinationPolicy.select("বাংলা", songs, second.id))
        assertEquals(listOf(first, second), songs)
    }
}
