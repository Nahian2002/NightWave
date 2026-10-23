package com.nightwave.player.offline

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.concurrent.Executors

/**
 * Foreground screen for explicit, permissioned direct-audio downloads.
 * Closing the screen cancels the transfer. No silent background retrieval.
 */
class PermittedAudioDownloadActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var transfer: PermittedAudioDownloader? = null
    private lateinit var urlInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var rights: CheckBox
    private lateinit var progress: ProgressBar
    private lateinit var message: TextView
    private lateinit var action: TextView
    private lateinit var cancelAction: TextView
    private var active = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(30))
            setBackgroundColor(0xFF101A20.toInt())
        }
        panel.addView(label("NightWave", 13f, 0xFF98E7DC.toInt(), true))
        panel.addView(label("Download audio", 29f, Color.WHITE, true).apply {
            setPadding(0, dp(8), 0, dp(8))
        })
        panel.addView(label(
            "Save a permitted direct MP3, M4A, AAC, FLAC, WAV, OGG, OPUS or WebM " +
                "audio file for offline listening. A direct HTTPS link is required.",
            14f, 0xFFA7BBC7.toInt()
        ).apply { setPadding(0, 0, 0, dp(20)) })

        panel.addView(label("Search free music in NightWave →", 15f, 0xFF10171C.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = buttonBackground(0xFF98E5DB.toInt())
            setOnClickListener {
                startActivity(Intent(this@PermittedAudioDownloadActivity,
                    FreeAudioCatalogActivity::class.java))
            }
        }, LinearLayout.LayoutParams(-1, dp(49)).apply { bottomMargin = dp(20) })
        panel.addView(label("DIRECT HTTPS AUDIO FILE URL", 12f, 0xFF9ADBD4.toInt(), true))
        urlInput = EditText(this).apply {
            hint = "https://artist.example/music/track.mp3 or /download?id=123"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(false)
            minLines = 1
            maxLines = 3
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF7B909F.toInt())
            background = fieldBackground()
            setPadding(dp(15), dp(11), dp(15), dp(11))
        }
        panel.addView(urlInput, margins(0, dp(8), 0, dp(19)))

        panel.addView(label("TRACK NAME (OPTIONAL)", 12f, 0xFF9ADBD4.toInt(), true))
        nameInput = EditText(this).apply {
            hint = "Track name (extension optional)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            isSingleLine = true
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF7B909F.toInt())
            background = fieldBackground()
            setPadding(dp(15), dp(11), dp(15), dp(11))
        }
        panel.addView(nameInput, margins(0, dp(8), 0, dp(18)))

        rights = CheckBox(this).apply {
            text = "I own this audio or have permission to download and save it."
            textSize = 13f
            setTextColor(0xFFE2F1F1.toInt())
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFF99E5DB.toInt())
        }
        panel.addView(rights, margins(0, 0, 0, dp(18)))

        action = label("Download to Offline Library", 16f, 0xFF10171C.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = buttonBackground(0xFF98E5DB.toInt())
            setOnClickListener { beginDownload() }
        }
        panel.addView(action, LinearLayout.LayoutParams(-1, dp(55)))
        cancelAction = label("Cancel download", 14f, 0xFFE4F1F0.toInt(), true).apply {
            gravity = Gravity.CENTER
            visibility = android.view.View.GONE
            setOnClickListener {
                transfer?.cancel()
                message.text = "Cancelling transfer and removing its temporary file..."
            }
        }
        panel.addView(cancelAction, LinearLayout.LayoutParams(-1, dp(51)))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            visibility = android.view.View.GONE
            progressTintList = android.content.res.ColorStateList.valueOf(0xFF98E5DB.toInt())
        }
        panel.addView(progress, margins(0, dp(15), 0, dp(9)))
        message = label("Ready. Downloads remain in NightWave's private Offline Library.",
            13f, 0xFFB5C8D4.toInt())
        panel.addView(message, margins(0, dp(10), 0, dp(16)))
        panel.addView(label(
            "YouTube watch links, streaming manifests and protected media are not direct " +
                "audio downloads. NightWave does not convert YouTube videos to MP3. " +
                "This direct-link transfer stops when you leave this screen. " +
                "Original audio format is preserved. " +
                "Temporary network errors are retried, but unsupported audio is never saved. " +
                "Audio is deleted if NightWave is uninstalled.",
            12f, 0xFF91A5B5.toInt()
        ))
        panel.addView(label("Back to Offline Library", 14f, 0xFF98E5DB.toInt(), true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, dp(8))
            setOnClickListener { finish() }
        })
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFF101A20.toInt())
            isFillViewport = true
            addView(panel)
        }
        val frame = FrameLayout(this).apply {
            setBackgroundColor(0xFF101A20.toInt())
        }
        val contentWidth = if (resources.configuration.screenWidthDp >= 780)
            dp(760) else -1
        frame.addView(scroll, FrameLayout.LayoutParams(contentWidth, -1,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        setContentView(frame)
        NightWaveSafeInsets.bind(frame)
    }

    private fun beginDownload() {
        if (active) return
        if (!rights.isChecked) {
            message.text = "Confirm that you have permission to save the audio."
            return
        }
        val spec = try {
            PermittedAudioDownloadPolicy.prepare(
                urlInput.text.toString(), nameInput.text.toString())
        } catch (error: IllegalArgumentException) {
            message.text = error.message ?: "Enter a valid direct audio file link."
            return
        }
        active = true
        urlInput.isEnabled = false
        nameInput.isEnabled = false
        rights.isEnabled = false
        action.alpha = 0.45f
        progress.visibility = android.view.View.VISIBLE
        progress.isIndeterminate = true
        cancelAction.visibility = android.view.View.VISIBLE
        message.text = "Connecting securely to the audio server..."
        val job = PermittedAudioDownloader(this)
        transfer = job
        worker.execute {
            val result = runCatching {
                job.download(spec) { received, total ->
                    runOnUiThread {
                        if (isFinishing || isDestroyed || transfer !== job) return@runOnUiThread
                        if (total != null && total > 0L) {
                            progress.isIndeterminate = false
                            progress.progress = (received * 100L / total).toInt().coerceIn(0, 100)
                        } else progress.isIndeterminate = true
                        message.text = "${received / (1024 * 1024)} MB received" +
                            (total?.let { " of ${it / (1024 * 1024)} MB" } ?: "")
                    }
                }
            }
            runOnUiThread {
                if (isFinishing || isDestroyed || transfer !== job) return@runOnUiThread
                active = false
                transfer = null
                urlInput.isEnabled = true
                nameInput.isEnabled = true
                rights.isEnabled = true
                action.alpha = 1f
                cancelAction.visibility = android.view.View.GONE
                progress.visibility = android.view.View.GONE
                message.text = result.fold(
                    onSuccess = { "Offline audio ready: '${it.title}'. Existing matching files are reused." },
                    onFailure = { "Download failed: ${it.message?.take(135) ?: "Please retry."}" }
                )
            }
        }
    }

    override fun onDestroy() {
        transfer?.cancel()
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (resources.displayMetrics.density * value + 0.5f).toInt()
    private fun margins(left: Int, top: Int, right: Int, bottom: Int) =
        LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(left, top, right, bottom)
        }
    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
    private fun fieldBackground() = GradientDrawable().apply {
        setColor(0xFF1C2930.toInt())
        cornerRadius = dp(14).toFloat()
        setStroke(dp(1), 0xFF384C54.toInt())
    }
    private fun buttonBackground(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(19).toFloat()
    }
}
