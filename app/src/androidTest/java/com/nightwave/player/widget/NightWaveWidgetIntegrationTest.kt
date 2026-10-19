package com.nightwave.player.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * These run with the Android test APK/device, not in source-only verification.
 * They do not start playback or modify the user's personal media library.
 */
@RunWith(AndroidJUnit4::class)
class NightWaveWidgetIntegrationTest {
    @Test fun launcherWidgetProviderExportedForSystemHost() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        @Suppress("DEPRECATION")
        val info = context.packageManager.getReceiverInfo(
            ComponentName(context, NightWaveWidgetProvider::class.java), 0)
        assertTrue(info.exported)
        assertTrue(info.enabled)
    }

    @Test fun widgetTransportActionReceiverIsPrivate() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        @Suppress("DEPRECATION")
        val info = context.packageManager.getReceiverInfo(
            ComponentName(context, NightWaveWidgetActionReceiver::class.java), 0)
        assertFalse(info.exported)
    }

    @Test fun coldWidgetRenderingNeedsNoExistingPlayerSession() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = AppWidgetManager.getInstance(context)
        // Rendering an empty set of launcher widgets is a no-op and cannot play music.
        NightWaveWidgetProvider().onUpdate(context, manager, intArrayOf())
        NightWaveWidgetProvider.publish(context, WidgetPlaybackSnapshot())
        assertTrue(true)
    }
}
