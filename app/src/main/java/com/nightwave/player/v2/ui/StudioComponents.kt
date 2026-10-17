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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.nightwave.player.net.VideoPreview

@Composable
fun NightWaveWordmark(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Box(
            modifier = Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceVariant))
            ), contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(29.dp)) {
                val x = size.width
                val y = size.height
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x * .04f, y * .5f)
                    lineTo(x * .25f, y * .5f)
                    lineTo(x * .42f, y * .13f)
                    lineTo(x * .6f, y * .86f)
                    lineTo(x * .74f, y * .5f)
                    lineTo(x * .96f, y * .5f)
                }
                drawPath(p, Color.White, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Column {
            Text("nightwave.", style = MaterialTheme.typography.titleLarge)
            Text("YOUR SOUND. YOUR SPACE.", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeading(title: String, hint: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val highlight = MaterialTheme.colorScheme.primary
    Card(onClick = onClick, modifier = modifier.fillMaxWidth().height(222.dp),
        shape = RoundedCornerShape(28.dp), elevation = CardDefaults.cardElevation(8.dp)) {
        Box(Modifier.fillMaxWidth().height(222.dp).background(
            Brush.linearGradient(
                listOf(highlight.copy(alpha = .46f), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface),
                start = Offset(0f, 0f), end = Offset(1100f, 700f)
            )
        )) {
            Canvas(Modifier.align(Alignment.CenterEnd).size(200.dp)) {
                drawCircle(highlight.copy(alpha = .27f), radius = size.minDimension * .42f)
                drawCircle(Color.White.copy(alpha = .4f), radius = size.minDimension * .29f,
                    style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(Color.White.copy(alpha = .45f), radius = size.minDimension * .10f)
            }
            Column(Modifier.align(Alignment.CenterStart).padding(22.dp).fillMaxWidth(.74f),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(eyebrow, style = MaterialTheme.typography.labelSmall)
                Text(headline, style = MaterialTheme.typography.headlineLarge)
                Text(subhead, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("DISCOVER  ↗", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun MusicTrackTile(video: VideoPreview, modifier: Modifier = Modifier, onOptions: (() -> Unit)? = null, onClick: () -> Unit) {
    Row(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        AsyncImage(model = video.thumbnail, contentDescription = "Artwork for " + video.title,
            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(13.dp)), contentScale = ContentScale.Crop)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(video.title, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(video.author, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onOptions != null) {
            IconButton(onClick = onOptions, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Options for " + video.title,
                    tint = MaterialTheme.colorScheme.primary)
            }
        } else {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun EmptyMusicCard(title: String, description: String, action: String, onAction: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAction)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("◌", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(action + "  ↗", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
