package com.nightwave.player.playback

/**
 * Foreground-service Intent positions refer to the ORIGINAL submitted URL
 * array. Validate the selected identity before discarding invalid/duplicate
 * rows; otherwise the numeric position can silently point to another song.
 */
object PublicQueueIntentPolicy {
    fun retainedSelectionIndex(
        submittedUrls: List<String>,
        originalIndex: Int,
        validatedEntries: List<QueueEntry>
    ): Int? {
        val requested = submittedUrls.getOrNull(originalIndex) ?: return null
        if (!QueueRestorePolicy.isCanonicalPublicWatchUrl(requested)) return null
        return validatedEntries.indexOfFirst { it.url == requested }
            .takeIf { it >= 0 }
    }
}
