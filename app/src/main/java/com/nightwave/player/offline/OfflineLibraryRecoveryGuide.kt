package com.nightwave.player.offline

/**
 * NW87: non-destructive, accurate recovery instructions for indexed audio.
 * No file is moved, deleted, repaired, downloaded or re-associated here.
 */
internal object OfflineLibraryRecoveryGuide {
    fun title(health: OfflineFileHealth): String = when (health) {
        OfflineFileHealth.AVAILABLE -> "Local audio available"
        OfflineFileHealth.MISSING -> "Original audio file missing"
        OfflineFileHealth.SIZE_CHANGED -> "Saved audio size changed"
        OfflineFileHealth.UNREADABLE -> "Saved audio cannot be read"
        OfflineFileHealth.UNSAFE_METADATA -> "Saved audio metadata needs review"
    }

    fun help(health: OfflineFileHealth): String = when (health) {
        OfflineFileHealth.AVAILABLE ->
            "This indexed copy is present, readable and matches its recorded size. " +
                "Use Check audio to probe its audio track and sample; actual playback and seeking depend on device support."
        OfflineFileHealth.MISSING ->
            "The saved index still has this entry, but its local audio file is missing. " +
                "If you have the original authorized recording, import it again. " +
                "Recheck files after restoring storage. Nothing was deleted."
        OfflineFileHealth.SIZE_CHANGED ->
            "The file no longer matches its recorded size. Playback is blocked " +
                "until checked. Preserve the current file and your backup; " +
                "re-import a trusted copy rather than forcing playback."
        OfflineFileHealth.UNREADABLE ->
            "NightWave could not read usable audio from this copy. " +
                "Check device storage and file accessibility, then check the audio again. " +
                "The index and any existing file remain untouched."
        OfflineFileHealth.UNSAFE_METADATA ->
            "The saved file's index metadata is inconsistent or unsafe. " +
                "Do not delete or automatically rebuild it; preserve the app data " +
                "and use a verified backup or trusted re-import."
    }

    fun emptyIssuesMessage(issues: Int): String =
        if (issues > 0)
            "$issues indexed audio " + (if (issues == 1) "entry needs" else "entries need") +
                " attention. Use the options button beside an affected track for recovery guidance."
        else "No indexed audio issues. All currently checked files appear available."

    fun recheckActionLabel() = "Recheck saved files"
}
