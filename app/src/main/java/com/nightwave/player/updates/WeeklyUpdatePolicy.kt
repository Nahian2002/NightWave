package com.nightwave.player.updates

import java.net.URI
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.math.BigInteger

internal object WeeklyUpdatePolicy {
    const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    const val RETRY_MS = 24L * 60 * 60 * 1000
    const val MAX_APK_BYTES = 150L * 1024 * 1024
    fun minimumSdkAccepted(device: Int, archive: Int?, approved: Int?): Boolean {
        if (device < 23 || (approved != null && approved <= 0)) return false
        val minimum = if (device >= 24) archive else approved
        if (minimum == null || minimum <= 0 || minimum > device) return false
        return device < 24 || approved == null || approved == archive
    }
    fun due(now: Long, last: Long, interval: Long = WEEK_MS): Boolean =
        now > 0 && (last <= 0 || (now >= last && now - last >= interval))
    fun allowedApk(url: String): Boolean = runCatching {
        val u = URI(url)
        u.scheme == "https" && u.host == "github.com" && u.port == -1 &&
            u.rawUserInfo == null && u.rawQuery == null && u.rawFragment == null &&
            u.path.startsWith("/Nahian2002/NightWave/releases/download/") &&
            u.path.endsWith(".apk") && !u.rawPath.contains("%") && !u.path.contains("/../")
    }.getOrDefault(false)
    fun verify(payload: ByteArray, signature: ByteArray, modulus: ByteArray, exponent: ByteArray): Boolean =
        runCatching {
            val key = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(
                BigInteger(1, modulus), BigInteger(1, exponent)))
            Signature.getInstance("SHA256withRSA").run { initVerify(key); update(payload); verify(signature) }
        }.getOrDefault(false)
}
