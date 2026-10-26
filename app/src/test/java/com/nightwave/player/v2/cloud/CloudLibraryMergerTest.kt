package com.nightwave.player.v2.cloud

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.data.LibraryArchive
import com.nightwave.player.v2.data.LibraryBackupCodec
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prepared now for a later Gradle run. No network, Google account or device required. */
class CloudLibraryMergerTest {
    private fun track(n: Int): VideoPreview {
        val id = "Ab" + n.toString().padStart(9, '0')
        return VideoPreview("Song $n", "Artist $n",
            "https://www.youtube.com/watch?v=$id", "", 120L)
    }

    private fun archive(
        favorites: List<VideoPreview> = emptyList(),
        playlists: List<Pair<String, List<VideoPreview>>> = emptyList(),
        displayName: String = "Local listener"
    ): LibraryArchive = LibraryArchive(
        displayName = displayName, theme = "MIDNIGHT", avatar = "orbit",
        favorites = favorites, playlists = playlists
    )

    private fun preview(
        local: LibraryArchive, remote: LibraryArchive
    ) = CloudLibraryMerger.preview(LibraryBackupCodec.encode(local), remote)

    @Test fun addsOnlyNewRemoteFavoritesAndKeepsLocalOrder() {
        val result = preview(archive(listOf(track(1), track(2))),
            archive(listOf(track(2), track(3))))
        assertTrue(result.hasChanges)
        assertEquals(1, result.addedFavoriteCount)
        assertEquals(listOf(track(1), track(2), track(3)), result.mergedArchive.favorites)
    }

    @Test fun unchangedArchiveIsNotApplied() {
        val local = archive(listOf(track(1)), listOf("Chill" to listOf(track(1))))
        val result = preview(local, local)
        assertFalse(result.hasChanges)
        assertEquals(0, result.addedFavoriteCount)
        assertEquals(0, result.addedPlaylistCount)
    }

    @Test fun importsNewPlaylistWithoutMutatingExisting() {
        val local = archive(playlists = listOf("Workout" to listOf(track(1))))
        val remote = archive(playlists = listOf("Focus" to listOf(track(2))))
        val result = preview(local, remote)
        assertEquals(listOf("Workout", "Focus"), result.mergedArchive.playlists.map { it.first })
        assertEquals(listOf(track(1)), result.mergedArchive.playlists.first().second)
        assertEquals(1, result.addedPlaylistCount)
    }

    @Test fun differentSameNameCreatesDriveCopyInsteadOfOverwriting() {
        val local = archive(playlists = listOf("Favorites" to listOf(track(1))))
        val remote = archive(playlists = listOf("favorites" to listOf(track(2))))
        val result = preview(local, remote)
        assertEquals(listOf("Favorites", "favorites (Drive)"),
            result.mergedArchive.playlists.map { it.first })
        assertEquals(listOf(track(1)), result.mergedArchive.playlists[0].second)
        assertEquals(listOf(track(2)), result.mergedArchive.playlists[1].second)
        assertEquals(listOf("favorites"), result.conflictingPlaylistNames)
    }

    @Test fun repeatedSameRemoteImportIsIdempotent() {
        val remote = archive(playlists = listOf("Chill" to listOf(track(2))))
        val first = preview(archive(playlists = listOf("Chill" to listOf(track(1)))), remote)
        val second = preview(first.mergedArchive, remote)
        assertFalse(second.hasChanges)
        assertEquals(first.mergedArchive.playlists, second.mergedArchive.playlists)
    }

    @Test fun thirdChangedSnapshotUsesDrive2Copy() {
        val local = archive(playlists = listOf(
            "Chill" to listOf(track(1)),
            "Chill (Drive)" to listOf(track(2))
        ))
        val result = preview(local, archive(playlists = listOf("Chill" to listOf(track(3)))))
        assertEquals("Chill (Drive 2)", result.mergedArchive.playlists.last().first)
    }

    @Test fun preservesLocalProfileRegardlessOfCloudProfile() {
        val result = preview(archive(displayName = "Only local"),
            archive(listOf(track(1)), displayName = "Other device"))
        assertEquals("Only local", result.mergedArchive.displayName)
    }

    @Test fun refusesFavoriteLimitRatherThanTruncateLocal() {
        val local = archive(favorites = (1..500).map(::track))
        val remote = archive(favorites = listOf(track(501)))
        assertThrows(IllegalArgumentException::class.java) { preview(local, remote) }
    }

    @Test fun refusesPlaylistLimitRatherThanOverwrite() {
        val local = archive(playlists = (1..50).map { "List $it" to listOf(track(it)) })
        val remote = archive(playlists = listOf("Additional" to listOf(track(51))))
        assertThrows(IllegalArgumentException::class.java) { preview(local, remote) }
    }

    @Test fun mergeConflictAndSecondPreviewStayStableAcrossLanguageChanges() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val first = preview(archive(playlists = listOf("I" to listOf(track(1)))),
                archive(playlists = listOf("i" to listOf(track(2)))))
            assertEquals(1, first.addedPlaylistCount)
            assertEquals("i (Drive)", first.mergedArchive.playlists.last().first)
            Locale.setDefault(Locale.US)
            val second = preview(first.mergedArchive,
                archive(playlists = listOf("i" to listOf(track(2)))))
            assertFalse(second.hasChanges)
            assertEquals(first.mergedArchive.playlists, second.mergedArchive.playlists)
        } finally { Locale.setDefault(original) }
    }

    @Test fun rejectsInvalidRemoteVideoBeforeAnyMerge() {
        val invalid = archive(favorites = listOf(
            VideoPreview("Invalid", "Artist", "https://example.org/video", "", 10L)
        ))
        assertThrows(IllegalArgumentException::class.java) {
            preview(archive(listOf(track(1))), invalid)
        }
    }
}
