package com.nightwave.player.offline

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.nightwave.player.playback.PlaybackService
import java.util.concurrent.Executors

/** Native offline UI: safe to open even if the new Compose preview crashes. */
class OfflineLibraryActivity : ComponentActivity() {
    private lateinit var library: OfflineAudioLibrary
    private lateinit var tracks: LinearLayout
    private lateinit var state: TextView
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
        root.addView(label("+  Import audio files", 15f, 0xFF101A21.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = cardBackground(0xFF91E2D7.toInt())
            setOnClickListener {
                pick.launch(arrayOf("audio/*", "application/ogg", "application/octet-stream"))
            }
        }, LinearLayout.LayoutParams(-1, dp(51)))
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
        val all = try { library.songs() } catch (e: Exception) {
            status("Library index needs attention: " + e.message)
            return
        }
        status(all.size.toString() + " tracks saved · Available offline")
        tracks.removeAllViews()
        if (all.isEmpty()) tracks.addView(label(
            "No offline songs yet. Import audio you own or are licensed to use.",
            14f, 0xFFABB8CC.toInt()
        ).apply { setPadding(dp(5), dp(25), 0, 0) })
        all.forEach { song ->
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
            description.addView(label((song.bytes / 1024 / 1024).toString() + " MB · Offline",
                12f, 0xFF94A9BB.toInt()))
            row.addView(description, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(label("▶", 21f, 0xFF91E2D7.toInt()).apply {
                gravity = Gravity.CENTER
                contentDescription = "Play " + song.title
                setOnClickListener {
                    if (library.fileFor(song) == null) {
                        status("Audio file not available: " + song.title)
                    } else {
                        runCatching {
                            startService(Intent(this@OfflineLibraryActivity, PlaybackService::class.java)
                                .setAction(PlaybackService.ACTION_PLAY_OFFLINE)
                                .putExtra(PlaybackService.EXTRA_OFFLINE_ID, song.id))
                        }.onSuccess { status("Opening " + song.title) }
                            .onFailure { status("Offline playback could not start. Please retry.") }
                    }
                }
            }, LinearLayout.LayoutParams(dp(49), dp(47)))
            row.addView(label("⋮", 24f, 0xFFB9C9D6.toInt()).apply {
                gravity = Gravity.CENTER
                contentDescription = "Remove offline track " + song.title
                setOnClickListener {
                    AlertDialog.Builder(this@OfflineLibraryActivity)
                        .setTitle("Remove offline song?")
                        .setMessage("Delete " + song.title + " from NightWave offline storage?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ ->
                            if (library.delete(song)) refresh()
                            else status("Unable to delete offline copy")
                        }.show()
                }
            }, LinearLayout.LayoutParams(dp(39), dp(47)))
            tracks.addView(row, LinearLayout.LayoutParams(-1, dp(65)).apply {
                bottomMargin = dp(8)
            })
        }
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
