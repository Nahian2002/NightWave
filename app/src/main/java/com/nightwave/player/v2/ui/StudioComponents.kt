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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
        horizontalArrangement = Arrangement.spacedBy(9.dp)
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
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Night", style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.65).sp),
                maxLines = 1, color = MaterialTheme.colorScheme.onBackground)
            Text("Wave", style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.65).sp),
                maxLines = 1, color = MaterialTheme.colorScheme.primary)
        }
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

/** Uses real last-listened metadata. This surface never invents a recommendation. */
@Composable
fun ListeningSpotlight(
    lastTrack: VideoPreview?,
    onResume: () -> Unit,
    onDiscover: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val hasRecent = lastTrack != null
    Card(
        onClick = if (hasRecent) onResume else onDiscover,
        modifier = Modifier.fillMaxWidth().height(174.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
    ) {
        Box(Modifier.fillMaxWidth().height(174.dp).background(
            Brush.linearGradient(listOf(colors.surfaceVariant, colors.surface, colors.surfaceVariant),
                start = Offset.Zero, end = Offset(900f, 400f))
        )) {
            Box(Modifier.align(Alignment.CenterEnd).size(145.dp)
                .offset(x = 16.dp).clip(RoundedCornerShape(22.dp))
                .background(colors.primary.copy(alpha = .13f)),
                contentAlignment = Alignment.Center) {
                if (hasRecent && !lastTrack?.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = lastTrack?.thumbnail, contentDescription = "Last played artwork",
                        modifier = Modifier.size(137.dp).clip(RoundedCornerShape(19.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Canvas(Modifier.size(125.dp)) {
                        drawCircle(colors.primary.copy(alpha = .2f), radius = size.minDimension * .43f)
                        drawCircle(colors.primary.copy(alpha = .63f), radius = size.minDimension * .29f,
                            style = Stroke(width = 2.dp.toPx()))
                        drawCircle(colors.primary, radius = size.minDimension * .055f)
                    }
                }
            }
            Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.71f)
                .padding(start = 20.dp, top = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (hasRecent) "PICK UP WHERE YOU LEFT OFF" else "YOUR NEXT LISTEN",
                    style = MaterialTheme.typography.labelSmall, color = colors.primary)
                Text(if (hasRecent) lastTrack?.title.orEmpty() else "Make tonight sound better",
                    style = MaterialTheme.typography.headlineSmall, color = colors.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (hasRecent) lastTrack?.author.orEmpty() else "Discover something worth replaying",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(19.dp),
                        tint = colors.primary)
                    Text(if (hasRecent) "Play again" else "Discover music",
                        style = MaterialTheme.typography.labelLarge, color = colors.primary)
                }
            }
        }
    }
}

/** Artwork placeholder remains visible if upstream thumbnails cannot load. */
@Composable
private fun TrackArtwork(video: VideoPreview, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(11.dp))
        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface))),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(28.dp)) {
            drawCircle(Color.White.copy(alpha = .12f), radius = size.minDimension * .47f)
            drawCircle(Color.White.copy(alpha = .5f), radius = size.minDimension * .29f,
                style = Stroke(width = 2.dp.toPx()))
            drawCircle(Color.White.copy(alpha = .7f), radius = size.minDimension * .06f)
        }
        if (video.thumbnail.isNotBlank()) {
            AsyncImage(model = video.thumbnail,
                contentDescription = "Artwork for " + video.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop)
        }
    }
}

@Composable
fun MusicTrackTile(
    video: VideoPreview,
    modifier: Modifier = Modifier,
    onOptions: (() -> Unit)? = null,
    isPlaying: Boolean = false,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) scheme.primary.copy(alpha = .10f) else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 7.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TrackArtwork(video, Modifier.size(51.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(video.title, style = MaterialTheme.typography.titleMedium,
                color = if (isPlaying) scheme.primary else scheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (isPlaying) "●  Playing · " + video.author else video.author,
                style = MaterialTheme.typography.bodyMedium,
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
