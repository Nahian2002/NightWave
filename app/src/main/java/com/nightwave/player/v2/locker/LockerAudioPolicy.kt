package com.nightwave.player.v2.locker

import com.nightwave.player.offline.OfflineSong
import java.io.File
import java.security.MessageDigest

/** Drive audio is explicitly user-imported private media, never a stream URL. */
data class LockerRemoteTrack(
    val id: String, val name: String, val mimeType: String, val bytes: Long,
    val md5: String = "", val sha256: String = "",
    val sourceLocalId: String = ""
)
data class LockerUploadOutcome(val remote: LockerRemoteTrack, val created: Boolean)

object LockerAudioPolicy {
    const val MAX_FILE_BYTES = 250L * 1024L * 1024L
    const val CHUNK_BYTES = 1024 * 1024
    const val MAX_VISIBLE = 100
    const val FOLDER_NAME = "NightWave Music"
    private val id = Regex("[A-Za-z0-9_-]{8,200}")
    private val privateId = Regex("[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)")
    private val suffixToMime = mapOf(
        "mp3" to "audio/mpeg", "m4a" to "audio/mp4", "aac" to "audio/aac",
        "flac" to "audio/flac", "wav" to "audio/wav", "ogg" to "audio/ogg",
        "opus" to "audio/ogg", "mp4" to "audio/mp4", "webm" to "audio/webm",
        "3gp" to "audio/3gpp"
    )
    fun validDriveId(value: String) = id.matches(value)
    fun validLocalId(value: String) = privateId.matches(value)
    fun extensionFor(mime: String, name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        require(suffixToMime[ext] == mime) { "Unsupported cloud audio type" }
        return ext
    }
    /**
     * Preserve the uploaded phone's private song ID only if there is no local
     * collision. Never rebind a different piece of audio to an occupied ID.
     */
    fun chooseRestoredId(remote: LockerRemoteTrack, occupied: (String) -> Boolean): String {
        val ext = extensionFor(remote.mimeType, remote.name)
        val preferred = remote.sourceLocalId
        if (validLocalId(preferred) && preferred.endsWith(".$ext") &&
            !occupied(preferred)) return preferred
        return java.util.UUID.randomUUID().toString() + "." + ext
    }

    fun safeTitle(name: String): String = name.substringBeforeLast('.')
        .replace(Regex("[\\p{Cntrl}/\\\\]"), " ")
        .trim().take(110).ifBlank { "Audio track" }
    fun validSha256(value: String) = Regex("[a-f0-9]{64}").matches(value)

    fun fileName(song: OfflineSong): String {
        require(privateId.matches(song.id)) { "Not an imported local audio ID" }
        val extension = song.id.substringAfterLast('.')
        require(suffixToMime[extension] == song.mimeType) { "Audio type mismatch" }
        val title = song.title.replace(Regex("[\\p{Cntrl}/\\\\]"), " ")
            .trim().take(110).ifBlank { "Audio track" }
        return "$title.$extension"
    }

    fun validateLocal(song: OfflineSong, file: File) {
        fileName(song)
        require(song.bytes in 1..MAX_FILE_BYTES &&
            file.isFile && file.canRead() && file.length() == song.bytes) {
            "Local audio missing or size changed"
        }
    }

    fun validateRemote(remote: LockerRemoteTrack): LockerRemoteTrack {
        require(validDriveId(remote.id) && remote.name.length in 1..180)
        require(remote.bytes in 1..MAX_FILE_BYTES)
        extensionFor(remote.mimeType, remote.name)
        require(remote.sourceLocalId.isBlank() || validLocalId(remote.sourceLocalId)) {
            "Invalid original song identifier"
        }
        require(remote.md5.isBlank() || Regex("[a-fA-F0-9]{32}").matches(remote.md5))
        require(remote.sha256.isBlank() || validSha256(remote.sha256))
        return remote
    }

    /** A download can only be trusted when both uploaded checksums are present. */
    fun requireDownloadable(remote: LockerRemoteTrack): LockerRemoteTrack {
        validateRemote(remote)
        require(remote.sha256.isNotBlank() &&
            remote.md5.matches(Regex("[a-fA-F0-9]{32}"))) {
            "Cloud audio lacks verified upload metadata"
        }
        return remote
    }

    fun hashes(file: File): Pair<String, String> {
        val sha = MessageDigest.getInstance("SHA-256")
        val md5 = MessageDigest.getInstance("MD5") // Drive's metadata checksum protocol
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                check(!Thread.currentThread().isInterrupted) { "File hashing cancelled" }
                val n = input.read(buffer)
                if (n < 0) break
                sha.update(buffer, 0, n)
                md5.update(buffer, 0, n)
            }
        }
        return Pair(sha.digest().joinToString("") { "%02x".format(it) },
            md5.digest().joinToString("") { "%02x".format(it) })
    }
}
