package com.nightwave.player.offline

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.concurrent.Executors

/**
 * Search -> choose -> save to Offline Library, entirely within NightWave.
 * This is an independent free-content catalog, NOT YouTube stream extraction.
 */
class FreeAudioCatalogActivity : ComponentActivity() {
    companion object {
        const val EXTRA_QUERY = "com.nightwave.player.FREE_AUDIO_QUERY"
    }

    private val worker = Executors.newFixedThreadPool(2)
    private val catalog = FreeAudioCatalogClient()
    private var downloading: PermittedAudioDownloader? = null
    private var busySearch = false
    private var busyDownload = false
    private var searchGeneration = 0
    private lateinit var input: EditText
    private lateinit var searchAction: TextView
    private lateinit var message: TextView
    private lateinit var results: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var cancelAction: TextView
    private lateinit var openSaved: TextView
    private var savedTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0C131B.toInt())
            setPadding(dp(20), dp(20), dp(20), dp(25))
        }
        root.addView(text("‹  NightWave", 18f, 0xFF98E5DB.toInt(), true).apply {
            setOnClickListener { finish() }
            setPadding(0, 0, 0, dp(15))
        })
        root.addView(text("Find music to save", 26f, Color.WHITE, true))
        root.addView(text(
            "Search an independent free-audio catalog. Only files marked CC0 or public domain " +
                "can be downloaded here. YouTube search results are not downloadable through this screen.",
            13f, 0xFFB5C4CE.toInt()
        ).apply { setPadding(0, dp(10), 0, dp(17)) })

        input = EditText(this).apply {
            hint = "Search music, instrument, artist…"
            textSize = 15f
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF7F9CAB.toInt())
            background = rounded(0xFF192932.toInt())
            setPadding(dp(13), dp(12), dp(13), dp(12))
        }
        root.addView(input, LinearLayout.LayoutParams(-1, dp(55)))
        searchAction = text("Search free audio", 15f, 0xFF0C1B20.toInt(), true).apply {
            background = rounded(0xFF94E2D8.toInt())
            gravity = Gravity.CENTER
            setOnClickListener { search() }
        }
        root.addView(searchAction, LinearLayout.LayoutParams(-1, dp(51)).apply {
            topMargin = dp(10)
        })

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            visibility = View.GONE
            max = 100
            isIndeterminate = true
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(15) })
        cancelAction = text("Cancel current download", 14f, 0xFFF1BDBD.toInt(), true).apply {
            visibility = View.GONE
            gravity = Gravity.CENTER
            setOnClickListener {
                downloading?.cancel()
                message.text = "Cancelling; the partial audio will not be saved."
            }
        }
        root.addView(cancelAction, LinearLayout.LayoutParams(-1, dp(46)))

        message = text("Search for openly licensed audio to download inside NightWave.",
            13f, 0xFFC1D1D9.toInt()).apply { setPadding(0, dp(12), 0, dp(14)) }
        root.addView(message)
        openSaved = text("Open saved song in Offline Library  →",
            14f, 0xFF94E2D8.toInt(), true).apply {
            visibility = View.GONE
            setPadding(0, 0, 0, dp(12))
            setOnClickListener {
                startActivity(Intent(this@FreeAudioCatalogActivity,
                    OfflineLibraryActivity::class.java)
                    .putExtra(OfflineLibraryActivity.EXTRA_SEARCH_QUERY, savedTitle))
            }
        }
        root.addView(openSaved)
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply {
            isFillViewport = false
            addView(results)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(text(
            "Download quality is the source file's original quality. Audio remains in " +
                "NightWave private storage and is removed if the app is uninstalled.",
            11f, 0xFF91A5B5.toInt()
        ).apply { setPadding(0, dp(9), 0, 0) })
        setContentView(root)
        val initial = intent.getStringExtra(EXTRA_QUERY).orEmpty().take(100)
        if (initial.isNotBlank()) {
            input.setText(initial)
            input.setSelection(input.length())
            search()
        }
    }

    private fun search() {
        if (busySearch || busyDownload) return
        val term = FreeAudioCatalogPolicy.safeSearchTerm(input.text.toString())
        if (term.isBlank()) {
            message.text = "Enter a song title, artist or music style."
            return
        }
        busySearch = true
        searchAction.alpha = 0.5f
        results.removeAllViews()
        message.text = "Searching CC0 and public-domain audio on Wikimedia Commons…"
        val generation = ++searchGeneration
        worker.execute {
            val found = runCatching { catalog.search(term) }
            runOnUiThread {
                if (isFinishing || isDestroyed || generation != searchGeneration) return@runOnUiThread
                busySearch = false
                searchAction.alpha = 1f
                found.onSuccess { list ->
                    message.text = if (list.isEmpty())
                        "No matching free-to-download audio found. This catalog does not cover all popular songs."
                    else "\${list.size} downloadable audio files. Check title and source before saving."
                    list.forEach { showEntry(it) }
                }.onFailure {
                    message.text = "Free-audio search unavailable. Check internet and retry."
                }
            }
        }
    }

    private fun showEntry(entry: FreeAudioCandidate) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(0xFF182630.toInt())
            setPadding(dp(13), dp(13), dp(13), dp(13))
        }
        card.addView(text(entry.title, 15f, Color.WHITE, true).apply {
            maxLines = 2
        })
        card.addView(text(
            "Wikimedia Commons · \${entry.licenseName} · " +
                "\${(entry.bytes / (1024 * 1024)).coerceAtLeast(1)} MB",
            12f, 0xFFA8BFCC.toInt()
        ).apply { setPadding(0, dp(7), 0, dp(8)) })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(text("Save offline", 14f, 0xFF0C1B20.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = rounded(0xFF94E2D8.toInt())
            setOnClickListener { confirmDownload(entry) }
        }, LinearLayout.LayoutParams(0, dp(43), 1f))
        buttons.addView(text("License / source ↗", 12f, 0xFF9FE3DF.toInt(), true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                if (FreeAudioCatalogPolicy.safeDescriptionUrl(entry.descriptionUrl))
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.descriptionUrl)))
            }
        }, LinearLayout.LayoutParams(0, dp(43), 1f))
        card.addView(buttons)
        results.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })
    }

    private fun confirmDownload(entry: FreeAudioCandidate) {
        if (busyDownload || busySearch) return
        AlertDialog.Builder(this)
            .setTitle("Save this free audio?")
            .setMessage(
                "\${entry.title}\n\nMarked \${entry.licenseName} on Wikimedia Commons. " +
                    "This may be a different recording from a YouTube search result. " +
                    "NightWave saves the original audio format, not a converted MP3."
            )
            .setPositiveButton("Download") { _, _ -> beginDownload(entry) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun beginDownload(entry: FreeAudioCandidate) {
        if (busyDownload) return
        if (!FreeAudioCatalogPolicy.permittedLicense(entry.licenseName) ||
            !FreeAudioCatalogPolicy.safeMediaUrl(entry.sourceUrl)) {
            message.text = "Source is not approved for this download."
            return
        }
        val source = runCatching {
            PermittedAudioDownloadPolicy.prepare(entry.sourceUrl, entry.saveName)
        }.getOrElse {
            message.text = "This file is not a supported direct audio download."
            return
        }
        busyDownload = true
        searchAction.alpha = 0.5f
        progress.visibility = View.VISIBLE
        progress.isIndeterminate = true
        cancelAction.visibility = View.VISIBLE
        openSaved.visibility = View.GONE
        message.text = "Connecting to Wikimedia audio storage…"
        val job = PermittedAudioDownloader(this)
        downloading = job
        worker.execute {
            val finished = runCatching {
                job.download(source) { received, total ->
                    runOnUiThread {
                        if (isFinishing || isDestroyed || downloading !== job) return@runOnUiThread
                        if (total != null && total > 0) {
                            progress.isIndeterminate = false
                            progress.progress = (received * 100L / total).toInt().coerceIn(0, 100)
                        } else progress.isIndeterminate = true
                        message.text = "Downloaded \${received / 1024} KB" +
                            (total?.let { " / \${it / 1024} KB" } ?: "")
                    }
                }
            }
            runOnUiThread {
                if (isFinishing || isDestroyed || downloading !== job) return@runOnUiThread
                downloading = null
                busyDownload = false
                searchAction.alpha = 1f
                progress.visibility = View.GONE
                cancelAction.visibility = View.GONE
                finished.onSuccess {
                    savedTitle = it.title
                    openSaved.visibility = View.VISIBLE
                    message.text = "Saved offline: \${it.title}. Play it without internet in Offline Library."
                }.onFailure {
                    message.text = "Download unsuccessful: " +
                        (it.message?.take(100) ?: "Try another free audio file.")
                }
            }
        }
    }

    override fun onDestroy() {
        ++searchGeneration
        downloading?.cancel()
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun dp(value: Int) = (resources.displayMetrics.density * value + 0.5f).toInt()
    private fun rounded(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(15).toFloat()
    }
    private fun text(label: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = label
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
}
