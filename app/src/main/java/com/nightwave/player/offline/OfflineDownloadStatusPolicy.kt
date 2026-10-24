package com.nightwave.player.offline

/**
 * Finished transfer metadata cannot prove continued access to an offline
 * recording. This pure policy requires a readable indexed file at the time
 * the Downloads UI is rendered. Deleted/corrupt tracks are not "Ready".
 */
object OfflineDownloadStatusPolicy {
    fun isReady(
        workSucceeded: Boolean,
        savedId: String?,
        currentlyAvailableIds: Set<String>
    ): Boolean = workSucceeded && !savedId.isNullOrBlank() &&
        savedId in currentlyAvailableIds

    fun statusForCompleted(ready: Boolean): String =
        if (ready) "Ready offline · Verified local file"
        else "Saved previously · Local audio is missing or unavailable"
}
