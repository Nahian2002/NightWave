package com.nightwave.player.v2.data.room

import org.junit.Assert.*
import org.junit.Test

class LegacyLibrarySnapshotParserTest {
    private val first = """{"url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ","title":"Song One","creator":"Artist","artwork":"","duration":120}"""
    private val second = """{"url":"https://www.youtube.com/watch?v=J---aiyznGQ","title":"Song Two","creator":"Artist","artwork":"","duration":180}"""

    private fun source() = mapOf(
        "playlist_names" to """["Evening","Study"]""",
        "favorites" to "[$first,$second]",
        "recent" to "[$second]",
        "list_Evening" to "[$second,$first]",
        "list_Study" to "[]"
    )

    @Test fun readsLegacyPlaylistOrderAndFavorites() {
        val read = LegacyLibrarySnapshotParser.parse(source())
        assertEquals(listOf("Evening", "Study"), read.playlists.map { it.first })
        assertEquals(listOf("Song Two", "Song One"), read.playlists.first().second.map { it.title })
        assertEquals(listOf("Song One", "Song Two"), read.favorites.map { it.title })
        assertEquals(1, read.recent.size)
        assertEquals(64, read.digest.length)
    }

    @Test fun idempotentDigestDoesNotDependOnMapOrder() {
        val original = source()
        val reversed = original.entries.reversed().associate { it.key to it.value }
        assertEquals(LegacyLibrarySnapshotParser.parse(original).digest,
            LegacyLibrarySnapshotParser.parse(reversed).digest)
    }

    @Test fun digestChangesWhenPlaylistOrderChanges() {
        val previous = LegacyLibrarySnapshotParser.parse(source()).digest
        val updated = LegacyLibrarySnapshotParser.parse(
            source() + ("list_Evening" to "[$first,$second]")
        ).digest
        assertNotEquals(previous, updated)
    }

    @Test fun refusesInvalidVideoRatherThanLosingItSilently() {
        assertThrows(IllegalArgumentException::class.java) {
            LegacyLibrarySnapshotParser.parse(source() +
                ("favorites" to """[{"url":"https://evil.invalid/track","title":"unsafe"}]"""))
        }
    }

    @Test fun refusesMalformedLegacyJson() {
        assertThrows(org.json.JSONException::class.java) {
            LegacyLibrarySnapshotParser.parse(source() + ("playlist_names" to "[broken"))
        }
    }

    @Test fun refusesDuplicatePlaylistNamesCaseInsensitively() {
        assertThrows(IllegalArgumentException::class.java) {
            LegacyLibrarySnapshotParser.parse(source() + ("playlist_names" to """["Study","study"]"""))
        }
    }

    @Test fun refusesDuplicateTrackUrls() {
        assertThrows(IllegalArgumentException::class.java) {
            LegacyLibrarySnapshotParser.parse(source() + ("favorites" to "[$first,$first]"))
        }
    }

    @Test fun rejectsUnexpectedVideoMetadataFields() {
        val injected = """[{"url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "title":"Song","creator":"Artist","artwork":"","duration":120,
            "unauthorized":"token"}]"""
        assertThrows(IllegalArgumentException::class.java) {
            LegacyLibrarySnapshotParser.parse(source() + ("favorites" to injected))
        }
    }

    @Test fun emptyLibraryIsValidAndDeterministic() {
        val firstEmpty = LegacyLibrarySnapshotParser.parse(emptyMap())
        val secondEmpty = LegacyLibrarySnapshotParser.parse(mapOf("unused" to "metadata"))
        assertTrue(firstEmpty.playlists.isEmpty())
        assertTrue(firstEmpty.favorites.isEmpty())
        assertEquals(firstEmpty.digest, secondEmpty.digest)
    }
}
