package com.nightwave.player.offline

/**
 * NW87: honest, bounded labels for the Transfer Center.
 * Counts come from WorkManager and separately verified local-file snapshots.
 * This is presentation only; it does not mutate jobs, files or eligibility.
 */
internal object DownloadCenterDashboardPolicy {
    fun tab(filter: DownloadCenterViewPolicy.Filter, count: Int?, checkingFiles: Boolean): String {
        val name = DownloadCenterViewPolicy.label(filter)
        // An indeterminate file check must never display a misleading Ready 0
        // or Issues 0. WorkManager-active counts are authoritative immediately.
        if (checkingFiles && filter in setOf(
                DownloadCenterViewPolicy.Filter.READY,
                DownloadCenterViewPolicy.Filter.ATTENTION
            )) return "$name · …"
        if (count == null || count < 0) return name
        val digits = if (count > 99) "99+" else count.toString()
        return "$name · $digits"
    }

    fun overview(
        active: Int,
        ready: Int,
        issues: Int,
        total: Int,
        checkingFiles: Boolean,
        checkFailed: Boolean
    ): String = when {
        checkingFiles -> "Checking saved audio · ${active.coerceAtLeast(0)} active · " +
            "${total.coerceAtLeast(0)} transfer entries"
        checkFailed -> "Saved audio verification unavailable · Retry by reopening Transfers"
        else -> "${active.coerceAtLeast(0)} active · ${ready.coerceAtLeast(0)} ready offline · " +
            "${issues.coerceAtLeast(0)} need attention · ${total.coerceAtLeast(0)} transfers"
    }

    fun helpLine(): String =
        "Resume reuses checked bytes on supported sources; others restart safely. " +
        "Hide removes a history card, not its offline file."
}
