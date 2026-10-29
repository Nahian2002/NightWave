package com.nightwave.player.v2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.insights.ListeningMemorySummary
import com.nightwave.player.v2.insights.ListeningTrendEngine

/** Readable, local UTC totals. Bars compare recorded time; they are not seek sliders. */
@Composable
fun ListeningTrendPanel(memory: ListeningMemorySummary, todayUtc: Long) {
    var window by rememberSaveable { mutableIntStateOf(7) }
    val colors = MaterialTheme.colorScheme
    val points = ListeningTrendEngine.rolling(memory.daily, todayUtc, window)
    val display = if (window == 30) ListeningTrendEngine.groupedByWeek(points) else points
    val maximum = display.maxOfOrNull { it.observedMs } ?: 0L
    val total = points.sumOf { it.observedMs }
    val sessions = points.sumOf { it.qualifiedSessions }

    Column(Modifier.fillMaxWidth().testTag("listening-trends"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Listening trends", style = MaterialTheme.typography.titleSmall)
        NightWaveCompactActions {
            FilterChip(selected = window == 7, onClick = { window = 7 },
                modifier = Modifier.heightIn(min = 48.dp).testTag("listening-window-7"),
                label = { Text("7 days") })
            FilterChip(selected = window == 30, onClick = { window = 30 },
                modifier = Modifier.heightIn(min = 48.dp).testTag("listening-window-30"),
                label = { Text("30 days") })
        }
        Text(NightWaveListeningPresentationPolicy.duration(total) + " recorded · " +
            NightWaveListeningPresentationPolicy.count(sessions, "session") + " (30s+)",
            style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        Text(NightWaveListeningPresentationPolicy.periodLabel(points),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        if (maximum == 0L) {
            Text("No listening time recorded for this period.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        } else {
            display.forEach { point ->
                val time = NightWaveListeningPresentationPolicy.duration(point.observedMs)
                val fraction = (point.observedMs.toDouble() / maximum.toDouble()).toFloat().coerceIn(0f, 1f)
                Column(Modifier.fillMaxWidth().testTag("listening-trend-row:${point.startDayUtc}")
                    .clearAndSetSemantics { contentDescription = "${point.labelUtc()} UTC: $time recorded" },
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(NightWaveListeningPresentationPolicy.dateLabel(point), modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        Text(time, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                    }
                    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                        val radius = CornerRadius(size.height / 2f)
                        drawRoundRect(colors.surfaceVariant, cornerRadius = radius)
                        if (fraction > 0f) drawRoundRect(colors.primary,
                            size = Size(size.width * fraction, size.height), cornerRadius = radius)
                    }
                }
            }
        }
        Text("Recorded while statistics are on. The 30-day view groups days into weeks.",
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}
