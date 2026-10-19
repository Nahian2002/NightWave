package com.nightwave.player.auto

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.playback.PlaybackService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Safe, read-only instrumentation; never selects or plays personal tracks. */
@RunWith(AndroidJUnit4::class)
class NightWaveAutoBrowserIntegrationTest {
    @Test fun playbackServiceStillOwnsTheSingleMediaLibraryService() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val info = context.packageManager.getServiceInfo(
            ComponentName(context, PlaybackService::class.java), 0)
        assertNotNull(info)
        assertTrue(info.exported)
        assertTrue(info.foregroundServiceType and
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK != 0)
    }

    @Test fun browserCanReadRootAndNoPlayIsTriggered() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val pending = MediaBrowser.Builder(context, token).buildAsync()
        try {
            val browser = pending.get(10, TimeUnit.SECONDS)
            val wasPlaying = browser.isPlaying
            val wasSelected = browser.currentMediaItem?.mediaId
            val root = browser.getLibraryRoot(null).get(10, TimeUnit.SECONDS)
            assertEquals(AutoBrowsePolicy.ROOT, root.value?.mediaId)
            val sections = browser.getChildren(
                AutoBrowsePolicy.ROOT, 0, 50, null).get(10, TimeUnit.SECONDS)
            assertNotNull(sections.value)
            assertFalse(sections.value.orEmpty().isEmpty())
            // Browsing must preserve existing playback too: avoid a test
            // assumption that the app was initially stopped.
            assertEquals(wasPlaying, browser.isPlaying)
            assertEquals(wasSelected, browser.currentMediaItem?.mediaId)
        } finally {
            MediaBrowser.releaseFuture(pending)
        }
    }
}
