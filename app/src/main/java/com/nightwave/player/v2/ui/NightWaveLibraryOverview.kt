package com.nightwave.player.v2.ui

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

import com.nightwave.player.R

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
    val strings = rememberNightWaveStrings()
    Column(Modifier.fillMaxWidth().testTag("composition-library-header").padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(strings.get(R.string.nw141_your_library_1ad84), modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
        Text(summary, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Honest guidance when no songs have been deliberately saved. History is not saved membership. */
@Composable
internal fun NightWaveLibraryStartCard(hasHistory: Boolean, onFindMusic: () -> Unit) {
    val strings = rememberNightWaveStrings()
    Surface(Modifier.fillMaxWidth().testTag("composition-library-start"),
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Keep your favourites", style = MaterialTheme.typography.titleSmall)
            Text(if (hasHistory) "Like or playlist a song to save it beyond history."
                else "Find a song to save, or import your own in Offline music.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            NightWaveMatteTextButton(onClick = onFindMusic,
                modifier = Modifier.heightIn(min = 48.dp).testTag("composition-library-find"),
                contentPadding = PaddingValues(horizontal = 0.dp)) { Text(strings.get(R.string.nw141_find_music_0a757)) }
        }
    }
}
