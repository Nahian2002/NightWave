package com.nightwave.player.v2.ui

import android.view.HapticFeedbackConstants

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.layout.heightIn
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
import com.nightwave.player.v2.ui.NightWaveMatteIconButton as IconButton
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
import com.nightwave.player.v2.ui.NightWaveMatteButton as Button
import androidx.compose.material3.ButtonDefaults
import com.nightwave.player.v2.ui.NightWaveMatteOutlinedButton as OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
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
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Refresh
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.playback.StudioPlayerActions
import com.nightwave.player.v2.playback.StudioPlayerState
import com.nightwave.player.v2.playback.PlaybackHealthPolicy
import com.nightwave.player.v2.playback.PlaybackPhase
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.v2.lyrics.TimedLyrics
import com.nightwave.player.v2.lyrics.LyricsTrackBindingPolicy
import com.nightwave.player.v2.lyrics.LyricsSyncPolicy
import com.nightwave.player.v2.lyrics.LyricsPresentationPolicy
import com.nightwave.player.v2.lyrics.LyricsReadingPolicy
import com.nightwave.player.playback.PlaybackQueue
import com.nightwave.player.playback.QueueEditSnapshot
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
    val colors = MaterialTheme.colorScheme
    val health = PlaybackHealthPolicy.classify(state.hasTrack, state.connected,
        state.offline, state.playing, state.buffering, state.resolving, state.error)
    val statusLabel = NightWaveNowPlayingPresentationPolicy.statusLabel(health)
    val identity = NightWaveEditorialPresentationPolicy.identity(state.title, state.artist)
    val reducedMotion = LocalNightWaveReduceMotion.current
    val hapticView = LocalView.current
    val miniShape = RoundedCornerShape(NightWavePremiumDesignTokens.ARTWORK_CORNER_DP.dp)
    val miniScale by animateFloatAsState(
        targetValue = NightWaveMotionPolicy.artworkScale(state.playing, reducedMotion),
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = .78f, stiffness = 500f),
        label = "Mini player artwork motion"
    )
    var travel by remember(state.videoUrl) { mutableStateOf(0f) }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = NightWavePremiumDesignTokens.pageGutterDp(
            androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()).dp, vertical = 3.dp)
            .testTag("nightwave-mini-player")
            .semantics { stateDescription = statusLabel }
            .clip(miniShape)
            .background(colors.surfaceVariant)
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
            val miniLayout = NightWaveMiniPlayerLayoutPolicy.resolve(
                widthDp = maxWidth.value, fontScale = fontScale,
                queueCount = state.queue.size)
            val showNext = miniLayout.showNext
            Row(
                modifier = Modifier.fillMaxWidth().height(miniLayout.rowHeightDp.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(miniLayout.labelGapDp.dp)
                ) {
                    if (miniLayout.showArtwork) {
                        val currentArtwork = remember(state.videoUrl, state.thumbnail) {
                            NightWaveArtworkTransitionPolicy.frame(
                                state.videoUrl, state.thumbnail)
                        }
                        if (reducedMotion) {
                            // Direct composition avoids even a zero-duration
                            // overlap of outgoing artwork for Reduce Motion.
                            CoverArt(state.thumbnail, Modifier.size(miniLayout.artworkDp.dp),
                                11, "Artwork for " + identity.title, trackIdentity = state.videoUrl)
                        } else {
                            Crossfade(
                                targetState = currentArtwork,
                                animationSpec = tween(
                                    durationMillis = NightWaveMotionPolicy.COVER_MS),
                                label = "Mini player changing cover"
                            ) { artwork ->
                                CoverArt(artwork.thumbnail,
                                    Modifier.size(miniLayout.artworkDp.dp).graphicsLayer {
                                        scaleX = miniScale
                                        scaleY = miniScale
                                    }, 11, NightWaveArtworkTransitionPolicy
                                        .accessibilityDescription(
                                            artwork, currentArtwork, identity.title), trackIdentity = artwork.trackId)
                            }
                        }
                    }
                    Column(Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(if (state.title.isBlank()) "Restoring your last track…"
                            else identity.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold),
                            color = colors.onSurface,
                            maxLines = miniLayout.titleLines,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            if (health.phase == PlaybackPhase.LOOKING_UP ||
                                health.phase == PlaybackPhase.BUFFERING ||
                                health.phase == PlaybackPhase.DISCONNECTED ||
                                health.phase == PlaybackPhase.ERROR) statusLabel
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
                    Icon(if (state.canPause) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.canPause) "Pause playback" else "Resume playback",
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
            trackColor = colors.onSurface.copy(alpha = .15f)
        )
    }
}

@Composable
internal fun CoverArt(
    url: String, modifier: Modifier = Modifier, radius: Int = 24,
    description: String? = "Song artwork", trackIdentity: String = "", large: Boolean = false,
    onResolved: ((android.graphics.drawable.Drawable) -> Unit)? = null
) {
    val context = LocalContext.current
    val artworkColors = MaterialTheme.colorScheme
    val candidates = remember(url, large) { NightWaveArtworkPolicy.requestCandidates(url, large) }
    var candidateIndex by remember(trackIdentity, url, large) { mutableIntStateOf(0) }
    var failed by remember(trackIdentity, url, large) { mutableStateOf(false) }
    var imageLoaded by remember(trackIdentity, url, large, candidateIndex) { mutableStateOf(false) }
    var pixels by remember { mutableIntStateOf(0) }
    val attempt = candidateIndex
    val displayUrl = candidates.getOrNull(attempt)
    val shape = RoundedCornerShape(radius.dp)
    Box(modifier.onSizeChanged { pixels = NightWaveArtworkPolicy.requestPixels(it.width, it.height) }
        .clip(shape).background(artworkColors.surfaceVariant)
        .semantics {
            description?.let { contentDescription = if (failed || displayUrl == null)
                "$it · image unavailable" else it }
        }, contentAlignment = Alignment.Center) {
        if (!imageLoaded) Canvas(Modifier.fillMaxSize(.64f)) {
            drawCircle(artworkColors.onSurface.copy(alpha = .12f), radius = size.minDimension * .44f)
            drawCircle(artworkColors.onSurface.copy(alpha = .32f), radius = size.minDimension * .33f,
                style = Stroke(width = 2.dp.toPx()))
            drawCircle(artworkColors.onSurface.copy(alpha = .5f), radius = size.minDimension * .09f)
        }
        if (!failed && displayUrl != null && pixels > 0) {
            val request = remember(displayUrl, pixels, context) {
                ImageRequest.Builder(context).data(displayUrl).size(pixels, pixels)
                    .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED)
                    .allowHardware(!large).crossfade(false).build()
            }
            val advance = {
                if (candidateIndex == attempt) {
                    if (attempt + 1 < candidates.size) candidateIndex = attempt + 1 else failed = true
                }
            }
            AsyncImage(model = request, contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (large) ContentScale.Fit else ContentScale.Crop,
                onError = { advance() },
                onSuccess = { loaded ->
                    if (NightWaveArtworkPolicy.isUnavailableLargeStill(displayUrl,
                            loaded.result.drawable.intrinsicWidth, loaded.result.drawable.intrinsicHeight)) advance()
                    else if (candidateIndex == attempt) {
                        imageLoaded = true
                        onResolved?.invoke(loaded.result.drawable)
                    }
                })
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
    // One transition owns backdrop and content from the same requested frame.
    // Translation alone keeps the player visible throughout; no blank alpha-zero viewport.
    val enter = if (reduceMotion) EnterTransition.None else
        slideInVertically(
            animationSpec = tween(durationMillis = NightWaveMotionPolicy.PLAYER_ENTER_MS),
            initialOffsetY = NightWaveMotionPolicy::playerEntryOffsetPx
        )
    val exit = if (reduceMotion) ExitTransition.None else
        slideOutVertically(
            animationSpec = tween(durationMillis = NightWaveMotionPolicy.PLAYER_EXIT_MS),
            targetOffsetY = NightWaveMotionPolicy::playerExitOffsetPx
        )
    val overlayVisibility = updateTransition(targetState = expanded, label = "player-overlay")
    // NW58: retain the solid backdrop for the same transition lifetime,
    // including an enter reversed before its current state becomes expanded.
    // It disappears with the completed exit, never one effect/frame earlier.
    if (NightWavePlayerOverlayPolicy.retainBackdrop(
            expanded, overlayVisibility.currentState, overlayVisibility.targetState, overlayVisibility.isRunning)) {
        Box(Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("nw58-opaque-player-backdrop"))
    }
    overlayVisibility.AnimatedVisibility(
        visible = { it },
        enter = enter,
        exit = exit
    ) {
        val colors = MaterialTheme.colorScheme.copy(onSurfaceVariant =
            paletteColor(NightWaveMattePolicy.playerSecondary(LocalNightWavePalette.current)))
        var showSourceDetails by remember(state.videoUrl) { mutableStateOf(false) }
        var resolvedArtwork by remember(state.videoUrl, state.thumbnail) {
            mutableStateOf<android.graphics.drawable.Drawable?>(null)
        }
        if (showSourceDetails) NightWaveTrackDetails(state.title, state.artist, state.videoUrl) {
            showSourceDetails = false
        }
        BoxWithConstraints(Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(colors.background)) {
            NightWaveArtworkBackdrop(resolvedArtwork, Modifier.fillMaxSize())
            val fontScale = LocalDensity.current.fontScale
            // Capture BoxWithConstraints width before entering nested Compose scopes.
            // Inner Column/Row scopes cannot implicitly access the outer receiver.
            val playerWidthDp = maxWidth.value
            val playerLayout = NightWaveNowPlayingLayoutPolicy.resolve(
                playerWidthDp, maxHeight.value, fontScale)
            val contentPaddingDp = playerLayout.horizontalPaddingDp
            val art = playerLayout.artworkDp.dp
            val artHeight = NightWaveArtworkFramePolicy.heightDp(playerLayout.artworkDp,
                resolvedArtwork?.intrinsicWidth ?: 0, resolvedArtwork?.intrinsicHeight ?: 0, state.offline).dp
            val playerIdentity = remember(state.title, state.artist) {
                NightWaveEditorialPresentationPolicy.identity(state.title, state.artist)
            }
            val queuePreview = remember(
                state.queue, state.queueIndex, state.manualNextUrls,
                state.shuffle, state.repeat
            ) {
                NightWaveNowPlayingPresentationPolicy.queuePreview(
                    urls = state.queue.map { it.url },
                    titles = state.queue.map { it.title },
                    currentIndex = state.queueIndex,
                    manualNextUrls = state.manualNextUrls,
                    shuffled = state.shuffle,
                    repeatOne = state.repeat == PlaybackQueue.Repeat.ONE,
                    repeatAll = state.repeat == PlaybackQueue.Repeat.ALL
                )
            }
            val queuePreviewTitle = queuePreview.sourceSlot?.let { at ->
                state.queue.getOrNull(at)?.let {
                    NightWaveEditorialPresentationPolicy.identity(it.title, it.author).title
                }
            } ?: queuePreview.subtitle
            val playerHealth = PlaybackHealthPolicy.classify(state.hasTrack, state.connected,
                state.offline, state.playing, state.buffering, state.resolving, state.error)
            val playerStatus = NightWaveNowPlayingPresentationPolicy.statusLabel(playerHealth)
            val canSwipeArtwork = state.connected && !state.buffering && state.queue.size > 1
            var horizontalTravel by remember(state.videoUrl) { mutableStateOf(0f) }
            val playingArtworkScale by animateFloatAsState(
                targetValue = NightWaveMotionPolicy.artworkScale(
                    state.playing, reduceMotion),
                animationSpec = if (reduceMotion) snap() else spring(
                    dampingRatio = .78f, stiffness = 310f),
                label = "Now Playing artwork spring"
            )
            val lyricsSupported = LyricsTrackBindingPolicy.storageId(state.videoUrl) != null
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
            if (lyricsImmersive && lyricsSupported && lyrics != null) {
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
                        // Large seek/skip jumps should not animate through up
                        // to 1,200 lazy-list rows and stall low-end devices.
                        if (LyricsReadingPolicy.shouldAnimateScroll(
                                lyricListState.firstVisibleItemIndex, target, reduceMotion))
                            lyricListState.animateScrollToItem(target)
                        else lyricListState.scrollToItem(target)
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
                            Text(playerIdentity.title,
                                maxLines = if (fontScale >= 1.3f) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium)
                            Text(playerIdentity.artistOrCreator,
                                maxLines = if (fontScale >= 1.8f) 2 else 1,
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
                        Text(LyricsReadingPolicy.followHint(lyricsAutoFollow),
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall)
                        // NW107: TalkBack users can deliberately pause automatic
                        // scrolling instead of having to swipe to disable it.
                        // Rejoin uses the existing active-cue LaunchedEffect.
                        OutlinedButton(
                            onClick = {
                                lyricsAutoFollow =
                                    LyricsReadingPolicy.toggledFollow(lyricsAutoFollow)
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                                .testTag("lyrics-follow-toggle")
                                .semantics {
                                    contentDescription =
                                        LyricsReadingPolicy.followActionDescription(
                                            lyricsAutoFollow)
                                    stateDescription =
                                        LyricsReadingPolicy.followStateDescription(
                                            lyricsAutoFollow)
                                }
                        ) {
                            Text(LyricsReadingPolicy.followActionLabel(lyricsAutoFollow))
                        }
                        Spacer(Modifier.width(7.dp))
                        OutlinedButton(onClick = onEditLyrics,
                            modifier = Modifier.heightIn(min = 48.dp)) {
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
                                    .heightIn(min = 56.dp)
                                    .testTag("lyrics-cue-$index")
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = LyricsReadingPolicy.cueDescription(
                                            cue, lyricsOffsetMs, state.durationMs)
                                        stateDescription =
                                            LyricsReadingPolicy.currentLineState(current)
                                    },
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
                        tonalElevation = 0.dp
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
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("nw133-player-content")
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
                        Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = "Save track to playlist",
                            tint = colors.onBackground)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier.size(width = art, height = artHeight)
                        .testTag("nightwave-now-playing-artwork")
                        .shadow(8.dp,
                            RoundedCornerShape(playerLayout.artworkCornerDp.dp))
                        .graphicsLayer {
                            scaleX = playingArtworkScale
                            scaleY = playingArtworkScale
                        }
                        .clip(RoundedCornerShape(playerLayout.artworkCornerDp.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp,
                            colors.onBackground.copy(alpha = NightWaveMattePolicy.BORDER_ALPHA),
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
                    val currentArtwork = remember(state.videoUrl, state.thumbnail) {
                        NightWaveArtworkTransitionPolicy.frame(
                            state.videoUrl, state.thumbnail)
                    }
                    if (reduceMotion) {
                        CoverArt(state.thumbnail, Modifier.fillMaxSize(),
                            playerLayout.artworkCornerDp,
                            "Artwork for " + playerIdentity.title, trackIdentity = state.videoUrl, large = true,
                            onResolved = { resolvedArtwork = it })
                    } else {
                        Crossfade(
                            targetState = currentArtwork,
                            animationSpec = tween(
                                durationMillis = NightWaveMotionPolicy.COVER_MS),
                            label = "Player artwork"
                        ) { artwork ->
                            CoverArt(artwork.thumbnail, Modifier.fillMaxSize(),
                                playerLayout.artworkCornerDp,
                                NightWaveArtworkTransitionPolicy.accessibilityDescription(
                                    artwork, currentArtwork, playerIdentity.title), trackIdentity = artwork.trackId, large = true,
                                onResolved = if (artwork.trackId == state.videoUrl && artwork.thumbnail == state.thumbnail)
                                    { image -> resolvedArtwork = image } else null)
                        }
                    }
                    if (state.buffering) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape)
                                .background(colors.background.copy(alpha = .76f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (reduceMotion) {
                                // A genuinely static buffering cue; the
                                // indeterminate spinner itself is animation.
                                Text("Loading",
                                    modifier = Modifier.testTag(
                                        "nightwave-now-playing-static-loading"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onBackground)
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(29.dp),
                                    color = colors.primary, strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
                // Don't advertise the source type as a prominent pill on
                // every Now Playing screen. Keep only a useful gesture hint.
                if (canSwipeArtwork) {
                    Text(NightWaveNowPlayingPresentationPolicy.artworkGestureHint(
                            canSwipeArtwork).orEmpty(),
                        modifier = Modifier.padding(top = 9.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = .85f))
                }
                Spacer(Modifier.height(if (playerLayout.compact) 13.dp else 16.dp))
                // NW116: let long original/Bangla titles use the full width
                // on split-screen and large fonts. Normal phones retain the
                // existing Like-on-the-right layout and visual hierarchy.
                val fullWidthMetadata = NightWaveNowPlayingPresentationPolicy
                    .useFullWidthMetadata(playerWidthDp, fontScale)
                val songMetadata: @Composable (Modifier) -> Unit = { detailModifier ->
                    Column(detailModifier) {
                        Text(if (state.title.isBlank()) "Choose a track"
                            else playerIdentity.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onBackground,
                            maxLines = NightWaveNowPlayingPresentationPolicy.titleLines(
                                fontScale, playerWidthDp),
                            overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        Text(playerIdentity.artistOrCreator,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                            maxLines = NightWaveNowPlayingPresentationPolicy.artistLines(fontScale),
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                val likeStatus = NightWaveNowPlayingPresentationPolicy.likeStatusLabel(
                    liked, state.offline)
                val likeControl: @Composable () -> Unit = {
                    IconButton(onClick = onLike, enabled = !state.offline,
                        modifier = Modifier.size(48.dp)
                            .testTag("nightwave-now-playing-like-action")
                            .semantics { stateDescription = likeStatus
                        }) {
                        Icon(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (liked) "Unlike song" else "Like song",
                            tint = if (liked) colors.primary else colors.onBackground)
                    }
                }
                if (fullWidthMetadata) {
                    Column(Modifier.fillMaxWidth()) {
                        songMetadata(Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End) {
                            Text(likeStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                                    .testTag("nightwave-now-playing-like-status"))
                            likeControl()
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically) {
                        songMetadata(Modifier.weight(1f))
                        likeControl()
                    }
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("nw133-player-status"),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(playerStatus, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (playerHealth.needsUserAction) colors.error else colors.onSurfaceVariant)
                    if (playerHealth.needsUserAction) {
                        IconButton(onClick = actions::retry, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Outlined.Refresh, contentDescription = if (state.connected)
                                "Retry playback" else "Reconnect player", tint = colors.primary)
                        }
                    }
                    NightWaveMatteTextButton(onClick = { showSourceDetails = true },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("editorial-track-details")) { Text("Details") }
                }
                // A scrub in progress belongs to exactly one track. Changing tracks
                // clears the pending drag so an old percentage cannot seek the next.
                var dragFraction by remember(state.videoUrl) { mutableStateOf<Float?>(null) }
                val fraction = dragFraction ?: PlayerProgressRules.fraction(
                    state.positionMs, state.durationMs)
                NightWaveSeekTimeline(
                    trackIdentity = state.videoUrl,
                    fraction = fraction,
                    enabled = NightWaveNowPlayingPresentationPolicy.canSeek(state.videoUrl,
                        state.connected, state.durationMs, state.resolving, state.error),
                    onPreview = { dragFraction = it },
                    onCommit = { originatingTrack, requested ->
                        NightWaveNowPlayingPresentationPolicy.seekPosition(originatingTrack,
                            state.videoUrl, state.connected, state.durationMs, state.resolving, state.error, requested)
                            ?.let { actions.seekToForTrack(originatingTrack, it) }
                        dragFraction = null
                    },
                    onCancel = { dragFraction = null },
                    positionDescription = timeLabel(PlayerProgressRules.requestedPosition(
                        state.durationMs, fraction) ?: 0L) + " of " + timeLabel(state.durationMs)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(timeLabel(PlayerProgressRules.requestedPosition(
                        state.durationMs, fraction) ?: 0L), style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant)
                    Text(timeLabel(state.durationMs), style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(9.dp))
                // NW116: retain the familiar five-control bar on regular
                // phones, but move Shuffle/Repeat to a secondary row when
                // split-screen width or large text would crowd the 48dp taps.
                // Both layouts call the SAME actions and retain accessibility.
                val shuffleControl: @Composable () -> Unit = {
                    IconButton(onClick = actions::toggleShuffle,
                        modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Shuffle,
                            contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off",
                            modifier = Modifier.size(25.dp),
                            tint = if (state.shuffle) colors.primary else colors.onSurfaceVariant)
                    }
                }
                val repeatControl: @Composable () -> Unit = {
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
                // A physical 48dp tap target must not be squeezed into
                // a narrower row. Preserve every transport action even when
                // Android presents an ultra-narrow multiwindow viewport.
                val previousControl: @Composable () -> Unit = {
                    IconButton(onClick = actions::previous, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous track",
                            modifier = Modifier.size(34.dp), tint = colors.onBackground)
                    }
                }
                val playControl: @Composable () -> Unit = {
                    NightWaveMattePlayButton(onClick = actions::playPause,
                        modifier = Modifier.size(if (playerLayout.compact) 64.dp else 72.dp)
                            .testTag("nightwave-now-playing-play-pause")) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (state.canPause) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.canPause) "Pause" else "Play",
                                tint = paletteColor(NightWaveMattePolicy.transportInk(LocalNightWavePalette.current)),
                                modifier = Modifier.size(33.dp))
                        }
                    }
                }
                val nextControl: @Composable () -> Unit = {
                    IconButton(onClick = actions::next, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.SkipNext, contentDescription = "Next track",
                            modifier = Modifier.size(34.dp), tint = colors.onBackground)
                    }
                }
                if (playerLayout.stackPrimaryTransportControls) {
                    Column(Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        previousControl()
                        playControl()
                        nextControl()
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (playerLayout.splitTransportControls)
                            Arrangement.SpaceEvenly else Arrangement.SpaceBetween) {
                        if (!playerLayout.splitTransportControls) shuffleControl()
                        previousControl()
                        playControl()
                        nextControl()
                        if (!playerLayout.splitTransportControls) repeatControl()
                    }
                }
                if (playerLayout.splitTransportControls) {
                    // Never hide Shuffle/Repeat; each action keeps its 48dp
                    // surface even when the two no longer fit side-by-side.
                    if (playerLayout.stackSecondaryTransportControls) {
                        Column(Modifier.fillMaxWidth().padding(top = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            shuffleControl()
                            repeatControl()
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(top = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            shuffleControl()
                            repeatControl()
                        }
                    }
                }
                // NW116: compact split windows and large system fonts give
                // each action the full width rather than clipping its caption.
                // The default phone still uses the original two-card toolbar.
                val offlineAction: @Composable (Modifier) -> Unit = { buttonModifier ->
                    Surface(
                        onClick = onSaveOffline,
                        enabled = state.hasTrack,
                        modifier = buttonModifier.heightIn(min = 64.dp)
                            .testTag("nightwave-now-playing-offline-action"),
                        shape = RoundedCornerShape(
                            NightWavePremiumDesignTokens.CARD_CORNER_DP.dp),
                        color = colors.surfaceVariant.copy(alpha = .74f)
                    ) {
                        Box(Modifier.fillMaxWidth().padding(
                            horizontal = 8.dp, vertical = 15.dp),
                            contentAlignment = Alignment.Center) {
                            Text(
                                NightWaveNowPlayingPresentationPolicy.offlineActionLabel(
                                    state.hasTrack, state.offline),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                color = colors.onSurface)
                        }
                    }
                }
                val lyricsAction: @Composable (Modifier) -> Unit = { buttonModifier ->
                    Surface(
                        onClick = { lyricsOpen = !lyricsOpen },
                        modifier = buttonModifier.heightIn(min = 64.dp)
                            .testTag("nightwave-now-playing-lyrics-action"),
                        shape = RoundedCornerShape(
                            NightWavePremiumDesignTokens.CARD_CORNER_DP.dp),
                        color = if (lyricsOpen) colors.primaryContainer else colors.surface,
                        tonalElevation = 0.dp
                    ) {
                        Column(Modifier.fillMaxWidth().padding(
                            horizontal = 12.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(if (lyricsOpen) "Hide lyrics" else "Lyrics",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = colors.onSurface)
                            Text(NightWaveNowPlayingPresentationPolicy.lyricsSubtitle(
                                state.offline, lyrics != null),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                color = colors.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (playerLayout.stackQuickActions) {
                    Column(Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        offlineAction(Modifier.fillMaxWidth())
                        lyricsAction(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top) {
                        offlineAction(Modifier.weight(1f))
                        lyricsAction(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = onOpenQueue,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                        .testTag("nightwave-now-playing-queue-action"),
                    shape = RoundedCornerShape(
                        NightWavePremiumDesignTokens.CARD_CORNER_DP.dp),
                    color = colors.surface,
                    tonalElevation = 0.dp
                ) {
                    Row(Modifier.fillMaxWidth().padding(
                        horizontal = 17.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(queuePreview.heading,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant)
                            if (reduceMotion) {
                                // No previous title layer survives even one
                                // instant frame when motion is reduced.
                                Text(queuePreviewTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = colors.onSurface)
                            } else {
                                Crossfade(
                                    targetState = queuePreviewTitle,
                                    animationSpec = tween(NightWaveMotionPolicy.FEEDBACK_MS),
                                    label = "Next queued song"
                                ) { title ->
                                    Text(title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        color = colors.onSurface)
                                }
                            }
                        }
                        Icon(Icons.AutoMirrored.Outlined.QueueMusic,
                            contentDescription = "Open queue", tint = colors.primary)
                    }
                }
                if (lyricsOpen) {
                    Spacer(Modifier.height(10.dp))
                    if (lyricsSupported) {
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
                    if (!lyricsSupported) {
                        Text("Choose a local song or supported public video to attach your own LRC. " +
                            "NightWave does not fetch lyrics from streaming sites.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant)
                    } else if (lyrics == null) {
                        Text(if (state.offline) "Import your own timestamped .lrc file for this on-device copy."
                            else "Attach your own timestamped LRC to this exact video. Public-video lyrics stay on this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onImportLyrics,
                            modifier = Modifier.fillMaxWidth()) { Text("Import synchronized lyrics") }
                    } else {
                        val active = LyricsSyncPolicy.activeIndex(
                            lyrics, state.positionMs, lyricsOffsetMs)
                        val start = (active - 2).coerceAtLeast(0)
                        Text(if (state.offline) "Your lyrics · This on-device copy" else "Your lyrics · This video",
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
                state.error?.takeIf { it.isNotBlank() }?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.error)
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
    reduceMotion: Boolean,
    onMoveUpcoming: ((Int, Int, QueueEditSnapshot) -> Boolean)? = null,
    onClearUpcoming: ((QueueEditSnapshot) -> Boolean)? = null
) {
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editingOrder by remember { mutableStateOf(false) }
    var clearRequest by remember { mutableStateOf<QueueEditSnapshot?>(null) }
    val editSnapshot = QueueEditSnapshot(state.queue.map { it.url }, state.videoUrl, state.manualNextUrls)
    fun move(from: Int, to: Int) {
        if (onMoveUpcoming?.invoke(from, to, editSnapshot) != true) scope.launch {
            snackbarHost.showSnackbar("Queue changed. Reopen it and try again.")
        }
    }
    clearRequest?.let { reviewed ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { clearRequest = null },
            title = { Text("Clear Up Next?") },
            text = { Text("Remove the upcoming songs from this queue. Your current song, saved music and files stay in place.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    clearRequest = null
                    if (onClearUpcoming?.invoke(reviewed) != true) scope.launch {
                        snackbarHost.showSnackbar("Queue changed. Review it before clearing.")
                    }
                }, modifier = Modifier.heightIn(min = 48.dp).testTag("nw134-confirm-clear")) { Text("Clear Up Next") }
            }, dismissButton = {
                androidx.compose.material3.TextButton(onClick = { clearRequest = null },
                    modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
            })
    }
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
            if (removable && upcoming.isNotEmpty()) {
                NightWaveCompactActions {
                    if (onMoveUpcoming != null && upcoming.size > 1) NightWaveMatteTextButton(
                        onClick = { editingOrder = !editingOrder },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("nw134-edit-order")) {
                        Text(if (editingOrder) "Done editing" else "Edit order")
                    }
                    if (onClearUpcoming != null) NightWaveMatteTextButton(
                        onClick = { clearRequest = editSnapshot },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("nw134-clear-upcoming")) { Text("Clear Up Next") }
                }
                if (editingOrder) Text("Use song options to move within Play Next or Up Next.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (!state.offline && state.shuffle) {
                Text("Turn Shuffle off to reorder, remove or clear upcoming songs.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
                                onRemove = { requestRemove(index, video.url) },
                                onMoveEarlier = if (editingOrder && removable && pos > 0 &&
                                    ((video.url in pendingUrls) == (upcoming[pos - 1].value.url in pendingUrls)))
                                    ({ move(index, upcoming[pos - 1].index) }) else null,
                                onMoveLater = if (editingOrder && removable && pos < upcoming.lastIndex &&
                                    ((video.url in pendingUrls) == (upcoming[pos + 1].value.url in pendingUrls)))
                                    ({ move(index, upcoming[pos + 1].index) }) else null
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
    onRemove: () -> Unit,
    onMoveEarlier: (() -> Unit)? = null,
    onMoveLater: (() -> Unit)? = null
) {
    val display = remember(title, subtitle) {
        NightWaveEditorialPresentationPolicy.identity(title, subtitle)
    }
    val fontScale = LocalDensity.current.fontScale
    val titleLines = NightWavePremiumDesignTokens.titleLines(fontScale,
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat())
    var travel by remember(title, number) { mutableStateOf(0f) }
    val animatedTravel by animateFloatAsState(
        targetValue = travel,
        animationSpec = tween(durationMillis = NightWaveMotionPolicy.duration(
            reduceMotion, NightWaveMotionPolicy.FEEDBACK_MS)),
        label = "Upcoming track swipe"
    )
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(NightWavePremiumDesignTokens.SEARCH_CARD_CORNER_DP.dp))) {
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
                .heightIn(min = NightWavePremiumDesignTokens.rowMinHeightDp(fontScale).dp)
                .graphicsLayer { translationX = animatedTravel }
                .clip(RoundedCornerShape(NightWavePremiumDesignTokens.SEARCH_CARD_CORNER_DP.dp))
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
                .clickable(role = Role.Button, onClick = onSelect)
                .semantics {
                    contentDescription = NightWaveAccessibilityPolicy.queueAnnouncement(
                        number, display.title, display.artistOrCreator, isPlaying)
                    if (isPlaying) stateDescription = "Playing"
                }
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
                Text(display.title, maxLines = titleLines, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall)
                Text(display.artistOrCreator, maxLines = if (fontScale >= 1.8f) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isPlaying) {
                Text("PLAYING", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            } else if (canRemove && (onMoveEarlier != null || onMoveLater != null)) {
                var optionsOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { optionsOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Queue options for ${display.title}")
                    }
                    DropdownMenu(expanded = optionsOpen, onDismissRequest = { optionsOpen = false }) {
                        if (onMoveEarlier != null) DropdownMenuItem(text = { Text("Move earlier") },
                            onClick = { optionsOpen = false; onMoveEarlier() })
                        if (onMoveLater != null) DropdownMenuItem(text = { Text("Move later") },
                            onClick = { optionsOpen = false; onMoveLater() })
                        DropdownMenuItem(text = { Text("Remove from Up Next") },
                            onClick = { optionsOpen = false; onRemove() })
                    }
                }
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
