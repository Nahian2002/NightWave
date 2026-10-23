package com.nightwave.player.offline

/**
 * NW24 bounded scalable library policy. Keeps the NW1 SharedPreferences index
 * format and the exact song IDs; no migration/rekey or destructive cleanup.
 *
 * The JSON index still has a finite safe limit. Room migration and truly
 * unbounded libraries require separately verified data migration.
 */
object OfflineLibraryCapacity {
    const val MAX_ITEMS = 5_000
    const val MAX_LIBRARY_BYTES = 32L * 1024L * 1024L * 1024L
    const val MAX_INDEX_CHARS = 3_000_000
    const val MIN_FREE_HEADROOM_BYTES = 16L * 1024L * 1024L

    fun acceptsNew(indexedCount: Int, currentBytes: Long, addedBytes: Long): Boolean =
        indexedCount in 0 until MAX_ITEMS &&
            currentBytes in 0..MAX_LIBRARY_BYTES &&
            addedBytes > 0 &&
            addedBytes <= MAX_LIBRARY_BYTES - currentBytes

    fun canStage(availableBytes: Long, estimatedBytes: Long): Boolean =
        estimatedBytes > 0 &&
            estimatedBytes <= MAX_LIBRARY_BYTES &&
            availableBytes >= estimatedBytes + MIN_FREE_HEADROOM_BYTES

    fun safeIndex(raw: String): Boolean = raw.length <= MAX_INDEX_CHARS

    fun pageSize(): Int = 60
    fun pageCount(total: Int): Int =
        if (total <= 0) 1 else 1 + (total - 1) / pageSize()

    fun safePage(requested: Int, total: Int): Int =
        requested.coerceIn(0, pageCount(total) - 1)
}
