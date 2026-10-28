package com.nightwave.player.v2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * Clean, slim seek timeline with an actual circular thumb instead of the
 * tall, detached-looking Material3 default slider marker.
 *
 * Drag previews do not send Media3 seek commands until finger release.
 * Tap and accessibility progress events commit once.
 */
@Composable
internal fun NightWaveSeekTimeline(
    trackIdentity: String,
    fraction: Float,
    enabled: Boolean,
    onPreview: (Float) -> Unit,
    onCommit: (String, Float) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    positionDescription: String? = null
) {
    val colors = MaterialTheme.colorScheme
    val dark = LocalNightWavePalette.current.isDark
    val shown = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f
    val previewCallback by rememberUpdatedState(onPreview)
    val commitCallback by rememberUpdatedState(onCommit)
    val cancelCallback by rememberUpdatedState(onCancel)
    var pressed by remember(trackIdentity) { androidx.compose.runtime.mutableStateOf(false) }
    var dragging by remember(trackIdentity) { androidx.compose.runtime.mutableStateOf(false) }
    var focused by remember { androidx.compose.runtime.mutableStateOf(false) }
    val context = LocalContext.current
    val accessibility = remember(context) {
        context.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
    }
    var touchExploration by remember(accessibility) {
        androidx.compose.runtime.mutableStateOf(accessibility?.isTouchExplorationEnabled == true)
    }
    androidx.compose.runtime.DisposableEffect(accessibility) {
        val listener = android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener {
            touchExploration = it
        }
        accessibility?.addTouchExplorationStateChangeListener(listener)
        onDispose { accessibility?.removeTouchExplorationStateChangeListener(listener) }
    }
    var dragged by remember(trackIdentity) { mutableFloatStateOf(shown) }
    val tapDragModifier = if (!enabled) Modifier else Modifier
        .pointerInput(enabled, trackIdentity) {
            val edge = 8.dp.toPx()
            detectTapGestures(onPress = {
                pressed = true
                try { tryAwaitRelease() } finally { pressed = false }
            }) { pos ->
                NightWaveSeekGesturePolicy.touchFractionOrNull(
                    pos.x, size.width.toFloat(), edge
                )?.let { commitCallback(trackIdentity, it) }
            }
        }
        .pointerInput(enabled, trackIdentity) {
            val edge = 8.dp.toPx()
            // Pointer-input state is scoped to this trackIdentity. No drag
            // from an old song or unusably narrow timeline may seek another.
            var validDrag = false
            detectDragGestures(
                onDragStart = { pos ->
                    dragging = true
                    val mapped = NightWaveSeekGesturePolicy.touchFractionOrNull(
                        pos.x, size.width.toFloat(), edge)
                    validDrag = mapped != null
                    if (mapped != null) {
                        dragged = mapped
                        previewCallback(mapped)
                    }
                },
                onDragEnd = {
                    dragging = false
                    if (validDrag) commitCallback(trackIdentity, dragged)
                    else cancelCallback()
                    validDrag = false
                },
                onDragCancel = {
                    dragging = false
                    validDrag = false
                    cancelCallback()
                }
            ) { change, _ ->
                change.consume()
                val mapped = NightWaveSeekGesturePolicy.touchFractionOrNull(
                    change.position.x, size.width.toFloat(), edge)
                if (mapped != null && validDrag) {
                    dragged = mapped
                    previewCallback(mapped)
                } else {
                    validDrag = false
                }
            }
        }

    Canvas(
        // Keep a slim painted rail but offer 48dp for touch exploration.
        modifier.fillMaxWidth().height(48.dp)
            .testTag("nightwave-player-seek")
            .semantics {
                contentDescription = "Seek playback"
                positionDescription?.let { stateDescription = it }
                if (!enabled) disabled()
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                if (enabled) setProgress { requested ->
                    val mapped = NightWaveSeekGesturePolicy
                        .accessibilityFractionOrNull(requested)
                    if (mapped != null) {
                        commitCallback(trackIdentity, mapped)
                        true
                    } else false
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (!enabled || event.type != KeyEventType.KeyUp) false else {
                    val requested = when (event.key) {
                        Key.DirectionLeft -> shown - .05f
                        Key.DirectionRight -> shown + .05f
                        Key.MoveHome -> 0f
                        Key.MoveEnd -> 1f
                        else -> null
                    }
                    if (requested != null) {
                        commitCallback(trackIdentity, requested.coerceIn(0f, 1f)); true
                    } else false
                }
            }
            .focusable(enabled)
            .then(tapDragModifier)
    ) {
        val edge = 8.dp.toPx()
        val startX = NightWaveSeekGesturePolicy.thumbX(0f, size.width, edge)
        val fullX = NightWaveSeekGesturePolicy.thumbX(1f, size.width, edge)
        val cy = size.height / 2f
        val endX = NightWaveSeekGesturePolicy.thumbX(shown, size.width, edge)
        val baseline = if (dark) paletteColor(0xFF2C2C2E) else colors.outline
        drawLine(
            baseline, Offset(startX, cy), Offset(fullX, cy),
            strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
        )
        drawLine(
            if (enabled) colors.primary else colors.onSurfaceVariant,
            Offset(startX, cy), Offset(endX, cy),
            strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
        )
        if (enabled && NightWaveMattePolicy.showSeekThumb(pressed || dragging, focused, touchExploration)) drawCircle(
            color = colors.primary,
            radius = 6.dp.toPx(), center = Offset(endX, cy)
        )
    }
}
