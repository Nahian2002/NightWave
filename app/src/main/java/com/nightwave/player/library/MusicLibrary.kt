package com.nightwave.player.library

import android.content.Context
import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview
import org.json.JSONArray

/**
 * Legacy-compatible public library facade.
 *
 * Every mutation is routed through one in-process writer that commits the
 * entire logical change synchronously. It never uses async apply(), so a
 * playlist rename cannot publish names before its songs have been written.
 * The existing preference file and public method signatures are preserved.
 */
class MusicLibrary(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)

    fun favorites(): List<VideoPreview> = read("favorites")
    fun recent(): List<VideoPreview> = read("recent")
    fun isFavorite(url: String): Boolean = favorites().any { it.url == url }

    fun toggleFavorite(video: VideoPreview): Boolean =
        LegacyLibraryTransaction.mutate(app, LibraryChange.ToggleFavorite(video)).added

    fun addRecent(video: VideoPreview) {
        LegacyLibraryTransaction.mutate(app, LibraryChange.RecordRecent(video))
    }

    fun playlistNames(): List<String> = try {
        val names = JSONArray(prefs.getString("playlist_names", "[]") ?: "[]")
        (0 until names.length()).map { names.optString(it) }.filter { it.isNotBlank() }
    } catch (_: Exception) { emptyList() }

    fun createPlaylist(rawName: String): Boolean =
        LegacyLibraryTransaction.mutate(app, LibraryChange.CreatePlaylist(rawName)).accepted

    fun deletePlaylist(name: String): Boolean =
        LegacyLibraryTransaction.mutate(app, LibraryChange.DeletePlaylist(name)).accepted

    fun renamePlaylist(old: String, newName: String): Boolean =
        LegacyLibraryTransaction.mutate(app, LibraryChange.RenamePlaylist(old, newName)).accepted

    fun playlist(name: String): List<VideoPreview> =
        if (playlistNames().contains(name)) read("list_" + name) else emptyList()

    fun addToPlaylist(name: String, video: VideoPreview): Boolean =
        LegacyLibraryTransaction.mutate(app, LibraryChange.AddToPlaylist(name, video)).accepted

    /** Confirmed import is an all-or-nothing legacy-library transaction. */
    fun importSharedPlaylist(name: String, tracks: List<VideoPreview>): LibraryChangeResult =
        LegacyLibraryTransaction.mutate(app, LibraryChange.ImportSharedPlaylist(name, tracks))

    fun removeFromPlaylist(name: String, url: String): Boolean =
        LegacyLibraryTransaction.mutate(app, LibraryChange.RemoveFromPlaylist(name, url)).accepted

    /** Legacy readers remain functional for old and new saved collections. */
    private fun read(key: String): List<VideoPreview> = try {
        val arr = JSONArray(prefs.getString(key, "[]") ?: "[]")
        (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            val url = VideoLink.normalize(obj.optString("url")) ?: return@mapNotNull null
            VideoPreview(
                obj.optString("title").take(250),
                obj.optString("creator").take(150),
                url,
                obj.optString("artwork").take(2000),
                obj.optLong("duration", 0)
            )
        }
    } catch (_: Exception) { emptyList() }
}
