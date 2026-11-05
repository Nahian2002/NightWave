package com.nightwave.player.offline

import android.content.Intent
import com.nightwave.player.R
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
import java.util.concurrent.Executors

/**
 * Foreground screen for explicit, permissioned direct-audio downloads.
 * Closing the screen cancels the transfer. No silent background retrieval.
 */
class PermittedAudioDownloadActivity : NightWaveOfflineShellActivity() {
    override val offlineRoute = NightWaveOfflineRoute.LINK
    private val worker = Executors.newSingleThreadExecutor()
    private var transfer: PermittedAudioDownloader? = null
    private lateinit var urlInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var rights: CheckBox
    private lateinit var progress: ProgressBar
    private lateinit var message: TextView
    private lateinit var action: TextView
    private lateinit var queueAction: TextView
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
        panel.addView(label(getString(R.string.nw151_download_title), 29f, Color.WHITE, true).apply {
            setPadding(0, dp(8), 0, dp(8))
        })
        panel.addView(label(
            getString(R.string.nw151_audio_help),
            14f, 0xFFA7BBC7.toInt()
        ).apply { setPadding(0, 0, 0, dp(20)) })

        panel.addView(label(getString(R.string.nw151_find_audio), 15f, 0xFF10171C.toInt(), true).apply {
            gravity = Gravity.CENTER
            minimumHeight = dp(48)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = buttonBackground(0xFF98E5DB.toInt())
            setOnClickListener {
                startActivity(Intent(this@PermittedAudioDownloadActivity,
                    FreeAudioCatalogActivity::class.java))
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) })
        panel.addView(label(getString(R.string.nw151_url_label), 12f, 0xFF9ADBD4.toInt(), true))
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

        panel.addView(label(getString(R.string.nw151_name_label), 12f, 0xFF9ADBD4.toInt(), true))
        nameInput = EditText(this).apply {
            hint = getString(R.string.nw151_name_hint)
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
            text = getString(R.string.nw151_rights)
            textSize = 13f
            setTextColor(0xFFE2F1F1.toInt())
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFF99E5DB.toInt())
        }
        panel.addView(rights, margins(0, 0, 0, dp(18)))

        action = label(getString(R.string.nw151_download), 16f, 0xFF10171C.toInt(), true).apply {
            gravity = Gravity.CENTER
            minimumHeight = dp(48)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = buttonBackground(0xFF98E5DB.toInt())
            setOnClickListener { beginDownload() }
        }
        panel.addView(action, LinearLayout.LayoutParams(-1, -2))
        queueAction = label(getString(R.string.nw151_background), 15f, 0xFF98E5DB.toInt(), true).apply {
            gravity = Gravity.CENTER
            minimumHeight = dp(48)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener { queueBackgroundDownload() }
        }
        panel.addView(queueAction, LinearLayout.LayoutParams(-1, -2))
        panel.addView(label(getString(R.string.nw151_queued), 14f, 0xFF98E5DB.toInt(), true).apply {
            gravity = Gravity.CENTER
            minimumHeight = dp(48)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener {
                startActivity(Intent(this@PermittedAudioDownloadActivity,
                    CatalogDownloadsActivity::class.java))
            }
        }, LinearLayout.LayoutParams(-1, -2))
        cancelAction = label(getString(R.string.nw151_cancel), 14f, 0xFFE4F1F0.toInt(), true).apply {
            gravity = Gravity.CENTER
            minimumHeight = dp(48)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            visibility = android.view.View.GONE
            setOnClickListener {
                transfer?.cancel()
                message.text = getString(R.string.nw151_cancelling)
            }
        }
        panel.addView(cancelAction, LinearLayout.LayoutParams(-1, -2))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            visibility = android.view.View.GONE
            progressTintList = android.content.res.ColorStateList.valueOf(0xFF98E5DB.toInt())
        }
        panel.addView(progress, margins(0, dp(15), 0, dp(9)))
        message = label(getString(R.string.nw151_ready),
            13f, 0xFFB5C8D4.toInt())
        panel.addView(message, margins(0, dp(10), 0, dp(16)))
        panel.addView(label(
            getString(R.string.nw151_boundaries),
            12f, 0xFF91A5B5.toInt()
        ))
        panel.addView(label(getString(R.string.nw151_back), 14f, 0xFF98E5DB.toInt(), true).apply {
            gravity = Gravity.CENTER
            minimumHeight = dp(48)
            setPadding(dp(12), dp(10), dp(12), dp(10))
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
        setOfflineContent(frame, panel.getChildAt(0))
        if (savedInstanceState != null) {
            // Draft text survives recreation; permission must be confirmed
            // afresh and a foreground transfer is never silently restarted.
            urlInput.setText(savedInstanceState.getString("nw128-url-draft").orEmpty().take(4096))
            nameInput.setText(NightWaveOfflineNavigationPolicy.query(savedInstanceState.getString("nw128-name-draft")))
            rights.isChecked = false
            if (savedInstanceState.getBoolean("nw128-foreground-active")) {
                message.text = getString(R.string.nw128_transfer_interrupted)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("nw128-url-draft", urlInput.text.toString().take(4096))
        outState.putString("nw128-name-draft", NightWaveOfflineNavigationPolicy.query(nameInput.text.toString()))
        outState.putBoolean("nw128-foreground-active", active)
        super.onSaveInstanceState(outState)
    }

    private fun queueBackgroundDownload() {
        if (active) return
        if (!rights.isChecked) {
            message.text = getString(R.string.nw151_confirm_own)
            return
        }
        val source = try {
            PermittedAudioDownloadPolicy.prepare(
                urlInput.text.toString(), nameInput.text.toString())
        } catch (error: IllegalArgumentException) {
            message.text = error.message ?: "Enter a permitted direct HTTPS audio file."
            return
        }
        message.text = "Submitting offline download to Android's background queue..."
        runCatching { DirectAudioDownloadQueue.enqueue(this, source) }
            .onSuccess { operation ->
                operation.result.addListener({
                    if (isFinishing || isDestroyed) return@addListener
                    runCatching { operation.result.get() }
                        .onSuccess {
                            message.text = "Audio download queued. Follow the NightWave notification; " +
                                "completed files appear in Offline Library."
                        }
                        .onFailure {
                            message.text = getString(R.string.nw151_queue_failed)
                        }
                }, androidx.core.content.ContextCompat.getMainExecutor(this))
            }
            .onFailure {
                message.text = "Could not queue the background download. " +
                    (it.message?.take(85) ?: "Check the link.")
            }
    }

    private fun beginDownload() {
        if (active) return
        if (!rights.isChecked) {
            message.text = getString(R.string.nw151_confirm_save)
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
        message.text = getString(R.string.nw151_connecting)
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
