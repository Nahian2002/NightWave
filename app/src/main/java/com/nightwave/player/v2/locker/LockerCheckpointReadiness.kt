package com.nightwave.player.v2.locker

/** Read-only preview: does NOT download, truncate, delete or trust file bytes. */
enum class LockerCheckpointState {
    NONE, NEEDS_REPAIR, ACCOUNT_MISMATCH, READY, MISSING_DATA,
    SHORT_DATA, UNSAFE_STAGING, HASH_CHECK_PENDING
}

data class LockerCheckpointPreview(
    val state: LockerCheckpointState,
    val pending: LockerPendingDownload? = null,
    val currentFileBytes: Long = 0L
) {
    val canResume: Boolean
        get() = state == LockerCheckpointState.READY ||
            state == LockerCheckpointState.HASH_CHECK_PENDING

    val explanation: String
        get() = when (state) {
            LockerCheckpointState.NONE -> "No interrupted download."
            LockerCheckpointState.NEEDS_REPAIR ->
                "Recovery record unreadable. Explicitly discard the record before restarting."
            LockerCheckpointState.ACCOUNT_MISMATCH ->
                "Connect the original Google account to resume this download."
            LockerCheckpointState.MISSING_DATA ->
                "Temporary audio is missing. Discard the checkpoint before downloading again."
            LockerCheckpointState.SHORT_DATA ->
                "Temporary audio is shorter than the saved checkpoint. Discard it before retrying."
            LockerCheckpointState.UNSAFE_STAGING ->
                "Temporary storage path is unsafe or unreadable. No files were changed."
            LockerCheckpointState.READY ->
                "Resume from the saved byte checkpoint. Uncommitted extra bytes will be discarded."
            LockerCheckpointState.HASH_CHECK_PENDING ->
                "All bytes are recorded; final SHA-256 and MD5 checks are still required."
        }
}

object LockerCheckpointReadiness {
    fun classify(
        pending: LockerPendingDownload?,
        checkpointUnreadable: Boolean,
        sameAccount: Boolean,
        fileExists: Boolean,
        regularFile: Boolean,
        unsafePath: Boolean,
        actualBytes: Long
    ): LockerCheckpointState = when {
        pending == null -> if (checkpointUnreadable) LockerCheckpointState.NEEDS_REPAIR
            else LockerCheckpointState.NONE
        !sameAccount -> LockerCheckpointState.ACCOUNT_MISMATCH
        unsafePath || (fileExists && !regularFile) || actualBytes < 0L ->
            LockerCheckpointState.UNSAFE_STAGING
        !fileExists && pending.committedBytes > 0L -> LockerCheckpointState.MISSING_DATA
        fileExists && actualBytes < pending.committedBytes -> LockerCheckpointState.SHORT_DATA
        // The underlying downloader is create-only at offset zero. A file
        // already present cannot be trusted or replaced automatically.
        fileExists && pending.committedBytes == 0L -> LockerCheckpointState.UNSAFE_STAGING
        pending.committedBytes == pending.remote.bytes -> LockerCheckpointState.HASH_CHECK_PENDING
        else -> LockerCheckpointState.READY
    }
}
