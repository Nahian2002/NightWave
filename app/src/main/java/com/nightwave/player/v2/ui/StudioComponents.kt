package com.nightwave.player.v2.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.net.MusicIdentity

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
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.65f).sp),
                maxLines = 1, color = MaterialTheme.colorScheme.onBackground)
            Text("Wave", style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.65f).sp),
                maxLines = 1, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SectionHeading(title: String, hint: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold),
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
        modifier = Modifier.fillMaxWidth().height(181.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(181.dp).background(
            Brush.linearGradient(listOf(colors.surfaceVariant, colors.surface, colors.surfaceVariant),
                start = Offset.Zero, end = Offset(900f, 400f))
        )) {
            // Row measurement, not overlapping Box alignments, owns the space.
            // Narrow screens shrink artwork before sacrificing the title area.
            val artworkSize = if (maxWidth < 340.dp) 96.dp else 124.dp
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(if (hasRecent) "PICK UP WHERE YOU LEFT OFF" else "YOUR NEXT LISTEN",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(if (hasRecent) lastTrack?.let {
                            MusicIdentity.display(it.title, it.author).title
                        }.orEmpty() else "Make tonight sound better",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Text(if (hasRecent) lastTrack?.let {
                            MusicIdentity.display(it.title, it.author).artistOrCreator
                        }.orEmpty() else "Discover something worth replaying",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.PlayArrow, null,
                            modifier = Modifier.size(19.dp), tint = colors.primary)
                        Text(if (hasRecent) "Play again" else "Discover music",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(Modifier.size(artworkSize).clip(RoundedCornerShape(20.dp))
                    .background(colors.primary.copy(alpha = .13f)),
                    contentAlignment = Alignment.Center) {
                    if (hasRecent && !lastTrack?.thumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = lastTrack?.thumbnail, contentDescription = "Last played artwork",
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Canvas(Modifier.fillMaxSize(.78f)) {
                            drawCircle(colors.primary.copy(alpha = .2f),
                                radius = size.minDimension * .43f)
                            drawCircle(colors.primary.copy(alpha = .63f),
                                radius = size.minDimension * .29f,
                                style = Stroke(width = 2.dp.toPx()))
                            drawCircle(colors.primary, radius = size.minDimension * .055f)
                        }
                    }
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
    onRemove: (() -> Unit)? = null,
    isPlaying: Boolean = false,
    surface: NightWaveTrackSurface = NightWaveTrackSurface.HOME,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val identity = MusicIdentity.display(video.title, video.author)
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val shape = RoundedCornerShape(if (surface == NightWaveTrackSurface.SEARCH) 14.dp else 18.dp)
    val background = if (isPlaying) scheme.primary.copy(alpha = .15f)
        else if (surface == NightWaveTrackSurface.SEARCH) scheme.surfaceVariant.copy(alpha = .56f)
        else scheme.surface
    Row(
        modifier = modifier.fillMaxWidth()
            .heightIn(min = NightWaveCollectionChrome.TRACK_MIN_HEIGHT_DP.dp).clip(shape)
            .background(background)
            .testTag("nightwave-track-" + surface.name.lowercase())
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TrackArtwork(video, Modifier.size(NightWaveCollectionChrome.TRACK_ARTWORK_DP.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(identity.title, style = MaterialTheme.typography.titleMedium,
                color = if (isPlaying) scheme.primary else scheme.onSurface,
                maxLines = if (largeText) 2 else 1, overflow = TextOverflow.Ellipsis)
            Text(if (isPlaying) "●  Playing · " + identity.artistOrCreator
                else identity.artistOrCreator,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Outlined.Close,
                    contentDescription = "Remove " + identity.title + " from Recently Played",
                    modifier = Modifier.size(19.dp), tint = scheme.onSurfaceVariant)
            }
        }
        if (onOptions != null) {
            IconButton(onClick = onOptions, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.MoreVert,
                    contentDescription = "Options for " + video.title,
                    tint = scheme.onSurfaceVariant)
            }
        }
    }
}

/** Compact collection entry using genuine stored song artwork and count. */
@Composable
fun MusicCollectionTile(
    name: String,
    subtitle: String,
    cover: VideoPreview?,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth()
            .heightIn(min = NightWaveCollectionChrome.COLLECTION_MIN_HEIGHT_DP.dp)
            .testTag("nightwave-library-collection"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (cover != null) {
                TrackArtwork(cover, Modifier.size(NightWaveCollectionChrome.COLLECTION_ARTWORK_DP.dp))
            } else {
                Box(Modifier.size(NightWaveCollectionChrome.COLLECTION_ARTWORK_DP.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null,
                        tint = colors.primary, modifier = Modifier.size(29.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = colors.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.titleLarge,
                color = colors.primary)
        }
    }
}

@Composable
fun EmptyMusicCard(title: String, description: String, action: String, onAction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAction),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .26f))
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
