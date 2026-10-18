package com.nightwave.player.v2.locker

/**
 * Metadata-only view of the explicitly authorized NightWave Music folder.
 *
 * A capped Drive listing must never be presented as the user's entire
 * Google Drive library or as their account's total storage usage.
 */
data class LockerCloudInventory(
    val tracks: List<LockerRemoteTrack>,
    val hasMore: Boolean = false,
    val pagesRead: Int = 0
)

enum class LockerVaultFilter { ALL, VERIFIED, NEEDS_ATTENTION }
enum class LockerVaultSort { NAME, LARGEST, SMALLEST }

data class LockerVaultSummary(
    val listedCount: Int,
    val verifiedCount: Int,
    val needsAttentionCount: Int,
    val listedBytes: Long,
    val partialListing: Boolean
)

object LockerVaultBrowsePolicy {
    const val MAX_PAGES = 5
    const val PAGE_SIZE = 100
    const val PAGE_INCREMENT = 40
    const val MAX_FILTER_LENGTH = 100

    fun downloadable(track: LockerRemoteTrack): Boolean =
        runCatching { LockerAudioPolicy.requireDownloadable(track) }.isSuccess

    /** No network, tokens, file I/O or mutation; safe for repeated search input. */
    fun browse(
        inventory: LockerCloudInventory,
        query: String,
        filter: LockerVaultFilter = LockerVaultFilter.ALL,
        sort: LockerVaultSort = LockerVaultSort.NAME
    ): List<LockerRemoteTrack> {
        val term = query.trim().take(MAX_FILTER_LENGTH)
        val filtered = inventory.tracks.asSequence()
            .filter { term.isBlank() || it.name.contains(term, ignoreCase = true) }
            .filter { track ->
                when (filter) {
                    LockerVaultFilter.ALL -> true
                    LockerVaultFilter.VERIFIED -> downloadable(track)
                    LockerVaultFilter.NEEDS_ATTENTION -> !downloadable(track)
                }
            }.toList()
        return when (sort) {
            LockerVaultSort.NAME -> filtered.sortedWith(
                compareBy<LockerRemoteTrack> { it.name.lowercase(java.util.Locale.ROOT) }
                    .thenBy { it.id })
            LockerVaultSort.LARGEST -> filtered.sortedWith(
                compareByDescending<LockerRemoteTrack> { it.bytes }
                    .thenBy { it.name.lowercase(java.util.Locale.ROOT) }
                    .thenBy { it.id })
            LockerVaultSort.SMALLEST -> filtered.sortedWith(
                compareBy<LockerRemoteTrack> { it.bytes }
                    .thenBy { it.name.lowercase(java.util.Locale.ROOT) }
                    .thenBy { it.id })
        }
    }

    fun summary(inventory: LockerCloudInventory): LockerVaultSummary {
        val verified = inventory.tracks.count(::downloadable)
        return LockerVaultSummary(
            listedCount = inventory.tracks.size,
            verifiedCount = verified,
            needsAttentionCount = inventory.tracks.size - verified,
            listedBytes = inventory.tracks.fold(0L) { sum, track ->
                // Each validated file is <=250 MiB; 500 entries cannot overflow Long.
                sum + track.bytes.coerceAtLeast(0L)
            },
            partialListing = inventory.hasMore
        )
    }

    /** UI display budget; never silently hide filtered rows without a More control. */
    fun visible(filtered: List<LockerRemoteTrack>, count: Int): List<LockerRemoteTrack> =
        filtered.take(count.coerceIn(0, MAX_PAGES * PAGE_SIZE))

    fun bytesLabel(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        val mib = 1024L * 1024L
        return if (safe >= mib) "${safe / mib} MiB"
        else "${(safe + 1023L) / 1024L} KiB"
    }
}
