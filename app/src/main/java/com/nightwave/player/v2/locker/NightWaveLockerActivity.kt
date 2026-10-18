package com.nightwave.player.v2.locker

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
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
import java.text.DateFormat
import java.util.Date
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
    private lateinit var vaultSummary: TextView
    private lateinit var moreButton: TextView
    private lateinit var nextCloudPageButton: TextView
    private lateinit var sortButton: TextView
    private lateinit var filterButton: TextView
    private lateinit var transferView: TextView
    private var inventory = LockerCloudInventory(emptyList())
    private var searchQuery = ""
    private var activeFilter = LockerVaultFilter.ALL
    private var activeSort = LockerVaultSort.NAME
    private var shownCount = LockerVaultBrowsePolicy.PAGE_INCREMENT
    private lateinit var checkpoints: LockerDownloadCheckpointStore
    private lateinit var history: LockerTransferHistoryStore
    private lateinit var historyView: LinearLayout
    private lateinit var recoveryView: TextView
    private var historyExpanded = false
    private lateinit var resumeButton: TextView
    private lateinit var discardButton: TextView
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
            if (::transferView.isInitialized)
                transferView.text = "Authorization cancelled · No transfer started"
            status("Google permission was cancelled.")
        } else {
            runCatching {
                Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(result.data!!)
            }.onSuccess { authorized(it) }
                .onFailure {
                    busy = false
                    pendingAction = ""
                    if (::transferView.isInitialized)
                        transferView.text = "Authorization failed · No transfer started"
                    status("Google authorization failed. Try reconnecting.")
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        offline = OfflineAudioLibrary(this)
        state = LockerAccountState(this)
        checkpoints = LockerDownloadCheckpointStore(this)
        history = LockerTransferHistoryStore(this)
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
        resumeButton = button("Resume interrupted download") {
            if (busy) return@button
            val readiness = checkpoints.inspect(state.account())
            val saved = readiness.pending
            if (!readiness.canResume || saved == null) {
                status(readiness.explanation)
                updatePending()
                return@button
            }
            AlertDialog.Builder(this)
                .setTitle("Resume verified download?")
                .setMessage("Resume ${saved.remote.name} from " +
                    "${saved.committedBytes / 1024} KB of " +
                    "${saved.remote.bytes / 1024} KB? " +
                    readiness.explanation + " Google Drive will be reauthorized, " +
                    "and the file will be checked before entering your library.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Resume") { _, _ -> begin("download", saved.remote) }
                .show()
        }
        layout.addView(resumeButton)
        discardButton = button("Discard incomplete download") {
            if (busy) return@button
            val saved = checkpoints.read()
            if (saved == null && !checkpoints.needsRepair()) return@button
            AlertDialog.Builder(this)
                .setTitle("Discard incomplete download?")
                .setMessage(saved?.let {
                    "Remove only the temporary download of ${it.remote.name} " +
                        "from this phone. Drive audio and existing songs are untouched."
                } ?: "Discard an unreadable recovery record? Google Drive audio " +
                    "and saved songs are untouched. Unknown orphan temporary files " +
                    "will not be deleted automatically.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Discard") { _, _ ->
                    runCatching { checkpoints.discard() }
                        .onSuccess {
                            updatePending()
                            status("Temporary download discarded. Google Drive file preserved.")
                        }.onFailure {
                            updatePending()
                            status("Unable to discard the recovery record. No remote file changed.")
                        }
                }
                .show()
        }
        layout.addView(discardButton)
        recoveryView = text("", 12f, 0xFFFFC07D.toInt()).apply {
            setPadding(0, dp(3), 0, dp(5))
        }
        layout.addView(recoveryView)
        layout.addView(button("Disconnect this phone's music access") {
            if (busy) return@button
            if (state.disconnect()) {
                updateAccount()
                showInventory(LockerCloudInventory(emptyList()))
                status("Disconnected on this phone. Music files remain in Google Drive.")
            }
        })
        statusView = text("", 12f, 0xFFABC1D0.toInt()).apply {
            setPadding(0, dp(12), 0, dp(12))
        }
        layout.addView(statusView)
        transferView = text("No transfer active · One verified transfer at a time.",
            12f, 0xFF9DE4D8.toInt())
        layout.addView(transferView)
        vaultSummary = text("Cloud library not loaded yet.", 12f, 0xFFBAC8D4.toInt())
            .apply { setPadding(0, dp(8), 0, dp(6)) }
        layout.addView(vaultSummary)
        val search = EditText(this).apply {
            hint = "Search your listed cloud songs"
            setSingleLine(true)
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF8095A8.toInt())
            setBackgroundColor(0xFF172432.toInt())
            setPadding(dp(12), dp(6), dp(12), dp(6))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString().orEmpty()
                    shownCount = LockerVaultBrowsePolicy.PAGE_INCREMENT
                    if (::listView.isInitialized) renderVault()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        layout.addView(search, LinearLayout.LayoutParams(-1, dp(44)))
        val browseControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        filterButton = button("Filter: All ▾") {
            val names = arrayOf("All listed", "Verified downloads", "Needs attention")
            AlertDialog.Builder(this)
                .setTitle("Cloud library filter")
                .setSingleChoiceItems(names, activeFilter.ordinal) { dialog, which ->
                    activeFilter = LockerVaultFilter.values()[which]
                    shownCount = LockerVaultBrowsePolicy.PAGE_INCREMENT
                    dialog.dismiss()
                    renderVault()
                }.setNegativeButton("Cancel", null).show()
        }
        browseControls.addView(filterButton, LinearLayout.LayoutParams(0, dp(45), 1f))
        sortButton = button("Sort: Name ▾") {
            val names = arrayOf("Name A–Z", "Largest first", "Smallest first")
            AlertDialog.Builder(this)
                .setTitle("Sort cloud songs")
                .setSingleChoiceItems(names, activeSort.ordinal) { dialog, which ->
                    activeSort = LockerVaultSort.values()[which]
                    shownCount = LockerVaultBrowsePolicy.PAGE_INCREMENT
                    dialog.dismiss()
                    renderVault()
                }.setNegativeButton("Cancel", null).show()
        }
        browseControls.addView(sortButton, LinearLayout.LayoutParams(0, dp(45), 1f))
        layout.addView(browseControls)
        listView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        historyView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scrollItems = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollItems.addView(text("RECENT TRANSFERS · On this device only",
            12f, 0xFF9DE4D8.toInt(), true).apply {
            setPadding(0, dp(12), 0, dp(5))
        })
        scrollItems.addView(historyView)
        scrollItems.addView(button("Clear local transfer history") {
            if (busy) {
                status("Wait for the current transfer to finish before clearing history.")
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Clear local transfer history?")
                    .setMessage("This clears only the local transfer journal. " +
                        "No Google Drive audio, offline songs, or resumable " +
                        "download checkpoints will be deleted.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear history") { _, _ ->
                        runCatching { history.clear() }
                            .onSuccess {
                                historyExpanded = false
                                renderTransferHistory()
                                status("Local transfer history cleared; music and checkpoints preserved.")
                            }
                            .onFailure { status("Could not clear the local transfer history.") }
                    }.show()
            }
        })
        scrollItems.addView(text("YOUR LISTED CLOUD MUSIC", 12f,
            0xFF9DE4D8.toInt(), true).apply {
            setPadding(0, dp(14), 0, dp(5))
        })
        scrollItems.addView(listView)
        layout.addView(ScrollView(this).apply { addView(scrollItems) },
            LinearLayout.LayoutParams(-1, 0, 1f))
        moreButton = button("Show more listed songs") {
            shownCount = (shownCount + LockerVaultBrowsePolicy.PAGE_INCREMENT)
                .coerceAtMost(LockerVaultBrowsePolicy.MAX_TOTAL_PAGES *
                    LockerVaultBrowsePolicy.PAGE_SIZE)
            renderVault()
        }
        layout.addView(moreButton)
        nextCloudPageButton = button("Load next cloud page") {
            if (busy) {
                status("Wait for the current transfer or Drive request to finish.")
            } else if (!LockerVaultBrowsePolicy.canLoadMore(inventory)) {
                status("No further safe continuation is available. Refresh to start a new scan.")
            } else begin("more")
        }
        layout.addView(nextCloudPageButton)
        layout.addView(text("Upload only with permission. Tap a cloud track to review and " +
            "download it. Downloaded files are checked before entering Offline Library.",
            12f, 0xFF8AA0B3.toInt()))
        setContentView(layout)
        updateAccount()
        updatePending()
        renderTransferHistory()
        status("Connect Google Drive, then choose a manual upload or browse your cloud library.")
    }

    private fun begin(action: String, download: LockerRemoteTrack? = null) {
        if (busy) return
        if (action == "upload" && selected == null) {
            status("No local audio selected for upload.")
            return
        }
        if (action == "download" && download == null) return
        if (action == "more" && !LockerVaultBrowsePolicy.canLoadMore(inventory)) {
            status("Cloud cursor is unavailable. Refresh to browse again.")
            return
        }
        busy = true
        pendingAction = action
        pendingDownload = download
        status("Requesting limited Google Drive music permission…")
        if (action == "upload" || action == "download") {
            transferView.text = "Preparing verified " + action + "…"
        }
        Identity.getAuthorizationClient(this)
            .authorize(NightWaveLockerAuthorization.request(state.account().takeIf { it.isNotBlank() }))
            .addOnSuccessListener { response ->
                if (response.hasResolution()) {
                    val intent = response.pendingIntent
                    if (intent == null) {
                        busy = false
                        pendingAction = ""
                        transferView.text = "Authorization unavailable · No transfer started"
                        status("Google could not open the permission screen.")
                    } else runCatching {
                        resolution.launch(IntentSenderRequest.Builder(intent.intentSender).build())
                    }.onFailure {
                        busy = false
                        pendingAction = ""
                        transferView.text = "Authorization unavailable · No transfer started"
                        status("Unable to open Google's music permission screen.")
                    }
                } else authorized(response)
            }
            .addOnFailureListener {
                busy = false
                pendingAction = ""
                transferView.text = "Permission denied · No transfer started"
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
            transferView.text = "Account mismatch · No transfer started"
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
            // The journal is optional; failure to write a history row must
            // not abort a user-authorized transfer or decide its outcome.
            val journalId = runCatching {
                when (action) {
                    "upload" -> {
                        val song = requireNotNull(selected)
                        history.start(LockerTransferKind.UPLOAD, song.title, song.bytes)
                    }
                    "download" -> {
                        val track = requireNotNull(chosenRemote)
                        history.start(LockerTransferKind.DOWNLOAD, track.name, track.bytes)
                    }
                    else -> null
                }
            }.getOrNull()
            val outcome = runCatching {
                val client = NightWaveMusicDriveClient(token)
                if (action == "upload") {
                    val song = requireNotNull(selected)
                    val file = requireNotNull(offline.fileFor(song)) {
                        "Selected local audio is unavailable"
                    }
                    client.upload(song, file) { percent ->
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                status("Uploading authorized audio… $percent%")
                                transferView.text = "Upload · $percent% (verification required)"
                            }
                        }
                    }
                } else if (action == "download") {
                    val track = requireNotNull(chosenRemote)
                    val staging = client.downloadWithRecovery(
                        track, File(filesDir, "locker_staging"),
                        expected, checkpoints
                    ) { percent ->
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                status("Downloading and verifying audio… $percent%")
                                transferView.text = "Download · $percent% (verification required)"
                            }
                        }
                    }
                    try {
                        val imported = offline.importVerifiedCloud(staging, track)
                        checkpoints.discard()
                        imported
                    } catch (error: Exception) {
                        // If import moved the staging file but index commit
                        // failed, its old checkpoint must not be replayed.
                        if (!staging.exists()) checkpoints.discard()
                        throw error
                    }
                } else if (action == "more") client.inventoryMore(inventory)
                else client.inventory()
            }
            if (journalId != null) {
                val resultState = when (val value = outcome.getOrNull()) {
                    is LockerUploadOutcome -> if (value.created)
                        LockerTransferState.COMPLETED else LockerTransferState.REUSED
                    is CloudOfflineImportResult -> if (value.created)
                        LockerTransferState.COMPLETED else LockerTransferState.REUSED
                    else -> LockerTransferState.NOT_VERIFIED
                }
                // No account, token, remote ID or exception detail is written.
                runCatching { history.finish(journalId, resultState) }
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                busy = false
                updatePending()
                renderTransferHistory()
                if (!state.account().equals(expected, true)) {
                    status("Account changed. Result discarded.")
                    return@runOnUiThread
                }
                outcome.onSuccess { value ->
                    when (value) {
                        is LockerUploadOutcome -> {
                            transferView.text = if (value.created)
                                "Upload complete · Google Drive verified"
                            else "Existing verified cloud copy reused"
                            status(if (value.created) "Audio uploaded and checksum verified."
                                else "Matching uploaded song already exists.")
                            // A later explicit refresh lists remote files.
                        }
                        is CloudOfflineImportResult -> {
                            transferView.text = "Download complete · Saved audio verified"
                            status(when {
                                !value.created ->
                                    "This audio is already saved offline; no duplicate was added."
                                value.originalIdPreserved ->
                                    "Downloaded and verified. Original song ID retained for LRC recovery."
                                else ->
                                    "Downloaded and verified. Saved as a new offline song."
                            })
                        }
                        is LockerCloudInventory -> {
                            if (action == "more") {
                                // Keep user's search/filter/show-more state.
                                inventory = value
                                renderVault()
                            } else showInventory(value)
                            status(if (value.hasMore)
                                "${value.tracks.size} files listed. More cloud files exist beyond this scan."
                            else "${value.tracks.size} cloud audio files listed. Choose a verified song.")
                        }
                    }
                }.onFailure {
                    if (action == "upload" || action == "download") {
                        transferView.text = "Transfer interrupted or unverified · No success recorded"
                    }
                    if (action == "download" && checkpoints.read() != null) {
                        status("Download interrupted. Resume after checking your connection, " +
                            "or discard the temporary copy. Saved songs are unchanged.")
                    } else if (action == "upload") {
                        status("Upload could not be verified. No local song changed. " +
                            "Retry explicitly: NightWave first checks for a matching " +
                            "Drive copy before starting another session.")
                    } else if (action == "list" || action == "more") {
                        status("Cloud browsing request failed. Previously listed songs, if shown, " +
                            "may be stale; your music was not modified. Retry explicitly.")
                    } else {
                        status("Music Drive action failed; local audio was not modified. " +
                            "Check connection and try again.")
                    }
                }
            }
        }
    }

    private fun showInventory(loaded: LockerCloudInventory) {
        inventory = loaded
        shownCount = LockerVaultBrowsePolicy.PAGE_INCREMENT
        renderVault()
    }

    /** All browsing is on the already received metadata; no Drive search calls. */
    private fun renderVault() {
        if (!::listView.isInitialized || !::vaultSummary.isInitialized ||
            !::moreButton.isInitialized || !::nextCloudPageButton.isInitialized) return
        val summary = LockerVaultBrowsePolicy.summary(inventory)
        val matching = LockerVaultBrowsePolicy.browse(
            inventory, searchQuery, activeFilter, activeSort)
        val shown = LockerVaultBrowsePolicy.visible(matching, shownCount)
        val warning = if (summary.partialListing)
            " · Partial Drive scan: more files may exist"
        else ""
        vaultSummary.text = "${summary.listedCount} listed · " +
            "${summary.verifiedCount} verified · " +
            "${summary.needsAttentionCount} need attention · " +
            LockerVaultBrowsePolicy.bytesLabel(summary.listedBytes) +
            " in listed NightWave music" + warning + "\n" +
            "${matching.size} match current filters; showing ${shown.size}. " +
            "Scanned ${inventory.pagesRead} Drive page(s). " +
            "This is not your total Google Drive usage." + "\n" +
            "Verified files: " + LockerVaultBrowsePolicy.bytesLabel(summary.verifiedBytes) +
            " · Needs attention: " +
            LockerVaultBrowsePolicy.bytesLabel(summary.needsAttentionBytes) +
            " · Formats: " + summary.formatCounts.joinToString(", ") {
                "${it.first} ${it.second}"
            }
        filterButton.text = when (activeFilter) {
            LockerVaultFilter.ALL -> "Filter: All ▾"
            LockerVaultFilter.VERIFIED -> "Filter: Verified ▾"
            LockerVaultFilter.NEEDS_ATTENTION -> "Filter: Attention ▾"
        }
        sortButton.text = when (activeSort) {
            LockerVaultSort.NAME -> "Sort: Name ▾"
            LockerVaultSort.LARGEST -> "Sort: Largest ▾"
            LockerVaultSort.SMALLEST -> "Sort: Smallest ▾"
        }
        moreButton.visibility = if (matching.size > shown.size) View.VISIBLE else View.GONE
        moreButton.text = "Show more · ${matching.size - shown.size} listed matches remaining"
        val moreFromDrive = LockerVaultBrowsePolicy.canLoadMore(inventory)
        nextCloudPageButton.visibility = if (moreFromDrive) View.VISIBLE else View.GONE
        nextCloudPageButton.text =
            "Load next Drive batch · ${inventory.pagesRead}/${LockerVaultBrowsePolicy.MAX_TOTAL_PAGES} pages"
        if (inventory.hasMore && !moreFromDrive) {
            status("Cloud inventory reached its safe scan limit. " +
                "Listed totals are partial; no music has been changed.")
        }
        listView.removeAllViews()
        if (matching.isEmpty()) {
            listView.addView(text(
                if (inventory.tracks.isEmpty())
                    "No music files listed. Connect and tap Refresh to browse."
                else "No matching songs in this Drive scan. Try another search or filter.",
                13f, 0xFFABBCCE.toInt()
            ).apply { setPadding(0, dp(12), 0, dp(12)) })
            return
        }
        shown.forEach { song ->
            val verified = LockerVaultBrowsePolicy.downloadable(song)
            val info = if (verified) "Verified download available"
                else "Missing verification metadata · Download blocked"
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(9), dp(10), dp(9))
                setBackgroundColor(0xFF172432.toInt())
            }
            row.addView(text(song.name, 14f, Color.WHITE, true).apply {
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            row.addView(text(
                LockerVaultBrowsePolicy.bytesLabel(song.bytes) + " · " + info,
                12f, if (verified) 0xFF9DE4D8.toInt()
                    else 0xFFFFC07D.toInt()
            ).apply { maxLines = 2 })
            row.setOnClickListener {
                if (busy) {
                    status("Wait for the current Drive action to finish.")
                } else if (!verified) {
                    status("This cloud file lacks verified checksums; download is blocked.")
                } else {
                    // Existing Drive client independently rechecks current
                    // metadata, folder membership and both hashes.
                    if (runCatching { LockerAudioPolicy.requireDownloadable(song) }.isFailure) {
                        status("This cloud file has no verified checksums; download is blocked.")
                    } else {
                        AlertDialog.Builder(this@NightWaveLockerActivity)
                            .setTitle("Download to NightWave?")
                            .setMessage("Download ${song.name} to this phone (" +
                                LockerVaultBrowsePolicy.bytesLabel(song.bytes) + ")? " +
                                "The file is verified before being added. " +
                                "Existing offline songs and lyrics are not overwritten.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Download to phone") { _, _ ->
                                begin("download", song)
                            }.show()
                    }
                }
            }
            listView.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(7)
            })
        }
    }

    private fun updatePending() {
        if (!::resumeButton.isInitialized || !::discardButton.isInitialized ||
            !::recoveryView.isInitialized) return
        val preview = checkpoints.inspect(state.account())
        resumeButton.visibility = if (preview.canResume) View.VISIBLE else View.GONE
        discardButton.visibility = if (preview.state == LockerCheckpointState.NONE)
            View.GONE else View.VISIBLE
        discardButton.text = if (preview.state == LockerCheckpointState.NEEDS_REPAIR)
            "Discard damaged download checkpoint" else "Discard incomplete download"
        resumeButton.text = preview.pending?.takeIf { preview.canResume }?.let {
            "Resume: ${it.remote.name} (${LockerResumePolicy.percent(it.committedBytes, it.remote.bytes)}%)"
        } ?: "Resume interrupted download"
        recoveryView.visibility = if (preview.state == LockerCheckpointState.NONE)
            View.GONE else View.VISIBLE
        recoveryView.text = preview.explanation
    }

    private fun renderTransferHistory() {
        if (!::historyView.isInitialized) return
        historyView.removeAllViews()
        val snapshot = history.read()
        if (snapshot.unreadable) {
            historyView.addView(text(
                "Local transfer journal unreadable. No records were overwritten. " +
                    "You can clear only this history using the control below.",
                12f, 0xFFFFC07D.toInt()))
            return
        }
        if (snapshot.entries.isEmpty()) {
            historyView.addView(text("No transfers recorded on this device.",
                12f, 0xFF94A9BB.toInt()))
            return
        }
        val display = if (historyExpanded) snapshot.entries else snapshot.entries.take(5)
        display.forEach { event ->
            val heading = (if (event.kind == LockerTransferKind.UPLOAD)
                "↑ Upload · " else "↓ Download · ") + event.title
            val date = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT).format(Date(event.timeMs))
            val detail = LockerVaultBrowsePolicy.bytesLabel(event.bytes) + " · " +
                LockerTransferHistoryPolicy.description(event.state) + " · " + date
            historyView.addView(text(heading, 13f, Color.WHITE, true).apply {
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(dp(8), dp(6), dp(8), dp(0))
            })
            historyView.addView(text(detail, 12f,
                if (event.state == LockerTransferState.COMPLETED ||
                    event.state == LockerTransferState.REUSED) 0xFF9DE4D8.toInt()
                else 0xFFFFC07D.toInt()).apply {
                setPadding(dp(8), 0, dp(8), dp(6))
                maxLines = 2
            })
        }
        if (snapshot.entries.size > 5) {
            historyView.addView(button(if (historyExpanded) "Show recent five"
                else "Show all ${snapshot.entries.size} transfers") {
                historyExpanded = !historyExpanded
                renderTransferHistory()
            })
        }
    }

    override fun onResume() {
        super.onResume()
        if (::historyView.isInitialized) {
            updatePending()
            renderTransferHistory()
        }
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
