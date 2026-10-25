package com.nightwave.player.v2.cloud

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import java.security.MessageDigest

/**
 * Read-only public APK identity. No Google email, access token, auth result,
 * app account storage or keystore private material is accessed.
 */
object InstalledApkOAuthIdentity {
    @Suppress("DEPRECATION")
    fun signingSha1(context: Context): String? = runCatching {
        val manager = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else info.signatures
        val certificate = signatures?.singleOrNull() ?: return@runCatching null
        MessageDigest.getInstance("SHA-1").digest(certificate.toByteArray())
            .joinToString(":") { "%02X".format(it.toInt() and 0xFF) }
    }.getOrNull()

    /** Copyable, credential-free checklist; same signature used on this phone. */
    fun report(context: Context): String {
        val packageName = context.packageName
        val fingerprint = signingSha1(context) ?: "Unavailable - use check-google-oauth.cmd"
        val serviceCode = runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        }.getOrDefault(-1)
        val serviceStatus = if (serviceCode == ConnectionResult.SUCCESS) {
            "Available"
        } else "Needs attention (code $serviceCode)"
        return listOf(
            "NightWave Google Drive authorization setup",
            "APK installed on this device (public details only)",
            "Android package: $packageName",
            "APK signing certificate SHA-1: $fingerprint",
            "Google Play services: $serviceStatus",
            "Requested Drive scope: https://www.googleapis.com/auth/drive.appdata",
            "",
            "In Google Cloud Console, check the same project:",
            "1. Google Auth Platform > Clients > Android OAuth client.",
            "2. Confirm package AND signing SHA-1 match the values above.",
            "3. Enable the Google Drive API in this project.",
            "4. Set up the OAuth consent audience, and add this Google account",
            "   as a test user if the app is in Testing mode.",
            "5. Ensure Google Play services are current, then retry.",
            "",
            "Status 8 means Google reported an internal authorization error.",
            "This checklist helps diagnose configuration; it cannot guarantee",
            "connection or change Google's OAuth registration automatically.",
            "Never paste OAuth access tokens, client secrets or passwords."
        ).joinToString("\n")
    }
}
