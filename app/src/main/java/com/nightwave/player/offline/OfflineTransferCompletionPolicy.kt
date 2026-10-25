package com.nightwave.player.offline

/**
 * NW66: A download may report SUCCESS only when its saved private-library ID
 * was independently observed after the import finished, and the worker has
 * not been stopped. A WorkManager progress count or downloaded byte count is
 * never proof that indexed, readable offline audio exists.
 *
 * This pure policy does not mutate the file, the index or any WorkManager job.
 */
internal object OfflineTransferCompletionPolicy {
    fun canCommit(wasStopped: Boolean, savedId: String?,
                  verifiedAvailableIds: Set<String>): Boolean =
        !wasStopped && !savedId.isNullOrBlank() &&
            savedId in verifiedAvailableIds

    const val NOT_VERIFIED =
        "Saved audio could not be verified. Check Offline Library before retrying."
}
