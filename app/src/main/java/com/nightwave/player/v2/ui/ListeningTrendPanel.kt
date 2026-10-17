package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.insights.ListeningMemorySummary
import com.nightwave.player.v2.insights.ListeningTrendEngine

/** Compact, completely local and screen-reader-friendly UTC trend visualization. */
@Composable
fun ListeningTrendPanel(memory: ListeningMemorySummary, todayUtc: Long) {
    var window by rememberSaveable { mutableIntStateOf(7) }
    val colors = MaterialTheme.colorScheme
    val points = ListeningTrendEngine.rolling(memory.daily, todayUtc, window)
    val display = if (window == 30) ListeningTrendEngine.groupedByWeek(points) else points
    val maximum = display.maxOfOrNull { it.observedMs } ?: 0L
    val total = points.sumOf { it.observedMs }
    val sessions = points.sumOf { it.qualifiedSessions }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Listening trends", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f))
            FilterChip(selected = window == 7, onClick = { window = 7 },
                label = { Text("7 days") })
            FilterChip(selected = window == 30, onClick = { window = 30 },
                label = { Text("30 days") })
        }
        Text("${total / 60_000L} min observed · $sessions qualified sessions",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant)
        if (maximum == 0L) {
            Text("No listening time recorded for this period.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
        } else {
            display.forEach { point ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(point.labelUtc(), modifier = Modifier.weight(1.65f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { if (maximum > 0L)
                            (point.observedMs.toDouble() / maximum.toDouble())
                                .toFloat().coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.weight(2f)
                    )
                    Text("${point.observedMs / 60_000L}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurface,
                        modifier = Modifier.weight(.48f),
                        maxLines = 1)
                }
            }
        }
        Text("UTC dates · from opted-in recorded playback only. " +
            "The 30-day view groups days into short periods. Missed or unrecorded " +
            "playback may not appear; older daily buckets expire automatically.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant)
    }
}
