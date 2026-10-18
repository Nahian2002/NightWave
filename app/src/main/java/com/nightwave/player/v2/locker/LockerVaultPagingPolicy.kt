package com.nightwave.player.v2.locker

/**
 * Pure guards for in-memory Drive pagination and targeted duplicate queries.
 * Page tokens are opaque and never logged, persisted or included in errors.
 */
internal object LockerVaultPagingPolicy {
    fun acceptNextToken(next: String, previouslySeen: MutableSet<String>): String {
        require(next.length in 1..2048 && previouslySeen.add(next)) {
            "Invalid or repeated Drive continuation token"
        }
        return next
    }

    fun uploadChecksumQuery(folderId: String, sha256: String): String {
        require(LockerAudioPolicy.validDriveId(folderId))
        require(LockerAudioPolicy.validSha256(sha256))
        return "'$folderId' in parents and trashed = false and " +
            "appProperties has { key='nightwave_sha256' and value='$sha256' }"
    }

    fun hasUploadMatch(
        remote: LockerRemoteTrack,
        sha256: String,
        bytes: Long,
        mimeType: String,
        md5: String
    ): Boolean {
        LockerAudioPolicy.validateRemote(remote)
        require(remote.sha256 == sha256) { "Unexpected targeted checksum result" }
        return remote.bytes == bytes && remote.mimeType == mimeType &&
            remote.md5.equals(md5, ignoreCase = true)
    }
}
