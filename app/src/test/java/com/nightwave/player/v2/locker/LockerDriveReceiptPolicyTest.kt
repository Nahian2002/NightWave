package com.nightwave.player.v2.locker

import com.nightwave.player.offline.OfflineSong
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deterministic Google Drive response fixtures, never real user files or OAuth. */
class LockerDriveReceiptPolicyTest {
    private val folder = "nightwaveFolder_0001"
    private val sha = "a".repeat(64)
    private val md5 = "b".repeat(32)
    private val song = OfflineSong(
        "00000000-0000-0000-0000-000000000001.mp3", "Licensed track",
        "audio/mpeg", 1024L
    )
    private fun good(): JSONObject = JSONObject()
        .put("id", "driveFile_0123456789")
        .put("name", "Licensed track.mp3")
        .put("mimeType", "audio/mpeg")
        .put("size", "1024")
        .put("md5Checksum", md5)
        .put("parents", JSONArray().put(folder))
        .put("appProperties", JSONObject()
            .put("nightwave_sha256", sha)
            .put("nightwave_local_id", song.id))

    private fun verify(data: JSONObject = good(), parent: String = folder,
                       expectedSha: String = sha, expectedMd5: String = md5) =
        LockerDriveReceiptPolicy.requireCompletedUpload(
            data, song, parent, expectedSha, expectedMd5
        )
    private fun rejected(data: JSONObject) {
        assertThrows(IllegalArgumentException::class.java) { verify(data) }
    }

    @Test fun completeDriveReceiptAcceptsValidSource() {
        val result = verify()
        assertTrue(result.created)
        assertEquals(song.id, result.remote.sourceLocalId)
        assertEquals(song.bytes, result.remote.bytes)
        assertEquals(sha, result.remote.sha256)
    }
    @Test fun acceptsUppercaseDriveMd5WithoutChangingExpectedHash() {
        val result = verify(good().put("md5Checksum", md5.uppercase()))
        assertEquals(md5.uppercase(), result.remote.md5)
    }
    @Test fun missingAppPropertiesAreRejected() {
        rejected(good().apply { remove("appProperties") })
    }
    @Test fun noShaPropertyIsRejected() {
        val response = good()
        response.getJSONObject("appProperties").remove("nightwave_sha256")
        rejected(response)
    }
    @Test fun missingOriginalLocalIdRejected() {
        val response = good()
        response.getJSONObject("appProperties").remove("nightwave_local_id")
        rejected(response)
    }
    @Test fun otherAudioContentShaRejected() {
        val response = good()
        response.getJSONObject("appProperties").put("nightwave_sha256", "c".repeat(64))
        rejected(response)
    }
    @Test fun mismatchedDriveMd5Rejected() {
        rejected(good().put("md5Checksum", "c".repeat(32)))
    }
    @Test fun missingServerMd5Rejected() {
        rejected(good().apply { remove("md5Checksum") })
    }
    @Test fun wrongCloudFolderRejected() {
        rejected(good().put("parents", JSONArray().put("otherFolder_12345")))
    }
    @Test fun noCloudParentsRejected() {
        rejected(good().apply { remove("parents") })
    }
    @Test fun missingDriveFileIdRejected() {
        rejected(good().apply { remove("id") })
    }
    @Test fun unexpectedMediaTypeRejected() {
        rejected(good().put("mimeType", "audio/flac"))
    }
    @Test fun wrongFilenameRejected() {
        rejected(good().put("name", "Other recording.mp3"))
    }
    @Test fun wrongFileLengthRejected() {
        rejected(good().put("size", "1000"))
    }
    @Test fun invalidFileLengthRejected() {
        rejected(good().put("size", "huge"))
    }
    @Test fun wrongPrivateSongIdRejected() {
        val response = good()
        response.getJSONObject("appProperties").put(
            "nightwave_local_id", "00000000-0000-0000-0000-000000000002.mp3"
        )
        rejected(response)
    }
    @Test fun invalidExpectedChecksumRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            verify(expectedSha = "invalid")
        }
    }
    @Test fun invalidExpectedParentRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            verify(parent = "../other")
        }
    }
}
