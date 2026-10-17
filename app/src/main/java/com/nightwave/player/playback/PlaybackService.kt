package com.nightwave.player.playback

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
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            sampleListening()
            main.removeCallbacks(progressWriter)
            if (isPlaying) main.postDelayed(progressWriter, 2500L)
            else persistProgress(flush = true)
        }

        override fun onPlaybackStateChanged(state: Int) {
            sampleListening()
            if (state == Player.STATE_ENDED) {
                // Switching Media3 items inside a Player callback is unsafe on
                // some devices; post the guarded transition onto the main loop.
                main.post {
                    if (exoPlayer?.playbackState == Player.STATE_ENDED) {
                        if (offlinePlaying) {
                            if (!advanceOffline(forward = true)) {
                                offlineQueue.current?.let { offlineSession.savePosition(it, 0L, flush = true) }
                            }
                        } else playNext(true)
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Repeated playback of the same track is a new session, not a
            // permanent single lifetime play. Other transitions are detected
            // by the track identity in ListeningSessionMeter.observe().
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                sampleListening(endSession = true)
            }
            sampleListening()
        }

        override fun onPlayerError(error: PlaybackException) {
            sampleListening()
            val video = exoPlayer?.currentMediaItem?.mediaId ?: return
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
            if (!retriable) {
                savedQueue.recordFailure(video, "Playback failed. Try another song or Retry.")
                return
            }
            autoRetriedVideo = video
            val position = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
            Log.w("NightWavePlayback", "Transient playback error; one stream refresh requested")
            // Avoid mutating ExoPlayer inside its error callback.
            main.post {
                if (autoRetriedVideo == video && queue.current?.url == video &&
                    exoPlayer != null) {
                    queue.current?.let { resolveAndPlay(it, autoPlay = true,
                        resumePositionMs = position, recoveryAttempt = true) }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedQueue = QueueStateStore(this)
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
                val index = intent.getIntExtra(EXTRA_INDEX, -1)
                val next = offlineQueue.jump(index)
                if (next != null) playOfflineId(next)
            }
            ACTION_RESUME_SAVED -> resumeSavedQueue()
            ACTION_RETRY_CURRENT -> retryCurrent()
            ACTION_NEXT -> playNext(false)
            ACTION_PREVIOUS -> playPrevious()
            ACTION_JUMP -> if (!offlinePlaying) queue.jump(intent.getIntExtra(EXTRA_INDEX, -1))?.let { savedQueue.save(queue); resolveAndPlay(it) }
            ACTION_SHUFFLE -> if (!offlinePlaying) {
                queue.shuffle = !queue.shuffle; savedQueue.save(queue)
            }
            ACTION_REPEAT -> if (!offlinePlaying) {
                queue.repeat = when (queue.repeat) {
                    PlaybackQueue.Repeat.OFF -> PlaybackQueue.Repeat.ALL
                    PlaybackQueue.Repeat.ALL -> PlaybackQueue.Repeat.ONE
                    PlaybackQueue.Repeat.ONE -> PlaybackQueue.Repeat.OFF
                }
                savedQueue.save(queue)
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
            exoPlayer?.apply { prepare(); play() }
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

    /** Offline IDs are bounded; never write them into public-video bookmarks. */
    private fun playOfflineId(
        id: String, localLibrary: OfflineAudioLibrary = OfflineAudioLibrary(this),
        resumePositionMs: Long = 0L
    ): Boolean {
        val song = runCatching { localLibrary.find(id) }.getOrNull() ?: return false
        val file = localLibrary.fileFor(song) ?: return false
        val player = exoPlayer ?: return false
        if (offlinePlaying) persistProgress(flush = true)
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
        generation++ // Cancel unresolved online audio from a previous song.
        cancelPendingResolution()
        autoRetriedVideo = null
        player.apply {
            pause()
            setMediaItem(item)
            playWhenReady = true
            prepare()
            if (resumePositionMs > 0L) seekTo(resumePositionMs)
            play()
        }
        return true
    }

    private fun advanceOffline(forward: Boolean): Boolean {
        // Missing/deleted files are skipped in a bounded pass.
        repeat(offlineQueue.size) {
            val id = (if (forward) offlineQueue.next() else offlineQueue.previous())
                ?: return false
            if (playOfflineId(id)) return true
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
        // Flush a local checkpoint before changing playback mode or clearing
        // its MediaItem. Otherwise the last 2.5-second progress tick could be lost.
        if (offlinePlaying) persistProgress(flush = true)
        offlinePlaying = false
        offlineQueue.reset()
        if (!recoveryAttempt) autoRetriedVideo = null
        savedQueue.clearFailure()
        cancelPendingResolution()
        // Drop expired previous stream metadata immediately; the UI can display
        // the current queue entry during fresh extraction instead of a stale song.
        exoPlayer?.apply {
            stop()
            clearMediaItems()
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
            savedQueue.recordFailure(entry.url,
                "Music lookup is busy. Tap Retry in a moment.")
            // Bounded workers are occupied; fail visibly, never queue indefinitely.
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        sampleListening(endSession = true)
        persistProgress(flush = true)
        main.removeCallbacks(progressWriter)
        generation++
        cancelPendingResolution()
        resolver.shutdownNow()
        listeningWriter.shutdown() // drain pending bounded, consent-fenced writes
        exoPlayer?.removeListener(trackListener)
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
}
