package com.nightwave.player.v2.data.room

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview
import org.json.JSONArray
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Validates exactly what the old app persists. Never silently skip corrupt
 * entries: an error must leave the original preferences and Room snapshot alone.
 */
data class LegacyLibrarySnapshot(
    val favorites: List<VideoPreview>,
    val recent: List<VideoPreview>,
    val playlists: List<Pair<String, List<VideoPreview>>>,
    val digest: String
)

object LegacyLibrarySnapshotParser {
    fun parse(source: Map<String, String?>): LegacyLibrarySnapshot {
        val namesJson = JSONArray(source["playlist_names"] ?: "[]")
        require(namesJson.length() <= 50) { "Too many legacy playlists" }
        val names = (0 until namesJson.length()).map { namesJson.getString(it) }
        require(names.all { it.isNotBlank() && it.length <= 60 }) { "Invalid playlist name" }
        require(names.map { it.lowercase() }.distinct().size == names.size) { "Duplicate playlist names" }

        fun songs(key: String, maximum: Int): List<VideoPreview> {
            val arr = JSONArray(source[key] ?: "[]")
            require(arr.length() <= maximum) { "Too many items in $key" }
            val videos = (0 until arr.length()).map { pos ->
                val row = arr.getJSONObject(pos)
                val url = VideoLink.normalize(row.getString("url"))
                    ?: throw IllegalArgumentException("Invalid video in $key")
                require(row.keys().asSequence().toSet().all { it in setOf("url", "title", "creator", "artwork", "duration") }) {
                    "Unexpected video field in $key"
                }
                val duration = row.optLong("duration", 0)
                require(duration in 0L..86400L) { "Invalid duration in $key" }
                val title = row.optString("title")
                val creator = row.optString("creator")
                val artwork = row.optString("artwork")
                require(title.length <= 250 && creator.length <= 150 && artwork.length <= 2000) {
                    "Invalid metadata size in $key"
                }
                VideoPreview(title, creator, url, artwork, duration)
            }
            require(videos.distinctBy { it.url }.size == videos.size) { "Duplicate videos in $key" }
            return videos
        }

        val playlists = names.map { name -> name to songs("list_$name", 300) }
        val favorites = songs("favorites", 500)
        val recent = songs("recent", 100)

        // Hash the exact known-key values, not a re-serialized JSON document:
        // it should detect ordinary legacy writes without deleting or rewriting.
        val relevantKeys = (listOf("favorites", "recent", "playlist_names") +
            names.map { "list_$it" }).sorted()
        // Preserve the exact digest contract without materializing a second
        // whole-library string and UTF-8 byte array during full-corpus reads.
        val digest = MessageDigest.getInstance("SHA-256")
        relevantKeys.forEachIndexed { index, key ->
            if (index > 0) digest.update(0.toByte())
            val raw = source[key]
            digest.update((key.length.toString() + ":" + key + ":" +
                (raw?.length ?: -1).toString() + ":").toByteArray(StandardCharsets.UTF_8))
            if (raw != null) digest.update(raw.toByteArray(StandardCharsets.UTF_8))
        }
        val hash = digest.digest()
            .joinToString("") { "%02x".format(it) }
        return LegacyLibrarySnapshot(favorites, recent, playlists, hash)
    }
}
