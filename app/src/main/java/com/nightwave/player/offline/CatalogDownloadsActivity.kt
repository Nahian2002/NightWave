package com.nightwave.player.offline

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ProgressBar
import android.content.res.ColorStateList
import androidx.activity.ComponentActivity
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nightwave.player.net.VideoPreview

/** WorkManager queue remains accessible even if the Compose player cannot open. */
class CatalogDownloadsActivity : ComponentActivity() {
    private lateinit var entries: LinearLayout
    private lateinit var overview: TextView
    private lateinit var historyToggle: TextView
    private lateinit var filterRow: LinearLayout
    private var currentFilter = DownloadCenterViewPolicy.Filter.ALL
    private var showHiddenJobs = false
    // Only one 60-row page of WorkManager history is inflated at a time.
    // Filtering and retry deduplication happen before paging.
    private var historyPage = 0
    private var resetHistoryScroll = false
    private lateinit var scroll: ScrollView
    private val manager by lazy { WorkManager.getInstance(applicationContext) }
    private var catalogJobs: List<WorkInfo> = emptyList()
    private var directJobs: List<WorkInfo> = emptyList()
    private var searchedJobs: List<WorkInfo> = emptyList()
    private val progressSamples = HashMap<UUID, Pair<Long, Long>>()
    private val observedSpeed = HashMap<UUID, Long>()
    private val uiHandler = Handler(Looper.getMainLooper())
    // WorkManager can notify separately for each source/progress tick. Keep
    // only one pending UI paint and always read the newest lists when it runs.
    private var refreshScheduled = false
    private var lastPaintedAtMs = -1L
    private var displayedFilter: DownloadCenterViewPolicy.Filter? = null
    private val coalescedRefresh = Runnable {
        refreshScheduled = false
        if (!isFinishing && !isDestroyed && ::entries.isInitialized) refreshJobs()
    }
    // One slow storage scan should not block the UI or queue unlimited work.
    // Retain only the newest waiting health check; superseded generations
    // can never replace current WorkManager state or claim a deleted file Ready.
    private val fileVerifier = ThreadPoolExecutor(
        1, 1, 15L, TimeUnit.SECONDS,
        ArrayBlockingQueue<Runnable>(1),
        ThreadPoolExecutor.DiscardOldestPolicy()
    )
    private var requestedSavedIds: Set<String>? = null
    private var verifiedAvailableIds: Set<String> = emptySet()
    private var verificationGeneration = 0
    private var verifyingFiles = false
    private var fileCheckFailed = false

    private fun savedIds(infos: List<WorkInfo>): Set<String> =
        infos.asSequence()
            .filter { it.state == WorkInfo.State.SUCCEEDED }
            .mapNotNull { job ->
                when {
                    SearchTrackSaveQueue.TAG in job.tags ->
                        job.outputData.getString(SearchTrackSaveQueue.KEY_ID)
                    DirectAudioDownloadQueue.TAG in job.tags ->
                        job.outputData.getString(DirectAudioDownloadQueue.KEY_DONE)
                    else -> job.outputData.getString(CatalogDownloadQueue.KEY_DONE)
                }
            }.filter(String::isNotBlank).toSet()

    private fun requestFileVerification(infos: List<WorkInfo>) {
        val ids = savedIds(infos)
        if (!DownloadFileVerificationPolicy.needsRead(ids, requestedSavedIds)) return
        requestedSavedIds = ids
        verifiedAvailableIds = emptySet()
        fileCheckFailed = false
        verifyingFiles = ids.isNotEmpty()
        val ticket = ++verificationGeneration
        if (ids.isEmpty()) return
        fileVerifier.execute {
            val answer = runCatching {
                OfflineAudioLibrary(applicationContext).availableIds(ids)
            }
            uiHandler.post {
                if (!DownloadFileVerificationPolicy.canPublish(
                        ticket, verificationGeneration, isFinishing, isDestroyed))
                    return@post
                verifyingFiles = false
                fileCheckFailed = answer.isFailure
                verifiedAvailableIds = answer.getOrDefault(emptySet())
                render(catalogJobs + directJobs + searchedJobs)
            }
        }
    }
    private val stagnantTransferRefresh = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                if ((catalogJobs + directJobs + searchedJobs).any {
                        it.state == WorkInfo.State.RUNNING
                    } && ::entries.isInitialized) requestObserverRefresh()
                uiHandler.postDelayed(this, 5_000L)
            }
        }
    }
    private fun requestObserverRefresh() {
        if (isFinishing || isDestroyed || !::entries.isInitialized ||
            refreshScheduled) return
        refreshScheduled = true
        val delay = DownloadRefreshPolicy.delayMs(
            lastPaintedAtMs, SystemClock.elapsedRealtime())
        if (!uiHandler.postDelayed(coalescedRefresh, delay)) {
            refreshScheduled = false
        }
    }

    private fun refreshJobs() {
        // User actions and resume must be immediate; a delayed observer tick
        // must never replay outdated list data after this explicit refresh.
        uiHandler.removeCallbacks(coalescedRefresh)
        refreshScheduled = false
        val latest = catalogJobs + directJobs + searchedJobs
        requestFileVerification(latest)
        render(latest)
    }

    override fun onResume() {
        super.onResume()
        // A user may delete saved audio in Offline Library and return here
        // without any WorkManager status event. Recheck real file availability.
        // Re-entering from Offline Library may reveal a deleted private file.
        // Recheck on background IO even when WorkManager records are unchanged.
        requestedSavedIds = null
        lastPaintedAtMs = -1L
        if (::entries.isInitialized && ::overview.isInitialized) refreshJobs()
        uiHandler.removeCallbacks(stagnantTransferRefresh)
        uiHandler.postDelayed(stagnantTransferRefresh, 5_000L)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("nw48-download-filter", currentFilter.name)
        outState.putInt("nw63-download-page", historyPage)
        outState.putBoolean("nw63-show-hidden", showHiddenJobs)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        uiHandler.removeCallbacks(stagnantTransferRefresh)
        uiHandler.removeCallbacks(coalescedRefresh)
        refreshScheduled = false
        lastPaintedAtMs = -1L
        progressSamples.clear()
        observedSpeed.clear()
        verificationGeneration++
        requestedSavedIds = null
        super.onPause()
    }

    override fun onDestroy() {
        verificationGeneration++
        uiHandler.removeCallbacks(stagnantTransferRefresh)
        uiHandler.removeCallbacks(coalescedRefresh)
        refreshScheduled = false
        fileVerifier.shutdownNow()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentFilter = savedInstanceState?.getString("nw48-download-filter")
            ?.let { stored -> runCatching {
                DownloadCenterViewPolicy.Filter.valueOf(stored)
            }.getOrNull() } ?: DownloadCenterViewPolicy.Filter.ALL
        historyPage = savedInstanceState?.getInt("nw63-download-page", 0) ?: 0
        showHiddenJobs = savedInstanceState?.getBoolean("nw63-show-hidden", false) ?: false
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
            setBackgroundColor(0xFF0C131B.toInt())
        }
        root.addView(label("‹  NightWave", 18f, 0xFF98E5DB.toInt(), true).apply {
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = "Back to NightWave"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(label("Downloads", 24f, Color.WHITE, true))
        root.addView(label("Save music for offline listening",
            13f, 0xFFB7CAD4.toInt()).apply {
            setPadding(0, dp(3), 0, dp(10))
        })
        // Two equally prominent destinations instead of stacked full-width buttons.
        val destinations = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        destinations.addView(label("Discover audio", 14f, 0xFF0C1B20.toInt(), true).apply {
            background = rounded(0xFF94E2D8.toInt())
            gravity = Gravity.CENTER
            contentDescription = "Discover authorized downloadable music"
            setOnClickListener {
                startActivity(Intent(this@CatalogDownloadsActivity,
                    FreeAudioCatalogActivity::class.java))
            }
        }, LinearLayout.LayoutParams(0, dp(49), 1f).apply {
            rightMargin = dp(5)
        })
        destinations.addView(label("Offline library", 14f, 0xFF9CE9DF.toInt(), true).apply {
            background = rounded(0xFF1B313A.toInt())
            gravity = Gravity.CENTER
            contentDescription = "Open saved offline audio library"
            setOnClickListener {
                startActivity(Intent(this@CatalogDownloadsActivity,
                    OfflineLibraryActivity::class.java))
            }
        }, LinearLayout.LayoutParams(0, dp(49), 1f).apply {
            leftMargin = dp(5)
        })
        root.addView(destinations)
        overview = label("Loading your downloads…", 13f, 0xFFB7CAD4.toInt()).apply {
            setPadding(0, dp(13), 0, 0)
        }
        root.addView(overview, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(4)
        })
        historyToggle = label("", 13f, 0xFF91E2D7.toInt(), true).apply {
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener {
                showHiddenJobs = !showHiddenJobs
                historyPage = 0
                resetHistoryScroll = true
                refreshJobs()
            }
        }
        root.addView(historyToggle, LinearLayout.LayoutParams(-1, dp(48)))
        filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(filterRow, LinearLayout.LayoutParams(-1, dp(51)).apply {
            bottomMargin = dp(9)
        })
        entries = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll = ScrollView(this).apply { addView(entries) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val frame = FrameLayout(this).apply {
            setBackgroundColor(0xFF0C131B.toInt())
        }
        val contentWidth = if (resources.configuration.screenWidthDp >= 780) dp(760) else -1
        frame.addView(root, FrameLayout.LayoutParams(contentWidth, -1,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        setContentView(frame)
        NightWaveSafeInsets.bind(frame)
        manager.getWorkInfosByTagLiveData(CatalogDownloadQueue.TAG)
            .observe(this) { updates ->
                catalogJobs = updates.orEmpty()
                requestObserverRefresh()
            }
        manager.getWorkInfosByTagLiveData(DirectAudioDownloadQueue.TAG)
            .observe(this) { updates ->
                directJobs = updates.orEmpty()
                requestObserverRefresh()
            }
        manager.getWorkInfosByTagLiveData(SearchTrackSaveQueue.TAG)
            .observe(this) { updates ->
                searchedJobs = updates.orEmpty()
                requestObserverRefresh()
            }
    }

    private fun savedIdFromOutput(info: WorkInfo): String? = when {
        SearchTrackSaveQueue.TAG in info.tags ->
            info.outputData.getString(SearchTrackSaveQueue.KEY_ID)
        DirectAudioDownloadQueue.TAG in info.tags ->
            info.outputData.getString(DirectAudioDownloadQueue.KEY_DONE)
        else -> info.outputData.getString(CatalogDownloadQueue.KEY_DONE)
    }

    private fun render(infos: List<WorkInfo>) {
        if (isFinishing || isDestroyed) return
        lastPaintedAtMs = SystemClock.elapsedRealtime()
        val originalPosition = if (resetHistoryScroll) 0
            else if (::scroll.isInitialized) scroll.scrollY else 0
        resetHistoryScroll = false
        entries.removeAllViews()
        val hidden = DownloadHistoryVisibility.hiddenIds(this)
        val displayInfos = if (showHiddenJobs) infos else infos.filterNot {
            it.id.toString() in hidden
        }
        historyToggle.text = if (showHiddenJobs)
            "Hide dismissed jobs · ${hidden.size} hidden  ▴"
        else "Show dismissed jobs · ${hidden.size} hidden  ▾"
        // NW62: no private library index read or file stat on the UI thread.
        // Worker snapshots are generation-fenced; never mark a pending or
        // failed verification as Ready merely because WorkManager succeeded.
        val availableIds = verifiedAvailableIds
        fun isVerified(info: WorkInfo): Boolean {
            val saved = when {
                SearchTrackSaveQueue.TAG in info.tags ->
                    info.outputData.getString(SearchTrackSaveQueue.KEY_ID)
                DirectAudioDownloadQueue.TAG in info.tags ->
                    info.outputData.getString(DirectAudioDownloadQueue.KEY_DONE)
                else -> info.outputData.getString(CatalogDownloadQueue.KEY_DONE)
            }
            return DownloadFileVerificationPolicy.ready(
                info.state == WorkInfo.State.SUCCEEDED, saved, availableIds,
                verifyingFiles, fileCheckFailed)
        }
        // Static tabs are rebuilt only when selection changes, not on every
        // byte-progress callback. Download cards remain state authoritative.
        if (displayedFilter != currentFilter) {
            filterRow.removeAllViews()
            DownloadCenterViewPolicy.Filter.entries.forEach { filter ->
            filterRow.addView(label(DownloadCenterViewPolicy.label(filter), 13f,
                if (currentFilter == filter) 0xFF0C1B20.toInt()
                else 0xFFB8D0DA.toInt(), currentFilter == filter).apply {
                gravity = Gravity.CENTER
                background = rounded(if (currentFilter == filter)
                    0xFF94E2D8.toInt() else 0xFF1B313A.toInt())
                contentDescription = "Show " + DownloadCenterViewPolicy.label(filter) +
                    " downloads"
                setOnClickListener {
                    currentFilter = filter
                    historyPage = 0
                    resetHistoryScroll = true
                    refreshJobs()
                }
            }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            })
            }
            displayedFilter = currentFilter
        }
        fun savedId(info: WorkInfo): String? = savedIdFromOutput(info)
        // WorkManager SUCCEEDED records survive deletion of the saved file.
        // Batch-check their IDs against the actual private audio library
        // before displaying Ready Offline or offering a play button.
        // The former UUID ordering could hide the newest saved tracks once
        // 60 historical transfers existed. Active jobs first, then most
        // recently requested jobs. Older untagged jobs remain visible later.
        val seenSearchUrls = HashSet<String>()
        val historyJobs = displayInfos.sortedWith(compareBy<WorkInfo> { it.state.isFinished }
            .thenByDescending { OfflineSaveRetryPolicy.requestedAt(it.tags) }
            .thenBy { it.id.toString() })
            .filter { info ->
                // A newer retry supersedes an older entry for the same song.
                // Deduplicate before calculating counters and page positions.
                val url = if (SearchTrackSaveQueue.TAG in info.tags)
                    OfflineSaveRetryPolicy.retryUrl(info.tags) else null
                url == null || seenSearchUrls.add(url)
            }
        val active = historyJobs.count {
            DownloadCenterViewPolicy.isActive(it.state.name)
        }
        val ready = historyJobs.count { isVerified(it) }
        val attention = historyJobs.count {
            DownloadFileVerificationPolicy.needsAttention(
                it.state.name, savedIdFromOutput(it), availableIds,
                verifyingFiles, fileCheckFailed)
        }
        overview.text = when {
            verifyingFiles -> "Checking saved files · $active active"
            fileCheckFailed -> "Could not verify saved files · Reopen Downloads to retry"
            else -> "$active active · $ready ready offline · $attention need attention"
        }
        val filteredJobs = historyJobs.filter { info ->
            // SUCCEEDED work cannot become an Issues alert while its
            // local file verification is still pending or inconclusive.
            if (currentFilter == DownloadCenterViewPolicy.Filter.ATTENTION)
                DownloadFileVerificationPolicy.needsAttention(
                    info.state.name, savedIdFromOutput(info),
                    availableIds, verifyingFiles, fileCheckFailed)
            else DownloadCenterViewPolicy.matches(
                currentFilter, info.state.name, isVerified(info))
        }
        // NW64: discard speed samples for jobs no longer running, even if
        // their row is on another page or hidden. Avoid stale rates and
        // unbounded transient UI bookkeeping across long sessions.
        val runningIds = infos.asSequence()
            .filter { it.state == WorkInfo.State.RUNNING }
            .map { it.id }.toSet()
        progressSamples.keys.retainAll(runningIds)
        observedSpeed.keys.retainAll(runningIds)
        // NW63: the previous .take(60) permanently hid older history.
        // Page after filtering, preserving every WorkManager record without
        // inflating unbounded rows. A shrinking queue clamps the saved page.
        historyPage = DownloadHistoryPagePolicy.normalizedPage(
            historyPage, filteredJobs.size)
        val pageStart = DownloadHistoryPagePolicy.offset(
            historyPage, filteredJobs.size)
        val visibleJobs = filteredJobs.subList(pageStart,
            minOf(filteredJobs.size, pageStart + DownloadHistoryPagePolicy.PAGE_SIZE))
        if (visibleJobs.isEmpty()) {
            entries.addView(label(
                if (verifyingFiles) "Checking saved audio files…"
                else if (fileCheckFailed) "Saved-file verification unavailable"
                else DownloadCenterViewPolicy.emptyMessage(currentFilter),
                14f, 0xFFB7CAD4.toInt()).apply { setPadding(dp(4), dp(15), 0, 0) })
            return
        }
        visibleJobs.forEach { info ->
            val isDirect = DirectAudioDownloadQueue.TAG in info.tags
            val isSearch = SearchTrackSaveQueue.TAG in info.tags
            val actuallySaved = DownloadFileVerificationPolicy.ready(
                info.state == WorkInfo.State.SUCCEEDED, savedId(info), availableIds,
                verifyingFiles, fileCheckFailed)
            val titlePrefix = when {
                isSearch -> SearchTrackSaveQueue.TITLE_TAG
                isDirect -> DirectAudioDownloadQueue.TITLE_TAG
                else -> CatalogDownloadQueue.TITLE_TAG
            }
            val title = info.tags.firstOrNull {
                it.startsWith(titlePrefix)
            }?.removePrefix(titlePrefix).orEmpty().ifBlank { "Audio file" }
            val received = info.progress.getLong(
                when {
                    isSearch -> SearchTrackSaveQueue.KEY_RECEIVED
                    isDirect -> DirectAudioDownloadQueue.KEY_RECEIVED
                    else -> CatalogDownloadQueue.KEY_RECEIVED
                }, 0L)
            val total = info.progress.getLong(
                when {
                    isSearch -> SearchTrackSaveQueue.KEY_TOTAL
                    isDirect -> DirectAudioDownloadQueue.KEY_TOTAL
                    else -> CatalogDownloadQueue.KEY_TOTAL
                }, -1L)
            val progress = DownloadCenterViewPolicy.percent(received, total)
                ?.let { " · $it%" }
                ?: if (received > 0L) " · " + (received / 1024L) + " KB" else ""
            val pausedByUser = isSearch &&
                SearchOfflinePauseState.isMarked(this, info.id)
            val now = SystemClock.elapsedRealtime()
            val previousSample = progressSamples[info.id]
            if (info.state == WorkInfo.State.RUNNING) {
                if (previousSample == null || received < previousSample.first) {
                    progressSamples[info.id] = received to now
                    observedSpeed.remove(info.id)
                } else if (received > previousSample.first) {
                    // Do not discard a valid baseline when UI ticks under
                    // 250ms: accumulate changes until we can calculate speed.
                    val rate = OfflineTransferRatePolicy.bytesPerSecond(
                        received - previousSample.first, now - previousSample.second)
                    if (rate != null) {
                        observedSpeed[info.id] = rate
                        progressSamples[info.id] = received to now
                    }
                } else if (now - previousSample.second > 8000L) {
                    observedSpeed.remove(info.id)
                }
            } else {
                progressSamples.remove(info.id)
                observedSpeed.remove(info.id)
            }
            val rate = if (info.state == WorkInfo.State.RUNNING)
                DownloadCenterViewPolicy.recentRate(observedSpeed[info.id],
                    progressSamples[info.id]?.second, now)
                else null
            val rateText = if (received > 0L && info.state == WorkInfo.State.RUNNING) {
                " · " + (if (rate == null && previousSample != null &&
                    now - previousSample.second > 8_000L)
                    "No new bytes · Waiting for source"
                    else OfflineTransferRatePolicy.display(rate)) +
                    (OfflineTransferRatePolicy.etaSeconds(received, total, rate)
                        ?.takeIf { it in 1..86400L }
                        ?.let { " · about " + (it / 60L) + "m " + (it % 60L) + "s left" }
                        ?: "")
            } else ""
            val state = when (info.state) {
                WorkInfo.State.ENQUEUED -> if (pausedByUser)
                    "Pausing · Waiting for worker to stop" else "Queued · Waiting for network"
                WorkInfo.State.RUNNING -> if (pausedByUser)
                    "Pausing · Cancelling current transfer" else
                    "Downloading" + progress + rateText
                WorkInfo.State.SUCCEEDED -> when {
                    verifyingFiles -> "Checking saved audio file…"
                    fileCheckFailed -> "Could not verify saved audio · Reopen Downloads"
                    else -> OfflineDownloadStatusPolicy.statusForCompleted(actuallySaved)
                }
                WorkInfo.State.FAILED -> "Failed · " +
                    info.outputData.getString(
                        when {
                            isSearch -> SearchTrackSaveQueue.KEY_ERROR
                            isDirect -> DirectAudioDownloadQueue.KEY_ERROR
                            else -> CatalogDownloadQueue.KEY_ERROR
                        }).orEmpty()
                WorkInfo.State.CANCELLED -> if (pausedByUser)
                    "Paused · Resume restarts from zero (partial bytes not retained)"
                    else "Cancelled · No partial track saved"
                WorkInfo.State.BLOCKED -> "Waiting for another queued job"
            }
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(0xFF162731.toInt())
                setPadding(dp(15), dp(13), dp(15), dp(8))
            }
            val heading = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            heading.addView(label(title, 16f, Color.WHITE, true).apply {
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(0, -2, 1f))
            if (info.state.isFinished) {
                heading.addView(label(
                    if (info.id.toString() in hidden) "↺" else "×",
                    22f, 0xFFADC0CE.toInt(), true).apply {
                    gravity = Gravity.CENTER
                    contentDescription = if (info.id.toString() in hidden)
                        "Restore this download entry to history"
                    else "Hide this download from history; the audio file will not be deleted"
                    setOnClickListener {
                        val ok = if (info.id.toString() in hidden)
                            DownloadHistoryVisibility.restore(this@CatalogDownloadsActivity, info.id)
                        else DownloadHistoryVisibility.hide(this@CatalogDownloadsActivity, info.id)
                        if (ok) refreshJobs()
                        else overview.text = "Could not update download history visibility."
                    }
                }, LinearLayout.LayoutParams(dp(48), dp(48)))
            }
            card.addView(heading)
            card.addView(label(state, 13f,
                if (info.state == WorkInfo.State.FAILED || (info.state == WorkInfo.State.SUCCEEDED && !actuallySaved))
                    0xFFF0B7B7.toInt() else 0xFFB4C7D3.toInt()).apply {
                setPadding(0, dp(5), 0, dp(7))
                maxLines = 3
            })
            if (info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED) {
                val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    val pct = DownloadCenterViewPolicy.percent(received, total)
                    isIndeterminate = pct == null
                    if (pct != null) {
                        max = 1000
                        this.progress = pct * 10
                    }
                    progressTintList = ColorStateList.valueOf(0xFF94E2D8.toInt())
                    progressBackgroundTintList = ColorStateList.valueOf(0xFF2D434F.toInt())
                    indeterminateTintList = ColorStateList.valueOf(0xFF94E2D8.toInt())
                }
                card.addView(bar, LinearLayout.LayoutParams(-1, dp(5)).apply {
                    bottomMargin = dp(7)
                })
            }
            when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                    val pauseUrl = if (isSearch)
                        OfflineSaveRetryPolicy.retryUrl(info.tags) else null
                    val actionRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    if (pauseUrl != null && !pausedByUser) {
                        actionRow.addView(label("Pause", 13f,
                            0xFF91E2D7.toInt(), true).apply {
                            gravity = Gravity.CENTER
                            contentDescription = "Pause download; resuming restarts from zero"
                            setOnClickListener { confirmSearchPause(info) }
                        }, LinearLayout.LayoutParams(0, dp(48), 1f))
                    }
                    actionRow.addView(label("Cancel", 14f, 0xFFF0B7B7.toInt(), true).apply {
                        gravity = Gravity.CENTER
                        contentDescription = "Cancel download"
                        setOnClickListener {
                            // Cancel is distinct from Pause. A deliberate
                            // cancel must never leave a resumable UI marker.
                            if (isSearch) SearchOfflinePauseState.clear(this@CatalogDownloadsActivity, info.id)
                            manager.cancelWorkById(info.id)
                        }
                    }, LinearLayout.LayoutParams(0, dp(48), 1f))
                    card.addView(actionRow)
                }
                WorkInfo.State.SUCCEEDED -> {
                    val savedTitle = if (isDirect || isSearch) title else info.outputData.getString(
                        CatalogDownloadQueue.KEY_SAVED_TITLE).orEmpty()
                    card.addView(label(
                        if (!actuallySaved) "Open Offline Library  →"
                        else if (isSearch) "Play saved offline  →" else "Open saved song  →",
                        14f, 0xFF94E2D8.toInt(), true).apply {
                        gravity = Gravity.CENTER
                        setOnClickListener {
                            val id = if (isSearch && actuallySaved)
                                info.outputData.getString(SearchTrackSaveQueue.KEY_ID) else null
                            val offline = runCatching {
                                val library = OfflineAudioLibrary(this@CatalogDownloadsActivity)
                                id?.let { library.find(it) }
                                    ?.takeIf { library.fileFor(it) != null }
                            }.getOrNull()
                            if (offline != null) {
                                startService(Intent(this@CatalogDownloadsActivity,
                                    com.nightwave.player.playback.PlaybackService::class.java)
                                    .setAction(com.nightwave.player.playback.PlaybackService.ACTION_PLAY_OFFLINE)
                                    .putExtra(com.nightwave.player.playback.PlaybackService.EXTRA_OFFLINE_ID,
                                        offline.id))
                            } else {
                                startActivity(Intent(this@CatalogDownloadsActivity,
                                    OfflineLibraryActivity::class.java)
                                    .putExtra(OfflineLibraryActivity.EXTRA_SEARCH_QUERY, savedTitle))
                            }
                        }
                    }, LinearLayout.LayoutParams(-1, dp(48)))
                    // A historical success record is not an offline file.
                    // Offer recovery of the exact searched recording only
                    // when the local file was later deleted/unavailable.
                    val restoreUrl = if (isSearch && !actuallySaved)
                        OfflineSaveRetryPolicy.retryUrl(info.tags) else null
                    if (restoreUrl != null) {
                        card.addView(label("Save this song again  →", 14f,
                            0xFF9FE3DF.toInt(), true).apply {
                            gravity = Gravity.CENTER
                            setOnClickListener { confirmSearchRetry(restoreUrl, title) }
                        }, LinearLayout.LayoutParams(-1, dp(48)))
                    }
                }
                else -> {
                    val retryUrl = if (isSearch)
                        OfflineSaveRetryPolicy.retryUrl(info.tags) else null
                    if (retryUrl != null &&
                        OfflineSaveRetryPolicy.canOfferRetry(
                            info.state.isFinished, false, info.tags)) {
                        card.addView(label(
                            if (pausedByUser) "Start download again  →"
                            else "Retry saving this song  →", 14f,
                            0xFF9FE3DF.toInt(), true).apply {
                            gravity = Gravity.CENTER
                            setOnClickListener {
                                confirmSearchRetry(retryUrl, title,
                                    if (pausedByUser) info.id else null)
                            }
                        }, LinearLayout.LayoutParams(-1, dp(48)))
                    }
                    card.addView(label(
                        if (isSearch) "View other offline audio sources  →"
                        else if (isDirect) "Try another direct audio link  →"
                        else "Find another recording  →", 14f,
                        0xFF94E2D8.toInt(), true).apply {
                        gravity = Gravity.CENTER
                        setOnClickListener {
                            if (isDirect) {
                                startActivity(Intent(this@CatalogDownloadsActivity,
                                    PermittedAudioDownloadActivity::class.java))
                            } else {
                                startActivity(Intent(this@CatalogDownloadsActivity,
                                    FreeAudioCatalogActivity::class.java)
                                    .putExtra(FreeAudioCatalogActivity.EXTRA_QUERY, title))
                            }
                        }
                    }, LinearLayout.LayoutParams(-1, dp(48)))
                }
            }
            entries.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(9)
            })
        }
        if (DownloadHistoryPagePolicy.pageCount(filteredJobs.size) > 1) {
            val pageSummary = DownloadHistoryPagePolicy.summary(
                historyPage, filteredJobs.size)
            entries.addView(label(pageSummary, 12f, 0xFFB7CAD4.toInt()).apply {
                gravity = Gravity.CENTER
                contentDescription = pageSummary
            }, LinearLayout.LayoutParams(-1, dp(35)))
            val pager = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val canGoNewer = DownloadHistoryPagePolicy.hasPrevious(
                historyPage, filteredJobs.size)
            pager.addView(label("‹  Newer", 14f, 0xFF98E5DB.toInt(), true).apply {
                gravity = Gravity.CENTER
                isEnabled = canGoNewer
                alpha = if (canGoNewer) 1f else 0.40f
                contentDescription = "Newer download history page"
                setOnClickListener {
                    historyPage = (historyPage - 1).coerceAtLeast(0)
                    resetHistoryScroll = true
                    render(catalogJobs + directJobs + searchedJobs)
                }
            }, LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                marginEnd = dp(5)
            })
            val canGoOlder = DownloadHistoryPagePolicy.hasNext(
                historyPage, filteredJobs.size)
            pager.addView(label("Older  ›", 14f, 0xFF98E5DB.toInt(), true).apply {
                gravity = Gravity.CENTER
                isEnabled = canGoOlder
                alpha = if (canGoOlder) 1f else 0.40f
                contentDescription = "Older download history page"
                setOnClickListener {
                    historyPage += 1
                    resetHistoryScroll = true
                    render(catalogJobs + directJobs + searchedJobs)
                }
            }, LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                marginStart = dp(5)
            })
            entries.addView(pager, LinearLayout.LayoutParams(-1, dp(52)).apply {
                bottomMargin = dp(15)
            })
        }
        scroll.post {
            if (!isFinishing && !isDestroyed) scroll.scrollTo(0, originalPosition)
        }
    }

    private fun confirmSearchPause(info: WorkInfo) {
        if (SearchTrackSaveQueue.TAG !in info.tags ||
            OfflineSaveRetryPolicy.retryUrl(info.tags) == null ||
            info.state.isFinished) return
        AlertDialog.Builder(this)
            .setTitle("Pause this download?")
            .setMessage("NightWave will stop this transfer safely. Its partial audio " +
                "is removed; tapping Resume starts again from zero because " +
                "the source may not support byte-range continuation.")
            .setPositiveButton("Pause") { _, _ ->
                if (SearchOfflinePauseState.mark(this, info.id)) {
                    manager.cancelWorkById(info.id)
                    overview.text = "Stopping download. Resume is available once cancelled."
                } else overview.text = "Could not record pause safely. Nothing cancelled."
            }
            .setNegativeButton("Keep downloading", null)
            .show()
    }

    private fun confirmSearchRetry(canonicalUrl: String, title: String,
        resumeOf: UUID? = null) {
        // WorkInfo exposes only tags (not original worker input). This URL
        // came from one exact validated 11-character video ID. No signed CDN
        // URL, personal data or auth cookie is persisted or retried here.
        val validUrl = OfflineSaveRetryPolicy.retryUrl(
            setOf(OfflineSaveRetryPolicy.sourceTag(canonicalUrl)))
        if (validUrl != canonicalUrl) return
        AlertDialog.Builder(this)
            .setTitle(if (resumeOf != null) "Resume saving?" else "Retry offline save?")
            .setMessage((if (resumeOf != null)
                "Resume restarts from zero; earlier bytes were not retained. " else "") +
                "Retry this same searched recording, rather than a different " +
                "catalog match. Only proceed if you have permission to keep it offline. " +
                "Unavailable, restricted, or protected audio cannot be saved.")
            .setPositiveButton("Retry save") { _, _ ->
                val requested = runCatching {
                    SearchTrackSaveQueue.enqueue(
                        this, VideoPreview(title, "", canonicalUrl, "", 0L))
                }
                requested.onSuccess { operation ->
                    operation.result.addListener({
                        if (isFinishing || isDestroyed) return@addListener
                        runCatching { operation.result.get() }
                            .onSuccess {
                                if (resumeOf != null) SearchOfflinePauseState.clear(
                                    this@CatalogDownloadsActivity, resumeOf)
                                overview.text = "Save queued. Its result appears in Downloads."
                            }
                            .onFailure {
                                overview.text = "Could not queue retry. Try from Search."
                            }
                    }, androidx.core.content.ContextCompat.getMainExecutor(this))
                    overview.text = "Requesting another save attempt..."
                }.onFailure {
                    overview.text = "Retry cannot be queued. Try from Search."
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rounded(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(17).toFloat()
    }
    private fun label(value: String, sp: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = sp
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
    private fun dp(value: Int) = (resources.displayMetrics.density * value + 0.5f).toInt()
}
