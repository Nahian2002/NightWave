package com.nightwave.player.offline

/**
 * NW62 — snapshots for optional Downloads UI file-health verification.
 * Only a file validated by the private library may be marked Ready Offline.
 * Cache equality compares completed saved-file identities, not progress ticks.
 */
internal object DownloadFileVerificationPolicy {
    fun needsRead(ids: Set<String>, lastRequested: Set<String>?): Boolean =
        lastRequested == null || ids != lastRequested

    fun canPublish(generation: Int, newestGeneration: Int,
                   isFinishing: Boolean, isDestroyed: Boolean): Boolean =
        generation >= 0 && generation == newestGeneration &&
            !isFinishing && !isDestroyed

    fun ready(success: Boolean, savedId: String?,
              verifiedAvailable: Set<String>,
              checking: Boolean, verificationFailed: Boolean): Boolean =
        !checking && !verificationFailed &&
            OfflineDownloadStatusPolicy.isReady(success, savedId, verifiedAvailable)

    fun needsAttention(workState: String, savedId: String?,
                       verifiedAvailable: Set<String>, checking: Boolean,
                       verificationFailed: Boolean): Boolean {
        if (workState == "SUCCEEDED" && (checking || verificationFailed)) return false
        return DownloadCenterViewPolicy.needsAttention(
            workState, ready(workState == "SUCCEEDED", savedId,
                verifiedAvailable, checking, verificationFailed))
    }
}
