package com.nightwave.player.v2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.playback.StudioPlayerActions
import com.nightwave.player.v2.playback.StudioPlayerState
import com.nightwave.player.v2.playback.PlaybackHealthPolicy
import com.nightwave.player.v2.playback.PlaybackPhase
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.v2.lyrics.TimedLyrics
import com.nightwave.player.playback.PlaybackQueue
import com.nightwave.player.playback.SleepTimerPolicy

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
    val health = PlaybackHealthPolicy.classify(state.hasTrack, state.connected,
        state.offline, state.playing, state.buffering, state.resolving, state.error)
    val identity = MusicIdentity.display(state.title, state.artist)
    var travel by remember(state.videoUrl) { mutableStateOf(0f) }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(listOf(
                    NightWaveVisualLanguage.accent(state.videoUrl).copy(alpha = .24f),
                    colors.surfaceVariant,
                    colors.surface
                ))
            )
            .border(1.dp, colors.outline.copy(alpha = .17f), RoundedCornerShape(18.dp))
            .pointerInput(state.videoUrl) {
                detectVerticalDragGestures(
                    onDragStart = { travel = 0f },
                    onVerticalDrag = { change, delta ->
                        change.consume()
                        travel += delta
                    },
                    onDragEnd = {
                        when (PlayerGestureRules.decideVerticalSwipe(
                            travel, PlayerGestureRules.MIN_VERTICAL_SWIPE_DP.dp.toPx()
                        )) {
                            PlayerVerticalSwipe.MINIMIZE -> onHide()
                            PlayerVerticalSwipe.EXPAND -> onExpand()
                            PlayerVerticalSwipe.NONE -> Unit
                        }
                        travel = 0f
                    },
                    onDragCancel = { travel = 0f }
                )
            }
    ) {
        // Visible gesture affordance. The parent still owns swipe-down hide and
        // swipe-up expansion; this indicator does not add a competing gesture.
        Box(Modifier.fillMaxWidth().height(9.dp),
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 32.dp, height = 3.dp)
                .clip(CircleShape)
                .background(colors.onSurfaceVariant.copy(alpha = .48f)))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Preserve room for a legible title on 320–360dp screens.
            // Next remains available in expanded Now Playing on narrow screens.
            val showNext = state.queue.size > 1 && maxWidth >= 365.dp
            Row(
                modifier = Modifier.fillMaxWidth().height(54.dp)
                    .padding(start = 7.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onExpand),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    CoverArt(state.thumbnail, Modifier.size(40.dp), 9)
                    Column(Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(if (state.title.isBlank()) "Restoring your last track…"
                            else identity.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            if (health.phase == PlaybackPhase.LOOKING_UP ||
                                health.phase == PlaybackPhase.BUFFERING ||
                                health.phase == PlaybackPhase.DISCONNECTED ||
                                health.phase == PlaybackPhase.ERROR) health.title
                            else identity.artistOrCreator,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant, maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = actions::playPause, modifier = Modifier.size(48.dp)) {
                    Icon(if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.playing) "Pause playback" else "Resume playback",
                        tint = colors.onSurface)
                }
                if (showNext) {
                    IconButton(onClick = actions::next, modifier = Modifier.size(48.dp),
                        enabled = state.connected && !state.buffering) {
                        Icon(Icons.Outlined.SkipNext, contentDescription = "Next track",
                            tint = colors.onSurface)
                    }
                }
                // Only updates the host's compactHidden UI flag. This MUST NOT
                // invoke pause/stop/service intents or clear Media3's queue.
                IconButton(onClick = onHide, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Close,
                        contentDescription = "Hide mini-player; music keeps playing",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                }
            }
        }
        LinearProgressIndicator(
            progress = {
                PlayerProgressRules.fraction(state.positionMs, state.durationMs)
            },
            modifier = Modifier.fillMaxWidth().height(3.dp),
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
        // The motif stays behind a real artwork request. If Coil cannot load a
        // thumbnail (offline/missing image), the branded fallback remains visible.
        Canvas(Modifier.fillMaxSize(.64f)) {
            drawCircle(Color.White.copy(alpha = .26f), radius = size.minDimension * .44f)
            drawCircle(Color.White.copy(alpha = .65f), radius = size.minDimension * .33f,
                style = Stroke(width = 2.dp.toPx()))
            drawCircle(Color.White.copy(alpha = .7f), radius = size.minDimension * .09f)
        }
        if (url.isNotBlank()) {
            AsyncImage(model = url, contentDescription = "Album artwork",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
    sleepRemainingMs: Long,
    onSleepTimer: (Int?) -> Unit,
    onDismiss: () -> Unit,
    lyrics: TimedLyrics? = null,
    onImportLyrics: () -> Unit = {},
    onRemoveLyrics: () -> Unit = {}
) {
    val enter = if (reduceMotion) EnterTransition.None else slideInVertically(initialOffsetY = { it }) + fadeIn()
    val exit = if (reduceMotion) ExitTransition.None else slideOutVertically(targetOffsetY = { it }) + fadeOut()
    AnimatedVisibility(visible = expanded, enter = enter, exit = exit) {
        val colors = MaterialTheme.colorScheme
        BoxWithConstraints(Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(colors.background)) {
            // Deterministic, artwork-keyed ambient light; never crop or blur
            // remote thumbnails, preserving battery life and privacy.
            NightWaveBackdrop(state.videoUrl.ifBlank { "nightwave-player" }, Modifier.fillMaxSize())
            val art = minOf(maxWidth - 64.dp, maxHeight * .36f, 356.dp)
            val canSwipeArtwork = state.connected && !state.buffering && state.queue.size > 1
            var horizontalTravel by remember(state.videoUrl) { mutableStateOf(0f) }
            var lyricsOpen by remember(state.videoUrl) { mutableStateOf(false) }
            var sleepOptionsOpen by remember { mutableStateOf(false) }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 13.dp)
                    .pointerInput(Unit) {
                        var dragged = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragged = 0f },
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                dragged += amount
                            },
                            onDragEnd = {
                                if (PlayerGestureRules.decideVerticalSwipe(
                                        dragged, PlayerGestureRules.MIN_VERTICAL_SWIPE_DP.dp.toPx()
                                    ) == PlayerVerticalSwipe.MINIMIZE) onDismiss()
                                dragged = 0f
                            }
                        )
                    },
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.ArrowDownward, contentDescription = "Minimize player",
                            tint = colors.onBackground)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NIGHTWAVE", style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                        Text(if (state.offline) "OFFLINE AUDIO" else "NOW PLAYING",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onBackground)
                    }
                    IconButton(onClick = onSaveToPlaylist, enabled = !state.offline,
                        modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Save track to playlist",
                            tint = colors.onBackground)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.size(art).clip(RoundedCornerShape(27.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp,
                            NightWaveVisualLanguage.accent(state.videoUrl).copy(alpha = .46f),
                            RoundedCornerShape(27.dp)).then(
                        if (canSwipeArtwork) Modifier.pointerInput(
                            state.videoUrl, state.queue.size, state.connected, state.buffering
                        ) {
                            detectHorizontalDragGestures(
                                onDragStart = { horizontalTravel = 0f },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    horizontalTravel += amount
                                },
                                onDragEnd = {
                                    when (PlayerGestureRules.decideHorizontalSwipe(
                                        horizontalTravel, PlayerGestureRules.MIN_SWIPE_DP.dp.toPx(),
                                        state.connected, state.buffering, state.queue.size
                                    )) {
                                        PlayerArtworkSwipe.NEXT -> actions.next()
                                        PlayerArtworkSwipe.PREVIOUS -> actions.previous()
                                        PlayerArtworkSwipe.NONE -> Unit
                                    }
                                    horizontalTravel = 0f
                                },
                                onDragCancel = { horizontalTravel = 0f }
                            )
                        } else Modifier
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = state.thumbnail,
                        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 180),
                        label = "Player artwork"
                    ) { thumbnail ->
                        CoverArt(thumbnail, Modifier.fillMaxSize(), 26)
                    }
                    if (state.buffering) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape)
                                .background(colors.background.copy(alpha = .76f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(29.dp),
                                color = colors.primary, strokeWidth = 2.dp
                            )
                        }
                    }
                }
                Row(Modifier.padding(top = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(9.dp),
                        color = colors.surfaceVariant) {
                        Text(if (state.offline) "LOCAL AUDIO" else "PUBLIC VIDEO",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                    }
                    if (canSwipeArtwork) {
                        Text("Swipe artwork to skip", style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(17.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (state.title.isBlank()) "Choose a track"
                            else MusicIdentity.display(state.title, state.artist).title,
                            style = MaterialTheme.typography.headlineSmall, color = colors.onBackground,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        Text(MusicIdentity.display(state.title, state.artist).artistOrCreator,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onLike, enabled = !state.offline) {
                        Icon(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (liked) "Unlike song" else "Like song",
                            tint = if (liked) colors.primary else colors.onBackground)
                    }
                }
                Spacer(Modifier.height(11.dp))
                // A scrub in progress belongs to exactly one track. Changing tracks
                // clears the pending drag so an old percentage cannot seek the next.
                var dragFraction by remember(state.videoUrl) { mutableStateOf<Float?>(null) }
                val fraction = dragFraction ?: PlayerProgressRules.fraction(
                    state.positionMs, state.durationMs)
                Slider(value = fraction, onValueChange = { dragFraction = it },
                    onValueChangeFinished = {
                        dragFraction?.let {
                            PlayerProgressRules.requestedPosition(state.durationMs, it)
                                ?.let(actions::seekTo)
                        }
                        dragFraction = null
                    },
                    enabled = state.connected && state.durationMs > 0L)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(timeLabel(PlayerProgressRules.requestedPosition(
                        state.durationMs, fraction) ?: 0L), style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant)
                    Text(timeLabel(state.durationMs), style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = actions::toggleShuffle, enabled = !state.offline,
                        modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Shuffle, contentDescription = "Toggle shuffle",
                            tint = if (state.shuffle) colors.primary else colors.onSurfaceVariant)
                    }
                    IconButton(onClick = actions::previous, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous track",
                            modifier = Modifier.size(34.dp), tint = colors.onBackground)
                    }
                    Surface(onClick = actions::playPause,
                        color = colors.primary,
                        shadowElevation = if (reduceMotion) 0.dp else 5.dp,
                        shape = CircleShape, modifier = Modifier.size(68.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.playing) "Pause" else "Play",
                                tint = colors.onPrimary, modifier = Modifier.size(33.dp))
                        }
                    }
                    IconButton(onClick = actions::next, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.SkipNext, contentDescription = "Next track",
                            modifier = Modifier.size(34.dp), tint = colors.onBackground)
                    }
                    IconButton(onClick = actions::cycleRepeat, enabled = !state.offline,
                        modifier = Modifier.size(48.dp)) {
                        Icon(if (state.repeat == PlaybackQueue.Repeat.ONE) Icons.Outlined.RepeatOne
                            else Icons.Outlined.Repeat,
                            contentDescription = "Repeat " + state.repeat.name,
                            tint = if (state.repeat == PlaybackQueue.Repeat.OFF)
                                colors.onSurfaceVariant else colors.primary)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Surface(
                    onClick = onOpenQueue, modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp), color = colors.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Up next · " + state.queue.size + " tracks", style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Outlined.QueueMusic, contentDescription = "Open queue", tint = colors.primary)
                    }
                }
                Spacer(Modifier.height(11.dp))
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { sleepOptionsOpen = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(SleepTimerPolicy.label(sleepRemainingMs))
                    }
                    DropdownMenu(
                        expanded = sleepOptionsOpen,
                        onDismissRequest = { sleepOptionsOpen = false }
                    ) {
                        SleepTimerPolicy.choicesMinutes.forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text("Pause music after " + minutes + " min") },
                                onClick = {
                                    sleepOptionsOpen = false
                                    onSleepTimer(minutes)
                                }
                            )
                        }
                        if (sleepRemainingMs > 0L) {
                            DropdownMenuItem(
                                text = { Text("Cancel sleep timer") },
                                onClick = {
                                    sleepOptionsOpen = false
                                    onSleepTimer(null)
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = { lyricsOpen = !lyricsOpen },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = colors.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Lyrics", style = MaterialTheme.typography.titleMedium)
                        Text(if (lyricsOpen) "Hide" else "View",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary)
                    }
                }
                if (lyricsOpen) {
                    Spacer(Modifier.height(10.dp))
                    if (!state.offline) {
                        Text("Synchronized lyrics are not available for public videos. " +
                            "NightWave does not copy lyrics from streaming sites.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant)
                    } else if (lyrics == null) {
                        Text("Import your own timestamped .lrc file to show lyrics for this local song.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onImportLyrics,
                            modifier = Modifier.fillMaxWidth()) { Text("Import synchronized lyrics") }
                    } else {
                        val active = lyrics.activeIndex(state.positionMs)
                        val start = (active - 2).coerceAtLeast(0)
                        Text("Synchronized with this offline track",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Column(Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            lyrics.cues.drop(start).take(6).forEachIndexed { i, cue ->
                                val current = start + i == active
                                Text(cue.text, style = if (current)
                                    MaterialTheme.typography.titleMedium
                                    else MaterialTheme.typography.bodyLarge,
                                    color = if (current) colors.primary
                                        else colors.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            OutlinedButton(onClick = onImportLyrics,
                                modifier = Modifier.weight(1f)) { Text("Replace .lrc") }
                            OutlinedButton(onClick = onRemoveLyrics,
                                modifier = Modifier.weight(1f)) { Text("Remove lyrics") }
                        }
                    }
                }
                Spacer(Modifier.height(26.dp))
                if (state.buffering) {
                    val health = PlaybackHealthPolicy.classify(
                        state.hasTrack, state.connected, state.offline,
                        state.playing, state.buffering, state.resolving, state.error)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(25.dp),
                            color = colors.primary, strokeWidth = 2.dp)
                        Text(health.title, style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant)
                    }
                }
                state.error?.takeIf { it.isNotBlank() }?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.error)
                    Button(onClick = actions::retry) {
                        Text(if (state.connected) "Retry playback" else "Reconnect player")
                    }
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
    onSelect: (Int) -> Unit,
    onRemoveUpcoming: (Int, String) -> String?,
    onUndoRemoval: (String) -> Unit,
    reduceMotion: Boolean
) {
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun requestRemove(index: Int, url: String) {
        val token = onRemoveUpcoming(index, url) ?: return
        scope.launch {
            snackbarHost.currentSnackbarData?.dismiss()
            val result = snackbarHost.showSnackbar(
                message = "Removed from Up Next",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onUndoRemoval(token)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            Text("Up next", style = MaterialTheme.typography.headlineSmall)
            val removable = !state.offline && !state.shuffle && state.connected
            Text(
                when {
                    state.offline -> "${state.queue.size} offline tracks · swipe removal unavailable for local files"
                    state.shuffle -> "${state.queue.size} tracks · turn off shuffle to edit upcoming order"
                    else -> "${state.queue.size} tracks · swipe left on upcoming songs to remove"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(13.dp))
            if (state.queue.isEmpty()) {
                EmptyMusicCard("Your queue is empty", "Play any song to start listening.",
                    "Close queue", onDismiss)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    // Repeated URLs can occur in a restored/imported queue. A duplicate
                    // LazyColumn key crashes Compose; use the absolute queue slot.
                    itemsIndexed(state.queue, key = { index, _ -> index }) { index, video ->
                        val canRemove = removable && index > state.queueIndex &&
                            state.queueIndex >= 0 &&
                            video.url.startsWith("https://www.youtube.com/watch?v=")
                        SwipeUpcomingRow(
                            number = index + 1,
                            title = video.title,
                            subtitle = video.author,
                            artwork = video.thumbnail,
                            isPlaying = index == state.queueIndex,
                            canRemove = canRemove,
                            reduceMotion = reduceMotion,
                            onSelect = { onSelect(index) },
                            onRemove = { requestRemove(index, video.url) }
                        )
                    }
                }
            }
            SnackbarHost(hostState = snackbarHost, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SwipeUpcomingRow(
    number: Int,
    title: String,
    subtitle: String,
    artwork: String,
    isPlaying: Boolean,
    canRemove: Boolean,
    reduceMotion: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    var travel by remember(title, number) { mutableStateOf(0f) }
    val animatedTravel by animateFloatAsState(
        targetValue = travel,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 130),
        label = "Upcoming track swipe"
    )
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))) {
        if (canRemove) {
            Row(
                Modifier.matchParentSize().background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(5.dp))
                Text("Remove", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .graphicsLayer { translationX = animatedTravel }
                .clip(RoundedCornerShape(14.dp))
                .background(if (isPlaying) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surface)
                .then(if (canRemove) Modifier.pointerInput(title, number, canRemove) {
                    detectHorizontalDragGestures(
                        onDragStart = { travel = 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            travel = (travel + amount).coerceIn(-116.dp.toPx(), 0f)
                        },
                        onDragEnd = {
                            val shouldRemove = travel <= -72.dp.toPx()
                            travel = 0f
                            if (shouldRemove) onRemove()
                        },
                        onDragCancel = { travel = 0f }
                    )
                } else Modifier)
                .clickable(onClick = onSelect)
                .testTag("nightwave-upcoming-$number")
                .padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(number.toString(), modifier = Modifier.width(19.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium)
            CoverArt(artwork, Modifier.size(45.dp), 10)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall)
                Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isPlaying) {
                Text("PLAYING", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            } else if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove ${title.take(36)} from Up Next",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
