package com.nightwave.player.offline

import android.content.Context
import com.nightwave.player.core.VideoLink

/**
 * Maps a canonical search result to a fully validated app-private OfflineSong.
 * Bookmarking a YouTube URL never creates this association. No original
 * streaming URL, OAuth token or signed CDN URL is persisted.
 */
object SearchTrackOfflineIndex {
    private const val PREFS = "nightwave_search_offline_v1"

    private fun key(url: String): String? = VideoLink.normalize(url)
        ?.substringAfter("v=")?.takeIf { it.length == 11 }
        ?.let { "v_$it" }

    fun readySong(context: Context, videoUrl: String): OfflineSong? {
        val key = key(videoUrl) ?: return null
        val prefs = context.applicationContext.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(key, null) ?: return null
        val library = OfflineAudioLibrary(context)
        return runCatching {
            library.find(id)?.takeIf { library.fileFor(it) != null }
        }.getOrNull()
    }

    fun linkVerified(context: Context, videoUrl: String, saved: OfflineSong): Boolean {
        val key = key(videoUrl) ?: return false
        val library = OfflineAudioLibrary(context)
        if (library.find(saved.id) == null || library.fileFor(saved) == null) return false
        val prefs = context.applicationContext.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE)
        // A bounded relation, with no untrusted ID/path supplied by callers.
        if (!prefs.contains(key) && prefs.all.size >= OfflineAudioLibrary.MAX_SONGS) {
            return false
        }
        return prefs.edit().putString(key, saved.id).commit()
    }
}
