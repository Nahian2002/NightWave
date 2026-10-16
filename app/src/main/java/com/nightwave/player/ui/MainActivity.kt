package com.nightwave.player.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
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
import android.widget.ScrollView
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
    private var lastArtworkUri: String? = null
    private var websiteDialog: android.app.Dialog? = null
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
        binding.nativeSearchButton.setOnClickListener {
            val typed = binding.searchBox.text.toString().trim()
            if (typed.isNotEmpty()) {
                when (val parsed = SearchInputResolver.resolve(typed)) {
                    is SearchInput.Keywords -> search(parsed.query)
                    is SearchInput.VideoUrl -> playUrl(parsed.url)
                    else -> showStatus("Enter a song or artist to search.", true)
                }
            } else showStatus("Enter a song or artist to search.", true)
        }
        binding.searchButton.setOnClickListener {
            binding.searchButton.animate().scaleX(0.89f).scaleY(0.89f).setDuration(85)
                .withEndAction {
                    binding.searchButton.animate().scaleX(1f).scaleY(1f).setDuration(130).start()
                }.start()
            searchOrPlay()
        }
        binding.homeTab.setOnClickListener { openPage(Page.HOME) }
        binding.searchTab.setOnClickListener { openPage(Page.SEARCH) }
        binding.libraryTab.setOnClickListener { openPage(Page.LIBRARY) }
        binding.prevTrackButton.setOnClickListener { sendQueueAction(PlaybackService.ACTION_PREVIOUS) }
        binding.nextTrackButton.setOnClickListener { sendQueueAction(PlaybackService.ACTION_NEXT) }
        binding.shuffleButton.setTextColor(
            if (shuffle) Color.rgb(193, 175, 255) else Color.rgb(195, 194, 217)
        )
        binding.shuffleButton.setOnClickListener {
            shuffle = !shuffle
            sendQueueAction(PlaybackService.ACTION_SHUFFLE)
            binding.shuffleButton.setTextColor(if (shuffle) Color.rgb(193, 175, 255) else Color.rgb(195, 194, 217))
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
        binding.searchBox.setOnFocusChangeListener { view, focused ->
            view.animate().scaleX(if (focused) 1.015f else 1f)
                .scaleY(if (focused) 1.015f else 1f).setDuration(175).start()
        }
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
        intent?.getStringExtra("nightwave_web_search")?.takeIf { it.isNotBlank() }?.let { term ->
            openPage(Page.SEARCH)
            binding.searchBox.setText(term)
            openYouTubeWebsite(term)
        }
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
        websiteDialog?.dismiss()
        websiteDialog = null
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
            is SearchInput.Keywords -> openYouTubeWebsite(parsed.query)
            SearchInput.BadLink -> showStatus("This is not a valid public YouTube video link.", true)
            SearchInput.Blank -> showStatus("Type a song or artist name to search.", true)
        }
    }

    /** YouTube's own result browser avoids the fragile third-party search index. */
    private fun openYouTubeWebsite(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            showStatus("Type a song or artist name.", true)
            return
        }
        binding.resultsHeading.text = "Searching YouTube"
        showStatus("Choose a video in the YouTube search screen. NightWave will handle playback.")
        websiteDialog?.dismiss()
        websiteDialog = YouTubeSearchBrowser(this).show(clean) { videoUrl ->
            binding.resultsHeading.text = "Now listening"
            playUrl(videoUrl)
        }
    }

    private fun changeSearchMode(mode: SearchMode) {
        if (mode == searchMode) return
        searchMode = mode
        updateSearchChips()
        val input = binding.searchBox.text.toString().trim()
        // Selecting Songs/Videos doesn't retry a known-failing external search backend.
        // The main search button always opens YouTube's own search page.
    }

    private fun updateSearchChips() {
        val songs = searchMode == SearchMode.SONGS
        binding.songsChip.setBackgroundResource(if (songs) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
        binding.videosChip.setBackgroundResource(if (songs) R.drawable.bg_chip_normal else R.drawable.bg_chip_selected)
        binding.songsChip.setTextColor(if (songs) Color.rgb(21, 20, 41) else Color.rgb(215, 216, 234))
        binding.videosChip.setTextColor(if (songs) Color.rgb(215, 216, 234) else Color.rgb(21, 20, 41))
        val selected = if (songs) binding.songsChip else binding.videosChip
        selected.animate().scaleX(1.035f).scaleY(1.035f).setDuration(125).start()
        (if (songs) binding.videosChip else binding.songsChip).animate()
            .scaleX(1f).scaleY(1f).setDuration(125).start()
    }

    private fun search(query: String) {
        val id = ++token
        val selectedMode = searchMode
        page = Page.SEARCH
        updateTabs()
        waitingToPlay = null
        binding.resultsContainer.removeAllViews()
        binding.resultsHeading.text = if (selectedMode == SearchMode.SONGS) "Discover your sound" else "Explore videos"
        binding.searchProgress.visibility = View.VISIBLE
        binding.resultsScroll.scrollTo(0, 0)
        showStatus("Finding the best matches for “" + query + "”…")
        worker.execute {
            val outcome = runCatching { gateway.search(query, selectedMode) }
            main.post {
                if (!active || id != token || page != Page.SEARCH) return@post
                binding.searchProgress.visibility = View.GONE
                outcome.fold(
                    onSuccess = { result ->
                        val videos = result.videos
                        results = videos
                        binding.resultsHeading.text = "Results for “" + query.take(34) + "”"
                        showStatus(videos.size.toString() + " videos  ·  " +
                            result.note + "  ·  Tap to listen")
                        videos.forEach { addVideoCard(it, videos) }
                        animatePage()
                    },
                    onFailure = {
                        binding.resultsHeading.text = "Couldn't load search"
                        showStatus("Both direct and independent search providers are unavailable. " +
                            "Please retry when connected.", true)
                        labelAction("▶  Browse YouTube results directly") {
                            openYouTubeWebsite(query)
                        }
                        labelAction("↻  Retry native results") { search(query) }
                        labelAction("ⓘ  Connection details") {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Search diagnostics")
                                .setMessage(searchErrorText(it))
                                .setPositiveButton("OK", null).show()
                        }
                        sectionTitle("Browse another vibe")
                        showMoodGrid()
                    }
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
        setPlayerArtwork(video.thumbnail)
        binding.playerPanel.visibility = View.VISIBLE
        binding.playerPanel.alpha = 0f
        binding.playerPanel.translationY = dp(28).toFloat()
        binding.playerPanel.animate().alpha(1f).translationY(0f).setDuration(320).start()
        showStatus("Now playing • You can lock your screen")
    }

    private fun showPlayerState(player: Player) {
        if (player.mediaItemCount == 0) return
        binding.playerPanel.visibility = View.VISIBLE
        val item = player.currentMediaItem
        currentVideoUrl = item?.mediaId?.takeIf { VideoLink.normalize(it) != null } ?: currentVideoUrl
        binding.currentTitle.text = item?.mediaMetadata?.title ?: "Playing"
        binding.currentCreator.text = item?.mediaMetadata?.artist ?: ""
        setPlayerArtwork(item?.mediaMetadata?.artworkUri?.toString().orEmpty())
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

    private fun setPlayerArtwork(uri: String) {
        if (lastArtworkUri == uri) return
        lastArtworkUri = uri
        binding.playerArtwork.setImageResource(R.drawable.ic_nightwave)
        if (uri.isNotBlank()) thumbs.load(uri, binding.playerArtwork)
        binding.playerArtwork.animate().alpha(0.48f).setDuration(90).withEndAction {
            binding.playerArtwork.animate().alpha(1f).setDuration(260).start()
        }.start()
    }

    private fun animatePage() {
        binding.resultsContainer.alpha = 0f
        binding.resultsContainer.translationY = dp(15).toFloat()
        binding.resultsContainer.animate().alpha(1f).translationY(0f)
            .setDuration(260).start()
    }

    private fun searchErrorText(error: Throwable): String {
        var root = error
        while (root.cause != null && root.cause !== root) root = root.cause!!
        return when (root) {
            is java.net.UnknownHostException -> "You're offline. Connect to the internet and retry."
            is java.net.SocketTimeoutException -> "YouTube didn't respond in time. Try again."
            else -> "YouTube search could not load results. Try Videos or retry. " +
                (error.message?.take(190) ?: "The extractor may need an update.")
        }
    }

    private fun seekBy(offset: Long) {
        val player = controller ?: return
        if (player.mediaItemCount == 0) return
        val end = player.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: Long.MAX_VALUE
        player.seekTo((player.currentPosition + offset).coerceIn(0L, end))
    }

    /** Compact elevated track tile with cover art and a working actions menu. */
    private fun addVideoCard(
        video: VideoPreview,
        collection: List<VideoPreview> = results,
        playlist: String? = null
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(9), dp(6), dp(9))
            setBackgroundResource(R.drawable.bg_result_item)
            elevation = dp(2).toFloat()
            isClickable = true
            isFocusable = true
            contentDescription = "Play " + video.title + " by " + video.author
            setOnClickListener {
                animate().scaleX(0.97f).scaleY(0.97f).setDuration(85)
                    .withEndAction { animate().scaleX(1f).scaleY(1f).setDuration(160).start() }
                    .start()
                playCollection(video, collection)
            }
            setOnLongClickListener { showSongActions(video, playlist); true }
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_nightwave)
            setBackgroundResource(R.drawable.bg_search_hint)
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(dp(65), dp(65))
        }
        thumbs.load(video.thumbnail, image)
        card.addView(image)
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), 0, dp(5), 0)
            layoutParams = LinearLayout.LayoutParams(0, dp(65), 1f)
        }
        details.addView(TextView(this).apply {
            text = video.title
            setTextColor(Color.rgb(249, 249, 255))
            textSize = 14.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        details.addView(TextView(this).apply {
            text = video.author + if (video.durationSeconds > 0)
                "   ·   " + TimeLabel.format(video.durationSeconds * 1_000)
            else ""
            setTextColor(Color.rgb(158, 167, 198))
            textSize = 12f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) }
        })
        card.addView(details)
        card.addView(TextView(this).apply {
            text = "⋮"
            gravity = Gravity.CENTER
            textSize = 24f
            setTextColor(Color.rgb(190, 181, 255))
            contentDescription = "Options for " + video.title
            isClickable = true
            isFocusable = true
            setOnClickListener { showSongActions(video, playlist) }
        }, LinearLayout.LayoutParams(dp(37), dp(48)))
        binding.resultsContainer.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
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
        binding.favoriteButton.setTextColor(if (liked) Color.rgb(255, 169, 197) else Color.rgb(222, 221, 237))
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
        val activeColor = Color.rgb(193, 175, 255)
        val inactive = Color.rgb(159, 164, 194)
        binding.homeTab.setTextColor(if (page == Page.HOME) activeColor else inactive)
        binding.searchTab.setTextColor(if (page == Page.SEARCH) activeColor else inactive)
        binding.libraryTab.setTextColor(if (page == Page.LIBRARY) activeColor else inactive)
        binding.topSearchArea.visibility = View.VISIBLE
        binding.searchRow.visibility = if (page == Page.SEARCH) View.VISIBLE else View.GONE
        binding.filtersRow.visibility = if (page == Page.SEARCH) View.VISIBLE else View.GONE
        binding.nativeSearchButton.visibility = if (page == Page.SEARCH) View.VISIBLE else View.GONE
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
                binding.resultsHeading.text = "Search YouTube by name"
                showStatus(if (results.isEmpty())
                    "Search on YouTube's own website or choose a mood to browse music."
                    else "Tap a song to play, or hold for more options.")
                if (results.isEmpty()) {
                    sectionTitle("Pick a vibe")
                    showMoodGrid()
                } else {
                    results.forEach { addVideoCard(it, results) }
                }
            }
            Page.LIBRARY -> showLibrary()
        }
        animatePage()
    }

    /** Functional discovery cards, not fake song listings or invented recommendations. */
    private fun showMoodGrid() {
        val moods = listOf(
            arrayOf("MIDNIGHT", "Night Drive", "night drive music", "✦"),
            arrayOf("SLOW DOWN", "Chill & Lofi", "lofi chill music", "◌"),
            arrayOf("BENGAL BEATS", "Bangla Hits", "popular bangla songs", "♫"),
            arrayOf("HIGH ENERGY", "Workout", "workout music mix", "ϟ"),
            arrayOf("YOUR ERA", "Bollywood", "popular hindi songs", "♡"),
            arrayOf("LATE HOURS", "Soft & Acoustic", "acoustic love songs", "☾")
        )
        val palettes = listOf(
            intArrayOf(0xFF5B447B.toInt(), 0xFF222E5E.toInt()),
            intArrayOf(0xFF175B6B.toInt(), 0xFF263565.toInt()),
            intArrayOf(0xFF8C476A.toInt(), 0xFF462852.toInt()),
            intArrayOf(0xFF915C38.toInt(), 0xFF47325D.toInt()),
            intArrayOf(0xFF6658A6.toInt(), 0xFF2F3E69.toInt()),
            intArrayOf(0xFF276A64.toInt(), 0xFF283A68.toInt())
        )
        for (rowIndex in 0..2) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
            }
            for (column in 0..1) {
                val idx = rowIndex * 2 + column
                val (eyebrow, title, query, symbol) = moods[idx].toList()
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(15), dp(12), dp(12), dp(12))
                    background = GradientDrawable(
                        GradientDrawable.Orientation.TL_BR, palettes[idx]
                    ).apply { cornerRadius = dp(19).toFloat() }
                    elevation = dp(3).toFloat()
                    isClickable = true
                    isFocusable = true
                    contentDescription = "Search " + title
                    setOnClickListener {
                        binding.searchBox.setText(query)
                        openYouTubeWebsite(query)
                    }
                }
                card.addView(TextView(this).apply {
                    text = symbol
                    textSize = 34f
                    gravity = Gravity.END
                    setTextColor(Color.rgb(226, 229, 255))
                }, LinearLayout.LayoutParams(-1, dp(47)))
                card.addView(TextView(this).apply {
                    text = eyebrow
                    textSize = 10f
                    letterSpacing = 0.09f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.rgb(205, 220, 239))
                })
                card.addView(TextView(this).apply {
                    text = title
                    textSize = 19f
                    typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    setPadding(0, dp(5), 0, 0)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
                row.addView(card, LinearLayout.LayoutParams(0, dp(147), 1f).apply {
                    if (column == 0) marginEnd = dp(10)
                })
            }
            binding.resultsContainer.addView(row)
        }
    }

    private fun sectionTitle(title: String) {
        binding.resultsContainer.addView(TextView(this).apply {
            text = title
            setTextColor(Color.rgb(248, 248, 255))
            textSize = 20f
            letterSpacing = -0.022f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(4), dp(24), dp(4), dp(13))
        })
    }

    private fun labelAction(label: String, tap: () -> Unit) {
        binding.resultsContainer.addView(TextView(this).apply {
            text = label + "   ↗"
            setTextColor(Color.rgb(210, 204, 255))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(18), dp(17), dp(16), dp(17))
            setBackgroundResource(R.drawable.bg_result_item)
            elevation = dp(2).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener {
                animate().scaleX(0.97f).scaleY(0.97f).setDuration(75).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(125).start()
                }.start()
                tap()
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(11) })
    }

    private fun showHome() {
        binding.resultsHeading.text = "Made for your mood."
        showStatus("Your own listening space, always on.")
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(24), dp(14), dp(24))
            setBackgroundResource(R.drawable.bg_hero)
            elevation = dp(4).toFloat()
            isClickable = true
            isFocusable = true
            contentDescription = "Explore music"
        }
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        copy.addView(TextView(this).apply {
            text = "✦  YOUR NEXT OBSESSION"
            textSize = 10f
            letterSpacing = 0.12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(183, 232, 233))
        })
        copy.addView(TextView(this).apply {
            text = "Press play.\nFade away."
            textSize = 28f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            letterSpacing = -0.04f
            setTextColor(Color.WHITE)
            setPadding(0, dp(14), 0, dp(14))
        })
        copy.addView(TextView(this).apply {
            text = "Millions of public videos to explore.\nYour soundtrack, your rules."
            textSize = 12f
            setTextColor(Color.rgb(216, 215, 241))
            setLineSpacing(dp(3).toFloat(), 1f)
        })
        copy.addView(TextView(this).apply {
            text = "EXPLORE  ↗"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(199, 192, 255))
            setPadding(0, dp(21), 0, 0)
        })
        hero.addView(copy)
        val emblem = TextView(this).apply {
            text = "◉"
            gravity = Gravity.CENTER
            textSize = 78f
            setTextColor(Color.rgb(173, 226, 238))
            rotation = -18f
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(82, 74, 159), Color.rgb(35, 130, 151))
            ).apply { cornerRadius = dp(19).toFloat() }
            elevation = dp(8).toFloat()
            layoutParams = LinearLayout.LayoutParams(dp(102), dp(124)).apply {
                marginStart = dp(13)
            }
        }
        hero.addView(emblem)
        hero.setOnClickListener { openPage(Page.SEARCH) }
        binding.resultsContainer.addView(hero, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(8)
        })
        val recent = library.recent()
        if (recent.isNotEmpty()) {
            sectionTitle("↺  Back in rotation")
            recent.take(5).forEach { addVideoCard(it, recent) }
        }
        val favorites = library.favorites()
        sectionTitle("♥  On repeat")
        if (favorites.isEmpty()) {
            labelAction("Save your favorites here") { openPage(Page.SEARCH) }
        } else {
            favorites.take(5).forEach { addVideoCard(it, favorites) }
        }
        sectionTitle("Browse by mood")
        showMoodGrid()
        labelAction("Search for an exact song or artist") { openPage(Page.SEARCH) }
        sectionTitle("NightWave 2.0 · Design preview")
        labelAction("✦  Explore the new premium interface") {
            startActivity(Intent(this, com.nightwave.player.v2.ui.NightWaveStudioActivity::class.java))
        }
    }

    /** Library with real saved state and working actions, including a link-import recovery path. */
    private fun showLibrary() {
        val favorites = library.favorites()
        val names = library.playlistNames()
        val recent = library.recent()
        binding.resultsHeading.text = "Your collection."
        showStatus(favorites.size.toString() + " liked  ·  " + names.size +
            " playlists  ·  " + recent.size + " recently played")
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fun libraryTile(
            title: String,
            subtitle: String,
            glyph: String,
            colors: IntArray,
            tap: () -> Unit
        ): LinearLayout {
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(17), dp(16), dp(15), dp(15))
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR, colors
                ).apply { cornerRadius = dp(21).toFloat() }
                elevation = dp(4).toFloat()
                isClickable = true
                isFocusable = true
                setOnClickListener { tap() }
            }
            tile.addView(TextView(this).apply {
                text = glyph
                textSize = 40f
                setTextColor(Color.WHITE)
                gravity = Gravity.START
            }, LinearLayout.LayoutParams(-1, dp(60)))
            tile.addView(TextView(this).apply {
                text = title
                textSize = 19f
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                setTextColor(Color.WHITE)
                maxLines = 1
            })
            tile.addView(TextView(this).apply {
                text = subtitle
                textSize = 12f
                setTextColor(Color.rgb(218, 220, 245))
                setPadding(0, dp(6), 0, 0)
            })
            return tile
        }
        header.addView(libraryTile(
            "Liked Songs", favorites.size.toString() + " tracks", "♥",
            intArrayOf(0xFF78498F.toInt(), 0xFF35315D.toInt())
        ) { showCollection("Liked Songs", favorites) }, LinearLayout.LayoutParams(0, dp(166), 1f).apply {
            marginEnd = dp(10)
        })
        header.addView(libraryTile(
            "New playlist", "Create a collection", "＋",
            intArrayOf(0xFF196C81.toInt(), 0xFF293C68.toInt())
        ) { createPlaylistDialog() }, LinearLayout.LayoutParams(0, dp(166), 1f))
        binding.resultsContainer.addView(header, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
        labelAction("Paste a YouTube link to save a track") { addVideoLinkDialog() }
        labelAction("See your playing queue") { showQueueDialog() }
        sectionTitle("Your playlists")
        if (names.isEmpty()) {
            binding.resultsContainer.addView(TextView(this).apply {
                text = "Start a playlist for your late-night mixes, favorite artists, or study sessions."
                textSize = 14f
                setTextColor(Color.rgb(165, 174, 205))
                setPadding(dp(12), dp(10), dp(12), dp(20))
            })
        } else {
            names.forEach { name ->
                labelAction("▤  " + name + "   ·   " + library.playlist(name).size + " songs") {
                    showCollection(name, library.playlist(name), name)
                }
            }
            labelAction("Manage playlists") { managePlaylists() }
        }
        if (recent.isNotEmpty()) {
            sectionTitle("Recently on repeat")
            recent.take(12).forEach { addVideoCard(it, recent) }
        } else {
            sectionTitle("Start listening")
            labelAction("Explore music by mood") { openPage(Page.SEARCH) }
        }
    }

    private fun addVideoLinkDialog() {
        val input = EditText(this).apply {
            hint = "https://youtu.be/..."
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(22), dp(14), dp(22), dp(14))
        }
        AlertDialog.Builder(this).setTitle("Save a song from YouTube")
            .setMessage("Paste a public YouTube video URL to add it to your playlist, even if search is unavailable.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Find song") { _, _ ->
                val url = VideoLink.normalize(input.text.toString())
                if (url == null) {
                    showStatus("Please enter a valid public YouTube video link.", true)
                } else {
                    showStatus("Checking song details…")
                    worker.execute {
                        val lookup = runCatching { gateway.resolve(url).video }
                        main.post {
                            if (!active) return@post
                            lookup.fold(
                                onSuccess = { showPlaylistPicker(it) },
                                onFailure = {
                                    showStatus("Couldn't retrieve this video. Try opening its link in Search.", true)
                                }
                            )
                        }
                    }
                }
            }.show()
    }

    private fun managePlaylists() {
        val names = library.playlistNames()
        if (names.isEmpty()) return
        AlertDialog.Builder(this).setTitle("Manage playlists")
            .setItems(names.toTypedArray()) { _, selected ->
                val chosen = names[selected]
                AlertDialog.Builder(this).setTitle(chosen)
                    .setItems(arrayOf("Rename", "Delete")) { _, action ->
                        when (action) {
                            0 -> {
                                val input = EditText(this).apply {
                                    setText(chosen)
                                    setSingleLine(true)
                                }
                                AlertDialog.Builder(this).setTitle("Rename playlist").setView(input)
                                    .setNegativeButton("Cancel", null)
                                    .setPositiveButton("Save") { _, _ ->
                                        if (library.renamePlaylist(chosen, input.text.toString()))
                                            openPage(Page.LIBRARY)
                                        else showStatus("Playlist name is already used or invalid.", true)
                                    }.show()
                            }
                            1 -> AlertDialog.Builder(this)
                                .setTitle("Delete “" + chosen + "”?")
                                .setMessage("This removes the playlist, not your liked songs or listening history.")
                                .setNegativeButton("Keep", null)
                                .setPositiveButton("Delete") { _, _ ->
                                    library.deletePlaylist(chosen)
                                    openPage(Page.LIBRARY)
                                }.show()
                        }
                    }.show()
            }.show()
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

    /** Full-height listening screen; all controls use the same background MediaController. */
    private fun openNowPlayingDialog() {
        val video = currentVideo() ?: return
        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_root)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(25), dp(24), dp(25), dp(24))
            setBackgroundResource(R.drawable.bg_root)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "✦  NIGHTWAVE  /  NOW PLAYING"
            textSize = 12f
            letterSpacing = 0.1f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(181, 223, 232))
            layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f)
            gravity = Gravity.CENTER_VERTICAL
        })
        header.addView(TextView(this).apply {
            text = "×"
            textSize = 31f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            isClickable = true
            isFocusable = true
            contentDescription = "Close player"
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        container.addView(header)

        val artworkSize = resources.displayMetrics.widthPixels - dp(50)
        val art = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_nightwave)
            setBackgroundResource(R.drawable.bg_now_playing)
            clipToOutline = true
            elevation = dp(12).toFloat()
            layoutParams = LinearLayout.LayoutParams(-1, artworkSize).apply {
                topMargin = dp(24)
                bottomMargin = dp(28)
            }
        }
        thumbs.load(video.thumbnail, art)
        container.addView(art)
        container.addView(TextView(this).apply {
            text = video.title
            textSize = 27f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setTextColor(Color.WHITE)
            maxLines = 3
        })
        container.addView(TextView(this).apply {
            text = video.author
            textSize = 15f
            setTextColor(Color.rgb(178, 181, 217))
            setPadding(0, dp(8), 0, dp(18))
        })
        val fullProgress = SeekBar(this).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(187, 172, 255))
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.rgb(187, 172, 255))
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(69, 72, 111))
        }
        fullProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) = Unit
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) {
                controller?.let { player ->
                    val duration = player.duration
                    if (duration > 0 && duration != C.TIME_UNSET) {
                        player.seekTo(duration * fullProgress.progress / 1000L)
                    }
                }
            }
        })
        container.addView(fullProgress, LinearLayout.LayoutParams(-1, dp(40)))

        val time = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.rgb(169, 174, 206))
            gravity = Gravity.END
        }
        container.addView(time)
        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(19), 0, dp(16))
        }
        fun control(symbol: String, description: String, weight: Float, tap: () -> Unit): TextView {
            return TextView(this).apply {
                text = symbol
                textSize = if (description == "Play or pause") 28f else 25f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(241, 238, 255))
                isClickable = true
                isFocusable = true
                contentDescription = description
                layoutParams = LinearLayout.LayoutParams(0, dp(68), weight)
                setOnClickListener {
                    animate().scaleX(0.83f).scaleY(0.83f).setDuration(95).withEndAction {
                        animate().scaleX(1f).scaleY(1f).setDuration(135).start()
                    }.start()
                    tap()
                }
            }
        }
        controls.addView(control("⇄", "Toggle shuffle", 1f) {
            shuffle = !shuffle
            sendQueueAction(PlaybackService.ACTION_SHUFFLE)
            binding.shuffleButton.setTextColor(if (shuffle) Color.rgb(193, 175, 255) else Color.rgb(195, 194, 217))
        })
        controls.addView(control("|◀", "Previous song", 1f) {
            sendQueueAction(PlaybackService.ACTION_PREVIOUS)
        })
        val play = control(if (controller?.isPlaying == true) "Ⅱ" else "▶", "Play or pause", 1.35f) {
            controller?.let { if (it.isPlaying) it.pause() else it.play() }
        }.apply {
            setTextColor(Color.rgb(16, 19, 42))
            setBackgroundResource(R.drawable.bg_action)
        }
        controls.addView(play)
        controls.addView(control("▶|", "Next song", 1f) {
            sendQueueAction(PlaybackService.ACTION_NEXT)
        })
        controls.addView(control("♡", "Like this song", 1f) {
            library.toggleFavorite(video)
            updateFavoriteButton()
        })
        container.addView(controls)
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        actions.addView(control("＋  SAVE", "Add to a playlist", 1f) {
            showPlaylistPicker(video)
        })
        actions.addView(control("≡  UP NEXT", "View playing queue", 1f) {
            showQueueDialog()
        })
        container.addView(actions)
        val repeatAction = TextView(this).apply {
            text = "↻  REPEAT: " + listOf("OFF", "ALL", "ONE")[repeat]
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(193, 175, 255))
            setBackgroundResource(R.drawable.bg_chip_normal)
            isClickable = true
            isFocusable = true
            contentDescription = "Change repeat mode"
            setOnClickListener {
                repeat = (repeat + 1) % 3
                sendQueueAction(PlaybackService.ACTION_REPEAT)
                text = "↻  REPEAT: " + listOf("OFF", "ALL", "ONE")[repeat]
            }
            layoutParams = LinearLayout.LayoutParams(-1, dp(42)).apply {
                topMargin = dp(14)
                bottomMargin = dp(12)
            }
        }
        container.addView(repeatAction)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(container)
        }
        dialog.setContentView(scroll)
        val update = object : Runnable {
            override fun run() {
                if (!dialog.isShowing) return
                controller?.let { player ->
                    val length = player.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0
                    val elapsed = player.currentPosition.coerceAtLeast(0)
                    if (length > 0) fullProgress.progress =
                        ((elapsed.coerceAtMost(length) * 1000) / length).toInt()
                    time.text = TimeLabel.format(elapsed) + "  /  " + TimeLabel.format(length)
                    play.text = if (player.isPlaying) "Ⅱ" else "▶"
                }
                main.postDelayed(this, 800)
            }
        }
        dialog.setOnDismissListener { main.removeCallbacks(update) }
        dialog.show()
        dialog.window?.setLayout(-1, -1)
        art.alpha = 0.3f
        art.translationY = dp(28).toFloat()
        art.animate().alpha(1f).translationY(0f).setDuration(420).start()
        main.post(update)
    }

    private fun showStatus(message: String, isError: Boolean = false) {
        binding.statusText.text = message
        binding.statusText.setTextColor(if (isError) Color.rgb(255, 175, 187) else Color.rgb(153, 161, 197))
    }

    private fun errorText(error: Throwable): String = when (error) {
        is java.net.UnknownHostException -> "No internet connection. Connect and retry."
        is java.net.SocketTimeoutException -> "Connection timed out. Try again."
        else -> "Could not access this video: ${error.message?.take(140) ?: "Unsupported or temporarily blocked."}"
    }

    private fun dp(value: Int) = (resources.displayMetrics.density * value + 0.5f).toInt()
}
