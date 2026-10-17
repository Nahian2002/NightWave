package com.nightwave.player.v2.ui

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.nightwave.player.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * No app launch, playlist mutation, playback, signing reset or USB device
 * access. Checks Android's actual merged-package entry point contracts.
 */
@RunWith(AndroidJUnit4::class)
class NightWaveDefaultEntryInstrumentedTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test fun launcherResolvesOnlyToPremiumNightWave() {
        val launch = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(context.packageName)
        val matches = context.packageManager.queryIntentActivities(
            launch, 0
        ).filter { it.activityInfo.packageName == context.packageName }
        assertEquals("NightWave must expose exactly one launcher", 1, matches.size)
        assertEquals(NightWaveStudioActivity::class.java.name,
            matches.single().activityInfo.name)
    }

    @Test fun sharedTextOpensPremiumAndClassicIsNotExternallyExposed() {
        val shared = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .setPackage(context.packageName)
        val matches = context.packageManager.queryIntentActivities(
            shared, PackageManager.MATCH_DEFAULT_ONLY
        ).filter { it.activityInfo.packageName == context.packageName }
        assertEquals("Only the premium UI handles shared text", 1, matches.size)
        assertEquals(NightWaveStudioActivity::class.java.name,
            matches.single().activityInfo.name)

        val classicInfo = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java), 0
        )
        val premiumInfo = context.packageManager.getActivityInfo(
            ComponentName(context, NightWaveStudioActivity::class.java), 0
        )
        assertFalse("Classic fallback must remain private", classicInfo.exported)
        assertTrue("Premium launcher must be exported", premiumInfo.exported)
    }
}
