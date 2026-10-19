package com.nightwave.player.v2.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
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
    fraction: Float,
    enabled: Boolean,
    onPreview: (Float) -> Unit,
    onCommit: (Float) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shown = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f
    val previewCallback by rememberUpdatedState(onPreview)
    val commitCallback by rememberUpdatedState(onCommit)
    val cancelCallback by rememberUpdatedState(onCancel)
    var dragged by remember { mutableFloatStateOf(shown) }
    val tapDragModifier = if (!enabled) Modifier else Modifier
        .pointerInput(enabled) {
            val edge = 8.dp.toPx()
            detectTapGestures { pos ->
                val width = (size.width.toFloat() - edge * 2f).coerceAtLeast(1f)
                val chosen = ((pos.x - edge) / width).coerceIn(0f, 1f)
                commitCallback(chosen)
            }
        }
        .pointerInput(enabled) {
            val edge = 8.dp.toPx()
            val width = (size.width.toFloat() - edge * 2f).coerceAtLeast(1f)
            detectDragGestures(
                onDragStart = { pos ->
                    dragged = ((pos.x - edge) / width).coerceIn(0f, 1f)
                    previewCallback(dragged)
                },
                onDragEnd = { commitCallback(dragged) },
                onDragCancel = { cancelCallback() }
            ) { change, _ ->
                change.consume()
                dragged = ((change.position.x - edge) / width).coerceIn(0f, 1f)
                previewCallback(dragged)
            }
        }

    Canvas(
        modifier.fillMaxWidth().height(32.dp)
            .testTag("nightwave-player-seek")
            .semantics {
                contentDescription = "Seek playback"
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                if (enabled) setProgress { requested ->
                    commitCallback(requested.coerceIn(0f, 1f))
                    true
                }
            }
            .then(tapDragModifier)
    ) {
        val edge = 8.dp.toPx()
        val usable = (size.width - 2f * edge).coerceAtLeast(1f)
        val cy = size.height / 2f
        val endX = edge + usable * shown
        val baseline = colors.onSurfaceVariant.copy(alpha = .25f)
        drawLine(
            baseline, Offset(edge, cy), Offset(edge + usable, cy),
            strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
        )
        drawLine(
            if (enabled) colors.primary else colors.onSurfaceVariant,
            Offset(edge, cy), Offset(endX, cy),
            strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
        )
        drawCircle(
            color = if (enabled) colors.primary else colors.onSurfaceVariant,
            radius = 6.dp.toPx(), center = Offset(endX, cy)
        )
    }
}
