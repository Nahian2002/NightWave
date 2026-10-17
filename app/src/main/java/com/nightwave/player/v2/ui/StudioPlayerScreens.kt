package com.nightwave.player.v2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.playback.StudioPlayerActions
import com.nightwave.player.v2.playback.StudioPlayerState
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.playback.PlaybackQueue

private fun timeLabel(ms: Long): String {
    val secs = (ms.coerceAtLeast(0L) / 1000L)
    return if (secs >= 3600) {
        (secs / 3600).toString() + ":" + ((secs / 60) % 60).toString().padStart(2, '0') +
            ":" + (secs % 60).toString().padStart(2, '0')
    } else (secs / 60).toString() + ":" + (secs % 60).toString().padStart(2, '0')
}

@Composable
fun StudioMiniPlayer(
    state: StudioPlayerState,
    actions: StudioPlayerActions,
    onExpand: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.hasTrack) return
    val colors = MaterialTheme.colorScheme
    var travel by remember(state.videoUrl) { mutableStateOf(0f) }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant)
            .pointerInput(state.videoUrl) {
                detectVerticalDragGestures(
                    onDragStart = { travel = 0f },
                    onVerticalDrag = { change, delta ->
                        change.consume()
                        travel += delta
                    },
                    onDragEnd = {
                        if (travel > 70f) onHide()
                        else if (travel < -70f) onExpand()
                        travel = 0f
                    },
                    onDragCancel = { travel = 0f }
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp)
                .clickable(onClick = onExpand)
                .padding(start = 7.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CoverArt(state.thumbnail, Modifier.size(44.dp), 9)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(state.title.ifBlank { "Restoring your last track…" },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (state.buffering) "Connecting…" else state.artist.ifBlank { "NightWave" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = actions::playPause, modifier = Modifier.size(48.dp)) {
                Icon(if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.playing) "Pause playback" else "Resume playback",
                    tint = colors.onSurface)
            }
        }
        LinearProgressIndicator(
            progress = {
                if (state.durationMs > 0L)
                    (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
            },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = colors.primary,
            trackColor = colors.outline.copy(alpha = .28f)
        )
    }
}

@Composable
private fun CoverArt(url: String, modifier: Modifier = Modifier, radius: Int = 24) {
    val shape = RoundedCornerShape(radius.dp)
    Box(modifier.clip(shape).background(
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = .5f),
            MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface))
    ), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            AsyncImage(model = url, contentDescription = "Album artwork",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Canvas(Modifier.fillMaxSize(.64f)) {
                drawCircle(Color.White.copy(alpha = .26f), radius = size.minDimension * .44f)
                drawCircle(Color.White.copy(alpha = .65f), radius = size.minDimension * .33f,
                    style = Stroke(width = 2.dp.toPx()))
                drawCircle(Color.White.copy(alpha = .7f), radius = size.minDimension * .09f)
            }
        }
    }
}

@Composable
fun StudioNowPlayingOverlay(
    expanded: Boolean,
    state: StudioPlayerState,
    actions: StudioPlayerActions,
    reduceMotion: Boolean,
    liked: Boolean,
    onLike: () -> Unit,
    onOpenQueue: () -> Unit,
    onSaveToPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    val enter = if (reduceMotion) EnterTransition.None else slideInVertically(initialOffsetY = { it }) + fadeIn()
    val exit = if (reduceMotion) ExitTransition.None else slideOutVertically(targetOffsetY = { it }) + fadeOut()
    AnimatedVisibility(visible = expanded, enter = enter, exit = exit) {
        val colors = MaterialTheme.colorScheme
        val background = Brush.verticalGradient(listOf(colors.surfaceVariant, colors.background, colors.background))
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).background(background)) {
            val art = minOf(maxWidth - 82.dp, maxHeight * .41f, 430.dp)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.ArrowDownward, contentDescription = "Minimize player",
                            tint = colors.onBackground)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NIGHTWAVE", style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                        Text("NOW PLAYING", style = MaterialTheme.typography.labelLarge,
                            color = colors.onBackground)
                    }
                    IconButton(onClick = onSaveToPlaylist, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Save track to playlist",
                            tint = colors.onBackground)
                    }
                }
                Spacer(Modifier.height(8.dp))
                CoverArt(state.thumbnail,
                    Modifier.size(art).clip(RoundedCornerShape(26.dp)), 26)
                Spacer(Modifier.height(28.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(state.title.ifBlank { "Choose a track" },
                            style = MaterialTheme.typography.headlineSmall, color = colors.onBackground,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        Text(state.artist, style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onLike) {
                        Icon(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (liked) "Unlike song" else "Like song",
                            tint = if (liked) colors.primary else colors.onBackground)
                    }
                }
                Spacer(Modifier.height(20.dp))
                var dragFraction by remember { mutableStateOf<Float?>(null) }
                val fraction = dragFraction ?: if (state.durationMs > 0L)
                    (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
                Slider(value = fraction, onValueChange = { dragFraction = it },
                    onValueChangeFinished = {
                        dragFraction?.let { actions.seekTo((state.durationMs * it).toLong()) }
                        dragFraction = null
                    },
                    enabled = state.durationMs > 0)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(timeLabel((state.durationMs * fraction).toLong()), style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant)
                    Text(timeLabel(state.durationMs), style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = actions::toggleShuffle, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Shuffle, contentDescription = "Toggle shuffle",
                            tint = if (state.shuffle) colors.primary else colors.onSurfaceVariant)
                    }
                    IconButton(onClick = actions::previous, modifier = Modifier.size(53.dp)) {
                        Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous track",
                            modifier = Modifier.size(34.dp), tint = colors.onBackground)
                    }
                    Surface(onClick = actions::playPause, color = colors.primary,
                        shape = CircleShape, modifier = Modifier.size(72.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.playing) "Pause" else "Play",
                                tint = colors.onPrimary, modifier = Modifier.size(36.dp))
                        }
                    }
                    IconButton(onClick = actions::next, modifier = Modifier.size(53.dp)) {
                        Icon(Icons.Outlined.SkipNext, contentDescription = "Next track",
                            modifier = Modifier.size(34.dp), tint = colors.onBackground)
                    }
                    IconButton(onClick = actions::cycleRepeat, modifier = Modifier.size(48.dp)) {
                        Icon(if (state.repeat == PlaybackQueue.Repeat.ONE) Icons.Outlined.RepeatOne
                            else Icons.Outlined.Repeat,
                            contentDescription = "Repeat " + state.repeat.name,
                            tint = if (state.repeat == PlaybackQueue.Repeat.OFF)
                                colors.onSurfaceVariant else colors.primary)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Surface(
                    onClick = onOpenQueue, modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(17.dp), color = colors.surface
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Up next · " + state.queue.size + " tracks", style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Outlined.QueueMusic, contentDescription = "Open queue", tint = colors.primary)
                    }
                }
                Spacer(Modifier.height(26.dp))
                if (state.buffering) {
                    CircularProgressIndicator(modifier = Modifier.size(25.dp),
                        color = colors.primary, strokeWidth = 2.dp)
                }
                state.error?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.error)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StudioQueueSheet(
    state: StudioPlayerState,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Up next", style = MaterialTheme.typography.headlineSmall)
            Text(state.queue.size.toString() + " saved tracks · tap to play",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            if (state.queue.isEmpty()) {
                EmptyMusicCard("Your queue is empty", "Play any song to start your listening session.",
                    "Close queue", onDismiss)
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 38.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    itemsIndexed(state.queue, key = { _, video -> video.url }) { index, video ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(if (index == state.queueIndex)
                                    MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                                .clickable { onSelect(index) }.padding(9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp)
                        ) {
                            Text((index + 1).toString(), modifier = Modifier.width(20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CoverArt(video.thumbnail, Modifier.size(49.dp), 10)
                            Column(Modifier.weight(1f)) {
                                Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium)
                                Text(video.author, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (index == state.queueIndex) {
                                Text("PLAYING", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
