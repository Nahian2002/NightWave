package com.nightwave.player.updates

internal enum class UpdateCheckStatus { DISABLED, NO_COMPATIBLE, AVAILABLE, OFFLINE, UNAVAILABLE, INVALID_METADATA }
internal data class UpdateCheckResult(val status: UpdateCheckStatus, val update: ApprovedUpdate? = null)
internal class UpdateCheckException(val status: UpdateCheckStatus) : Exception(status.name)

internal object UpdateCheckPolicy {
    // Called only AFTER metadata authentication. HTTP errors cannot supply enabled=false.
    fun authenticated(enabled: Boolean, installed: Long, release: Long, device: Int, minimum: Int?): UpdateCheckStatus = when {
        !enabled -> UpdateCheckStatus.DISABLED
        release <= installed || (minimum != null && minimum > device) -> UpdateCheckStatus.NO_COMPATIBLE
        else -> UpdateCheckStatus.AVAILABLE
    }
}
