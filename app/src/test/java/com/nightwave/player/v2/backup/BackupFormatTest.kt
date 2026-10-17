package com.nightwave.player.v2.backup

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.model.ProfileSettings
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BackupFormatTest {
    private fun song(id: String) = VideoPreview(
        "Song " + id, "Artist", "https://www.youtube.com/watch?v=" + id,
        "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg", 180L
    )
    private val one = song("dQw4w9WgXcQ")
    private val two = song("J---aiyznGQ")

    private fun example(includeHistory: Boolean = false): PortableBackup = PortableBackup(
        BackupPreferences.from(ProfileSettings(
            localId = "secret-device-id", displayName = "Test Listener"
        )),
        MusicCollection(
            favorites = listOf(one),
            recent = if (includeHistory) listOf(two) else emptyList(),
            playlists = linkedMapOf("Driving" to listOf(one, two))
        ),
        1700000000000L, includeHistory
    )

    @Test fun roundTripPreservesTracksAndPreferencesWithoutDeviceId() {
        val bytes = BackupFormat.export(example())
        val restored = BackupFormat.import(bytes)
        assertEquals(listOf(one), restored.library.favorites)
        assertEquals(listOf(one, two), restored.library.playlists["Driving"])
        assertEquals("Test Listener", restored.profile.displayName)
        val content = String(bytes)
        assertFalse(content.contains("secret-device-id"))
        assertFalse(content.contains("access_token"))
        assertTrue(content.contains("checksum_sha256"))
    }

    @Test fun historyIsPrivateByDefault() {
        val data = BackupFormat.import(BackupFormat.export(example()))
        assertFalse(data.includesHistory)
        assertTrue(data.library.recent.isEmpty())
        val optedIn = BackupFormat.import(BackupFormat.export(example(includeHistory = true)))
        assertEquals(listOf(two), optedIn.library.recent)
    }

    @Test fun corruptedChecksumIsRejectedBeforeImport() {
        val raw = JSONObject(String(BackupFormat.export(example())))
        raw.put("checksum_sha256", "0".repeat(64))
        assertThrows(InvalidBackupException::class.java) {
            BackupFormat.import(raw.toString().toByteArray())
        }
    }

    @Test fun unknownFieldsFailClosed() {
        val raw = JSONObject(String(BackupFormat.export(example())))
        raw.put("refresh_token", "dangerous")
        assertThrows(InvalidBackupException::class.java) {
            BackupFormat.import(raw.toString().toByteArray())
        }
    }

    @Test fun invalidVideoLinksCannotBeExported() {
        val bad = example().copy(library = MusicCollection(
            listOf(one.copy(url = "https://youtube.com.attacker.invalid/watch?v=dQw4w9WgXcQ")),
            emptyList(), linkedMapOf()
        ))
        assertThrows(InvalidBackupException::class.java) { BackupFormat.export(bad) }
    }

    @Test fun cannotImportOversizedOrEmptyFiles() {
        assertThrows(InvalidBackupException::class.java) { BackupFormat.import(ByteArray(0)) }
        assertThrows(InvalidBackupException::class.java) {
            BackupFormat.import(ByteArray(BackupFormat.MAX_BYTES + 1))
        }
    }

    @Test fun importedSettingsKeepLocalDeviceIdentity() {
        val local = ProfileSettings(localId = "existing-device", displayName = "Local")
        val recovered = example().profile.onto(local)
        assertEquals("existing-device", recovered.localId)
        assertEquals("Test Listener", recovered.displayName)
    }
}
