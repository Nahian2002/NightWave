package com.nightwave.player.offline

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.FrameLayout
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.nightwave.player.playback.PlaybackService
import com.nightwave.player.v2.ui.NightWaveAdaptiveChromePolicy
import com.nightwave.player.v2.locker.NightWaveLockerActivity
import com.nightwave.player.v2.lyrics.PrivateLyricsStore
import java.util.concurrent.Executors

/** Native offline UI: safe to open even if the new Compose preview crashes. */
class OfflineLibraryActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SEARCH_QUERY = "com.nightwave.player.offline.SEARCH_QUERY"
    }
    private lateinit var library: OfflineAudioLibrary
    private val lyrics by lazy { PrivateLyricsStore(applicationContext) }
    private lateinit var tracks: LinearLayout
    private lateinit var trackScroll: ScrollView
    private lateinit var state: TextView
    private lateinit var storageSummary: TextView
    private lateinit var resumeControl: TextView
    private lateinit var sortControl: TextView
    private lateinit var playAllControl: TextView
    private lateinit var availabilityFilters: LinearLayout
    private var availabilityFilter = OfflineLibraryViewPolicy.Filter.ALL
    private lateinit var pageLabel: TextView
    private lateinit var previousPage: TextView
    private lateinit var nextPage: TextView
    private val uiHandler = Handler(Looper.getMainLooper())
    private var pendingSearch: Runnable? = null
    private var page = 0
    private var searchText = ""
    private var sortMode = 0
    private var visibleSongs: List<OfflineSong> = emptyList()
    private var cachedReport: OfflineLibraryHealthReport? = null
    private var playableIds: Set<String> = emptySet()
    private val sortLabels = arrayOf("Recently imported", "Title A–Z", "Smallest first", "Largest first")
    private val worker = Executors.newSingleThreadExecutor()
    private val pick = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { selected ->
        if (selected.isEmpty()) return@registerForActivityResult
        status("Importing " + selected.size + " audio files…")
        worker.execute {
            var imported = 0
            val failures = mutableListOf<String>()
            selected.take(100).forEach { uri ->
                try {
                    library.import(uri)
                    imported++
                } catch (e: Exception) {
                    failures.add(e.message ?: "Unknown import error")
                }
            }
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    cachedReport = null
                    refresh()
                    status(imported.toString() + " imported" +
                        if (failures.isNotEmpty()) " · " + failures.size +
                            " failed: " + failures.first().take(100) else "")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        availabilityFilter = savedInstanceState?.getString("nw48-offline-filter")
            ?.let { stored -> runCatching {
                OfflineLibraryViewPolicy.Filter.valueOf(stored)
            }.getOrNull() } ?: OfflineLibraryViewPolicy.Filter.ALL
        library = OfflineAudioLibrary(this)
        searchText = intent.getStringExtra(EXTRA_SEARCH_QUERY).orEmpty().trim().take(160)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(17), dp(18), dp(10))
            setBackgroundColor(0xFF090D14.toInt())
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("‹", 30f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(46), dp(45)))
        header.addView(label("NightWave", 22f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header)
        root.addView(label("Offline music", 26f, Color.WHITE, true).apply {
            setPadding(0, dp(20), 0, dp(4))
        })
        root.addView(label("Your saved audio · Ready without internet", 14f, 0xFFABB8CC.toInt()).apply {
            setPadding(0, 0, 0, dp(7))
        })
        storageSummary = label("Checking offline storage…", 12f, 0xFF91E2D7.toInt()).apply {
            maxLines = 2
            setPadding(0, 0, 0, dp(10))
            contentDescription = "Offline music storage and device free space"
        }
        root.addView(storageSummary, LinearLayout.LayoutParams(-1, -2))
        val search = EditText(this).apply {
            hint = "Search saved songs"
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF8294A8.toInt())
            setPadding(dp(15), 0, dp(15), 0)
            background = cardBackground(0xFF172432.toInt())
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchText = s?.toString()?.trim().orEmpty()
                    page = 0
                    pendingSearch?.let(uiHandler::removeCallbacks)
                    val update = Runnable {
                        if (::tracks.isInitialized && !isFinishing) {
                            refresh()
                            if (::trackScroll.isInitialized) {
                                trackScroll.post { trackScroll.scrollTo(0, 0) }
                            }
                        }
                    }
                    pendingSearch = update
                    uiHandler.postDelayed(update, 220L)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        if (searchText.isNotBlank()) search.setText(searchText)
        root.addView(search, LinearLayout.LayoutParams(-1, dp(49)).apply {
            topMargin = dp(6); bottomMargin = dp(10)
        })
        fun quickAction(text: String, description: String, bright: Boolean,
            action: () -> Unit): TextView = label(text, 14f,
                if (bright) 0xFF101A21.toInt() else 0xFFB1EBE3.toInt(), true).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(5), 0, dp(5), 0)
            background = cardBackground(
                if (bright) 0xFF91E2D7.toInt() else 0xFF213840.toInt())
            contentDescription = description
            setOnClickListener { action() }
        }
        val quickActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        quickActions.addView(quickAction("Downloads", "Review download progress and history",
            false) {
            startActivity(Intent(this@OfflineLibraryActivity, CatalogDownloadsActivity::class.java))
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(5) })
        quickActions.addView(quickAction("Import files", "Import audio from your device", true) {
            pick.launch(arrayOf("audio/*", "application/ogg", "application/octet-stream"))
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(5) })
        root.addView(quickActions, LinearLayout.LayoutParams(-1, dp(48)).apply {
            bottomMargin = dp(4)
        })
        // Compact by default: otherwise fixed-height secondary controls can
        // push the entire offline song list below a short phone's viewport.
        val extraTools = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val toolsToggle = label("More sources & playback  ▾", 13f,
            0xFFB1EBE3.toInt(), true).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            background = cardBackground(0xFF1B303A.toInt())
            contentDescription = "Expand music sources and offline playback controls"
            setOnClickListener {
                val opening = extraTools.visibility != View.VISIBLE
                extraTools.visibility = if (opening) View.VISIBLE else View.GONE
                text = if (opening) "Hide extra tools  ▴" else "More sources & playback  ▾"
                contentDescription = if (opening)
                    "Collapse music sources and offline playback controls"
                else "Expand music sources and offline playback controls"
            }
        }
        root.addView(toolsToggle, LinearLayout.LayoutParams(-1, dp(48)).apply {
            bottomMargin = dp(4)
        })
        root.addView(extraTools)
        val otherSources = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        otherSources.addView(quickAction("Find audio", "Browse licensed audio sources", false) {
            startActivity(Intent(this@OfflineLibraryActivity, FreeAudioCatalogActivity::class.java))
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(5) })
        otherSources.addView(quickAction("Direct link", "Download your authorized audio URL", false) {
            startActivity(Intent(this@OfflineLibraryActivity,
                PermittedAudioDownloadActivity::class.java))
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(5) })
        extraTools.addView(otherSources, LinearLayout.LayoutParams(-1, dp(48)).apply {
            bottomMargin = dp(6)
        })
        extraTools.addView(label("☁  My Google Drive music  ›", 13f,
            0xFF91E2D7.toInt(), true).apply {
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = "Open your own Google Drive music"
            setPadding(dp(7), 0, 0, 0)
            setOnClickListener {
                startActivity(Intent(this@OfflineLibraryActivity, NightWaveLockerActivity::class.java))
            }
        }, LinearLayout.LayoutParams(-1, dp(44)))
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        sortControl = label("Sort: " + sortLabels[sortMode] + "  ▾", 12f, 0xFFB5C8D8.toInt()).apply {
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setOnClickListener {
                AlertDialog.Builder(this@OfflineLibraryActivity)
                    .setTitle("Sort offline music")
                    .setSingleChoiceItems(sortLabels, sortMode) { dialog, which ->
                        sortMode = which
                        page = 0
                        text = "Sort: " + sortLabels[sortMode] + "  ▾"
                        dialog.dismiss()
                        refresh()
                        trackScroll.post { trackScroll.scrollTo(0, 0) }
                    }.setNegativeButton("Cancel", null).show()
            }
        }
        controls.addView(sortControl, LinearLayout.LayoutParams(0, dp(49), 1f))
        playAllControl = label("▶  Play all", 13f, 0xFF91E2D7.toInt(), true).apply {
            gravity = Gravity.CENTER
            contentDescription = "Play all visible offline tracks"
            setOnClickListener {
                val first = visibleSongs.firstOrNull {
                    it.id in playableIds && library.fileFor(it) != null
                }
                if (first == null) status("No playable audio in the current view.")
                else launchPlayback(first)
            }
        }
        controls.addView(playAllControl, LinearLayout.LayoutParams(dp(94), dp(49)))
        val shuffleAllControl = label("⇄  Shuffle", 13f, 0xFF91E2D7.toInt(), true).apply {
            gravity = Gravity.CENTER
            contentDescription = "Shuffle and play the current offline song list"
            setOnClickListener {
                val first = visibleSongs.firstOrNull {
                    it.id in playableIds && library.fileFor(it) != null
                }
                if (first == null) status("No playable songs in this offline list.")
                else launchPlayback(first, shuffle = true)
            }
        }
        controls.addView(shuffleAllControl, LinearLayout.LayoutParams(dp(99), dp(49)))
        root.addView(controls)
        // Read-only file health filter: missing or damaged audio is still
        // indexed and accessible for manual recovery, never auto-cleaned.
        availabilityFilters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(availabilityFilters, LinearLayout.LayoutParams(-1, dp(52)).apply {
            topMargin = dp(5)
            bottomMargin = dp(4)
        })
        // Local playback shortcuts use the same Media3 service as the widget
        // and notification. They never invoke a network resolve.
        val mediaControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = cardBackground(0xFF142B31.toInt())
        }
        fun playbackShortcut(title: String, description: String, action: String) =
            label(title, 16f, 0xFFE0F7F3.toInt(), true).apply {
                gravity = Gravity.CENTER
                contentDescription = description
                setOnClickListener {
                    runCatching {
                        startService(Intent(this@OfflineLibraryActivity,
                            PlaybackService::class.java).setAction(action))
                    }.onFailure { status("Playback control failed. Select an offline song first.") }
                }
            }
        mediaControls.addView(playbackShortcut("⏮", "Previous offline song",
            PlaybackService.ACTION_PREVIOUS), LinearLayout.LayoutParams(0, dp(48), 1f))
        mediaControls.addView(playbackShortcut("⏯", "Toggle offline pause or play",
            PlaybackService.ACTION_TOGGLE_OFFLINE_PLAY_PAUSE),
            LinearLayout.LayoutParams(0, dp(48), 1f))
        mediaControls.addView(playbackShortcut("⏭", "Next offline song",
            PlaybackService.ACTION_NEXT), LinearLayout.LayoutParams(0, dp(48), 1f))
        extraTools.addView(mediaControls, LinearLayout.LayoutParams(-1, dp(48)).apply {
            bottomMargin = dp(7)
        })
        val paging = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        previousPage = label("‹  Previous", 14f, 0xFF91E2D7.toInt(), true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                if (page > 0) {
                    page--
                    refresh()
                    trackScroll.post { trackScroll.scrollTo(0, 0) }
                }
            }
        }
        paging.addView(previousPage, LinearLayout.LayoutParams(0, dp(49), 1f))
        pageLabel = label("Page 1 / 1", 12f, 0xFFB5C8D8.toInt()).apply {
            gravity = Gravity.CENTER
        }
        paging.addView(pageLabel, LinearLayout.LayoutParams(0, dp(49), 1f))
        nextPage = label("Next  ›", 14f, 0xFF91E2D7.toInt(), true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                if (page + 1 < OfflineLibraryCapacity.pageCount(visibleSongs.size)) {
                    page++
                    refresh()
                    trackScroll.post { trackScroll.scrollTo(0, 0) }
                }
            }
        }
        paging.addView(nextPage, LinearLayout.LayoutParams(0, dp(49), 1f))
        root.addView(paging)
        resumeControl = label("", 13f, 0xFF91E2D7.toInt(), true).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), 0, dp(15), 0)
            background = cardBackground(0xFF223A42.toInt())
            contentDescription = "Resume the previous offline listening session"
            setOnClickListener {
                val saved = OfflineSessionStore(this@OfflineLibraryActivity).read()
                if (saved == null || runCatching {
                    library.find(saved.selectedId)?.let(library::fileFor)
                }.getOrNull() == null) {
                    refresh()
                    status("The previous offline track is no longer available.")
                } else {
                    runCatching {
                        startService(Intent(this@OfflineLibraryActivity, PlaybackService::class.java)
                            .setAction(PlaybackService.ACTION_RESUME_OFFLINE))
                    }.onSuccess { status("Resuming offline music…") }
                        .onFailure { status("Couldn't resume local music. Try again.") }
                }
            }
        }
        root.addView(resumeControl, LinearLayout.LayoutParams(-1, dp(48)).apply {
            bottomMargin = dp(5)
        })
        state = label("", 12f, 0xFFABBCD0.toInt()).apply {
            setPadding(dp(2), dp(15), 0, dp(12))
        }
        root.addView(state)
        tracks = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        trackScroll = ScrollView(this).apply { addView(tracks) }
        root.addView(trackScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(label(
            "Private on-device storage · Deleted if NightWave is uninstalled",
            11f, 0xFF8294A8.toInt()
        ).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(45)))
        val frame = FrameLayout(this).apply {
            setBackgroundColor(0xFF090D14.toInt())
        }
        val contentWidth = if (resources.configuration.screenWidthDp >= 780) dp(760) else -1
        frame.addView(root, FrameLayout.LayoutParams(contentWidth, -1,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        setContentView(frame)
        NightWaveSafeInsets.bind(frame)
        // onResume performs the initial and every subsequent fresh disk health check.
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("nw48-offline-filter", availabilityFilter.name)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        cachedReport = null
        if (::tracks.isInitialized) refresh()
    }

    private fun refresh() {
        val report = cachedReport ?: library.healthReport().also { cachedReport = it }
        if (!report.indexReadable) {
            storageSummary.text = "Storage totals unavailable · offline index needs attention"
            tracks.removeAllViews()
            visibleSongs = emptyList()
            playableIds = emptySet()
            resumeControl.visibility = View.GONE
            playAllControl.alpha = 0.45f
            status("Offline index unreadable · No files or entries changed")
            tracks.addView(label(
                "Your saved index needs attention. NightWave has not deleted or repaired anything. " +
                    "Keep the app installed and preserve its data before attempting recovery.",
                14f, 0xFFFFC07D.toInt()
            ).apply { setPadding(dp(5), dp(25), dp(5), 0) })
            return
        }
        val all = report.entries.map { it.song }
        val availableEntries = report.entries.filter {
            it.health == OfflineFileHealth.AVAILABLE
        }
        val importedBytes = availableEntries.sumOf { it.song.bytes.coerceAtLeast(0L) }
        val freeBytes = runCatching { filesDir.usableSpace }.getOrDefault(-1L)
        storageSummary.text = OfflineStorageSummary.label(
            availableEntries.size, importedBytes, freeBytes, report.attentionCount)
        val healthById = report.entries.associate { it.song.id to it.health }
        playableIds = report.entries.asSequence()
            .filter { it.health == OfflineFileHealth.AVAILABLE }
            .map { it.song.id }.toSet()
        val healthFiltered = OfflineTrackSearch.match(searchText, all)
            .filter { song ->
                OfflineLibraryViewPolicy.includes(availabilityFilter,
                    healthById[song.id] ?: OfflineFileHealth.UNREADABLE)
            }
        visibleSongs = when (sortMode) {
            1 -> healthFiltered.sortedWith(compareBy<OfflineSong> { it.title.lowercase() }.thenBy { it.id })
            2 -> healthFiltered.sortedWith(compareBy<OfflineSong> { it.bytes }.thenBy { it.title.lowercase() })
            3 -> healthFiltered.sortedWith(compareByDescending<OfflineSong> { it.bytes }.thenBy { it.title.lowercase() })
            else -> if (searchText.isBlank()) healthFiltered.asReversed() else healthFiltered
        }
        availabilityFilters.removeAllViews()
        OfflineLibraryViewPolicy.Filter.entries.forEach { filter ->
            availabilityFilters.addView(label(OfflineLibraryViewPolicy.label(filter),
                12f, if (availabilityFilter == filter) 0xFF09131B.toInt()
                else 0xFFB6D1D8.toInt(), availabilityFilter == filter).apply {
                gravity = Gravity.CENTER
                contentDescription = "Show " + OfflineLibraryViewPolicy.label(filter) +
                    " offline songs"
                background = cardBackground(if (availabilityFilter == filter)
                    0xFF91E2D7.toInt() else 0xFF213840.toInt())
                setOnClickListener {
                    if (availabilityFilter != filter) {
                        availabilityFilter = filter
                        page = 0
                        refresh()
                        trackScroll.post { trackScroll.scrollTo(0, 0) }
                    }
                }
            }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            })
        }
        val checkpoint = OfflineSessionStore(this).read()
        val resumeSong = all.firstOrNull { it.id == checkpoint?.selectedId }
            ?.takeIf { it.id in playableIds && library.fileFor(it) != null }
        resumeControl.visibility = if (resumeSong == null) View.GONE else View.VISIBLE
        if (resumeSong != null) {
            val minute = (checkpoint?.positionMs ?: 0L) / 60_000
            resumeControl.text = "↻  Resume · " + resumeSong.title +
                (if (minute > 0) " · " + minute + "m" else "")
            resumeControl.maxLines = 1
            resumeControl.ellipsize = android.text.TextUtils.TruncateAt.END
        }
        status(visibleSongs.size.toString() + " of " + all.size +
            " tracks · " + report.playableCount + " available · " +
            (all.sumOf { it.bytes } / (1024 * 1024)) + " MB indexed" +
            if (report.attentionCount > 0)
                " · " + report.attentionCount + " need attention (nothing removed)" else "")
        playAllControl.alpha = if (visibleSongs.any { it.id in playableIds }) 1f else 0.45f
        page = OfflineLibraryCapacity.safePage(page, visibleSongs.size)
        val numberOfPages = OfflineLibraryCapacity.pageCount(visibleSongs.size)
        pageLabel.text = "Page " + (page + 1) + " / " + numberOfPages
        previousPage.isEnabled = page > 0
        nextPage.isEnabled = page + 1 < numberOfPages
        previousPage.alpha = if (previousPage.isEnabled) 1f else 0.35f
        nextPage.alpha = if (nextPage.isEnabled) 1f else 0.35f
        val pageSongs = visibleSongs.drop(page * OfflineLibraryCapacity.pageSize())
            .take(OfflineLibraryCapacity.pageSize())
        tracks.removeAllViews()
        if (visibleSongs.isEmpty()) tracks.addView(label(
            OfflineLibraryViewPolicy.emptyMessage(
                availabilityFilter, all.isNotEmpty(), searchText.isNotBlank()),
            14f, 0xFFABB8CC.toInt()
        ).apply { setPadding(dp(5), dp(25), 0, 0) })
        pageSongs.forEach { song ->
            val importedLyrics = lyrics.hasImportedLrc(song.id)
            val health = healthById[song.id] ?: OfflineFileHealth.UNREADABLE
            val available = health == OfflineFileHealth.AVAILABLE
            val availability = when (health) {
                OfflineFileHealth.AVAILABLE -> "Offline"
                OfflineFileHealth.MISSING -> "File missing · Entry retained"
                OfflineFileHealth.SIZE_CHANGED -> "File size changed · Entry retained"
                OfflineFileHealth.UNREADABLE -> "File unreadable · Entry retained"
                OfflineFileHealth.UNSAFE_METADATA -> "Unsafe metadata · Entry retained"
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(13), dp(5), dp(5), dp(5))
                background = cardBackground(0xFF172432.toInt())
            }
            val description = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            description.addView(label(song.title, 14f, Color.WHITE, true).apply {
                maxLines = if (resources.configuration.fontScale >= 1.3f) 2 else 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            description.addView(label((song.bytes / 1024 / 1024).toString() +
                " MB · " + availability +
                (if (importedLyrics) " · LRC lyrics" else ""),
                12f, if (available) 0xFF94A9BB.toInt() else 0xFFFFC07D.toInt()).apply {
                maxLines = 2
            })
            row.addView(description, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(label("▶", 21f, 0xFF91E2D7.toInt()).apply {
                gravity = Gravity.CENTER
                alpha = if (available) 1f else 0.35f
                contentDescription = "Play " + song.title
                setOnClickListener {
                    if (available) launchPlayback(song)
                    else status(availability + ". File was not removed.")
                }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            row.addView(label("☁", 20f, 0xFF91E2D7.toInt()).apply {
                gravity = Gravity.CENTER
                alpha = if (available) 1f else 0.35f
                contentDescription = "Upload " + song.title + " to your own Google Drive"
                setOnClickListener {
                    if (available && library.fileFor(song) != null) {
                        startActivity(Intent(this@OfflineLibraryActivity, NightWaveLockerActivity::class.java)
                            .putExtra(NightWaveLockerActivity.EXTRA_OFFLINE_ID, song.id))
                    } else {
                        status("Audio unavailable for upload. Entry preserved.")
                    }
                }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            row.addView(label("⋮", 24f, 0xFFB9C9D6.toInt()).apply {
                gravity = Gravity.CENTER
                contentDescription = "Remove offline track " + song.title
                setOnClickListener {
                    if (!available || library.fileFor(song) == null) {
                        AlertDialog.Builder(this@OfflineLibraryActivity)
                            .setTitle("Offline audio needs attention")
                            .setMessage(song.title + ": " + availability + ". " +
                                "The entry and file (if present) remain untouched. " +
                                "Re-import the original audio or seek manual recovery; " +
                                "NightWave will not delete an unverified copy.")
                            .setPositiveButton("OK", null).show()
                    } else {
                        AlertDialog.Builder(this@OfflineLibraryActivity)
                            .setTitle("Remove offline song?")
                            .setMessage("Delete " + song.title + " from NightWave offline storage?")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Remove") { _, _ ->
                                if (library.delete(song)) {
                                    cachedReport = null
                                    refresh()
                                }
                                else status("Unable to delete offline copy; entry kept if unverified")
                            }.show()
                    }
                }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            tracks.addView(row, LinearLayout.LayoutParams(-1,
                dp(NightWaveAdaptiveChromePolicy.nativeRowHeightDp(
                    resources.configuration.fontScale))).apply {
                bottomMargin = dp(8)
            })
        }
    }

    private fun launchPlayback(song: OfflineSong, shuffle: Boolean = false) {
        if (library.fileFor(song) == null) {
            status("Audio file unavailable: " + song.title)
            return
        }
        // Only app-private UUID IDs are passed; playback validates against its own
        // library index before opening any media file.
        val playable = visibleSongs.filter { it.id in playableIds }.map { it.id }
        val selectedOffset = playable.indexOf(song.id).coerceAtLeast(0)
        // Large libraries may exceed Media3's bounded immediate queue; always
        // include the tapped song, even when selected on the last library page.
        val ids = ArrayList(playable.drop(selectedOffset)
            .take(OfflinePlaybackQueue.MAX_ITEMS))
        runCatching {
            startService(Intent(this, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_PLAY_OFFLINE)
                .putExtra(PlaybackService.EXTRA_OFFLINE_ID, song.id)
                .putStringArrayListExtra(PlaybackService.EXTRA_OFFLINE_IDS, ids))
        }.onSuccess {
            // Keep the selected song first; shuffle affects upcoming items.
            runCatching {
                startService(Intent(this, PlaybackService::class.java)
                    .setAction(PlaybackService.ACTION_SHUFFLE)
                    .putExtra(PlaybackService.EXTRA_SHUFFLE_ENABLED, shuffle))
            }
            status(if (shuffle) "Shuffling saved songs · " + song.title
                else "Playing " + song.title + " · Next follows this list")
        }.onFailure { status("Offline playback could not start. Please retry.") }
    }

    private fun status(message: String) {
        if (::state.isInitialized) state.text = message
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

    private fun cardBackground(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(14).toFloat()
    }

    private fun dp(v: Int) = (resources.displayMetrics.density * v + .5f).toInt()
    override fun onDestroy() {
        pendingSearch?.let(uiHandler::removeCallbacks)
        worker.shutdown()
        super.onDestroy()
    }
}
