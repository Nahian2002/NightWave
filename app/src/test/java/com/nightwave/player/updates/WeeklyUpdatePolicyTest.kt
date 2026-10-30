package com.nightwave.player.updates

import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import org.junit.Assert.*
import org.junit.Test

class WeeklyUpdatePolicyTest {
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
