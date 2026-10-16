package com.nightwave.player.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.nightwave.player.R
import com.nightwave.player.core.TimeLabel
import com.nightwave.player.core.SearchInput
import com.nightwave.player.core.SearchInputResolver
import com.nightwave.player.core.VideoLink
import com.nightwave.player.databinding.ActivityMainBinding
import com.nightwave.player.net.PlayableAudio
import com.nightwave.player.net.SearchMode
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.net.YouTubeGateway
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.playback.PlaybackService
import com.nightwave.player.playback.QueueStateStore
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private val gateway = YouTubeGateway()
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var thumbs: ThumbnailLoader
    private lateinit var library: MusicLibrary
    private enum class Page { HOME, SEARCH, LIBRARY }
    private var page = Page.HOME
    private var results = emptyList<VideoPreview>()
    private var lastRecordedUrl: String? = null
    private var shuffle = false
    private var repeat = 0
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var active = false
    private var seekInProgress = false
    private var searchMode = SearchMode.SONGS
    private var token = 0
    private var currentVideoUrl: String? = null
    private var pendingLink: String? = null
    private var waitingToPlay: PlayableAudio? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = showPlayerState(player)
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            showStatus("Playback failed. The YouTube stream may have expired or become unavailable. Select the video to retry.", true)
        }
    }

    private val refresh = object : Runnable {
        override fun run() {
            controller?.let(::showPlayerState)
            if (active) main.postDelayed(this, 1_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        thumbs = ThumbnailLoader(this)
        library = MusicLibrary(this)
        val restoredQueue = QueueStateStore(this).read()
        shuffle = restoredQueue.shuffle
        repeat = restoredQueue.repeat.ordinal
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        binding.searchButton.setOnClickListener { searchOrPlay() }
        binding.homeTab.setOnClickListener { openPage(Page.HOME) }
        binding.searchTab.setOnClickListener { openPage(Page.SEARCH) }
        binding.libraryTab.setOnClickListener { openPage(Page.LIBRARY) }
        binding.prevTrackButton.setOnClickListener { sendQueueAction(PlaybackService.ACTION_PREVIOUS) }
        binding.nextTrackButton.setOnClickListener { sendQueueAction(PlaybackService.ACTION_NEXT) }
        binding.shuffleButton.setTextColor(
            if (shuffle) Color.rgb(178, 243, 105) else Color.rgb(185, 201, 177)
        )
        binding.shuffleButton.setOnClickListener {
            shuffle = !shuffle
            sendQueueAction(PlaybackService.ACTION_SHUFFLE)
            binding.shuffleButton.setTextColor(if (shuffle) Color.rgb(178, 243, 105) else Color.rgb(185, 201, 177))
        }
        binding.favoriteButton.setOnClickListener {
            currentVideo()?.let {
                library.toggleFavorite(it)
                updateFavoriteButton()
                if (page != Page.SEARCH) openPage(page)
            }
        }
        binding.playlistButton.setOnClickListener { currentVideo()?.let(::showPlaylistPicker) }
        binding.queueButton.setOnClickListener { showQueueDialog() }
        binding.nowPlayingButton.setOnClickListener { openNowPlayingDialog() }
        binding.songsChip.setOnClickListener { changeSearchMode(SearchMode.SONGS) }
        binding.videosChip.setOnClickListener { changeSearchMode(SearchMode.VIDEOS) }
        updateSearchChips()
        binding.searchBox.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) { searchOrPlay(); true } else false
        }
        binding.toggleButton.setOnClickListener {
            controller?.let { if (it.isPlaying) it.pause() else if (it.mediaItemCount > 0) it.play() }
        }
        binding.backButton.setOnClickListener { seekBy(-15_000) }
        binding.forwardButton.setOnClickListener { seekBy(15_000) }
        binding.openButton.setOnClickListener {
            val url = currentVideoUrl ?: return@setOnClickListener
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            catch (_: Exception) { showStatus("No app can open this video link.", true) }
        }
        binding.progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) = Unit
            override fun onStartTrackingTouch(bar: SeekBar?) { seekInProgress = true }
            override fun onStopTrackingTouch(bar: SeekBar?) {
                val c = controller
                if (c != null && c.duration > 0 && c.duration != C.TIME_UNSET) {
                    c.seekTo((c.duration * (bar?.progress ?: 0) / 1_000L).coerceIn(0, c.duration))
                }
                seekInProgress = false
            }
        })
        handleIncoming(intent)
        openPage(Page.HOME)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    private fun handleIncoming(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            val url = VideoLink.normalize(shared)
            if (url != null) {
                binding.searchBox.setText(url)
                if (active) playUrl(url) else pendingLink = url
            }
        }
    }

    override fun onStart() {
        super.onStart()
        active = true
        val f = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java)))
            .buildAsync()
        future = f
        f.addListener({
            try {
                val connected = f.get()
                if (!active || future !== f) return@addListener
                controller = connected
                connected.addListener(playerListener)
                showPlayerState(connected)
                val queued = waitingToPlay
                waitingToPlay = null
                if (queued != null) startPlayback(queued)
                else pendingLink?.let { link -> pendingLink = null; playUrl(link) }
            } catch (_: Exception) {
                showStatus("Media service unavailable. Restart the app.", true)
            }
        }, java.util.concurrent.Executor { command -> main.post(command) })
        main.post(refresh)
    }

    override fun onStop() {
        active = false
        main.removeCallbacks(refresh)
        controller?.removeListener(playerListener)
        controller = null
        future?.let { MediaController.releaseFuture(it) }
        future = null
        super.onStop()
    }

    override fun onDestroy() {
        token++
        worker.shutdownNow()
        thumbs.shutdown()
        super.onDestroy()
    }

    private fun searchOrPlay() {
        val input = binding.searchBox.text.toString()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(binding.searchBox.windowToken, 0)
        binding.searchBox.clearFocus()
        openPage(Page.SEARCH)
        when (val parsed = SearchInputResolver.resolve(input)) {
            is SearchInput.VideoUrl -> playUrl(parsed.url)
            is SearchInput.Keywords -> search(parsed.query)
            SearchInput.BadLink -> showStatus("This is not a valid public YouTube video link.", true)
            SearchInput.Blank -> showStatus("Type a song or artist name to search.", true)
        }
    }

    private fun changeSearchMode(mode: SearchMode) {
        if (mode == searchMode) return
        searchMode = mode
        updateSearchChips()
        val input = binding.searchBox.text.toString().trim()
        if (SearchInputResolver.resolve(input) is SearchInput.Keywords) search(input)
    }

    private fun updateSearchChips() {
        val songs = searchMode == SearchMode.SONGS
        binding.songsChip.setBackgroundResource(if (songs) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
        binding.videosChip.setBackgroundResource(if (songs) R.drawable.bg_chip_normal else R.drawable.bg_chip_selected)
        binding.songsChip.setTextColor(if (songs) Color.rgb(11, 13, 15) else Color.WHITE)
        binding.videosChip.setTextColor(if (songs) Color.WHITE else Color.rgb(11, 13, 15))
    }

    private fun search(query: String) {
        val id = ++token
        val selectedMode = searchMode
        page = Page.SEARCH
        updateTabs()
        waitingToPlay = null
        binding.resultsContainer.removeAllViews()
        binding.resultsHeading.text = if (selectedMode == SearchMode.SONGS) "Song results" else "Video results"
        binding.searchProgress.visibility = View.VISIBLE
        binding.resultsScroll.scrollTo(0, 0)
        showStatus("Searching for “" + query + "”…")
        worker.execute {
            val outcome = runCatching { gateway.search(query, selectedMode) }
            main.post {
                if (!active || id != token || page != Page.SEARCH) return@post
                binding.searchProgress.visibility = View.GONE
                outcome.fold(
                    onSuccess = { result ->
                        val videos = result.videos
                        results = videos
                        showStatus(when {
                            videos.isEmpty() -> "No matches found. Try a different song or artist name."
                            result.usedVideoFallback -> "Music results unavailable; showing " + videos.size + " YouTube videos. Tap to play."
                            else -> videos.size.toString() + " results • Tap a thumbnail to listen"
                        })
                        videos.forEach { addVideoCard(it, videos) }
                    },
                    onFailure = { showStatus("Search failed: " + errorText(it), true) }
                )
            }
        }
    }

    private fun playUrl(url: String, replaceQueue: Boolean = true) {
        if (replaceQueue) {
            val preview = results.firstOrNull { it.url == url }
                ?: library.recent().firstOrNull { it.url == url }
                ?: VideoPreview("YouTube video", "YouTube", url, "", 0)
            setQueue(listOf(preview), 0)
        }
        val id = ++token
        binding.searchProgress.visibility = View.GONE
        waitingToPlay = null
        showStatus("Finding an audio stream…")
        binding.playerPanel.visibility = View.VISIBLE
        binding.currentTitle.text = "Loading video…"
        binding.currentCreator.text = "Connecting to YouTube"
        worker.execute {
            val outcome = runCatching { gateway.resolve(url) }
            main.post {
                if (id != token) return@post
                outcome.fold(
                    onSuccess = { playable ->
                        if (active) startPlayback(playable) else waitingToPlay = playable
                    },
                    onFailure = { showStatus(errorText(it), true); binding.currentTitle.text = "Couldn't play this video" }
                )
            }
        }
    }

    private fun startPlayback(playable: PlayableAudio) {
        val player = controller
        if (player == null) {
            waitingToPlay = playable
            showStatus("Connecting to the background player…")
            return
        }
        val video = playable.video
        currentVideoUrl = video.url
        library.addRecent(video)
        lastRecordedUrl = video.url
        updateFavoriteButton()
        val metadata = MediaMetadata.Builder()
            .setTitle(video.title)
            .setArtist(video.author)
            .apply { if (video.thumbnail.isNotEmpty()) setArtworkUri(Uri.parse(video.thumbnail)) }
            .build()
        val item = MediaItem.Builder()
            .setMediaId(video.url)
            .setUri(playable.audioUrl)
            .apply { playable.mimeType?.let { setMimeType(it) } }
            .setMediaMetadata(metadata)
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.play()
        binding.currentTitle.text = video.title
        binding.currentCreator.text = video.author
        binding.playerPanel.visibility = View.VISIBLE
        showStatus("Now playing • You can lock your screen")
    }

    private fun showPlayerState(player: Player) {
        if (player.mediaItemCount == 0) return
        binding.playerPanel.visibility = View.VISIBLE
        val item = player.currentMediaItem
        currentVideoUrl = item?.mediaId?.takeIf { VideoLink.normalize(it) != null } ?: currentVideoUrl
        binding.currentTitle.text = item?.mediaMetadata?.title ?: "Playing"
        binding.currentCreator.text = item?.mediaMetadata?.artist ?: ""
        binding.toggleButton.text = if (player.isPlaying) "Ⅱ" else "▶"
        val current = currentVideo()
        if (current != null && lastRecordedUrl != current.url) {
            library.addRecent(current)
            lastRecordedUrl = current.url
            if (page == Page.HOME || page == Page.LIBRARY) openPage(page)
        }
        updateFavoriteButton()
        val duration = player.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0
        val position = player.currentPosition.coerceAtLeast(0)
        binding.timeText.text = "${TimeLabel.format(position)} / ${TimeLabel.format(duration)}"
        if (!seekInProgress) {
            binding.progress.progress = if (duration > 0) ((position.coerceAtMost(duration) * 1000) / duration).toInt() else 0
        }
        binding.progress.isEnabled = duration > 0
    }

    private fun seekBy(offset: Long) {
        val player = controller ?: return
        if (player.mediaItemCount == 0) return
        val end = player.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: Long.MAX_VALUE
        player.seekTo((player.currentPosition + offset).coerceIn(0L, end))
    }

    /** Compact music-first track row. Long-press or tap ⋮ for queue and playlist actions. */
    private fun addVideoCard(
        video: VideoPreview,
        collection: List<VideoPreview> = results,
        playlist: String? = null
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(8), dp(3), dp(8))
            isClickable = true
            isFocusable = true
            contentDescription = "Play " + video.title + " by " + video.author
            setOnClickListener { playCollection(video, collection) }
            setOnLongClickListener { showSongActions(video, playlist); true }
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_nightwave)
            background = GradientDrawable().apply {
                setColor(Color.rgb(33, 40, 37))
                cornerRadius = dp(10).toFloat()
            }
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(dp(74), dp(74))
        }
        thumbs.load(video.thumbnail, image)
        card.addView(image)
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), 0, dp(6), 0)
            layoutParams = LinearLayout.LayoutParams(0, dp(74), 1f)
        }
        details.addView(TextView(this).apply {
            text = video.title
            setTextColor(Color.rgb(242, 247, 242))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        details.addView(TextView(this).apply {
            text = video.author + (if (video.durationSeconds > 0) "  •  " + TimeLabel.format(video.durationSeconds * 1_000) else "")
            setTextColor(Color.rgb(159, 173, 164))
            textSize = 12f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) }
        })
        card.addView(details)
        card.addView(TextView(this).apply {
            text = "⋮"
            gravity = Gravity.CENTER
            textSize = 25f
            setTextColor(Color.rgb(185, 201, 177))
            contentDescription = "More options for " + video.title
            isClickable = true
            isFocusable = true
            setOnClickListener { showSongActions(video, playlist) }
        }, LinearLayout.LayoutParams(dp(42), dp(54)))
        binding.resultsContainer.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(2)
        })
    }

    private fun currentVideo(): VideoPreview? {
        val media = controller?.currentMediaItem ?: return null
        val url = VideoLink.normalize(media.mediaId) ?: return null
        return VideoPreview(
            media.mediaMetadata.title?.toString() ?: "Untitled song",
            media.mediaMetadata.artist?.toString() ?: "YouTube creator",
            url,
            media.mediaMetadata.artworkUri?.toString().orEmpty(),
            0
        )
    }

    private fun updateFavoriteButton() {
        val video = currentVideo()
        val liked = video != null && library.isFavorite(video.url)
        binding.favoriteButton.text = if (liked) "♥" else "♡"
        binding.favoriteButton.setTextColor(if (liked) Color.rgb(178, 243, 105) else Color.WHITE)
    }

    private fun sendQueueAction(action: String, extras: (Intent.() -> Unit)? = null) {
        try {
            val intent = Intent(this, PlaybackService::class.java).setAction(action)
            extras?.invoke(intent)
            startService(intent)
        } catch (error: Exception) {
            showStatus("Could not update the playback queue: " + errorText(error), true)
        }
    }

    private fun setQueue(items: List<VideoPreview>, initial: Int) {
        val songs = items.distinctBy { it.url }.take(100)
        if (songs.isEmpty()) return
        val selectedUrl = items.getOrNull(initial)?.url
        sendQueueAction(PlaybackService.ACTION_REPLACE) {
            putStringArrayListExtra(PlaybackService.EXTRA_URLS, ArrayList(songs.map { it.url }))
            putStringArrayListExtra(PlaybackService.EXTRA_TITLES, ArrayList(songs.map { it.title }))
            putStringArrayListExtra(PlaybackService.EXTRA_CREATORS, ArrayList(songs.map { it.author }))
            putStringArrayListExtra(PlaybackService.EXTRA_ARTWORKS, ArrayList(songs.map { it.thumbnail }))
            putExtra(PlaybackService.EXTRA_INDEX, songs.indexOfFirst { it.url == selectedUrl }.coerceAtLeast(0))
        }
    }

    private fun addToQueue(video: VideoPreview) {
        sendQueueAction(PlaybackService.ACTION_ENQUEUE) {
            putExtra(PlaybackService.EXTRA_URLS, video.url)
            putExtra(PlaybackService.EXTRA_TITLES, video.title)
            putExtra(PlaybackService.EXTRA_CREATORS, video.author)
            putExtra(PlaybackService.EXTRA_ARTWORKS, video.thumbnail)
        }
        showStatus("Added to the playback queue: " + video.title)
    }

    private fun playCollection(video: VideoPreview, collection: List<VideoPreview>) {
        val list = if (collection.any { it.url == video.url }) collection else listOf(video)
        setQueue(list, list.indexOfFirst { it.url == video.url })
        playUrl(video.url, false)
    }

    private fun updateTabs() {
        val activeColor = Color.rgb(178, 243, 105)
        val inactive = Color.rgb(166, 181, 172)
        binding.homeTab.setTextColor(if (page == Page.HOME) activeColor else inactive)
        binding.searchTab.setTextColor(if (page == Page.SEARCH) activeColor else inactive)
        binding.libraryTab.setTextColor(if (page == Page.LIBRARY) activeColor else inactive)
        binding.topSearchArea.visibility = View.VISIBLE
        binding.searchRow.visibility = if (page == Page.SEARCH) View.VISIBLE else View.GONE
        binding.filtersRow.visibility = if (page == Page.SEARCH) View.VISIBLE else View.GONE
        binding.searchProgress.visibility = View.GONE
    }

    private fun openPage(destination: Page) {
        page = destination
        updateTabs()
        binding.resultsContainer.removeAllViews()
        binding.resultsScroll.scrollTo(0, 0)
        when (destination) {
            Page.HOME -> showHome()
            Page.SEARCH -> {
                binding.resultsHeading.text = if (searchMode == SearchMode.SONGS) "Explore songs" else "Explore videos"
                showStatus(if (results.isEmpty()) "Search any song, album or artist to start listening." else "Tap a song to play. Hold for more options.")
                results.forEach { addVideoCard(it, results) }
            }
            Page.LIBRARY -> showLibrary()
        }
    }

    private fun sectionTitle(title: String) {
        binding.resultsContainer.addView(TextView(this).apply {
            text = title
            setTextColor(Color.rgb(242, 246, 242))
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), dp(22), dp(4), dp(12))
        })
    }

    private fun labelAction(label: String, tap: () -> Unit) {
        binding.resultsContainer.addView(TextView(this).apply {
            text = label
            setTextColor(Color.rgb(178, 243, 105))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = GradientDrawable().apply {
                setColor(Color.rgb(26, 34, 27))
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener { tap() }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(11) })
    }

    private fun showHome() {
        binding.resultsHeading.text = "Good to have you back"
        showStatus("Your music, your mood. No account required.")
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(23), dp(22), dp(23))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(51, 86, 57), Color.rgb(27, 39, 35), Color.rgb(19, 25, 27))
            ).apply { cornerRadius = dp(22).toFloat() }
        }
        hero.addView(TextView(this).apply {
            text = "JUST PRESS PLAY"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(178, 243, 105))
        })
        hero.addView(TextView(this).apply {
            text = "Find the sound\nfor right now."
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(10), 0, dp(10))
        })
        hero.addView(TextView(this).apply {
            text = "Search songs, build playlists and listen with the screen off."
            textSize = 13f
            setTextColor(Color.rgb(207, 222, 210))
        })
        hero.setOnClickListener { openPage(Page.SEARCH) }
        binding.resultsContainer.addView(hero, LinearLayout.LayoutParams(-1, -2))
        val recent = library.recent()
        if (recent.isNotEmpty()) {
            sectionTitle("Recently played")
            recent.take(6).forEach { addVideoCard(it, recent) }
        }
        val favorites = library.favorites()
        sectionTitle("Liked songs  ♥")
        if (favorites.isEmpty()) {
            labelAction("♡  Save your favorite songs while listening") { openPage(Page.SEARCH) }
        } else {
            favorites.take(5).forEach { addVideoCard(it, favorites) }
        }
        labelAction("⌕  Search more music") { openPage(Page.SEARCH) }
    }

    private fun showLibrary() {
        binding.resultsHeading.text = "Your Library"
        showStatus("Saved locally on this device • No sign-in needed")
        val favorites = library.favorites()
        labelAction("♥  Liked Songs  •  " + favorites.size + " tracks") {
            showCollection("Liked Songs", favorites)
        }
        labelAction("＋  Create a playlist") { createPlaylistDialog() }
        sectionTitle("Your playlists")
        val names = library.playlistNames()
        if (names.isEmpty()) {
            binding.resultsContainer.addView(TextView(this).apply {
                text = "No playlists yet. Create one to save music for later."
                textSize = 13f
                setTextColor(Color.rgb(166, 181, 172))
                setPadding(dp(7), dp(8), dp(7), dp(16))
            })
        }
        names.forEach { name ->
            labelAction("▤  " + name + "   •   " + library.playlist(name).size + " songs") {
                showCollection(name, library.playlist(name), name)
            }
        }
        if (library.recent().isNotEmpty()) {
            sectionTitle("Listening history")
            val recent = library.recent()
            recent.take(20).forEach { addVideoCard(it, recent) }
        }
    }

    private fun showCollection(title: String, items: List<VideoPreview>, playlist: String? = null) {
        page = Page.LIBRARY
        updateTabs()
        binding.resultsScroll.scrollTo(0, 0)
        binding.resultsHeading.text = title
        showStatus(items.size.toString() + " saved songs • Tap to play")
        binding.resultsContainer.removeAllViews()
        labelAction("←  Back to library") { openPage(Page.LIBRARY) }
        if (items.isNotEmpty()) labelAction("▶  Play all") { playCollection(items.first(), items) }
        items.forEach { addVideoCard(it, items, playlist) }
    }

    private fun createPlaylistDialog() {
        val input = EditText(this).apply {
            hint = "e.g. Late night drive"
            setSingleLine(true)
            setPadding(dp(22), dp(15), dp(22), dp(15))
        }
        AlertDialog.Builder(this)
            .setTitle("New playlist")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                if (library.createPlaylist(input.text.toString())) {
                    openPage(Page.LIBRARY)
                    showStatus("Playlist created. Add songs using the ⋮ menu.")
                } else showStatus("Use a unique name (up to 60 characters).", true)
            }.show()
    }

    private fun showPlaylistPicker(video: VideoPreview) {
        val names = library.playlistNames()
        val options = arrayOf("＋ Create new playlist", *names.toTypedArray())
        AlertDialog.Builder(this).setTitle("Save “" + video.title.take(38) + "”")
            .setItems(options) { _, which ->
                if (which == 0) {
                    val input = EditText(this).apply {
                        hint = "Playlist name"
                        setSingleLine(true)
                    }
                    AlertDialog.Builder(this).setTitle("Create playlist").setView(input)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Create and save") { _, _ ->
                            val name = input.text.toString().trim()
                            if (library.createPlaylist(name) && library.addToPlaylist(name, video)) {
                                showStatus("Saved to " + name)
                                if (page == Page.LIBRARY) openPage(page)
                            } else showStatus("Could not create or save this playlist.", true)
                        }.show()
                } else {
                    val name = names[which - 1]
                    if (library.addToPlaylist(name, video)) showStatus("Saved to " + name)
                    else showStatus("Already in " + name + " or playlist is full.")
                    if (page == Page.LIBRARY) openPage(page)
                }
            }.show()
    }

    private fun showSongActions(video: VideoPreview, playlist: String? = null) {
        val options = mutableListOf(
            if (library.isFavorite(video.url)) "♥ Unlike song" else "♡ Like song",
            "＋ Add to playlist",
            "≡ Add to playing queue",
            "↗ Open original on YouTube"
        )
        if (playlist != null) options.add("Remove from this playlist")
        AlertDialog.Builder(this).setTitle(video.title)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        library.toggleFavorite(video)
                        showStatus("Liked songs updated")
                        if (page != Page.SEARCH) openPage(page)
                    }
                    1 -> showPlaylistPicker(video)
                    2 -> addToQueue(video)
                    3 -> {
                        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.url))) }
                        catch (_: Exception) { showStatus("No browser available.", true) }
                    }
                    4 -> if (playlist != null) {
                        library.removeFromPlaylist(playlist, video.url)
                        showCollection(playlist, library.playlist(playlist), playlist)
                    }
                }
            }.show()
    }

    private fun showQueueDialog() {
        val state = QueueStateStore(this).read()
        if (state.tracks.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Playing queue")
                .setMessage("Search a song and tap Play to create a queue. You can also add tracks using ⋮.")
                .setPositiveButton("Got it", null).show()
            return
        }
        val labels = state.tracks.mapIndexed { index, song ->
            (if (index == state.index) "▶  " else "    ") + song.title.take(55) +
                "\n    " + song.creator.take(50)
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Up next  •  " + state.tracks.size + " tracks")
            .setSingleChoiceItems(labels, state.index) { dialog, selected ->
                sendQueueAction(PlaybackService.ACTION_JUMP) { putExtra(PlaybackService.EXTRA_INDEX, selected) }
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun openNowPlayingDialog() {
        val video = currentVideo() ?: return
        val artwork = ImageView(this).apply {
            setImageResource(R.drawable.ic_nightwave)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(-1, dp(255)).apply { bottomMargin = dp(20) }
        }
        thumbs.load(video.thumbnail, artwork)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(14))
            addView(artwork)
            addView(TextView(this@MainActivity).apply {
                text = video.title
                textSize = 23f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                maxLines = 2
            })
            addView(TextView(this@MainActivity).apply {
                text = video.author
                textSize = 14f
                setTextColor(Color.rgb(174, 191, 174))
                setPadding(0, dp(7), 0, dp(12))
            })
        }
        val dialog = AlertDialog.Builder(this).setTitle("Now Playing")
            .setView(content)
            .setNeutralButton("Repeat: " + listOf("Off", "All", "One")[repeat]) { _, _ ->
                repeat = (repeat + 1) % 3
                sendQueueAction(PlaybackService.ACTION_REPEAT)
                showStatus("Repeat: " + listOf("Off", "All", "One")[repeat])
            }
            .setNegativeButton("Add to playlist") { _, _ -> showPlaylistPicker(video) }
            .setPositiveButton("Done", null).create()
        dialog.show()
    }

    private fun showStatus(message: String, isError: Boolean = false) {
        binding.statusText.text = message
        binding.statusText.setTextColor(if (isError) Color.rgb(255, 169, 154) else Color.rgb(167, 178, 171))
    }

    private fun errorText(error: Throwable): String = when (error) {
        is java.net.UnknownHostException -> "No internet connection. Connect and retry."
        is java.net.SocketTimeoutException -> "Connection timed out. Try again."
        else -> "Could not access this video: ${error.message?.take(140) ?: "Unsupported or temporarily blocked."}"
    }

    private fun dp(value: Int) = (resources.displayMetrics.density * value + 0.5f).toInt()
}
