package com.nightwave.player.v2.cloud

import android.accounts.Account
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.Scope

/** Single narrow scope; no broad Drive, Gmail, or profile permissions. */
object NightWaveCloudAuthorization {
    const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"

    fun request(accountName: String? = null): AuthorizationRequest {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE)))
        if (!accountName.isNullOrBlank()) {
            builder.setAccount(Account(accountName, "com.google"))
        }
        return builder.build()
    }

    @Suppress("DEPRECATION")
    fun authorizedAccount(result: AuthorizationResult): String? =
        result.toGoogleSignInAccount()?.account?.name?.takeIf { it.contains('@') }

    fun requireToken(result: AuthorizationResult): String {
        check(!result.hasResolution()) { "Google permission requires user interaction" }
        check(SCOPE in result.grantedScopes) { "Drive backup scope not granted" }
        return requireNotNull(result.accessToken?.takeIf { it.isNotBlank() }) {
            "Google authorization did not provide a token"
        }
    }
}
