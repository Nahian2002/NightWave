package com.nightwave.player.v2.ui

import android.app.AlertDialog
import android.app.Dialog
import android.widget.EditText
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
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
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.net.YouTubeGateway
import com.nightwave.player.net.VideoSearchResults
import com.nightwave.player.net.SearchMode
import com.nightwave.player.net.SavedTrackSearch
import com.nightwave.player.ui.MainActivity
import com.nightwave.player.ui.YouTubeSearchBrowser
import com.nightwave.player.v2.data.GuestProfileStore
import com.nightwave.player.v2.data.ProfileDataStoreMirror
import com.nightwave.player.v2.data.room.NightWaveDatabase
import com.nightwave.player.v2.data.room.RoomShadowMigrator
import com.nightwave.player.v2.data.room.VerifiedRoomReader
import com.nightwave.player.v2.playback.StudioPlaybackBridge
import com.nightwave.player.v2.playback.StudioPlayerState
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
    val error: String = ""
)

/**
 * Default NightWave 2.0 application surface. The original UI remains an
 * explicit, reversible fallback in Profile and keeps the same local library.
 */
class NightWaveStudioActivity : ComponentActivity() {
    private lateinit var profiles: GuestProfileStore
    private lateinit var library: MusicLibrary
    private lateinit var playback: StudioPlaybackBridge
    private lateinit var archiveManager: LibraryArchiveManager
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
    private var offlineResumeTitle by mutableStateOf<String?>(null)
    private var offlineResumePositionMs by mutableStateOf(0L)
    private var names by mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profiles = GuestProfileStore(this)
        library = MusicLibrary(this)
        playback = StudioPlaybackBridge(this)
        archiveManager = LibraryArchiveManager(this)
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
            LaunchedEffect(playback.state.videoUrl) {
                if (playback.state.videoUrl.isNotBlank()) refresh()
            }
            NightWaveMaterialTheme(profile.theme) {
                StudioRoot(
                    profile = profile, favorites = favorites, recent = recentlyPlayed,
                    playlistNames = names, getPlaylist = { name ->
                        if (roomPreviewActive) roomPreviewPlaylists[name] ?: emptyList()
                        else library.playlist(name)
                    },
                    playerState = playback.state, playerActions = playback,
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
                    onSaveProfile = { updated ->
                        if (profiles.save(updated)) {
                            profile = profiles.load()
                            refreshShadows()
                        }
                    },
                    searchState = nativeSearchState,
                    onSearch = { query, mode -> nativeSearch(query, mode) },
                    onWebSearch = { query ->
                        youtubeDialog?.dismiss()
                        youtubeDialog = YouTubeSearchBrowser(this).show(query) { url ->
                            playback.play(VideoPreview("Loading track…", "YouTube", url, "", 0L))
                        }
                    },
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
            nativeSearch(shared, SearchMode.SONGS)
        }
    }

    private fun nativeSearch(query: String, mode: SearchMode) {
        val clean = query.trim().take(160)
        if (clean.isBlank()) return
        val request = ++nativeSearchGeneration
        nativeSearchJob?.cancel()
        nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode, loading = true)
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
                        error = "Native results are unavailable right now. Use YouTube search below.")
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
        if (::roomShadow.isInitialized && ::profileShadow.isInitialized &&
            ::verifiedReader.isInitialized) refreshShadows()
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
    playlistNames: List<String>,
    getPlaylist: (String) -> List<VideoPreview>,
    onSaveProfile: (ProfileSettings) -> Unit,
    onExportGuest: () -> Unit,
    onImportGuest: () -> Unit,
    onRecoverLastChange: () -> Unit,
    storageStatus: String,
    onCheckStorage: () -> Unit,
    roomReadStatus: String,
    roomPreviewActive: Boolean,
    onEnableRoomPreview: () -> Unit,
    onUseLegacyReads: () -> Unit,
    playerState: StudioPlayerState,
    playerActions: StudioPlaybackBridge,
    offlineResumeTitle: String?,
    offlineResumePositionMs: Long,
    onResumeOffline: () -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (VideoPreview) -> Unit,
    onSongOptions: (VideoPreview) -> Unit,
    onSaveCurrentToPlaylist: () -> Unit,
    onSearch: (String, SearchMode) -> Unit,
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
            Row(Modifier.fillMaxWidth().height(60.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                NightWaveWordmark()
                IconButton(onClick = { navigateToTab(StudioTab.PROFILE) }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Open NightWave settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Surface(onClick = { compactHidden = false },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(36.dp)) {
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
                Surface(color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
                    Row(
                        Modifier.fillMaxWidth().height(65.dp)
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (item in StudioTab.entries) {
                            val image = when (item) {
                                StudioTab.HOME -> Icons.Outlined.Home
                                StudioTab.SEARCH -> Icons.Outlined.Search
                                StudioTab.LIBRARY -> Icons.Outlined.LibraryMusic
                                StudioTab.PROFILE -> Icons.Outlined.PersonOutline
                            }
                            val selected = tab == item
                            val tabColor by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                                    else Color.Transparent,
                                animationSpec = tween(durationMillis =
                                    if (profile.motion == MotionPreference.REDUCED) 0 else 180),
                                label = "Navigation selection"
                            )
                            Column(
                                Modifier.weight(1f).fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(tabColor)
                                    .clickable { navigateToTab(item) }
                                    .semantics { this.selected = selected }
                                    .testTag("nightwave-tab-" + item.name.lowercase()),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(image, contentDescription = item.caption,
                                    modifier = Modifier.size(19.dp),
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(item.caption, style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
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
                        playingUrl = playerState.videoUrl.takeIf { playerState.playing }.orEmpty(),
                        onSearch = { query ->
                            onSearch(query, SearchMode.SONGS)
                            navigateToTab(StudioTab.SEARCH)
                        },
                        onPlay = onPlay, onSongOptions = onSongOptions,
                        openLibrary = { navigateToTab(StudioTab.LIBRARY) }
                    )
                    StudioTab.SEARCH -> StudioSearch(
                        Modifier, searchState, onSearch, onWebSearch,
                        onPlay, onSongOptions,
                        playerState.videoUrl.takeIf { playerState.playing }.orEmpty(),
                        savedTracks = remember(favorites, recent, playlistNames) {
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
                        storageStatus = storageStatus, onCheckStorage = onCheckStorage,
                        roomReadStatus = roomReadStatus,
                        roomPreviewActive = roomPreviewActive,
                        onEnableRoomPreview = onEnableRoomPreview,
                        onUseLegacyReads = onUseLegacyReads,
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
    favorites: List<VideoPreview>, playingUrl: String, onSearch: (String) -> Unit,
    onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onSongOptions: (VideoPreview) -> Unit, openLibrary: () -> Unit
) {
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
        contentPadding = PaddingValues(horizontal = 19.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeading("Good to have you back",
                profile.displayName.substringBefore(' ').take(20) + " · Your listening space")
        }
        item {
            ListeningSpotlight(
                lastTrack = recent.firstOrNull(),
                onResume = { recent.firstOrNull()?.let { onPlay(it, recent) } },
                onDiscover = { onSearch("night drive music") }
            )
        }
        item { SectionHeading("Find your frequency", "Six directions. The next track is yours to choose.") }
        DiscoveryShortcuts.picks.chunked(2).forEachIndexed { row, shortcuts ->
            item(key = "discovery-$row") {
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    shortcuts.forEachIndexed { i, shortcut ->
                        MoodTile(shortcut.title, shortcut.subtitle,
                            palettes[row * 2 + i], Modifier.weight(1f)) {
                            onSearch(shortcut.query)
                        }
                    }
                }
            }
        }
        item { SectionHeading("Listen again", "From your recent plays") }
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
    Card(onClick = onClick, modifier = modifier.height(108.dp),
        shape = RoundedCornerShape(19.dp)) {
        Column(Modifier.fillMaxSize().background(Brush.linearGradient(gradient)).padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White.copy(alpha = .82f),
                modifier = Modifier.size(18.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = .84f))
            }
        }
    }
}

@Composable
private fun StudioSearch(
    modifier: Modifier,
    state: StudioSearchSnapshot,
    onNativeSearch: (String, SearchMode) -> Unit,
    onWebSearch: (String) -> Unit,
    onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onSongOptions: (VideoPreview) -> Unit,
    playingUrl: String,
    savedTracks: List<VideoPreview>
) {
    var query by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(SearchMode.SONGS) }
    LaunchedEffect(state.query) { if (state.query.isNotBlank()) query = state.query }
    val submit = { if (query.isNotBlank()) onNativeSearch(query.trim(), mode) }
    val savedMatches = remember(query, savedTracks) {
        SavedTrackSearch.match(query, savedTracks)
    }
    LazyColumn(
        modifier.fillMaxWidth().widthIn(max = 740.dp).fillMaxHeight(),
        contentPadding = PaddingValues(horizontal = 19.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeading("Explore sound", "Search for songs, artists and public videos") }
        if (query.isBlank() && state.videos.isEmpty() && !state.loading) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("START SOMEWHERE", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DiscoveryShortcuts.picks.take(4).chunked(2).forEach { choices ->
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            choices.forEach { shortcut ->
                                FilterChip(selected = false,
                                    onClick = {
                                        mode = SearchMode.SONGS
                                        query = shortcut.query
                                        onNativeSearch(shortcut.query, SearchMode.SONGS)
                                    },
                                    label = { Text(shortcut.title, maxLines = 1) },
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(160) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp),
                singleLine = true, placeholder = { Text("Song or artist name") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() })
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                FilterChip(selected = mode == SearchMode.SONGS,
                    onClick = { mode = SearchMode.SONGS }, label = { Text("Songs") })
                FilterChip(selected = mode == SearchMode.VIDEOS,
                    onClick = { mode = SearchMode.VIDEOS }, label = { Text("Videos") })
            }
        }
        item {
            Button(onClick = submit, enabled = query.isNotBlank() && !state.loading,
                modifier = Modifier.fillMaxWidth().height(49.dp),
                shape = RoundedCornerShape(15.dp)) {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                }
                Text(if (state.loading) "Searching…" else "Find music")
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
        if (state.loading) {
            item {
                Text("Checking public video search… Your saved songs remain available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (state.videos.isNotEmpty()) {
            item { SectionHeading("Public results",
                state.videos.size.toString() + " tracks · " + state.provider) }
            items(state.videos, key = { "public:" + it.url }) { song ->
                MusicTrackTile(song, isPlaying = playingUrl == song.url,
                    onOptions = { onSongOptions(song) },
                    onClick = { onPlay(song, state.videos) })
            }
        } else if (state.error.isNotBlank()) {
            item {
                Column(Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Online search is unavailable",
                        style = MaterialTheme.typography.titleMedium)
                    Text(if (savedMatches.isNotEmpty())
                        "Your saved matches are above. You can retry public search or open YouTube."
                        else "Public search didn't return results. Retry or search YouTube directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = submit, enabled = query.isNotBlank()) {
                            Text("Retry")
                        }
                        OutlinedButton(onClick = {
                            onWebSearch(query.ifBlank { state.query })
                        }, enabled = query.isNotBlank()) {
                            Text("YouTube ↗")
                        }
                    }
                }
            }
        } else if (state.query.isNotBlank() && savedMatches.isEmpty()) {
            item {
                Text("No matches yet. Try a different artist or open YouTube search.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.error.isBlank() && query.isNotBlank()) {
            item {
                OutlinedButton(onClick = { onWebSearch(query.trim()) },
                    modifier = Modifier.fillMaxWidth().height(45.dp)) {
                    Text("Search on YouTube ↗")
                }
            }
        }
        item {
            Text("Saved songs work without public search. YouTube availability and playback can change.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onSongOptions: (VideoPreview) -> Unit
) {
    val tracks = if (selectedPlaylist == null || selectedPlaylist == LIKED_COLLECTION)
        favorites else getPlaylist(selectedPlaylist)
    var localFilter by rememberSaveable(selectedPlaylist) { mutableStateOf("") }
    val matched = if (selectedPlaylist == null || localFilter.isBlank()) tracks else tracks.filter {
        it.title.contains(localFilter.trim(), ignoreCase = true) ||
            it.author.contains(localFilter.trim(), ignoreCase = true)
    }
    val visibleTracks = if (selectedPlaylist == null) matched.take(5) else matched
    LazyColumn(modifier.fillMaxWidth().widthIn(max = 740.dp).fillMaxHeight(),
        contentPadding = PaddingValues(19.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedPlaylist != null) IconButton(onClick = { onSelectPlaylist(null) }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to Library")
                }
                Column {
                    SectionHeading(if (selectedPlaylist == LIKED_COLLECTION) "Liked Songs" else selectedPlaylist ?: "Your library",
                        if (selectedPlaylist == null) "Your music, all in one place"
                        else tracks.size.toString() + " saved tracks")
                }
            }
        }
        if (selectedPlaylist == null) {
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
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
                )
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
                onClick = { onPlay(song, if (selectedPlaylist == null) tracks else matched) })
        }
        item {
            OutlinedButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                Text("CREATE, RENAME OR DELETE PLAYLISTS")
            }
        }
    }
}

@Composable
private fun StudioProfile(
    modifier: Modifier, profile: ProfileSettings, save: (ProfileSettings) -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, onRecoverLastChange: () -> Unit,
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
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(
        modifier = modifier.widthIn(max = 720.dp).fillMaxHeight(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Your space", style = MaterialTheme.typography.headlineSmall)
                    Text("Profile, listening and preferences",
                        style = MaterialTheme.typography.bodySmall, color = muted)
                }
                Text("LOCAL", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    Text("Backups do not include audio files or passwords. Cloud sync is not active.",
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
                        Text("Cloud recovery is not available yet.",
                            style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                }
            }
        }

        item {
            StudioProfileGroup("App health") {
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
