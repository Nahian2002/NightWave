package com.nightwave.player.v2.cloud

/**
 * No tokens, account identifiers, response bodies or full request URLs in errors.
 * HTTP status only. Allows the UI/worker to avoid retrying permanent OAuth/API
 * configuration failures as if they were transient connectivity problems.
 */
class DriveHttpFailure(val statusCode: Int) :
    IllegalStateException("Google Drive request failed (HTTP $statusCode)")

object DriveHttpFailurePolicy {
    fun needsAccountAttention(code: Int): Boolean = code == 401 || code == 403
    fun mayRetry(code: Int): Boolean = code == 408 || code == 429 || code in 500..599

    fun message(code: Int): String = when (code) {
        401 -> "Google Drive authorization expired. Reconnect in NightWave settings."
        403 -> "Drive access was refused. Check OAuth setup, Drive API access or account permissions."
        404 -> "This Drive backup is unavailable. Select another snapshot."
        408, 429 -> "Drive is busy or timed out. Try again later."
        in 500..599 -> "Drive service is unavailable right now. Retry later."
        else -> "Google Drive request failed (HTTP $code). No local audio was changed."
    }
}
