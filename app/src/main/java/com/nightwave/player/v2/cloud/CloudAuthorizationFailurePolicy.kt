package com.nightwave.player.v2.cloud

/**
 * Safe public failure metadata. The stage and Google's numeric status are
 * diagnostic only; never record email, account IDs, Intent data, access
 * tokens, refresh tokens or exception message strings.
 */
object CloudAuthorizationFailurePolicy {
    enum class Stage { REQUEST, ACCOUNT_PICKER, DRIVE_CHECK }

    fun stageLabel(stage: Stage): String = when (stage) {
        Stage.REQUEST -> "Before Google account picker"
        Stage.ACCOUNT_PICKER -> "After Google account picker"
        Stage.DRIVE_CHECK -> "After permission, during Drive verification"
    }

    fun shortMessage(code: Int?, stage: Stage): String {
        val where = when (stage) {
            Stage.REQUEST -> "before opening account selection"
            Stage.ACCOUNT_PICKER -> "after account selection"
            Stage.DRIVE_CHECK -> "while verifying Drive access"
        }
        return when (code) {
            8 -> "Google internal error (8) $where. Retry once; if it repeats, check Play services and app setup."
            10 -> "Google configuration error (10). Match the installed APK package and SHA-1 in Google Cloud."
            7 -> "Google network error (7). Check your connection before retrying."
            16, 12501 -> "Google authorization cancelled. Your local library was not changed."
            17, 4 -> "Google account services unavailable ($code). Update or enable Play services."
            13 -> "Google returned error (13) $where. Retry once if services are available."
            null -> "Google authorization could not finish $where. Try again or check the setup report."
            else -> "Google authorization error ($code) $where. See the setup report."
        }
    }

    /**
     * Only a *user initiated* repeat is allowed. Never silently relaunch
     * account permission or repeatedly retry an API configuration failure.
     */
    fun canSuggestRetry(code: Int?): Boolean = when (code) {
        7, 8, 13 -> true
        else -> false
    }

    fun supportedStage(raw: String?): Stage? =
        Stage.entries.firstOrNull { it.name == raw }
}
