package com.nightwave.player.v2.cloud

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Reads only this installed APK's public package and signing certificate. */
@RunWith(AndroidJUnit4::class)
class InstalledApkOAuthIdentityInstrumentedTest {
    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test fun fingerprintIsFromInstalledApkAndUsesGoogleConsoleHexFormat() {
        val fingerprint = InstalledApkOAuthIdentity.signingSha1(app)
        assertNotNull("A normal NightWave APK has one signing certificate", fingerprint)
        assertTrue(Regex("([0-9A-F]{2}:){19}[0-9A-F]{2}").matches(fingerprint!!))
    }

    @Test fun reportIncludesOnlyPublicSetupFactsAndNotAccountCredentials() {
        val report = InstalledApkOAuthIdentity.report(app)
        assertTrue(report.contains("Android package: " + app.packageName))
        assertTrue(report.contains("Google Play services: "))
        assertTrue(report.contains("drive.appdata"))
        assertTrue(report.contains("APK signing certificate SHA-1"))
        assertTrue(report.contains("Never paste OAuth access tokens"))
        assertEquals("com.nightwave.player", app.packageName)
    }
}
