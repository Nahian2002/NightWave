package com.nightwave.player.v2.locker

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.nightwave.player.offline.OfflineAudioLibrary
import com.nightwave.player.offline.OfflineSong
import com.nightwave.player.offline.CloudOfflineImportResult
import java.io.File
import java.util.concurrent.Executors

/**
 * Deliberately separate audio permissions and UI from existing JSON backups.
 * Cloud uploads require an explicit tap and fresh authorized account/scope.
 * No server, credential persistence, automatic upload or MP3 conversion.
 */
class NightWaveLockerActivity : ComponentActivity() {
    companion object { const val EXTRA_OFFLINE_ID = "nightwave_locker_song_id" }
    private lateinit var offline: OfflineAudioLibrary
    private lateinit var state: LockerAccountState
    private lateinit var statusView: TextView
    private lateinit var listView: LinearLayout
    private lateinit var accountView: TextView
    private lateinit var uploadButton: TextView
    private var selected: OfflineSong? = null
    private var pendingAction = ""
    private var pendingDownload: LockerRemoteTrack? = null
    private var busy = false
    private val worker = Executors.newSingleThreadExecutor()
    private val resolution = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) {
            busy = false
            pendingAction = ""
            status("Google permission was cancelled.")
        } else {
            runCatching {
                Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(result.data!!)
            }.onSuccess { authorized(it) }
                .onFailure {
                    busy = false
                    pendingAction = ""
                    status("Google authorization failed. Try reconnecting.")
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        offline = OfflineAudioLibrary(this)
        state = LockerAccountState(this)
        val id = intent.getStringExtra(EXTRA_OFFLINE_ID).orEmpty()
        selected = runCatching { offline.find(id) }.getOrNull()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(0xFF090D14.toInt())
        }
        layout.addView(text("NightWave Music", 25f, Color.WHITE, true))
        layout.addView(text("Your audio · Your Google Drive", 13f, 0xFF9AB0C3.toInt()))
        layout.addView(text("No NightWave server. Only files you own or have permission to store.",
            12f, 0xFFBAC8D4.toInt()))
        accountView = text("", 13f, 0xFF9DE4D8.toInt())
        accountView.setPadding(0, dp(18), 0, dp(10))
        layout.addView(accountView)
        layout.addView(button("Connect Google Drive · music files") {
            begin("connect")
        })
        val title = selected?.title ?: "No local song selected"
        layout.addView(text("Selected: " + title, 13f, Color.WHITE).apply {
            setPadding(0, dp(15), 0, dp(4))
            maxLines = 2
        })
        uploadButton = button("Upload selected local song") {
            if (selected == null) status("Open an offline song and choose upload.")
            else begin("upload")
        }
        layout.addView(uploadButton)
        layout.addView(button("Refresh my uploaded songs") { begin("list") })
        layout.addView(button("Disconnect this phone's music access") {
            if (busy) return@button
            if (state.disconnect()) {
                updateAccount()
                listView.removeAllViews()
                status("Disconnected on this phone. Music files remain in Google Drive.")
            }
        })
        statusView = text("", 12f, 0xFFABC1D0.toInt()).apply {
            setPadding(0, dp(12), 0, dp(12))
        }
        layout.addView(statusView)
        listView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(ScrollView(this).apply { addView(listView) },
            LinearLayout.LayoutParams(-1, 0, 1f))
        layout.addView(text("Upload only with permission. Tap a cloud track to review and " +
            "download it. Downloaded files are checked before entering Offline Library.",
            12f, 0xFF8AA0B3.toInt()))
        setContentView(layout)
        updateAccount()
        status("Connect Google Drive, then choose a manual upload or browse your cloud library.")
    }

    private fun begin(action: String, download: LockerRemoteTrack? = null) {
        if (busy) return
        if (action == "upload" && selected == null) {
            status("No local audio selected for upload.")
            return
        }
        if (action == "download" && download == null) return
        busy = true
        pendingAction = action
        pendingDownload = download
        status("Requesting limited Google Drive music permission…")
        Identity.getAuthorizationClient(this)
            .authorize(NightWaveLockerAuthorization.request(state.account().takeIf { it.isNotBlank() }))
            .addOnSuccessListener { response ->
                if (response.hasResolution()) {
                    val intent = response.pendingIntent
                    if (intent == null) {
                        busy = false
                        pendingAction = ""
                        status("Google could not open the permission screen.")
                    } else runCatching {
                        resolution.launch(IntentSenderRequest.Builder(intent.intentSender).build())
                    }.onFailure {
                        busy = false
                        pendingAction = ""
                        status("Unable to open Google's music permission screen.")
                    }
                } else authorized(response)
            }
            .addOnFailureListener {
                busy = false
                pendingAction = ""
                status("Drive permission failed. Check Google Play services and OAuth setup.")
            }
    }

    private fun authorized(result: AuthorizationResult) {
        val action = pendingAction
        val chosenRemote = pendingDownload
        pendingAction = ""
        pendingDownload = null
        val name = NightWaveLockerAuthorization.account(result)
        val token = runCatching { NightWaveLockerAuthorization.token(result) }.getOrNull()
        if (name.isNullOrBlank() || token == null || !state.connect(name)) {
            busy = false
            status("Google account mismatch or Drive music permission missing. " +
                "Disconnect locally before changing accounts.")
            return
        }
        updateAccount()
        if (action == "connect") {
            busy = false
            status("Music Drive permission granted. Uploads, browsing and downloads require separate actions.")
            return
        }
        worker.execute {
            val expected = name
            val outcome = runCatching {
                val client = NightWaveMusicDriveClient(token)
                if (action == "upload") {
                    val song = requireNotNull(selected)
                    val file = requireNotNull(offline.fileFor(song)) {
                        "Selected local audio is unavailable"
                    }
                    client.upload(song, file) { percent ->
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed)
                                status("Uploading authorized audio… $percent%")
                        }
                    }
                } else if (action == "download") {
                    val track = requireNotNull(chosenRemote)
                    val staging = client.downloadToStaging(track,
                        File(filesDir, "locker_staging")) { percent ->
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed)
                                status("Downloading and verifying audio… $percent%")
                        }
                    }
                    try {
                        offline.importVerifiedCloud(staging, track)
                    } finally {
                        // The import moves a verified file into offline_audio.
                        // Every failed/incomplete staged copy is cleaned up.
                        staging.delete()
                    }
                } else client.list()
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                busy = false
                if (!state.account().equals(expected, true)) {
                    status("Account changed. Result discarded.")
                    return@runOnUiThread
                }
                outcome.onSuccess { value ->
                    when (value) {
                        is LockerUploadOutcome -> {
                            status(if (value.created) "Audio uploaded and checksum verified."
                                else "Matching uploaded song already exists.")
                            // A later explicit refresh lists remote files.
                        }
                        is CloudOfflineImportResult -> {
                            status(when {
                                !value.created ->
                                    "This audio is already saved offline; no duplicate was added."
                                value.originalIdPreserved ->
                                    "Downloaded and verified. Original song ID retained for LRC recovery."
                                else ->
                                    "Downloaded and verified. Saved as a new offline song."
                            })
                        }
                        is List<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val songs = value.filterIsInstance<LockerRemoteTrack>()
                            showSongs(songs)
                            status("${songs.size} cloud audio files found. Tap a track to download.")
                        }
                    }
                }.onFailure {
                    status("Music Drive action failed; local audio was not modified. " +
                        "Check connection and try again.")
                }
            }
        }
    }

    private fun showSongs(songs: List<LockerRemoteTrack>) {
        listView.removeAllViews()
        if (songs.isEmpty()) {
            listView.addView(text("No authorized uploads found.", 13f,
                0xFFABBCCE.toInt()))
        }
        songs.take(LockerAudioPolicy.MAX_VISIBLE).forEach { song ->
            listView.addView(text(song.name + " · " +
                ((song.bytes + 1023L) / 1024L) + " KB · Tap to download",
                13f, Color.WHITE).apply {
                setPadding(0, dp(8), 0, dp(8))
                setOnClickListener {
                    val verified = runCatching {
                        LockerAudioPolicy.requireDownloadable(song)
                    }.isSuccess
                    if (!verified) {
                        status("This cloud file has no verified checksums; download is blocked.")
                    } else {
                        AlertDialog.Builder(this@NightWaveLockerActivity)
                            .setTitle("Download to NightWave?")
                            .setMessage("Download ${song.name} to this phone (" +
                                ((song.bytes + 1023L) / 1024L) + " KB)? " +
                                "The file is verified before being added. " +
                                "Existing offline songs and lyrics are not overwritten.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Download to phone") { _, _ ->
                                begin("download", song)
                            }
                            .show()
                    }
                }
            })
        }
        if (songs.size > LockerAudioPolicy.MAX_VISIBLE)
            listView.addView(text("Additional uploads are not displayed in this first release.",
                12f, 0xFFABBCCE.toInt()))
    }

    private fun updateAccount() {
        accountView.text = state.account().takeIf { it.isNotBlank() }
            ?.let { "Connected to $it (music files)" } ?: "Music Drive is not connected"
    }
    private fun status(message: String) { if (::statusView.isInitialized) statusView.text = message }
    private fun text(content: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = content
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
    private fun button(label: String, action: () -> Unit) =
        TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(0xFF98E0D5.toInt())
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(7), dp(7), dp(7))
            minimumHeight = dp(47)
            setOnClickListener { action() }
        }
    private fun dp(v: Int) = (resources.displayMetrics.density * v + .5f).toInt()
    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }
}
