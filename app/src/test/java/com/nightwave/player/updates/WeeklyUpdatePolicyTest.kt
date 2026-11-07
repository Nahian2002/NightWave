package com.nightwave.player.updates

import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import org.junit.Assert.*
import org.junit.Test

class WeeklyUpdatePolicyTest {
    @Test fun android6FailsClosedWithoutSignedMinimumOrWhenReleaseNeedsNewerOs() {
        assertFalse(WeeklyUpdatePolicy.minimumSdkAccepted(23, null, null))
        assertFalse(WeeklyUpdatePolicy.minimumSdkAccepted(23, null, 24))
        assertFalse(WeeklyUpdatePolicy.minimumSdkAccepted(23, null, 0))
        assertTrue(WeeklyUpdatePolicy.minimumSdkAccepted(23, null, 23))
    }
    @Test fun newerAndroidComparesActualArchiveAndSignedMinimumWithLegacyFeedSupport() {
        assertTrue(WeeklyUpdatePolicy.minimumSdkAccepted(24, 23, 23))
        assertTrue(WeeklyUpdatePolicy.minimumSdkAccepted(26, 23, null))
        assertFalse(WeeklyUpdatePolicy.minimumSdkAccepted(24, 26, 23))
        assertFalse(WeeklyUpdatePolicy.minimumSdkAccepted(26, 24, 23))
        assertFalse(WeeklyUpdatePolicy.minimumSdkAccepted(24, null, 23))
    }
    @Test fun sevenDaysMeansOneLatestOfferRatherThanEveryBuild() {
        val start = 1_000_000L
        assertTrue(WeeklyUpdatePolicy.due(start, 0))
        assertFalse(WeeklyUpdatePolicy.due(start + WeeklyUpdatePolicy.WEEK_MS - 1, start))
        assertTrue(WeeklyUpdatePolicy.due(start + WeeklyUpdatePolicy.WEEK_MS, start))
        assertFalse(WeeklyUpdatePolicy.due(start - 1, start))
        assertFalse(WeeklyUpdatePolicy.due(0, start))
    }
    @Test fun retryIntervalAndOverflowNeverSpam() {
        assertFalse(WeeklyUpdatePolicy.due(100, Long.MAX_VALUE))
        assertFalse(WeeklyUpdatePolicy.due(100 + WeeklyUpdatePolicy.RETRY_MS - 1, 100, WeeklyUpdatePolicy.RETRY_MS))
        assertTrue(WeeklyUpdatePolicy.due(100 + WeeklyUpdatePolicy.RETRY_MS, 100, WeeklyUpdatePolicy.RETRY_MS))
    }
    @Test fun exactReleaseOwnerAndHttpsAreRequired() {
        val path = "/Nahian2002/NightWave/releases/download/stable/app.apk"
        assertTrue(WeeklyUpdatePolicy.allowedApk("https://github.com$path"))
        for (url in listOf("http://github.com$path", "https://github.com.evil.test$path",
            "https://user@github.com$path", "https://github.com:443$path", "https://github.com$path?x=1",
            "https://github.com$path#x", "https://github.com/other/NightWave/releases/download/x/app.apk",
            "https://github.com/Nahian2002/NightWave/releases/download/%2e%2e/app.apk")) {
            assertFalse(url, WeeklyUpdatePolicy.allowedApk(url))
        }
    }
    @Test fun privateCredentialsAreOnlyForTheExactGithubFeedOrAssetApi() {
        val feed = PrivateGithubUpdatePolicy.FEED
        val asset = PrivateGithubUpdatePolicy.asset(721466240)
        for (endpoint in listOf(feed, asset)) {
            assertTrue(PrivateGithubUpdatePolicy.canSendCredential(java.net.URL(endpoint), endpoint))
            for (host in listOf("release-assets.githubusercontent.com", "api.github.com.example.org")) {
                val redirect = java.net.URL(endpoint.replace("api.github.com", host))
                assertFalse(PrivateGithubUpdatePolicy.canSendCredential(redirect, endpoint))
            }
        }
        assertFalse(PrivateGithubUpdatePolicy.canSendCredential(java.net.URL(asset), feed))
        val cdn = java.net.URL("https://release-assets.githubusercontent.com/object?signature=fixture")
        assertTrue(PrivateGithubUpdatePolicy.isAssetRedirect(cdn))
        assertFalse(PrivateGithubUpdatePolicy.isAssetRedirect(java.net.URL(asset)))
        assertFalse(PrivateGithubUpdatePolicy.isAssetRedirect(java.net.URL("https://release-assets.githubusercontent.com:8443/object")))
    }

    @Test fun privateAccessRejectsClassicTokensAndHeaderInjection() {
        val token = "github_pat_" + "X".repeat(30)
        assertTrue(PrivateGithubUpdatePolicy.validToken(token))
        assertFalse(PrivateGithubUpdatePolicy.validToken("ghp_" + "X".repeat(30)))
        assertFalse(PrivateGithubUpdatePolicy.validToken(token + "\r\n"))
        assertTrue(PrivateGithubUpdatePolicy.needsAccess(401))
        assertTrue(PrivateGithubUpdatePolicy.needsAccess(403))
        assertTrue(PrivateGithubUpdatePolicy.needsAccess(404))
        assertFalse(PrivateGithubUpdatePolicy.needsAccess(429))
    }

    @Test fun signedPayloadRejectsTamperingAndAnotherKey() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val public = pair.public as RSAPublicKey
        val bytes = "approved release 42".toByteArray()
        val signed = Signature.getInstance("SHA256withRSA").run { initSign(pair.private); update(bytes); sign() }
        assertTrue(WeeklyUpdatePolicy.verify(bytes, signed, public.modulus.toByteArray(), public.publicExponent.toByteArray()))
        assertFalse(WeeklyUpdatePolicy.verify("release 43".toByteArray(), signed, public.modulus.toByteArray(), public.publicExponent.toByteArray()))
        signed[0] = (signed[0].toInt() xor 1).toByte()
        assertFalse(WeeklyUpdatePolicy.verify(bytes, signed, public.modulus.toByteArray(), public.publicExponent.toByteArray()))
    }
}
