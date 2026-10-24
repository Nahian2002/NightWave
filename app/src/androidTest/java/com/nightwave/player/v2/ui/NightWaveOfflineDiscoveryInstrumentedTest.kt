package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Visual-route gate: no media download, account or network requests in test. */
@RunWith(AndroidJUnit4::class)
class NightWaveOfflineDiscoveryInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchHeaderShowsSaveOfflineEntryOnCompactPhone() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    NightWaveDiscoveryHeader(searchQuery = "piano music")
                }
            }
        }
        compose.onNodeWithTag("nightwave-discovery-header").assertExists()
        compose.onNodeWithTag("nightwave-save-offline-search").assertExists()
    }
}
