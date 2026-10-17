package com.nightwave.player.v2.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.nightwave.player.offline.OfflineAudioLibrary
import com.nightwave.player.offline.OfflineSessionStore
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
    val offline: Boolean get() = videoUrl.startsWith("offline:")
}

interface StudioPlayerActions {
    fun play(video: VideoPreview, collection: List<VideoPreview> = listOf(video))
    fun playPause()
    fun retry()
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
    private var cachedOfflineIds: List<String> = emptyList()
    private var cachedOfflineQueue: List<VideoPreview> = emptyList()
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            try {
                updateFromPlayer(player)
            } catch (error: Exception) {
                Log.w("NightWaveMedia", "Player state refresh unavailable: " +
                    error.javaClass.simpleName)
                state = state.copy(error = "Player state refresh failed. Tap Reconnect.")
            }
        }
    }
    private val ticker = object : Runnable {
        override fun run() {
            if (released || !ticking) return
            controller?.let {
                try {
                    updateFromPlayer(it)
                } catch (error: Exception) {
                    Log.w("NightWaveMedia", "Playback UI tick failed: " +
                        error.javaClass.simpleName)
                    state = state.copy(error = "Player state could not be updated.")
                }
            }
            main.postDelayed(this, 750L)
        }
    }

    @MainThread
    fun connect() {
        if (released || future != null || controller != null) return
        try {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val pending = MediaController.Builder(context, token).buildAsync()
            future = pending
            pending.addListener({
                // An outdated or released controller must never be published
                // over the new Activity's active session.
                if (released || future !== pending) return@addListener
                try {
                    val resolved = pending.get()
                    controller = resolved
                    resolved.addListener(listener)
                    refreshQueue()
                    updateFromPlayer(resolved)
                } catch (error: Exception) {
                    // Previous versions retained a failed future forever:
                    // subsequent UI resume could never connect again.
                    Log.w("NightWaveMedia", "Media controller unavailable: " +
                        error.javaClass.simpleName)
                    future = null
                    controller = null
                    runCatching { MediaController.releaseFuture(pending) }
                    state = state.copy(connected = false,
                        error = "Music service disconnected. Tap Reconnect.")
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(context))
        } catch (error: Exception) {
            future = null
            controller = null
            Log.w("NightWaveMedia", "Controller setup failed: " +
                error.javaClass.simpleName)
            state = state.copy(connected = false,
                error = "Music service connection failed. Tap Reconnect.")
        }
    }

    @MainThread
    fun resumeUiUpdates() {
        if (released) return
        // One bounded attempt on foreground re-entry, never a reconnect loop.
        if (controller == null && future == null) connect()
        if (ticking) return
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

    /** Cached per ordered local list; no offline library JSON scan every UI tick. */
    private fun offlineQueueFor(id: String): Pair<List<VideoPreview>, Int> {
        val checkpoint = OfflineSessionStore(context).read() ?: return emptyList<VideoPreview>() to -1
        if (checkpoint.ids != cachedOfflineIds) {
            val known = runCatching { OfflineAudioLibrary(context).songs().associateBy { it.id } }
                .getOrDefault(emptyMap())
            cachedOfflineQueue = checkpoint.ids.mapNotNull { savedId ->
                known[savedId]?.let {
                    VideoPreview(it.title, "Offline · NightWave", "offline:" + it.id, "", 0L)
                }
            }
            cachedOfflineIds = checkpoint.ids
        }
        return cachedOfflineQueue to cachedOfflineQueue.indexOfFirst { it.url == "offline:" + id }
    }

    private fun updateFromPlayer(player: Player) {
        val media = player.currentMediaItem
        val metadata = media?.mediaMetadata
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        val persistence = QueueStateStore(context)
        val current = persistence.read()
        val offlineId = media?.mediaId?.takeIf { it.startsWith("offline:") }
        val offlineList = if (offlineId != null) offlineQueueFor(offlineId.removePrefix("offline:"))
            else (emptyList<VideoPreview>() to -1)
        val savedTrack = current.tracks.getOrNull(current.index)
        val savedBookmark = persistence.bookmark(savedTrack?.url)
        state = state.copy(
            connected = true,
            title = if (offlineId != null) metadata?.title?.toString().orEmpty()
                .ifBlank { "Offline audio" }
                else metadata?.title?.toString()?.takeIf { it.isNotBlank() }
                    ?: savedTrack?.title.orEmpty(),
            artist = if (offlineId != null) metadata?.artist?.toString().orEmpty()
                .ifBlank { "Offline · NightWave" }
                else metadata?.artist?.toString()?.takeIf { it.isNotBlank() }
                    ?: savedTrack?.creator.orEmpty(),
            videoUrl = offlineId ?: media?.mediaId.orEmpty()
                .takeIf { it.startsWith("https://www.youtube.com/watch?v=") }
                ?: savedTrack?.url.orEmpty(),
            thumbnail = if (offlineId != null) metadata?.artworkUri?.toString().orEmpty()
                else metadata?.artworkUri?.toString()?.takeIf { it.isNotBlank() }
                    ?: savedTrack?.artwork.orEmpty(),
            playing = player.isPlaying,
            buffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = if (media != null) player.currentPosition.coerceAtLeast(0L)
                else savedBookmark?.positionMs ?: 0L,
            durationMs = if (duration > 0L) duration
                else if (offlineId != null) 0L else savedBookmark?.durationMs ?: 0L,
            shuffle = current.shuffle,
            repeat = current.repeat,
            queue = if (offlineId != null) offlineList.first
                else current.tracks.map { VideoPreview(it.title, it.creator, it.url, it.artwork, 0) },
            queueIndex = if (offlineId != null) offlineList.second else current.index,
            error = if (player.playerError != null)
                "Audio playback failed. Tap Retry or choose another song."
                else if (offlineId != null) null else persistence.failure(savedTrack?.url)
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
        controller?.let {
            if (it.isPlaying) it.pause()
            else if (it.playerError != null) action(PlaybackService.ACTION_RETRY_CURRENT)
            else if (it.currentMediaItem != null) it.play()
            else if (state.offline) action(PlaybackService.ACTION_RESUME_OFFLINE)
            else if (state.queueIndex >= 0) action(PlaybackService.ACTION_RESUME_SAVED)
            else state = state.copy(error = "Choose a song to start listening.")
        } ?: run {
            if (state.offline) action(PlaybackService.ACTION_RESUME_OFFLINE)
            else if (state.queueIndex >= 0) action(PlaybackService.ACTION_RESUME_SAVED)
            else state = state.copy(error = "Player is connecting. Try again.")
        }
    }

    override fun retry() {
        if (!state.connected) connect()
        else action(PlaybackService.ACTION_RETRY_CURRENT)
    }
    override fun next() = action(PlaybackService.ACTION_NEXT)
    override fun previous() = action(PlaybackService.ACTION_PREVIOUS)
    override fun jumpTo(index: Int) = action(
        if (state.offline) PlaybackService.ACTION_JUMP_OFFLINE
        else PlaybackService.ACTION_JUMP, index
    )
    override fun toggleShuffle() {
        if (!state.offline) action(PlaybackService.ACTION_SHUFFLE)
    }
    override fun cycleRepeat() {
        if (!state.offline) action(PlaybackService.ACTION_REPEAT)
    }

    override fun seekTo(positionMs: Long) {
        controller?.let { player ->
            if (player.duration > 0 && player.duration != C.TIME_UNSET) {
                player.seekTo(positionMs.coerceIn(0L, player.duration))
            }
        }
    }

    override fun refreshQueue() {
        val persistence = QueueStateStore(context)
        val snapshot = persistence.read()
        val entry = snapshot.tracks.getOrNull(snapshot.index)
        val bookmark = persistence.bookmark(entry?.url)
        state = state.copy(
            queue = if (state.offline) state.queue
                else snapshot.tracks.map { VideoPreview(it.title, it.creator, it.url, it.artwork, 0L) },
            queueIndex = if (state.offline) state.queueIndex else snapshot.index,
            shuffle = snapshot.shuffle,
            repeat = snapshot.repeat,
            title = if (state.offline) state.title else entry?.title ?: state.title,
            artist = if (state.offline) state.artist else entry?.creator ?: state.artist,
            thumbnail = if (state.offline) state.thumbnail else entry?.artwork ?: state.thumbnail,
            videoUrl = if (state.offline) state.videoUrl else entry?.url ?: state.videoUrl,
            positionMs = if (state.offline) state.positionMs else bookmark?.positionMs ?: state.positionMs,
            durationMs = if (state.offline) state.durationMs else bookmark?.durationMs ?: state.durationMs,
            error = if (state.offline) state.error else persistence.failure(entry?.url)
        )
    }
}
