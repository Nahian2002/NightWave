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
import com.nightwave.player.v2.ui.NightWaveMatteButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.nightwave.player.v2.ui.NightWaveMatteOutlinedButton as OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.nightwave.player.v2.ui.NightWaveMatteTextButton as TextButton
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nightwave.player.playback.AudioTransitionUiPolicy
import com.nightwave.player.playback.CrossfadePolicy
import com.nightwave.player.playback.CrossfadeSettingsStore
import com.nightwave.player.playback.OfflineNormalizationStore
import com.nightwave.player.playback.EqualizerCapabilities
import com.nightwave.player.playback.EqualizerCommandPolicy
import com.nightwave.player.playback.EqualizerStatusStore
import com.nightwave.player.playback.PlaybackService
import com.nightwave.player.playback.SoundStudioPolicy
import com.nightwave.player.playback.SoundStudioTuningPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.util.UUID

/**
 * In-app Android equalizer controls, not a simulated DSP.
 * Each action is acknowledged by the Media3-owning playback service.
 * Capability and band limits are always read from the actual audio session.
 */
// This component intentionally uses the pinned Media3 unstable API.
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun NightWaveSoundStudioDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val screenLifecycle = LocalView.current.findViewTreeLifecycleOwner()?.lifecycle
    val scope = rememberCoroutineScope()
    val capabilityStore = remember(context) { EqualizerCapabilities(context) }
    val statusStore = remember(context) { EqualizerStatusStore(context) }
    val crossfadeStore = remember(context) { CrossfadeSettingsStore(context) }
    val normalizationStore = remember(context) { OfflineNormalizationStore(context) }

    var capabilities by remember { mutableStateOf(capabilityStore.read()) }
    var status by remember { mutableStateOf(statusStore.read()) }
    var loading by remember { mutableStateOf(true) }
    var liveConfirmed by remember { mutableStateOf(false) }
    var pendingCommand by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }
    var draftLevels by remember { mutableStateOf(emptyList<Int>()) }
    var selectedShape by remember { mutableStateOf<SoundStudioTuningPolicy.Shape?>(null) }
    var selectedCrossfade by remember { mutableStateOf(crossfadeStore.read()) }
    var balanceEnabled by remember { mutableStateOf(normalizationStore.enabled()) }
    var transitionBusy by remember { mutableStateOf(false) }
    var transitionNotice by remember { mutableStateOf("") }

    fun applySnapshot(resetDraft: Boolean) {
        val latest = capabilityStore.read()
        val identityChanged = EqualizerCommandPolicy.shouldResetDraft(
            capabilities.commandToken, latest.commandToken)
        if (resetDraft || identityChanged) {
            val allowed = SoundStudioPolicy.safeManualRange(
                latest.minimumMb, latest.maximumMb)
            draftLevels = if (allowed == null) emptyList()
                else if (SoundStudioPolicy.validLevels(
                    latest.levelsMb, latest.centersHz.size,
                    allowed.first, allowed.last
                )) latest.levelsMb else SoundStudioPolicy.flatLevels(
                    latest.centersHz.size, allowed.first, allowed.last
                )
            selectedShape = null
        }
        val latestStatus = statusStore.read()
        if (pendingCommand == null && latestStatus != status) notice = ""
        if (capabilities.sessionReady && identityChanged && !resetDraft)
            notice = "Audio output changed. Review the refreshed EQ before applying."
        capabilities = latest
        status = latestStatus
    }

    suspend fun refreshFromEngine(resetDraft: Boolean) {
        val requestId = UUID.randomUUID().toString()
        val sent = runCatching {
            context.startService(Intent(context, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_EQ_DISCOVER)
                .putExtra(PlaybackService.EXTRA_EQ_COMMAND_ID, requestId))
        }.isSuccess
        if (!sent) {
            liveConfirmed = false
            notice = "Audio engine could not be reached. Try playing a track first."
            return
        }
        // Cached preferences are not proof that this service answered. Require
        // the exact discovery acknowledgement, including unavailable sessions.
        for (attempt in 0 until 10) {
            delay(200L)
            if (pendingCommand != null) return // Let the explicit Apply own its reply.
            if (statusStore.read().commandId == requestId) {
                applySnapshot(resetDraft)
                liveConfirmed = true
                return
            }
        }
        liveConfirmed = false
        notice = "No fresh reply from playback. Refresh before applying EQ."
    }

    fun discover() {
        if (pendingCommand != null || loading || transitionBusy) return
        loading = true
        scope.launch {
            refreshFromEngine(resetDraft = true)
            loading = false
        }
    }

    fun issueCommand(action: String, configure: (Intent) -> Unit) {
        if (pendingCommand != null || transitionBusy) return
        val requestId = UUID.randomUUID().toString()
        pendingCommand = requestId
        notice = ""
        val result = runCatching {
            context.startService(Intent(context, PlaybackService::class.java).apply {
                this.action = action
                putExtra(PlaybackService.EXTRA_EQ_COMMAND_ID, requestId)
                putExtra(PlaybackService.EXTRA_EQ_SESSION_TOKEN, capabilities.commandToken)
                configure(this)
            })
        }
        if (result.isFailure) {
            pendingCommand = null
            notice = "Couldn't send the equalizer command to playback."
            return
        }
        scope.launch {
            repeat(25) {
                delay(160L)
                if (pendingCommand != requestId) return@launch
                val latest = statusStore.read()
                if (latest.commandId == requestId) {
                    // Turning EQ Off releases the effect and invalidates the
                    // cached capabilities. Re-probe the still-playing session,
                    // so the presets remain available without reopening.
                    if (action == PlaybackService.ACTION_EQ_PRESET_SET &&
                        latest.selected == 0) {
                        runCatching {
                            context.startService(Intent(context, PlaybackService::class.java)
                                .setAction(PlaybackService.ACTION_EQ_DISCOVER))
                        }
                        delay(300L)
                    }
                    // Preset changes should show device-reported gains,
                    // whereas manual edits keep the just-applied draft.
                    applySnapshot(resetDraft = action == PlaybackService.ACTION_EQ_PRESET_SET ||
                        action == PlaybackService.ACTION_EQ_BANDS_APPLY)
                    pendingCommand = null
                    liveConfirmed = true
                    notice = latest.message
                    return@launch
                }
            }
            if (pendingCommand == requestId) {
                pendingCommand = null
                applySnapshot(resetDraft = false)
                liveConfirmed = false
                notice = "No reply from the audio engine. Playback may have stopped."
            }
        }
    }

    fun setCrossfadeDuration(seconds: Int) {
        if (transitionBusy || !CrossfadePolicy.accepts(seconds)) return
        transitionBusy = true
        transitionNotice = "Applying crossfade preference…"
        val sent = runCatching {
            context.startService(Intent(context, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_CROSSFADE_SET
                putExtra(PlaybackService.EXTRA_CROSSFADE_SECONDS, seconds)
            })
        }.isSuccess
        if (!sent) {
            transitionBusy = false
            transitionNotice = "Couldn't reach playback. Preference was not confirmed."
            return
        }
        scope.launch {
            var confirmed = false
            for (attempt in 0 until 12) {
                delay(150L)
                selectedCrossfade = crossfadeStore.read()
                if (selectedCrossfade == seconds) {
                    confirmed = true
                    break
                }
            }
            transitionBusy = false
            transitionNotice = if (confirmed)
                "Crossfade preference saved for eligible offline tracks."
            else "Playback did not confirm crossfade. Reopen Sound Studio to verify."
        }
    }

    fun setOfflineBalancing(enabled: Boolean) {
        if (transitionBusy) return
        transitionBusy = true
        transitionNotice = "Updating offline volume balancing…"
        val sent = runCatching {
            context.startService(Intent(context, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_NORMALIZATION_SET
                putExtra(PlaybackService.EXTRA_NORMALIZATION_ENABLED, enabled)
            })
        }.isSuccess
        if (!sent) {
            transitionBusy = false
            transitionNotice = "Couldn't reach playback. Preference was not confirmed."
            return
        }
        scope.launch {
            var confirmed = false
            for (attempt in 0 until 12) {
                delay(150L)
                balanceEnabled = normalizationStore.enabled()
                if (balanceEnabled == enabled) {
                    confirmed = true
                    break
                }
            }
            transitionBusy = false
            transitionNotice = if (confirmed)
                "Offline volume balancing preference saved."
            else "Playback did not confirm balancing. Check settings after playback starts."
        }
    }

    LaunchedEffect(Unit) {
        refreshFromEngine(resetDraft = true)
        loading = false
    }

    // Observe only while this screen is visible. Keep edits for an unchanged
    // device identity; a new frequency/range/preset table invalidates the draft.
    LaunchedEffect(statusStore, screenLifecycle) {
        val lifecycle = screenLifecycle ?: return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                delay(2_000L)
                if (!loading && pendingCommand == null && !transitionBusy) {
                    refreshFromEngine(resetDraft = false)
                }
            }
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val manualRange = SoundStudioPolicy.safeManualRange(
        capabilities.minimumMb, capabilities.maximumMb)
    val validManual = liveConfirmed && capabilities.sessionReady && manualRange != null &&
        manualRange.first < manualRange.last &&
        SoundStudioPolicy.validLevels(
            draftLevels, capabilities.centersHz.size,
            manualRange.first, manualRange.last)
    val busy = loading || pendingCommand != null || transitionBusy
    val draftEdited = capabilities.sessionReady &&
        SoundStudioTuningPolicy.hasChanged(draftLevels, capabilities.levelsMb)
    val verifiedActive = liveConfirmed && !loading && capabilities.sessionReady && status.active
    val tuningStatus = if (!liveConfirmed) "Refresh to check current EQ" else
        SoundStudioTuningPolicy.statusLabel(verifiedActive, status.selected, draftEdited)

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(.94f).fillMaxHeight(.87f),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
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
                                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(15.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Text(if (!liveConfirmed || !capabilities.sessionReady) "WAITING FOR AUDIO"
                                        else if (verifiedActive) "ANDROID SESSION EQ"
                                        else "EFFECT OFF",
                                        color = if (verifiedActive) primary else muted,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.weight(1f))
                                    if (busy) CircularProgressIndicator(
                                        modifier = Modifier.size(17.dp),
                                        strokeWidth = 2.dp)
                                }
                                Text(if (liveConfirmed) status.message else "Waiting for current playback status.",
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(tuningStatus, style = MaterialTheme.typography.labelMedium,
                                    color = if (draftEdited) primary else muted,
                                    fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (liveConfirmed && capabilities.sessionReady)
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

                    item(key = "nw88-local-audio-transitions") {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(NightWavePremiumDesignTokens.CARD_CORNER_DP.dp)) {
                            Column(Modifier.fillMaxWidth().padding(15.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Local playback quality",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text("Gapless is preferred when crossfade is off. " +
                                    "Crossfade uses verified local tracks only; " +
                                    "it pauses for Shuffle, Repeat One or non-1× speed. " +
                                    "Streaming sources are not reprocessed.",
                                    style = MaterialTheme.typography.bodySmall, color = muted)
                                Text("Crossfade · " +
                                    AudioTransitionUiPolicy.dialogDetail(selectedCrossfade),
                                    style = MaterialTheme.typography.titleSmall)
                                Row(Modifier.fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    listOf(0, 2, 4, 6, 8, 10).forEach { seconds ->
                                        FilterChip(
                                            selected = selectedCrossfade == seconds,
                                            enabled = !transitionBusy && !busy,
                                            onClick = { setCrossfadeDuration(seconds) },
                                            label = { Text(if (seconds == 0) "Off"
                                                else "${seconds}s") }
                                        )
                                    }
                                }
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Offline volume balancing",
                                            style = MaterialTheme.typography.titleSmall)
                                        Text("Optional 12-second PCM sample. " +
                                            "Attenuates louder local tracks only; " +
                                            "not ReplayGain or streaming normalization.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = muted)
                                    }
                                    Switch(
                                        checked = balanceEnabled,
                                        enabled = !transitionBusy && !busy,
                                        onCheckedChange = { setOfflineBalancing(it) }
                                    )
                                }
                                if (transitionNotice.isNotBlank())
                                    Text(transitionNotice,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = muted)
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
                                    // Persisted capability data is untrusted after
                                    // process recreation; never draw an invalid range.
                                    val minLevel = minOf(capabilities.minimumMb,
                                        capabilities.maximumMb)
                                    val maxLevel = maxOf(capabilities.minimumMb,
                                        capabilities.maximumMb)
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
                                            enabled = !busy && liveConfirmed && capabilities.sessionReady &&
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
                            TextButton(enabled = !busy && liveConfirmed && capabilities.sessionReady &&
                                manualRange != null, onClick = {
                                    manualRange?.let { range ->
                                        draftLevels = SoundStudioPolicy.flatLevels(
                                            capabilities.centersHz.size,
                                            range.first, range.last)
                                        selectedShape = SoundStudioTuningPolicy.Shape.FLAT
                                    }
                                }) { Text("Reset flat") }
                        }
                    }

                    item(key = "nw49-guided-eq") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Guided tuning",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text("Select a real device-band curve, then apply it to hear the change.",
                                style = MaterialTheme.typography.bodySmall, color = muted)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                SoundStudioTuningPolicy.Shape.entries.forEach { shape ->
                                    FilterChip(
                                        selected = selectedShape == shape,
                                        enabled = !busy && liveConfirmed && capabilities.sessionReady &&
                                            manualRange != null &&
                                            capabilities.centersHz.isNotEmpty(),
                                        onClick = {
                                            val updated = SoundStudioTuningPolicy.draft(
                                                shape, capabilities.centersHz,
                                                capabilities.minimumMb, capabilities.maximumMb)
                                            if (updated != null) {
                                                draftLevels = updated
                                                selectedShape = shape
                                            } else notice = "This audio device cannot use that tuning."
                                        },
                                        label = { Text(SoundStudioTuningPolicy.label(shape)) }
                                    )
                                }
                            }
                            selectedShape?.let { shape ->
                                Text(SoundStudioTuningPolicy.hint(shape),
                                    style = MaterialTheme.typography.labelSmall, color = muted)
                            }
                            if (draftEdited) Text("Preview only · Apply manual EQ to hear changes.",
                                style = MaterialTheme.typography.labelSmall, color = primary)
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
                                    if (manualRange != null &&
                                        manualRange.first < manualRange.last) {
                                        Slider(
                                            value = draftLevels[index].toFloat(),
                                            enabled = !busy,
                                            onValueChange = { value ->
                                                draftLevels = SoundStudioPolicy.updateLevel(
                                                    draftLevels, index, value.toInt(),
                                                    manualRange.first,
                                                    manualRange.last)
                                                selectedShape = null
                                            },
                                            valueRange = (
                                                manualRange.first.toFloat()..
                                                    manualRange.last.toFloat()
                                            )
                                        )
                                    } else {
                                        Text("This device exposes a fixed band level.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = muted)
                                    }
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
