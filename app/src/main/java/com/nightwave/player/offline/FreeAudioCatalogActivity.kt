package com.nightwave.player.offline

import com.nightwave.player.v2.ui.NightWaveNativeMatte
import com.nightwave.player.v2.ui.NightWaveNativeMattePolicy


import android.app.AlertDialog
import android.content.Intent
import com.nightwave.player.R
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import com.nightwave.player.playback.PlaybackService
import java.util.concurrent.Executors

/**
 * Search -> choose -> save to Offline Library, entirely within NightWave.
 * This is an independent free-content catalog, NOT YouTube stream extraction.
 */
// This component intentionally uses the pinned Media3 unstable API.
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class FreeAudioCatalogActivity : NightWaveOfflineShellActivity() {
    private val nativeMattePalette get() = NightWaveNativeMatte.palette(this)

    override val offlineRoute = NightWaveOfflineRoute.CATALOG
    companion object {
        const val EXTRA_QUERY = "com.nightwave.player.FREE_AUDIO_QUERY"
        const val EXTRA_ARTIST = "com.nightwave.player.FREE_AUDIO_ARTIST"
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
    private lateinit var playSaved: TextView
    private var initialTitle: String = ""
    private var initialArtist: String = ""
    private var fromYouTubeSearch = false
    private var savedTitle: String = ""
    private var savedId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF0C131B.toInt()
        window.navigationBarColor = 0xFF0C131B.toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(nativeMattePalette.background.toInt()) }
            setPadding(dp(18), dp(14), dp(18), dp(16))
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
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search()
                    true
                } else false
            }
            setTextColor(nativeMattePalette.foreground.toInt())
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
        // Keep every supported offline source discoverable in one screen.
        // Neither import nor direct-file download silently changes a YouTube
        // song into a different recording.
        val offlineSources = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        offlineSources.addView(text("Import my audio", 13f, 0xFF9FE3DF.toInt(), true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                startActivity(Intent(this@FreeAudioCatalogActivity,
                    OfflineLibraryActivity::class.java))
            }
        }, LinearLayout.LayoutParams(0, dp(45), 1f))
        offlineSources.addView(text("Authorized audio link", 13f, 0xFF9FE3DF.toInt(), true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                startActivity(Intent(this@FreeAudioCatalogActivity,
                    PermittedAudioDownloadActivity::class.java))
            }
        }, LinearLayout.LayoutParams(0, dp(45), 1f))
        root.addView(offlineSources)

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
        root.addView(text("Downloads and queue  →",
            15f, 0xFF94E2D8.toInt(), true).apply {
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = "View persistent offline download progress"
            setOnClickListener { openDownloads() }
        }, LinearLayout.LayoutParams(-1, dp(48)))
        playSaved = text("▶  Play saved song", 15f, 0xFF091B20.toInt(), true).apply {
            visibility = View.GONE
            gravity = Gravity.CENTER
            background = rounded(0xFF94E2D8.toInt())
            contentDescription = "Play downloaded audio without internet"
            setOnClickListener {
                if (savedId.isNotBlank()) {
                    runCatching {
                        startService(Intent(this@FreeAudioCatalogActivity,
                            PlaybackService::class.java)
                            .setAction(PlaybackService.ACTION_PLAY_OFFLINE)
                            .putExtra(PlaybackService.EXTRA_OFFLINE_ID, savedId))
                    }.onFailure {
                        message.text = "Could not start playback. Try Offline Library."
                    }
                }
            }
        }
        root.addView(playSaved, LinearLayout.LayoutParams(-1, dp(52)).apply {
            bottomMargin = dp(10)
        })
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
        val frame = FrameLayout(this).apply {
            setBackgroundColor(NightWaveNativeMattePolicy.surface(0xFF0C131B, nativeMattePalette).toInt())
        }
        val contentWidth = if (resources.configuration.screenWidthDp > 780)
            dp(760) else -1
        frame.addView(root, FrameLayout.LayoutParams(
            contentWidth, -1, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        setOfflineContent(frame, root.getChildAt(0))
        val draft = NightWaveOfflineNavigationPolicy.catalogDraft(
            savedInstanceState?.getString("nw128-catalog-query"),
            savedInstanceState?.getString("nw128-catalog-origin-title"),
            savedInstanceState?.getString("nw128-catalog-origin-artist"),
            intent.getStringExtra(EXTRA_QUERY), intent.getStringExtra(EXTRA_ARTIST))
        val initial = draft.query
        initialTitle = draft.originTitle
        initialArtist = draft.originArtist
        fromYouTubeSearch = initialArtist.isNotBlank()
        if (initial.isNotBlank()) {
            input.setText(initial)
            input.setSelection(input.length())
            if (savedInstanceState == null) search()
            else message.text = getString(R.string.nw128_search_interrupted)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("nw128-catalog-query", NightWaveOfflineNavigationPolicy.query(input.text.toString()))
        outState.putString("nw128-catalog-origin-title", initialTitle)
        outState.putString("nw128-catalog-origin-artist", initialArtist)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        val previousIntent = this.intent
        super.onNewIntent(intent)
        if (!intent.hasExtra(EXTRA_QUERY)) return
        if (busyDownload) {
            setIntent(previousIntent)
            message.text = getString(R.string.nw128_transfer_busy)
            return
        }
        ++searchGeneration // Any pending response belongs to the old request.
        busySearch = false
        resetOfflineViewport()
        searchAction.alpha = 1f
        results.removeAllViews()
        openSaved.visibility = View.GONE
        playSaved.visibility = View.GONE
        savedId = ""
        initialTitle = NightWaveOfflineNavigationPolicy.query(intent.getStringExtra(EXTRA_QUERY))
        initialArtist = NightWaveOfflineNavigationPolicy.query(intent.getStringExtra(EXTRA_ARTIST))
        fromYouTubeSearch = initialArtist.isNotBlank()
        input.setText(initialTitle)
        search()
    }

    private fun search() {
        if (busySearch || busyDownload) return
        val term = FreeAudioCatalogPolicy.safeSearchTerm(input.text.toString())
        if (term.isBlank()) {
            message.text = "Enter a song title, artist or music style."
            return
        }
        busySearch = true
        (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(input.windowToken, 0)
        searchAction.alpha = 0.5f
        results.removeAllViews()
        message.text = "Checking openly licensed recordings…"
        val generation = ++searchGeneration
        val artist = if (term == FreeAudioCatalogPolicy.safeSearchTerm(initialTitle))
            initialArtist else ""
        val queries = OfflineDiscoveryPolicy.queries(term, artist)
        // The title-only query removes decorative video markers; no network
        // request or catalog eligibility rule is changed by this review hint.
        val requestTitle = queries.lastOrNull() ?: term
        worker.execute {
            // At most two metadata searches, only the second when the first
            // returns no eligible recordings. Never silently choose a
            // possible alternate recording for the user.
            val found = runCatching {
                var matches = emptyList<FreeAudioCandidate>()
                for (candidateQuery in queries) {
                    matches = catalog.search(candidateQuery)
                    if (matches.isNotEmpty()) break
                }
                matches
            }
            runOnUiThread {
                if (isFinishing || isDestroyed || generation != searchGeneration) return@runOnUiThread
                busySearch = false
                searchAction.alpha = 1f
                found.onSuccess { list ->
                    val reviewed = OfflineDiscoveryReviewPolicy.rank(requestTitle, list)
                    message.text = if (reviewed.isEmpty())
                        "No eligible offline recording found in this catalog. " +
                            "The YouTube track remains online-only; you can import audio you own."
                    else "${reviewed.size} eligible audio files, ordered by title words. " +
                            "These may be DIFFERENT recordings. Verify artist, title and license."
                    reviewed.forEach { showEntry(it, requestTitle) }
                }.onFailure {
                    message.text = "Open-audio search unavailable. Check internet and retry."
                }
            }
        }
    }

    private fun showEntry(entry: FreeAudioCandidate, requestTitle: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(nativeMattePalette.surface.toInt())
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), (nativeMattePalette.foreground.toInt() and 0x00FFFFFF) or 0x0D000000)
            }
            elevation = dp(2).toFloat()
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }
        card.addView(text(entry.title, 15f, Color.WHITE, true).apply {
            maxLines = 2
        })
        card.addView(text(
            OfflineDiscoveryReviewPolicy.reviewHint(requestTitle, entry.title),
            11f, 0xFFE2C99A.toInt()
        ).apply { setPadding(0, dp(4), 0, 0) })
        if (fromYouTubeSearch) {
            card.addView(text(
                "Independent recording; not verified as the same YouTube performance.",
                11f, 0xFFE2C99A.toInt()
            ).apply { setPadding(0, dp(5), 0, 0) })
        }
        card.addView(text(
            "Wikimedia Commons · ${entry.licenseName} · " +
                displaySize(entry.bytes),
            12f, 0xFFA8BFCC.toInt()
        ).apply { setPadding(0, dp(7), 0, dp(8)) })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(text("Save offline", 14f, 0xFF0C1B20.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = rounded(0xFF94E2D8.toInt())
            setOnClickListener { confirmDownload(entry) }
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        buttons.addView(text("License / source ↗", 12f, 0xFF9FE3DF.toInt(), true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                if (FreeAudioCatalogPolicy.safeDescriptionUrl(entry.descriptionUrl))
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.descriptionUrl)))
            }
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        card.addView(buttons)
        results.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(12)
        })
        card.alpha = 0f
        card.translationY = dp(7).toFloat()
        card.animate().alpha(1f).translationY(0f).setDuration(220).start()
    }

    private fun confirmDownload(entry: FreeAudioCandidate) {
        if (busyDownload || busySearch) return
        AlertDialog.Builder(this)
            .setTitle("Save this free audio?")
            .setMessage(
                "${entry.title}\n\nMarked ${entry.licenseName} on Wikimedia Commons. " +
                    "This may be a different recording from a YouTube search result. " +
                    "NightWave saves the original audio format, not a converted MP3."
            )
            .setPositiveButton("Queue in background") { _, _ -> queueDownload(entry) }
            .setNeutralButton("Download here") { _, _ -> beginDownload(entry) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openDownloads() {
        startActivity(Intent(this, CatalogDownloadsActivity::class.java))
    }

    private fun queueDownload(entry: FreeAudioCandidate) {
        if (busyDownload || busySearch) return
        message.text = "Submitting background download…"
        runCatching { CatalogDownloadQueue.enqueue(this, entry) }
            .onSuccess { operation ->
                operation.result.addListener({
                    if (!isFinishing && !isDestroyed) {
                        runCatching { operation.result.get() }
                            .onSuccess {
                                message.text = "Ready in Downloads: " + entry.title +
                                    ". Open the queue to follow progress or see existing work."
                            }
                            .onFailure {
                                message.text = "Could not queue this audio. Try Download here."
                            }
                    }
                }, androidx.core.content.ContextCompat.getMainExecutor(this))
            }
            .onFailure {
                message.text = "Could not queue this audio. Try Download here."
            }
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
        playSaved.visibility = View.GONE
        savedId = ""
        message.text = "Connecting to Wikimedia audio storage…"
        val job = PermittedAudioDownloader(this)
        downloading = job
        worker.execute {
            val finished = runCatching {
                job.download(source, entry.sha1Base36, entry.bytes) { received, total ->
                    runOnUiThread {
                        if (isFinishing || isDestroyed || downloading !== job) return@runOnUiThread
                        if (total != null && total > 0) {
                            progress.isIndeterminate = false
                            progress.progress = (received * 100L / total).toInt().coerceIn(0, 100)
                        } else progress.isIndeterminate = true
                        message.text = "Downloaded ${received / 1024} KB" +
                            (total?.let { " / ${it / 1024} KB" } ?: "")
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
                    savedId = it.id
                    openSaved.visibility = View.VISIBLE
                    playSaved.visibility = View.VISIBLE
                    message.text = "Saved offline: ${it.title}. Play it without internet in Offline Library."
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

    private fun displaySize(bytes: Long): String =
        if (bytes < 1_048_576L) "${(bytes + 1023L) / 1024L} KB"
        else String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1_048_576.0)

    private fun dp(value: Int) = (resources.displayMetrics.density * value + 0.5f).toInt()
    private fun rounded(color: Int) = GradientDrawable().apply {
        setColor(NightWaveNativeMattePolicy.surface(color.toLong(), nativeMattePalette).toInt())
        cornerRadius = dp(20).toFloat()
    }
    private fun text(label: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = label
            textSize = size
            setTextColor(NightWaveNativeMattePolicy.text(color.toLong(), nativeMattePalette).toInt())
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
}
