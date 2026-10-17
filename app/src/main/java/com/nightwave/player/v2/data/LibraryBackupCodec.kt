package com.nightwave.player.v2.data

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64

/** A data-only, deterministic and bounded backup format. No credentials or signed streams. */
data class LibraryArchive(
    val displayName: String,
    val theme: String,
    val avatar: String,
    val favorites: List<VideoPreview>,
    val playlists: List<Pair<String, List<VideoPreview>>>,
    val recent: List<VideoPreview> = emptyList()
)

object LibraryBackupCodec {
    const val VERSION = 1
    const val MAX_BYTES = 2_000_000
    private const val MAX_FAVORITES = 500
    private const val MAX_PLAYLISTS = 50
    private const val MAX_PER_PLAYLIST = 300

    private fun track(video: VideoPreview): JSONObject {
        val url = VideoLink.normalize(video.url)
            ?: throw IllegalArgumentException("Invalid public video")
        return JSONObject().put("url", url)
            .put("title", video.title.take(250))
            .put("creator", video.author.take(150))
            .put("artwork", video.thumbnail.take(2000))
            .put("duration", video.durationSeconds.coerceIn(0, 86400))
    }

    private fun readTrack(value: JSONObject): VideoPreview {
        val raw = value.getString("url")
        val url = VideoLink.normalize(raw) ?: throw IllegalArgumentException("Invalid video link in backup")
        if (value.has("stream_url") || value.has("audio_url") || value.has("token")) {
            throw IllegalArgumentException("Forbidden media or credential field")
        }
        return VideoPreview(
            title = value.optString("title").take(250),
            author = value.optString("creator").take(150),
            url = url,
            thumbnail = value.optString("artwork").take(2000),
            durationSeconds = value.optLong("duration", 0).coerceIn(0, 86400)
        )
    }

    private fun tracks(videos: List<VideoPreview>, cap: Int): JSONArray {
        require(videos.size <= cap) { "Too many tracks" }
        return JSONArray().also { array -> videos.forEach { array.put(track(it)) } }
    }

    private fun parseTracks(data: JSONArray, cap: Int): List<VideoPreview> {
        require(data.length() <= cap) { "Too many tracks in backup" }
        val parsed = (0 until data.length()).map { readTrack(data.getJSONObject(it)) }
        require(parsed.map { it.url }.distinct().size == parsed.size) { "Duplicate tracks in backup" }
        return parsed
    }

    private fun hexDigest(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(value)
            .joinToString("") { "%02x".format(it) }

    fun encode(archive: LibraryArchive): ByteArray {
        require(archive.playlists.size <= MAX_PLAYLISTS)
        require(archive.playlists.map { it.first.lowercase() }.distinct().size == archive.playlists.size)
        require(archive.playlists.all { it.first.isNotBlank() && it.first.length <= 60 })
        val data = JSONObject()
            .put("format", VERSION)
            .put("profile", JSONObject()
                .put("display_name", archive.displayName.take(40))
                .put("theme", archive.theme)
                .put("avatar", archive.avatar))
            .put("favorites", tracks(archive.favorites, MAX_FAVORITES))
            .put("playlists", JSONArray().also { arr ->
                archive.playlists.forEach { (name, songs) ->
                    arr.put(JSONObject().put("name", name).put("tracks", tracks(songs, MAX_PER_PLAYLIST)))
                }
            })
        // History is deliberately not exported without explicit opt-in.
        val payload = data.toString()
        val envelope = JSONObject().put("payload_base64", Base64.getEncoder().encodeToString(payload.toByteArray(Charsets.UTF_8)))
            .put("sha256", hexDigest(payload.toByteArray(Charsets.UTF_8)))
        val bytes = envelope.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "Backup too large" }
        return bytes
    }

    fun decode(bytes: ByteArray): LibraryArchive {
        require(bytes.size in 1..MAX_BYTES) { "Invalid backup size" }
        val raw = JSONObject(bytes.toString(Charsets.UTF_8))
        require(raw.keySet() == setOf("payload_base64", "sha256")) { "Unexpected envelope fields" }
        val payload = Base64.getDecoder().decode(raw.getString("payload_base64"))
        require(payload.size <= MAX_BYTES) { "Invalid payload size" }
        require(raw.getString("sha256") == hexDigest(payload)) {
            "Backup integrity check failed"
        }
        val data = JSONObject(payload.toString(Charsets.UTF_8))
        require(data.getInt("format") == VERSION) { "Unsupported backup version" }
        require(data.keySet().all { it in setOf("format", "profile", "favorites", "playlists") }) {
            "Unexpected backup fields"
        }
        val p = data.getJSONObject("profile")
        require(p.keySet().all { it in setOf("display_name", "theme", "avatar") }) {
            "Unexpected profile fields"
        }
        val favorites = parseTracks(data.getJSONArray("favorites"), MAX_FAVORITES)
        val list = data.getJSONArray("playlists")
        require(list.length() <= MAX_PLAYLISTS) { "Too many playlists" }
        val playlists = (0 until list.length()).map { i ->
            val item = list.getJSONObject(i)
            require(item.keySet() == setOf("name", "tracks")) { "Unexpected playlist fields" }
            val name = item.getString("name")
            require(name.isNotBlank() && name.length <= 60) { "Invalid playlist name" }
            name to parseTracks(item.getJSONArray("tracks"), MAX_PER_PLAYLIST)
        }
        require(playlists.map { it.first.lowercase() }.distinct().size == playlists.size) {
            "Duplicate playlist names"
        }
        val theme = p.getString("theme")
        require(theme in setOf("MIDNIGHT", "AURORA", "PEARL", "AMOLED")) { "Invalid theme" }
        val avatar = p.getString("avatar")
        require(avatar in setOf("orbit", "wave", "prism", "moon", "vinyl", "spark")) { "Invalid avatar" }
        return LibraryArchive(p.optString("display_name").take(40), theme, avatar, favorites, playlists)
    }
}
