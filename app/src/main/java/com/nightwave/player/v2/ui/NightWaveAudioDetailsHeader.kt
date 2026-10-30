package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nightwave.player.R

/** A single independent action; labels reflow without shrinking system text. */
@Composable
internal fun NightWaveAudioDetailsHeader(expanded: Boolean, onToggle: () -> Unit) {
    val strings = rememberNightWaveStrings()
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stack = NightWaveAdaptiveViewportPolicy.stackDetail(maxWidth.value, LocalDensity.current.fontScale)
        Surface(onClick = onToggle,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("nw142-audio-details"),
            shape = RoundedCornerShape(15.dp), color = colors.surfaceVariant.copy(alpha = .58f)) {
            val labels: @Composable () -> Unit = {
                Text(strings.get(R.string.nw141_sound_playback_06fb2), style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface)
                Text(strings.get(R.string.nw141_crossfade_sleep_timer_listening_level_73204),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            val action: @Composable () -> Unit = {
                Text(strings.get(if (expanded) R.string.nw141_less_ae523 else R.string.nw141_more_d47d7),
                    style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
            if (stack) Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                labels(); action()
            } else Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) { labels() }
                action()
            }
        }
    }
}
