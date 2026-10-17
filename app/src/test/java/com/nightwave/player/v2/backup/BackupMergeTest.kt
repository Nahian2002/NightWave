package com.nightwave.player.v2.backup

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.model.ProfileSettings
import org.junit.Assert.*
import org.junit.Test

class BackupMergeTest {
    private fun video(i: Int) = VideoPreview("Song $i", "Artist",
        "https://www.youtube.com/watch?v=" + i.toString().padStart(11, '0'),
        "", 130)
    private val settings = BackupPreferences.from(ProfileSettings(localId = "private"))

    private fun incoming(library: MusicCollection, includeHistory: Boolean = false) =
        PortableBackup(settings, library, 1L, includeHistory)

    @Test fun mergePreservesLocalSongOrderingAndDeduplicates() {
        val local = MusicCollection(listOf(video(1)), emptyList(),
            linkedMapOf("Chill" to listOf(video(2))))
        val imported = incoming(MusicCollection(listOf(video(1), video(3)), emptyList(),
            linkedMapOf("chill" to listOf(video(2), video(4)))))
        val merged = BackupMerge.prepare(local, imported, ImportStrategy.MERGE_LIBRARY)
        assertEquals(listOf(video(1), video(3)), merged.favorites)
        assertEquals(listOf("Chill"), merged.playlists.keys.toList())
        assertEquals(listOf(video(2), video(4)), merged.playlists["Chill"])
    }

    @Test fun noHistoryBackupCannotEraseLocalHistory() {
        val local = MusicCollection(emptyList(), listOf(video(5)), linkedMapOf())
        val imported = incoming(MusicCollection(listOf(video(1)), emptyList(), linkedMapOf()))
        val merged = BackupMerge.prepare(local, imported, ImportStrategy.REPLACE_LIBRARY)
        assertEquals(listOf(video(5)), merged.recent)
    }

    @Test fun importingTooManySongsFailsWithoutTruncating() {
        val local = MusicCollection((0..490).map(::video), emptyList(), linkedMapOf())
        val imported = incoming(MusicCollection((500..525).map(::video), emptyList(), linkedMapOf()))
        assertThrows(InvalidBackupException::class.java) {
            BackupMerge.prepare(local, imported, ImportStrategy.MERGE_LIBRARY)
        }
    }

    @Test fun keepExistingDoesNotModifyLocalData() {
        val local = MusicCollection(listOf(video(1)), emptyList(), linkedMapOf())
        val backup = incoming(MusicCollection(listOf(video(3)), emptyList(), linkedMapOf()))
        assertEquals(local, BackupMerge.prepare(local, backup, ImportStrategy.KEEP_EXISTING))
    }

    @Test fun playlistCapacityConflictFails() {
        val localPlaylists = linkedMapOf<String,List<VideoPreview>>()
        repeat(50) { localPlaylists["List$it"] = emptyList() }
        val local = MusicCollection(emptyList(), emptyList(), LinkedHashMap(localPlaylists))
        val imported = incoming(MusicCollection(emptyList(), emptyList(), linkedMapOf("New" to emptyList())))
        assertThrows(InvalidBackupException::class.java) {
            BackupMerge.prepare(local, imported, ImportStrategy.MERGE_LIBRARY)
        }
    }
}
