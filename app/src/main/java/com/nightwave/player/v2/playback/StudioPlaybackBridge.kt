package com.nightwave.player.v2.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.playback.PlaybackService
import com.nightwave.player.playback.QueueStateStore
import com.nightwave.player.playback.PlaybackQueue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class StudioPlayerState(
    val connected: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val videoUrl: String = "",
    val thumbnail: String = "",
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: PlaybackQueue.Repeat = PlaybackQueue.Repeat.OFF,
    val queue: List<VideoPreview> = emptyList(),
    val queueIndex: Int = -1,
    val error: String? = null
) {
    val hasTrack: Boolean get() = videoUrl.isNotBlank()
}

interface StudioPlayerActions {
    fun play(video: VideoPreview, collection: List<VideoPreview> = listOf(video))
    fun playPause()
    fun enqueue(video: VideoPreview)
    fun previous()
    fun next()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
    fun cycleRepeat()
    fun jumpTo(index: Int)
    fun refreshQueue()
}

/**
 * A lifecycle-owned Media3 Controller. The Compose screen never owns an ExoPlayer,
 * signed stream URL or background service. A loss of connection leaves the user
 * in the UI with a reconnectable service instead of a pretend playing state.
 */
class StudioPlaybackBridge(private val context: Context) : StudioPlayerActions {
    var state by mutableStateOf(StudioPlayerState())
        private set

    private val main = Handler(Looper.getMainLooper())
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var released = false
    private var ticking = false
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updateFromPlayer(player)
    }
    private val ticker = object : Runnable {
        override fun run() {
            if (released || !ticking) return
            controller?.let { updateFromPlayer(it) }
            main.postDelayed(this, 750L)
        }
    }

    @MainThread
    fun connect() {
        if (released || future != null) return
        try {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val pending = MediaController.Builder(context, token).buildAsync()
            future = pending
            pending.addListener({
                if (released) {
                    return@addListener
                }
                try {
                    val resolved = pending.get()
                    controller = resolved
                    resolved.addListener(listener)
                    refreshQueue()
                    updateFromPlayer(resolved)
                } catch (error: Exception) {
                    state = state.copy(connected = false, error = "Can't connect to the music service.")
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(context))
        } catch (error: Exception) {
            state = state.copy(connected = false, error = "Music service connection failed.")
        }
    }

    @MainThread
    fun resumeUiUpdates() {
        if (released || ticking) return
        ticking = true
        main.post(ticker)
    }

    @MainThread
    fun pauseUiUpdates() {
        ticking = false
        main.removeCallbacks(ticker)
    }

    @MainThread
    fun release() {
        if (released) return
        released = true
        pauseUiUpdates()
        controller?.removeListener(listener)
        controller = null
        future?.let { MediaController.releaseFuture(it) }
        future = null
    }

    private fun updateFromPlayer(player: Player) {
        val media = player.currentMediaItem
        val metadata = media?.mediaMetadata
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        val current = QueueStateStore(context).read()
        state = state.copy(
            connected = true,
            title = metadata?.title?.toString().orEmpty(),
            artist = metadata?.artist?.toString().orEmpty(),
            videoUrl = media?.mediaId.orEmpty().takeIf { it.startsWith("https://www.youtube.com/watch?v=") }.orEmpty(),
            thumbnail = metadata?.artworkUri?.toString().orEmpty(),
            playing = player.isPlaying,
            buffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            shuffle = current.shuffle,
            repeat = current.repeat,
            queue = current.tracks.map { VideoPreview(it.title, it.creator, it.url, it.artwork, 0) },
            queueIndex = current.index,
            error = null
        )
    }

    private fun action(name: String, index: Int? = null) {
        try {
            val intent = Intent(context, PlaybackService::class.java).setAction(name)
            if (index != null) intent.putExtra(PlaybackService.EXTRA_INDEX, index)
            context.startService(intent)
            // Service processes commands on the main thread; next ticker refreshes snapshot.
        } catch (e: Exception) {
            state = state.copy(error = "Could not send playback command.")
        }
    }

    override fun play(video: VideoPreview, collection: List<VideoPreview>) {
        val plan = QueueSelectionRules.plan(video, collection)
        val tracks = plan.songs
        if (tracks.isEmpty()) {
            state = state.copy(error = "This is not a supported public video link.")
            return
        }
        val index = plan.selectedIndex
        try {
            val request = Intent(context, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_PLAY_QUEUE)
                .putStringArrayListExtra(PlaybackService.EXTRA_URLS, ArrayList(tracks.map { it.url }))
                .putStringArrayListExtra(PlaybackService.EXTRA_TITLES, ArrayList(tracks.map { it.title }))
                .putStringArrayListExtra(PlaybackService.EXTRA_CREATORS, ArrayList(tracks.map { it.author }))
                .putStringArrayListExtra(PlaybackService.EXTRA_ARTWORKS, ArrayList(tracks.map { it.thumbnail }))
                .putExtra(PlaybackService.EXTRA_INDEX, index)
            context.startService(request)
        } catch (e: Exception) {
            state = state.copy(error = "Could not start this track.")
        }
    }

    override fun enqueue(video: VideoPreview) {
        try {
            val request = Intent(context, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_ENQUEUE)
                .putExtra(PlaybackService.EXTRA_URLS, video.url)
                .putExtra(PlaybackService.EXTRA_TITLES, video.title)
                .putExtra(PlaybackService.EXTRA_CREATORS, video.author)
                .putExtra(PlaybackService.EXTRA_ARTWORKS, video.thumbnail)
            context.startService(request)
        } catch (e: Exception) {
            state = state.copy(error = "Could not add this track to the queue.")
        }
    }

    override fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
            ?: run { state = state.copy(error = "Player is connecting. Try again.") }
    }
    override fun next() = action(PlaybackService.ACTION_NEXT)
    override fun previous() = action(PlaybackService.ACTION_PREVIOUS)
    override fun jumpTo(index: Int) = action(PlaybackService.ACTION_JUMP, index)
    override fun toggleShuffle() = action(PlaybackService.ACTION_SHUFFLE)
    override fun cycleRepeat() = action(PlaybackService.ACTION_REPEAT)

    override fun seekTo(positionMs: Long) {
        controller?.let { player ->
            if (player.duration > 0 && player.duration != C.TIME_UNSET) {
                player.seekTo(positionMs.coerceIn(0L, player.duration))
            }
        }
    }

    override fun refreshQueue() {
        val snapshot = QueueStateStore(context).read()
        state = state.copy(
            queue = snapshot.tracks.map { VideoPreview(it.title, it.creator, it.url, it.artwork, 0L) },
            queueIndex = snapshot.index,
            shuffle = snapshot.shuffle,
            repeat = snapshot.repeat
        )
    }
}
