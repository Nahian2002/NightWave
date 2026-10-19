package com.nightwave.player.auto

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.atomic.AtomicReference
import com.nightwave.player.playback.PlaybackService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            val rootPending = AtomicReference<ListenableFuture<LibraryResult<MediaItem>>>()
            val childrenPending = AtomicReference<
                ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>>()
            var wasPlaying = false
            var wasSelected: String? = null
            // MediaBrowser/MediaController commands require their application Looper.
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                wasPlaying = browser.isPlaying
                wasSelected = browser.currentMediaItem?.mediaId
                rootPending.set(browser.getLibraryRoot(null))
                childrenPending.set(browser.getChildren(
                    AutoBrowsePolicy.ROOT, 0, 50, null))
            }
            val root = rootPending.get().get(10, TimeUnit.SECONDS)
            val sections = childrenPending.get().get(10, TimeUnit.SECONDS)
            assertEquals(AutoBrowsePolicy.ROOT, root.value?.mediaId)
            assertNotNull(sections.value)
            assertFalse(sections.value.orEmpty().isEmpty())
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertEquals(wasPlaying, browser.isPlaying)
                assertEquals(wasSelected, browser.currentMediaItem?.mediaId)
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                MediaBrowser.releaseFuture(pending)
            }
        }
    }
}
