package com.nightwave.player.offline

import android.content.Context
import com.nightwave.player.core.VideoLink

/**
 * Maps a canonical search result to a fully validated app-private OfflineSong.
 * Bookmarking a YouTube URL never creates this association. No original
 * streaming URL, OAuth token or signed CDN URL is persisted.
 */
data class SearchOfflineSelection(val selectedId: String, val queueIds: ArrayList<String>)

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

    /**
     * Resolve matching saved tracks in one library snapshot to avoid repeated
     * index JSON parsing. Never include a missing or mismatched private file.
     * Search results remain read-only; no playlist or offline file is changed.
     */
    fun offlineSelection(
        context: Context, videoUrl: String, orderedUrls: List<String>
    ): SearchOfflineSelection? {
        val selectedKey = key(videoUrl) ?: return null
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedLinks = prefs.all
        val library = OfflineAudioLibrary(context)
        val indexed = runCatching { library.songs().associateBy(OfflineSong::id) }
            .getOrNull() ?: return null
        val inspected = HashMap<String, Boolean>()
        fun verifiedId(trackKey: String?): String? {
            if (trackKey == null) return null
            val id = savedLinks[trackKey] as? String ?: return null
            val song = indexed[id] ?: return null
            if (inspected.getOrPut(id) { library.fileFor(song) != null }) return id
            return null
        }
        val selectedId = verifiedId(selectedKey) ?: return null
        val list = orderedUrls.take(OfflineSearchQueuePolicy.MAX_IDS * 2)
            .map { verifiedId(key(it)) }
        val ids = OfflineSearchQueuePolicy.plan(selectedId, list)
        return SearchOfflineSelection(selectedId, ArrayList(ids))
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
