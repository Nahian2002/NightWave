package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.insights.ListeningInsights

/** Honest recent-library snapshot, not total lifetime plays or listening time. */
@Composable
fun ListeningInsightsPanel(insights: ListeningInsights) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((number, title) in listOf(
                insights.uniqueRecentTracks to "Recent tracks",
                insights.distinctRecentCreators to "Recent artists",
                insights.likedTracks to "Liked songs"
            )) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceVariant
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(number.toString(), style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text(title, style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                    }
                }
            }
        }
        Text("${insights.playlists} saved playlists",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant)
        if (insights.creators.isNotEmpty()) {
            Text("Artists represented in recent music",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface)
            insights.creators.forEach { creator ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(creator.name, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface, maxLines = 1)
                    Text("${creator.distinctRecentTracks} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant)
                }
            }
        } else {
            Text("Play music to begin building your recent listening snapshot.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
        }
        Text("Based on the device's limited recent-song list. These are distinct tracks, " +
            "not play counts, rankings by listening time or all-time statistics.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant)
    }
}
