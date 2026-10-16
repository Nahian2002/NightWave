package com.nightwave.player.library

import android.content.Context
import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview
import org.json.JSONArray
import org.json.JSONObject

/** Private on-device library. Stores video metadata/URLs, never signed audio stream URLs. */
class MusicLibrary(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)

    fun favorites(): List<VideoPreview> = read("favorites")
    fun recent(): List<VideoPreview> = read("recent")
    fun isFavorite(url: String) = favorites().any { it.url == url }

    fun toggleFavorite(video: VideoPreview): Boolean {
        val items = favorites().toMutableList()
        val existing = items.indexOfFirst { it.url == video.url }
        if (existing >= 0) items.removeAt(existing) else items.add(0, video)
        write("favorites", items.take(500))
        return existing < 0
    }

    fun addRecent(video: VideoPreview) {
        write("recent", (listOf(video) + recent().filter { it.url != video.url }).take(100))
    }

    fun playlistNames(): List<String> = try {
        val names = JSONArray(prefs.getString("playlist_names", "[]") ?: "[]")
        (0 until names.length()).map { names.optString(it) }.filter { it.isNotBlank() }
    } catch (_: Exception) { emptyList() }

    fun createPlaylist(rawName: String): Boolean {
        val name = rawName.trim().take(60)
        if (name.isEmpty() || playlistNames().any { it.equals(name, true) } || playlistNames().size >= 50) return false
        prefs.edit().putString("playlist_names", JSONArray(playlistNames() + name).toString()).apply()
        write("list_" + name, emptyList())
        return true
    }

    fun deletePlaylist(name: String): Boolean {
        val names = playlistNames()
        if (!names.contains(name)) return false
        prefs.edit()
            .putString("playlist_names", JSONArray(names.filter { it != name }).toString())
            .remove("list_" + name)
            .apply()
        return true
    }

    fun renamePlaylist(old: String, newName: String): Boolean {
        val names = playlistNames()
        val nextName = newName.trim().take(60)
        if (!names.contains(old) || nextName.isEmpty() ||
            names.any { it.equals(nextName, true) && it != old }) return false
        if (old == nextName) return true
        val songs = playlist(old)
        val nextNames = names.map { if (it == old) nextName else it }
        write("list_" + nextName, songs)
        prefs.edit()
            .putString("playlist_names", JSONArray(nextNames).toString())
            .remove("list_" + old)
            .apply()
        return true
    }

    fun playlist(name: String): List<VideoPreview> =
        if (playlistNames().contains(name)) read("list_" + name) else emptyList()

    fun addToPlaylist(name: String, video: VideoPreview): Boolean {
        if (!playlistNames().contains(name)) return false
        val tracks = playlist(name)
        if (tracks.any { it.url == video.url } || tracks.size >= 300) return false
        write("list_" + name, tracks + video)
        return true
    }

    fun removeFromPlaylist(name: String, url: String): Boolean {
        if (!playlistNames().contains(name)) return false
        val tracks = playlist(name)
        val filtered = tracks.filter { it.url != url }
        if (filtered.size == tracks.size) return false
        write("list_" + name, filtered)
        return true
    }

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

    private fun write(key: String, videos: List<VideoPreview>) {
        val arr = JSONArray()
        videos.forEach { v ->
            arr.put(JSONObject().put("url", v.url).put("title", v.title)
                .put("creator", v.author).put("artwork", v.thumbnail)
                .put("duration", v.durationSeconds))
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}
