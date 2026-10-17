package com.nightwave.player.library

/**
 * Explicit cutover contract. This release serializes legacy writes only.
 * Room and DataStore are still mirrors/read previews, NOT primary writers.
 *
 * Never flip the constant as a cosmetic feature toggle. Remove this gate
 * only in a separately reviewed Room-owner migration, with all evidence
 * recorded and rollback behavior exercised on a physical test device.
 */
object LibraryStoragePolicy {
    const val ROOM_PRIMARY_WRITES_ENABLED = false
    const val LEGACY_SINGLE_WRITER_ENABLED = true
    const val MANUAL_LAST_WRITE_RECOVERY_ENABLED = true
}

data class RoomPromotionEvidence(
    val androidBuildPassed: Boolean = false,
    val legacyDeviceTestsPassed: Boolean = false,
    val roomDeviceTestsPassed: Boolean = false,
    val parityVerified: Boolean = false,
    val backupVerified: Boolean = false,
    val rollbackVerified: Boolean = false,
    val legacyWritesDisabled: Boolean = false,
    val explicitUserApproval: Boolean = false
) {
    fun readyForPromotion(): Boolean =
        androidBuildPassed && legacyDeviceTestsPassed && roomDeviceTestsPassed &&
            parityVerified && backupVerified && rollbackVerified &&
            legacyWritesDisabled && explicitUserApproval
}
