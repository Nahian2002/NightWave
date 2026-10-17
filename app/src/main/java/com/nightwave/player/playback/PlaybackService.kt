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
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.net.YouTubeGateway
import com.nightwave.player.ui.MainActivity
import java.util.concurrent.Executors

/** The service owns automatic next-track resolution, even while the Activity is stopped. */
@UnstableApi
class PlaybackService : MediaSessionService() {
    companion object {
        const val ACTION_REPLACE = "com.nightwave.player.REPLACE_QUEUE"
        const val ACTION_RESUME_SAVED = "com.nightwave.player.RESUME_SAVED"
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
    private val progressWriter = object : Runnable {
        override fun run() {
            persistProgress()
            main.postDelayed(this, 2500L)
        }
    }

    private fun persistProgress() {
        val player = exoPlayer ?: return
        val url = player.currentMediaItem?.mediaId ?: return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        savedQueue.saveBookmark(url, player.currentPosition.coerceAtLeast(0L), duration)
    }

    private val trackListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) playNext(true)
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
            .setMediaSourceFactory(DefaultMediaSourceFactory(networkSource))
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
                        else -> false // Media3 still handles play/pause/headset buttons.
                    }
                }
            }).build()
        // Recover the last public video/queue paused: never surprise-play on reopen.
        queue.current?.let { previous ->
            resolveAndPlay(previous, autoPlay = false,
                resumePositionMs = savedQueue.bookmark(previous.url)?.positionMs ?: 0L)
        }
        main.postDelayed(progressWriter, 2500L)
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
            ACTION_RESUME_SAVED -> {
                val current = queue.current
                if (exoPlayer?.currentMediaItem != null) exoPlayer?.play()
                else current?.let {
                    resolveAndPlay(it, autoPlay = true,
                        resumePositionMs = savedQueue.bookmark(it.url)?.positionMs ?: 0L)
                }
            }
            ACTION_NEXT -> playNext(false)
            ACTION_PREVIOUS -> playPrevious()
            ACTION_JUMP -> queue.jump(intent.getIntExtra(EXTRA_INDEX, -1))?.let { savedQueue.save(queue); resolveAndPlay(it) }
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

    private fun playNext(automatic: Boolean) {
        queue.next(automatic)?.let { savedQueue.save(queue); resolveAndPlay(it) }
    }

    private fun playPrevious() {
        queue.previous()?.let { savedQueue.save(queue); resolveAndPlay(it) }
    }

    private fun resolveAndPlay(
        entry: QueueEntry, autoPlay: Boolean = true, resumePositionMs: Long = 0L
    ) {
        if (autoPlay && resumePositionMs == 0L) savedQueue.saveBookmark(entry.url, 0L, 0L)
        val request = ++generation
        resolver.execute {
            val outcome = runCatching { gateway.resolve(entry.url) }
            main.post {
                if (request != generation) return@post
                outcome.onSuccess { playable ->
                    val video = playable.video
                    if (autoPlay) MusicLibrary(this).addRecent(video)
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
                // Never loop infinitely through unavailable streams.
                outcome.onFailure { exoPlayer?.pause() }
            }
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
