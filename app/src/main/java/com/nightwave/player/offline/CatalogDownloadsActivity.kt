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
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ProgressBar
import android.content.res.ColorStateList
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var scroll: ScrollView
    private val manager by lazy { WorkManager.getInstance(applicationContext) }
    private var catalogJobs: List<WorkInfo> = emptyList()
    private var directJobs: List<WorkInfo> = emptyList()
    private var searchedJobs: List<WorkInfo> = emptyList()
    private val progressSamples = HashMap<UUID, Pair<Long, Long>>()
    private val observedSpeed = HashMap<UUID, Long>()
    // Previous code checked every saved file on the UI thread whenever a
    // WorkManager event or periodic transfer tick refreshed the whole list.
    private var healthRefreshJob: Job? = null
    private var healthReadGeneration = 0L
    private var uiPaused = false
    private val uiHandler = Handler(Looper.getMainLooper())
    private val stagnantTransferRefresh = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                if ((catalogJobs + directJobs + searchedJobs).any {
                        it.state == WorkInfo.State.RUNNING
                    } && ::entries.isInitialized) refreshJobs()
                uiHandler.postDelayed(this, 5_000L)
            }
        }
    }
    private fun refreshJobs() {
        if (uiPaused || !::entries.isInitialized || isFinishing || isDestroyed) return
        val snapshot = catalogJobs + directJobs + searchedJobs
        val generation = ++healthReadGeneration
        healthRefreshJob?.cancel()
        healthRefreshJob = lifecycleScope.launch {
            // Inspect real physical files on IO. Status remains unverified
            // until this finishes; a WorkManager SUCCEEDED flag is never
            // treated as proof that the downloaded file still exists.
            val available = withContext(Dispatchers.IO) {
                val savedIds = snapshot.asSequence()
                    .filter { it.state == WorkInfo.State.SUCCEEDED }
                    .mapNotNull { job ->
                        when {
                            SearchTrackSaveQueue.TAG in job.tags ->
                                job.outputData.getString(SearchTrackSaveQueue.KEY_ID)
                            DirectAudioDownloadQueue.TAG in job.tags ->
                                job.outputData.getString(DirectAudioDownloadQueue.KEY_DONE)
                            else -> job.outputData.getString(CatalogDownloadQueue.KEY_DONE)
                        }
                    }.toSet()
                runCatching {
                    OfflineAudioLibrary(applicationContext).availableIds(savedIds)
                }.getOrDefault(emptySet())
            }
            if (!DownloadHealthRefreshPolicy.mayPublish(
                    generation, healthReadGeneration, uiPaused, isFinishing, isDestroyed
                )) return@launch
            render(snapshot, available)
        }
    }

    override fun onResume() {
        super.onResume()
        uiPaused = false
        // A user may delete saved audio in Offline Library and return here
        // without any WorkManager status event. Recheck real file availability.
        if (::entries.isInitialized && ::overview.isInitialized) refreshJobs()
        uiHandler.removeCallbacks(stagnantTransferRefresh)
        uiHandler.postDelayed(stagnantTransferRefresh, 5_000L)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("nw48-download-filter", currentFilter.name)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        uiPaused = true
        ++healthReadGeneration
        healthRefreshJob?.cancel()
        uiHandler.removeCallbacks(stagnantTransferRefresh)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentFilter = savedInstanceState?.getString("nw48-download-filter")
            ?.let { stored -> runCatching {
                DownloadCenterViewPolicy.Filter.valueOf(stored)
            }.getOrNull() } ?: DownloadCenterViewPolicy.Filter.ALL
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
                refreshJobs()
            }
        manager.getWorkInfosByTagLiveData(DirectAudioDownloadQueue.TAG)
            .observe(this) { updates ->
                directJobs = updates.orEmpty()
                refreshJobs()
            }
        manager.getWorkInfosByTagLiveData(SearchTrackSaveQueue.TAG)
            .observe(this) { updates ->
                searchedJobs = updates.orEmpty()
                refreshJobs()
            }
    }

    private fun render(infos: List<WorkInfo>, availableIds: Set<String>) {
        if (isFinishing || isDestroyed) return
        val originalPosition = if (::scroll.isInitialized) scroll.scrollY else 0
        entries.removeAllViews()
        val hidden = DownloadHistoryVisibility.hiddenIds(this)
        val displayInfos = if (showHiddenJobs) infos else infos.filterNot {
            it.id.toString() in hidden
        }
        historyToggle.text = if (showHiddenJobs)
            "Hide dismissed jobs · ${hidden.size} hidden  ▴"
        else "Show dismissed jobs · ${hidden.size} hidden  ▾"
        // A generation-fenced, physical-file-verified snapshot arrives
        // from Dispatchers.IO. Never do canonical-file or index I/O here.
        fun isVerified(info: WorkInfo): Boolean {
            val saved = when {
                SearchTrackSaveQueue.TAG in info.tags ->
                    info.outputData.getString(SearchTrackSaveQueue.KEY_ID)
                DirectAudioDownloadQueue.TAG in info.tags ->
                    info.outputData.getString(DirectAudioDownloadQueue.KEY_DONE)
                else -> info.outputData.getString(CatalogDownloadQueue.KEY_DONE)
            }
            return OfflineDownloadStatusPolicy.isReady(
                info.state == WorkInfo.State.SUCCEEDED, saved, availableIds)
        }
        val active = displayInfos.count {
            DownloadCenterViewPolicy.isActive(it.state.name)
        }
        val ready = displayInfos.count { isVerified(it) }
        val attention = displayInfos.count {
            DownloadCenterViewPolicy.needsAttention(it.state.name, isVerified(it))
        }
        overview.text = "$active active · $ready ready offline · $attention need attention"
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
                    refreshJobs()
                }
            }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            })
        }
        fun savedId(info: WorkInfo): String? = when {
            SearchTrackSaveQueue.TAG in info.tags ->
                info.outputData.getString(SearchTrackSaveQueue.KEY_ID)
            DirectAudioDownloadQueue.TAG in info.tags ->
                info.outputData.getString(DirectAudioDownloadQueue.KEY_DONE)
            else -> info.outputData.getString(CatalogDownloadQueue.KEY_DONE)
        }
        // WorkManager SUCCEEDED records survive deletion of the saved file.
        // Batch-check their IDs against the actual private audio library
        // before displaying Ready Offline or offering a play button.
        // The former UUID ordering could hide the newest saved tracks once
        // 60 historical transfers existed. Active jobs first, then most
        // recently requested jobs. Older untagged jobs remain visible later.
        val seenSearchUrls = HashSet<String>()
        val visibleJobs = displayInfos.sortedWith(compareBy<WorkInfo> { it.state.isFinished }
            .thenByDescending { OfflineSaveRetryPolicy.requestedAt(it.tags) }
            .thenBy { it.id.toString() })
            .filter { info ->
                // Retry creates a fresh WorkRequest. Keep the newest/current
                // entry for each identified song; do not show stale failures
                // as duplicate rows next to a newer successful attempt.
                val url = if (SearchTrackSaveQueue.TAG in info.tags)
                    OfflineSaveRetryPolicy.retryUrl(info.tags) else null
                url == null || seenSearchUrls.add(url)
            }.filter { info ->
                DownloadCenterViewPolicy.matches(
                    currentFilter, info.state.name, isVerified(info))
            }.take(60)
        if (visibleJobs.isEmpty()) {
            entries.addView(label(DownloadCenterViewPolicy.emptyMessage(currentFilter),
                14f, 0xFFB7CAD4.toInt()).apply { setPadding(dp(4), dp(15), 0, 0) })
            return
        }
        visibleJobs.forEach { info ->
            val isDirect = DirectAudioDownloadQueue.TAG in info.tags
            val isSearch = SearchTrackSaveQueue.TAG in info.tags
            val actuallySaved = OfflineDownloadStatusPolicy.isReady(
                info.state == WorkInfo.State.SUCCEEDED, savedId(info), availableIds)
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
                WorkInfo.State.SUCCEEDED ->
                    OfflineDownloadStatusPolicy.statusForCompleted(actuallySaved)
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
