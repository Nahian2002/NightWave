package com.nightwave.player.v2.locker

/**
 * Local, optional metadata history only. No Google account, remote file ID,
 * private staging path, OAuth token, resumable URL, or error text is recorded.
 */
enum class LockerTransferKind { UPLOAD, DOWNLOAD }
enum class LockerTransferState { STARTED, COMPLETED, REUSED, NOT_VERIFIED }

data class LockerTransferRecord(
    val id: String,
    val kind: LockerTransferKind,
    val state: LockerTransferState,
    val title: String,
    val bytes: Long,
    val timeMs: Long
)

data class LockerTransferHistorySnapshot(
    val entries: List<LockerTransferRecord>,
    val unreadable: Boolean = false
)

object LockerTransferHistoryPolicy {
    const val MAX_ENTRIES = 30
    const val MAX_BYTES = 16 * 1024
    const val MAX_TITLE = 72

    /** STARTED after process death is uncertain, NOT evidence of a failed upload. */
    fun description(state: LockerTransferState): String = when (state) {
        LockerTransferState.STARTED -> "Unconfirmed · Check Drive or download checkpoint"
        LockerTransferState.COMPLETED -> "Verified complete"
        LockerTransferState.REUSED -> "Existing verified copy reused"
        LockerTransferState.NOT_VERIFIED -> "Interrupted or not verified"
    }

    fun displayTitle(source: String): String {
        val clean = source.replace(Regex("[\\p{Cntrl}/\\\\]"), " ")
            .replace(Regex("(?i)(https?://|bearer\\s|access_token|refresh_token)"), " ")
            .trim().take(MAX_TITLE)
        return clean.ifBlank { "Music file" }
    }

    fun valid(record: LockerTransferRecord): Boolean =
        record.id.matches(Regex("[0-9a-f-]{36}")) &&
            record.title.isNotBlank() && record.title.length <= MAX_TITLE &&
            record.title == displayTitle(record.title) &&
            record.bytes in 1L..LockerAudioPolicy.MAX_FILE_BYTES &&
            record.timeMs > 0L

    fun capped(newestFirst: List<LockerTransferRecord>): List<LockerTransferRecord> =
        newestFirst.distinctBy { it.id }.take(MAX_ENTRIES)

    fun finish(
        existing: List<LockerTransferRecord>,
        id: String,
        result: LockerTransferState
    ): List<LockerTransferRecord> {
        require(result != LockerTransferState.STARTED)
        // Refuse to invent a completed record from a missing journal entry.
        require(existing.any { it.id == id }) { "Transfer journal entry is missing" }
        return existing.map { if (it.id == id && it.state == LockerTransferState.STARTED)
            it.copy(state = result) else it }
    }
}
