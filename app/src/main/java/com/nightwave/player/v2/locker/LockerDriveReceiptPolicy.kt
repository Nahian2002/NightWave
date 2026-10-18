package com.nightwave.player.v2.locker

import com.nightwave.player.offline.OfflineSong
import org.json.JSONObject

/**
 * Pure upload completion contract. Drive's response body is untrusted data,
 * not proof of a completed, correct audio upload on its own.
 * No network, token, files, Drive permissions, or SharedPreferences here.
 */
object LockerDriveReceiptPolicy {
    fun requireCompletedUpload(
        response: JSONObject,
        source: OfflineSong,
        expectedFolder: String,
        expectedSha256: String,
        expectedMd5: String
    ): LockerUploadOutcome {
        require(LockerAudioPolicy.validDriveId(expectedFolder)) {
            "Invalid expected Drive folder"
        }
        require(LockerAudioPolicy.validSha256(expectedSha256) &&
            expectedMd5.matches(Regex("[a-fA-F0-9]{32}"))) {
            "Invalid expected audio hashes"
        }
        val fields = response.optJSONObject("appProperties")
            ?: throw IllegalArgumentException("Missing upload file properties")
        val bytes = response.optString("size").toLongOrNull()
            ?: throw IllegalArgumentException("Upload size missing")
        val track = LockerAudioPolicy.requireDownloadable(LockerRemoteTrack(
            response.optString("id"), response.optString("name"),
            response.optString("mimeType"), bytes,
            response.optString("md5Checksum"),
            fields.optString("nightwave_sha256"),
            fields.optString("nightwave_local_id")
        ))
        val parents = response.optJSONArray("parents")
            ?: throw IllegalArgumentException("Upload parent folder missing")
        val correctFolder = (0 until parents.length()).any {
            parents.optString(it) == expectedFolder
        }
        require(
            track.bytes == source.bytes &&
            track.mimeType == source.mimeType &&
            track.name == LockerAudioPolicy.fileName(source) &&
            track.sourceLocalId == source.id &&
            track.md5.equals(expectedMd5, ignoreCase = true) &&
            track.sha256 == expectedSha256 &&
            correctFolder
        ) {
            "Drive completion metadata does not match the original local audio"
        }
        return LockerUploadOutcome(track, created = true)
    }
}
