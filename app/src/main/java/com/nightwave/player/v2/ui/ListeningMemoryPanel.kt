package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.nightwave.player.v2.ui.NightWaveMatteOutlinedButton as OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.insights.ListeningMemorySummary
import com.nightwave.player.v2.insights.ListeningTrendEngine

/** Consent and erase controls never launch audio or network requests. */
@Composable
fun ListeningMemoryPanel(
    memory: ListeningMemorySummary,
    onEnabledChange: (Boolean) -> Unit,
    onErase: () -> Unit,
    onRefresh: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Save listening statistics", style = MaterialTheme.typography.titleSmall)
                Text(if (memory.enabled) "Saved only on this device"
                    else "Off · listening statistics are not being recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant)
            }
            Switch(checked = memory.enabled, onCheckedChange = onEnabledChange,
                modifier = Modifier.testTag("listening-memory-consent"))
        }
        Text("Active playback time is recorded. Sessions count after 30 seconds. " +
            "No microphone, online tracking or cloud transfer.",
            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        NightWaveStatisticsCards(listOf(
            NightWaveListeningPresentationPolicy.duration(memory.listenedMs) to "Recorded time",
            memory.qualifiedPlays.toString() to "Sessions (30s+)",
            memory.trackedSongs.toString() to "Different songs"
        ))
        ListeningTrendPanel(
            memory = memory,
            todayUtc = ListeningTrendEngine.utcEpochDay(System.currentTimeMillis())
        )
        if (memory.tracks.isNotEmpty()) {
            Text("Recorded listening by song", style = MaterialTheme.typography.titleSmall)
            memory.tracks.forEach { track ->
                val identity = NightWaveEditorialPresentationPolicy.identity(track.title, track.artist)
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(identity.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium)
                    Text(identity.artistOrCreator + " · " + NightWaveListeningPresentationPolicy.duration(track.listenedMs),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
        NightWaveCompactActions {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.heightIn(min = 48.dp)) { Text("Refresh") }
            OutlinedButton(onClick = onErase, modifier = Modifier.heightIn(min = 48.dp)) { Text("Erase & turn off") }
        }
        Text("Interruptions can leave gaps. Statistics stay on this phone and do not transfer to another device.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant)
    }
}
