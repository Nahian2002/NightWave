package com.nightwave.player.playback

import android.media.audiofx.Equalizer
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.Bundle
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
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.LibraryResult
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.nightwave.player.auto.AutoBrowseCatalog
import com.nightwave.player.auto.AutoBrowsePolicy
import com.nightwave.player.auto.AutoPlaySelection
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.nightwave.player.widget.NightWaveWidgetProvider
import com.nightwave.player.widget.NightWaveWidgetController
import com.nightwave.player.widget.WidgetPlaybackSnapshot
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.net.YouTubeGateway
import com.nightwave.player.offline.OfflineAudioLibrary
import com.nightwave.player.offline.OfflinePlaybackQueue
import com.nightwave.player.offline.OfflineGaplessPolicy
import com.nightwave.player.offline.OfflinePlaybackPreferencesStore
import com.nightwave.player.offline.OfflineSessionStore
import com.nightwave.player.v2.ui.NightWaveStudioActivity
import java.util.concurrent.Executors
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.Future

/** The service owns automatic next-track resolution, even while the Activity is stopped. */
@UnstableApi
class PlaybackService : MediaLibraryService() {
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
        const val ACTION_PLAY_NEXT = "com.nightwave.player.PLAY_NEXT"
        const val ACTION_REMOVE_UPCOMING = "com.nightwave.player.REMOVE_UPCOMING"
        const val ACTION_UNDO_REMOVE_UPCOMING = "com.nightwave.player.UNDO_REMOVE_UPCOMING"
        const val EXTRA_QUEUE_TOKEN = "queue_token"
        const val EXTRA_EXPECTED_URL = "expected_queue_url"
        const val ACTION_TOGGLE_OFFLINE_PLAY_PAUSE = "com.nightwave.player.TOGGLE_OFFLINE_PLAY_PAUSE"
        const val ACTION_NEXT = "com.nightwave.player.NEXT"
        const val ACTION_PREVIOUS = "com.nightwave.player.PREVIOUS"
        const val ACTION_JUMP = "com.nightwave.player.JUMP"
        const val ACTION_SHUFFLE = "com.nightwave.player.SHUFFLE"
        const val EXTRA_SHUFFLE_ENABLED = "shuffle_enabled"
        const val EXTRA_SHUFFLE_REQUEST_ID = "shuffle_request_id"
        const val ACTION_REPEAT = "com.nightwave.player.REPEAT"
        const val EXTRA_REPEAT_MODE = "repeat_mode"
        const val EXTRA_REPEAT_REQUEST_ID = "repeat_request_id"
        const val ACTION_SLEEP_TIMER_SET = "com.nightwave.player.SLEEP_TIMER_SET"
        const val ACTION_SLEEP_TIMER_CANCEL = "com.nightwave.player.SLEEP_TIMER_CANCEL"
        const val ACTION_OFFLINE_SPEED_SET = "com.nightwave.player.OFFLINE_SPEED_SET"
        const val ACTION_CROSSFADE_SET = "com.nightwave.player.CROSSFADE_SET"
        const val ACTION_NORMALIZATION_SET = "com.nightwave.player.NORMALIZATION_SET"
        const val EXTRA_NORMALIZATION_ENABLED = "normalization_enabled"
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

    private var mediaSession: MediaLibrarySession? = null
    private val carCatalog by lazy { AutoBrowseCatalog(this) }
    private var exoPlayer: ExoPlayer? = null

    /** Widgets mirror playback events only, never a high-frequency position timer. */
    private fun publishWidgetState() {
        val player = exoPlayer
        val item = player?.currentMediaItem
        NightWaveWidgetProvider.publish(this, WidgetPlaybackSnapshot(
            hasTrack = item != null,
            isPlaying = player?.isPlaying == true,
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            artist = item?.mediaMetadata?.artist?.toString().orEmpty()
        ))
    }
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
        if (requestedEqPreset == 0) { releaseEqualizer(); return }
        if (sessionId <= 0) {
            releaseEqualizer()
            eqStatus.write(requestedEqPreset, false,
                "Waiting for an active audio session. Start a song first.")
            return
        }
        if (activeEqualizer != null && equalizerSessionId == sessionId) return
        releaseEqualizer()
        var candidate: Equalizer? = null
        try {
            candidate = Equalizer(0, sessionId)
            eqCapabilities.publish(candidate, sessionId)
            // Android effects are shared per audio session and another effect
            // may own this engine. Never report success without control.
            if (!candidate.hasControl()) {
                eqStatus.write(requestedEqPreset, false,
                    "Equalizer unavailable: another effect controls this audio session.")
                return
            }
            if (requestedEqPreset == 21) {
                // Manual mode: range and band count come from this exact device/session.
                val range = candidate.bandLevelRange
                for ((index, level) in manualLevels) {
                    if (index >= candidate.numberOfBands.toInt() || level < range[0] || level > range[1]) {
                        eqStatus.write(requestedEqPreset, false,
                            "Manual equalizer settings are unsupported by this audio session.")
                        return
                    }
                    candidate.setBandLevel(index.toShort(), level)
                }
            } else {
                val presetIndex = requestedEqPreset - 1
                if (presetIndex !in 0 until candidate.numberOfPresets.toInt()) {
                    eqStatus.write(requestedEqPreset, false,
                        "This equalizer preset is unavailable on the active device.")
                    return
                }
                candidate.usePreset(presetIndex.toShort())
            }
            candidate.enabled = true
            if (!candidate.enabled) {
                eqStatus.write(requestedEqPreset, false,
                    "Android audio effects could not be enabled on this session.")
                return
            }
            activeEqualizer = candidate
            equalizerSessionId = sessionId
            // Re-read the actual preset/manual band gains after applying the
            // effect, so Sound Studio's response curve is not invented.
            runCatching { eqCapabilities.publish(candidate, sessionId) }
            // Enabled is a platform API acknowledgement, NOT a claim that
            // every Bluetooth/output route audibly applies the effect.
            eqStatus.write(requestedEqPreset, true,
                "Enabled on playback session. Audibility depends on audio route.")
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
    private lateinit var normalizationStore: OfflineNormalizationStore
    private var normalizationEnabled = false
    private val normalizationGains = mutableMapOf<String, Float>()
    private val normalizationPending = mutableSetOf<String>()
    private val normalizationUnsupported = mutableSetOf<String>()
    private val normalizationWorker = ThreadPoolExecutor(
        1, 1, 30L, TimeUnit.SECONDS, ArrayBlockingQueue<Runnable>(1),
        ThreadPoolExecutor.AbortPolicy()
    )
    private var normalizationEpoch = 0
    private var normalizationRampEpoch = 0

    /** One volume owner for both ordinary playback and the two crossfade decks. */
    private fun normalizedMaster(mediaId: String?): Float {
        val percent = if (::listeningLevelStore.isInitialized)
            listeningLevelStore.read() else ListeningLevelPolicy.DEFAULT_PERCENT
        val listening = ListeningLevelPolicy.gain(percent)
        val id = mediaId?.takeIf { it.startsWith("offline:") }?.removePrefix("offline:")
        val reduction = OfflineNormalizationPolicy.gain(
            normalizationEnabled && offlinePlaying && id != null,
            if (id != null) normalizationGains[id] else null
        )
        return OfflineNormalizationPolicy.combined(listening, reduction)
    }

    private fun applyCurrentMaster() {
        normalizationRampEpoch++ // cancel any pending measurement ramp
        if (crossfadeStartedAtMs > 0L) return // fade tick controls both decks
        exoPlayer?.volume = normalizedMaster(exoPlayer?.currentMediaItem?.mediaId)
    }

    /** First-time analysis gently reduces loudness instead of stepping abruptly. */
    private fun smoothlyApplyMeasuredMaster() {
        val player = exoPlayer ?: return
        if (crossfadeStartedAtMs > 0L) return
        val id = player.currentMediaItem?.mediaId ?: return
        val from = player.volume
        val target = normalizedMaster(id)
        val token = ++normalizationRampEpoch
        if (kotlin.math.abs(from - target) < .01f) {
            player.volume = target
            return
        }
        for (step in 1..6) {
            main.postDelayed({
                if (token == normalizationRampEpoch && exoPlayer === player &&
                    player.currentMediaItem?.mediaId == id &&
                    crossfadeStartedAtMs == 0L) {
                    player.volume = (from + (target - from) * (step / 6f)).coerceIn(0f, 1f)
                }
            }, step * 40L)
        }
    }

    /**
     * A bounded decoder/reader worker. No filesystem scan or PCM processing
     * runs on MediaSession callbacks or the UI thread.
     */
    private fun scheduleNormalization(id: String?) {
        if (!normalizationEnabled || !offlinePlaying || id.isNullOrBlank() ||
            normalizationGains.containsKey(id) || normalizationUnsupported.contains(id) ||
            !normalizationPending.add(id)) return
        val token = normalizationEpoch
        try {
            normalizationWorker.execute {
                val result = runCatching {
                    val library = OfflineAudioLibrary(this)
                    val song = library.find(id)
                    val file = song?.let { library.fileFor(it) }
                    if (song == null || file == null) null else {
                        normalizationStore.savedGain(id, song.bytes) ?: run {
                            val measured = OfflineLoudnessAnalyzer().measure(file)
                            if (measured != null)
                                normalizationStore.saveGain(id, song.bytes, measured)
                            measured
                        }
                    }
                }.getOrNull()
                main.post {
                    normalizationPending.remove(id)
                    if (token != normalizationEpoch || !normalizationEnabled) return@post
                    if (result != null) {
                        normalizationGains[id] = result
                        if (exoPlayer?.currentMediaItem?.mediaId == "offline:$id")
                            smoothlyApplyMeasuredMaster()
                    } else {
                        // Unsupported/undecodable files remain playable at unity;
                        // don't burn CPU repeating the same failed analysis.
                        normalizationUnsupported.add(id)
                    }
                }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            normalizationPending.remove(id) // never queue unbounded analysis
        }
    }

    private fun normalizeSelectedAndNext() {
        val ids = offlineQueue.snapshot()
        val index = offlineQueue.index
        scheduleNormalization(ids.getOrNull(index))
        scheduleNormalization(ids.getOrNull(index + 1))
    }

    private val crossfadeTick = object : Runnable {
        override fun run() {
            val player = exoPlayer ?: return
            if (!offlinePlaying || !offlineGaplessActive ||
                offlineQueue.shuffle || offlineQueue.repeat == PlaybackQueue.Repeat.ONE ||
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
                    val (oldGain, newGain) = CrossfadePolicy.gains(progress, 1f)
                    player.volume = oldGain *
                        normalizedMaster(player.currentMediaItem?.mediaId)
                    incoming.volume = newGain *
                        normalizedMaster(incoming.currentMediaItem?.mediaId)
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
            !offlineQueue.shuffle && offlineQueue.repeat != PlaybackQueue.Repeat.ONE &&
            exoPlayer?.isPlaying == true) main.post(crossfadeTick)
    }

    private fun prepareCrossfadeDeck(outgoing: ExoPlayer, nextIndex: Int) {
        val id = offlineQueue.snapshot().getOrNull(nextIndex) ?: return
        if (nextIndex !in 0 until outgoing.mediaItemCount ||
            outgoing.getMediaItemAt(nextIndex).mediaId != "offline:$id") return
        val library = OfflineAudioLibrary(this)
        val indexed = runCatching { library.find(id) }.getOrNull() ?: return
        if (library.fileFor(indexed) == null) return // no unchecked path preload
        scheduleNormalization(id)
        var candidate: ExoPlayer? = null
        try {
            candidate = ExoPlayer.Builder(this)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                setWakeMode(C.WAKE_MODE_LOCAL)
                volume = 0f
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA).build(), false)
                setMediaItems((0 until outgoing.mediaItemCount).map(outgoing::getMediaItemAt),
                    nextIndex, 0L)
                repeatMode = outgoing.repeatMode
                shuffleModeEnabled = false // overlap currently excludes shuffle
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
        applyCurrentMaster()
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
        publishWidgetState()
        applyEqualizer(new.audioSessionId)
        new.volume = normalizedMaster(new.currentMediaItem?.mediaId)
        if (offlineQueue.jump(index) != null) {
            offlineSession.saveSelection(ids, id, new.currentPosition.coerceAtLeast(0L))
        }
        normalizeSelectedAndNext()
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
    private lateinit var offlineControls: OfflinePlaybackPreferencesStore
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
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            publishWidgetState()
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
            publishWidgetState()
        }

        override fun onPlaybackStateChanged(state: Int) {
            sampleListening()
            publishWidgetState()
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
                                if (!advanceOffline(forward = true, automatic = true)) {
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
            publishWidgetState()
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
                    automatic = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
                    shuffled = offlineQueue.shuffle,
                    repeatAll = offlineQueue.repeat == PlaybackQueue.Repeat.ALL)
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
                if (offlinePlaying && mediaItem?.mediaId ==
                    "offline:" + offlineQueue.current) {
                    offlineSession.savePosition(offlineQueue.current!!, 0L)
                }
            }
            sampleListening()
            if (!crossfadeHandoffInProgress) {
                // Public tracks must also restore unity/Listening Level after
                // leaving a normalized private file.
                applyCurrentMaster()
                if (offlinePlaying) normalizeSelectedAndNext()
            }
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
        normalizationStore = OfflineNormalizationStore(this)
        normalizationEnabled = normalizationStore.enabled()
        crossfadeSettings = CrossfadeSettingsStore(this)
        crossfadeSeconds = crossfadeSettings.read()
        // A process/service restart never silently resurrects an old timer.
        // Playback itself also requires an explicit user resume after restart.
        clearSleepTimer()
        eqCapabilities.clear()
        eqStatus.write(0, false, "Off")
        savedQueue.clearLookup() // process restart must not leave a ghost spinner
        offlineSession = OfflineSessionStore(this)
        offlineControls = OfflinePlaybackPreferencesStore(this)
        offlineQueue.setShuffle(offlineControls.shuffle())
        offlineQueue.setRepeat(offlineControls.repeat())
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
            .setHandleAudioBecomingNoisy(true)
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
        mediaSession = MediaLibrarySession.Builder(this, player,
            object : MediaLibrarySession.Callback {
                // Car and MediaBrowser catalog callbacks never fetch network
                // media, start playback, read PCM or edit saved library content.
                override fun onGetLibraryRoot(
                    session: MediaLibrarySession,
                    browser: MediaSession.ControllerInfo,
                    params: LibraryParams?
                ): ListenableFuture<LibraryResult<MediaItem>> =
                    Futures.immediateFuture(LibraryResult.ofItem(
                        carCatalog.item(AutoBrowsePolicy.ROOT)!!, params))

                override fun onGetChildren(
                    session: MediaLibrarySession,
                    browser: MediaSession.ControllerInfo,
                    parentId: String, page: Int, pageSize: Int,
                    params: LibraryParams?
                ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                    val items = runCatching {
                        when {
                            // A car requesting offline-only content should never
                            // be shown public tracks as available without data.
                            params?.isOffline == true && parentId == AutoBrowsePolicy.ROOT ->
                                listOfNotNull(carCatalog.item(AutoBrowsePolicy.IMPORTED))
                            params?.isOffline == true && parentId != AutoBrowsePolicy.IMPORTED ->
                                emptyList()
                            else -> carCatalog.children(parentId)
                        }
                    }.getOrNull()
                        ?: return Futures.immediateFuture(
                            LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                    val selected = AutoBrowsePolicy.page(items, page, pageSize)
                    return Futures.immediateFuture(
                        LibraryResult.ofItemList(ImmutableList.copyOf(selected), params))
                }

                override fun onGetItem(
                    session: MediaLibrarySession,
                    browser: MediaSession.ControllerInfo,
                    mediaId: String
                ): ListenableFuture<LibraryResult<MediaItem>> {
                    val item = runCatching { carCatalog.item(mediaId) }.getOrNull()
                        ?: return Futures.immediateFuture(
                            LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                    return Futures.immediateFuture(LibraryResult.ofItem(item, null))
                }

                override fun onSearch(
                    session: MediaLibrarySession,
                    browser: MediaSession.ControllerInfo,
                    query: String, params: LibraryParams?
                ): ListenableFuture<LibraryResult<Void>> {
                    val count = runCatching {
                        val results = carCatalog.search(query)
                        if (params?.isOffline == true)
                            results.count { it.mediaId.startsWith(AutoBrowsePolicy.OFFLINE_PREFIX) }
                        else results.size
                    }.getOrDefault(0)
                    session.notifySearchResultChanged(browser, query, count, params)
                    return Futures.immediateFuture(LibraryResult.ofVoid())
                }

                override fun onGetSearchResult(
                    session: MediaLibrarySession,
                    browser: MediaSession.ControllerInfo,
                    query: String, page: Int, pageSize: Int,
                    params: LibraryParams?
                ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                    val items = runCatching { carCatalog.search(query) }.getOrDefault(emptyList())
                        .let { results ->
                            if (params?.isOffline == true) results.filter {
                                it.mediaId.startsWith(AutoBrowsePolicy.OFFLINE_PREFIX)
                            } else results
                        }
                    return Futures.immediateFuture(LibraryResult.ofItemList(
                        ImmutableList.copyOf(AutoBrowsePolicy.page(items, page, pageSize)),
                        params))
                }

                /**
                 * Car selection must flow through NightWave's existing queue
                 * rather than letting Media3 directly play a YouTube page URL.
                 * Post after the controller's empty setMediaItems response to
                 * prevent that response clearing the newly prepared timeline.
                 */
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: List<MediaItem>
                ): ListenableFuture<List<MediaItem>> {
                    val requestedId = mediaItems.singleOrNull()?.mediaId
                    val selection = requestedId?.let {
                        runCatching { carCatalog.selection(it) }.getOrNull()
                    }
                    if (selection != null) main.post {
                        pendingQueueRemoval = null
                        when (selection) {
                            is AutoPlaySelection.Offline -> {
                                val local = OfflineAudioLibrary(this@PlaybackService)
                                if (offlineQueue.select(selection.queueIds, selection.songId) != null)
                                    playOfflineId(selection.songId, local)
                            }
                            is AutoPlaySelection.Streaming -> {
                                queue.replace(selection.queue, selection.index)
                                queue.useExplicitDisplayOrder()
                                savedQueue.save(queue)
                                queue.current?.let { resolveAndPlay(it) }
                            }
                        }
                    }
                    // Reject unowned/URI-only IDs. Our explicit service-owned
                    // player transition replaces this empty controller timeline.
                    return Futures.immediateFuture(emptyList())
                }


                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    // Existing car, headset and notification controllers retain
                    // the default permitted commands. Custom queue commands are
                    // available only to NightWave's own widget controller.
                    if (controller.packageName != packageName)
                        return super.onConnect(session, controller)
                    val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(SessionCommand(NightWaveWidgetController.COMMAND_NEXT, Bundle.EMPTY))
                        .add(SessionCommand(NightWaveWidgetController.COMMAND_PREVIOUS, Bundle.EMPTY))
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(commands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    if (controller.packageName != packageName) {
                        return Futures.immediateFuture(
                            SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
                    }
                    val result = when (customCommand.customAction) {
                        NightWaveWidgetController.COMMAND_NEXT -> {
                            playNext(false)
                            SessionResult.RESULT_SUCCESS
                        }
                        NightWaveWidgetController.COMMAND_PREVIOUS -> {
                            playPrevious()
                            SessionResult.RESULT_SUCCESS
                        }
                        else -> SessionResult.RESULT_ERROR_NOT_SUPPORTED
                    }
                    return Futures.immediateFuture(SessionResult(result))
                }

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
            }).setSessionActivity(openApp).build()
        // The saved queue, artwork and bookmark are available immediately from
        // QueueStateStore. Do NOT issue a network request on service creation.
        // A MediaController may bind repeatedly while Android recovers an app.
        // Only explicit Play / RESUME_SAVED is allowed to resolve streaming URLs.
        publishWidgetState()
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
                    if (!QueueRestorePolicy.isCanonicalPublicWatchUrl(url))
                        return@mapIndexedNotNull null
                    QueueEntry(url, titles.getOrNull(index).orEmpty(), creators.getOrNull(index).orEmpty(), artworks.getOrNull(index).orEmpty())
                }
                val originalSelection = intent.getIntExtra(EXTRA_INDEX, 0)
                // Positions belong to the unfiltered original Intent array.
                // An invalid selected row must never autoplay some other song.
                val selected = if (urls.isEmpty()) {
                    0 // A deliberately empty replacement still clears the queue.
                } else {
                    PublicQueueIntentPolicy.retainedSelectionIndex(
                        urls.take(100), originalSelection, entries)
                        ?: return result
                }
                val explicitDisplayOrder = intent.action == ACTION_PLAY_QUEUE &&
                    intent.getBooleanExtra(EXTRA_QUEUE_DISPLAY_ORDER, false)
                val proposedQueue = entries.distinctBy { it.url }.take(100)
                val resolvedSelectionIndex =
                    PublicQueueSelectionPolicy.selectedIndex(entries, proposedQueue, selected)
                val selectedEntry = proposedQueue.getOrNull(resolvedSelectionIndex)
                val sameQueue = intent.action == ACTION_PLAY_QUEUE && !offlinePlaying &&
                    proposedQueue.isNotEmpty() && queue.snapshot() == proposedQueue &&
                    queue.index == resolvedSelectionIndex
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
            ACTION_ENQUEUE, ACTION_PLAY_NEXT -> {
                // Never enqueue an online item behind privately imported audio.
                // Keep the two playback modes isolated and the current song intact.
                if (!offlinePlaying) {
                    val url = intent.getStringExtra(EXTRA_URLS)
                    if (url != null && QueueRestorePolicy.isCanonicalPublicWatchUrl(url)) {
                        val entry = QueueEntry(url,
                            intent.getStringExtra(EXTRA_TITLES).orEmpty(),
                            intent.getStringExtra(EXTRA_CREATORS).orEmpty(),
                            intent.getStringExtra(EXTRA_ARTWORKS).orEmpty())
                        val wasEmpty = queue.current == null
                        val applied = if (intent.action == ACTION_PLAY_NEXT)
                            queue.insertNext(entry) else queue.add(entry)
                        if (applied) {
                            savedQueue.save(queue)
                            if (wasEmpty && intent.action == ACTION_PLAY_NEXT) {
                                // Play Next on an empty queue behaves like Play Now.
                                offlineQueue.reset()
                                queue.current?.let { resolveAndPlay(it) }
                            }
                        }
                    }
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
                val sessionId = exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
                val availablePresets = eqCapabilities.read().takeIf {
                    it.sessionReady && it.audioSessionId == sessionId
                }?.presetNames?.size ?: 0
                // Off is always permitted. All other values must correspond
                // to a preset advertised by this exact active audio session.
                if (preset == 0 || (preset in 1..availablePresets && sessionId > 0)) {
                    requestedEqPreset = preset
                    // Applying a different preset to an unchanged audio session
                    // previously returned early and silently kept the old effect.
                    releaseEqualizer()
                    applyEqualizer(sessionId)
                } else eqStatus.write(requestedEqPreset, activeEqualizer?.enabled == true,
                    "Rejected: start playback and refresh supported presets.")
            }
            ACTION_EQ_DISCOVER -> {
                val session = exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
                if (session > 0) {
                    var probe: Equalizer? = null
                    try {
                        probe = activeEqualizer ?: Equalizer(0, session)
                        eqCapabilities.publish(probe, session)
                        // On a cold app launch the old SharedPreferences receipt
                        // can still say "active"; the new service has no effect.
                        val activeOnSession = activeEqualizer === probe &&
                            equalizerSessionId == session && probe.enabled &&
                            probe.hasControl()
                        eqStatus.write(requestedEqPreset, activeOnSession,
                            if (activeOnSession)
                                "Enabled on playback session. Audibility depends on audio route."
                            else "Equalizer off. Select an available device preset.")
                    } catch (_: Exception) {
                        eqCapabilities.clear()
                        eqStatus.write(requestedEqPreset, false,
                            "No supported equalizer available on this audio session.")
                    } finally {
                        if (probe !== activeEqualizer) runCatching { probe?.release() }
                    }
                } else {
                    eqCapabilities.clear()
                    eqStatus.write(requestedEqPreset, false,
                        "No active audio session. Play a song to enable the equalizer.")
                }
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
            ACTION_NORMALIZATION_SET -> {
                val enabled = intent.getBooleanExtra(EXTRA_NORMALIZATION_ENABLED, false)
                if (normalizationStore.setEnabled(enabled)) {
                    normalizationEnabled = enabled
                    normalizationEpoch++
                    normalizationGains.clear()
                    normalizationPending.clear()
                    normalizationUnsupported.clear()
                    if (enabled) normalizeSelectedAndNext()
                    applyCurrentMaster()
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
                        applyCurrentMaster()
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
            ACTION_TOGGLE_OFFLINE_PLAY_PAUSE -> {
                // Offline Library mini-controls never start an online resolve.
                // A saved offline item must already be active in Media3.
                val audio = exoPlayer
                if (offlinePlaying && audio?.currentMediaItem != null) {
                    if (audio.isPlaying) audio.pause() else audio.play()
                    publishWidgetState()
                }
            }
            ACTION_NEXT -> playNext(false)
            ACTION_PREVIOUS -> playPrevious()
            ACTION_JUMP -> if (!offlinePlaying) queue.jump(intent.getIntExtra(EXTRA_INDEX, -1))?.let { savedQueue.save(queue); resolveAndPlay(it) }
            ACTION_SHUFFLE -> if (offlinePlaying) {
                val explicit = intent.hasExtra(EXTRA_SHUFFLE_ENABLED)
                val enabled = if (explicit)
                    intent.getBooleanExtra(EXTRA_SHUFFLE_ENABLED, false) else !offlineQueue.shuffle
                if (offlineControls.setShuffle(enabled)) {
                    cancelCrossfade()
                    offlineQueue.setShuffle(enabled)
                    if (offlineGaplessActive) exoPlayer?.shuffleModeEnabled = enabled
                    scheduleCrossfadeCheck()
                }
            } else {
                // New UI sends absolute state; preserve legacy toggle only when
                // no explicit value was supplied. Rapid taps cannot be inverted
                // by delayed reads of the previously persisted queue.
                val explicit = intent.hasExtra(EXTRA_SHUFFLE_ENABLED)
                val enabled = ShuffleModePolicy.resolve(
                    queue.shuffle, explicit,
                    intent.getBooleanExtra(EXTRA_SHUFFLE_ENABLED, false))
                queue.shuffle = enabled
                savedQueue.save(queue)
                val requestId = intent.getStringExtra(EXTRA_SHUFFLE_REQUEST_ID).orEmpty()
                if (requestId.isNotBlank() && savedQueue.read().shuffle == enabled) {
                    ShuffleModeReceiptStore(this).acknowledge(requestId, enabled)
                }
            }
            ACTION_REPEAT -> if (offlinePlaying) {
                val mode = RepeatModePolicy.resolve(offlineQueue.repeat,
                    intent.hasExtra(EXTRA_REPEAT_MODE),
                    intent.getStringExtra(EXTRA_REPEAT_MODE))
                if (mode != null && offlineControls.setRepeat(mode)) {
                    cancelCrossfade()
                    offlineQueue.setRepeat(mode)
                    applyOfflinePlaybackModes()
                    scheduleCrossfadeCheck()
                }
            } else {
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

    /** A single local item cannot represent a larger fallback queue's Repeat All. */
    private fun applyOfflinePlaybackModes() {
        if (!offlinePlaying) return
        val player = exoPlayer ?: return
        player.shuffleModeEnabled = offlineGaplessActive && offlineQueue.shuffle
        player.repeatMode = when (offlineQueue.repeat) {
            PlaybackQueue.Repeat.ONE -> Player.REPEAT_MODE_ONE
            PlaybackQueue.Repeat.ALL -> if (offlineGaplessActive || offlineQueue.size == 1)
                Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            PlaybackQueue.Repeat.OFF -> Player.REPEAT_MODE_OFF
        }
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
            applyOfflinePlaybackModes()
            // Offline speed applies to every item, not to public streams.
            setPlaybackSpeed(OfflinePlaybackSpeedPolicy.speed(offlineSpeedStore.read()))
            playWhenReady = true
            prepare()
            if (timeline == null && resumePositionMs > 0L) seekTo(resumePositionMs)
            play()
        }
        normalizeSelectedAndNext()
        applyCurrentMaster()
        scheduleCrossfadeCheck()
        return true
    }

    private fun advanceOffline(forward: Boolean, automatic: Boolean = false): Boolean {
        cancelCrossfade()
        val player = exoPlayer
        if (offlineGaplessActive && player != null) {
            // The timeline already owns validated local media items. Avoid
            // per-track teardown/re-prepare on manual Next and Previous.
            val next = if (forward) player.nextMediaItemIndex
                else player.previousMediaItemIndex
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
            val id = (if (forward) offlineQueue.next(automatic) else offlineQueue.previous())
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
            advanceOffline(forward = true, automatic = automatic)
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
        exoPlayer?.apply {
            shuffleModeEnabled = false
            repeatMode = Player.REPEAT_MODE_OFF
        }
        normalizationEpoch++
        normalizationPending.clear()
        offlineQueue.reset()
        applyCurrentMaster() // clear offline attenuation before public source resolution
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onDestroy() {
        NightWaveWidgetProvider.publish(this, WidgetPlaybackSnapshot())
        cancelCrossfade()
        clearSleepTimer()
        sampleListening(endSession = true)
        persistProgress(flush = true)
        main.removeCallbacks(progressWriter)
        generation++
        cancelPendingResolution()
        resolver.shutdownNow()
        normalizationEpoch++
        normalizationRampEpoch++
        normalizationWorker.shutdownNow()
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
