package com.nightwave.player.offline

/**
 * NW87: non-destructive, accurate recovery instructions for indexed audio.
 * No file is moved, deleted, repaired, downloaded or re-associated here.
 */
internal object OfflineLibraryRecoveryGuide {
    fun title(health: OfflineFileHealth): String = when (health) {
        OfflineFileHealth.AVAILABLE -> "Ready offline"
        OfflineFileHealth.MISSING -> "Original audio file missing"
        OfflineFileHealth.SIZE_CHANGED -> "Saved audio size changed"
        OfflineFileHealth.UNREADABLE -> "Saved audio cannot be read"
        OfflineFileHealth.UNSAFE_METADATA -> "Saved audio metadata needs review"
    }

    fun help(health: OfflineFileHealth): String = when (health) {
        OfflineFileHealth.AVAILABLE ->
            "This indexed copy is available for local playback. " +
                "You can open it without an internet connection."
        OfflineFileHealth.MISSING ->
            "The saved index still has this entry, but its local audio file is missing. " +
                "If you have the original authorized recording, import it again. " +
                "Recheck files after restoring storage. Nothing was deleted."
        OfflineFileHealth.SIZE_CHANGED ->
            "The file no longer matches its recorded size. Playback is blocked " +
                "until checked. Preserve the current file and your backup; " +
                "re-import a trusted copy rather than forcing playback."
        OfflineFileHealth.UNREADABLE ->
            "The audio file is not readable as regular private audio. " +
                "Check device storage and file accessibility, then recheck. " +
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
