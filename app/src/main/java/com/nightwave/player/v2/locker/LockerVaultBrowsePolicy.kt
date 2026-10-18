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
    val pagesRead: Int = 0,
    /** In-memory only; never persist or log Drive continuation tokens. */
    val nextPageToken: String? = null,
    val folderId: String? = null,
    val seenTokens: Set<String> = emptySet()
)

enum class LockerVaultFilter { ALL, VERIFIED, NEEDS_ATTENTION }
enum class LockerVaultSort { NAME, LARGEST, SMALLEST }

data class LockerVaultSummary(
    val listedCount: Int,
    val verifiedCount: Int,
    val needsAttentionCount: Int,
    val listedBytes: Long,
    val verifiedBytes: Long = 0L,
    val needsAttentionBytes: Long = 0L,
    val formatCounts: List<Pair<String, Int>> = emptyList(),
    val partialListing: Boolean
)

object LockerVaultBrowsePolicy {
    const val MAX_PAGES = 5 // bounded calls per explicit Refresh / Load More
    const val MAX_TOTAL_PAGES = 40 // safety cap; not a claim of complete Drive storage
    const val MAX_DUPLICATE_QUERY_PAGES = 20 // targeted SHA search must finish
    const val PAGE_SIZE = 100
    const val PAGE_INCREMENT = 40
    const val MAX_FILTER_LENGTH = 100

    fun canLoadMore(inventory: LockerCloudInventory): Boolean {
        val cursor = inventory.nextPageToken ?: return false
        return inventory.hasMore && inventory.pagesRead in 1 until MAX_TOTAL_PAGES &&
            cursor.length in 1..2048 &&
            cursor in inventory.seenTokens &&
            LockerAudioPolicy.validDriveId(inventory.folderId.orEmpty())
    }

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
        val accepted = inventory.tracks.filter(::downloadable)
        val verified = accepted.size
        val totalBytes = inventory.tracks.sumOf { it.bytes.coerceAtLeast(0L) }
        val verifiedBytes = accepted.sumOf { it.bytes.coerceAtLeast(0L) }
        val formats = inventory.tracks.groupingBy {
            it.name.substringAfterLast('.', "audio").uppercase(java.util.Locale.ROOT)
        }.eachCount().entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenBy { it.key }
        ).take(4).map { it.key to it.value }
        return LockerVaultSummary(
            listedCount = inventory.tracks.size,
            verifiedCount = verified,
            needsAttentionCount = inventory.tracks.size - verified,
            listedBytes = totalBytes,
            verifiedBytes = verifiedBytes,
            needsAttentionBytes = totalBytes - verifiedBytes,
            formatCounts = formats,
            partialListing = inventory.hasMore
        )
    }

    /** UI display budget; never silently hide filtered rows without a More control. */
    fun visible(filtered: List<LockerRemoteTrack>, count: Int): List<LockerRemoteTrack> =
        filtered.take(count.coerceIn(0, MAX_TOTAL_PAGES * PAGE_SIZE))

    fun bytesLabel(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        val mib = 1024L * 1024L
        return if (safe >= mib) "${safe / mib} MiB"
        else "${(safe + 1023L) / 1024L} KiB"
    }
}
