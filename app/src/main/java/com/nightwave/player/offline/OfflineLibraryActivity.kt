package com.nightwave.player.offline

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.nightwave.player.playback.PlaybackService
import com.nightwave.player.v2.locker.NightWaveLockerActivity
import java.util.concurrent.Executors

/** Native offline UI: safe to open even if the new Compose preview crashes. */
class OfflineLibraryActivity : ComponentActivity() {
    private lateinit var library: OfflineAudioLibrary
    private lateinit var tracks: LinearLayout
    private lateinit var state: TextView
    private lateinit var resumeControl: TextView
    private lateinit var sortControl: TextView
    private lateinit var playAllControl: TextView
    private var searchText = ""
    private var sortMode = 0
    private var visibleSongs: List<OfflineSong> = emptyList()
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
        library = OfflineAudioLibrary(this)
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
        root.addView(label("Offline library", 26f, Color.WHITE, true).apply {
            setPadding(0, dp(20), 0, dp(4))
        })
        root.addView(label("Saved audio, available without internet.", 14f, 0xFFABB8CC.toInt()).apply {
            setPadding(0, 0, 0, dp(15))
        })
        root.addView(label("☁  My Google Drive music", 14f, 0xFF91E2D7.toInt(), true).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), 0, 0, 0)
            setOnClickListener {
                startActivity(Intent(this@OfflineLibraryActivity, NightWaveLockerActivity::class.java))
            }
        }, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(label("+  Import audio files", 15f, 0xFF101A21.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = cardBackground(0xFF91E2D7.toInt())
            setOnClickListener {
                pick.launch(arrayOf("audio/*", "application/ogg", "application/octet-stream"))
            }
        }, LinearLayout.LayoutParams(-1, dp(51)))
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
                    if (::tracks.isInitialized) refresh()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(49)).apply {
            topMargin = dp(14)
        })
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        sortControl = label("Sort: " + sortLabels[sortMode] + "  ▾", 12f, 0xFFB5C8D8.toInt()).apply {
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener {
                AlertDialog.Builder(this@OfflineLibraryActivity)
                    .setTitle("Sort offline music")
                    .setSingleChoiceItems(sortLabels, sortMode) { dialog, which ->
                        sortMode = which
                        text = "Sort: " + sortLabels[sortMode] + "  ▾"
                        dialog.dismiss()
                        refresh()
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
        controls.addView(playAllControl, LinearLayout.LayoutParams(dp(105), dp(49)))
        root.addView(controls)
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
        root.addView(ScrollView(this).apply { addView(tracks) },
            LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(label(
            "Private on-device storage · Deleted if NightWave is uninstalled",
            11f, 0xFF8294A8.toInt()
        ).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(45)))
        setContentView(root)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::tracks.isInitialized) refresh()
    }

    private fun refresh() {
        val report = library.healthReport()
        if (!report.indexReadable) {
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
        val healthById = report.entries.associate { it.song.id to it.health }
        playableIds = report.entries.asSequence()
            .filter { it.health == OfflineFileHealth.AVAILABLE }
            .map { it.song.id }.toSet()
        val filtered = all.filter { it.title.contains(searchText, ignoreCase = true) }
        visibleSongs = when (sortMode) {
            1 -> filtered.sortedWith(compareBy<OfflineSong> { it.title.lowercase() }.thenBy { it.id })
            2 -> filtered.sortedWith(compareBy<OfflineSong> { it.bytes }.thenBy { it.title.lowercase() })
            3 -> filtered.sortedWith(compareByDescending<OfflineSong> { it.bytes }.thenBy { it.title.lowercase() })
            else -> filtered.asReversed()
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
        tracks.removeAllViews()
        if (visibleSongs.isEmpty()) tracks.addView(label(
            if (all.isEmpty()) "No offline songs yet. Import audio you own or are licensed to use."
            else "No matching songs. Try another search.",
            14f, 0xFFABB8CC.toInt()
        ).apply { setPadding(dp(5), dp(25), 0, 0) })
        visibleSongs.forEach { song ->
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
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            description.addView(label((song.bytes / 1024 / 1024).toString() +
                " MB · " + availability,
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
            }, LinearLayout.LayoutParams(dp(49), dp(47)))
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
            }, LinearLayout.LayoutParams(dp(43), dp(47)))
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
                                if (library.delete(song)) refresh()
                                else status("Unable to delete offline copy; entry kept if unverified")
                            }.show()
                    }
                }
            }, LinearLayout.LayoutParams(dp(39), dp(47)))
            tracks.addView(row, LinearLayout.LayoutParams(-1, dp(65)).apply {
                bottomMargin = dp(8)
            })
        }
    }

    private fun launchPlayback(song: OfflineSong) {
        if (library.fileFor(song) == null) {
            status("Audio file unavailable: " + song.title)
            return
        }
        // Only app-private UUID IDs are passed; playback validates against its own
        // library index before opening any media file.
        val ids = ArrayList(visibleSongs
            .filter { it.id in playableIds }
            .map { it.id }.take(OfflinePlaybackQueue.MAX_ITEMS))
        runCatching {
            startService(Intent(this, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_PLAY_OFFLINE)
                .putExtra(PlaybackService.EXTRA_OFFLINE_ID, song.id)
                .putStringArrayListExtra(PlaybackService.EXTRA_OFFLINE_IDS, ids))
        }.onSuccess { status("Playing " + song.title + " · Next follows this list") }
            .onFailure { status("Offline playback could not start. Please retry.") }
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
    override fun onDestroy() { worker.shutdown(); super.onDestroy() }
}
