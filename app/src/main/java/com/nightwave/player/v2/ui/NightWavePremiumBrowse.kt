package com.nightwave.player.v2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Lightweight browse chrome only; music identity and artwork stay authentic. */
@Composable
fun NightWaveBrowseHeader(
    title: String,
    subtitle: String,
    search: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val key = if (search) "nightwave-search" else "nightwave-library"
    val accent = NightWaveVisualLanguage.accent(key)
    Surface(
        modifier = modifier.fillMaxWidth()
            .testTag(if (search) "nightwave-search-header" else "nightwave-library-header"),
        color = colors.surface,
        shape = RoundedCornerShape(23.dp)
    ) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(
                    accent.copy(alpha = .18f), colors.surfaceVariant, colors.surface
                )))
                .padding(horizontal = 17.dp, vertical = 14.dp)
        ) {
            Canvas(Modifier.align(Alignment.CenterEnd).size(82.dp)) {
                val center = Offset(size.width * .61f, size.height * .5f)
                drawCircle(
                    color = accent.copy(alpha = .24f),
                    radius = size.minDimension * .42f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(
                    color = accent.copy(alpha = .11f),
                    radius = size.minDimension * .29f,
                    center = center
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = .19f))
                        .border(1.dp, accent.copy(alpha = .20f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (search) Icons.Outlined.Search else Icons.Outlined.LibraryMusic,
                        contentDescription = null,
                        tint = colors.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (search) "EXPLORE" else "YOUR MUSIC",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        NightWaveBrowseChromePolicy.title(title, "NightWave"),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Makes the source of every search result section unambiguous. */
@Composable
fun NightWaveSearchSourceHeading(
    title: String,
    resultCount: Int,
    source: NightWaveBrowseChromePolicy.Source
) {
    val colors = MaterialTheme.colorScheme
    val label = NightWaveBrowseChromePolicy.sourceDetail(source)
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(width = 4.dp, height = 21.dp)
                .clip(CircleShape).background(colors.primary))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            Text(
                resultCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.testTag(if (source ==
                    NightWaveBrowseChromePolicy.Source.SAVED_LIBRARY)
                    "nightwave-saved-results-count" else "nightwave-public-results-count")
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}
