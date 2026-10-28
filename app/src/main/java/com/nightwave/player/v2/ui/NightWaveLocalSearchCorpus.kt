package com.nightwave.player.v2.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.net.*
import com.nightwave.player.offline.OfflineAudioLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Primary legacy ownership is unchanged; verified Room preview is not a writer.
 * Observe actual storage edits, including background offline completions.
 */
@Composable
internal fun rememberNightWaveLocalCorpus(context: Context, active: Boolean, refresh: Int): Pair<LocalMusicCorpusState, () -> Unit> {
    val app = context.applicationContext
    val scope = rememberCoroutineScope()
    val controller = remember(app) {
        LocalMusicSearchController(scope) {
            withContext(Dispatchers.IO) {
                LocalMusicSnapshot(MusicLibrary(app).searchSnapshot(), OfflineAudioLibrary(app).songs())
            }
        }
    }
    DisposableEffect(app, controller) {
        val public = app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
        val offline = app.getSharedPreferences("nightwave_offline_v1", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { source, key ->
            if ((source === offline && (key == null || key == "items")) ||
                (source === public && (key == null || key in setOf("favorites", "recent", "playlist_names") || key.startsWith("list_")))) {
                controller.invalidate()
            }
        }
        public.registerOnSharedPreferenceChangeListener(listener)
        offline.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            public.unregisterOnSharedPreferenceChangeListener(listener)
            offline.unregisterOnSharedPreferenceChangeListener(listener)
            controller.close()
        }
    }
    val owner = LocalContext.current as? LifecycleOwner
    DisposableEffect(controller, owner, active, refresh) {
        fun configure() = controller.configure(active &&
            (owner == null || owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)), refresh)
        val observer = LifecycleEventObserver { _, _ -> configure() }
        owner?.lifecycle?.addObserver(observer)
        configure()
        onDispose { owner?.lifecycle?.removeObserver(observer); controller.configure(false, refresh) }
    }
    val state by controller.state.collectAsState()
    // A manual Activity refresh must invalidate the visible index in this
    // very composition, not one producer launch/frame later.
    val current = if (state.refresh == refresh) state else LocalMusicCorpusState(state.revision, refresh)
    return current to { controller.invalidate() }
}
