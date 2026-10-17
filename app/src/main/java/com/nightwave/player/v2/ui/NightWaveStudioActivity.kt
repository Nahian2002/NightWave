package com.nightwave.player.v2.ui

import android.app.AlertDialog
import android.app.Dialog
import android.widget.EditText
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.nightwave.player.v2.lyrics.PrivateLyricsStore
import com.nightwave.player.v2.lyrics.TimedLyrics
import com.nightwave.player.v2.lyrics.TimedLyricsCodec
import com.nightwave.player.v2.insights.ListeningInsights
import com.nightwave.player.v2.insights.ListeningInsightsEngine
import com.nightwave.player.v2.insights.ListeningMemoryStore
import com.nightwave.player.v2.insights.ListeningMemorySummary
import com.nightwave.player.v2.cloud.CloudBackupStatus
import com.nightwave.player.v2.cloud.CloudBackupState
import com.nightwave.player.v2.cloud.CloudBackupCodec
import com.nightwave.player.v2.cloud.CloudBackupPreview
import com.nightwave.player.v2.cloud.NightWaveDriveClient
import com.nightwave.player.v2.cloud.DriveSnapshot
import com.nightwave.player.v2.cloud.CloudLibraryMerger
import com.nightwave.player.v2.cloud.CloudMergePlan
import com.nightwave.player.v2.data.CloudMergeApplyResult
import com.nightwave.player.v2.cloud.NightWaveCloudAuthorization
import com.nightwave.player.v2.cloud.NightWaveCloudSchedule
import com.nightwave.player.v2.data.LibraryArchiveManager
import com.nightwave.player.diagnostics.NightWaveCrashRecorder
import com.nightwave.player.diagnostics.NightWaveExitHistory
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import com.nightwave.player.playback.SleepTimerStatusStore
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.offline.OfflineLibraryActivity
import com.nightwave.player.offline.OfflineAudioLibrary
import com.nightwave.player.offline.OfflineSessionStore
import com.nightwave.player.playback.PlaybackService
import com.nightwave.player.library.LegacyLibraryTransaction
import com.nightwave.player.core.VideoLink
import com.nightwave.player.core.SearchInput
import com.nightwave.player.core.SearchInputResolver
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.net.YouTubeGateway
import com.nightwave.player.net.VideoSearchResults
import com.nightwave.player.net.SearchMode
import com.nightwave.player.net.SavedTrackSearch
import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.ui.MainActivity
import com.nightwave.player.ui.YouTubeSearchBrowser
import com.nightwave.player.v2.data.GuestProfileStore
import com.nightwave.player.v2.data.ProfileDataStoreMirror
import com.nightwave.player.v2.data.room.NightWaveDatabase
import com.nightwave.player.v2.data.room.RoomShadowMigrator
import com.nightwave.player.v2.data.room.VerifiedRoomReader
import com.nightwave.player.v2.playback.StudioPlaybackBridge
import com.nightwave.player.v2.playback.StudioPlayerState
import com.nightwave.player.v2.playback.SpotlightResumePolicy
import com.nightwave.player.v2.playback.SpotlightAction
import com.nightwave.player.v2.recommendation.PersonalMixEngine
import com.nightwave.player.v2.recommendation.PersonalMix
import com.nightwave.player.v2.social.PlaylistShareFormatter
import com.nightwave.player.v2.model.MotionPreference
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.model.ProfileSettings

/** Native result rows are an enhancement; the official browser remains a fallback. */
private data class StudioSearchSnapshot(
    val query: String = "",
    val mode: SearchMode = SearchMode.SONGS,
    val loading: Boolean = false,
    val videos: List<VideoPreview> = emptyList(),
    val provider: String = "",
    val error: String = "",
    val hint: String = ""
)

/**
 * Default NightWave 2.0 application surface. The original UI remains an
 * explicit, reversible fallback in Profile and keeps the same local library.
 */
class NightWaveStudioActivity : ComponentActivity() {
    private lateinit var profiles: GuestProfileStore
    private lateinit var library: MusicLibrary
    private lateinit var playback: StudioPlaybackBridge
    private lateinit var sleepTimerStatus: SleepTimerStatusStore
    private var sleepRemainingMs by mutableStateOf(0L)
    private lateinit var archiveManager: LibraryArchiveManager
    private lateinit var lyricsStore: PrivateLyricsStore
    private lateinit var listeningMemoryStore: ListeningMemoryStore
    private var memorySummary by mutableStateOf(
        ListeningMemorySummary(false, 0L, 0, 0, emptyList())
    )
    private var memoryReadGeneration = 0
    private var pendingLyricsId: String? = null
    private var lyricsRevision by mutableStateOf(0)
    private var loadedLyrics by mutableStateOf<Pair<String, TimedLyrics>?>(null)
    private lateinit var cloudStore: CloudBackupState
    private var cloudInfo by mutableStateOf(CloudBackupStatus())
    private var cloudBusy by mutableStateOf(false)
    private var cloudPendingAction = ""
    private lateinit var roomShadow: RoomShadowMigrator
    private lateinit var verifiedReader: VerifiedRoomReader
    private var roomPreviewRequested = false
    private var roomPreviewActive by mutableStateOf(false)
    private var roomReadStatus by mutableStateOf("Legacy library reads are active")
    private var roomPreviewPlaylists by mutableStateOf<Map<String, List<VideoPreview>>>(emptyMap())
    private lateinit var profileShadow: ProfileDataStoreMirror
    private var shadowStatus by mutableStateOf("Legacy storage active · Room/DataStore migration pending")
    private val nativeGateway = YouTubeGateway()
    private var nativeSearchState by mutableStateOf(StudioSearchSnapshot())
    private var nativeSearchJob: Job? = null
    private var nativeSearchGeneration = 0
    private var searchNavigationToken by mutableStateOf(0)
    private var shadowRunning = false
    private var shadowRequested = false
    private var youtubeDialog: Dialog? = null
    private val cloudResolution = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) {
            cloudBusy = false
            cloudPendingAction = ""
            cloudStore.status("Google authorization cancelled")
            refreshCloud()
        } else {
            runCatching {
                Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data!!)
            }.onSuccess { finishCloudAuthorization(it) }
                .onFailure {
                    cloudBusy = false
                    cloudPendingAction = ""
                    cloudStore.status("Google authorization was not completed")
                    refreshCloud()
                }
        }
    }

    private val importLyricsFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val id = pendingLyricsId
        pendingLyricsId = null
        if (uri == null || id == null) return@registerForActivityResult
        if (playback.state.videoUrl != "offline:$id") {
            showMessage("Song changed; import lyrics again for the correct track.")
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    requireNotNull(contentResolver.openInputStream(uri)) {
                        "Could not read the selected lyrics file"
                    }.use { stream ->
                        val data = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            require(data.size() + count <= TimedLyricsCodec.MAX_BYTES) {
                                "Lyrics exceed the 64 KB import limit"
                            }
                            data.write(buffer, 0, count)
                        }
                        // Reject a stale selector without associating it with
                        // any other track or persisting arbitrary file contents.
                        check(playback.state.videoUrl == "offline:$id") {
                            "The currently playing track changed"
                        }
                        lyricsStore.save(id, data.toByteArray())
                    }
                }
            }
            outcome.onSuccess {
                lyricsRevision++
                showMessage("Synchronized lyrics imported for this local song.")
            }.onFailure {
                showMessage("Lyrics import unsuccessful: " +
                    (it.message ?: "Invalid synchronized LRC file"))
            }
        }
    }

    private val exportFile = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            val data = archiveManager.export()
            requireNotNull(contentResolver.openOutputStream(uri)) { "Can't open destination" }
                .use { it.write(data) }
        }.onSuccess { showMessage("Guest backup exported successfully.") }
            .onFailure { showMessage("Export failed: " + (it.message ?: "unknown error")) }
    }
    private val importFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            requireNotNull(contentResolver.openInputStream(uri)).use { input ->
                val limit = com.nightwave.player.v2.data.LibraryBackupCodec.MAX_BYTES
                val buffer = ByteArray(8192)
                val out = java.io.ByteArrayOutputStream()
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(out.size() + count <= limit) { "Backup exceeds the size limit" }
                    out.write(buffer, 0, count)
                }
                archiveManager.preview(out.toByteArray())
            }
        }.onSuccess { archive ->
            AlertDialog.Builder(this)
                .setTitle("Replace your local library?")
                .setMessage(
                    "Backup contains " + archive.favorites.size + " liked songs and " +
                    archive.playlists.size + " playlists. This will replace your current favorites " +
                    "and playlists. Your local listening history stays on this phone. " +
                    "Export your existing library first if you want to keep it."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Replace library") { _, _ ->
                    if (archiveManager.restoreWithConfirmation(archive)) {
                        profile = profiles.load()
                        refresh()
                        showMessage("Guest library restored.")
                    } else showMessage("Import could not complete. Your previous library is retained.")
                }.show()
        }.onFailure { showMessage("Invalid backup: " + (it.message ?: "cannot read file")) }
    }
    private var profile by mutableStateOf(ProfileSettings(localId = "pending"))
    private var favorites by mutableStateOf<List<VideoPreview>>(emptyList())
    private var recentlyPlayed by mutableStateOf<List<VideoPreview>>(emptyList())
    /** Invalidates cached playlist recommendations after edits without renaming playlists. */
    private var libraryRevision by mutableStateOf(0)
    private var offlineResumeTitle by mutableStateOf<String?>(null)
    private var offlineResumePositionMs by mutableStateOf(0L)
    private var names by mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profiles = GuestProfileStore(this)
        library = MusicLibrary(this)
        playback = StudioPlaybackBridge(this)
        sleepTimerStatus = SleepTimerStatusStore(this)
        archiveManager = LibraryArchiveManager(this)
        lyricsStore = PrivateLyricsStore(this)
        listeningMemoryStore = ListeningMemoryStore(this)
        refreshListeningMemory()
        cloudStore = CloudBackupState(this)
        refreshCloud()
        // Room/DataStore are read-only previews at this phase. A failure
        // opening the optional shadow must not take down the classic library
        // or the premium player on app launch.
        try {
            val database = NightWaveDatabase.get(this)
            roomShadow = RoomShadowMigrator(this, database)
            verifiedReader = VerifiedRoomReader(this, database)
            profileShadow = ProfileDataStoreMirror(this)
        } catch (error: Exception) {
            shadowStatus = "Optional storage preview unavailable; original library retained."
            roomReadStatus = "Original library reads remain active."
        }
        profile = profiles.load()
        refresh()
        setContent {
            // Display only. The media service remains the single timer owner,
            // including when this Activity is no longer visible.
            LaunchedEffect(Unit) {
                while (true) {
                    sleepRemainingMs = sleepTimerStatus.remainingMs()
                    delay(5_000L)
                }
            }
            LaunchedEffect(playback.state.videoUrl) {
                if (playback.state.videoUrl.isNotBlank()) refresh()
            }
            LaunchedEffect(playback.state.videoUrl, lyricsRevision) {
                loadedLyrics = null
                val current = playback.state.videoUrl
                if (current.startsWith("offline:")) {
                    val id = current.removePrefix("offline:")
                    val result = withContext(Dispatchers.IO) {
                        runCatching { lyricsStore.read(id) }.getOrNull()
                    }
                    if (playback.state.videoUrl == current && result != null) {
                        loadedLyrics = id to result
                    }
                }
            }
            NightWaveMaterialTheme(profile.theme) {
                StudioRoot(
                    profile = profile, favorites = favorites, recent = recentlyPlayed,
                    libraryRevision = libraryRevision,
                    playlistNames = names, getPlaylist = { name ->
                        if (roomPreviewActive) roomPreviewPlaylists[name] ?: emptyList()
                        else library.playlist(name)
                    },
                    playerState = playback.state, playerActions = playback,
                    sleepRemainingMs = sleepRemainingMs,
                    onSleepTimer = { minutes ->
                        runCatching {
                            startService(Intent(this, PlaybackService::class.java).apply {
                                action = if (minutes == null)
                                    PlaybackService.ACTION_SLEEP_TIMER_CANCEL
                                else PlaybackService.ACTION_SLEEP_TIMER_SET
                                if (minutes != null)
                                    putExtra(PlaybackService.EXTRA_SLEEP_MINUTES, minutes)
                            })
                        }.onSuccess {
                            lifecycleScope.launch {
                                delay(300L)
                                sleepRemainingMs = sleepTimerStatus.remainingMs()
                            }
                        }.onFailure {
                            showMessage("Sleep timer couldn't be updated. Try again.")
                        }
                    },
                    memory = memorySummary,
                    onMemoryEnabledChange = { setListeningMemory(it) },
                    onMemoryErase = { confirmEraseListeningMemory() },
                    onMemoryRefresh = { refreshListeningMemory() },
                    currentLyrics = loadedLyrics?.takeIf {
                        playback.state.videoUrl == "offline:" + it.first
                    }?.second,
                    onImportLyrics = {
                        val id = playback.state.videoUrl.takeIf {
                            it.startsWith("offline:")
                        }?.removePrefix("offline:")
                        if (id != null) {
                            pendingLyricsId = id
                            importLyricsFile.launch(arrayOf("text/*", "application/octet-stream"))
                        }
                    },
                    onRemoveLyrics = {
                        val id = playback.state.videoUrl.takeIf {
                            it.startsWith("offline:")
                        }?.removePrefix("offline:")
                        if (id != null) lifecycleScope.launch {
                            val done = withContext(Dispatchers.IO) {
                                runCatching { lyricsStore.delete(id) }.getOrDefault(false)
                            }
                            if (done) {
                                lyricsRevision++
                                showMessage("Private lyrics removed from this device.")
                            } else showMessage("Lyrics could not be removed.")
                        }
                    },
                    offlineResumeTitle = offlineResumeTitle,
                    offlineResumePositionMs = offlineResumePositionMs,
                    onResumeOffline = {
                        runCatching {
                            startService(Intent(this, PlaybackService::class.java)
                                .setAction(PlaybackService.ACTION_RESUME_OFFLINE))
                        }.onFailure {
                            showMessage("Offline music could not resume. Open the Offline library.")
                        }
                    },
                    isFavorite = { url -> library.isFavorite(url) },
                    onToggleFavorite = { video -> library.toggleFavorite(video); refresh() },
                    onSongOptions = { video -> showSongOptions(video) },
                    onSharePlaylist = { name, tracks -> sharePlaylist(name, tracks) },
                    onSaveCurrentToPlaylist = { saveCurrentToPlaylist() },
                    storageStatus = shadowStatus,
                    onCheckStorage = { refreshShadows() },
                    roomReadStatus = roomReadStatus,
                    roomPreviewActive = roomPreviewActive,
                    onEnableRoomPreview = { enableVerifiedRoomPreview() },
                    onUseLegacyReads = { disableRoomPreview() },
                    onExportGuest = { exportFile.launch("nightwave-guest-backup.json") },
                    onImportGuest = { importFile.launch(arrayOf("application/json", "application/octet-stream")) },
                    onRecoverLastChange = { confirmLastWriteRecovery() },
                    cloud = cloudInfo,
                    cloudBusy = cloudBusy,
                    onCloudConnect = { requestCloudAction("connect") },
                    onCloudBackup = { requestCloudAction("backup") },
                    onCloudPreviewRestore = { requestCloudAction("preview") },
                    onCloudCompare = { requestCloudAction("merge") },
                    onCloudDisconnect = { disconnectCloud() },
                    onCloudHistory = { enabled ->
                        if (cloudStore.setHistory(enabled)) refreshCloud()
                    },
                    onCloudWifi = { enabled ->
                        if (cloudStore.setWifiOnly(enabled)) {
                            refreshCloud()
                            runCatching { NightWaveCloudSchedule.update(this) }
                                .onFailure { cloudStore.status("Could not update backup schedule") }
                        }
                    },
                    onSaveProfile = { updated ->
                        if (profiles.save(updated)) {
                            profile = profiles.load()
                            refreshShadows()
                        }
                    },
                    searchState = nativeSearchState,
                    onSearch = { query, mode -> nativeSearch(query, mode) },
                    onPrimarySearch = { query, mode -> primarySearch(query, mode) },
                    onPrepareSearch = { query, mode -> prepareSearch(query, mode) },
                    onWebSearch = { query -> openOfficialSearch(query) },
                    onPlay = { video, collection -> playback.play(video, collection) },
                    onLegacyPlayer = { manageLibrary() },
                    onOfflineLibrary = { startActivity(Intent(this, OfflineLibraryActivity::class.java)) },
                    onShareDiagnostics = { showCrashSummaryDialog() },
                    onOpenClassic = {
                        startActivity(Intent(this, MainActivity::class.java))
                    },
                    searchNavigationToken = searchNavigationToken
                )
            }
        }
        playback.connect()
        // Rotation must not repeat external share playback or web searches.
        if (savedInstanceState == null) handleIncomingShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShare(intent)
    }

    /**
     * The premium Activity owns ACTION_SEND now that it is the launcher.
     * Accept bounded plain text only; never treat arbitrary external links as
     * media, and don't replay an old share during a configuration change.
     */
    private fun handleIncomingShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val shared = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim().take(160)
        if (shared.isBlank()) return
        searchNavigationToken++
        val watchUrl = VideoLink.normalize(shared)
        if (watchUrl != null) {
            playback.play(VideoPreview("Shared track", "YouTube", watchUrl, "", 0L))
        } else {
            prepareSearch(shared, SearchMode.SONGS)
        }
    }

    /**
     * Official YouTube discovery is the primary route. Public scrapers are
     * optional quick results and must not block the primary search action.
     */
    private fun primarySearch(query: String, mode: SearchMode) {
        val clean = query.trim().take(160)
        when (SearchInputResolver.resolve(clean)) {
            SearchInput.Blank -> return
            is SearchInput.VideoUrl, SearchInput.BadLink -> nativeSearch(clean, mode)
            is SearchInput.Keywords -> {
                prepareSearch(clean, mode)
                openOfficialSearch(clean)
            }
        }
    }

    /** Home shortcuts prefill without starting an unreliable provider call. */
    private fun prepareSearch(query: String, mode: SearchMode) {
        val clean = query.trim().take(160)
        if (clean.isBlank()) return
        nativeSearchJob?.cancel()
        nativeSearchGeneration++
        nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode)
    }

    private fun openOfficialSearch(query: String) {
        val clean = query.trim().take(160)
        if (clean.isBlank()) return
        youtubeDialog?.dismiss()
        youtubeDialog = YouTubeSearchBrowser(this).show(clean) { url ->
            playback.play(VideoPreview("Loading track…", "YouTube", url, "", 0L))
        }
    }

    private fun refreshCloud() {
        if (::cloudStore.isInitialized) cloudInfo = cloudStore.read()
    }

    private fun requestCloudAction(action: String) {
        if (cloudBusy) return
        if (action != "connect" && cloudStore.read().account.isBlank()) {
            showMessage("Connect Google Drive first.")
            return
        }
        cloudPendingAction = action
        cloudBusy = true
        val name = if (action == "connect") null else cloudStore.read().account
        Identity.getAuthorizationClient(this)
            .authorize(NightWaveCloudAuthorization.request(name))
            .addOnSuccessListener { response ->
                if (response.hasResolution()) {
                    val pending = response.pendingIntent
                    if (pending == null) {
                        cloudBusy = false
                        cloudPendingAction = ""
                        showMessage("Google could not open account authorization.")
                    } else {
                        runCatching {
                            cloudResolution.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                        }.onFailure {
                            cloudBusy = false
                            cloudPendingAction = ""
                            cloudStore.status("Google permission dialog unavailable")
                            refreshCloud()
                        }
                    }
                } else finishCloudAuthorization(response)
            }
            .addOnFailureListener {
                cloudBusy = false
                cloudPendingAction = ""
                cloudStore.status("Could not authorize Google Drive")
                refreshCloud()
                showMessage("Google authorization unavailable. Check Google Play services.")
            }
    }

    private fun finishCloudAuthorization(result: AuthorizationResult) {
        val action = cloudPendingAction
        cloudPendingAction = ""
        val account = NightWaveCloudAuthorization.authorizedAccount(result)
        val token = runCatching { NightWaveCloudAuthorization.requireToken(result) }.getOrNull()
        if (account.isNullOrBlank() || token == null) {
            cloudBusy = false
            cloudStore.status("Google account or Drive permission could not be verified")
            refreshCloud()
            return
        }
        if (action == "connect") {
            cloudBusy = false
            if (cloudStore.connect(account)) {
                runCatching { NightWaveCloudSchedule.stop(this) }
                refreshCloud()
                showMessage("Drive connected. Choose Back up now or Preview restore.")
            } else showMessage("Google account could not be saved.")
            return
        }
        val expectedAccount = cloudStore.read().account
        if (expectedAccount.isBlank() || !expectedAccount.equals(account, ignoreCase = true)) {
            cloudBusy = false
            cloudStore.status("Google account mismatch; reconnect before using backup")
            refreshCloud()
            return
        }
        lifecycleScope.launch {
            val resultTask = withContext(Dispatchers.IO) {
                runCatching {
                    val drive = NightWaveDriveClient(token)
                    when (action) {
                        "backup" -> {
                            val local = archiveManager.export()
                            val settings = cloudStore.read()
                            val history = if (settings.includeHistory) library.recent() else emptyList()
                            val bytes = CloudBackupCodec.encode(local, history, settings.includeHistory)
                            check(cloudStore.read().account.equals(expectedAccount, true)) {
                                "Google account was disconnected"
                            }
                            drive.uploadVerified(bytes)
                            null
                        }
                        "preview", "merge" -> {
                            drive.listSnapshots().takeIf { it.isNotEmpty() }
                                ?: error("No NightWave cloud backup found")
                        }
                        else -> error("Invalid cloud action")
                    }
                }
            }
            cloudBusy = false
            resultTask.onSuccess { preview ->
                if (action == "backup") {
                    if (cloudStore.success(expectedAccount, System.currentTimeMillis())) {
                        runCatching {
                            NightWaveCloudSchedule.update(this@NightWaveStudioActivity)
                        }.onFailure {
                            cloudStore.status("Upload verified; backup schedule needs attention")
                        }
                        refreshCloud()
                        showMessage("Cloud backup uploaded and verified.")
                    } else {
                        showMessage("Backup uploaded, but scheduling needs a fresh connection.")
                    }
                } else if (preview != null) {
                    showCloudSnapshotPicker(preview, token,
                        forMerge = action == "merge", expectedAccount = expectedAccount)
                }
            }.onFailure { error ->
                // Only bounded, non-sensitive status to UI; never display an access token.
                cloudStore.status(if (error.message == "No NightWave cloud backup found")
                    "No cloud backups found in this Google account"
                else "Drive operation unsuccessful; local library unchanged")
                refreshCloud()
                showMessage(cloudStore.read().lastResult)
            }
        }
    }

    private fun showCloudSnapshotPicker(
        snapshots: List<DriveSnapshot>, token: String,
        forMerge: Boolean, expectedAccount: String
    ) {
        val options = snapshots.take(20)
        val labels = options.mapIndexed { index, snapshot ->
            val timestamp = snapshot.name.removePrefix("nightwave-cloud-v2-")
                .substringBefore("-").toLongOrNull()
            val whenSaved = timestamp?.let {
                java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))
            } ?: "Unknown date"
            "${index + 1}. $whenSaved"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(if (forMerge) "Compare with cloud snapshot" else "Choose a cloud backup")
            .setItems(labels) { _, which ->
                val chosen = options[which]
                cloudBusy = true
                lifecycleScope.launch {
                    val outcome = withContext(Dispatchers.IO) {
                        runCatching {
                            check(cloudStore.read().account.equals(expectedAccount, true)) {
                                "Google account changed"
                            }
                            val remote = CloudBackupCodec.decode(
                                NightWaveDriveClient(token).download(chosen.id))
                            val plan = if (forMerge) CloudLibraryMerger.preview(
                                archiveManager.export(), remote.archive) else null
                            remote to plan
                        }
                    }
                    cloudBusy = false
                    outcome.onSuccess { (remote, plan) ->
                        if (!cloudStore.read().account.equals(expectedAccount, true)) {
                            showMessage("Google account changed; compare again.")
                        } else if (plan != null) showCloudMergePreview(plan, expectedAccount)
                        else showCloudRestorePreview(remote)
                    }
                        .onFailure {
                            cloudStore.status("Selected backup unavailable or failed validation")
                            refreshCloud()
                            showMessage("Selected backup could not be opened; local data unchanged.")
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Preview changes before confirmation. Applying checks account and local
     * checkpoint; neither a late playlist edit nor a different Google account
     * may be overwritten by the merge.
     */
    private fun showCloudMergePreview(plan: CloudMergePlan, account: String) {
        val conflicts = if (plan.conflictingPlaylistNames.isEmpty()) ""
            else "\n\nPlaylist name conflicts: " +
                plan.conflictingPlaylistNames.take(5).joinToString(", ") +
                ". Separate (Drive) copies will be added."
        val message = "Cloud comparison: ${plan.addedFavoriteCount} additional liked tracks, " +
            "${plan.addedPlaylistCount} additional playlists, " +
            "${plan.newTrackReferences} imported playlist entries." +
            conflicts + "\n\nLocal profile, playlists and listening history are preserved. " +
            "This operation does not upload or delete cloud files."
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (plan.hasChanges) "Review library merge" else "Already up to date")
            .setMessage(message)
            .setNegativeButton("Close", null)
        if (plan.hasChanges) {
            dialog.setPositiveButton("Merge into this device") { _, _ ->
                if (cloudBusy || !cloudStore.read().account.equals(account, ignoreCase = true)) {
                    showMessage("Google account changed or busy; compare again.")
                    return@setPositiveButton
                }
                cloudBusy = true
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching { archiveManager.mergeIfUnchanged(plan) }
                            .getOrDefault(CloudMergeApplyResult.FAILED)
                    }
                    cloudBusy = false
                    when (result) {
                        CloudMergeApplyResult.APPLIED -> {
                            profile = profiles.load()
                            refresh()
                            showMessage("Library merged. Cloud backups are unchanged.")
                        }
                        CloudMergeApplyResult.STALE_PREVIEW ->
                            showMessage("Your library changed. Compare again before merging.")
                        CloudMergeApplyResult.NO_CHANGES ->
                            showMessage("Library already contains those tracks.")
                        CloudMergeApplyResult.FAILED ->
                            showMessage("Merge could not finish. Review your library before retrying.")
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showCloudRestorePreview(preview: CloudBackupPreview) {
        val message = "Cloud backup: ${preview.archive.favorites.size} liked songs and " +
            "${preview.archive.playlists.size} playlists." +
            if (preview.includesHistory) " Also contains ${preview.history.size} recent tracks."
            else " No listening history included."
        val dialog = AlertDialog.Builder(this)
            .setTitle("Review cloud restore")
            .setMessage(message + "\n\nRestoring replaces local favorites and playlists. " +
                "Export your current library first if you need both copies. " +
                "The existing recovery snapshot is preserved.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Restore library") { _, _ ->
                restoreCloudAfterConfirmation(preview, false)
            }
        if (preview.includesHistory && preview.history.isNotEmpty()) {
            dialog.setNeutralButton("Restore + history") { _, _ ->
                restoreCloudAfterConfirmation(preview, true)
            }
        }
        dialog.show()
    }

    private fun restoreCloudAfterConfirmation(preview: CloudBackupPreview, withHistory: Boolean) {
        val history = if (withHistory) preview.history else null
        if (archiveManager.restoreWithConfirmation(preview.archive, history)) {
            profile = profiles.load()
            refresh()
            showMessage("Cloud library restored. Automatic backup was not changed.")
        } else showMessage("Restore could not finish; your previous library was preserved.")
    }

    private fun disconnectCloud() {
        if (cloudBusy) {
            showMessage("Wait for the current backup operation to finish.")
            return
        }
        cloudStore.disconnect()
        runCatching { NightWaveCloudSchedule.stop(this) }
        refreshCloud()
        showMessage("Disconnected locally. Existing cloud snapshots were not deleted.")
    }

    private fun nativeSearch(query: String, mode: SearchMode) {
        val clean = query.trim().take(160)
        when (val input = SearchInputResolver.resolve(clean)) {
            SearchInput.Blank -> return
            is SearchInput.VideoUrl -> {
                nativeSearchJob?.cancel()
                nativeSearchGeneration++
                nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode,
                    hint = "Opening this public video in NightWave…")
                playback.play(VideoPreview("Loading track…", "YouTube", input.url, "", 0L))
                return
            }
            SearchInput.BadLink -> {
                nativeSearchJob?.cancel()
                nativeSearchGeneration++
                nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode,
                    hint = "This link is not a supported public YouTube video. Check the URL.")
                return
            }
            is SearchInput.Keywords -> Unit
        }
        val request = ++nativeSearchGeneration
        nativeSearchJob?.cancel()
        // A retry for the same query should not erase successful public
        // results while the network is temporarily unavailable.
        val previous = nativeSearchState.takeIf {
            it.query.equals(clean, ignoreCase = true) && it.mode == mode
        }
        nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode, loading = true,
            videos = previous?.videos.orEmpty(), provider = previous?.provider.orEmpty())
        nativeSearchJob = lifecycleScope.launch {
            val result: Result<VideoSearchResults> = try {
                withContext(Dispatchers.IO) { Result.success(nativeGateway.search(clean, mode)) }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Result.failure(error)
            }
            if (request != nativeSearchGeneration || isFinishing || isDestroyed) return@launch
            result.fold(
                onSuccess = { response ->
                    nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode,
                        videos = response.videos, provider = response.note)
                },
                onFailure = {
                    nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode,
                        videos = previous?.videos.orEmpty(), provider = previous?.provider.orEmpty(),
                        error = "Online providers could not be reached. Saved music still works.")
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (::playback.isInitialized) playback.resumeUiUpdates()
    }

    override fun onStop() {
        nativeSearchJob?.cancel()
        nativeSearchGeneration++
        if (nativeSearchState.loading) {
            nativeSearchState = nativeSearchState.copy(loading = false,
                error = "Search was paused. Tap Find music to retry.")
        }
        if (::playback.isInitialized) playback.pauseUiUpdates()
        super.onStop()
    }

    override fun onDestroy() {
        nativeSearchJob?.cancel()
        nativeSearchGeneration++
        youtubeDialog?.dismiss()
        youtubeDialog = null
        if (::playback.isInitialized) playback.release()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (::library.isInitialized) refresh()
        refreshListeningMemory()
        if (::roomShadow.isInitialized && ::profileShadow.isInitialized &&
            ::verifiedReader.isInitialized) refreshShadows()
    }

    /** Reading small private aggregates never changes player or cloud state. */
    private fun refreshListeningMemory() {
        if (!::listeningMemoryStore.isInitialized) return
        val generation = ++memoryReadGeneration
        lifecycleScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                runCatching { listeningMemoryStore.snapshot() }.getOrNull()
            }
            if (generation == memoryReadGeneration && snapshot != null) {
                memorySummary = snapshot
            }
        }
    }

    private fun setListeningMemory(enabled: Boolean) {
        if (!::listeningMemoryStore.isInitialized) return
        val result = listeningMemoryStore.setEnabled(enabled)
        if (!result) {
            showMessage("Could not change listening privacy settings.")
        }
        refreshListeningMemory()
    }

    private fun confirmEraseListeningMemory() {
        AlertDialog.Builder(this)
            .setTitle("Erase listening statistics?")
            .setMessage("Remove the local play-session statistics and switch tracking off. " +
                "This cannot be undone. Your songs, playlists and listening history stay untouched.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Erase & turn off") { _, _ ->
                if (listeningMemoryStore.eraseAndDisable()) {
                    refreshListeningMemory()
                    showMessage("Listening statistics erased and tracking disabled.")
                } else showMessage("Could not erase listening statistics.")
            }.show()
    }

    private fun saveCurrentToPlaylist() {
        val now = playback.state
        if (now.offline) {
            showMessage("Imported audio stays in your Offline library.")
            return
        }
        if (now.hasTrack) saveSongToPlaylist(VideoPreview(
            now.title.ifBlank { "YouTube video" }, now.artist, now.videoUrl,
            now.thumbnail, now.durationMs / 1000L
        ))
    }

    private fun saveSongToPlaylist(video: VideoPreview) {
        val names = library.playlistNames()
        val choices = names + "＋ Create playlist"
        AlertDialog.Builder(this).setTitle("Save to playlist")
            .setItems(choices.toTypedArray()) { _, option ->
                if (option < names.size) {
                    if (library.addToPlaylist(names[option], video)) refresh()
                    else showMessage("Already saved or the playlist is full.")
                } else promptPlaylistName { created ->
                    if (library.createPlaylist(created)) {
                        library.addToPlaylist(created, video)
                        refresh()
                    } else showMessage("Choose a different playlist name.")
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showSongOptions(video: VideoPreview) {
        val likeText = if (library.isFavorite(video.url)) "Unlike song" else "Like song"
        AlertDialog.Builder(this).setTitle(video.title)
            .setItems(arrayOf(likeText, "Save to playlist", "Add to queue", "Play now")) { _, selected ->
                when (selected) {
                    0 -> { library.toggleFavorite(video); refresh() }
                    1 -> saveSongToPlaylist(video)
                    2 -> playback.enqueue(video)
                    3 -> playback.play(video)
                }
            }.setNegativeButton("Close", null).show()
    }

    private fun promptPlaylistName(initial: String = "", onSave: (String) -> Unit) {
        val name = EditText(this).apply {
            setSingleLine(true)
            hint = "Playlist name"
            setText(initial)
        }
        AlertDialog.Builder(this).setTitle(if (initial.isBlank()) "New playlist" else "Rename playlist")
            .setView(name)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ -> onSave(name.text.toString().trim()) }
            .show()
    }

    /**
     * Private app-only crash evidence is available directly from premium UI.
     * Show a preview first and require an explicit confirmation before opening
     * Android's share chooser. No telemetry is uploaded automatically.
     */
    private fun showCrashSummaryDialog() {
        // Share exactly the text displayed in the confirmation dialog.
        // Keep the newest exception evidence, plus the OS exit history at end.
        val report = NightWaveCrashRecorder.report(this).take(5000) + "\n\n" +
            NightWaveExitHistory.report(this)
        val contextText = "Includes app-only exception types/stack frames and recent " +
            "Android process-exit reasons. No song titles, URLs, file paths, " +
            "full system traces, account details or device identifiers are collected."
        AlertDialog.Builder(this)
            .setTitle("NightWave crash diagnostics")
            .setMessage(contextText + "\n\n" + report)
            .setNegativeButton("Close", null)
            .setPositiveButton("Share summary") { _, _ ->
                try {
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, report)
                    }, "Share NightWave crash diagnostics"))
                } catch (error: Exception) {
                    showMessage("No sharing app was available. The report remains on this device.")
                }
            }.show()
    }

    /** Explicit sharesheet only; never shares imported audio files or private data. */
    private fun sharePlaylist(name: String, tracks: List<VideoPreview>) {
        val text = PlaylistShareFormatter.format(name, tracks)
        if (text == null) {
            showMessage("No shareable public links in this playlist.")
            return
        }
        runCatching {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, "Share NightWave playlist"))
        }.onFailure { showMessage("No sharing app was available.") }
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun manageLibrary() {
        val names = library.playlistNames()
        val choices = arrayOf("＋ Create playlist", *names.toTypedArray())
        AlertDialog.Builder(this).setTitle("Manage your playlists")
            .setItems(choices) { _, selected ->
                if (selected == 0) {
                    promptPlaylistName { name ->
                        if (library.createPlaylist(name)) refresh()
                        else showMessage("That playlist name is unavailable.")
                    }
                } else {
                    val name = names[selected - 1]
                    AlertDialog.Builder(this).setTitle(name)
                        .setItems(arrayOf("Rename", "Delete")) { _, action ->
                            if (action == 0) promptPlaylistName(name) { updated ->
                                if (library.renamePlaylist(name, updated)) refresh()
                                else showMessage("That playlist name is unavailable.")
                            } else AlertDialog.Builder(this)
                                .setTitle("Delete " + name + "?")
                                .setMessage("Songs remain in your favorites and listening history.")
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Delete") { _, _ ->
                                    library.deletePlaylist(name)
                                    refresh()
                                }.show()
                        }.show()
                }
            }.setNegativeButton("Close", null).show()
    }

    private fun confirmLastWriteRecovery() {
        AlertDialog.Builder(this)
            .setTitle("Undo the last library edit?")
            .setMessage(
                "This restores the state immediately before the most recent saved library change. " +
                "It might be a recent listening-history update, favorite, or playlist edit. " +
                "It will only run if none of the saved library keys have changed since that write. " +
                "For long-term recovery, export a backup first."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Undo last edit") { _, _ ->
                val restored = LegacyLibraryTransaction.recoverLastWriteIfUnchanged(this)
                if (restored) {
                    disableRoomPreview()
                    refresh()
                    showMessage("Last library edit undone.")
                } else {
                    showMessage("Recovery not available or library has changed. Nothing replaced.")
                }
            }.show()
    }

    private fun refresh() {
        // A legacy edit invalidates any previously displayed Room snapshot.
        roomPreviewActive = false
        roomPreviewPlaylists = emptyMap()
        favorites = library.favorites()
        recentlyPlayed = library.recent()
        names = library.playlistNames()
        libraryRevision++
        val checkpoint = OfflineSessionStore(this).read()
        val song = checkpoint?.selectedId?.let { id ->
            runCatching { OfflineAudioLibrary(this).find(id) }.getOrNull()
        }?.takeIf { OfflineAudioLibrary(this).fileFor(it) != null }
        offlineResumeTitle = song?.title
        offlineResumePositionMs = if (song != null) (checkpoint?.positionMs ?: 0L) else 0L
        // Do not enqueue a complete Room rewrite for every player/queue update.
        // onResume and the explicit "Recheck" action handle shadow verification.
    }

    /**
     * A secondary, read-only-vs-legacy sync. On any failure the old library
     * remains unchanged and the UI can still play music.
     */
    private fun enableVerifiedRoomPreview() {
        roomPreviewRequested = true
        roomReadStatus = "Validating every track before reading Room…"
        refreshShadows()
    }

    private fun disableRoomPreview() {
        roomPreviewRequested = false
        roomPreviewActive = false
        roomPreviewPlaylists = emptyMap()
        favorites = library.favorites()
        recentlyPlayed = library.recent()
        names = library.playlistNames()
        roomReadStatus = "Legacy library reads are active"
    }

    private fun refreshShadows() {
        if (!::roomShadow.isInitialized || !::profileShadow.isInitialized ||
            !::verifiedReader.isInitialized) {
            roomPreviewRequested = false
            roomReadStatus = "Storage preview unavailable; NightWave is using your original library."
            return
        }
        if (shadowRunning) {
            shadowRequested = true
            return
        }
        shadowRunning = true
        lifecycleScope.launch {
            try {
                do {
                    shadowRequested = false
                    shadowStatus = "Checking local storage copy…"
                    try {
                        val libraryReport = roomShadow.reconcile()
                        profileShadow.reconcile()
                        val check = verifiedReader.inspect()
                        shadowStatus = "Room/DataStore copy · " +
                            libraryReport.favorites + " likes · " +
                            libraryReport.playlists + " playlists · " +
                            libraryReport.songs + " unique songs"
                        roomReadStatus = if (check.canUseRoom)
                            "Exact Room parity verified against the current legacy library"
                        else "Legacy fallback · " + check.parity.blocker.name
                        if (roomPreviewRequested && check.canUseRoom) {
                            val snapshot = check.snapshot!!
                            // Compose previews can read the validated Room snapshot.
                            roomPreviewPlaylists = snapshot.playlists.toMap()
                            favorites = snapshot.favorites
                            recentlyPlayed = snapshot.recent
                            names = snapshot.playlistNames()
                            roomPreviewActive = true
                            roomReadStatus = "Room read preview active for this session"
                        } else if (roomPreviewActive || roomPreviewRequested) {
                            roomPreviewActive = false
                            roomPreviewPlaylists = emptyMap()
                            favorites = library.favorites()
                            recentlyPlayed = library.recent()
                            names = library.playlistNames()
                        }
                    } catch (error: kotlinx.coroutines.CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        roomPreviewActive = false
                        roomPreviewPlaylists = emptyMap()
                        favorites = library.favorites()
                        recentlyPlayed = library.recent()
                        names = library.playlistNames()
                        roomReadStatus = "Legacy fallback · migration or validation failed"
                        shadowStatus = "Storage preview needs attention: " +
                            (error.message ?: "Could not migrate legacy data").take(150) +
                            ". Original library remains in use."
                    }
                } while (shadowRequested)
            } finally {
                shadowRunning = false
            }
        }
    }
}

private const val LIKED_COLLECTION = "nightwave://local-liked"
private enum class StudioTab(val caption: String) {
    HOME("Home"), SEARCH("Search"), LIBRARY("Library"), PROFILE("You")
}

@Composable
private fun StudioRoot(
    profile: ProfileSettings,
    favorites: List<VideoPreview>,
    recent: List<VideoPreview>,
    libraryRevision: Int,
    playlistNames: List<String>,
    getPlaylist: (String) -> List<VideoPreview>,
    onSaveProfile: (ProfileSettings) -> Unit,
    onExportGuest: () -> Unit,
    onImportGuest: () -> Unit,
    onRecoverLastChange: () -> Unit,
    cloud: CloudBackupStatus,
    cloudBusy: Boolean,
    onCloudConnect: () -> Unit,
    onCloudBackup: () -> Unit,
    onCloudPreviewRestore: () -> Unit,
    onCloudCompare: () -> Unit,
    onCloudDisconnect: () -> Unit,
    onCloudHistory: (Boolean) -> Unit,
    onCloudWifi: (Boolean) -> Unit,
    storageStatus: String,
    onCheckStorage: () -> Unit,
    roomReadStatus: String,
    roomPreviewActive: Boolean,
    onEnableRoomPreview: () -> Unit,
    onUseLegacyReads: () -> Unit,
    playerState: StudioPlayerState,
    playerActions: StudioPlaybackBridge,
    sleepRemainingMs: Long,
    onSleepTimer: (Int?) -> Unit,
    memory: ListeningMemorySummary,
    onMemoryEnabledChange: (Boolean) -> Unit,
    onMemoryErase: () -> Unit,
    onMemoryRefresh: () -> Unit,
    currentLyrics: TimedLyrics?,
    onImportLyrics: () -> Unit,
    onRemoveLyrics: () -> Unit,
    offlineResumeTitle: String?,
    offlineResumePositionMs: Long,
    onResumeOffline: () -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (VideoPreview) -> Unit,
    onSongOptions: (VideoPreview) -> Unit,
    onSharePlaylist: (String, List<VideoPreview>) -> Unit,
    onSaveCurrentToPlaylist: () -> Unit,
    onSearch: (String, SearchMode) -> Unit,
    onPrimarySearch: (String, SearchMode) -> Unit,
    onPrepareSearch: (String, SearchMode) -> Unit,
    onWebSearch: (String) -> Unit,
    searchState: StudioSearchSnapshot,
    onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onLegacyPlayer: () -> Unit,
    onOfflineLibrary: () -> Unit,
    onShareDiagnostics: () -> Unit,
    onOpenClassic: () -> Unit,
    searchNavigationToken: Int
) {
    // The pager is the single source of truth for swipes, tabs and Back.
    // Pager state survives rotation and process recreation through Compose Saver.
    val pagerState = rememberPagerState(initialPage = StudioTab.HOME.ordinal) {
        StudioTab.entries.size
    }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val tab = StudioTab.entries[pagerState.currentPage]
    LaunchedEffect(tab, playerState.positionMs / 15000L) {
        if (tab == StudioTab.PROFILE || tab == StudioTab.HOME) onMemoryRefresh()
    }
    LaunchedEffect(tab) {
        // An active search/library text editor must not leave the soft keyboard
        // covering another page after a user swipe or bottom-tab selection.
        focus.clearFocus()
        keyboard?.hide()
    }
    var selectedPlaylist by rememberSaveable { mutableStateOf<String?>(null) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var compactHidden by rememberSaveable { mutableStateOf(false) }
    fun navigateToTab(destination: StudioTab) {
        if (destination == tab) return
        if (destination != StudioTab.LIBRARY) selectedPlaylist = null
        scope.launch {
            if (profile.motion == MotionPreference.REDUCED) {
                pagerState.scrollToPage(destination.ordinal)
            } else {
                pagerState.animateScrollToPage(destination.ordinal)
            }
        }
    }
    LaunchedEffect(playerState.videoUrl) { compactHidden = false }
    LaunchedEffect(searchNavigationToken) {
        // Never replay shared media on rotation or slide through other pages.
        if (searchNavigationToken > 0) pagerState.scrollToPage(StudioTab.SEARCH.ordinal)
    }
    BackHandler(enabled = showQueue || expanded ||
        (tab == StudioTab.LIBRARY && selectedPlaylist != null) || tab != StudioTab.HOME) {
        when {
            showQueue -> showQueue = false
            expanded -> expanded = false
            tab == StudioTab.LIBRARY && selectedPlaylist != null -> selectedPlaylist = null
            tab != StudioTab.HOME -> navigateToTab(StudioTab.HOME)
        }
    }
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(Modifier.fillMaxWidth().height(56.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                NightWaveWordmark()
                Surface(
                    onClick = { navigateToTab(StudioTab.PROFILE) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Open NightWave settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(21.dp))
                    }
                }
            }
        },
        bottomBar = {
            Column {
                if (!playerState.error.isNullOrBlank() &&
                    (playerState.hasTrack || !playerState.connected)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(playerState.error.orEmpty(),
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = { playerActions.retry() }) {
                                Text(if (!playerState.connected) "Reconnect" else "Retry")
                            }
                        }
                    }
                }
                if (playerState.hasTrack && compactHidden) {
                    // Collapsed UI keeps playback alive and offers one-tap restore.
                    Surface(onClick = { compactHidden = false },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(40.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Outlined.PlayArrow,
                                contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Now playing · Tap to show",
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else if (playerState.hasTrack) {
                    StudioMiniPlayer(playerState, playerActions,
                        onExpand = { expanded = true },
                        onHide = { compactHidden = true })
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(58.dp)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (item in StudioTab.entries) {
                            val image = when (item) {
                                StudioTab.HOME -> Icons.Outlined.Home
                                StudioTab.SEARCH -> Icons.Outlined.Search
                                StudioTab.LIBRARY -> Icons.Outlined.LibraryMusic
                                StudioTab.PROFILE -> Icons.Outlined.PersonOutline
                            }
                            val active = tab == item
                            val tabColor by animateColorAsState(
                                targetValue = if (active) MaterialTheme.colorScheme.primary.copy(alpha = .17f)
                                    else Color.Transparent,
                                animationSpec = tween(durationMillis =
                                    if (profile.motion == MotionPreference.REDUCED) 0 else 170),
                                label = "NightWave navigation focus"
                            )
                            Row(
                                Modifier.weight(if (active) 1.25f else 1f).fillMaxHeight()
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(tabColor)
                                    .clickable { navigateToTab(item) }
                                    .semantics { this.selected = active }
                                    .testTag("nightwave-tab-" + item.name.lowercase()),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(image, contentDescription = item.caption,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                                if (active) {
                                    Spacer(Modifier.width(7.dp))
                                    Text(item.caption,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { safe ->
        // HorizontalPager owns page swipes; child LazyColumns own vertical scroll.
        // Full player / Up Next overlay gestures never move the background tabs.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(safe).testTag("nightwave-tab-pager"),
            key = { StudioTab.entries[it].name },
            beyondViewportPageCount = 1,
            userScrollEnabled = !expanded && !showQueue,
            pageSpacing = 0.dp
        ) { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                when (StudioTab.entries[page]) {
                    StudioTab.HOME -> StudioHome(
                        modifier = Modifier, profile = profile, recent = recent,
                        favorites = favorites,
                        memory = memory,
                        playlistTracks = remember(libraryRevision, playlistNames, roomPreviewActive) {
                            playlistNames.take(25).flatMap(getPlaylist).distinctBy { it.url }.take(350)
                        },
                        playingUrl = playerState.videoUrl.takeIf { playerState.playing }.orEmpty(),
                        onSearch = { query ->
                            onPrepareSearch(query, SearchMode.SONGS)
                            navigateToTab(StudioTab.SEARCH)
                        },
                        onResumeSpotlight = { song, collection ->
                            when (SpotlightResumePolicy.choose(
                                tappedUrl = song.url, currentUrl = playerState.videoUrl,
                                currentQueueIndex = playerState.queueIndex, offline = playerState.offline,
                                playing = playerState.playing, buffering = playerState.buffering,
                                hasError = playerState.error != null
                            )) {
                                SpotlightAction.KEEP_CURRENT -> Unit
                                SpotlightAction.RESUME_CURRENT -> playerActions.resumeCurrent()
                                SpotlightAction.RETRY_CURRENT -> playerActions.retry()
                                SpotlightAction.START_SELECTED -> onPlay(song, collection)
                            }
                        },
                        onPlay = onPlay, onSongOptions = onSongOptions,
                        openLibrary = { navigateToTab(StudioTab.LIBRARY) }
                    )
                    StudioTab.SEARCH -> StudioSearch(
                        Modifier, searchState, onPrimarySearch, onSearch, onWebSearch,
                        onPlay, onSongOptions,
                        playerState.videoUrl.takeIf { playerState.playing }.orEmpty(),
                        savedTracks = remember(libraryRevision, favorites, recent, playlistNames, roomPreviewActive) {
                            (favorites + recent + playlistNames.take(25).flatMap(getPlaylist))
                                .distinctBy { it.url }
                                .take(350)
                        }
                    )
                    StudioTab.LIBRARY -> StudioLibrary(
                        modifier = Modifier, favorites = favorites, names = playlistNames,
                        playingUrl = playerState.videoUrl.takeIf { playerState.playing }.orEmpty(),
                        selectedPlaylist = selectedPlaylist,
                        onSelectPlaylist = { selectedPlaylist = it },
                        getPlaylist = getPlaylist, onPlay = onPlay,
                        onSongOptions = onSongOptions,
                        onSharePlaylist = onSharePlaylist,
                        onManage = onLegacyPlayer, onOffline = onOfflineLibrary,
                        offlineResumeTitle = offlineResumeTitle,
                        offlineResumePositionMs = offlineResumePositionMs,
                        onResumeOffline = onResumeOffline,
                        onSearch = { navigateToTab(StudioTab.SEARCH) }
                    )
                    StudioTab.PROFILE -> StudioProfile(
                        modifier = Modifier, profile = profile, save = onSaveProfile,
                        onExport = onExportGuest, onImport = onImportGuest,
                        onRecoverLastChange = onRecoverLastChange,
                        cloud = cloud, cloudBusy = cloudBusy,
                        onCloudConnect = onCloudConnect,
                        onCloudBackup = onCloudBackup,
                        onCloudPreviewRestore = onCloudPreviewRestore,
                        onCloudCompare = onCloudCompare,
                        onCloudDisconnect = onCloudDisconnect,
                        onCloudHistory = onCloudHistory,
                        onCloudWifi = onCloudWifi,
                        storageStatus = storageStatus, onCheckStorage = onCheckStorage,
                        roomReadStatus = roomReadStatus,
                        roomPreviewActive = roomPreviewActive,
                        onEnableRoomPreview = onEnableRoomPreview,
                        onUseLegacyReads = onUseLegacyReads,
                        insights = remember(recent, favorites, playlistNames) {
                            ListeningInsightsEngine.calculate(recent, favorites, playlistNames)
                        },
                        memory = memory,
                        onMemoryEnabledChange = onMemoryEnabledChange,
                        onMemoryErase = onMemoryErase,
                        onMemoryRefresh = onMemoryRefresh,
                        playbackState = playerState,
                        onRecoverPlayback = { playerActions.retry() },
                        onShareDiagnostics = onShareDiagnostics,
                        onOpenClassic = onOpenClassic
                    )
                }
            }
        }
    }
    StudioNowPlayingOverlay(
        expanded = expanded && playerState.hasTrack,
        state = playerState, actions = playerActions,
        reduceMotion = profile.motion == MotionPreference.REDUCED,
        liked = playerState.hasTrack && !playerState.offline &&
            isFavorite(playerState.videoUrl),
        onLike = {
            if (playerState.hasTrack && !playerState.offline) onToggleFavorite(VideoPreview(
                playerState.title, playerState.artist, playerState.videoUrl,
                playerState.thumbnail, playerState.durationMs / 1000L
            ))
        },
        onOpenQueue = { showQueue = true },
        onSaveToPlaylist = onSaveCurrentToPlaylist,
        sleepRemainingMs = sleepRemainingMs,
        onSleepTimer = onSleepTimer,
        lyrics = currentLyrics,
        onImportLyrics = onImportLyrics,
        onRemoveLyrics = onRemoveLyrics,
        onDismiss = { expanded = false }
    )
    if (showQueue) StudioQueueSheet(
        state = playerState, onDismiss = { showQueue = false },
        onSelect = { index -> playerActions.jumpTo(index); showQueue = false },
        onRemoveUpcoming = { index, url -> playerActions.removeUpcoming(index, url) },
        onUndoRemoval = { token -> playerActions.undoUpcoming(token) },
        reduceMotion = profile.motion == MotionPreference.REDUCED
    )
    }
}

@Composable
private fun StudioHome(
    modifier: Modifier, profile: ProfileSettings, recent: List<VideoPreview>,
    favorites: List<VideoPreview>, playlistTracks: List<VideoPreview>,
    memory: ListeningMemorySummary,
    playingUrl: String, onSearch: (String) -> Unit,
    onResumeSpotlight: (VideoPreview, List<VideoPreview>) -> Unit,
    onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onSongOptions: (VideoPreview) -> Unit, openLibrary: () -> Unit
) {
    val dayNumber = java.time.LocalDate.now().toEpochDay()
    val mixes = remember(favorites, recent, playlistTracks, dayNumber, memory) {
        PersonalMixEngine.make(favorites, recent, playlistTracks, dayNumber,
            memoryTracks = memory.recommendationTracks,
            memoryEnabled = memory.enabled)
    }
    var chosenMixKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showAllMixTracks by rememberSaveable { mutableStateOf(false) }
    val chosenMix = mixes.firstOrNull { it.key == chosenMixKey }
    val palettes = listOf(
        listOf(Color(0xFF423163), Color(0xFF242B57)),
        listOf(Color(0xFF164F58), Color(0xFF19334D)),
        listOf(Color(0xFF744259), Color(0xFF3E2B56)),
        listOf(Color(0xFF4D577C), Color(0xFF273B51)),
        listOf(Color(0xFF705031), Color(0xFF4F334B)),
        listOf(Color(0xFF3B5867), Color(0xFF344066))
    )
    LazyColumn(
        modifier = modifier.fillMaxWidth().widthIn(max = 740.dp).fillMaxHeight(),
        // Scaffold already reserves the mini-player and navigation height;
        // only add breathing room, not an artificial 150dp blank spacer.
        contentPadding = PaddingValues(start = 19.dp, end = 19.dp,
            top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                NightWaveMicroLabel("Your listening space")
                val greeting = when (java.time.LocalTime.now().hour) {
                    in 5..11 -> "Good morning"
                    in 12..17 -> "Good afternoon"
                    else -> "Good evening"
                }
                Text(greeting, style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground)
                Text(profile.displayName.substringBefore(' ').take(20) +
                    " · Your music, your pace",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            NightWaveLibraryNumbers(favorites.size, recent.size, onLibrary = openLibrary)
        }
        item {
            ListeningSpotlight(
                lastTrack = recent.firstOrNull(),
                onResume = { recent.firstOrNull()?.let { onResumeSpotlight(it, recent) } },
                onDiscover = { onSearch("night drive music") }
            )
        }
        if (mixes.isNotEmpty()) {
            item {
                SectionHeading("Made for you", "Mixes built privately from your saved music")
            }
            mixes.chunked(2).forEachIndexed { row, pair ->
                item(key = "personal-mix-$row") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { mix ->
                            NightWaveMixCard(
                                mix = mix, selected = chosenMixKey == mix.key,
                                modifier = if (pair.size == 1) Modifier.fillMaxWidth()
                                    else Modifier.weight(1f),
                                compact = pair.size == 1,
                                onClick = {
                                    chosenMixKey = if (chosenMixKey == mix.key) null else mix.key
                                    showAllMixTracks = false
                                }
                            )
                        }
                    }
                }
            }
            if (chosenMix != null) {
                item(key = "mix-explanation") {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(chosenMix.description, style = MaterialTheme.typography.titleSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(chosenMix.reason, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(onClick = {
                            chosenMix.tracks.firstOrNull()?.let { onPlay(it, chosenMix.tracks) }
                        }) { Text("Play mix") }
                    }
                }
                items(chosenMix.tracks.take(if (showAllMixTracks) 12 else 3),
                    key = { "mix:" + it.url }) { song ->
                    MusicTrackTile(song, isPlaying = playingUrl == song.url,
                        onOptions = { onSongOptions(song) },
                        onClick = { onPlay(song, chosenMix.tracks) })
                }
                if (chosenMix.tracks.size > 3) {
                    item(key = "mix-more-tracks") {
                        OutlinedButton(onClick = { showAllMixTracks = !showAllMixTracks },
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (showAllMixTracks) "Show fewer tracks"
                                else "See all ${chosenMix.tracks.size} tracks")
                        }
                    }
                }
            }
        }
        item { SectionHeading("Explore a new mood", "Swipe across six ways to listen") }
        // One horizontally scrollable shelf instead of three oversized rows.
        // Uses existing real mood shortcuts and unchanged search actions.
        item(key = "discovery-shelf") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DiscoveryShortcuts.picks.forEachIndexed { index, shortcut ->
                    MoodTile(shortcut.title, shortcut.subtitle,
                        palettes[index % palettes.size], Modifier.width(178.dp)) {
                        onSearch(shortcut.query)
                    }
                }
            }
        }
        item { SectionHeading("Recently played", "Pick up another favorite") }
        if (recent.isEmpty()) item {
            EmptyMusicCard("Your soundtrack starts here",
                "Songs you play will appear here, ready for another listen.",
                "Discover music") { onSearch("music") }
        } else items(recent.take(6), key = { it.url }) { song ->
            MusicTrackTile(song, isPlaying = playingUrl == song.url,
                onOptions = { onSongOptions(song) },
                onClick = { onPlay(song, recent) })
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                SectionHeading("Your collection", favorites.size.toString() + " liked tracks")
                OutlinedButton(onClick = openLibrary) { Text("Open Library  →") }
            }
        }
    }
}

@Composable
private fun MoodTile(title: String, subtitle: String, gradient: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    NightWaveMoodArt(title, subtitle, gradient, modifier, onClick)
}

@Composable
private fun StudioSearch(
    modifier: Modifier,
    state: StudioSearchSnapshot,
    onPrimarySearch: (String, SearchMode) -> Unit,
    onNativeSearch: (String, SearchMode) -> Unit,
    onWebSearch: (String) -> Unit,
    onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onSongOptions: (VideoPreview) -> Unit,
    playingUrl: String,
    savedTracks: List<VideoPreview>
) {
    var query by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(SearchMode.SONGS) }
    var quickOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.query) { if (state.query.isNotBlank()) query = state.query }
    val submit = { if (query.isNotBlank()) onPrimarySearch(query.trim(), mode) }
    val savedMatches = remember(query, savedTracks) {
        SavedTrackSearch.match(query, savedTracks)
    }
    val knownCreators = remember(savedTracks) {
        MusicIdentity.creatorsFrom(savedTracks, limit = 8)
    }
    // Never present results from an older query as results for new typed text.
    val responseMatchesQuery = state.query.isNotBlank() &&
        query.trim().equals(state.query.trim(), ignoreCase = true)
    val searching = responseMatchesQuery && state.loading
    LazyColumn(
        modifier.fillMaxWidth().widthIn(max = 740.dp).fillMaxHeight(),
        contentPadding = PaddingValues(start = 19.dp, end = 19.dp,
            top = 16.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                NightWaveMicroLabel("Discover")
                SectionHeading("Explore sound", "Your next favorite starts here")
            }
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(160) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp),
                singleLine = true, placeholder = { Text("Song or artist name") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() })
            )
        }
        if (query.isNotBlank()) {
            item {
                Button(onClick = submit, modifier = Modifier.fillMaxWidth().height(49.dp),
                    shape = RoundedCornerShape(15.dp)) {
                    Text("Find music on YouTube")
                }
                Text("Saved entries stay in your library. Public video audio still needs internet.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (query.isBlank() && !searching) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    NightWaveMicroLabel("Explore by mood")
                    DiscoveryShortcuts.picks.take(4).chunked(2).forEach { choices ->
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            choices.forEach { shortcut ->
                                FilterChip(selected = false,
                                    onClick = {
                                        mode = SearchMode.SONGS
                                        query = shortcut.query
                                        onPrimarySearch(shortcut.query, SearchMode.SONGS)
                                    },
                                    label = { Text(shortcut.title, maxLines = 1) },
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            if (knownCreators.isNotEmpty()) {
                item(key = "local-creators") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeading("Artists & creators from your music",
                            "Browse your saved and recent tracks · on this device")
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            knownCreators.forEach { creator ->
                                FilterChip(selected = false,
                                    onClick = {
                                        // Only update the local search filter.
                                        // Online search remains a separate tap.
                                        mode = SearchMode.SONGS
                                        query = creator.name
                                        quickOptionsExpanded = false
                                    },
                                    label = {
                                        Text(creator.name + " · " + creator.trackCount,
                                            maxLines = 1)
                                    })
                            }
                        }
                    }
                }
            }
            if (savedTracks.isNotEmpty()) {
                item {
                    SectionHeading("Already in your library",
                        "Saved entries · public videos require internet to play")
                }
                items(savedTracks.take(3), key = { "search-starter:" + it.url }) { song ->
                    MusicTrackTile(song, isPlaying = playingUrl == song.url,
                        onOptions = { onSongOptions(song) },
                        onClick = { onPlay(song, savedTracks) })
                }
            } else {
                item {
                    Text("Your liked songs, playlists and recent music will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (query.isNotBlank()) {
            item {
                OutlinedButton(onClick = { quickOptionsExpanded = !quickOptionsExpanded },
                    modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Text(if (quickOptionsExpanded) "Hide advanced search"
                        else "More search options")
                }
            }
            if (quickOptionsExpanded || (responseMatchesQuery &&
                    (state.loading || state.error.isNotBlank() || state.videos.isNotEmpty()))) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NightWaveMicroLabel("Optional quick results")
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        FilterChip(selected = mode == SearchMode.SONGS,
                            onClick = { mode = SearchMode.SONGS }, label = { Text("Songs") })
                        FilterChip(selected = mode == SearchMode.VIDEOS,
                            onClick = { mode = SearchMode.VIDEOS }, label = { Text("Videos") })
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { onNativeSearch(query.trim(), mode) },
                    enabled = !searching,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    if (searching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (searching) "Checking quick results…" else "Try quick in-app results")
                }
            }
            }
        }
        if (savedMatches.isNotEmpty()) {
            item {
                SectionHeading("In your library",
                    savedMatches.size.toString() + " matches from liked, recent and playlists")
            }
            items(savedMatches, key = { "saved:" + it.url }) { song ->
                MusicTrackTile(song, isPlaying = playingUrl == song.url,
                    onOptions = { onSongOptions(song) },
                    onClick = { onPlay(song, savedMatches) })
            }
        }
        if (searching) {
            item {
                Text("Checking public video search… Your saved songs remain available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (responseMatchesQuery && state.videos.isNotEmpty()) {
            item { SectionHeading("Public results",
                state.videos.size.toString() + " tracks · " + state.provider) }
            if (state.error.isNotBlank()) {
                item {
                    Text("Showing earlier results for this search. Online refresh failed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(state.videos, key = { "public:" + it.url }) { song ->
                MusicTrackTile(song, isPlaying = playingUrl == song.url,
                    onOptions = { onSongOptions(song) },
                    onClick = { onPlay(song, state.videos) })
            }
        } else if (responseMatchesQuery && state.error.isNotBlank()) {
            item {
                Column(Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Quick results temporarily unavailable",
                        style = MaterialTheme.typography.titleSmall)
                    Text("Your library still works. Use Find music on YouTube above, or retry quick results.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onNativeSearch(query.trim(), mode) },
                            enabled = query.isNotBlank()) {
                            Text("Retry quick results")
                        }
                        OutlinedButton(onClick = { onWebSearch(query.trim()) },
                            enabled = query.isNotBlank()) {
                            Text("YouTube")
                        }
                    }
                }
            }
        } else if (responseMatchesQuery && state.hint.isNotBlank()) {
            item {
                Text(state.hint, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (query.isNotBlank() && savedMatches.isEmpty() && !searching) {
            item {
                Text("Nothing saved for this search yet. Try YouTube or another artist.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (query.isNotBlank()) {
            item {
                Text("Online search and playback depend on provider availability.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StudioLibrary(
    modifier: Modifier, favorites: List<VideoPreview>, names: List<String>,
    playingUrl: String,
    selectedPlaylist: String?, onSelectPlaylist: (String?) -> Unit,
    getPlaylist: (String) -> List<VideoPreview>, onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onManage: () -> Unit, onSearch: () -> Unit,
    onOffline: () -> Unit,
    offlineResumeTitle: String?, offlineResumePositionMs: Long,
    onResumeOffline: () -> Unit,
    onSongOptions: (VideoPreview) -> Unit,
    onSharePlaylist: (String, List<VideoPreview>) -> Unit
) {
    val tracks = if (selectedPlaylist == null || selectedPlaylist == LIKED_COLLECTION)
        favorites else getPlaylist(selectedPlaylist)
    var localFilter by rememberSaveable(selectedPlaylist) { mutableStateOf("") }
    var sortMode by rememberSaveable(selectedPlaylist) { mutableStateOf("saved") }
    val matched = if (selectedPlaylist == null || localFilter.isBlank()) tracks else tracks.filter {
        it.title.contains(localFilter.trim(), ignoreCase = true) ||
            it.author.contains(localFilter.trim(), ignoreCase = true)
    }
    val orderedTracks = when (sortMode) {
        "title" -> matched.sortedWith(compareBy({ it.title.lowercase() }, { it.url }))
        "artist" -> matched.sortedWith(compareBy({ it.author.lowercase() }, { it.title.lowercase() }))
        else -> matched
    }
    val visibleTracks = if (selectedPlaylist == null) orderedTracks.take(5) else orderedTracks
    LazyColumn(modifier.fillMaxWidth().widthIn(max = 740.dp).fillMaxHeight(),
        contentPadding = PaddingValues(start = 19.dp, end = 19.dp,
            top = 19.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedPlaylist != null) IconButton(onClick = { onSelectPlaylist(null) }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to Library")
                }
                Column {
                    NightWaveMicroLabel(if (selectedPlaylist == null) "Your collection" else "Playlist view")
                    SectionHeading(if (selectedPlaylist == LIKED_COLLECTION) "Liked Songs" else selectedPlaylist ?: "Your library",
                        if (selectedPlaylist == null) "Saved music across your collections"
                        else tracks.size.toString() + " saved tracks")
                }
            }
        }
        if (selectedPlaylist != null &&
            selectedPlaylist != LIKED_COLLECTION && tracks.isNotEmpty()
        ) {
            item(key = "share-current-playlist") {
                OutlinedButton(onClick = { onSharePlaylist(selectedPlaylist, tracks) },
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Share playlist")
                }
            }
        }
        if (selectedPlaylist == null) {
            // Actual collection rows below serve as navigation. Avoid
            // duplicating them with two oversized, noninteractive counters.
            item {
                Text("${favorites.size} liked tracks  ·  ${names.size} playlists",
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (offlineResumeTitle != null) {
                item {
                    Card(onClick = onResumeOffline, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("CONTINUE LISTENING · OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text(offlineResumeTitle, style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Resume on this device" +
                                (if (offlineResumePositionMs >= 60_000)
                                    " · " + (offlineResumePositionMs / 60_000) + " min" else "") +
                                "  ›", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                MusicCollectionTile("Offline music",
                    "Your imported audio · listen without Wi-Fi",
                    cover = null, onClick = onOffline)
            }
            item {
                MusicCollectionTile("Liked songs",
                    favorites.size.toString() + " tracks",
                    cover = favorites.firstOrNull()) {
                    onSelectPlaylist(LIKED_COLLECTION)
                }
            }
            if (names.isNotEmpty()) {
                item { SectionHeading("Your playlists", names.size.toString() + " collections") }
                items(names, key = { it }) { name ->
                    val songs = getPlaylist(name)
                    MusicCollectionTile(name, songs.size.toString() + " tracks",
                        cover = songs.firstOrNull()) { onSelectPlaylist(name) }
                }
            }
        }
        item {
            SectionHeading(if (selectedPlaylist == null) "Recently liked" else "Saved tracks",
                if (selectedPlaylist == null) "Your latest five liked songs" else null)
        }
        if (selectedPlaylist != null && tracks.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = localFilter, onValueChange = { localFilter = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true,
                    placeholder = { Text("Filter this collection") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (localFilter.isNotBlank()) IconButton(onClick = { localFilter = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear library filter")
                        }
                    }
                )
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(selected = sortMode == "saved", onClick = { sortMode = "saved" },
                        label = { Text("Saved order") })
                    FilterChip(selected = sortMode == "title", onClick = { sortMode = "title" },
                        label = { Text("Title") })
                    FilterChip(selected = sortMode == "artist", onClick = { sortMode = "artist" },
                        label = { Text("Artist") })
                }
            }
        }
        if (visibleTracks.isEmpty()) item {
            EmptyMusicCard(
                if (tracks.isEmpty()) "Nothing here yet" else "No matching tracks",
                if (tracks.isEmpty()) "Save a song to build your collection."
                else "Try another title or artist.",
                if (tracks.isEmpty()) "Find music" else "Clear filter") {
                if (tracks.isEmpty()) onSearch() else localFilter = ""
            }
        } else items(visibleTracks, key = { it.url }) { song ->
            MusicTrackTile(song, isPlaying = playingUrl == song.url,
                onOptions = { onSongOptions(song) },
                onClick = { onPlay(song, if (selectedPlaylist == null) tracks else orderedTracks) })
        }
        item {
            OutlinedButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                Text("Manage playlists")
            }
        }
    }
}

@Composable
private fun StudioProfile(
    modifier: Modifier, profile: ProfileSettings, save: (ProfileSettings) -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, onRecoverLastChange: () -> Unit,
    cloud: CloudBackupStatus,
    cloudBusy: Boolean,
    onCloudConnect: () -> Unit,
    onCloudBackup: () -> Unit,
    onCloudPreviewRestore: () -> Unit,
    onCloudCompare: () -> Unit,
    onCloudDisconnect: () -> Unit,
    onCloudHistory: (Boolean) -> Unit,
    onCloudWifi: (Boolean) -> Unit,
    insights: ListeningInsights,
    memory: ListeningMemorySummary,
    onMemoryEnabledChange: (Boolean) -> Unit,
    onMemoryErase: () -> Unit,
    onMemoryRefresh: () -> Unit,
    playbackState: StudioPlayerState,
    onRecoverPlayback: () -> Unit,
    storageStatus: String, onCheckStorage: () -> Unit,
    roomReadStatus: String, roomPreviewActive: Boolean,
    onEnableRoomPreview: () -> Unit, onUseLegacyReads: () -> Unit,
    onShareDiagnostics: () -> Unit, onOpenClassic: () -> Unit
) {
    var name by rememberSaveable(profile.localId) { mutableStateOf(profile.displayName) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    // Keep the everyday You screen legible; full insights, local analytics
    // controls and developer-like playback health are one tap away.
    var showSnapshot by rememberSaveable { mutableStateOf(false) }
    var showListeningMemory by rememberSaveable { mutableStateOf(false) }
    var showPlaybackHealth by rememberSaveable { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(
        modifier = modifier.widthIn(max = 720.dp).fillMaxHeight(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
            top = 12.dp, bottom = 52.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    NightWaveMicroLabel("Personalize NightWave")
                    Text("Your space", style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold))
                    Text("Profile, appearance and library protection",
                        style = MaterialTheme.typography.bodySmall, color = muted)
                }
                Surface(shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .13f)) {
                    Text(if (cloud.lastSuccessMs > 0L && cloud.enabled) "BACKUP ON"
                        else "LOCAL",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(21.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(when (profile.avatarStyle) {
                                "moon" -> "☾"
                                "spark" -> "✦"
                                "prism" -> "◇"
                                "vinyl" -> "◎"
                                "wave" -> "〰"
                                else -> "◉"
                            }, style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column(Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(profile.displayName, style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Guest profile · Saved on this device",
                                style = MaterialTheme.typography.bodySmall, color = muted,
                                maxLines = 1)
                        }
                        OutlinedButton(onClick = {
                            name = profile.displayName
                            editing = !editing
                        }) {
                            Text(if (editing) "Close" else "Edit")
                        }
                    }
                    if (editing) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OutlinedTextField(
                            value = name, onValueChange = { name = it.take(40) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text("Display name") }
                        )
                        Button(onClick = {
                            val updated = name.trim()
                            if (updated.isNotEmpty()) {
                                save(profile.copy(displayName = updated))
                                editing = false
                            }
                        }, modifier = Modifier.fillMaxWidth().height(44.dp),
                            enabled = name.trim().isNotEmpty() &&
                                name.trim() != profile.displayName) {
                            Text("Save profile")
                        }
                    }
                }
            }
        }

        item {
            StudioProfileGroup("Appearance & playback") {
                StudioProfileRow(
                    "Theme", profile.theme.name.lowercase().replaceFirstChar { it.titlecase() },
                    Icons.Outlined.AutoAwesome, { showAppearance = !showAppearance },
                    trailing = { Text(if (showAppearance) "Hide" else "Change",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary) }
                )
                if (showAppearance) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Colour theme", style = MaterialTheme.typography.labelSmall,
                            color = muted)
                        for (options in NightWaveTheme.entries.chunked(2)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                options.forEach { option ->
                                    FilterChip(
                                        selected = profile.theme == option,
                                        onClick = { save(profile.copy(theme = option)) },
                                        label = { Text(option.name.lowercase()
                                            .replaceFirstChar { it.titlecase() }) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Text("Profile icon", style = MaterialTheme.typography.labelSmall,
                            color = muted)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileSettings.allowedAvatars.sorted().forEach { style ->
                                FilterChip(
                                    selected = profile.avatarStyle == style,
                                    onClick = { save(profile.copy(avatarStyle = style)) },
                                    label = { Text(style.replaceFirstChar { it.titlecase() }) }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StudioProfileRow(
                    "Reduced motion", "Minimize screen transitions",
                    Icons.Outlined.Settings,
                    { save(profile.copy(motion =
                        if (profile.motion == MotionPreference.REDUCED)
                            MotionPreference.SYSTEM else MotionPreference.REDUCED)) },
                    trailing = {
                        Switch(checked = profile.motion == MotionPreference.REDUCED,
                            onCheckedChange = {
                                save(profile.copy(motion =
                                    if (it) MotionPreference.REDUCED else MotionPreference.SYSTEM))
                            })
                    }
                )
            }
        }

        item {
            StudioProfileGroup("Your listening snapshot") {
                StudioProfileRow("Recent listening",
                    "Artists and songs in your recent history",
                    Icons.Outlined.LibraryMusic, { showSnapshot = !showSnapshot },
                    trailing = {
                        Text(if (showSnapshot) "Hide" else "View",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    })
                if (showSnapshot) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        ListeningInsightsPanel(insights)
                    }
                }
            }
        }

        item {
            StudioProfileGroup("Listening memory · optional") {
                StudioProfileRow("Listening statistics",
                    if (memory.enabled) "On · Stored privately on this device"
                    else "Off · Opt in only if you choose",
                    Icons.Outlined.AutoAwesome,
                    { showListeningMemory = !showListeningMemory },
                    trailing = {
                        Text(if (showListeningMemory) "Hide" else "View",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    })
                if (showListeningMemory) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListeningMemoryPanel(
                        memory, onMemoryEnabledChange, onMemoryErase, onMemoryRefresh
                    )
                }
            }
        }

        item {
            StudioProfileGroup("Library & backup") {
                StudioProfileRow("Manage backups", "Export, restore and undo edits",
                    Icons.Outlined.LibraryMusic, { showBackup = !showBackup },
                    trailing = { Text(if (showBackup) "Hide" else "Open",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary) })
                if (showBackup) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    StudioProfileRow("Export music library", "Profile, favorites and playlists",
                        Icons.Outlined.LibraryMusic, onExport)
                    StudioProfileRow("Restore from backup", "Replaces collections after confirmation",
                        Icons.Outlined.LibraryMusic, onImport)
                    StudioProfileRow("Undo most recent library edit", "Restore last protected change",
                        Icons.Outlined.ArrowBack, onRecoverLastChange)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("GOOGLE DRIVE BACKUP", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 14.dp, top = 6.dp))
                    Text(if (cloud.account.isBlank()) "Not connected"
                        else "Connected: " + cloud.account,
                        modifier = Modifier.padding(horizontal = 14.dp),
                        style = MaterialTheme.typography.bodySmall)
                    Text(cloud.lastResult + if (cloud.lastSuccessMs > 0L)
                        " · Last verified: " + java.text.DateFormat.getDateTimeInstance()
                            .format(java.util.Date(cloud.lastSuccessMs)) else "",
                        modifier = Modifier.padding(horizontal = 14.dp),
                        style = MaterialTheme.typography.bodySmall, color = muted)
                    StudioProfileRow("Connect Google Drive", "Authorize private app-only backup access",
                        Icons.Outlined.Settings, onCloudConnect)
                    if (cloud.account.isNotBlank()) {
                        StudioProfileRow("Back up now", "Verified, versioned Google Drive snapshot",
                            Icons.Outlined.LibraryMusic, onCloudBackup)
                        StudioProfileRow("Preview cloud restore", "Choose before replacing local collections",
                            Icons.Outlined.LibraryMusic, onCloudPreviewRestore)
                        StudioProfileRow("Compare & merge devices",
                            "Add saved music without replacing local playlists",
                            Icons.Outlined.LibraryMusic, onCloudCompare)
                        StudioProfileRow("Back up listening history",
                            "Off by default; old snapshots keep their previous contents",
                            Icons.Outlined.Settings, { onCloudHistory(!cloud.includeHistory) },
                            trailing = {
                                Switch(checked = cloud.includeHistory,
                                    onCheckedChange = onCloudHistory)
                            })
                        StudioProfileRow("Wi-Fi-only automatic backup",
                            "Daily scheduling after first successful backup",
                            Icons.Outlined.Settings, { onCloudWifi(!cloud.wifiOnly) },
                            trailing = {
                                Switch(checked = cloud.wifiOnly,
                                    onCheckedChange = onCloudWifi)
                            })
                        StudioProfileRow("Disconnect Drive", "Stops backup; snapshots stay in Drive",
                            Icons.Outlined.ArrowBack, onCloudDisconnect)
                    }
                    if (cloudBusy) Text("Connecting or transferring backup…",
                        modifier = Modifier.padding(horizontal = 14.dp),
                        style = MaterialTheme.typography.bodySmall, color = muted)
                    Text("Backups contain metadata, never offline audio or passwords. " +
                        "Google consent is optional. Manual compare is not live device sync. " +
                        "Automatic backup may run later than scheduled.",
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodySmall, color = muted)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StudioProfileRow("Advanced storage", "Original library remains authoritative",
                    Icons.Outlined.Settings, { showAdvanced = !showAdvanced },
                    trailing = { Text(if (showAdvanced) "Hide" else "Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary) })
                if (showAdvanced) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(storageStatus, style = MaterialTheme.typography.bodySmall)
                        Text(roomReadStatus, style = MaterialTheme.typography.bodySmall,
                            color = muted)
                        Text("Room and DataStore are optional read-only mirrors. " +
                            "Your existing saved library stays in use.",
                            style = MaterialTheme.typography.bodySmall, color = muted)
                        OutlinedButton(onClick = onCheckStorage,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("RECHECK NEW STORAGE")
                        }
                        OutlinedButton(
                            onClick = if (roomPreviewActive) onUseLegacyReads
                                else onEnableRoomPreview,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (roomPreviewActive) "RETURN TO ORIGINAL LIBRARY"
                                else "TRY VERIFIED ROOM READS")
                        }
                        Text("Cloud recovery requires a Google-authorized, validated snapshot and " +
                            "always asks before replacing local collections.",
                            style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                }
            }
        }

        item {
            StudioProfileGroup("App health") {
                StudioProfileRow("Playback health",
                    if (!playbackState.error.isNullOrBlank() || !playbackState.connected)
                        "Action may be needed · Tap to inspect"
                    else "Current status and recovery controls",
                    Icons.Outlined.Settings,
                    { showPlaybackHealth = !showPlaybackHealth },
                    trailing = {
                        Text(if (showPlaybackHealth) "Hide" else "View",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    })
                if (showPlaybackHealth || !playbackState.connected ||
                    !playbackState.error.isNullOrBlank()) {
                    PlaybackHealthPanel(playbackState, onRecoverPlayback)
                }
                StudioProfileRow("Crash diagnostics", "Preview before sharing any report",
                    Icons.Outlined.Settings, onShareDiagnostics)
            }
        }
        item {
            StudioProfileGroup("Interface") {
                StudioProfileRow("Open classic interface", "Temporary fallback · Shared library",
                    Icons.Outlined.ArrowBack, onOpenClassic)
            }
        }
        item {
            Text("NightWave · Your music stays on this device unless you choose to export it.",
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}

@Composable
private fun StudioProfileGroup(
    title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(title, modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun StudioProfileRow(
    title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2)
        }
        if (trailing != null) trailing()
        else Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge)
    }
}
