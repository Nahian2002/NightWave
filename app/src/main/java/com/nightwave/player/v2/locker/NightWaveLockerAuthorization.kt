package com.nightwave.player.v2.locker

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.Scope

/** Separate opt-in Drive file permission. Never expands appDataFolder backup auth. */
object NightWaveLockerAuthorization {
    const val SCOPE = "https://www.googleapis.com/auth/drive.file"
    fun request(accountName: String? = null): AuthorizationRequest {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE)))
        if (!accountName.isNullOrBlank())
            builder.setAccount(Account(accountName, "com.google"))
        return builder.build()
    }
    @Suppress("DEPRECATION")
    fun account(result: AuthorizationResult): String? =
        result.toGoogleSignInAccount()?.account?.name?.takeIf { it.contains('@') }

    fun token(result: AuthorizationResult): String {
        check(!result.hasResolution() && SCOPE in result.grantedScopes)
        return requireNotNull(result.accessToken?.takeIf { it.isNotBlank() })
    }
}

class LockerAccountState(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "nightwave_music_locker_v1", Context.MODE_PRIVATE)
    fun account(): String = preferences.getString("account", "").orEmpty()
    fun connect(name: String): Boolean {
        if (name.isBlank() || !name.contains('@')) return false
        val old = account()
        if (old.isNotBlank() && !old.equals(name, ignoreCase = true)) return false
        return preferences.edit().putString("account", name).commit()
    }
    fun disconnect(): Boolean = preferences.edit().remove("account").commit()
}
