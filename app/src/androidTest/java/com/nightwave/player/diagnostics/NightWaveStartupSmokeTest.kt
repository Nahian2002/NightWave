package com.nightwave.player.diagnostics

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.ui.MainActivity
import com.nightwave.player.v2.ui.NightWaveStudioActivity
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator/disposable-profile only. Does not play music, import files,
 * modify playlists, clear application data, or open login flows.
 *
 * These instrumented tests are not part of the local Windows builder's
 * non-device :app:testDebugUnitTest task.
 */
@RunWith(AndroidJUnit4::class)
class NightWaveStartupSmokeTest {
    @Test
    fun classicPlayerLaunchesWithoutFatalException() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse("Classic NightWave should stay open", activity.isFinishing)
                assertFalse("Classic NightWave should not be destroyed", activity.isDestroyed)
            }
        }
    }

    @Test
    fun premiumPlayerLaunchesWithoutFatalException() {
        ActivityScenario.launch(NightWaveStudioActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse("Premium NightWave should stay open", activity.isFinishing)
                assertFalse("Premium NightWave should not be destroyed", activity.isDestroyed)
            }
        }
    }
}
