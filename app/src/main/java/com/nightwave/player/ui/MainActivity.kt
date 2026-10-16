package com.nightwave.player.ui

import android.Manifest
import android.app.Activity
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
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
import com.nightwave.player.core.VideoLink
import com.nightwave.player.databinding.ActivityMainBinding
import com.nightwave.player.net.PlayableAudio
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.net.YouTubeGateway
import com.nightwave.player.playback.PlaybackService
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private val gateway = YouTubeGateway()
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var thumbs: ThumbnailLoader
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var active = false
    private var seekInProgress = false
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
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        binding.searchButton.setOnClickListener { searchOrPlay() }
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
        val input = binding.searchBox.text.toString().trim()
        if (input.isBlank()) { showStatus("Type a search or paste a YouTube video link.", true); return }
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(binding.searchBox.windowToken, 0)
        binding.searchBox.clearFocus()
        val url = VideoLink.normalize(input)
        when {
            url != null -> playUrl(url)
            VideoLink.resemblesLink(input) -> showStatus("This is not a valid public YouTube video link.", true)
            else -> search(input)
        }
    }

    private fun search(query: String) {
        val id = ++token
        waitingToPlay = null
        binding.resultsContainer.removeAllViews()
        showStatus("Searching YouTube for “$query”…")
        worker.execute {
            val outcome = runCatching { gateway.search(query) }
            main.post {
                if (!active || id != token) return@post
                outcome.fold(
                    onSuccess = { videos ->
                        showStatus(if (videos.isEmpty()) "No videos found. Try another search." else "${videos.size} videos found • Tap one to listen")
                        videos.forEach { addVideoCard(it) }
                    },
                    onFailure = { showStatus(errorText(it), true) }
                )
            }
        }
    }

    private fun playUrl(url: String) {
        val id = ++token
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

    private fun addVideoCard(video: VideoPreview) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(11), dp(11), dp(11), dp(11))
            background = getDrawable(R.drawable.bg_card)
            isClickable = true
            isFocusable = true
            contentDescription = "Play ${video.title} by ${video.author}"
            setOnClickListener { playUrl(video.url) }
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_nightwave)
            layoutParams = LinearLayout.LayoutParams(dp(109), dp(76))
        }
        card.addView(image)
        thumbs.load(video.thumbnail, image)
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(3), 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        details.addView(TextView(this).apply {
            text = video.title
            setTextColor(Color.rgb(236, 243, 236))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        details.addView(TextView(this).apply {
            text = video.author
            setTextColor(Color.rgb(152, 167, 155))
            textSize = 12f
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) }
        })
        details.addView(TextView(this).apply {
            text = if (video.durationSeconds > 0) "${TimeLabel.format(video.durationSeconds * 1000)}  •  Tap to play" else "Tap to play"
            setTextColor(Color.rgb(178, 243, 105))
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) }
        })
        card.addView(details)
        binding.resultsContainer.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
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
