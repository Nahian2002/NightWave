package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Actual measured width and system type size decide whether all three metrics can sit side by side. */
@Composable
internal fun NightWaveStatisticsCards(values: List<Pair<String, String>>) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("listening-statistics-cards")) {
        val columns = NightWaveListeningPresentationPolicy.columns(maxWidth.value, LocalDensity.current.fontScale)
        if (columns == 1) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { (value, label) -> StatisticCard(value, label, Modifier.fillMaxWidth()) }
        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { (value, label) -> StatisticCard(value, label, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun StatisticCard(value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
