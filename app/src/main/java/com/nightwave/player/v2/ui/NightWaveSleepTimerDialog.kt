package com.nightwave.player.v2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.nightwave.player.v2.ui.NightWaveMatteButton as Button
import androidx.compose.material3.MaterialTheme
import com.nightwave.player.v2.ui.NightWaveMatteOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.nightwave.player.v2.ui.NightWaveMatteTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nightwave.player.playback.SleepTimerPolicy
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * A duration picker, not an alarm clock.
 * 1–120 minutes can be selected on the circular dial; 1–1440 via exact input.
 * The service remains the sole owner of the monotonic timer and cancellation.
 */
@Composable
fun NightWaveSleepTimerDialog(
    remainingMs: Long,
    onDismiss: () -> Unit,
    onSet: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val initial = if (remainingMs > 0L) {
        (((remainingMs - 1L) / 60_000L) + 1L)
            .coerceIn(SleepTimerPolicy.MIN_MINUTES.toLong(), SleepTimerPolicy.MAX_MINUTES.toLong())
            .toInt()
    } else 15
    var input by remember { mutableStateOf(initial.toString()) }
    val minutes = input.toIntOrNull()
    val valid = minutes != null && SleepTimerPolicy.accepts(minutes)
    val dialValue = (minutes ?: 15).coerceIn(
        SleepTimerPolicy.MIN_MINUTES, SleepTimerPolicy.DIAL_MAX_MINUTES)
    val colors = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
            shape = RoundedCornerShape(28.dp),
            color = colors.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("SLEEP TIMER", color = colors.primary,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.8.sp)
                Text("Set a duration", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    "Choose an exact duration. NightWave will pause music when the timer ends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Box(modifier = Modifier.size(208.dp), contentAlignment = Alignment.Center) {
                    Canvas(
                        modifier = Modifier.size(208.dp).testTag("sleep-timer-dial")
                            .semantics {
                                contentDescription = "Sleep timer duration, 1 to 120 minutes"
                                progressBarRangeInfo = ProgressBarRangeInfo(
                                    dialValue.toFloat(), 1f..120f, 118)
                                setProgress { requested ->
                                    if (!requested.isFinite()) false else {
                                        input = requested.roundToInt().coerceIn(1, 120).toString()
                                        true
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { point ->
                                    input = SleepTimerPolicy.dialMinutes(point.x, point.y,
                                        size.width.toFloat(), size.height.toFloat()).toString()
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { point ->
                                        input = SleepTimerPolicy.dialMinutes(point.x, point.y,
                                            size.width.toFloat(), size.height.toFloat()).toString()
                                    },
                                    onDrag = { change, _ ->
                                        input = SleepTimerPolicy.dialMinutes(
                                            change.position.x, change.position.y,
                                            size.width.toFloat(), size.height.toFloat()).toString()
                                        change.consume()
                                    }
                                )
                            }
                    ) {
                        val stroke = 11.dp.toPx()
                        val radius = size.minDimension / 2f - stroke * 2f
                        drawCircle(color = colors.primary.copy(alpha = 0.14f),
                            radius = radius, style = Stroke(width = stroke))
                        val sweep = 360f * dialValue.toFloat() /
                            SleepTimerPolicy.DIAL_MAX_MINUTES.toFloat()
                        drawArc(color = colors.primary, startAngle = -90f, sweepAngle = sweep,
                            useCenter = false, topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                            style = Stroke(width = stroke, cap = StrokeCap.Round))
                        val radians = Math.toRadians((sweep - 90f).toDouble())
                        drawCircle(color = colors.primary, radius = 10.dp.toPx(),
                            center = Offset(
                                center.x + (cos(radians) * radius).toFloat(),
                                center.y + (sin(radians) * radius).toFloat()))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${minutes ?: "—"}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text("MINUTES", style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                    }
                }
                Text("Drag the dial for 1–120 min, or enter any exact minute below.",
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    SleepTimerPolicy.choicesMinutes.forEach { preset ->
                        OutlinedButton(
                            onClick = { input = preset.toString() },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                        ) {
                            Text("${preset}m", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { value ->
                        if (value.length <= 4 && value.all { it.isDigit() }) input = value
                    },
                    modifier = Modifier.fillMaxWidth().testTag("sleep-timer-exact-minutes"),
                    singleLine = true,
                    label = { Text("Exact minutes (1–1440)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !valid,
                    supportingText = {
                        Text(if (valid) "Up to 24 hours; no server required"
                            else "Enter a whole number between 1 and 1440")
                    }
                )
                if (remainingMs > 0L) {
                    Text("Currently active: " + SleepTimerPolicy.label(remainingMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant)
                }
                if (remainingMs > 0L) {
                    OutlinedButton(onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Cancel active timer")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Back") }
                    Button(onClick = { if (valid) onSet(requireNotNull(minutes)) },
                        enabled = valid,
                        modifier = Modifier.testTag("sleep-timer-confirm")) {
                        Text(if (remainingMs > 0L) "Replace timer" else "Start timer")
                    }
                }
            }
        }
    }
}
