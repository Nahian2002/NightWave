package com.nightwave.player.v2.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nightwave.player.R
import com.nightwave.player.net.LocalTypingPreview
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.offline.OfflineSong

@Composable
internal fun NightWaveSearchTyping(
    preview: LocalTypingPreview,
    playingUrl: String,
    onCompletion: (String) -> Unit,
    onPlay: (VideoPreview) -> Unit,
    onOffline: (OfflineSong) -> Unit
) {
    val strings = rememberNightWaveStrings()
    val offline = preview.offline.firstOrNull()
    val saved = preview.saved.take(if (offline == null) 2 else 1)
    if (preview.completions.isEmpty() && offline == null && saved.isEmpty()) return
    Column(Modifier.fillMaxWidth().testTag("nw58-typing-matches"),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (preview.completions.isNotEmpty()) {
            Text(strings.get(R.string.nw58_typing_completions), style = MaterialTheme.typography.labelMedium)
            val scroll = remember(preview.query) { ScrollState(0) }
            Row(Modifier.fillMaxWidth().nightWaveShelfBoundary().horizontalScroll(scroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                preview.completions.forEach { text ->
                    FilterChip(selected = false, onClick = { onCompletion(text) },
                        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
            }
        }
        if (offline != null || saved.isNotEmpty()) {
            Text(strings.get(R.string.nw58_typing_matches), style = MaterialTheme.typography.labelMedium)
            if (offline != null) {
                OutlinedButton(onClick = { onOffline(offline) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(strings.get(R.string.nw58_typing_offline_title, offline.title),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            saved.forEach { song ->
                MusicTrackTile(song, surface = NightWaveTrackSurface.SEARCH,
                    isPlaying = playingUrl == song.url, onClick = { onPlay(song) })
            }
        }
    }
}
