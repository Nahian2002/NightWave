package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nightwave.player.R

internal enum class SearchStopReason { CANCELLED, BACKGROUNDED }

/** A deliberate stop is a neutral recoverable state, separate from a provider error. */
@Composable
internal fun NightWaveSearchStoppedCard(reason: SearchStopReason, hasResults: Boolean, onWeb: () -> Unit, onRetry: () -> Unit) {
    val strings = rememberNightWaveStrings()
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)
        .testTag("nw150-search-stopped").semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(strings.get(if (reason == SearchStopReason.CANCELLED) R.string.nw150_search_cancelled
            else R.string.nw150_search_paused), style = MaterialTheme.typography.titleSmall)
        Text(strings.get(if (hasResults) R.string.nw150_results_kept else R.string.nw150_search_stopped_hint),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NightWaveMatteOutlinedButton(onClick = onRetry,
            modifier = Modifier.heightIn(min = 48.dp).testTag("nw150-search-retry")) {
            Text(strings.get(R.string.nw141_retry_search_f468e))
        }
        NightWaveMatteTextButton(onClick = onWeb,
            modifier = Modifier.heightIn(min = 48.dp).testTag("nw150-search-web")) {
            Text(strings.get(R.string.nw141_open_youtube_search_027b5))
        }
    }
}
