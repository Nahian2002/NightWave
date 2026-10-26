package com.nightwave.player.v2.cloud

/**
 * NW53 consumer-facing, credential-free recovery instructions.
 * Google's numeric status is not proof of the cause. Do not persist tokens,
 * raw exception messages, account names, response bodies or intent extras.
 */
object CloudConnectionHelpPolicy {
    data class Guidance(
        val title: String,
        val detail: String,
        val nextStep: String,
        val offerSingleRetry: Boolean,
        val needsSetupReport: Boolean
    )

    fun guidance(stage: CloudAuthorizationFailurePolicy.Stage, code: Int?): Guidance {
        if (stage == CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK) {
            return when (code) {
                401 -> Guidance(
                    "Drive session expired",
                    "The permission sheet completed but Drive rejected the access token.",
                    "Reconnect Drive, then verify a new backup.", false, true)
                403 -> Guidance(
                    "Drive access needs attention",
                    "Google Drive refused the request. Confirm the Drive API, requested app-data scope and OAuth test-user access.",
                    "Copy the setup report and check Google Cloud.", false, true)
                408, 429, in 500..599 -> Guidance(
                    "Drive is temporarily unavailable",
                    "The permission step completed, but Drive did not finish verification.",
                    "Try connecting again when Google's service is available.", true, false)
                else -> Guidance(
                    "Drive verification incomplete",
                    "The account picker may have succeeded, but backup access has not been verified.",
                    "Copy the setup report if another attempt fails.", true, true)
            }
        }
        return when (code) {
            8 -> Guidance(
                "Google account connection interrupted",
                if (stage == CloudAuthorizationFailurePolicy.Stage.REQUEST)
                    "Google Play services returned internal error 8 before a verified account selection. Drive is not confirmed connected."
                else "Google Play services returned internal error 8 after account selection. The account picker appeared, but that does not mean Drive connected.",
                "Retry manually once. If it repeats, compare the installed APK signing SHA-1, package name, Google Play services and OAuth test-user setup.",
                true, true)
            10 -> Guidance(
                "Google setup requires correction",
                "Google identified an Android OAuth configuration problem; repeatedly retrying the same setup is unlikely to help.",
                "Copy the setup report. Match the installed package and SHA-1 in Google Cloud and verify the Drive API.",
                false, true)
            7, 13 -> Guidance(
                "Connection could not finish",
                "Google returned a temporary service or network error; your local music is unchanged.",
                "Check your connection and retry once.", true, false)
            16, 12501 -> Guidance(
                "Connection cancelled",
                "No Drive backup was created and your offline music remains local.",
                "Choose Connect Google Drive if you decide to continue.", false, false)
            else -> Guidance(
                "Google connection needs attention",
                "Drive access is not yet verified. This is optional and never replaces your local library.",
                "Copy the setup report to review Android OAuth and Google Play services status.",
                false, true)
        }
    }
}
