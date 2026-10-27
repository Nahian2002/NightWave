package com.nightwave.player.v2.cloud

/** Last checked access is distinct from a historical verified backup receipt. */
enum class CloudAccessState { UNKNOWN, CHECKED, NEEDS_REVIEW }

object CloudAccessPolicy {
    fun afterFailure(
        previous: CloudAccessState, stage: CloudAuthorizationFailurePolicy.Stage, code: Int?
    ): CloudAccessState = when {
        stage != CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK && code in setOf(16, 12501) -> previous
        stage == CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK && code in setOf(401, 403) ->
            CloudAccessState.NEEDS_REVIEW
        stage != CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK && code in setOf(4, 10, 17) ->
            CloudAccessState.NEEDS_REVIEW
        else -> CloudAccessState.UNKNOWN // A network failure does not prove a revoked grant.
    }

    fun label(connected: Boolean, state: CloudAccessState, checkedAtMs: Long): String = when {
        !connected -> "On this device"
        state == CloudAccessState.NEEDS_REVIEW -> "Review Drive access"
        state == CloudAccessState.CHECKED && checkedAtMs > 0L -> "Account checked"
        else -> "Account saved · access not checked"
    }

    fun needsHelp(connected: Boolean, state: CloudAccessState, hasFailure: Boolean): Boolean =
        !connected || state == CloudAccessState.NEEDS_REVIEW || hasFailure

    fun compactBadge(connected: Boolean, state: CloudAccessState, backupBadge: String): String = when {
        !connected -> "LOCAL"
        state == CloudAccessState.NEEDS_REVIEW -> "CHECK ACCESS"
        state == CloudAccessState.UNKNOWN -> "SAVED"
        else -> backupBadge
    }
}
