package com.nightwave.player.playback

import android.media.audiofx.Equalizer
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.nightwave.player.v2.insights.ListeningMemoryStore
import com.nightwave.player.v2.insights.ListeningSessionMeter
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.net.YouTubeGateway
import com.nightwave.player.offline.OfflineAudioLibrary
import com.nightwave.player.offline.OfflinePlaybackQueue
import com.nightwave.player.offline.OfflineGaplessPolicy
import com.nightwave.player.offline.OfflineSessionStore
import com.nightwave.player.v2.ui.NightWaveStudioActivity
import java.util.concurrent.Executors
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.Future

/** The service owns automatic next-track resolution, even while the Activity is stopped. */
@UnstableApi
class PlaybackService : MediaSessionService() {
    companion object {
        const val ACTION_REPLACE = "com.nightwave.player.REPLACE_QUEUE"
        const val ACTION_RESUME_SAVED = "com.nightwave.player.RESUME_SAVED"
        const val ACTION_RETRY_CURRENT = "com.nightwave.player.RETRY_CURRENT"
        const val ACTION_PLAY_OFFLINE = "com.nightwave.player.PLAY_OFFLINE"
        const val ACTION_RESUME_OFFLINE = "com.nightwave.player.RESUME_OFFLINE"
        const val ACTION_JUMP_OFFLINE = "com.nightwave.player.JUMP_OFFLINE"
        const val EXTRA_OFFLINE_ID = "offline_id"
        const val EXTRA_OFFLINE_IDS = "offline_ids"
        /** Atomic queue assignment followed by a single play request for the v2 interface. */
        const val ACTION_PLAY_QUEUE = "com.nightwave.player.PLAY_QUEUE"
        const val EXTRA_QUEUE_DISPLAY_ORDER = "queue_display_order"
        const val ACTION_ENQUEUE = "com.nightwave.player.ENQUEUE"
        const val ACTION_REMOVE_UPCOMING = "com.nightwave.player.REMOVE_UPCOMING"
        const val ACTION_UNDO_REMOVE_UPCOMING = "com.nightwave.player.UNDO_REMOVE_UPCOMING"
        const val EXTRA_QUEUE_TOKEN = "queue_token"
        const val EXTRA_EXPECTED_URL = "expected_queue_url"
        const val ACTION_NEXT = "com.nightwave.player.NEXT"
        const val ACTION_PREVIOUS = "com.nightwave.player.PREVIOUS"
        const val ACTION_JUMP = "com.nightwave.player.JUMP"
        const val ACTION_SHUFFLE = "com.nightwave.player.SHUFFLE"
        const val ACTION_REPEAT = "com.nightwave.player.REPEAT"
        const val EXTRA_REPEAT_MODE = "repeat_mode"
        const val EXTRA_REPEAT_REQUEST_ID = "repeat_request_id"
        const val ACTION_SLEEP_TIMER_SET = "com.nightwave.player.SLEEP_TIMER_SET"
        const val ACTION_SLEEP_TIMER_CANCEL = "com.nightwave.player.SLEEP_TIMER_CANCEL"
        const val ACTION_OFFLINE_SPEED_SET = "com.nightwave.player.OFFLINE_SPEED_SET"
        const val ACTION_CROSSFADE_SET = "com.nightwave.player.CROSSFADE_SET"
        const val EXTRA_CROSSFADE_SECONDS = "crossfade_seconds"
        const val ACTION_LISTENING_LEVEL_SET = "com.nightwave.player.LISTENING_LEVEL_SET"
        const val ACTION_EQ_PRESET_SET = "com.nightwave.player.EQ_PRESET_SET"
        const val EXTRA_EQ_PRESET = "eq_preset"
        const val ACTION_EQ_BAND_SET = "com.nightwave.player.EQ_BAND_SET"
        const val ACTION_EQ_BANDS_APPLY = "com.nightwave.player.EQ_BANDS_APPLY"
        const val ACTION_EQ_DISCOVER = "com.nightwave.player.EQ_DISCOVER"
        const val EXTRA_EQ_LEVELS_MB = "eq_levels_mb"
        const val EXTRA_EQ_COMMAND_ID = "eq_command_id"
        const val EXTRA_EQ_BAND = "eq_band"
        const val EXTRA_EQ_LEVEL_MB = "eq_level_mb"
        const val EXTRA_LISTENING_LEVEL_PERCENT = "listening_level_percent"
        const val EXTRA_OFFLINE_SPEED_PERCENT = "offline_speed_percent"
        const val EXTRA_SLEEP_MINUTES = "sleep_minutes"
        const val EXTRA_URLS = "urls"
        const val EXTRA_TITLES = "titles"
        const val EXTRA_CREATORS = "creators"
        const val EXTRA_ARTWORKS = "artworks"
        const val EXTRA_INDEX = "index"
    }

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private val queue = PlaybackQueue()
    private lateinit var savedQueue: QueueStateStore
    private val main = Handler(Looper.getMainLooper())
    private lateinit var sleepStatus: SleepTimerStatusStore
    private lateinit var offlineSpeedStore: OfflinePlaybackSpeedStore
    private lateinit var listeningLevelStore: ListeningLevelStore
    private var sleepTimerGeneration = 0
    private var sleepDeadlineElapsedMs = 0L
    private var sleepTimerRunnable: Runnable? = null
    // Suppress already-posted automatic Next callbacks after expiry, until
    // the listener observes a genuine new user-initiated Play event.
    private var sleepTimerPaused = false
    // 6W1: explicit, session-scoped and OFF by default. No volume amplification or file changes.
    private var requestedEqPreset = 0
    private val manualLevels = mutableMapOf<Int, Short>()
    private val eqStatus by lazy { EqualizerStatusStore(this) }
    private val eqCapabilities by lazy { EqualizerCapabilities(this) }
    private var activeEqualizer: Equalizer? = null
    private var equalizerSessionId = C.AUDIO_SESSION_ID_UNSET

    private fun releaseEqualizer() {
        val previous = activeEqualizer
        activeEqualizer = null
        equalizerSessionId = C.AUDIO_SESSION_ID_UNSET
        eqCapabilities.clear()
        eqStatus.write(requestedEqPreset, false, if (requestedEqPreset == 0) "Off" else "Not active")
        runCatching { previous?.enabled = false }
        runCatching { previous?.release() }
    }

    private fun applyEqualizer(sessionId: Int) {
        if (requestedEqPreset == 0 || sessionId <= 0) { releaseEqualizer(); return }
        if (activeEqualizer != null && equalizerSessionId == sessionId) return
        releaseEqualizer()
        var candidate: Equalizer? = null
        try {
            candidate = Equalizer(0, sessionId)
            eqCapabilities.publish(candidate, sessionId)
            if (requestedEqPreset == 21) {
                // Manual mode: range and band count come from this exact device/session.
                val range = candidate.bandLevelRange
                for ((index, level) in manualLevels) {
                    if (index >= candidate.numberOfBands.toInt() || level < range[0] || level > range[1]) return
                    candidate.setBandLevel(index.toShort(), level)
                }
            } else {
                val presetIndex = requestedEqPreset - 1
                if (presetIndex !in 0 until candidate.numberOfPresets.toInt()) return
                candidate.usePreset(presetIndex.toShort())
            }
            candidate.enabled = true
            if (!candidate.enabled) return
            activeEqualizer = candidate
            equalizerSessionId = sessionId
            eqStatus.write(requestedEqPreset, true, "Enabled by Android effect API")
            candidate = null // ownership transferred
        } catch (_: Exception) {
            eqStatus.write(requestedEqPreset, false, "Unavailable on this audio session")
            // Unsupported or already-owned audio sessions must not interrupt playback.
        } finally {
            runCatching { candidate?.release() }
        }
    }
    // A delayed STATE_ENDED notification must not advance the same track twice.
    private var handledEndGeneration = Int.MIN_VALUE
    private var handledEndMediaId: String? = null

    private fun clearSleepTimer() {
        sleepTimerGeneration++
        sleepTimerRunnable?.let(main::removeCallbacks)
        sleepTimerRunnable = null
        sleepDeadlineElapsedMs = 0L
        if (::sleepStatus.isInitialized) sleepStatus.clear()
    }

    private fun setSleepTimer(minutes: Int) {
        val deadline = SleepTimerPolicy.deadlineMs(SystemClock.elapsedRealtime(), minutes)
            ?: return // invalid input cannot cancel or extend an active timer
        clearSleepTimer()
        val token = sleepTimerGeneration
        sleepDeadlineElapsedMs = deadline
        sleepStatus.setDeadline(deadline)
        val runnable = Runnable {
            if (sleepTimerGeneration != token || sleepDeadlineElapsedMs != deadline) return@Runnable
            if (!SleepTimerPolicy.expires(SystemClock.elapsedRealtime(), deadline)) {
                // Late/early Handler dispatch still respects the monotonic deadline.
                val delay = SleepTimerPolicy.remainingMs(SystemClock.elapsedRealtime(), deadline)
                main.postDelayed(sleepTimerRunnable ?: return@Runnable, delay)
                return@Runnable
            }
            sleepTimerPaused = true
            cancelCrossfade()
            clearSleepTimer()
            // Pause both public and app-owned offline audio. If a public URL is
            // resolving, invalidate it to prevent the result from autoplaying.
            generation++
            cancelPendingResolution()
            exoPlayer?.pause()
            exoPlayer?.playWhenReady = false
            persistProgress(flush = true)
        }
        sleepTimerRunnable = runnable
        main.postDelayed(runnable,
            SleepTimerPolicy.remainingMs(SystemClock.elapsedRealtime(), deadline))
    }

    // One slow, stale extractor cannot block a newer selection. Maximum two
    // active workers + one queued request; do not spawn unbounded network jobs.
    private val resolver = ThreadPoolExecutor(2, 2, 30L, TimeUnit.SECONDS,
        ArrayBlockingQueue<Runnable>(1), ThreadPoolExecutor.AbortPolicy())
    private var resolveFuture: Future<*>? = null
    private var resolveDeadline: Runnable? = null
    private val gateway = YouTubeGateway()
    private var generation = 0
    /** Main-thread marker for a public track awaiting extractor resolution. */
    private var resolvingPublicUrl: String? = null
    private var offlinePlaying = false
    // True only for a fully validated local Media3 playlist. Public streams are never preloaded.
    private var offlineGaplessActive = false
    private lateinit var crossfadeSettings: CrossfadeSettingsStore
    private var crossfadeSeconds = CrossfadePolicy.DEFAULT_SECONDS
    private var crossfadeIncoming: ExoPlayer? = null
    private var crossfadeOutgoing: ExoPlayer? = null
    private var crossfadeTargetIndex = -1
    private var crossfadeStartedAtMs = 0L
    private var crossfadeHandoffInProgress = false

    private val crossfadeTick = object : Runnable {
        override fun run() {
            val player = exoPlayer ?: return
            if (!offlinePlaying || !offlineGaplessActive ||
                crossfadeSeconds == 0 || !player.isPlaying) {
                cancelCrossfade()
                return
            }
            val index = player.currentMediaItemIndex
            val duration = player.duration
            val position = player.currentPosition
            if (!CrossfadePolicy.canPrepare(crossfadeSeconds, offlinePlaying,
                    offlineGaplessActive, player.playbackParameters.speed == 1f,
                    index, player.mediaItemCount, duration, position)) {
                cancelCrossfade()
                return
            }
            val remaining = (duration - position).coerceAtLeast(0L)
            val overlap = CrossfadePolicy.startThresholdMs(crossfadeSeconds)
            if (crossfadeOutgoing != null && crossfadeOutgoing !== player) {
                cancelCrossfade()
                return
            }
            // External seeks can move the song away from the overlap zone.
            // Never keep a silent second deck playing after a seek backwards.
            if (crossfadeStartedAtMs > 0L && remaining > overlap + 500L) {
                cancelCrossfade()
                scheduleCrossfadeCheck()
                return
            }
            if (crossfadeIncoming == null && remaining <= overlap + 1_500L &&
                remaining > 350L) {
                prepareCrossfadeDeck(player, index + 1)
            }
            val incoming = crossfadeIncoming
            if (incoming != null && crossfadeTargetIndex == index + 1) {
                if (incoming.playerError != null) {
                    cancelCrossfade() // decoding failed; original gapless player remains.
                    return
                }
                if (crossfadeStartedAtMs == 0L && remaining <= overlap &&
                    remaining >= 350L && incoming.playbackState == Player.STATE_READY) {
                    crossfadeStartedAtMs = SystemClock.elapsedRealtime()
                    incoming.play()
                }
                if (crossfadeStartedAtMs > 0L) {
                    val progress = CrossfadePolicy.progress(
                        SystemClock.elapsedRealtime() - crossfadeStartedAtMs, crossfadeSeconds)
                    val (oldGain, newGain) = CrossfadePolicy.gains(
                        progress, ListeningLevelPolicy.gain(listeningLevelStore.read()))
                    player.volume = oldGain
                    incoming.volume = newGain
                    if (progress >= .97f || remaining <= 150L) {
                        finishCrossfade()
                        return
                    }
                } else if (remaining < 350L) {
                    cancelCrossfade() // decoder wasn't ready; preserve ordinary gapless.
                }
            }
            main.postDelayed(this, if (remaining <= overlap + 2_000L) 55L else 700L)
        }
    }

    private fun scheduleCrossfadeCheck() {
        main.removeCallbacks(crossfadeTick)
        if (crossfadeSeconds > 0 && offlineGaplessActive && offlinePlaying &&
            exoPlayer?.isPlaying == true) main.post(crossfadeTick)
    }

    private fun prepareCrossfadeDeck(outgoing: ExoPlayer, nextIndex: Int) {
        val id = offlineQueue.snapshot().getOrNull(nextIndex) ?: return
        if (nextIndex !in 0 until outgoing.mediaItemCount ||
            outgoing.getMediaItemAt(nextIndex).mediaId != "offline:$id") return
        val library = OfflineAudioLibrary(this)
        val indexed = runCatching { library.find(id) }.getOrNull() ?: return
        if (library.fileFor(indexed) == null) return // no unchecked path preload
        var candidate: ExoPlayer? = null
        try {
            candidate = ExoPlayer.Builder(this).build().apply {
                setWakeMode(C.WAKE_MODE_LOCAL)
                volume = 0f
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA).build(), false)
                setMediaItems((0 until outgoing.mediaItemCount).map(outgoing::getMediaItemAt),
                    nextIndex, 0L)
                playWhenReady = false
                prepare()
            }
            crossfadeOutgoing = outgoing
            crossfadeIncoming = candidate
            crossfadeTargetIndex = nextIndex
        } catch (error: Exception) {
            Log.w("NightWavePlayback", "Crossfade prebuffer unavailable", error)
            runCatching { candidate?.release() }
        }
    }

    private fun cancelCrossfade() {
        main.removeCallbacks(crossfadeTick)
        val deck = crossfadeIncoming
        crossfadeIncoming = null
        crossfadeOutgoing = null
        crossfadeTargetIndex = -1
        crossfadeStartedAtMs = 0L
        runCatching { deck?.pause(); deck?.release() }
        exoPlayer?.volume = if (::listeningLevelStore.isInitialized)
            ListeningLevelPolicy.gain(listeningLevelStore.read()) else 1f
    }

    /**
     * Both decks are already playing during the overlap. Transfer the media
     * session on its application looper; never create a second MediaSession.
     */
    private fun finishCrossfade() {
        val old = exoPlayer ?: return cancelCrossfade()
        val new = crossfadeIncoming ?: return cancelCrossfade()
        val index = crossfadeTargetIndex
        val ids = offlineQueue.snapshot()
        val id = ids.getOrNull(index) ?: return cancelCrossfade()
        val session = mediaSession ?: return cancelCrossfade()
        if (crossfadeOutgoing !== old || new.currentMediaItem?.mediaId != "offline:$id" ||
            !offlinePlaying || !offlineGaplessActive || sleepTimerPaused) {
            cancelCrossfade()
            return
        }
        crossfadeHandoffInProgress = true
        try {
            // Only the session-owned deck takes Android audio focus after the swap.
            new.setAudioAttributes(AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA).build(), true)
            session.setPlayer(new) // Media3 requires the same application Looper.
        } catch (error: Exception) {
            crossfadeHandoffInProgress = false
            Log.w("NightWavePlayback", "Crossfade session handoff unavailable", error)
            cancelCrossfade() // primary remains connected, ordinary gapless continues
            return
        }
        sampleListening(endSession = true)
        persistProgress(flush = true)
        old.removeListener(trackListener)
        exoPlayer = new
        crossfadeHandoffInProgress = false
        crossfadeIncoming = null
        crossfadeOutgoing = null
        crossfadeTargetIndex = -1
        crossfadeStartedAtMs = 0L
        main.removeCallbacks(crossfadeTick)
        releaseEqualizer()
        new.addListener(trackListener)
        applyEqualizer(new.audioSessionId)
        new.volume = ListeningLevelPolicy.gain(listeningLevelStore.read())
        if (offlineQueue.jump(index) != null) {
            offlineSession.saveSelection(ids, id, new.currentPosition.coerceAtLeast(0L))
        }
        generation++ // invalidate delayed outgoing track-end callbacks
        handledEndMediaId = null
        runCatching { old.pause(); old.release() }
        main.removeCallbacks(progressWriter)
        if (new.isPlaying) main.postDelayed(progressWriter, 2_500L)
        scheduleCrossfadeCheck()
    }
    // The service owns the only pending Undo. No playlist/audio-file writes.
    private var pendingQueueRemoval: Pair<String, PlaybackQueue.RemovedUpcoming>? = null
    private val offlineQueue = OfflinePlaybackQueue()
    private lateinit var offlineSession: OfflineSessionStore
    private lateinit var listeningMemory: ListeningMemoryStore
    private val listeningMeter = ListeningSessionMeter()
    private val listeningWriter = Executors.newSingleThreadExecutor()
    private var lastMemoryConsentRevision = -1L
    private var autoRetriedVideo: String? = null
    private val progressWriter = object : Runnable {
        override fun run() {
            val player = exoPlayer ?: return
            if (!player.isPlaying) return
            sampleListening()
            persistProgress()
            main.postDelayed(this, 2500L)
        }
    }

    private fun persistProgress(flush: Boolean = false) {
        val player = exoPlayer ?: return
        val url = player.currentMediaItem?.mediaId ?: return
        if (offlinePlaying && url.startsWith("offline:")) {
            if (::offlineSession.isInitialized) runCatching {
                offlineSession.savePosition(url.removePrefix("offline:"),
                    player.currentPosition.coerceAtLeast(0L), flush)
            }
            return
        }
        if (!::savedQueue.isInitialized || !url.startsWith("https://www.youtube.com/watch?v=")) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        // A corrupt preference must never terminate the background playback service.
        runCatching {
            savedQueue.saveBookmark(url, player.currentPosition.coerceAtLeast(0L), duration)
        }
    }

    /**
     * Only the Media3-owning service records session time. Player callbacks and
     * the preexisting 2.5s bookmark ticker run on the player/main looper.
     * Consent is checked every sample and AGAIN by the store under a lock.
     */
    private fun sampleListening(endSession: Boolean = false) {
        val player = exoPlayer ?: return
        try {
            if (!::listeningMemory.isInitialized) return
            val revision = listeningMemory.consentRevision()
            if (revision != lastMemoryConsentRevision) {
                listeningMeter.reset()
                lastMemoryConsentRevision = revision
            }
            if (!listeningMemory.isEnabled()) {
                listeningMeter.reset()
                return
            }
            val elapsed = SystemClock.elapsedRealtime()
            val wall = System.currentTimeMillis()
            val pulses = if (endSession) listeningMeter.finish(elapsed, wall)
                else listeningMeter.observe(
                    trackId = player.currentMediaItem?.mediaId,
                    trackTitle = player.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty(),
                    trackArtist = player.currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty(),
                    playing = player.isPlaying && player.playbackState == Player.STATE_READY,
                    nowElapsedMs = elapsed, nowWallMs = wall
                )
            // Never serialize/commit aggregates from a Media3 callback.
            // A single worker preserves pulse order; store rechecks consent.
            pulses.forEach { pulse ->
                listeningWriter.execute {
                    runCatching { listeningMemory.record(pulse, revision) }
                }
            }
        } catch (_: Exception) {
            // Personal insights must never interrupt streaming or offline music.
            listeningMeter.reset()
        }
    }

    private val trackListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            eqCapabilities.clear()
            applyEqualizer(audioSessionId)
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying && crossfadeOutgoing === exoPlayer &&
                !crossfadeHandoffInProgress) cancelCrossfade()
            if (isPlaying) {
                scheduleCrossfadeCheck()
                sleepTimerPaused = false
                // Genuine playback after Repeat One/manual Replay permits a new natural end.
                handledEndMediaId = null
            }
            sampleListening()
            main.removeCallbacks(progressWriter)
            if (isPlaying) main.postDelayed(progressWriter, 2500L)
            else persistProgress(flush = true)
        }

        override fun onPlaybackStateChanged(state: Int) {
            sampleListening()
            if (state == Player.STATE_ENDED) {
                // Media3 callbacks must not change their own player timeline.
                // Capture the exact ended item and selection generation BEFORE
                // posting; Next, Jump, Repeat, and app/process recovery may run
                // before a deferred callback gets its chance to execute.
                val ended = PlaybackTransitionPolicy.Ended(
                    generation, exoPlayer?.currentMediaItem?.mediaId, offlinePlaying)
                main.post {
                    if (!sleepTimerPaused && exoPlayer?.playbackState == Player.STATE_ENDED) {
                        val selected = if (offlinePlaying) offlineQueue.current?.let { "offline:" + it }
                            else queue.current?.url
                        if (PlaybackTransitionPolicy.shouldAdvance(
                                ended, generation, exoPlayer?.currentMediaItem?.mediaId,
                                selected, offlinePlaying,
                                isEnded = exoPlayer?.playbackState == Player.STATE_ENDED,
                                timerPaused = sleepTimerPaused,
                                alreadyHandled = handledEndGeneration == ended.generation &&
                                    handledEndMediaId == ended.mediaId
                            )
                        ) {
                            handledEndGeneration = ended.generation
                            handledEndMediaId = ended.mediaId
                            if (offlinePlaying) {
                                if (!advanceOffline(forward = true)) {
                                    // Last track is finished: keep queue/favorites/files,
                                    // but restart from 0 only on the user's next Play.
                                    offlineQueue.current?.let {
                                        offlineSession.savePosition(it, 0L, flush = true)
                                    }
                                }
                            } else playNext(true)
                        }
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (crossfadeHandoffInProgress) return
            if (crossfadeOutgoing === exoPlayer && crossfadeIncoming != null &&
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                cancelCrossfade() // external MediaController navigation
            }
            if (crossfadeOutgoing === exoPlayer && crossfadeIncoming != null &&
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                if (crossfadeStartedAtMs > 0L) finishCrossfade()
                else cancelCrossfade()
            }
            if (offlinePlaying && offlineGaplessActive &&
                (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)) {
                // Media3 advanced directly within the validated private-file timeline.
                // Keep the existing ID-based offline queue and checkpoint in sync
                // without recreating the player or opening another file.
                val ids = offlineQueue.snapshot()
                val index = OfflineGaplessPolicy.transitionIndex(
                    ids, offlineQueue.index, mediaItem?.mediaId,
                    automatic = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
                if (index != null && exoPlayer?.currentMediaItemIndex == index) {
                    val selectedId = ids[index]
                    if (offlineQueue.current != selectedId && offlineQueue.jump(index) != null) {
                        if (!offlineSession.saveSelection(ids, selectedId, 0L)) {
                            Log.w("NightWavePlayback", "Gapless transition checkpoint unavailable")
                        }
                    }
                }
            }
            // Repeated playback of the same track is a new session, not a
            // permanent single lifetime play. Other transitions are detected
            // by the track identity in ListeningSessionMeter.observe().
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                sampleListening(endSession = true)
            }
            sampleListening()
            scheduleCrossfadeCheck()
        }

        override fun onPlayerError(error: PlaybackException) {
            if (!crossfadeHandoffInProgress) cancelCrossfade()
            sampleListening()
            val player = exoPlayer ?: return
            val video = player.currentMediaItem?.mediaId ?: return
            if (offlinePlaying || queue.current?.url != video) return
            if (autoRetriedVideo == video) {
                savedQueue.recordFailure(video, "Stream refresh failed. Tap Retry to try again.")
                return
            }
            val retriable = error.errorCode in setOf(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
            )
            if (!PlaybackRecoveryPolicy.mayAutoRetry(
                    expectedUrl = video, selectedUrl = queue.current?.url,
                    loadedMediaId = player.currentMediaItem?.mediaId,
                    offline = offlinePlaying, timerPaused = sleepTimerPaused,
                    wantsPlayback = player.playWhenReady,
                    alreadyRetried = autoRetriedVideo == video,
                    retriableError = retriable, resolvingUrl = resolvingPublicUrl
                )) {
                savedQueue.recordFailure(video,
                    "Playback stopped. Tap Retry or choose another song.")
                return
            }
            autoRetriedVideo = video
            val retryGeneration = generation
            val position = player.currentPosition.coerceAtLeast(0L)
            Log.w("NightWavePlayback", "Transient playback error; one guarded refresh requested")
            // Avoid changing ExoPlayer inside its error callback. Recheck the
            // *current* user intent before posting any fresh network request.
            main.post {
                val currentPlayer = exoPlayer
                if (PlaybackRecoveryPolicy.acceptsPostedRetry(
                        expectedUrl = video,
                        scheduledGeneration = retryGeneration,
                        currentGeneration = generation,
                        selectedUrl = queue.current?.url,
                        loadedMediaId = currentPlayer?.currentMediaItem?.mediaId,
                        offline = offlinePlaying, timerPaused = sleepTimerPaused,
                        wantsPlayback = currentPlayer?.playWhenReady == true,
                        retryMarkerUrl = autoRetriedVideo,
                        resolvingUrl = resolvingPublicUrl
                    )) {
                    queue.current?.let { resolveAndPlay(it, autoPlay = true,
                        resumePositionMs = position, recoveryAttempt = true) }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedQueue = QueueStateStore(this)
        sleepStatus = SleepTimerStatusStore(this)
        offlineSpeedStore = OfflinePlaybackSpeedStore(this)
        listeningLevelStore = ListeningLevelStore(this)
        crossfadeSettings = CrossfadeSettingsStore(this)
        crossfadeSeconds = crossfadeSettings.read()
        // A process/service restart never silently resurrects an old timer.
        // Playback itself also requires an explicit user resume after restart.
        clearSleepTimer()
        eqCapabilities.clear()
        eqStatus.write(0, false, "Off")
        savedQueue.clearLookup() // process restart must not leave a ghost spinner
        offlineSession = OfflineSessionStore(this)
        listeningMemory = ListeningMemoryStore(this)
        savedQueue.restore(queue)
        // Do NOT play, seek, or access any offline file on service creation.
        // The user must request ACTION_RESUME_OFFLINE explicitly.
        val networkSource = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/121.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(20_000)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(this, networkSource)))
            .build().apply {
                // One service-owned output trim. Unity (100%) is the default.
                volume = ListeningLevelPolicy.gain(listeningLevelStore.read())
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(), true
                )
                setWakeMode(C.WAKE_MODE_NETWORK)
                playWhenReady = false
                addListener(trackListener)
            }
        exoPlayer = player
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, NightWaveStudioActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, player).setSessionActivity(openApp)
            .setCallback(object : MediaSession.Callback {
                override fun onMediaButtonEvent(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    intent: Intent
                ): Boolean {
                    @Suppress("DEPRECATION")
                    val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    if (event?.action != KeyEvent.ACTION_DOWN) return false
                    return when (event.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT -> { playNext(false); true }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { playPrevious(); true }
                        KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_HEADSETHOOK -> {
                            if (exoPlayer?.currentMediaItem == null && !offlinePlaying && queue.current != null) {
                                resumeSavedQueue()
                                true
                            } else false // Media3 handles ordinary play/pause.
                        }
                        else -> false
                    }
                }
            }).build()
        // The saved queue, artwork and bookmark are available immediately from
        // QueueStateStore. Do NOT issue a network request on service creation.
        // A MediaController may bind repeatedly while Android recovers an app.
        // Only explicit Play / RESUME_SAVED is allowed to resolve streaming URLs.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        // A new playback/queue command invalidates an older undo token; stale
        // UI actions must never resurrect a song in a different queue.
        if (intent?.action != ACTION_REMOVE_UPCOMING &&
            intent?.action != ACTION_UNDO_REMOVE_UPCOMING) pendingQueueRemoval = null
        when (intent?.action) {
            ACTION_REPLACE, ACTION_PLAY_QUEUE -> {
                val urls = intent.getStringArrayListExtra(EXTRA_URLS).orEmpty()
                val titles = intent.getStringArrayListExtra(EXTRA_TITLES).orEmpty()
                val creators = intent.getStringArrayListExtra(EXTRA_CREATORS).orEmpty()
                val artworks = intent.getStringArrayListExtra(EXTRA_ARTWORKS).orEmpty()
                val entries = urls.take(100).mapIndexedNotNull { index, url ->
                    if (!url.startsWith("https://www.youtube.com/watch?v=")) return@mapIndexedNotNull null
                    QueueEntry(url, titles.getOrNull(index).orEmpty(), creators.getOrNull(index).orEmpty(), artworks.getOrNull(index).orEmpty())
                }
                val selected = intent.getIntExtra(EXTRA_INDEX, 0)
                val explicitDisplayOrder = intent.action == ACTION_PLAY_QUEUE &&
                    intent.getBooleanExtra(EXTRA_QUEUE_DISPLAY_ORDER, false)
                val proposedQueue = entries.distinctBy { it.url }.take(100)
                val selectedEntry = proposedQueue.getOrNull(selected.coerceIn(
                    0, (proposedQueue.size - 1).coerceAtLeast(0)))
                val sameQueue = intent.action == ACTION_PLAY_QUEUE && !offlinePlaying &&
                    proposedQueue.isNotEmpty() && queue.snapshot() == proposedQueue &&
                    queue.index == selected.coerceIn(0, proposedQueue.lastIndex)
                val loaded = PlaybackResolutionPolicy.reusesLoadedTrack(
                    selectedEntry?.url, exoPlayer?.currentMediaItem?.mediaId,
                    exoPlayer?.playerError != null
                )
                val resolving = selectedEntry != null &&
                    PlaybackResolutionPolicy.awaitsSameResolution(selectedEntry.url, resolvingPublicUrl)
                if (sameQueue && (loaded || resolving)) {
                    // The same-queue fast path must honor explicit Library ordering.
                    if (explicitDisplayOrder) {
                        queue.useExplicitDisplayOrder()
                        savedQueue.save(queue)
                    }
                    // Double-tap/recomposition must not drop an already buffered
                    // stream or enqueue another slow extraction of the same URL.
                    if (loaded) exoPlayer?.apply {
                        if (playbackState == Player.STATE_ENDED) seekTo(0L)
                        if (playbackState == Player.STATE_ENDED ||
                            playbackState == Player.STATE_IDLE) prepare()
                        play()
                    }
                } else {
                    queue.replace(entries, selected)
                    if (explicitDisplayOrder) queue.useExplicitDisplayOrder()
                    generation++ // invalidate unresolved previous track jobs
                    cancelPendingResolution()
                    savedQueue.save(queue)
                    queue.current?.let { savedQueue.saveBookmark(it.url, 0L, 0L) }
                    if (intent.action == ACTION_PLAY_QUEUE) {
                        offlineQueue.reset()
                        exoPlayer?.pause()
                        queue.current?.let { resolveAndPlay(it) }
                    }
                }
            }
            ACTION_REMOVE_UPCOMING -> {
                pendingQueueRemoval = null
                val at = intent.getIntExtra(EXTRA_INDEX, -1)
                val expected = intent.getStringExtra(EXTRA_EXPECTED_URL).orEmpty()
                val token = intent.getStringExtra(EXTRA_QUEUE_TOKEN).orEmpty()
                if (!offlinePlaying && token.length == 36 &&
                    expected.startsWith("https://www.youtube.com/watch?v=")) {
                    queue.removeUpcoming(at, expected)?.let { removed ->
                        savedQueue.save(queue)
                        pendingQueueRemoval = token to removed
                    }
                }
            }
            ACTION_UNDO_REMOVE_UPCOMING -> {
                val token = intent.getStringExtra(EXTRA_QUEUE_TOKEN).orEmpty()
                val pending = pendingQueueRemoval
                pendingQueueRemoval = null // one-shot even if restore is denied
                if (!offlinePlaying && token.isNotBlank() &&
                    pending?.first == token &&
                    queue.restoreUpcoming(pending.second)) {
                    savedQueue.save(queue)
                }
            }
            ACTION_ENQUEUE -> {
                val url = intent.getStringExtra(EXTRA_URLS)
                if (url?.startsWith("https://www.youtube.com/watch?v=") == true) {
                    queue.add(QueueEntry(url, intent.getStringExtra(EXTRA_TITLES).orEmpty(),
                        intent.getStringExtra(EXTRA_CREATORS).orEmpty(),
                        intent.getStringExtra(EXTRA_ARTWORKS).orEmpty()))
                    savedQueue.save(queue)
                }
            }
            ACTION_PLAY_OFFLINE -> {
                cancelCrossfade()
                val id = intent.getStringExtra(EXTRA_OFFLINE_ID).orEmpty()
                val localLibrary = OfflineAudioLibrary(this)
                // Accept only registered private IDs, never externally supplied paths.
                val ownedIds = runCatching { localLibrary.songs().map { it.id }.toSet() }
                    .getOrDefault(emptySet())
                val submitted = intent.getStringArrayListExtra(EXTRA_OFFLINE_IDS)
                    .orEmpty().filter { it in ownedIds }
                val candidates = if (submitted.isEmpty()) listOf(id).filter { it in ownedIds }
                    else submitted
                if (offlineQueue.select(candidates, id) != null) {
                    if (!playOfflineId(id, localLibrary)) advanceOffline(forward = true)
                }
            }
            ACTION_RESUME_OFFLINE -> resumeOfflineSession()
            ACTION_JUMP_OFFLINE -> if (offlinePlaying) {
                cancelCrossfade()
                val index = intent.getIntExtra(EXTRA_INDEX, -1)
                val player = exoPlayer
                val id = offlineQueue.snapshot().getOrNull(index)
                if (offlineGaplessActive && id != null && player != null &&
                    index in 0 until player.mediaItemCount &&
                    player.getMediaItemAt(index).mediaId == "offline:$id") {
                    // Seek within the prepared playlist; listener persists the
                    // new ID on transition. Same-slot jumps still reset bookmark.
                    if (offlineQueue.index == index) {
                        offlineSession.saveSelection(offlineQueue.snapshot(), id, 0L)
                    }
                    player.seekTo(index, 0L)
                    player.play()
                } else if (!offlineGaplessActive) {
                    val next = offlineQueue.jump(index)
                    if (next != null) playOfflineId(next)
                }
            }
            ACTION_RESUME_SAVED -> resumeSavedQueue()
            ACTION_RETRY_CURRENT -> retryCurrent()
            ACTION_SLEEP_TIMER_SET -> setSleepTimer(intent.getIntExtra(EXTRA_SLEEP_MINUTES, 0))
            ACTION_SLEEP_TIMER_CANCEL -> clearSleepTimer()
            ACTION_EQ_PRESET_SET -> {
                val preset = intent.getIntExtra(EXTRA_EQ_PRESET, -1)
                if (preset in 0..21) {
                    requestedEqPreset = preset
                    applyEqualizer(exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET)
                } else eqStatus.write(requestedEqPreset, activeEqualizer?.enabled == true, "Rejected: invalid preset")
            }
            ACTION_EQ_DISCOVER -> {
                val session = exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
                if (session > 0) {
                    var probe: Equalizer? = null
                    try {
                        probe = activeEqualizer ?: Equalizer(0, session)
                        eqCapabilities.publish(probe, session)
                    } catch (_: Exception) {
                        eqCapabilities.clear()
                    } finally {
                        if (probe !== activeEqualizer) runCatching { probe?.release() }
                    }
                } else eqCapabilities.clear()
            }
            ACTION_EQ_BANDS_APPLY -> {
                val values = intent.getIntArrayExtra(EXTRA_EQ_LEVELS_MB)
                if (values != null && values.size in 1..32 && values.all { it in -1500..1500 }) {
                    val session = exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
                    val caps = eqCapabilities.read()
                    if (session > 0 && caps.sessionReady && caps.audioSessionId == session && values.size == caps.centersHz.size &&
                        values.all { it in caps.minimumMb..caps.maximumMb }) {
                        manualLevels.clear()
                        values.forEachIndexed { index, level -> manualLevels[index] = level.toShort() }
                        requestedEqPreset = 21
                        releaseEqualizer()
                        applyEqualizer(session)
                    } else eqStatus.write(requestedEqPreset, activeEqualizer?.enabled == true, "Rejected: stale session or unsupported bands")
                } else eqStatus.write(requestedEqPreset, activeEqualizer?.enabled == true, "Rejected: invalid band input")
            }
            ACTION_EQ_BAND_SET -> {
                val band = intent.getIntExtra(EXTRA_EQ_BAND, -1)
                val level = intent.getIntExtra(EXTRA_EQ_LEVEL_MB, Int.MIN_VALUE)
                if (band in 0..4 && level in -1500..1500) {
                    manualLevels[band] = level.toShort()
                    requestedEqPreset = 21
                    releaseEqualizer()
                    applyEqualizer(exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET)
                }
            }
            ACTION_CROSSFADE_SET -> {
                val seconds = intent.getIntExtra(EXTRA_CROSSFADE_SECONDS, -1)
                if (CrossfadePolicy.accepts(seconds) && crossfadeSettings.save(seconds)) {
                    cancelCrossfade()
                    crossfadeSeconds = seconds
                    scheduleCrossfadeCheck()
                }
            }
            ACTION_LISTENING_LEVEL_SET -> {
                val percent = intent.getIntExtra(EXTRA_LISTENING_LEVEL_PERCENT, -1)
                if (ListeningLevelPolicy.accepts(percent) &&
                    listeningLevelStore.save(percent)
                ) {
                    // No amplification above unity and no Android system-volume change.
                    if (crossfadeStartedAtMs == 0L)
                        exoPlayer?.volume = ListeningLevelPolicy.gain(percent)
                    // An active overlap recomputes both gains in its next tick.
                }
            }
            ACTION_OFFLINE_SPEED_SET -> {
                val percent = intent.getIntExtra(EXTRA_OFFLINE_SPEED_PERCENT, -1)
                if (OfflinePlaybackSpeedPolicy.accepts(percent) &&
                    offlineSpeedStore.save(percent) && offlinePlaying
                ) {
                    if (percent != OfflinePlaybackSpeedPolicy.NORMAL_PERCENT) cancelCrossfade()
                    exoPlayer?.setPlaybackSpeed(OfflinePlaybackSpeedPolicy.speed(percent))
                    scheduleCrossfadeCheck()
                }
            }
            ACTION_NEXT -> playNext(false)
            ACTION_PREVIOUS -> playPrevious()
            ACTION_JUMP -> if (!offlinePlaying) queue.jump(intent.getIntExtra(EXTRA_INDEX, -1))?.let { savedQueue.save(queue); resolveAndPlay(it) }
            ACTION_SHUFFLE -> if (!offlinePlaying) {
                queue.shuffle = !queue.shuffle; savedQueue.save(queue)
            }
            ACTION_REPEAT -> if (!offlinePlaying) {
                // Explicit selection is safe under rapid taps: the service must never
                // reinterpret an old command against a newer queue state.
                val explicit = intent?.hasExtra(EXTRA_REPEAT_MODE) == true
                val mode = RepeatModePolicy.resolve(
                    queue.repeat, explicit, intent?.getStringExtra(EXTRA_REPEAT_MODE))
                if (mode != null) {
                    queue.repeat = mode
                    savedQueue.save(queue)
                    val requestId = intent?.getStringExtra(EXTRA_REPEAT_REQUEST_ID)
                    // Acknowledge only a mode that is actually present in the
                    // persisted queue snapshot. Old legacy toggle calls have no ID.
                    if (!requestId.isNullOrEmpty() && savedQueue.read().repeat == mode) {
                        RepeatModeReceiptStore(this).acknowledge(requestId, mode)
                    }
                }
            }
        }
        if (intent?.action == ACTION_EQ_PRESET_SET || intent?.action == ACTION_EQ_BANDS_APPLY) {
            val id = intent.getStringExtra(EXTRA_EQ_COMMAND_ID).orEmpty()
            if (id.length in 1..80) {
                val receipt = eqStatus.read()
                eqStatus.write(receipt.selected, receipt.active, receipt.message, id)
            }
        }
        return result
    }

    /**
     * Invalidate queued/pending extraction jobs and their user-facing timeout.
     * This does not touch actual media playback, queue, or offline files.
     */
    private fun cancelPendingResolution() {
        resolveDeadline?.let(main::removeCallbacks)
        resolveDeadline = null
        resolveFuture?.cancel(true)
        resolveFuture = null
        resolver.queue.clear()
        resolvingPublicUrl = null
        if (::savedQueue.isInitialized) savedQueue.clearLookup()
    }

    /** Explicit retry after a playback/extraction error, without reusing signed stream URLs. */
    private fun retryCurrent() {
        if (offlinePlaying) {
            val selectedId = offlineQueue.current ?: offlineSession.read()?.selectedId
            val loadedId = exoPlayer?.currentMediaItem?.mediaId
            if (selectedId != null && loadedId == "offline:$selectedId") {
                // Retry the same app-owned local file without touching its
                // saved queue, copying it or erasing its checkpoint.
                exoPlayer?.apply { prepare(); play() }
            } else {
                // A stale/empty MediaItem is not proof the selected offline
                // file is still loaded; restore only through file ownership QA.
                resumeOfflineSession()
            }
            return
        }
        val entry = queue.current ?: return
        if (PlaybackResolutionPolicy.awaitsSameResolution(entry.url, resolvingPublicUrl)) return
        val currentId = exoPlayer?.currentMediaItem?.mediaId
        val bookmark = savedQueue.bookmark(entry.url)?.positionMs ?: 0L
        // Retry targets the user's selected saved queue entry, not any stale
        // media that happens to remain loaded. Never apply a different song's
        // position to the requested public video.
        val position = if (currentId == entry.url)
            (exoPlayer?.currentPosition?.coerceAtLeast(0L)?.takeIf { it > 0 } ?: bookmark)
        else bookmark
        resolveAndPlay(entry, autoPlay = true, resumePositionMs = position)
    }

    /** Called only by a user/transport Play action, never by onCreate. */
    private fun resumeSavedQueue() {
        // A second Resume tap while extractor work is pending must not queue
        // another resolution behind a slow first request.
        val entry = queue.current ?: return
        if (PlaybackResolutionPolicy.awaitsSameResolution(entry.url, resolvingPublicUrl)) {
            exoPlayer?.play() // resume intent only; do NOT re-extract
            return
        }
        val player = exoPlayer ?: return
        if (PlaybackResolutionPolicy.reusesLoadedTrack(
                entry.url, player.currentMediaItem?.mediaId, player.playerError != null)) {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
            if (player.playbackState == Player.STATE_ENDED ||
                player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
            return
        }
        // Never resume an offline/different cached item merely because some
        // MediaItem exists. Restore selected *public* queue and saved bookmark.
        resolveAndPlay(entry, autoPlay = true,
            resumePositionMs = savedQueue.bookmark(entry.url)?.positionMs ?: 0L)
    }

    /** Explicit restoration only: validate every ID against the current file index. */
    private fun resumeOfflineSession() {
        val saved = offlineSession.read() ?: return
        val localLibrary = OfflineAudioLibrary(this)
        val known = runCatching { localLibrary.songs().map { it.id }.toSet() }
            .getOrDefault(emptySet())
        val candidates = saved.ids.filter { it in known }
        if (offlineQueue.select(candidates, saved.selectedId) == null) return
        if (!playOfflineId(saved.selectedId, localLibrary, saved.positionMs)) {
            advanceOffline(forward = true)
        }
    }

    /**
     * Construct one Media3 timeline only if every ID belongs to a valid private
     * audio index entry and every physical file is readable. Never place
     * signed public URLs or unverified content URIs in this playlist.
     */
    private fun preparedOfflineTimeline(
        library: OfflineAudioLibrary, selectedId: String
    ): Pair<List<MediaItem>, Int>? {
        val ids = offlineQueue.snapshot()
        val selected = OfflineGaplessPolicy.selectedIndex(ids, selectedId) ?: return null
        val indexed = runCatching { library.songs().associateBy { it.id } }.getOrNull()
            ?: return null
        val items = ArrayList<MediaItem>(ids.size)
        for (id in ids) {
            val song = indexed[id] ?: return null
            val file = library.fileFor(song) ?: return null
            items.add(MediaItem.Builder()
                .setMediaId("offline:" + song.id)
                .setUri(Uri.fromFile(file))
                .setMimeType(song.mimeType)
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(song.title).setArtist("Offline · NightWave").build())
                .build())
        }
        return items to selected
    }

    /** Offline IDs are bounded; never write them into public-video bookmarks. */
    private fun playOfflineId(
        id: String, localLibrary: OfflineAudioLibrary = OfflineAudioLibrary(this),
        resumePositionMs: Long = 0L
    ): Boolean {
        cancelCrossfade()
        val song = runCatching { localLibrary.find(id) }.getOrNull() ?: return false
        val file = localLibrary.fileFor(song) ?: return false
        val player = exoPlayer ?: return false
        if (offlinePlaying) persistProgress(flush = true)
        // If the queue is missing a file or exceeds the bounded playlist cap,
        // preserve the existing per-track fallback rather than dropping songs.
        val timeline = preparedOfflineTimeline(localLibrary, id)
        val item = MediaItem.Builder().setMediaId("offline:" + song.id)
            .setUri(Uri.fromFile(file)).setMimeType(song.mimeType)
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle(song.title).setArtist("Offline · NightWave").build())
            .build()
        if (!offlineSession.saveSelection(offlineQueue.snapshot(), id, resumePositionMs)) {
            // Playback may still proceed, but no false promise of durable resume.
            Log.w("NightWavePlayback", "Could not save offline listening checkpoint")
        }
        offlinePlaying = true
        offlineGaplessActive = timeline != null
        generation++ // Cancel unresolved online audio from a previous song.
        cancelPendingResolution()
        autoRetriedVideo = null
        player.apply {
            pause()
            if (timeline != null) {
                // One prepared ExoPlayer timeline allows Media3 to advance
                // locally without STATE_ENDED -> pause -> setMediaItem -> prepare.
                setMediaItems(timeline.first, timeline.second, resumePositionMs.coerceAtLeast(0L))
            } else {
                setMediaItem(item)
            }
            // Offline speed applies to every item, not to public streams.
            setPlaybackSpeed(OfflinePlaybackSpeedPolicy.speed(offlineSpeedStore.read()))
            playWhenReady = true
            prepare()
            if (timeline == null && resumePositionMs > 0L) seekTo(resumePositionMs)
            play()
        }
        scheduleCrossfadeCheck()
        return true
    }

    private fun advanceOffline(forward: Boolean): Boolean {
        cancelCrossfade()
        val player = exoPlayer
        if (offlineGaplessActive && player != null) {
            // The timeline already owns validated local media items. Avoid
            // per-track teardown/re-prepare on manual Next and Previous.
            val next = if (forward) player.currentMediaItemIndex + 1
                else player.currentMediaItemIndex - 1
            if (next !in 0 until player.mediaItemCount) return false
            val id = offlineQueue.snapshot().getOrNull(next) ?: return false
            if (player.getMediaItemAt(next).mediaId != "offline:$id") return false
            player.seekTo(next, 0L)
            player.play()
            return true
        }
        // One private-library facade per transition. Revalidate every ID/file;
        // missing/deleted files are skipped in a bounded pass, never downloaded.
        val localLibrary = OfflineAudioLibrary(this)
        repeat(offlineQueue.size) {
            val id = (if (forward) offlineQueue.next() else offlineQueue.previous())
                ?: return false
            if (playOfflineId(id, localLibrary)) return true
        }
        return false
    }

    private fun playNext(automatic: Boolean) {
        // Automatic track advancement isn't an Intent; it must also revoke
        // any undo tied to the old playback position.
        pendingQueueRemoval = null
        if (offlinePlaying) {
            advanceOffline(forward = true)
            return
        }
        val player = exoPlayer
        val repeating = queue.current
        if (automatic && queue.repeat == PlaybackQueue.Repeat.ONE &&
            PlaybackResolutionPolicy.reusesLoadedTrack(
                repeating?.url, player?.currentMediaItem?.mediaId, player?.playerError != null)) {
            // Repeat-one should not launch a whole fresh extractor request.
            // A genuinely expired source still uses the existing one-refresh
            // onPlayerError path. Manual Next obeys the queue's normal rules.
            savedQueue.saveBookmark(repeating!!.url, 0L, 0L)
            player?.apply {
                seekTo(0L)
                prepare()
                playWhenReady = true
                play()
            }
            return
        }
        queue.next(automatic)?.let { savedQueue.save(queue); resolveAndPlay(it) }
    }

    private fun playPrevious() {
        cancelCrossfade()
        val player = exoPlayer
        // Music-player convention: Previous first restarts a song if >3 seconds in.
        if (player != null && player.currentPosition > 3_000L &&
            (offlinePlaying || player.currentMediaItem?.mediaId == queue.current?.url)) {
            player.seekTo(0L)
            if (offlinePlaying) offlineQueue.current?.let {
                offlineSession.savePosition(it, 0L, flush = true)
            }
            if (!offlinePlaying) queue.current?.let {
                savedQueue.saveBookmark(it.url, 0L,
                    player.duration.takeIf { duration -> duration > 0L && duration != C.TIME_UNSET } ?: 0L)
            }
            return
        }
        if (offlinePlaying) {
            if (!advanceOffline(forward = false)) {
                player?.seekTo(0L)
                offlineQueue.current?.let { offlineSession.savePosition(it, 0L, flush = true) }
            }
            return
        }
        queue.previous()?.let {
            pendingQueueRemoval = null
            savedQueue.save(queue)
            resolveAndPlay(it)
        }
    }

    private fun resolveAndPlay(
        entry: QueueEntry, autoPlay: Boolean = true,
        resumePositionMs: Long = 0L, recoveryAttempt: Boolean = false
    ) {
        cancelCrossfade()
        // Flush a local checkpoint before changing playback mode or clearing
        // its MediaItem. Otherwise the last 2.5-second progress tick could be lost.
        if (offlinePlaying) persistProgress(flush = true)
        offlinePlaying = false
        offlineGaplessActive = false
        offlineQueue.reset()
        if (!recoveryAttempt) autoRetriedVideo = null
        savedQueue.clearFailure()
        cancelPendingResolution()
        // Drop expired previous stream metadata immediately; the UI can display
        // the current queue entry during fresh extraction instead of a stale song.
        exoPlayer?.apply {
            stop()
            clearMediaItems()
            // Never apply offline speed preference to public stream playback.
            setPlaybackSpeed(1f)
            // Keep pending user intent observable by Media3 Play/Pause.
            // A Pause during extraction must not be reversed at completion.
            playWhenReady = autoPlay
        }
        if (autoPlay && resumePositionMs == 0L) savedQueue.saveBookmark(entry.url, 0L, 0L)
        val request = ++generation
        resolvingPublicUrl = entry.url
        savedQueue.markLookup(entry.url)
        if (resolver.isShutdown) {
            cancelPendingResolution()
            savedQueue.recordFailure(entry.url, "Playback is stopping. Open the app and Retry.")
            return
        }
        val startElapsedMs = SystemClock.elapsedRealtime()
        val timeout = Runnable {
            if (PlaybackResolutionPolicy.acceptsResult(request, generation,
                    entry.url, queue.current?.url, offlinePlaying) &&
                resolvingPublicUrl == entry.url &&
                PlaybackResolutionPolicy.timedOut(startElapsedMs, SystemClock.elapsedRealtime())) {
                generation++ // future resolution is now invalid
                cancelPendingResolution()
                exoPlayer?.playWhenReady = false
                savedQueue.recordFailure(entry.url,
                    "Audio lookup took too long. Tap Retry or choose another song.")
                Log.w("NightWavePlayback", "Stream extraction timed out; waiting for user retry")
            }
        }
        resolveDeadline = timeout
        main.postDelayed(timeout, PlaybackResolutionPolicy.RESOLUTION_TIMEOUT_MS)
        try {
            // submit() returns a cancellable Future; the bounded worker queue
            // avoids serial head-of-line delay from a superseded extraction.
            resolveFuture = resolver.submit(Runnable {
                if (Thread.currentThread().isInterrupted) return@Runnable
                val outcome = runCatching { gateway.resolve(entry.url) }
                main.post {
                    if (!PlaybackResolutionPolicy.acceptsResult(request, generation,
                            entry.url, queue.current?.url, offlinePlaying) ||
                        exoPlayer == null) return@post
                    cancelPendingResolution()
                    outcome.onSuccess { playable ->
                    val video = playable.video
                    if (!PlaybackRecoveryPolicy.acceptsResolvedTrack(entry.url, video.url)) {
                        // A provider redirect or malformed extractor result may
                        // describe another track. Never replace the selected
                        // song with it, regardless of callback generation.
                        exoPlayer?.playWhenReady = false
                        savedQueue.recordFailure(entry.url,
                            "Audio identity could not be verified. Tap Retry.")
                        Log.w("NightWavePlayback", "Stream identity mismatch; result discarded")
                        return@onSuccess
                    }
                    savedQueue.clearFailure()
                    if (autoPlay) runCatching { MusicLibrary(this).addRecent(video) }
                    val metadata = MediaMetadata.Builder().setTitle(video.title).setArtist(video.author)
                        .apply { if (video.thumbnail.isNotEmpty()) setArtworkUri(Uri.parse(video.thumbnail)) }
                        .build()
                    val mediaItem = MediaItem.Builder().setMediaId(video.url).setUri(playable.audioUrl)
                        .apply { playable.mimeType?.let { setMimeType(it) } }
                        .setMediaMetadata(metadata).build()
                    exoPlayer?.apply {
                        val shouldPlay = autoPlay && playWhenReady
                        setMediaItem(mediaItem)
                        if (resumePositionMs > 0L) seekTo(resumePositionMs)
                        playWhenReady = shouldPlay
                        prepare()
                        if (shouldPlay) play()
                    }
                }
                // An unavailable public stream is not a reason to replay an old song.
                    outcome.onFailure {
                        Log.w("NightWavePlayback", "Stream extraction failed: " +
                            it.javaClass.simpleName)
                        savedQueue.recordFailure(entry.url,
                            "No playable audio was available. Retry or select another track.")
                        exoPlayer?.pause()
                    }
                }
            })
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            generation++
            cancelPendingResolution()
            exoPlayer?.playWhenReady = false
            savedQueue.recordFailure(entry.url,
                "Music lookup is busy. Tap Retry in a moment.")
            // Bounded workers are occupied; fail visibly, never queue indefinitely.
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        cancelCrossfade()
        clearSleepTimer()
        sampleListening(endSession = true)
        persistProgress(flush = true)
        main.removeCallbacks(progressWriter)
        generation++
        cancelPendingResolution()
        resolver.shutdownNow()
        listeningWriter.shutdown() // drain pending bounded, consent-fenced writes
        exoPlayer?.removeListener(trackListener)
        releaseEqualizer()
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
}
