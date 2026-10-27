package com.nightwave.player.offline

/**
 * Lightweight, read-only availability check for app-owned audio.
 * Matching file size is NOT a cryptographic integrity verification.
 * No repair, deletion, renaming or index mutation is authorized here.
 */
enum class OfflineFileHealth {
    AVAILABLE,
    MISSING,
    SIZE_CHANGED,
    UNREADABLE,
    UNSAFE_METADATA
}

data class OfflineLibraryHealthEntry(
    val song: OfflineSong,
    val health: OfflineFileHealth
)

data class OfflineLibraryHealthReport(
    val indexReadable: Boolean,
    val entries: List<OfflineLibraryHealthEntry>
) {
    val attentionCount: Int
        get() = entries.count { it.health != OfflineFileHealth.AVAILABLE }
    val playableCount: Int
        get() = entries.size - attentionCount
}

/**
 * NW113: one read-only batch snapshot for transfer UI and search consumers.
 * A broken index is NOT proof that every saved audio file was deleted.
 * AVAILABLE here means safe path + readable regular file + expected length;
 * full checksum/decoder success must not be inferred from this quick probe.
 */
data class OfflineAvailabilitySnapshot(
    val indexReadable: Boolean,
    val healthById: Map<String, OfflineFileHealth>
) {
    val availableIds: Set<String>
        get() = if (!indexReadable) emptySet() else
            healthById.filterValues { it == OfflineFileHealth.AVAILABLE }.keys

    fun health(id: String?): OfflineFileHealth? =
        if (!indexReadable || id.isNullOrBlank()) null else healthById[id]
}

/** Pure policy for testable and conservative read-only classification. */
internal object OfflineLibraryHealthPolicy {
    fun classify(
        expectedBytes: Long,
        exists: Boolean,
        regularFile: Boolean,
        readable: Boolean,
        actualBytes: Long
    ): OfflineFileHealth = when {
        expectedBytes !in 1L..OfflineAudioLibrary.MAX_SONG_BYTES ->
            OfflineFileHealth.UNSAFE_METADATA
        !exists -> OfflineFileHealth.MISSING
        !regularFile || !readable -> OfflineFileHealth.UNREADABLE
        actualBytes != expectedBytes -> OfflineFileHealth.SIZE_CHANGED
        else -> OfflineFileHealth.AVAILABLE
    }
}
