package com.nightwave.player.v2.cloud

import android.content.Context

/**
 * Keeps only the last numeric Google status and failure stage to diagnose
 * errors from the installed APK. No account/email/token or exception text.
 */
class CloudAuthorizationFailureStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_cloud_auth_diagnostic_v1", Context.MODE_PRIVATE
    )

    data class LastFailure(val stage: CloudAuthorizationFailurePolicy.Stage, val code: Int?)

    fun read(): LastFailure? = runCatching {
        val stage = CloudAuthorizationFailurePolicy.supportedStage(
            prefs.getString("stage", null)) ?: return@runCatching null
        val raw = prefs.getInt("status_code", Int.MIN_VALUE)
        LastFailure(stage, raw.takeUnless { it == Int.MIN_VALUE })
    }.getOrNull()

    /** No raw Google error bodies, OAuth extras or authorization data. */
    fun record(stage: CloudAuthorizationFailurePolicy.Stage, code: Int?): Boolean =
        prefs.edit()
            .putString("stage", stage.name)
            .putInt("status_code", code?.takeIf { it in 0..999_999 } ?: Int.MIN_VALUE)
            .commit()

    fun clear(): Boolean = prefs.edit().clear().commit()
}
