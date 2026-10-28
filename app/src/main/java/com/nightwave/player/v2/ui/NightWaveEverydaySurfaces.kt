package com.nightwave.player.v2.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.nightwave.player.v2.ui.NightWaveMatteIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.net.VideoPreview

/** NW47: tappable destinations, not decorative chips; all touch targets >= 48dp. */
@Composable
fun NightWaveQuickActionPill(
    label: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag),
        color = colors.surface,
        shape = CircleShape,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = .25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
                tint = colors.primary)
            Text(label, style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface)
        }
    }
}

/**
 * NW47 recently-played rail: a real user-played song with separate play,
 * options and remove actions. Removing history never deletes a saved track.
 * Coil and the existing thumbnail fallback are retained; no fake cover art.
 */
@Composable
fun NightWaveRecentShelfTile(
    song: VideoPreview,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onOptions: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    val identity = remember(song.title, song.author) { MusicIdentity.display(song.title, song.author) }
    var showActions by remember(song.url) { mutableStateOf(false) }
    val tileWidth = NightWavePremiumDesignTokens.shelfWidthDp(
        LocalConfiguration.current.screenWidthDp.toFloat(), fontScale).dp
    Column(Modifier.width(tileWidth).testTag("nw47-recent-tile")) {
        Surface(onClick = onPlay, shape = RoundedCornerShape(NightWavePremiumDesignTokens.CARD_CORNER_DP.dp),
            modifier = Modifier.fillMaxWidth().testTag("nw47-recent-play")) {
            Box {
                TrackArtwork(song, Modifier.fillMaxWidth().aspectRatio(1f), large = true)
                if (isPlaying) Surface(
                    Modifier.align(Alignment.BottomStart).padding(7.dp),
                    color = colors.primary, shape = RoundedCornerShape(7.dp)) {
                    Text("PLAYING", Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        color = colors.onPrimary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        // Metadata shares a row with overflow; Play and overflow have disjoint
        // measured bounds. No persistent removal row under every cover.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 8.dp, end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(identity.title,
                    maxLines = maxOf(2, NightWavePremiumDesignTokens.titleLines(fontScale, tileWidth.value)),
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface)
                Text(identity.artistOrCreator,
                    maxLines = NightWavePremiumDesignTokens.supportingLines(fontScale),
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { showActions = true }, modifier = Modifier.size(48.dp)
                    .testTag("nw47-recent-options")) {
                    Icon(Icons.Outlined.MoreVert,
                        contentDescription = "Actions for " + identity.title, tint = colors.onSurface)
                }
                DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                    DropdownMenuItem(text = { Text("Track options") },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("nw127-recent-details"),
                        onClick = { showActions = false; onOptions() })
                    DropdownMenuItem(text = { Text("Remove from Recently Played") },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("nw47-recent-remove"),
                        onClick = { showActions = false; onRemove() })
                }
            }
        }
    }
}



/**
 * NW84: a compact shelf of real liked/playlist-only videos which are
 * absent from Recently Played. This does not create "recommended songs" or
 * invent cover art: the URL and thumbnail are the saved source values.
 */
@Composable
fun NightWaveNextListenTile(
    song: VideoPreview,
    provenance: String,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onOptions: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val identity = remember(song.title, song.author) {
        MusicIdentity.display(song.title, song.author)
    }
    Column(
        modifier = Modifier.width(NightWavePremiumDesignTokens.shelfWidthDp(
            LocalConfiguration.current.screenWidthDp.toFloat(), LocalDensity.current.fontScale).dp)
            .testTag("nw84-next-listen-tile"),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Surface(
            onClick = onPlay,
            shape = RoundedCornerShape(NightWavePremiumDesignTokens.CARD_CORNER_DP.dp),
            color = if (isPlaying) scheme.primary.copy(alpha = .12f)
                else scheme.surface,
            border = BorderStroke(1.dp,
                if (isPlaying) scheme.primary.copy(alpha = .44f)
                else scheme.outline.copy(alpha = .22f)),
            modifier = Modifier.fillMaxWidth().testTag("nw84-next-listen-play")
        ) {
            Column {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f)
                        .clip(RoundedCornerShape(
                            NightWavePremiumDesignTokens.CARD_CORNER_DP.dp))
                        .background(scheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    TrackArtwork(song, Modifier.fillMaxSize(), large = true)
                    if (isPlaying) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                            color = scheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("PLAYING",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onPrimary)
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(identity.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Text(identity.artistOrCreator,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant, maxLines =
                            NightWavePremiumDesignTokens.supportingLines(LocalDensity.current.fontScale),
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Row(Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(provenance, style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 6.dp))
            IconButton(onClick = onOptions,
                modifier = Modifier.size(
                    NightWavePremiumDesignTokens.MIN_TOUCH_TARGET_DP.dp)
                    .testTag("nw84-next-listen-options")) {
                Icon(Icons.Outlined.MoreVert,
                    contentDescription = "Options for " + identity.title,
                    tint = scheme.onSurfaceVariant)
            }
        }
    }
}
