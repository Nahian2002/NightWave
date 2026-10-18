package com.nightwave.player.offline

/**
 * Shared by every OfflineAudioLibrary instance within NightWave's process.
 * All index read/modify/write operations that can add or remove private audio
 * must hold this monitor. Never synchronize solely on a particular Activity's
 * OfflineAudioLibrary instance: Activity instances have different monitors.
 */
internal object OfflineLibraryIndexLock {
    val monitor = Any()
}
