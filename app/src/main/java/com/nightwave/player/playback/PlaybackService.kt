package com.nightwave.player.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.os.Handler
import android.os.Looper
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
import com.nightwave.player.ui.MainActivity
import java.util.concurrent.Executors

/** The service owns automatic next-track resolution, even while the Activity is stopped. */
@UnstableApi
class PlaybackService : MediaSessionService() {
    companion object {
        const val ACTION_REPLACE = "com.nightwave.player.REPLACE_QUEUE"
        const val ACTION_RESUME_SAVED = "com.nightwave.player.RESUME_SAVED"
        const val ACTION_RETRY_CURRENT = "com.nightwave.player.RETRY_CURRENT"
        const val ACTION_PLAY_OFFLINE = "com.nightwave.player.PLAY_OFFLINE"
        const val EXTRA_OFFLINE_ID = "offline_id"
        const val EXTRA_OFFLINE_IDS = "offline_ids"
        /** Atomic queue assignment followed by a single play request for the v2 interface. */
        const val ACTION_PLAY_QUEUE = "com.nightwave.player.PLAY_QUEUE"
        const val ACTION_ENQUEUE = "com.nightwave.player.ENQUEUE"
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
    private val resolver = Executors.newSingleThreadExecutor()
    private val gateway = YouTubeGateway()
    private var generation = 0
    private var offlinePlaying = false
    private val offlineQueue = OfflinePlaybackQueue()
    private var autoRetriedVideo: String? = null
    private val progressWriter = object : Runnable {
        override fun run() {
            val player = exoPlayer ?: return
            if (!player.isPlaying) return
            persistProgress()
            main.postDelayed(this, 2500L)
        }
    }

    private fun persistProgress() {
        val player = exoPlayer ?: return
        val url = player.currentMediaItem?.mediaId ?: return
        if (!::savedQueue.isInitialized || !url.startsWith("https://www.youtube.com/watch?v=")) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        // A corrupt preference must never terminate the background playback service.
        runCatching {
            savedQueue.saveBookmark(url, player.currentPosition.coerceAtLeast(0L), duration)
        }
    }

    private val trackListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            main.removeCallbacks(progressWriter)
            if (isPlaying) main.postDelayed(progressWriter, 2500L)
            else persistProgress()
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) playNext(true)
        }

        override fun onPlayerError(error: PlaybackException) {
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
        savedQueue.restore(queue)
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
            this, 0, Intent(this, MainActivity::class.java),
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
                queue.replace(entries, intent.getIntExtra(EXTRA_INDEX, 0))
                generation++ // invalidate unresolved previous track jobs
                savedQueue.save(queue)
                queue.current?.let { savedQueue.saveBookmark(it.url, 0L, 0L) }
                if (intent.action == ACTION_PLAY_QUEUE) {
                    offlineQueue.reset()
                    // Current item belongs to the same queue snapshot; do not touch old app entry points.
                    exoPlayer?.pause()
                    queue.current?.let { resolveAndPlay(it) }
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
                if (offlineQueue.select(candidates, id) != null &&
                    !playOfflineId(id, localLibrary)) {
                    advanceOffline(forward = true)
                }
            }
            ACTION_RESUME_SAVED -> resumeSavedQueue()
            ACTION_RETRY_CURRENT -> retryCurrent()
            ACTION_NEXT -> playNext(false)
            ACTION_PREVIOUS -> playPrevious()
            ACTION_JUMP -> if (!offlinePlaying) queue.jump(intent.getIntExtra(EXTRA_INDEX, -1))?.let { savedQueue.save(queue); resolveAndPlay(it) }
            ACTION_SHUFFLE -> { queue.shuffle = !queue.shuffle; savedQueue.save(queue) }
            ACTION_REPEAT -> {
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

    /** Explicit retry after a playback/extraction error, without reusing signed stream URLs. */
    private fun retryCurrent() {
        if (offlinePlaying) {
            exoPlayer?.apply { prepare(); play() }
            return
        }
        val entry = queue.current ?: return
        val currentId = exoPlayer?.currentMediaItem?.mediaId
        if (currentId != null && currentId != entry.url) return
        val bookmark = savedQueue.bookmark(entry.url)?.positionMs ?: 0L
        val position = exoPlayer?.currentPosition?.coerceAtLeast(0L)
            ?.takeIf { it > 0 } ?: bookmark
        resolveAndPlay(entry, autoPlay = true, resumePositionMs = position)
    }

    /** Called only by a user/transport Play action, never by onCreate. */
    private fun resumeSavedQueue() {
        if (exoPlayer?.currentMediaItem != null) {
            exoPlayer?.play()
        } else {
            queue.current?.let { entry ->
                resolveAndPlay(entry, autoPlay = true,
                    resumePositionMs = savedQueue.bookmark(entry.url)?.positionMs ?: 0L)
            }
        }
    }

    /** Offline IDs are transient; never write them into public-video bookmarks. */
    private fun playOfflineId(id: String, localLibrary: OfflineAudioLibrary = OfflineAudioLibrary(this)): Boolean {
        val song = runCatching { localLibrary.find(id) }.getOrNull() ?: return false
        val file = localLibrary.fileFor(song) ?: return false
        val item = MediaItem.Builder().setMediaId("offline:" + song.id)
            .setUri(Uri.fromFile(file)).setMimeType(song.mimeType)
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle(song.title).setArtist("Offline · NightWave").build())
            .build()
        offlinePlaying = true
        generation++ // Cancel unresolved online audio from a previous song.
        autoRetriedVideo = null
        exoPlayer?.apply {
            pause()
            setMediaItem(item)
            playWhenReady = true
            prepare()
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
        if (offlinePlaying) {
            advanceOffline(forward = true)
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
            if (!offlinePlaying) queue.current?.let {
                savedQueue.saveBookmark(it.url, 0L,
                    player.duration.takeIf { duration -> duration > 0L && duration != C.TIME_UNSET } ?: 0L)
            }
            return
        }
        if (offlinePlaying) {
            if (!advanceOffline(forward = false)) player?.seekTo(0L)
            return
        }
        queue.previous()?.let { savedQueue.save(queue); resolveAndPlay(it) }
    }

    private fun resolveAndPlay(
        entry: QueueEntry, autoPlay: Boolean = true,
        resumePositionMs: Long = 0L, recoveryAttempt: Boolean = false
    ) {
        offlinePlaying = false
        offlineQueue.reset()
        if (!recoveryAttempt) autoRetriedVideo = null
        savedQueue.clearFailure()
        // Drop expired previous stream metadata immediately; the UI can display
        // the current queue entry during fresh extraction instead of a stale song.
        exoPlayer?.apply {
            stop()
            clearMediaItems()
            playWhenReady = false
        }
        if (autoPlay && resumePositionMs == 0L) savedQueue.saveBookmark(entry.url, 0L, 0L)
        val request = ++generation
        if (resolver.isShutdown) return
        try {
            resolver.execute {
                val outcome = runCatching { gateway.resolve(entry.url) }
                main.post {
                    if (request != generation || exoPlayer == null) return@post
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
                        setMediaItem(mediaItem)
                        if (resumePositionMs > 0L) seekTo(resumePositionMs)
                        playWhenReady = autoPlay
                        prepare()
                        if (autoPlay) play()
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
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            // Service is shutting down; never crash because an async resolve was queued.
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        persistProgress()
        main.removeCallbacks(progressWriter)
        generation++
        resolver.shutdownNow()
        exoPlayer?.removeListener(trackListener)
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
}
