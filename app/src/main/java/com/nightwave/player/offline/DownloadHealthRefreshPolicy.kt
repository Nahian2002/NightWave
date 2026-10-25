package com.nightwave.player.offline

/**
 * NW62 — only the newest, foreground-owned disk-health snapshot can update
 * Downloads UI. This is a presentation policy; never changes private audio,
 * WorkManager progress, metadata, saved files or queues.
 */
internal object DownloadHealthRefreshPolicy {
    fun mayPublish(
        submittedGeneration: Long,
        currentGeneration: Long,
        paused: Boolean,
        finishing: Boolean,
        destroyed: Boolean
    ): Boolean = submittedGeneration > 0L &&
        submittedGeneration == currentGeneration &&
        !paused && !finishing && !destroyed
}
