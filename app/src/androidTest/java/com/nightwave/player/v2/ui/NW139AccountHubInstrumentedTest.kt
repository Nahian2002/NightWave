package com.nightwave.player.v2.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.v2.cloud.*
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.model.ProfileSettings
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Real account hub, isolated preferences and synthetic callbacks. Never calls Google. */
@RunWith(AndroidJUnit4::class)
class NW139AccountHubInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private class Sandbox(base: Context) : ContextWrapper(base) {
        private val suffix = "_nw139_hub_" + UUID.randomUUID()
        private val names = mutableSetOf<String>()
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            names += name; return super.getSharedPreferences(name + suffix, mode)
        }
        fun clear() { names.forEach { super.getSharedPreferences(it + suffix, MODE_PRIVATE).edit().clear().commit() } }
    }
    private val context = Sandbox(ApplicationProvider.getApplicationContext())
    private val events = mutableListOf<String>()
    private val status = CloudBackupStatus(account = "fixture@example.test", accessState = CloudAccessState.CHECKED,
        accessRevision = 1, lastAccessCheckMs = 1234L)
    @After fun cleanup() { context.clear() }
    private fun show(busy: Boolean = false) {
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                    NightWaveAccountHubDialog(ProfileSettings("fixture", "Guest fixture"), status, busy,
                        phase = CloudOperationPhase.TRANSFERRING_BACKUP,
                        onDismiss = { events += "dismiss" }, onConnect = { events += "connect" },
                        onBackUp = { events += "backup" }, onPreviewRestore = { events += "preview" },
                        onCompare = { events += "compare" }, onDisconnect = { events += "disconnect" },
                        onExportLocal = { events += "export" }, onHistory = { events += "history:$it" },
                        onLyrics = {}, onPreferences = {}, onWifiOnly = {})
                }
            }
        }
    }
    private fun scroll(text: String) = compose.onNodeWithTag("nw139-hub-content").performScrollToNode(hasText(text))
    @Test fun primaryBackupClosesHubBeforeStartingExplicitFlow() {
        show(); scroll("Create first backup")
        compose.onNodeWithTag("nw139-primary-recovery").performClick()
        compose.runOnIdle { assertEquals(listOf("dismiss", "backup"), events) }
    }
    @Test fun busyPhaseIsTruthfulWhileInformationalHelpRemainsReachable() {
        show(busy = true)
        compose.onNodeWithTag("nw139-hub-content").performScrollToNode(hasTestTag("nw139-primary-recovery"))
        compose.onNodeWithTag("nw139-primary-recovery").assertIsNotEnabled()
        scroll("Connection help")
        compose.onNodeWithTag("nw139-connection-help").assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(events.isEmpty()) }
    }
    @Test fun optionalDataChoiceRequiresOpeningOptionsAndExplicitToggle() {
        show()
        compose.onNodeWithText("Listening history").assertDoesNotExist()
        scroll("Backup options")
        compose.onNodeWithTag("nw139-backup-options").performClick()
        scroll("Listening history")
        compose.onNodeWithText("Listening history").performClick()
        compose.runOnIdle { assertEquals(listOf("history:true"), events) }
    }
    @Test fun disconnectStillRequiresItsSeparateConfirmation() {
        show(); scroll("Disconnect Drive on this device")
        compose.onNodeWithText("Disconnect Drive on this device").performClick()
        compose.runOnIdle { assertTrue(events.isEmpty()) }
        compose.onNodeWithText("Disconnect", substring = false).performClick()
        compose.runOnIdle { assertEquals(listOf("disconnect"), events) }
    }
}
