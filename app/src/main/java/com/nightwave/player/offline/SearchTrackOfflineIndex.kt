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
        val orderedKeys = orderedUrls.take(OfflineSearchQueuePolicy.MAX_IDS * 2)
            .map { key(it) }
        fun linkedId(trackKey: String?): String? =
            trackKey?.let { savedLinks[it] as? String }
        val selectedLink = linkedId(selectedKey) ?: return null
        val linked = orderedKeys.map(::linkedId)
        val requested = (linked.filterNotNull() + selectedLink).toSet()
        // One guarded index parse + only the relevant file-health inspections.
        // No repair/deletion occurs during a lookup, even for stale mappings.
        val available = OfflineAudioLibrary(context).availableIds(requested)
        val selectedId = selectedLink.takeIf { it in available } ?: return null
        val list = linked.map { it?.takeIf(available::contains) }
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
