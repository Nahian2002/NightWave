package com.nightwave.player.v2.locker

import com.nightwave.player.offline.OfflineSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Pure local file tests; no OAuth, Drive calls, or installed user files. */
class LockerAudioPolicyTest {
    private fun song(
        id: String = "00000000-0000-0000-0000-000000000001.mp3",
        title: String = "Owned demo",
        mime: String = "audio/mpeg",
        bytes: Long = 5L
    ) = OfflineSong(id, title, mime, bytes)

    @Test fun expectedAudioNameIsPreserved() {
        assertEquals("Owned demo.mp3", LockerAudioPolicy.fileName(song()))
    }

    @Test fun pathSeparatorsAndControlsAreStripped() {
        assertEquals("Bad title name.mp3", LockerAudioPolicy.fileName(
            song(title = "Bad/title\\name")))
    }

    @Test fun blankTitleGetsSafeName() {
        assertEquals("Audio track.mp3", LockerAudioPolicy.fileName(song(title = "\n  ")))
    }

    @Test fun outsidePrivateIdIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.fileName(song(id = "/storage/music/song.mp3"))
        }
    }

    @Test fun mismatchedMimeRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.fileName(song(mime = "application/octet-stream"))
        }
    }

    @Test fun expectedFlacIsAllowed() {
        assertEquals("Track.flac", LockerAudioPolicy.fileName(song(
            id = "00000000-0000-0000-0000-000000000001.flac", title = "Track",
            mime = "audio/flac")))
    }

    @Test fun missingFileIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.validateLocal(song(), File("/not-a-nightwave-file"))
        }
    }

    @Test fun modifiedAudioSizeRejected() {
        val file = File.createTempFile("nw-locker", ".mp3")
        try {
            file.writeBytes(byteArrayOf(1,2,3))
            assertThrows(IllegalArgumentException::class.java) {
                LockerAudioPolicy.validateLocal(song(bytes = 99L), file)
            }
        } finally { file.delete() }
    }

    @Test fun shaAndMd5AreExactForKnownBytes() {
        val file = File.createTempFile("nw-locker", ".mp3")
        try {
            file.writeText("abc")
            val pair = LockerAudioPolicy.hashes(file)
            assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", pair.first)
            assertEquals("900150983cd24fb0d6963f7d28e17f72", pair.second)
        } finally { file.delete() }
    }

    @Test fun localTrackAcceptsMatchingSize() {
        val file = File.createTempFile("nw-locker", ".mp3")
        try {
            file.writeBytes(byteArrayOf(1,2,3,4,5))
            LockerAudioPolicy.validateLocal(song(bytes = 5L), file)
        } finally { file.delete() }
    }

    @Test fun rejectsOversizeBeforeTransfer() {
        val file = File.createTempFile("nw-locker", ".mp3")
        try {
            file.writeBytes(byteArrayOf(1,2,3))
            assertThrows(IllegalArgumentException::class.java) {
                LockerAudioPolicy.validateLocal(
                    song(bytes = LockerAudioPolicy.MAX_FILE_BYTES + 1), file)
            }
        } finally { file.delete() }
    }

    @Test fun validRemoteAcceptsOwnedMp3Metadata() {
        val item = LockerRemoteTrack("abcDEF_0123", "My song.mp3", "audio/mpeg",
            123L, "900150983cd24fb0d6963f7d28e17f72",
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        assertEquals(item, LockerAudioPolicy.validateRemote(item))
    }

    @Test fun fakeRemoteTypeFails() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.validateRemote(LockerRemoteTrack(
                "abcDEF_0123", "song.mp3", "video/x-danger", 900L))
        }
    }

    @Test fun zeroRemoteLengthFails() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerAudioPolicy.validateRemote(LockerRemoteTrack(
                "abcDEF_0123", "song.mp3", "audio/mpeg", 0L))
        }
    }

    @Test fun missingDriveIdFails() {
        assertFalse(LockerAudioPolicy.validDriveId("../private"))
    }

    @Test fun sha256IsStrictHex() {
        assertTrue(LockerAudioPolicy.validSha256("a".repeat(64)))
        assertFalse(LockerAudioPolicy.validSha256("A".repeat(64)))
        assertFalse(LockerAudioPolicy.validSha256("a".repeat(63)))
    }

    @Test fun uploadChunkIsBoundedToOneMiB() {
        assertEquals(1024 * 1024, LockerAudioPolicy.CHUNK_BYTES)
    }
}
