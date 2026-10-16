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
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        binding.searchButton.setOnClickListener { searchOrPlay() }
        binding.homeTab.setOnClickListener { openPage(Page.HOME) }
        binding.searchTab.setOnClickListener { openPage(Page.SEARCH) }
        binding.libraryTab.setOnClickListener { openPage(Page.LIBRARY) }
        binding.prevTrackButton.setOnClickListener { sendQueueAction(PlaybackService.ACTION_PREVIOUS) }
        binding.nextTrackButton.setOnClickListener { sendQueueAction(PlaybackService.ACTION_NEXT) }
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
                if (!active || id != token) return@post
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

    /** Large YouTube-style thumbnail card. Tap to resolve and play with the existing service. */
    private fun addVideoCard(video: VideoPreview) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            contentDescription = "Play " + video.title + " by " + video.author
            setOnClickListener { playUrl(video.url) }
        }
        val thumbnailWidth = resources.displayMetrics.widthPixels - dp(28)
        val thumbnailHeight = (thumbnailWidth * 9f / 16f).toInt()
        val artwork = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, thumbnailHeight)
            background = GradientDrawable().apply {
                setColor(Color.rgb(34, 39, 42))
                cornerRadius = dp(13).toFloat()
            }
            clipToOutline = true
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_nightwave)
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        artwork.addView(image)
        thumbs.load(video.thumbnail, image)
        if (video.durationSeconds > 0) {
            val label = TextView(this).apply {
                text = TimeLabel.format(video.durationSeconds * 1_000)
                textSize = 12f
                setTextColor(Color.WHITE)
                setPadding(dp(7), dp(3), dp(7), dp(3))
                background = GradientDrawable().apply {
                    setColor(Color.argb(225, 0, 0, 0))
                    cornerRadius = dp(5).toFloat()
                }
                layoutParams = FrameLayout.LayoutParams(-2, -2, Gravity.END or Gravity.BOTTOM).apply {
                    setMargins(0, 0, dp(10), dp(10))
                }
            }
            artwork.addView(label)
        }
        card.addView(artwork)
        val meta = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(dp(3), dp(11), dp(3), dp(13))
        }
        meta.addView(TextView(this).apply {
            val letter = video.author.trim().firstOrNull()?.uppercaseChar() ?: '♫'
            text = letter.toString()
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(17, 23, 18))
            background = GradientDrawable().apply {
                setColor(Color.rgb(178, 243, 105))
                shape = GradientDrawable.OVAL
            }
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
        })
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        details.addView(TextView(this).apply {
            text = video.title
            setTextColor(Color.rgb(240, 245, 240))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        details.addView(TextView(this).apply {
            text = video.author + "  •  YouTube"
            setTextColor(Color.rgb(155, 168, 160))
            textSize = 12f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) }
        })
        meta.addView(details)
        card.addView(meta)
        binding.resultsContainer.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
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
