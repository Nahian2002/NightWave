package com.nightwave.player.v2.ui

import android.content.Intent
import com.nightwave.player.offline.FreeAudioCatalogActivity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NightWaveDiscoveryHeader(searchQuery: String = "") {
    val palette = MaterialTheme.colorScheme
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("nightwave-discovery-header"),
        shape = RoundedCornerShape(22.dp),
        color = palette.surface
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(
                    listOf(palette.primary.copy(alpha = .17f),
                        palette.surfaceVariant, palette.surface)
                )
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(16.dp))
                        .background(palette.primary.copy(alpha = .17f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null,
                        tint = palette.primary, modifier = Modifier.size(24.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f)) {
                    NightWaveMicroLabel("Discover")
                    Text("Find your sound",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Text("Your library first · Explore more when you choose",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.onSurfaceVariant, maxLines = 2)
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(
                                context, FreeAudioCatalogActivity::class.java)
                                .putExtra(FreeAudioCatalogActivity.EXTRA_QUERY,
                                    searchQuery.trim().take(100)))
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("nightwave-save-offline-search")
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Find music to save offline", maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
fun NightWaveCollectionHeader(
    name: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier.fillMaxWidth()
        .testTag("nightwave-collection-header"),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant.copy(alpha = .60f)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                .background(colors.primary.copy(alpha = .16f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.LibraryMusic, contentDescription = null,
                    tint = colors.primary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Text(description, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant)
            }
        }
    }
}
