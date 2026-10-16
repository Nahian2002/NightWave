package com.nightwave.player.v2.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
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
import com.nightwave.player.v2.data.GuestProfileStore
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
    private var profile by mutableStateOf(ProfileSettings(localId = "pending"))
    private var favorites by mutableStateOf<List<VideoPreview>>(emptyList())
    private var recentlyPlayed by mutableStateOf<List<VideoPreview>>(emptyList())
    private var names by mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profiles = GuestProfileStore(this)
        library = MusicLibrary(this)
        profile = profiles.load()
        refresh()
        setContent {
            NightWaveMaterialTheme(profile.theme) {
                StudioRoot(
                    profile = profile, favorites = favorites, recent = recentlyPlayed,
                    playlistNames = names, getPlaylist = { library.playlist(it) },
                    onSaveProfile = { updated ->
                        if (profiles.save(updated)) profile = profiles.load()
                    },
                    onSearch = { query ->
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            putExtra("nightwave_web_search", query)
                        })
                    },
                    onPlay = { video ->
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, video.url)
                        })
                    },
                    onLegacyPlayer = { startActivity(Intent(this, MainActivity::class.java)) },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::library.isInitialized) refresh()
    }

    private fun refresh() {
        favorites = library.favorites()
        recentlyPlayed = library.recent()
        names = library.playlistNames()
    }
}

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
    onSearch: (String) -> Unit,
    onPlay: (VideoPreview) -> Unit,
    onLegacyPlayer: () -> Unit,
    onClose: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(StudioTab.HOME) }
    var selectedPlaylist by rememberSaveable { mutableStateOf<String?>(null) }
    val accent = MaterialTheme.colorScheme.primary
    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
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
    ) { safe ->
        when (tab) {
            StudioTab.HOME -> StudioHome(
                modifier = Modifier.padding(safe), profile = profile, recent = recent,
                favorites = favorites, onSearch = onSearch, onPlay = onPlay,
                openLibrary = { tab = StudioTab.LIBRARY }
            )
            StudioTab.SEARCH -> StudioSearch(Modifier.padding(safe), onSearch)
            StudioTab.LIBRARY -> StudioLibrary(
                modifier = Modifier.padding(safe),
                favorites = favorites, names = playlistNames,
                selectedPlaylist = selectedPlaylist, onSelectPlaylist = { selectedPlaylist = it },
                getPlaylist = getPlaylist, onPlay = onPlay,
                onManage = onLegacyPlayer, onSearch = { tab = StudioTab.SEARCH }
            )
            StudioTab.PROFILE -> StudioProfile(
                modifier = Modifier.padding(safe), profile = profile,
                save = onSaveProfile
            )
        }
    }
}

@Composable
private fun StudioHome(
    modifier: Modifier, profile: ProfileSettings, recent: List<VideoPreview>,
    favorites: List<VideoPreview>, onSearch: (String) -> Unit,
    onPlay: (VideoPreview) -> Unit, openLibrary: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
            MusicTrackTile(song, onClick = { onPlay(song) })
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
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
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
    getPlaylist: (String) -> List<VideoPreview>, onPlay: (VideoPreview) -> Unit,
    onManage: () -> Unit, onSearch: () -> Unit
) {
    val tracks = if (selectedPlaylist == null) favorites else getPlaylist(selectedPlaylist)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedPlaylist != null) IconButton(onClick = { onSelectPlaylist(null) }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to Library")
                }
                Column {
                    SectionHeading(selectedPlaylist ?: "Your music.",
                        if (selectedPlaylist == null) "Every collection, under one roof."
                        else tracks.size.toString() + " songs in this playlist")
                }
            }
        }
        if (selectedPlaylist == null) {
            item {
                MusicFeatureCard("YOUR PRIVATE COLLECTION",
                    "Liked songs.", favorites.size.toString() + " tracks saved locally") {
                    onSelectPlaylist(null)
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
            MusicTrackTile(song, onClick = { onPlay(song) })
        }
        item {
            OutlinedButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                Text("Manage playlists in the current player")
            }
        }
    }
}

@Composable
private fun StudioProfile(modifier: Modifier, profile: ProfileSettings, save: (ProfileSettings) -> Unit) {
    var name by rememberSaveable(profile.localId) { mutableStateOf(profile.displayName) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SectionHeading("Make it yours.", "Your guest profile is saved on this phone.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(23.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(67.dp).background(MaterialTheme.colorScheme.primary,
                        CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.PersonOutline, contentDescription = "Guest avatar",
                            tint = MaterialTheme.colorScheme.onPrimary)
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
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
        item {
            EmptyMusicCard("Google recovery comes later",
                "Google sign-in and private backup will be integrated only after the storage migration and restore tests pass. No account connection is active yet.",
                "Guest mode is ready") { }
        }
    }
}
