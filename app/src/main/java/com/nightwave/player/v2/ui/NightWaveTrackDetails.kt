package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun NightWaveTrackDetails(title: String, creator: String, source: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Track details") },
        text = {
            SelectionContainer {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Original title", style = MaterialTheme.typography.labelMedium)
                    Text(title.ifBlank { "Untitled track" }, modifier = Modifier.testTag("editorial-original-title"))
                    Text("Artist / creator", style = MaterialTheme.typography.labelMedium)
                    Text(creator.ifBlank { "Unknown creator" })
                    Text("Source", style = MaterialTheme.typography.labelMedium)
                    Text(if (source.startsWith("offline:")) "On-device audio" else source,
                        modifier = Modifier.testTag("editorial-original-source"))
                }
            }
        }, confirmButton = { NightWaveMatteTextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") } })
}
