package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.insights.ListeningInsights

/** Honest recent-library snapshot, not total lifetime plays or listening time. */
@Composable
fun ListeningInsightsPanel(insights: ListeningInsights) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NightWaveStatisticsCards(listOf(
            insights.uniqueRecentTracks.toString() to "Recent songs",
            insights.distinctRecentCreators.toString() to "Creators",
            insights.likedTracks.toString() to "Liked songs"
        ))
        Text(NightWaveListeningPresentationPolicy.count(insights.playlists, "saved playlist"),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant)
        if (insights.creators.isNotEmpty()) {
            Text("Creators in your recent music",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface)
            insights.creators.forEach { creator ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(creator.name, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface, maxLines = 1)
                    Text(NightWaveListeningPresentationPolicy.count(creator.distinctRecentTracks, "track"),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant)
                }
            }
        } else {
            Text("Play music to begin building your recent listening snapshot.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
        }
        Text("A snapshot of this device's recent list. Counts show different songs, " +
            "not how many times you played them.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant)
    }
}
