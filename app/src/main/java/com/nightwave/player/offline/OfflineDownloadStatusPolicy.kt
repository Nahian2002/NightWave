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

    /**
     * NW113: a quick read-only file/length check is NOT SHA-256 or an actual
     * decoder playback test. Keep completed-work and file health distinct.
     * A stale WorkManager SUCCESS must not conceal missing/corrupt-indexed
     * audio, and no UI status may trigger index deletion or silent repair.
     */
    fun statusForCompleted(
        ready: Boolean, health: OfflineFileHealth? = null
    ): String = when {
        ready -> "Ready offline · Local file present (size checked)"
        health == OfflineFileHealth.MISSING ->
            "Saved previously · Audio file missing (index retained)"
        health == OfflineFileHealth.SIZE_CHANGED ->
            "Saved previously · File size changed (not ready)"
        health == OfflineFileHealth.UNREADABLE ->
            "Saved previously · File unreadable (not ready)"
        health == OfflineFileHealth.UNSAFE_METADATA ->
            "Saved previously · Unsafe file metadata (not ready)"
        else -> "Saved previously · No available indexed audio"
    }
}
