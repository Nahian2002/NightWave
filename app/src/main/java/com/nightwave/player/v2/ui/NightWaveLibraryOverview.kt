package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** A compact overview, distinct from the artwork-led header of an opened collection. */
@Composable
internal fun NightWaveLibraryOverviewHeader(summary: String) {
    Column(Modifier.fillMaxWidth().testTag("composition-library-header").padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Your library", style = MaterialTheme.typography.headlineSmall)
        Text(summary, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Honest guidance when no songs have been deliberately saved. History is not saved membership. */
@Composable
internal fun NightWaveLibraryStartCard(hasHistory: Boolean, onFindMusic: () -> Unit) {
    Surface(Modifier.fillMaxWidth().testTag("composition-library-start"),
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Make room for your favourites", style = MaterialTheme.typography.titleMedium)
            Text(if (hasHistory) "Like a song or add it to a playlist to keep it beyond your history."
                else "Find a song to like, or open Offline music to bring your own audio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            NightWaveMatteTextButton(onClick = onFindMusic,
                modifier = Modifier.heightIn(min = 48.dp).testTag("composition-library-find"),
                contentPadding = PaddingValues(horizontal = 0.dp)) { Text("Find music") }
        }
    }
}
