package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val minutes = memory.listenedMs / 60_000L
    Column(Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Save listening statistics", style = MaterialTheme.typography.titleSmall)
                Text(if (memory.enabled) "Recording locally on this device"
                    else "Off by default · no listening sessions recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant)
            }
            Switch(checked = memory.enabled, onCheckedChange = onEnabledChange)
        }
        Text("Only active playback time is counted. A session qualifies after 30 seconds " +
            "of actual playback. No microphones, online analytics or cloud transfers.",
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            for ((value, label) in listOf(
                "${minutes}m" to "Observed time",
                memory.qualifiedPlays.toString() to "30s+ sessions",
                memory.trackedSongs.toString() to "Tracked songs"
            )) {
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceVariant) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(value, style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface, maxLines = 1)
                        Text(label, style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant, maxLines = 2)
                    }
                }
            }
        }
        ListeningTrendPanel(
            memory = memory,
            todayUtc = ListeningTrendEngine.utcEpochDay(System.currentTimeMillis())
        )
        if (memory.tracks.isNotEmpty()) {
            Text("Most observed listening time", style = MaterialTheme.typography.titleSmall)
            memory.tracks.forEach { track ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                    }
                    Text("${track.listenedMs / 60_000L}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                Text("Refresh")
            }
            OutlinedButton(onClick = onErase, modifier = Modifier.weight(1f)) {
                Text("Erase & turn off")
            }
        }
        Text("This is observed play time, not a guaranteed full playback history. " +
            "Very short or interrupted intervals may not be recorded. Changing phones " +
            "does not transfer these statistics.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant)
    }
}
