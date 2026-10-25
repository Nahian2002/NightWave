package com.nightwave.player.v2.cloud

/**
 * Safe, deterministic OAuth failure descriptions. Never persist or display
 * account addresses, access tokens, pending intent data or exception strings.
 * Numeric status codes are supplied by Google's own ApiException.
 */
object CloudAuthorizationDiagnosticPolicy {
    fun resolution(resultOk: Boolean, hasData: Boolean, statusCode: Int?): String {
        if (statusCode != null) return apiStatus(statusCode)
        if (!resultOk) return if (hasData)
            "Google permission was not completed. Check your account selection and OAuth app setup."
        else "Google authorization closed without permission. Retry and finish Google's consent screen."
        return if (!hasData) "Google returned no permission result. Try again." else
            "Google returned a permission response that could not be verified."
    }

    fun apiStatus(code: Int): String = when (code) {
        10 -> "Google OAuth setup error (10). Check Android package, APK SHA-1, Drive API and test-user access."
        7 -> "Google authorization network error (7). Check internet and retry."
        8 -> "Google authorization internal error (8). Retry once; this status can be transient. " +
            "If it repeats, check Google Play services and open You > Account & backup > " +
            "Copy Google setup report for your installed APK signer SHA-1. " +
            "Verify your Android OAuth client and Drive API, or run check-google-oauth.cmd on Windows."
        13 -> "Google authorization error (13). Retry and verify your Android OAuth registration."
        16, 12501 -> "Google authorization was cancelled ($code). Finish the account consent screen."
        17, 4 -> "Google account access unavailable ($code). Check account and Play services."
        else -> "Google Drive authorization failed (status $code). Check OAuth setup and Play services."
    }
}
