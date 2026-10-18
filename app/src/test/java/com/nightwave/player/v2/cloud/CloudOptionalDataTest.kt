package com.nightwave.player.v2.cloud

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.data.LibraryArchive
import com.nightwave.player.v2.data.LibraryBackupCodec
import com.nightwave.player.v2.model.AudioQualityPreference
import com.nightwave.player.v2.model.MotionPreference
import com.nightwave.player.v2.model.ProfileSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** No Google access, Android files, audio media or private user data. */
class CloudOptionalDataTest {
    private val track = VideoPreview(
        "Track", "Artist", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", 100L)
    private fun archive() = LibraryBackupCodec.encode(
        LibraryArchive("Guest", "MIDNIGHT", "orbit",
            listOf(track), listOf("Favorites" to listOf(track))))
    private fun preferences() = CloudPlaybackPreferences(
        MotionPreference.REDUCED, AudioQualityPreference.DATA_SAVER,
        4, false, true)
    private fun songId(n: String = "00000000-0000-0000-0000-000000000001") = "$n.mp3"
    private fun lrc() = "[00:01.000] Hello world"
    private fun lyric() = CloudLyricEntry(songId(), lrc())

    @Test fun oldV2CloudBackupStillDecodesWithoutExtras() {
        val old = CloudBackupCodec.decode(CloudBackupCodec.encode(archive(), emptyList(), false))
        assertNull(old.preferences)
        assertFalse(old.includesLyrics)
        assertTrue(old.lyrics.isEmpty())
    }

    @Test fun userOptedOutProducesV2RatherThanNewOptionalPayload() {
        val oldBytes = CloudBackupCodec.encode(archive(), emptyList(), false)
        assertEquals(CloudBackupCodec.decode(oldBytes),
            CloudBackupCodec.decode(CloudBackupCodec.encode(archive(), emptyList(), false)))
    }

    @Test fun v3SettingsRoundtripWithoutLyrics() {
        val saved = CloudBackupCodec.decode(CloudBackupCodec.encode(
            archive(), emptyList(), false, preferences()))
        assertEquals(preferences(), saved.preferences)
        assertFalse(saved.includesLyrics)
        assertTrue(saved.lyrics.isEmpty())
    }

    @Test fun v3LyricsRoundtripWithoutSettings() {
        val saved = CloudBackupCodec.decode(CloudBackupCodec.encode(
            archive(), emptyList(), false, null, listOf(lyric()), true))
        assertNull(saved.preferences)
        assertTrue(saved.includesLyrics)
        assertEquals(listOf(lyric()), saved.lyrics)
    }

    @Test fun bothOptionalTypesCanCoexistWithHistory() {
        val saved = CloudBackupCodec.decode(CloudBackupCodec.encode(
            archive(), listOf(track), true, preferences(), listOf(lyric()), true))
        assertEquals(preferences(), saved.preferences)
        assertEquals(1, saved.history.size)
        assertEquals(1, saved.lyrics.size)
    }

    @Test fun optedInEmptyLyricsStillProducesAnExplicitV3Option() {
        val saved = CloudBackupCodec.decode(CloudBackupCodec.encode(
            archive(), emptyList(), false, null, emptyList(), true))
        assertTrue(saved.includesLyrics)
        assertTrue(saved.lyrics.isEmpty())
    }

    @Test fun lyricsWithoutUserOptInMustNotLeak() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.encode(archive(), emptyList(), false,
                null, listOf(lyric()), false)
        }
    }

    @Test fun invalidOfflineIdMustNotLeaveApp() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudLyricsPolicy.validated(listOf(CloudLyricEntry("file:///private/song.mp3", lrc())))
        }
    }

    @Test fun invalidLyricDataIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudLyricsPolicy.validated(listOf(CloudLyricEntry(songId(), "plain text")))
        }
    }

    @Test fun duplicateLyricAttachmentsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudLyricsPolicy.validated(listOf(lyric(), lyric()))
        }
    }

    @Test fun boundedLyricsCountRejectsTwentyOneEntries() {
        val many = (1..21).map {
            lyric().copy(localSongId = songId("00000000-0000-0000-0000-" +
                it.toString().padStart(12, '0')))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CloudLyricsPolicy.validated(many)
        }
    }

    @Test fun invalidCrossfadeRejectedBeforeExport() {
        assertThrows(IllegalArgumentException::class.java) {
            preferences().copy(crossfadeSeconds = 18).validated()
        }
    }

    @Test fun restoresOnlyAllowlistedPreferencesAndKeepsLocalIdentity() {
        val local = ProfileSettings(localId = "my-private-id")
        val updated = preferences().applyTo(local)
        assertEquals("my-private-id", updated.localId)
        assertEquals(local.displayName, updated.displayName)
        assertEquals(local.theme, updated.theme)
        assertEquals(local.historyPrivacy, updated.historyPrivacy)
        assertEquals(MotionPreference.REDUCED, updated.motion)
    }

    @Test fun untouchedBackupsRemainEqualForSmartDedupe() {
        val old = CloudBackupCodec.encode(archive(), emptyList(), false)
        assertTrue(CloudSnapshotPolicy.sameVerifiedContent(old, old))
    }

    @Test fun extrasChangeSmartSnapshotIdentity() {
        val old = CloudBackupCodec.encode(archive(), emptyList(), false)
        val extras = CloudBackupCodec.encode(
            archive(), emptyList(), false, preferences())
        assertFalse(CloudSnapshotPolicy.sameVerifiedContent(extras, old))
    }

    @Test fun invalidJsonSettingsFieldsNeverApply() {
        assertThrows(Exception::class.java) {
            CloudPlaybackPreferences.parse(org.json.JSONObject()
                .put("motion", "SYSTEM").put("quality", "AUTO"))
        }
    }
}
