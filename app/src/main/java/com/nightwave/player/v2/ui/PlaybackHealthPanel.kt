package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.nightwave.player.v2.ui.NightWaveMatteOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.playback.PlaybackHealthPolicy
import com.nightwave.player.v2.playback.StudioPlayerState

/** On-screen status, not telemetry: never reveals watch URLs, paths or tokens. */
@Composable
fun PlaybackHealthPanel(state: StudioPlayerState, onRecover: () -> Unit) {
    val health = PlaybackHealthPolicy.classify(
        hasTrack = state.hasTrack,
        connected = state.connected,
        offline = state.offline,
        playing = state.playing,
        buffering = state.buffering,
        resolving = state.resolving,
        error = state.error
    )
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(health.title, style = MaterialTheme.typography.titleMedium)
        Text(health.explanation, style = MaterialTheme.typography.bodySmall, color = muted)
        if (state.hasTrack) {
            Text(if (state.offline) "Source: imported local audio"
                else "Source: public video stream",
                style = MaterialTheme.typography.labelMedium, color = muted)
            Text("Queue entries: ${state.queue.size}",
                style = MaterialTheme.typography.labelMedium, color = muted)
        }
        if (health.needsUserAction) {
            OutlinedButton(onClick = onRecover, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.connected) "Retry selected track" else "Reconnect controls")
            }
        }
        Text("Status is based on current player events only. This panel " +
            "does not upload logs, reveal a song link or change playback automatically.",
            style = MaterialTheme.typography.labelSmall, color = muted)
    }
}
