package com.nightwave.player.v2.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nightwave.player.playback.EqualizerCapabilities
import com.nightwave.player.playback.EqualizerStatusStore
import com.nightwave.player.playback.PlaybackService
import com.nightwave.player.playback.SoundStudioPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * In-app Android equalizer controls, not a simulated DSP.
 * Each action is acknowledged by the Media3-owning playback service.
 * Capability and band limits are always read from the actual audio session.
 */
@Composable
internal fun NightWaveSoundStudioDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val capabilityStore = remember(context) { EqualizerCapabilities(context) }
    val statusStore = remember(context) { EqualizerStatusStore(context) }

    var capabilities by remember { mutableStateOf(capabilityStore.read()) }
    var status by remember { mutableStateOf(statusStore.read()) }
    var loading by remember { mutableStateOf(true) }
    var pendingCommand by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }
    var draftLevels by remember { mutableStateOf(emptyList<Int>()) }

    fun applySnapshot(resetDraft: Boolean) {
        val latest = capabilityStore.read()
        if (resetDraft || latest.audioSessionId != capabilities.audioSessionId ||
            latest.centersHz.size != capabilities.centersHz.size) {
            draftLevels = SoundStudioPolicy.flatLevels(
                latest.centersHz.size, latest.minimumMb, latest.maximumMb
            )
        }
        capabilities = latest
        status = statusStore.read()
    }

    fun discover() {
        if (pendingCommand != null || loading) return
        loading = true
        scope.launch {
            val sent = runCatching {
                context.startService(Intent(context, PlaybackService::class.java)
                    .setAction(PlaybackService.ACTION_EQ_DISCOVER))
            }.isSuccess
            if (!sent) notice = "Audio engine could not be reached. Try playing a track first."
            delay(400L)
            applySnapshot(resetDraft = false)
            loading = false
        }
    }

    fun issueCommand(action: String, configure: (Intent) -> Unit) {
        if (pendingCommand != null) return
        val requestId = UUID.randomUUID().toString()
        pendingCommand = requestId
        notice = ""
        val result = runCatching {
            context.startService(Intent(context, PlaybackService::class.java).apply {
                this.action = action
                putExtra(PlaybackService.EXTRA_EQ_COMMAND_ID, requestId)
                configure(this)
            })
        }
        if (result.isFailure) {
            pendingCommand = null
            notice = "Couldn't send the equalizer command to playback."
            return
        }
        scope.launch {
            repeat(14) {
                delay(120L)
                if (pendingCommand != requestId) return@launch
                val latest = statusStore.read()
                if (latest.commandId == requestId) {
                    applySnapshot(resetDraft = false)
                    pendingCommand = null
                    notice = latest.message
                    return@launch
                }
            }
            if (pendingCommand == requestId) {
                pendingCommand = null
                applySnapshot(resetDraft = false)
                notice = "No reply from the audio engine. Playback may have stopped."
            }
        }
    }

    LaunchedEffect(Unit) {
        val sent = runCatching {
            context.startService(Intent(context, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_EQ_DISCOVER))
        }.isSuccess
        if (!sent) notice = "Audio engine couldn't be reached."
        delay(400L)
        applySnapshot(resetDraft = true)
        loading = false
    }

    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val validManual = capabilities.sessionReady &&
        SoundStudioPolicy.validLevels(
            draftLevels, capabilities.centersHz.size,
            capabilities.minimumMb, capabilities.maximumMb)
    val busy = loading || pendingCommand != null

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(.94f).fillMaxHeight(.87f),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        start = 19.dp, end = 12.dp, top = 15.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NIGHTWAVE AUDIO", style = MaterialTheme.typography.labelSmall,
                            color = primary, fontWeight = FontWeight.SemiBold)
                        Text("Sound Studio", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = 17.dp, vertical = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    item {
                        Surface(shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(
                                Modifier.fillMaxWidth().background(
                                    Brush.horizontalGradient(listOf(
                                        primary.copy(alpha = .12f),
                                        MaterialTheme.colorScheme.surfaceVariant
                                    ))
                                ).padding(15.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Text(if (status.active) "EFFECT ACTIVE"
                                        else if (capabilities.sessionReady) "EFFECT OFF"
                                        else "WAITING FOR AUDIO",
                                        color = if (status.active) primary else muted,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.weight(1f))
                                    if (busy) CircularProgressIndicator(
                                        modifier = Modifier.size(17.dp),
                                        strokeWidth = 2.dp)
                                }
                                Text(status.message, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (capabilities.sessionReady)
                                        "${capabilities.centersHz.size} bands · " +
                                            "${capabilities.presetNames.size} device presets · " +
                                            "${capabilities.minimumMb / 100.0} to " +
                                            "${capabilities.maximumMb / 100.0} dB"
                                    else "Play a song, then refresh to inspect what Android supports.",
                                    color = muted, style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Frequency response",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Text("Preview", style = MaterialTheme.typography.labelSmall,
                                    color = muted)
                            }
                            Surface(shape = RoundedCornerShape(15.dp),
                                color = MaterialTheme.colorScheme.background) {
                                Canvas(Modifier.fillMaxWidth().height(105.dp).padding(12.dp)) {
                                    val minLevel = capabilities.minimumMb
                                    val maxLevel = capabilities.maximumMb
                                    val range = (maxLevel - minLevel).coerceAtLeast(1).toFloat()
                                    val baseline = (1f - ((0.coerceIn(minLevel,
                                        maxLevel) - minLevel) / range)) * size.height
                                    drawLine(
                                        color = muted.copy(alpha = .32f),
                                        start = androidx.compose.ui.geometry.Offset(0f, baseline),
                                        end = androidx.compose.ui.geometry.Offset(size.width, baseline),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    if (draftLevels.isNotEmpty()) {
                                        val path = Path()
                                        draftLevels.forEachIndexed { i, level ->
                                            val x = size.width * (i + .5f) / draftLevels.size
                                            val y = (1f - ((level - minLevel) / range)) * size.height
                                            if (i == 0) path.moveTo(x, y)
                                            else path.lineTo(x, y)
                                        }
                                        drawPath(path, color = primary,
                                            style = Stroke(width = 3.dp.toPx()))
                                    }
                                }
                            }
                            Text("Visual guide for the manual bands; not a live audio spectrum.",
                                style = MaterialTheme.typography.labelSmall, color = muted)
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Device presets",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                TextButton(enabled = !busy, onClick = {
                                    issueCommand(PlaybackService.ACTION_EQ_PRESET_SET) {
                                        it.putExtra(PlaybackService.EXTRA_EQ_PRESET, 0)
                                    }
                                }) { Text("Turn off") }
                            }
                            if (capabilities.presetNames.isEmpty()) {
                                Text("No compatible presets reported. You can still test " +
                                    "manual bands if the active session supports them.",
                                    style = MaterialTheme.typography.bodySmall, color = muted)
                            } else {
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    capabilities.presetNames.forEachIndexed { index, preset ->
                                        FilterChip(
                                            selected = status.active &&
                                                status.selected == index + 1,
                                            enabled = !busy && capabilities.sessionReady &&
                                                SoundStudioPolicy.validPreset(
                                                    index + 1, capabilities.presetNames.size),
                                            onClick = {
                                                issueCommand(PlaybackService.ACTION_EQ_PRESET_SET) {
                                                    it.putExtra(PlaybackService.EXTRA_EQ_PRESET, index + 1)
                                                }
                                            },
                                            label = { Text(preset) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Manual bands", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            TextButton(enabled = !busy && capabilities.sessionReady,
                                onClick = {
                                    draftLevels = SoundStudioPolicy.flatLevels(
                                        capabilities.centersHz.size,
                                        capabilities.minimumMb, capabilities.maximumMb)
                                }) { Text("Reset flat") }
                        }
                    }

                    if (!capabilities.sessionReady || capabilities.centersHz.isEmpty()) {
                        item {
                            Text(
                                "Manual control is unavailable until an Android playback " +
                                    "session reports supported frequency bands.",
                                style = MaterialTheme.typography.bodyMedium, color = muted)
                        }
                    } else {
                        items(capabilities.centersHz.size, key = { it }) { index ->
                            if (index < draftLevels.size) {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(SoundStudioPolicy.frequencyLabel(
                                            capabilities.centersHz[index]),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.weight(1f))
                                        Text(SoundStudioPolicy.gainLabel(draftLevels[index]),
                                            color = primary,
                                            style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Slider(
                                        value = draftLevels[index].toFloat(),
                                        enabled = !busy,
                                        onValueChange = { value ->
                                            draftLevels = SoundStudioPolicy.updateLevel(
                                                draftLevels, index, value.toInt(),
                                                capabilities.minimumMb,
                                                capabilities.maximumMb)
                                        },
                                        valueRange = capabilities.minimumMb.toFloat()..
                                            capabilities.maximumMb.toFloat()
                                    )
                                }
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    if (validManual) {
                                        val requested = draftLevels.toIntArray()
                                        issueCommand(PlaybackService.ACTION_EQ_BANDS_APPLY) {
                                            it.putExtra(PlaybackService.EXTRA_EQ_LEVELS_MB,
                                                requested)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy && validManual
                            ) { Text("Apply manual EQ") }
                        }
                    }

                    item {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(15.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Playback compatibility",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Effects depend on Android's audio session and the " +
                                        "selected speaker or headphones. Enabled does not " +
                                        "guarantee an audible change on every output route.",
                                    style = MaterialTheme.typography.bodySmall, color = muted)
                                if (notice.isNotBlank()) {
                                    Text(notice, style = MaterialTheme.typography.bodySmall,
                                        color = primary)
                                }
                                OutlinedButton(enabled = !busy, onClick = { discover() }) {
                                    Text("Refresh audio device")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
