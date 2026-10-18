package com.nightwave.player.v2.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.UUID
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
    /** Extractor is looking up a public watch URL, before Media3 has a stream. */
    val resolving: Boolean = false,
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
    fun removeUpcoming(index: Int, expectedUrl: String): String?
    fun undoUpcoming(token: String)
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
    private var ticksSinceReconcile = 0
    private var foregroundReconnectAvailable = true
    private val oneShotReconnect = Runnable {
        if (MediaReconnectPolicy.mayAttempt(
                released, ticking, foregroundReconnectAvailable,
                controller != null, future != null)) {
            foregroundReconnectAvailable = false
            connect() // one attempt while visible; never auto-play an old session
        }
    }

    /** One visible-screen retry only; no background loop or automatic Play. */
    private fun scheduleForegroundReconnect() {
        if (!MediaReconnectPolicy.mayAttempt(
                released, ticking, foregroundReconnectAvailable,
                controller != null, future != null)) return
        main.removeCallbacks(oneShotReconnect)
        main.postDelayed(oneShotReconnect, 1_200L)
    }

    private var cachedOfflineIds: List<String> = emptyList()
    private var cachedOfflineQueue: List<VideoPreview> = emptyList()
    /** Media3 notifies us when Android kills/restarts the session process. */
    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(disconnected: MediaController) {
            // This callback also runs during a normal release. Never resurrect
            // an Activity that has already been destroyed.
            if (released || controller !== disconnected) return
            // The future owns the MediaController resource, even when Android
            // has already disconnected its underlying MediaSession. Release
            // that stale future before allowing another controller to bind.
            val disconnectedFuture = future
            runCatching { disconnected.removeListener(listener) }
            controller = null
            future = null
            disconnectedFuture?.let { pending ->
                runCatching { MediaController.releaseFuture(pending) }
                    .onFailure { error ->
                        Log.w("NightWaveMedia", "Controller cleanup unavailable: " +
                            error.javaClass.simpleName)
                    }
            }
            state = state.copy(
                connected = false, playing = false, buffering = false,
                resolving = false,
                error = "Music service disconnected. Tap Reconnect."
            )
            // Only one bounded, visible-screen recovery; never a background
            // loop and never touch the queue/media or restart playback.
            scheduleForegroundReconnect()
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            // Media3 can deliver already-enqueued events after disconnect.
            // Never let an obsolete controller overwrite a replacement state.
            if (released || controller !== player) return
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
                    updatePlayhead(it)
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
        main.removeCallbacks(oneShotReconnect)
        try {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val pending = MediaController.Builder(context, token)
                .setListener(controllerListener)
                .buildAsync()
            future = pending
            pending.addListener({
                // An outdated or released controller must never be published
                // over the new Activity's active session.
                if (released || future !== pending) return@addListener
                try {
                    val resolved = pending.get()
                    controller = resolved
                    state = state.copy(error = null)
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
                        playing = false, buffering = false, resolving = false,
                        error = "Music service disconnected. Tap Reconnect.")
                    scheduleForegroundReconnect()
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(context))
        } catch (error: Exception) {
            future = null
            controller = null
            Log.w("NightWaveMedia", "Controller setup failed: " +
                error.javaClass.simpleName)
            state = state.copy(connected = false,
                playing = false, buffering = false, resolving = false,
                error = "Music service connection failed. Tap Reconnect.")
            scheduleForegroundReconnect()
        }
    }

    @MainThread
    fun resumeUiUpdates() {
        if (released) return
        foregroundReconnectAvailable = true
        ticksSinceReconcile = PlayerSnapshotPolicy.RECONCILE_TICKS
        // Imported files may have been deleted or re-imported while the Offline
        // Library Activity was on top. Refresh the cache on return, not on each tick.
        cachedOfflineIds = emptyList()
        cachedOfflineQueue = emptyList()
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
        main.removeCallbacks(oneShotReconnect)
    }

    @MainThread
    fun release() {
        if (released) return
        released = true
        pauseUiUpdates()
        ticksSinceReconcile = 0
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

    /**
     * The former 750 ms ticker loaded SharedPreferences and reparsed the saved
     * queue (and the offline checkpoint) on every tick. Playhead progress needs
     * only the already-bound controller. Media3 events still reconcile at once;
     * a periodic full read catches queue edits made by service Intent actions.
     */
    private fun updatePlayhead(player: Player) {
        val currentId = player.currentMediaItem?.mediaId
        if (PlayerSnapshotPolicy.needsFullRefresh(
                currentId, state.videoUrl, ticksSinceReconcile, state.playing)) {
            updateFromPlayer(player)
            return
        }
        ticksSinceReconcile++
        if (currentId == null) return // Keep the saved resume placeholder.
        val position = player.currentPosition.coerceAtLeast(0L)
        val liveDuration = player.duration.takeIf {
            it != C.TIME_UNSET && it > 0L
        }
        val duration = liveDuration ?: state.durationMs
        val playing = player.isPlaying
        val buffering = player.playbackState == Player.STATE_BUFFERING
        if (state.positionMs != position || state.durationMs != duration ||
            state.playing != playing || state.buffering != buffering ||
            state.resolving
        ) {
            state = state.copy(positionMs = position, durationMs = duration,
                playing = playing, buffering = buffering, resolving = false,
                connected = true)
        }
    }

    private fun updateFromPlayer(player: Player) {
        ticksSinceReconcile = 0
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
        val resolving = media == null && persistence.lookupPending(savedTrack?.url)
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
            buffering = player.playbackState == Player.STATE_BUFFERING || resolving,
            resolving = resolving,
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
                else if (offlineId != null || resolving) null
                else persistence.failure(savedTrack?.url)
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

    override fun play(video: VideoPreview, collection: List<VideoPreview>) =
        playValidated(video, collection, explicitDisplayOrder = false)

    /** Only the Library quick actions request one-time displayed queue order. */
    fun playLibraryCollection(collection: List<VideoPreview>) {
        val selected = collection.firstOrNull() ?: return
        playValidated(selected, collection, explicitDisplayOrder = true)
    }

    private fun playValidated(
        video: VideoPreview, collection: List<VideoPreview>, explicitDisplayOrder: Boolean
    ) {
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
                .putExtra(PlaybackService.EXTRA_QUEUE_DISPLAY_ORDER, explicitDisplayOrder)
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

    /**
     * A controller can disconnect between drawing Play/Seek and the click.
     * Media3 may then throw synchronously; never let that crash the Activity.
     * Clear this Activity's stale controller and leave the saved queue intact.
     */
    private fun handleControlFailure(operation: String, error: RuntimeException) {
        Log.w("NightWaveMedia", "Controller " + operation + " failed: " +
            error.javaClass.simpleName)
        controller?.let { active -> runCatching { active.removeListener(listener) } }
        controller = null
        future?.let { pending -> runCatching { MediaController.releaseFuture(pending) } }
        future = null
        state = state.copy(connected = false, playing = false, buffering = false,
            resolving = false, error = "Playback connection was interrupted. Tap Reconnect.")
        scheduleForegroundReconnect()
    }

    /**
     * Explicitly resume the selected queue/offline checkpoint in the service.
     * Service revalidates source identity, file ownership and bookmark before
     * loading anything. Never issue play() against an unrelated cached item.
     */
    private fun selectedMediaId(): String? =
        if (PlaybackCommandPolicy.isOfflineSelection(state.videoUrl)) state.videoUrl
        else state.queue.getOrNull(state.queueIndex)?.url
            ?: state.videoUrl.takeIf(PlaybackCommandPolicy::isPublicSelection)

    private fun requestSelectedResume() {
        when {
            PlaybackCommandPolicy.isOfflineSelection(state.videoUrl) ->
                action(PlaybackService.ACTION_RESUME_OFFLINE)
            state.queue.getOrNull(state.queueIndex)?.url
                ?.let(PlaybackCommandPolicy::isPublicSelection) == true ->
                action(PlaybackService.ACTION_RESUME_SAVED)
            else -> state = state.copy(error = "Choose a song to start listening.")
        }
    }

    /**
     * Idempotent Resume for Home's "Pick up where you left off" card.
     * Unlike playPause(), this can never accidentally pause a live controller.
     */
    fun resumeCurrent() {
        try {
            controller?.let { active ->
                when {
                    // Idempotent only when the playing item is *the
                    // selected track*. A previous queue's audio may remain
                    // playing while a new selection has not loaded yet.
                    PlaybackCommandPolicy.resumeSatisfied(
                        selectedMediaId(), active.currentMediaItem?.mediaId,
                        active.playerError != null, active.isPlaying
                    ) -> Unit
                    active.playerError != null -> action(PlaybackService.ACTION_RETRY_CURRENT)
                    active.currentMediaItem != null -> {
                        if (PlaybackCommandPolicy.canResumeLoaded(
                                selectedMediaId(), active.currentMediaItem?.mediaId,
                                active.playerError != null)) {
                            if (active.playbackState == Player.STATE_ENDED) active.seekTo(0L)
                            if (active.playbackState == Player.STATE_ENDED ||
                                active.playbackState == Player.STATE_IDLE) active.prepare()
                            active.play()
                        } else requestSelectedResume()
                    }
                    else -> requestSelectedResume()
                }
            } ?: run {
                requestSelectedResume()
            }
        } catch (error: RuntimeException) {
            handleControlFailure("resume", error)
        }
    }

    override fun playPause() {
        try {
            controller?.let {
                if (it.isPlaying ||
                    (it.currentMediaItem == null && !state.offline && state.queueIndex >= 0 &&
                        it.playWhenReady)) it.pause()
                else if (it.playerError != null) action(PlaybackService.ACTION_RETRY_CURRENT)
                else if (it.currentMediaItem != null) {
                    if (PlaybackCommandPolicy.canResumeLoaded(
                            selectedMediaId(), it.currentMediaItem?.mediaId,
                            it.playerError != null)) {
                        if (it.playbackState == Player.STATE_ENDED) it.seekTo(0L)
                        if (it.playbackState == Player.STATE_ENDED ||
                            it.playbackState == Player.STATE_IDLE) it.prepare()
                        it.play()
                    } else requestSelectedResume()
                }
                else requestSelectedResume()
            } ?: run {
                requestSelectedResume()
            }
        } catch (error: RuntimeException) {
            handleControlFailure("play/pause", error)
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
    /**
     * Returns a one-time token only when the request can be sent. The service
     * independently validates the index/url/current track before persisting.
     * Never send commands for imported offline files or a shuffled queue.
     */
    override fun removeUpcoming(index: Int, expectedUrl: String): String? {
        if (state.offline || state.shuffle || !state.connected ||
            index <= state.queueIndex || state.queue.getOrNull(index)?.url != expectedUrl ||
            !expectedUrl.startsWith("https://www.youtube.com/watch?v=")) return null
        val token = UUID.randomUUID().toString()
        return try {
            context.startService(Intent(context, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_REMOVE_UPCOMING)
                .putExtra(PlaybackService.EXTRA_INDEX, index)
                .putExtra(PlaybackService.EXTRA_EXPECTED_URL, expectedUrl)
                .putExtra(PlaybackService.EXTRA_QUEUE_TOKEN, token))
            scheduleQueueRefresh()
            token
        } catch (error: Exception) {
            state = state.copy(error = "Could not remove this Up Next entry.")
            null
        }
    }

    override fun undoUpcoming(token: String) {
        if (token.isBlank() || state.offline) return
        try {
            context.startService(Intent(context, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_UNDO_REMOVE_UPCOMING)
                .putExtra(PlaybackService.EXTRA_QUEUE_TOKEN, token))
            scheduleQueueRefresh()
        } catch (error: Exception) {
            state = state.copy(error = "Could not restore this Up Next entry.")
        }
    }

    private fun scheduleQueueRefresh() {
        // The service receives asynchronous intents. Don't read the old
        // persisted snapshot in the same call stack before onStartCommand.
        main.postDelayed({ if (!released) refreshQueue() }, 180L)
        main.postDelayed({ if (!released) refreshQueue() }, 700L)
    }

    override fun toggleShuffle() {
        if (!state.offline) action(PlaybackService.ACTION_SHUFFLE)
    }
    override fun cycleRepeat() {
        if (!state.offline) action(PlaybackService.ACTION_REPEAT)
    }

    override fun seekTo(positionMs: Long) {
        try {
            controller?.let { player ->
                val duration = player.duration
                if (duration > 0 && duration != C.TIME_UNSET) {
                    player.seekTo(positionMs.coerceIn(0L, duration))
                }
            }
        } catch (error: RuntimeException) {
            handleControlFailure("seek", error)
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
