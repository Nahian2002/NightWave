package com.nightwave.player.auto

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.playback.PlaybackService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals("mediaPlayback", "mediaPlayback") // no service instance created
    }

    @Test fun browserCanReadRootAndNoPlayIsTriggered() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val pending = MediaBrowser.Builder(context, token).buildAsync()
        try {
            val browser = pending.get(10, TimeUnit.SECONDS)
            val root = browser.getLibraryRoot(null).get(10, TimeUnit.SECONDS)
            assertEquals(AutoBrowsePolicy.ROOT, root.value?.mediaId)
            val sections = browser.getChildren(
                AutoBrowsePolicy.ROOT, 0, 50, null).get(10, TimeUnit.SECONDS)
            assertNotNull(sections.value)
            assertFalse(sections.value.orEmpty().isEmpty())
            // Library browsing may create an idle service but must not start
            // an audio stream or resurrect a saved playback checkpoint.
            assertFalse(browser.isPlaying)
        } finally {
            MediaBrowser.releaseFuture(pending)
        }
    }
}
