package com.nightwave.player.v2.ui

import android.view.HapticFeedbackConstants

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.nightwave.player.v2.lyrics.LyricsSyncPolicy
import com.nightwave.player.v2.lyrics.LyricsPresentationPolicy
import com.nightwave.player.playback.PlaybackQueue
import com.nightwave.player.playback.SleepTimerPolicy
import com.nightwave.player.playback.CrossfadePolicy
import com.nightwave.player.playback.AudioTransitionUiPolicy
import kotlin.math.roundToInt
import com.nightwave.player.playback.OfflinePlaybackSpeedPolicy
import com.nightwave.player.playback.ListeningLevelPolicy

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
    val fontScale = LocalDensity.current.fontScale
    val compactRowHeight = NightWaveAdaptiveChromePolicy.miniPlayerHeightDp(fontScale).dp
    val colors = MaterialTheme.colorScheme
    val health = PlaybackHealthPolicy.classify(state.hasTrack, state.connected,
        state.offline, state.playing, state.buffering, state.resolving, state.error)
    val identity = MusicIdentity.display(state.title, state.artist)
    val reducedMotion = LocalNightWaveReduceMotion.current
    val hapticView = LocalView.current
    val miniScale by animateFloatAsState(
        targetValue = NightWaveMotionPolicy.artworkScale(state.playing, reducedMotion),
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = .78f, stiffness = 500f),
        label = "Mini player artwork motion"
    )
    var travel by remember(state.videoUrl) { mutableStateOf(0f) }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)
            .testTag("nightwave-mini-player")
            .shadow(8.dp, RoundedCornerShape(19.dp))
            .clip(RoundedCornerShape(19.dp))
            .background(
                Brush.horizontalGradient(listOf(
                    NightWaveVisualLanguage.accent(state.videoUrl).copy(alpha = .20f),
                    colors.surfaceVariant,
                    colors.surface
                ))
            )
            .border(1.dp, colors.outline.copy(alpha = .13f), RoundedCornerShape(16.dp))
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
        Box(Modifier.fillMaxWidth().height(5.dp),
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 24.dp, height = 2.dp)
                .clip(CircleShape)
                .background(colors.onSurfaceVariant.copy(alpha = .48f)))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Preserve room for a legible title on 320–360dp screens.
            // Next remains available in expanded Now Playing on narrow screens.
            val showNext = NightWaveChromePolicy.showMiniNext(
                queueCount = state.queue.size, availableWidthDp = maxWidth.value)
            Row(
                modifier = Modifier.fillMaxWidth().height(compactRowHeight)
                    .padding(start = 8.dp, end = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (!reducedMotion)
                                hapticView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onExpand()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Crossfade(
                        targetState = state.thumbnail,
                        animationSpec = tween(
                            durationMillis = NightWaveMotionPolicy.duration(
                                reducedMotion, NightWaveMotionPolicy.COVER_MS)),
                        label = "Mini player changing cover"
                    ) { thumbnail ->
                        CoverArt(thumbnail, Modifier.size(
                            if (fontScale >= 1.3f) 44.dp else 40.dp)
                            .graphicsLayer {
                                scaleX = miniScale
                                scaleY = miniScale
                            }, 11)
                    }
                    Column(Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(if (state.title.isBlank()) "Restoring your last track…"
                            else identity.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold),
                            color = colors.onSurface, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            if (health.phase == PlaybackPhase.LOOKING_UP ||
                                health.phase == PlaybackPhase.BUFFERING ||
                                health.phase == PlaybackPhase.DISCONNECTED ||
                                health.phase == PlaybackPhase.ERROR) health.title
                            else identity.artistOrCreator,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant, maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = {
                    if (!reducedMotion)
                        hapticView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    actions.playPause()
                }, modifier = Modifier.size(48.dp)) {
                    Icon(if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.playing) "Pause playback" else "Resume playback",
                        tint = colors.onSurface)
                }
                if (showNext) {
                    IconButton(onClick = {
                        if (!reducedMotion)
                            hapticView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        actions.next()
                    }, modifier = Modifier.size(48.dp),
                        enabled = state.connected && !state.buffering) {
                        Icon(Icons.Outlined.SkipNext, contentDescription = "Next track",
                            tint = colors.onSurface)
                    }
                }
                // Only updates the host's compactHidden UI flag. This MUST NOT
                // invoke pause/stop/service intents or clear Media3's queue.
                IconButton(onClick = {
                    if (!reducedMotion)
                        hapticView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onHide()
                }, modifier = Modifier.size(48.dp)) {
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
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = colors.primary,
            trackColor = colors.outline.copy(alpha = .28f)
        )
    }
}

@Composable
private fun CoverArt(url: String, modifier: Modifier = Modifier, radius: Int = 24) {
    val preferredArtwork = remember(url) { NightWaveArtworkPolicy.preferredArtwork(url) }
    var useOriginal by remember(url) { mutableStateOf(false) }
    val displayUrl = if (useOriginal) url else preferredArtwork
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
        if (displayUrl.isNotBlank()) {
            AsyncImage(
                model = displayUrl,
                contentDescription = "Album artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    // Some uploads do not provide a high-quality thumbnail.
                    // Fall back to the original address instead of blank art.
                    if (!useOriginal && NightWaveArtworkPolicy.allowOriginalFallback(
                            url, preferredArtwork)) useOriginal = true
                }
            )
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
    onSaveOffline: () -> Unit,
    sleepRemainingMs: Long,
    onSleepTimer: (Int?) -> Unit,
    offlineSpeedPercent: Int,
    onOfflineSpeed: (Int) -> Unit,
    listeningLevelPercent: Int,
    onListeningLevel: (Int) -> Unit,
    crossfadeSeconds: Int,
    onCrossfade: (Int) -> Unit,
    normalizationEnabled: Boolean,
    onNormalizationChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    lyrics: TimedLyrics? = null,
    lyricsOffsetMs: Int = 0,
    onLyricsOffsetChange: (Int) -> Unit = {},
    onImportLyrics: () -> Unit = {},
    onEditLyrics: () -> Unit = {},
    onRemoveLyrics: () -> Unit = {}
) {
    // No full-height fly-in: short translated motion and alpha make the
    // player feel attached to the mini player instead of sliding across a screen.
    val enter = if (reduceMotion) EnterTransition.None else
        slideInVertically(
            animationSpec = tween(durationMillis = NightWaveMotionPolicy.PLAYER_ENTER_MS),
            initialOffsetY = NightWaveMotionPolicy::playerEntryOffsetPx
        ) + fadeIn(animationSpec = tween(
            durationMillis = NightWaveMotionPolicy.PLAYER_ENTER_MS))
    val exit = if (reduceMotion) ExitTransition.None else
        slideOutVertically(
            animationSpec = tween(durationMillis = NightWaveMotionPolicy.PLAYER_EXIT_MS),
            targetOffsetY = NightWaveMotionPolicy::playerExitOffsetPx
        ) + fadeOut(animationSpec = tween(
            durationMillis = NightWaveMotionPolicy.PLAYER_EXIT_MS))
    val overlayVisibility = remember { MutableTransitionState(false) }
    LaunchedEffect(expanded) { overlayVisibility.targetState = expanded }
    // NW58: opacity on the animated *full-screen container* revealed Home
    // through the player at the start/end of the NW50 transition. Keep a
    // solid, non-animated backdrop for the entire transition lifetime.
    // The backdrop goes away only when the closing animation is complete.
    if (NightWavePlayerOverlayPolicy.retainBackdrop(
            expanded, overlayVisibility.currentState, overlayVisibility.targetState)) {
        Box(Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("nw58-opaque-player-backdrop"))
    }
    AnimatedVisibility(
        visibleState = overlayVisibility,
        enter = enter,
        exit = exit
    ) {
        val colors = MaterialTheme.colorScheme
        BoxWithConstraints(Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(colors.background)) {
            // Deterministic, artwork-keyed ambient light; never crop or blur
            // remote thumbnails, preserving battery life and privacy.
            NightWaveBackdrop(state.videoUrl.ifBlank { "nightwave-player" }, Modifier.fillMaxSize())
            val fontScale = LocalDensity.current.fontScale
            val playerLayout = NightWaveNowPlayingLayoutPolicy.resolve(
                maxWidth.value, maxHeight.value, fontScale)
            val contentPaddingDp = playerLayout.horizontalPaddingDp
            val art = playerLayout.artworkDp.dp
            val canSwipeArtwork = state.connected && !state.buffering && state.queue.size > 1
            var horizontalTravel by remember(state.videoUrl) { mutableStateOf(0f) }
            val playingArtworkScale by animateFloatAsState(
                targetValue = NightWaveMotionPolicy.artworkScale(
                    state.playing, reduceMotion),
                animationSpec = if (reduceMotion) snap() else spring(
                    dampingRatio = .78f, stiffness = 310f),
                label = "Now Playing artwork spring"
            )
            var lyricsOpen by remember(state.videoUrl) { mutableStateOf(false) }
            var lyricsGuideOpen by remember(state.videoUrl) { mutableStateOf(false) }
            var lyricsImmersive by remember(state.videoUrl) { mutableStateOf(false) }
            var lyricsAutoFollow by remember(state.videoUrl) { mutableStateOf(true) }
            var lyricsFineTiming by remember(state.videoUrl) { mutableStateOf(false) }
            var crossfadeOptionsOpen by remember { mutableStateOf(false) }
            var crossfadeDraft by remember { mutableStateOf(crossfadeSeconds) }
            var sleepOptionsOpen by remember { mutableStateOf(false) }
            var speedOptionsOpen by remember { mutableStateOf(false) }
            var listeningOptionsOpen by remember { mutableStateOf(false) }
            var advancedAudioOpen by remember { mutableStateOf(false) }
            if (lyricsImmersive && state.offline && lyrics != null) {
                val activeCue = LyricsSyncPolicy.activeIndex(
                    lyrics, state.positionMs, lyricsOffsetMs)
                val lyricListState = rememberLazyListState()
                // Only a genuine touch drag suspends follow, not our own animated scroll.
                val lyricsDragged by lyricListState.interactionSource.collectIsDraggedAsState()
                LaunchedEffect(lyricsDragged) {
                    if (lyricsDragged) lyricsAutoFollow = false
                }
                LaunchedEffect(activeCue, lyricsAutoFollow, state.videoUrl, reduceMotion) {
                    if (lyricsAutoFollow && activeCue >= 0) {
                        val target = LyricsPresentationPolicy.scrollTarget(activeCue)
                        if (reduceMotion) lyricListState.scrollToItem(target)
                        else lyricListState.animateScrollToItem(target)
                    }
                }
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { lyricsImmersive = false }) {
                            Icon(Icons.Outlined.ArrowDownward,
                                contentDescription = "Return to now playing",
                                tint = colors.onBackground)
                        }
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("NIGHTWAVE LYRICS",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary)
                            Text(state.title, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium)
                            Text(state.artist, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            lyricsImmersive = false
                            onDismiss()
                        }) {
                            Icon(Icons.Outlined.Close,
                                contentDescription = "Minimize lyrics and player",
                                tint = colors.onBackground)
                        }
                    }
                    Row(Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (lyricsAutoFollow)
                                "Your imported LRC · Tap a line to seek"
                            else "Browsing lyrics · Tap Follow to resume",
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall)
                        OutlinedButton(onClick = { lyricsAutoFollow = true },
                            enabled = !lyricsAutoFollow,
                            modifier = Modifier.testTag("lyrics-follow-toggle")) {
                            Text("Follow")
                        }
                        Spacer(Modifier.width(7.dp))
                        OutlinedButton(onClick = onEditLyrics) {
                            Text("Edit")
                        }
                    }
                    LazyColumn(
                        state = lyricListState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(lyrics.cues,
                            key = { index, _ -> index }) { index, cue ->
                            val current = index == activeCue
                            // NW49: a queued tap belongs to the displayed track.
                            // It may not seek another song after a rapid skip.
                            val cueOriginTrack = state.videoUrl
                            Surface(
                                onClick = {
                                    actions.seekToForTrack(cueOriginTrack,
                                        LyricsSyncPolicy.seekPosition(
                                            cue, lyricsOffsetMs, state.durationMs))
                                    lyricsAutoFollow = true
                                },
                                enabled = state.connected && state.hasTrack &&
                                    cueOriginTrack.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                                    .testTag("lyrics-cue-$index"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (current) colors.primaryContainer
                                    else Color.Transparent
                            ) {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Text(cue.text,
                                        style = if (current)
                                            MaterialTheme.typography.titleLarge
                                        else MaterialTheme.typography.titleMedium,
                                        fontWeight = if (current)
                                            FontWeight.Bold else FontWeight.Normal,
                                        color = if (current) colors.onPrimaryContainer
                                            else colors.onSurfaceVariant)
                                    if (current) {
                                        Text(timeLabel(LyricsSyncPolicy.seekPosition(
                                            cue, lyricsOffsetMs, state.durationMs)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onPrimaryContainer)
                                        // No inferred line-duration bar for the final LRC cue.
                                        if (lyrics.cues.lastOrNull()?.timeMs?.let {
                                                it > cue.timeMs
                                            } == true) {
                                            LinearProgressIndicator(
                                                progress = {
                                                    LyricsPresentationPolicy.lineProgress(
                                                        lyrics, activeCue, state.positionMs,
                                                        lyricsOffsetMs)
                                                },
                                                modifier = Modifier.fillMaxWidth().height(3.dp)
                                                    .padding(top = 1.dp)
                                                    .testTag("lyrics-line-progress"),
                                                color = colors.primary,
                                                trackColor = colors.onPrimaryContainer.copy(alpha = .12f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lyric timing · " + LyricsSyncPolicy.label(lyricsOffsetMs),
                                style = MaterialTheme.typography.labelLarge)
                            Text("Positive timing delays lines; negative timing moves them earlier.",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall)
                            OutlinedButton(
                                onClick = { lyricsFineTiming = !lyricsFineTiming },
                                modifier = Modifier.fillMaxWidth().padding(top = 9.dp)
                                    .testTag("lyrics-fine-timing-toggle")
                            ) {
                                Text(if (lyricsFineTiming) "Fine timing · 50 ms"
                                    else "Timing step · 250 ms")
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = {
                                        onLyricsOffsetChange(
                                            if (lyricsFineTiming)
                                                LyricsSyncPolicy.fineAdjust(lyricsOffsetMs, -1)
                                            else LyricsSyncPolicy.adjust(lyricsOffsetMs, -1))
                                    },
                                    enabled = lyricsOffsetMs > -LyricsSyncPolicy.MAX_OFFSET_MS,
                                    modifier = Modifier.weight(1f)
                                ) { Text(if (lyricsFineTiming) "−0.05s" else "−0.25s") }
                                OutlinedButton(
                                    onClick = { onLyricsOffsetChange(0) },
                                    enabled = lyricsOffsetMs != 0,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reset") }
                                OutlinedButton(
                                    onClick = {
                                        onLyricsOffsetChange(
                                            if (lyricsFineTiming)
                                                LyricsSyncPolicy.fineAdjust(lyricsOffsetMs, 1)
                                            else LyricsSyncPolicy.adjust(lyricsOffsetMs, 1))
                                    },
                                    enabled = lyricsOffsetMs < LyricsSyncPolicy.MAX_OFFSET_MS,
                                    modifier = Modifier.weight(1f)
                                ) { Text(if (lyricsFineTiming) "+0.05s" else "+0.25s") }
                            }
                        }
                    }
                }
            } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = contentPaddingDp.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Row(Modifier.fillMaxWidth()
                    .padding(top = 7.dp, bottom = playerLayout.headerBottomSpaceDp.dp)
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
                        Text("NightWave",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground)
                        Text(if (state.offline) "FROM YOUR OFFLINE LIBRARY" else "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                    }
                    IconButton(onClick = onSaveToPlaylist, enabled = !state.offline,
                        modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Save track to playlist",
                            tint = colors.onBackground)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier.size(art)
                        .testTag("nightwave-now-playing-artwork")
                        .shadow(if (reduceMotion) 8.dp else 20.dp,
                            RoundedCornerShape(playerLayout.artworkCornerDp.dp))
                        .graphicsLayer {
                            scaleX = playingArtworkScale
                            scaleY = playingArtworkScale
                        }
                        .clip(RoundedCornerShape(playerLayout.artworkCornerDp.dp))
                        .background(
                            Brush.linearGradient(listOf(
                                NightWaveVisualLanguage.accent(state.videoUrl).copy(alpha = .36f),
                                colors.surfaceVariant, colors.surface
                            ))
                        )
                        .border(1.dp,
                            colors.onBackground.copy(alpha = .12f),
                            RoundedCornerShape(playerLayout.artworkCornerDp.dp)).then(
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
                        animationSpec = tween(
                            durationMillis = NightWaveMotionPolicy.duration(
                                reduceMotion, NightWaveMotionPolicy.COVER_MS)),
                        label = "Player artwork"
                    ) { thumbnail ->
                        CoverArt(thumbnail, Modifier.fillMaxSize(),
                            playerLayout.artworkCornerDp)
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
                // Don't advertise the source type as a prominent pill on
                // every Now Playing screen. Keep only a useful gesture hint.
                if (canSwipeArtwork) {
                    Text("SWIPE COVER TO SKIP",
                        modifier = Modifier.padding(top = 11.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = .85f))
                }
                Spacer(Modifier.height(if (playerLayout.compact) 13.dp else 22.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (state.title.isBlank()) "Choose a track"
                            else MusicIdentity.display(state.title, state.artist).title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onBackground,
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
                NightWaveSeekTimeline(
                    trackIdentity = state.videoUrl,
                    fraction = fraction,
                    enabled = state.connected && state.durationMs > 0L,
                    onPreview = { dragFraction = it },
                    onCommit = { originatingTrack, requested ->
                        PlayerProgressRules.requestedPosition(state.durationMs, requested)
                            ?.let { actions.seekToForTrack(originatingTrack, it) }
                        dragFraction = null
                    },
                    onCancel = { dragFraction = null }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(timeLabel(PlayerProgressRules.requestedPosition(
                        state.durationMs, fraction) ?: 0L), style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant)
                    Text(timeLabel(state.durationMs), style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = actions::toggleShuffle,
                        modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Shuffle,
                            contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off",
                            modifier = Modifier.size(25.dp),
                            tint = if (state.shuffle) colors.primary else colors.onSurfaceVariant)
                    }
                    IconButton(onClick = actions::previous, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous track",
                            modifier = Modifier.size(34.dp), tint = colors.onBackground)
                    }
                    Surface(onClick = actions::playPause,
                        color = colors.primary,
                        shadowElevation = if (reduceMotion) 0.dp else 10.dp,
                        shape = CircleShape,
                        modifier = Modifier.size(if (playerLayout.compact) 64.dp else 72.dp)
                            .testTag("nightwave-now-playing-play-pause")) {
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
                    IconButton(onClick = actions::cycleRepeat,
                        modifier = Modifier.size(48.dp)) {
                        Icon(if (state.repeat == PlaybackQueue.Repeat.ONE) Icons.Outlined.RepeatOne
                            else Icons.Outlined.Repeat,
                            modifier = Modifier.size(25.dp),
                            contentDescription = "Repeat mode: " + when (state.repeat) {
                                PlaybackQueue.Repeat.OFF -> "Off"
                                PlaybackQueue.Repeat.ALL -> "All songs"
                                PlaybackQueue.Repeat.ONE -> "Current song"
                            },
                            tint = if (state.repeat == PlaybackQueue.Repeat.OFF)
                                colors.onSurfaceVariant else colors.primary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = onSaveOffline,
                    enabled = state.hasTrack,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                        .testTag("nightwave-now-playing-offline-action"),
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surfaceVariant.copy(alpha = .74f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (!state.hasTrack) "Choose a track first"
                            else if (state.offline) "Open offline library"
                            else "Save for offline listening",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurface)
                    }
                }
                Spacer(Modifier.height(9.dp))
                Surface(
                    onClick = onOpenQueue,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                        .testTag("nightwave-now-playing-queue-action"),
                    shape = RoundedCornerShape(18.dp), color = colors.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Up next", style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant)
                            Text("Queue · " + state.queue.size + " " +
                                if (state.queue.size == 1) "track" else "tracks",
                                style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(Icons.AutoMirrored.Outlined.QueueMusic, contentDescription = "Open queue", tint = colors.primary)
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
                    if (state.offline) {
                        OutlinedButton(
                            onClick = { lyricsGuideOpen = !lyricsGuideOpen },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (lyricsGuideOpen) "Hide LRC import guide"
                                else "How to import synchronized lyrics")
                        }
                        if (lyricsGuideOpen) {
                            Surface(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 10.dp),
                                color = colors.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text("TIMESTAMPED LRC · QUICK GUIDE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary)
                                    Text("[00:12.50] First lyric line",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurface)
                                    Text("[00:15.25] Next lyric line",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurface)
                                    Text("Choose a UTF-8 .lrc file under 64 KB. " +
                                        "NightWave reviews song-name hints, timestamp problems " +
                                        "and an optional replacement before saving.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant)
                                    Text("LRC files must be supplied by you. " +
                                        "Matching filenames do not prove matching lyrics.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant)
                                }
                            }
                        }
                    }
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
                        val active = LyricsSyncPolicy.activeIndex(
                            lyrics, state.positionMs, lyricsOffsetMs)
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
                        OutlinedButton(onClick = { lyricsImmersive = true },
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Open full-screen synchronized lyrics")
                        }
                        Text("Lyric timing · " + LyricsSyncPolicy.label(lyricsOffsetMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(9.dp))
                        OutlinedButton(onClick = onEditLyrics,
                            modifier = Modifier.fillMaxWidth()
                                .testTag("lyrics-open-editor")) {
                            Text("Edit lyric lines & timestamps")
                        }
                        Spacer(Modifier.height(7.dp))
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            OutlinedButton(onClick = onImportLyrics,
                                modifier = Modifier.weight(1f)) { Text("Replace .lrc") }
                            OutlinedButton(onClick = onRemoveLyrics,
                                modifier = Modifier.weight(1f)) { Text("Remove lyrics") }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = { advancedAudioOpen = !advancedAudioOpen },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = colors.surfaceVariant.copy(alpha = .58f)
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("Sound & playback", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface)
                            Text("Crossfade · Sleep timer · Listening level",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant)
                        }
                        Text(if (advancedAudioOpen) "Less" else "More",
                            color = colors.primary,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (advancedAudioOpen) {
                Spacer(Modifier.height(11.dp))
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { listeningOptionsOpen = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Listening level · " +
                            ListeningLevelPolicy.label(listeningLevelPercent))
                    }
                    DropdownMenu(
                        expanded = listeningOptionsOpen,
                        onDismissRequest = { listeningOptionsOpen = false }
                    ) {
                        ListeningLevelPolicy.presetsPercent.forEach { percent ->
                            DropdownMenuItem(
                                text = { Text(ListeningLevelPolicy.label(percent)) },
                                onClick = {
                                    listeningOptionsOpen = false
                                    onListeningLevel(percent)
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(11.dp))
                OutlinedButton(
                    onClick = {
                        crossfadeDraft = crossfadeSeconds
                        crossfadeOptionsOpen = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (crossfadeSeconds == 0) "Crossfade · Off"
                        else "Crossfade · " + crossfadeSeconds + " sec")
                }
                Text(AudioTransitionUiPolicy.description(
                    offline = state.offline,
                    crossfadeSeconds = crossfadeSeconds,
                    shuffled = state.shuffle,
                    repeatOne = state.repeat == PlaybackQueue.Repeat.ONE,
                    normalSpeed = offlineSpeedPercent ==
                        OfflinePlaybackSpeedPolicy.NORMAL_PERCENT
                ), modifier = Modifier.fillMaxWidth().padding(
                    horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
                if (crossfadeOptionsOpen) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { crossfadeOptionsOpen = false },
                        title = { Text("Crossfade") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Overlap two locally imported songs. Streaming tracks use normal playback.",
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(AudioTransitionUiPolicy.dialogDetail(crossfadeDraft),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary)
                                Slider(
                                    value = crossfadeDraft.toFloat(),
                                    onValueChange = { crossfadeDraft =
                                        it.roundToInt().coerceIn(0, CrossfadePolicy.MAX_SECONDS) },
                                    valueRange = 0f..CrossfadePolicy.MAX_SECONDS.toFloat(),
                                    steps = CrossfadePolicy.MAX_SECONDS - 1,
                                    modifier = Modifier.testTag("crossfade-seconds-slider")
                                )
                                Text("Off by default. Applies only to eligible offline queues at 1× speed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                crossfadeOptionsOpen = false
                                onCrossfade(crossfadeDraft)
                            }) { Text("Apply") }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { crossfadeOptionsOpen = false }
                            ) { Text("Cancel") }
                        }
                    )
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
                }
                if (sleepOptionsOpen) {
                    NightWaveSleepTimerDialog(
                        remainingMs = sleepRemainingMs,
                        onDismiss = { sleepOptionsOpen = false },
                        onSet = { minutes ->
                            sleepOptionsOpen = false
                            onSleepTimer(minutes)
                        },
                        onCancel = {
                            sleepOptionsOpen = false
                            onSleepTimer(null)
                        }
                    )
                }

                if (state.offline) {
                    Spacer(Modifier.height(11.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Volume balance · " + if (normalizationEnabled) "On" else "Off",
                                    style = MaterialTheme.typography.titleSmall)
                                Text("Analyzes imported music locally and reduces loud tracks. No boosts.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = normalizationEnabled,
                                onCheckedChange = onNormalizationChange,
                                modifier = Modifier.testTag("offline-volume-normalization-switch")
                            )
                        }
                    }
                    Spacer(Modifier.height(11.dp))
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { speedOptionsOpen = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Offline playback speed · " +
                                OfflinePlaybackSpeedPolicy.label(offlineSpeedPercent))
                        }
                        DropdownMenu(
                            expanded = speedOptionsOpen,
                            onDismissRequest = { speedOptionsOpen = false }
                        ) {
                            OfflinePlaybackSpeedPolicy.presetsPercent.forEach { percent ->
                                DropdownMenuItem(
                                    text = { Text(OfflinePlaybackSpeedPolicy.label(percent)) },
                                    onClick = {
                                        speedOptionsOpen = false
                                        onOfflineSpeed(percent)
                                    }
                                )
                            }
                        }
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
            Text("Your listening queue", style = MaterialTheme.typography.headlineSmall)
            val removable = !state.offline && !state.shuffle && state.connected
            val currentIndex = state.queueIndex.takeIf { it in state.queue.indices }
            val pendingUrls = state.manualNextUrls
            val explicitNext = state.queue.withIndex().filter { slot ->
                slot.index != currentIndex && slot.value.url in pendingUrls
            }.sortedBy { pendingUrls.indexOf(it.value.url) }
            val explicitUrls = explicitNext.map { it.value.url }.toSet()
            val upcomingPool = state.queue.withIndex().filter { slot ->
                slot.value.url !in explicitUrls &&
                    (if (state.shuffle) slot.index != currentIndex
                     else currentIndex != null && slot.index > currentIndex)
            }
            val upcoming = explicitNext + upcomingPool
            val earlier = if (state.shuffle || currentIndex == null) emptyList()
                else state.queue.withIndex().filter { it.index < currentIndex }
            Text(
                when {
                    state.offline -> "Offline collection · private audio files stay on device"
                    state.shuffle && explicitNext.isNotEmpty() ->
                        "${explicitNext.size} Play Next choices · remaining tracks shuffled"
                    state.shuffle -> "Shuffle on · order varies until you choose Play next"
                    upcoming.isEmpty() -> "No tracks after this song · choose Play next to add one"
                    else -> "${upcoming.size} upcoming · swipe left to remove a queued track"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(13.dp))
            if (state.queue.isEmpty() || currentIndex == null) {
                EmptyMusicCard("Your queue is empty", "Play a song to begin listening.",
                    "Close queue", onDismiss)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    item(key = "queue-playing-header") {
                        Text("PLAYING NOW", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    item(key = "queue-playing-item") {
                        val playing = state.queue[currentIndex]
                        SwipeUpcomingRow(
                            number = currentIndex + 1, title = playing.title,
                            subtitle = playing.author, artwork = playing.thumbnail,
                            isPlaying = true, canRemove = false, reduceMotion = reduceMotion,
                            onSelect = { onSelect(currentIndex) }, onRemove = {}
                        )
                    }
                    item(key = "queue-upcoming-header") {
                        Text(if (explicitNext.isNotEmpty()) "PLAY NEXT"
                            else if (state.shuffle) "SHUFFLE POOL · ORDER VARIES"
                            else "UP NEXT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp))
                    }
                    if (upcoming.isEmpty()) {
                        item(key = "queue-no-upcoming") {
                            Text("Nothing else in your queue.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // Absolute slot keys never collide even when a restored queue
                    // contains duplicate public URLs.
                    itemsIndexed(upcoming, key = { _, slot -> "next-${slot.index}" }) { pos, slot ->
                        val index = slot.index
                        val video = slot.value
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            if (explicitNext.isNotEmpty() && pos == explicitNext.size) {
                                Text(if (state.shuffle) "SHUFFLE POOL · ORDER VARIES"
                                    else "UP NEXT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 9.dp))
                            }
                            SwipeUpcomingRow(
                                number = index + 1,
                                title = video.title,
                                subtitle = video.author,
                                artwork = video.thumbnail,
                                isPlaying = false,
                                canRemove = removable &&
                                    video.url.startsWith("https://www.youtube.com/watch?v="),
                                reduceMotion = reduceMotion,
                                onSelect = { onSelect(index) },
                                onRemove = { requestRemove(index, video.url) }
                            )
                        }
                    }
                    if (earlier.isNotEmpty()) {
                        item(key = "queue-earlier-header") {
                            Text("EARLIER IN THIS QUEUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp))
                        }
                        itemsIndexed(earlier, key = { _, slot -> "earlier-${slot.index}" }) { _, slot ->
                            val video = slot.value
                            SwipeUpcomingRow(
                                number = slot.index + 1, title = video.title,
                                subtitle = video.author, artwork = video.thumbnail,
                                isPlaying = false, canRemove = false,
                                reduceMotion = reduceMotion,
                                onSelect = { onSelect(slot.index) }, onRemove = {}
                            )
                        }
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
    val display = remember(title, subtitle) {
        MusicIdentity.display(title, subtitle)
    }
    var travel by remember(title, number) { mutableStateOf(0f) }
    val animatedTravel by animateFloatAsState(
        targetValue = travel,
        animationSpec = tween(durationMillis = NightWaveMotionPolicy.duration(
            reduceMotion, NightWaveMotionPolicy.FEEDBACK_MS)),
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
                Text(display.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall)
                Text(display.artistOrCreator, maxLines = 1, overflow = TextOverflow.Ellipsis,
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
