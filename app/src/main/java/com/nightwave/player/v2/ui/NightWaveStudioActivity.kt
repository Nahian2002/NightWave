package com.nightwave.player.v2.ui

import android.app.AlertDialog
import android.app.Dialog
import android.widget.EditText
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nightwave.player.v2.data.RoomMirrorRepository
import com.nightwave.player.v2.data.GuestSettingsMirror
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.ui.MainActivity
import com.nightwave.player.ui.YouTubeSearchBrowser
import com.nightwave.player.v2.data.GuestProfileStore
import com.nightwave.player.v2.playback.StudioPlaybackBridge
import com.nightwave.player.v2.playback.StudioPlayerState
import com.nightwave.player.v2.backup.GuestBackupRepository
import com.nightwave.player.v2.backup.BackupFormat
import com.nightwave.player.v2.backup.ImportStrategy
import com.nightwave.player.v2.model.ListeningPrivacy
import com.nightwave.player.v2.model.MotionPreference
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.model.ProfileSettings

/**
 * NightWave 2.0 design-preview shell.
 * Legacy search/player remains the production route until its real-device tests pass.
 */
class NightWaveStudioActivity : ComponentActivity() {
    private lateinit var profiles: GuestProfileStore
    private lateinit var library: MusicLibrary
    private lateinit var backup: GuestBackupRepository
    private var pendingExport: ByteArray? = null
    private var mirrorJob: Job? = null
    private lateinit var playback: StudioPlaybackBridge
    private var youtubeDialog: Dialog? = null
    private var profile by mutableStateOf(ProfileSettings(localId = "pending"))
    private var favorites by mutableStateOf<List<VideoPreview>>(emptyList())
    private var recentlyPlayed by mutableStateOf<List<VideoPreview>>(emptyList())
    private var names by mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profiles = GuestProfileStore(this)
        library = MusicLibrary(this)
        backup = GuestBackupRepository(this)
        if (backup.hasPendingRecovery()) {
            val recovered = runCatching { backup.recoverPendingRestore() }.getOrDefault(false)
            if (!recovered) showMessage("Previous library restore needs recovery. Do not clear app storage.")
        }
        playback = StudioPlaybackBridge(this)
        profile = profiles.load()
        refresh()
        stageStorageMirror()
        setContent {
            NightWaveMaterialTheme(profile.theme) {
                StudioRoot(
                    profile = profile, favorites = favorites, recent = recentlyPlayed,
                    playlistNames = names, getPlaylist = { library.playlist(it) },
                    playerState = playback.state, playerActions = playback,
                    isFavorite = { url -> library.isFavorite(url) },
                    onToggleFavorite = { video -> library.toggleFavorite(video); refresh() },
                    onSongOptions = { video -> showSongOptions(video) },
                    onSaveCurrentToPlaylist = { saveCurrentToPlaylist() },
                    onExportBackup = { startGuestExport() },
                    onImportBackup = { guestImportPicker.launch(arrayOf("application/json", "application/octet-stream")) },
                    onSaveProfile = { updated ->
                        if (profiles.save(updated)) {
                            profile = profiles.load()
                            stageStorageMirror()
                        }
                    },
                    onSearch = { query ->
                        youtubeDialog?.dismiss()
                        youtubeDialog = YouTubeSearchBrowser(this).show(query) { url ->
                            playback.play(VideoPreview("Loading track…", "YouTube", url, "", 0L))
                        }
                    },
                    onPlay = { video, collection -> playback.play(video, collection) },
                    onLegacyPlayer = { manageLibrary() },
                    onClose = { finish() }
                )
            }
        }
        playback.connect()
    }

    override fun onStart() {
        super.onStart()
        if (::playback.isInitialized) playback.resumeUiUpdates()
    }

    override fun onStop() {
        if (::playback.isInitialized) playback.pauseUiUpdates()
        super.onStop()
    }

    override fun onDestroy() {
        mirrorJob?.cancel()
        youtubeDialog?.dismiss()
        youtubeDialog = null
        if (::playback.isInitialized) playback.release()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (::library.isInitialized) {
            refresh()
            stageStorageMirror()
        }
    }

    private fun stageStorageMirror() {
        if (!::library.isInitialized || !::profiles.isInitialized) return
        mirrorJob?.cancel()
        mirrorJob = lifecycleScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    library.snapshotForBackup() to profiles.load()
                }
                withContext(Dispatchers.IO) {
                    RoomMirrorRepository(applicationContext).stage(snapshot.first)
                    GuestSettingsMirror(applicationContext).stage(snapshot.second)
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (error: Exception) {
                android.util.Log.w("NightWaveStorage", "Staging new storage failed; legacy library kept unchanged.", error)
            }
        }
    }

    private val guestExportPicker = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val bytes = pendingExport
        pendingExport = null
        if (uri == null || bytes == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                        stream.write(bytes)
                        stream.flush()
                    } ?: throw java.io.IOException("Could not open output document.")
                }
                showMessage("NightWave guest backup exported.")
            } catch (error: Exception) {
                showMessage("Backup export failed. Choose another folder.")
            }
        }
    }

    private val guestImportPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val (data, preview) = withContext(Dispatchers.IO) {
                    val raw = contentResolver.openInputStream(uri)?.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val chunk = ByteArray(8192)
                        while (true) {
                            val count = input.read(chunk)
                            if (count == -1) break
                            if (output.size() + count > BackupFormat.MAX_BYTES)
                                throw java.io.IOException("Backup file is too large.")
                            output.write(chunk, 0, count)
                        }
                        output.toByteArray()
                    } ?: throw java.io.IOException("Could not open selected backup.")
                    backup.inspect(raw)
                }
                val description = preview.incomingFavorites.toString() + " liked songs, " +
                    preview.incomingPlaylists + " playlists, " +
                    preview.incomingTracks + " playlist entries" +
                    (if (data.includesHistory) ", " + preview.incomingHistory + " history entries" else "")
                AlertDialog.Builder(this@NightWaveStudioActivity).setTitle("Restore NightWave backup?")
                    .setMessage(description + "\n\nChoose how to handle existing saved songs. " +
                        "No import happens until you confirm.")
                    .setItems(arrayOf(
                        "Merge saved music and restore profile",
                        "Replace saved music and restore profile",
                        "Merge music only (keep profile)"
                    )) { _, action ->
                        val strategy = if (action == 1) ImportStrategy.REPLACE_LIBRARY
                            else ImportStrategy.MERGE_LIBRARY
                        val restoreProfile = action != 2
                        val warning = if (strategy == ImportStrategy.REPLACE_LIBRARY)
                            "Replace local favorites and playlists with the backup? Existing data will be held in a private recovery copy during import."
                        else "Merge this backup with local favorites and playlists? Duplicates will be skipped."
                        AlertDialog.Builder(this@NightWaveStudioActivity).setTitle("Confirm restore")
                            .setMessage(warning)
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Restore") { _, _ ->
                                lifecycleScope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            backup.applyImport(data, strategy, restoreProfile)
                                        }
                                        profile = profiles.load()
                                        refresh()
                                        playback.refreshQueue()
                                        stageStorageMirror()
                                        showMessage("Guest backup restored.")
                                    } catch (error: Exception) {
                                        showMessage(error.message ?: "Import failed; previous data retained.")
                                    }
                                }
                            }.show()
                    }.setNegativeButton("Cancel", null).show()
            } catch (error: Exception) {
                showMessage("Cannot read backup: " + (error.message ?: "invalid document").take(120))
            }
        }
    }

    private fun startGuestExport() {
        lifecycleScope.launch {
            try {
                pendingExport = withContext(Dispatchers.IO) {
                    backup.createExport(profile.historyPrivacy == ListeningPrivacy.INCLUDE_IN_BACKUP)
                }
                guestExportPicker.launch("NightWave-Guest-Backup-" +
                    java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date()) + ".json")
            } catch (error: Exception) {
                pendingExport = null
                showMessage("Cannot prepare backup: " + (error.message ?: "unknown error").take(100))
            }
        }
    }

    private fun saveCurrentToPlaylist() {
        val now = playback.state
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

    private fun refresh() {
        favorites = library.favorites()
        recentlyPlayed = library.recent()
        names = library.playlistNames()
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
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    playerState: StudioPlayerState,
    playerActions: StudioPlaybackBridge,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (VideoPreview) -> Unit,
    onSongOptions: (VideoPreview) -> Unit,
    onSaveCurrentToPlaylist: () -> Unit,
    onSearch: (String) -> Unit,
    onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onLegacyPlayer: () -> Unit,
    onClose: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(StudioTab.HOME) }
    var selectedPlaylist by rememberSaveable { mutableStateOf<String?>(null) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                NightWaveWordmark()
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Return to current player",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        bottomBar = {
            Column {
            StudioMiniPlayer(playerState, playerActions, onExpand = { expanded = true })
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                for (item in StudioTab.entries) {
                    val image = when (item) {
                        StudioTab.HOME -> Icons.Outlined.Home
                        StudioTab.SEARCH -> Icons.Outlined.Search
                        StudioTab.LIBRARY -> Icons.Outlined.LibraryMusic
                        StudioTab.PROFILE -> Icons.Outlined.PersonOutline
                    }
                    NavigationBarItem(
                        selected = tab == item, onClick = {
                            tab = item
                            selectedPlaylist = null
                        },
                        icon = { Icon(image, contentDescription = item.caption) },
                        label = { Text(item.caption, style = MaterialTheme.typography.labelSmall) },
                        alwaysShowLabel = true
                    )
                }
            }
            }
        }
    ) { safe ->
        Crossfade(targetState = tab,
            animationSpec = tween(durationMillis = if (profile.motion == MotionPreference.REDUCED) 0 else 220),
            label = "NightWave navigation") { screen ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when (screen) {
            StudioTab.HOME -> StudioHome(
                modifier = Modifier.padding(safe), profile = profile, recent = recent,
                favorites = favorites, onSearch = onSearch, onPlay = onPlay,
                onSongOptions = onSongOptions,
                openLibrary = { tab = StudioTab.LIBRARY }
            )
            StudioTab.SEARCH -> StudioSearch(Modifier.padding(safe), onSearch)
            StudioTab.LIBRARY -> StudioLibrary(
                modifier = Modifier.padding(safe),
                favorites = favorites, names = playlistNames,
                selectedPlaylist = selectedPlaylist, onSelectPlaylist = { selectedPlaylist = it },
                getPlaylist = getPlaylist, onPlay = onPlay,
                onSongOptions = onSongOptions,
                onManage = onLegacyPlayer, onSearch = { tab = StudioTab.SEARCH }
            )
            StudioTab.PROFILE -> StudioProfile(
                modifier = Modifier.padding(safe), profile = profile,
                save = onSaveProfile, onExportBackup = onExportBackup,
                onImportBackup = onImportBackup
            )
        }
        }
        }
    }
    StudioNowPlayingOverlay(
        expanded = expanded && playerState.hasTrack,
        state = playerState, actions = playerActions,
        reduceMotion = profile.motion == MotionPreference.REDUCED,
        liked = playerState.hasTrack && isFavorite(playerState.videoUrl),
        onLike = {
            if (playerState.hasTrack) onToggleFavorite(VideoPreview(
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
        onSelect = { index -> playerActions.jumpTo(index); showQueue = false }
    )
    }
}

@Composable
private fun StudioHome(
    modifier: Modifier, profile: ProfileSettings, recent: List<VideoPreview>,
    favorites: List<VideoPreview>, onSearch: (String) -> Unit,
    onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onSongOptions: (VideoPreview) -> Unit, openLibrary: () -> Unit
) {
    LazyColumn(
        modifier = modifier.widthIn(max = 840.dp).fillMaxHeight(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        item {
            SectionHeading(
                "Good to see you, " + profile.displayName.substringBefore(' ').take(18) + ".",
                "Set the mood. We'll handle the atmosphere."
            )
        }
        item {
            MusicFeatureCard(
                "CURATED BY YOU", "Every mood.\nOne place.",
                "Your library and the sounds you love, beautifully together."
            ) { onSearch("night drive music") }
        }
        item { SectionHeading("Explore by mood", "Tap a collection to search real music.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MoodTile("After dark", "Night drive music",
                    listOf(Color(0xFF604780), Color(0xFF303B75)),
                    Modifier.weight(1f)) { onSearch("night drive music") }
                MoodTile("Slow mornings", "Lofi / acoustic",
                    listOf(Color(0xFF1C6B70), Color(0xFF354D84)),
                    Modifier.weight(1f)) { onSearch("lofi acoustic music") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MoodTile("Desi favorites", "Bangla / Hindi",
                    listOf(Color(0xFF945578), Color(0xFF57325B)),
                    Modifier.weight(1f)) { onSearch("popular bangla hindi songs") }
                MoodTile("Move faster", "Workout energy",
                    listOf(Color(0xFF9A6E3B), Color(0xFF68517D)),
                    Modifier.weight(1f)) { onSearch("workout music") }
            }
        }
        item { SectionHeading("Back in rotation", "Recently played on this device") }
        if (recent.isEmpty()) item {
            EmptyMusicCard("Start your first session",
                "Your actual listening history will appear here as you play.",
                "Find a song") { onSearch("music") }
        } else items(recent.take(6), key = { it.url }) { song ->
            MusicTrackTile(song, onOptions = { onSongOptions(song) }, onClick = { onPlay(song, recent) })
        }
        item { SectionHeading("Liked by you", favorites.size.toString() + " saved tracks") }
        item {
            OutlinedButton(onClick = openLibrary, modifier = Modifier.fillMaxWidth().height(51.dp)) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("OPEN YOUR LIBRARY")
            }
        }
    }
}

@Composable
private fun MoodTile(title: String, subtitle: String, gradient: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(152.dp),
        shape = RoundedCornerShape(21.dp)) {
        Column(Modifier.fillMaxSize().background(Brush.linearGradient(gradient)).padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White)
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
private fun StudioSearch(modifier: Modifier, onSearch: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val submit = { if (query.isNotBlank()) onSearch(query.trim()) }
    LazyColumn(modifier.widthIn(max = 840.dp).fillMaxHeight(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SectionHeading("What are we playing?", "Find music by name, artist or mood.") }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(160) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                singleLine = true, placeholder = { Text("Search any song or artist") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() })
            )
        }
        item {
            Button(onClick = submit, enabled = query.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("SEARCH YOUTUBE")
            }
        }
        item {
            EmptyMusicCard(
                "Search without exact links",
                "The existing playback engine remains in charge. This phase opens the current YouTube search flow until native search is verified.",
                "Find your sound"
            ) { onSearch(if (query.isBlank()) "music" else query) }
        }
    }
}

@Composable
private fun StudioLibrary(
    modifier: Modifier, favorites: List<VideoPreview>, names: List<String>,
    selectedPlaylist: String?, onSelectPlaylist: (String?) -> Unit,
    getPlaylist: (String) -> List<VideoPreview>, onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onManage: () -> Unit, onSearch: () -> Unit,
    onSongOptions: (VideoPreview) -> Unit
) {
    val tracks = if (selectedPlaylist == null || selectedPlaylist == LIKED_COLLECTION) favorites else getPlaylist(selectedPlaylist)
    LazyColumn(modifier.widthIn(max = 840.dp).fillMaxHeight(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedPlaylist != null) IconButton(onClick = { onSelectPlaylist(null) }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to Library")
                }
                Column {
                    SectionHeading(if (selectedPlaylist == LIKED_COLLECTION) "Liked Songs" else selectedPlaylist ?: "Your music.",
                        if (selectedPlaylist == null) "Every collection, under one roof."
                        else tracks.size.toString() + " songs in this playlist")
                }
            }
        }
        if (selectedPlaylist == null) {
            item {
                MusicFeatureCard("YOUR PRIVATE COLLECTION",
                    "Liked songs.", favorites.size.toString() + " tracks saved locally") {
                    onSelectPlaylist(LIKED_COLLECTION)
                }
            }
            if (names.isNotEmpty()) {
                item { SectionHeading("Playlists", "Tap to explore your saved songs") }
                items(names, key = { it }) { name ->
                    Card(onClick = { onSelectPlaylist(name) }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LibraryMusic, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(15.dp))
                            Column {
                                Text(name, style = MaterialTheme.typography.titleMedium)
                                Text(getPlaylist(name).size.toString() + " tracks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeading(if (selectedPlaylist == null) "Liked tracks" else "Playlist tracks") }
        if (tracks.isEmpty()) item {
            EmptyMusicCard("Nothing here yet",
                "Your real songs and playlists will appear here once saved.",
                "Search music", onAction = onSearch)
        } else items(tracks, key = { it.url }) { song ->
            MusicTrackTile(song, onOptions = { onSongOptions(song) }, onClick = { onPlay(song, tracks) })
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
    onExportBackup: () -> Unit, onImportBackup: () -> Unit
) {
    var name by rememberSaveable(profile.localId) { mutableStateOf(profile.displayName) }
    LazyColumn(modifier.widthIn(max = 840.dp).fillMaxHeight(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SectionHeading("Make it yours.", "Your guest profile is saved on this phone.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(23.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(67.dp).background(MaterialTheme.colorScheme.primary,
                        CircleShape), contentAlignment = Alignment.Center) {
                        Text(when (profile.avatarStyle) {
                            "moon" -> "☾"
                            "spark" -> "✦"
                            "prism" -> "◇"
                            "vinyl" -> "◎"
                            "wave" -> "〰"
                            else -> "◉"
                        }, style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
                        Text("GUEST PROFILE · ON THIS DEVICE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            OutlinedTextField(value = name, onValueChange = { name = it.take(40) },
                modifier = Modifier.fillMaxWidth(), label = { Text("Display name") },
                singleLine = true)
        }
        item {
            Button(onClick = { save(profile.copy(displayName = name)) },
                modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("SAVE PROFILE NAME")
            }
        }
        item { SectionHeading("Profile icon", "Choose a look without uploading personal photos") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (styles in ProfileSettings.allowedAvatars.sorted().chunked(3)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        for (style in styles) {
                            FilterChip(
                                selected = profile.avatarStyle == style,
                                onClick = { save(profile.copy(avatarStyle = style)) },
                                label = { Text(style.replaceFirstChar { it.titlecase() }) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        item { SectionHeading("App appearance", "Your chosen theme persists across restarts") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                for (options in NightWaveTheme.entries.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (option in options) {
                            FilterChip(selected = profile.theme == option,
                                onClick = { save(profile.copy(theme = option)) },
                                label = { Text(option.name.lowercase().replaceFirstChar { it.titlecase() }) },
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item { SectionHeading("Motion and comfort") }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Reduced animations", style = MaterialTheme.typography.titleMedium)
                    Text("Prefer fewer moving effects",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = profile.motion == MotionPreference.REDUCED,
                    onCheckedChange = { save(profile.copy(motion = if (it) MotionPreference.REDUCED else MotionPreference.SYSTEM)) })
            }
        }
        item { SectionHeading("Guest backup and recovery", "Export your saved library before uninstalling NightWave") }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Include listening history", style = MaterialTheme.typography.titleMedium)
                    Text("Optional. Off by default for privacy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = profile.historyPrivacy == ListeningPrivacy.INCLUDE_IN_BACKUP,
                    onCheckedChange = { enabled ->
                        save(profile.copy(historyPrivacy = if (enabled)
                            ListeningPrivacy.INCLUDE_IN_BACKUP else ListeningPrivacy.DEVICE_ONLY))
                    })
            }
        }
        item {
            Button(onClick = onExportBackup, modifier = Modifier.fillMaxWidth().height(53.dp)) {
                Text("EXPORT GUEST BACKUP")
            }
        }
        item {
            OutlinedButton(onClick = onImportBackup, modifier = Modifier.fillMaxWidth().height(53.dp)) {
                Text("IMPORT SAVED BACKUP")
            }
        }
        item {
            Text("Backup is a local JSON file. Save it to a safe location; uninstalling the app does not automatically restore it. Google sync is not yet enabled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(19.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Google recovery — planned", style = MaterialTheme.typography.titleMedium)
                    Text("This guest profile is stored locally. Google sign-in and private Drive recovery will be added after migration and restore tests. No account is connected yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
