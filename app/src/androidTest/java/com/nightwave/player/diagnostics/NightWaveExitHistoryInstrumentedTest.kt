package com.nightwave.player.diagnostics

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only test. Does not simulate a crash, clear an app, or read system logs. */
@RunWith(AndroidJUnit4::class)
class NightWaveExitHistoryInstrumentedTest {
    @Test fun compactSummaryUsesOnlyCoarseProcessExitReasons() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val summary = NightWaveExitHistory.report(context)
        assertTrue("Exit diagnostics should provide explanatory text", summary.isNotBlank())
        assertTrue("Exit summary must be bounded", summary.length < 1500)
        assertFalse("No private filesystem paths", summary.contains("/data/user/"))
        assertFalse("No system stack trace collection", summary.contains("at android."))
        assertFalse("No Android process identifiers", summary.contains("pid="))
    }

    @Test fun fatalExceptionRecorderCanBeReadWithoutModifyingMusicData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val summary = NightWaveCrashRecorder.report(context)
        assertTrue(summary.startsWith("NightWave"))
        assertTrue(summary.isNotBlank())
    }
}
