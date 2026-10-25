package com.nightwave.player.offline

/**
 * NW62 - One visual row per searched song *within the chosen filter*.
 *
 * WorkManager keeps historical requests. Selecting the newest request before
 * filtering loses earlier READY rows when a later retry failed. Select the
 * requested state first, then deduplicate public search source identities.
 *
 * This is UI-only. It never changes WorkManager, audio, downloads or indices.
 */
object DownloadHistoryProjectionPolicy {
    data class Row(
        val workId: String,
        val retryKey: String?,
        val state: String,
        val availableFile: Boolean,
        val requestedAtMs: Long
    )

    fun select(rows: List<Row>, filter: DownloadCenterViewPolicy.Filter): List<String> {
        val selected = rows
            .asSequence()
            .filter { DownloadCenterViewPolicy.matches(filter, it.state, it.availableFile) }
            .sortedWith(compareBy<Row> {
                !DownloadCenterViewPolicy.isActive(it.state)
            }.thenByDescending { it.requestedAtMs }
                .thenBy { it.workId })
        val seen = HashSet<String>()
        return selected.filter { row ->
            row.retryKey == null || seen.add(row.retryKey)
        }.map { it.workId }.toList()
    }

    fun count(rows: List<Row>, filter: DownloadCenterViewPolicy.Filter): Int =
        select(rows, filter).size
}
