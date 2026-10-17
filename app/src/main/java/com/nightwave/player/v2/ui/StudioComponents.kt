package com.nightwave.player.v2.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nightwave.player.net.VideoPreview

@Composable
fun NightWaveWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val accent = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(accent, MaterialTheme.colorScheme.surfaceVariant))),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(25.dp)) {
                val x = size.width
                val y = size.height
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x * .04f, y * .52f)
                    lineTo(x * .25f, y * .52f)
                    lineTo(x * .40f, y * .16f)
                    lineTo(x * .61f, y * .87f)
                    lineTo(x * .77f, y * .51f)
                    lineTo(x * .96f, y * .51f)
                }
                drawPath(p, Color.White, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Text(
            "NightWave",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SectionHeading(title: String, hint: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground)
        if (!hint.isNullOrBlank()) Text(hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MusicFeatureCard(
    eyebrow: String,
    headline: String,
    subhead: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(164.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(164.dp).background(
                Brush.linearGradient(listOf(colors.surfaceVariant, colors.surface, colors.surfaceVariant),
                    start = Offset.Zero, end = Offset(980f, 640f))
            )
        ) {
            Canvas(Modifier.align(Alignment.CenterEnd).size(172.dp)) {
                drawCircle(colors.primary.copy(alpha = .20f), radius = size.minDimension * .43f)
                drawCircle(colors.primary.copy(alpha = .48f), radius = size.minDimension * .33f,
                    style = Stroke(width = 1.7.dp.toPx()))
                drawCircle(colors.primary.copy(alpha = .74f), radius = size.minDimension * .10f)
            }
            Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.82f).padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall,
                    color = colors.primary)
                Text(headline, style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2, color = colors.onSurface)
                Text(subhead, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2, color = colors.onSurfaceVariant)
                Text("Explore  ↗", style = MaterialTheme.typography.labelLarge,
                    color = colors.primary)
            }
        }
    }
}

@Composable
fun MusicTrackTile(
    video: VideoPreview,
    modifier: Modifier = Modifier,
    onOptions: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 5.dp, horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = video.thumbnail, contentDescription = "Artwork for " + video.title,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                .background(scheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(video.title, style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(video.author, style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onOptions != null) {
            IconButton(onClick = onOptions, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.MoreVert,
                    contentDescription = "Options for " + video.title,
                    tint = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EmptyMusicCard(title: String, description: String, action: String, onAction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAction),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .48f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(action + "  →", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
