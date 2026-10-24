package com.nightwave.player.offline

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.work.WorkInfo
import androidx.work.WorkManager

/** WorkManager queue remains accessible even if the Compose player cannot open. */
class CatalogDownloadsActivity : ComponentActivity() {
    private lateinit var entries: LinearLayout
    private lateinit var overview: TextView
    private lateinit var scroll: ScrollView
    private val manager by lazy { WorkManager.getInstance(applicationContext) }
    private var catalogJobs: List<WorkInfo> = emptyList()
    private var directJobs: List<WorkInfo> = emptyList()
    private fun refreshJobs() = render(catalogJobs + directJobs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
            setBackgroundColor(0xFF0C131B.toInt())
        }
        root.addView(label("‹  NightWave", 18f, 0xFF98E5DB.toInt(), true).apply {
            setPadding(0, 0, 0, dp(15))
            setOnClickListener { finish() }
        })
        root.addView(label("Downloads", 28f, Color.WHITE, true))
        root.addView(label("Persistent queue · Authorized audio files",
            13f, 0xFFABC0CB.toInt()).apply {
            setPadding(0, dp(7), 0, dp(14))
        })
        root.addView(label("Search downloadable music  →", 14f, 0xFF0C1B20.toInt(), true).apply {
            background = rounded(0xFF94E2D8.toInt())
            gravity = Gravity.CENTER
            setOnClickListener {
                startActivity(Intent(this@CatalogDownloadsActivity,
                    FreeAudioCatalogActivity::class.java))
            }
        }, LinearLayout.LayoutParams(-1, dp(52)))
        root.addView(label("Open Offline Library  →", 14f, 0xFF94E2D8.toInt(), true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                startActivity(Intent(this@CatalogDownloadsActivity,
                    OfflineLibraryActivity::class.java))
            }
        }, LinearLayout.LayoutParams(-1, dp(48)))
        overview = label("Loading queued downloads…", 13f, 0xFFB7CAD4.toInt())
        root.addView(overview, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(11)
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
    }

    private fun render(infos: List<WorkInfo>) {
        if (isFinishing || isDestroyed) return
        val originalPosition = if (::scroll.isInitialized) scroll.scrollY else 0
        entries.removeAllViews()
        val active = infos.count { !it.state.isFinished }
        val completed = infos.count { it.state == WorkInfo.State.SUCCEEDED }
        overview.text = "$active queued/running · $completed completed · " +
            infos.size + " jobs"
        if (infos.isEmpty()) {
            entries.addView(label("No downloads yet. Search for CC0/public-domain audio.",
                14f, 0xFFB7CAD4.toInt()))
            return
        }
        infos.sortedWith(compareBy<WorkInfo> { it.state.isFinished }
            .thenBy { it.id.toString() }).take(60).forEach { info ->
            val isDirect = DirectAudioDownloadQueue.TAG in info.tags
            val titlePrefix = if (isDirect) DirectAudioDownloadQueue.TITLE_TAG
                else CatalogDownloadQueue.TITLE_TAG
            val title = info.tags.firstOrNull {
                it.startsWith(titlePrefix)
            }?.removePrefix(titlePrefix).orEmpty().ifBlank { "Audio file" }
            val received = info.progress.getLong(
                if (isDirect) DirectAudioDownloadQueue.KEY_RECEIVED
                else CatalogDownloadQueue.KEY_RECEIVED, 0L)
            val total = info.progress.getLong(
                if (isDirect) DirectAudioDownloadQueue.KEY_TOTAL
                else CatalogDownloadQueue.KEY_TOTAL, -1L)
            val progress = if (total > 0L) {
                " · " + (100L * received / total).coerceIn(0, 100) + "%"
            } else if (received > 0L) " · " + (received / 1024L) + " KB" else ""
            val state = when (info.state) {
                WorkInfo.State.ENQUEUED -> "Queued · Waiting for network"
                WorkInfo.State.RUNNING -> "Downloading" + progress
                WorkInfo.State.SUCCEEDED -> "Saved in Offline Library"
                WorkInfo.State.FAILED -> "Failed · " +
                    info.outputData.getString(
                        if (isDirect) DirectAudioDownloadQueue.KEY_ERROR
                        else CatalogDownloadQueue.KEY_ERROR).orEmpty()
                WorkInfo.State.CANCELLED -> "Cancelled · No partial track saved"
                WorkInfo.State.BLOCKED -> "Waiting for another queued job"
            }
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(0xFF182B35.toInt())
                setPadding(dp(14), dp(13), dp(14), dp(11))
            }
            card.addView(label(title, 15f, Color.WHITE, true).apply { maxLines = 2 })
            card.addView(label(state, 12f, 0xFFB4C7D3.toInt()).apply {
                setPadding(0, dp(6), 0, dp(8))
                maxLines = 3
            })
            when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                    card.addView(label("Cancel download", 14f, 0xFFF0B7B7.toInt(), true).apply {
                        gravity = Gravity.CENTER
                        setOnClickListener { manager.cancelWorkById(info.id) }
                    }, LinearLayout.LayoutParams(-1, dp(48)))
                }
                WorkInfo.State.SUCCEEDED -> {
                    val savedTitle = if (isDirect) title else info.outputData.getString(
                        CatalogDownloadQueue.KEY_SAVED_TITLE).orEmpty()
                    card.addView(label("Open saved song  →", 14f, 0xFF94E2D8.toInt(), true).apply {
                        gravity = Gravity.CENTER
                        setOnClickListener {
                            startActivity(Intent(this@CatalogDownloadsActivity,
                                OfflineLibraryActivity::class.java)
                                .putExtra(OfflineLibraryActivity.EXTRA_SEARCH_QUERY, savedTitle))
                        }
                    }, LinearLayout.LayoutParams(-1, dp(48)))
                }
                else -> {
                    card.addView(label(
                        if (isDirect) "Try another direct audio link  →"
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
                bottomMargin = dp(11)
            })
        }
        scroll.post {
            if (!isFinishing && !isDestroyed) scroll.scrollTo(0, originalPosition)
        }
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
