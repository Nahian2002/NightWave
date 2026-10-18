package com.nightwave.player.v2.locker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** No network, Android phone, Google account or user music in these tests. */
class LockerDownloadPolicyTest {
    private val sha = "a".repeat(64)
    private val md5 = "b".repeat(32)
    private val original = "00000000-0000-0000-0000-000000000001.mp3"
    private fun remote(
        id: String = "validDrive_01234",
        name: String = "My song.mp3",
        mime: String = "audio/mpeg",
        bytes: Long = 123L,
        md5Hash: String = md5,
        shaHash: String = sha,
        sourceId: String = original
    ) = LockerRemoteTrack(id, name, mime, bytes, md5Hash, shaHash, sourceId)

    @Test fun validUploadedFilePassesDownloadContract() {
        assertEquals(remote(), LockerAudioPolicy.requireDownloadable(remote()))
    }

    @Test fun missingUploadedSha256Blocked() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.requireDownloadable(remote(shaHash = ""))
        }
    }

    @Test fun missingDriveMd5Blocked() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.requireDownloadable(remote(md5Hash = ""))
        }
    }

    @Test fun malformedDriveMd5Blocked() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.requireDownloadable(remote(md5Hash = "xyz"))
        }
    }

    @Test fun uppercaseSha256Blocked() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.requireDownloadable(remote(shaHash = "A".repeat(64)))
        }
    }

    @Test fun invalidSourceIdBlocked() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.requireDownloadable(remote(sourceId = "../audio.mp3"))
        }
    }

    @Test fun unknownAudioTypeBlocked() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.requireDownloadable(remote(name = "track.exe"))
        }
    }

    @Test fun metadataCannotAdvertiseWrongMime() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.requireDownloadable(remote(mime = "audio/flac"))
        }
    }

    @Test fun zeroLengthBlocked() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.requireDownloadable(remote(bytes = 0L))
        }
    }

    @Test fun oversizedAudioBlocked() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.requireDownloadable(
                remote(bytes = LockerAudioPolicy.MAX_FILE_BYTES + 1))
        }
    }

    @Test fun exactFreeSourceIdCanReattachBackedUpLyrics() {
        val selected = LockerAudioPolicy.chooseRestoredId(remote()) { false }
        assertEquals(original, selected)
    }

    @Test fun occupiedSourceIdCannotBeOverwritten() {
        val chosen = LockerAudioPolicy.chooseRestoredId(remote()) { it == original }
        assertNotEquals(original, chosen)
        assertTrue(LockerAudioPolicy.validLocalId(chosen))
        assertTrue(chosen.endsWith(".mp3"))
    }

    @Test fun missingSourceIdGetsFreshOfflineIdentity() {
        val chosen = LockerAudioPolicy.chooseRestoredId(remote(sourceId = "")) { false }
        assertNotEquals(original, chosen)
        assertTrue(LockerAudioPolicy.validLocalId(chosen))
    }

    @Test fun mismatchedSourceFormatGetsNewIdentity() {
        val flac = remote(name = "Recording.flac", mime = "audio/flac", sourceId = original)
        val chosen = LockerAudioPolicy.chooseRestoredId(flac) { false }
        assertNotEquals(original, chosen)
        assertTrue(chosen.endsWith(".flac"))
    }

    @Test fun downloadedNameSanitizesPathSeparator() {
        assertEquals("unsafe name", LockerAudioPolicy.safeTitle("unsafe/name.mp3"))
    }

    @Test fun blankRemoteTitleGetsSafeFallback() {
        assertEquals("Audio track", LockerAudioPolicy.safeTitle(" .mp3"))
    }

    @Test fun originalSourceIdIsNotMandatoryForDownload() {
        val track = remote(sourceId = "")
        assertEquals(track, LockerAudioPolicy.requireDownloadable(track))
    }
}
