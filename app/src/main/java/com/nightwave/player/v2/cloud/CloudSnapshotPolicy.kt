package com.nightwave.player.v2.cloud

/**
 * The app-data backup envelope is checksum verified before any comparison.
 * Byte-wise equality alone can create needless snapshots when JSON order varies,
 * so compare the fully validated decoded content instead. No remote writes here.
 */
object CloudSnapshotPolicy {
    fun sameVerifiedContent(expected: ByteArray, remote: ByteArray): Boolean {
        val wanted = CloudBackupCodec.decode(expected)
        return try {
            wanted == CloudBackupCodec.decode(remote)
        } catch (_: Exception) {
            // A missing, corrupt or old remote snapshot is never treated as
            // proof that the new backup is already safely stored.
            false
        }
    }
}
