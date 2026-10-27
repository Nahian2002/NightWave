package com.nightwave.player.v2.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nightwave.player.offline.NightWaveOfflineRoute
import com.nightwave.player.v2.playback.StudioPlayerActions
import com.nightwave.player.v2.playback.StudioPlayerState

/** Shared chrome around retained native offline content. No engine or file owner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NightWaveOfflineShell(
    route: NightWaveOfflineRoute, state: StudioPlayerState, actions: StudioPlayerActions,
    reducedMotion: Boolean, onBack: () -> Unit, onRoute: (NightWaveOfflineRoute) -> Unit,
    onMain: (Int, Boolean) -> Unit, onRecovery: () -> Unit,
    content: @Composable () -> Unit
) {
    var overflow by remember { mutableStateOf(false) }
    var transport by rememberSaveable { mutableStateOf(false) }
    var hiddenForTrack by rememberSaveable { mutableStateOf<String?>(null) }
    // A hidden bar belongs only to that track. Restoring it never resumes audio.
    val hidden = hiddenForTrack == state.videoUrl && state.hasTrack
    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding().testTag("nw128-offline-shell"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("nw128-back")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                    Text("Offline music", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { overflow = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Outlined.MoreVert, "Offline navigation options")
                        }
                        DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                            DropdownMenuItem(text = { Text("Settings") },
                                onClick = { overflow = false; onMain(3, false) })
                            DropdownMenuItem(text = { Text("Use native recovery view") },
                                onClick = { overflow = false; onRecovery() })
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NightWaveOfflineRoute.entries.forEach { target ->
                        FilterChip(selected = route == target, onClick = { if (route != target) onRoute(target) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("nw128-route-${target.name}"),
                            label = { Text(target.label) })
                    }
                }
            }
        },
        bottomBar = {
            Column {
                if (state.hasTrack && hidden) TextButton(onClick = { hiddenForTrack = null },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("nw128-show-player")) {
                    Text("Show now playing · Music keeps playing")
                } else if (state.hasTrack) StudioMiniPlayer(state, actions,
                    onExpand = { transport = true }, onHide = { hiddenForTrack = state.videoUrl })
                NightWavePremiumNavigation(selectedIndex = 2, reducedMotion = reducedMotion,
                    onNavigate = { onMain(it, false) }, onReselect = { onMain(2, false) })
            }
        }
    ) { insets -> Box(Modifier.fillMaxSize().padding(insets)) { content() } }
    if (transport && state.hasTrack) ModalBottomSheet(onDismissRequest = { transport = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.title.ifBlank { "Restoring selected track…" }, style = MaterialTheme.typography.titleLarge)
            Text(state.artist, style = MaterialTheme.typography.bodyMedium)
            if (!state.connected) Text("Music service disconnected", color = MaterialTheme.colorScheme.error)
            NightWaveActionGroup {
                TextButton(onClick = actions::previous, enabled = state.connected) { Text("Previous") }
                TextButton(onClick = actions::playPause, enabled = state.connected) {
                    Text(if (state.playing) "Pause" else "Play")
                }
                TextButton(onClick = actions::next, enabled = state.connected) { Text("Next") }
            }
            if (!state.connected) TextButton(onClick = actions::retry) { Text("Reconnect") }
            TextButton(onClick = { transport = false; onMain(2, true) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("nw128-full-player")) {
                Text("Open full player and queue")
            }
        }
    }
}
