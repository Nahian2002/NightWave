package com.nightwave.player.v2.ui

import com.nightwave.player.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.nightwave.player.v2.ui.NightWaveMatteIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.recommendation.PersonalMix

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NightWaveMixPreview(
    mix: PersonalMix,
    presentation: NightWaveHomePresentationPolicy.MixPresentation?,
    playingUrl: String,
    onDismiss: () -> Unit,
    onPlay: (VideoPreview) -> Unit,
    onSongOptions: (VideoPreview) -> Unit
) {
    val strings = rememberNightWaveStrings()
    var showAll by rememberSaveable(mix.key) { mutableStateOf(false) }
    val firstTrack = mix.tracks.firstOrNull()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().testTag("nw155-mix-preview"),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "mix-summary") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(presentation?.title ?: mix.title, style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() })
                    Text(presentation?.reason ?: mix.reason, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NightWaveCompactActions {
                        OutlinedButton(
                            onClick = {
                                if (firstTrack != null) {
                                    onDismiss()
                                    onPlay(firstTrack)
                                }
                            },
                            enabled = firstTrack != null,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("nw131-play-mix")
                        ) { Text(strings.get(R.string.nw141_play_mix_52c1e)) }
                        TextButton(onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("nw155-close-mix")) {
                            Text(strings.get(R.string.nw141_close_7d9eb))
                        }
                    }
                }
            }
            items(mix.tracks.take(if (showAll) 12 else 3), key = { "mix:" + it.url }) { song ->
                MusicTrackTile(song, isPlaying = playingUrl == song.url,
                    onOptions = {
                        onDismiss()
                        onSongOptions(song)
                    },
                    onClick = {
                        onDismiss()
                        onPlay(song)
                    })
            }
            if (mix.tracks.size > 3) item(key = "mix-more-tracks") {
                OutlinedButton(onClick = { showAll = !showAll },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (showAll) strings.get(R.string.nw141_show_fewer_tracks_4f897)
                        else strings.get(R.string.nw141_see_mix, strings.number(mix.tracks.size)))
                }
            }
        }
    }
}

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
    val strings = rememberNightWaveStrings()
    val colors = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    val identity = remember(song.title, song.author) { NightWaveEditorialPresentationPolicy.identity(song.title, song.author) }
    var showActions by remember(song.url) { mutableStateOf(false) }
    val tileWidth = NightWavePremiumDesignTokens.shelfWidthDp(
        LocalConfiguration.current.screenWidthDp.toFloat(), fontScale).dp
    val playLabel = strings.get(R.string.nw58_play_track_title, identity.title)
    Column(Modifier.width(tileWidth).testTag("nw47-recent-tile")) {
        Surface(onClick = onPlay, shape = RoundedCornerShape(NightWavePremiumDesignTokens.CARD_CORNER_DP.dp),
            modifier = Modifier.fillMaxWidth().testTag("nw47-recent-play")
                .semantics { contentDescription = playLabel }) {
            Box {
                TrackArtwork(song, Modifier.fillMaxWidth().aspectRatio(16f / 9f), large = true, crop = true, decorative = true)
                if (isPlaying) Surface(
                    Modifier.align(Alignment.BottomStart).padding(7.dp),
                    color = colors.primary, shape = RoundedCornerShape(7.dp)) {
                    Text(strings.get(R.string.nw141_playing_86042), Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
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
                        contentDescription = strings.get(R.string.nw141_actions_track, identity.title), tint = colors.onSurface)
                }
                DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                    DropdownMenuItem(text = { Text(strings.get(R.string.nw141_track_options_c4b0a)) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("nw127-recent-details"),
                        onClick = { showActions = false; onOptions() })
                    DropdownMenuItem(text = { Text(strings.get(R.string.nw141_remove_from_recently_played_79a97)) },
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
    val strings = rememberNightWaveStrings()
    val scheme = MaterialTheme.colorScheme
    val identity = remember(song.title, song.author) {
        NightWaveEditorialPresentationPolicy.identity(song.title, song.author)
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
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(
                            NightWavePremiumDesignTokens.CARD_CORNER_DP.dp))
                        .background(scheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    TrackArtwork(song, Modifier.fillMaxSize(), large = true, crop = true, decorative = true)
                    if (isPlaying) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                            color = scheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(strings.get(R.string.nw141_playing_86042),
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
                    contentDescription = strings.get(R.string.nw141_options_track, identity.title),
                    tint = scheme.onSurfaceVariant)
            }
        }
    }
}
