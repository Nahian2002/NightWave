package com.nightwave.player.v2.cloud

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.data.LibraryArchive
import com.nightwave.player.v2.data.LibraryBackupCodec
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64

data class CloudBackupPreview(
    val archive: LibraryArchive,
    val history: List<VideoPreview>,
    val includesHistory: Boolean
)

/**
 * V2 cloud envelope, separate from the user's original v1 manual-export format.
 * History requires an explicit opt-in. Never include tokens, offline file paths
 * or signed audio streams. Do not restore automatically.
 */
object CloudBackupCodec {
    const val MAX_BYTES = 3_000_000
    private const val MAX_RECENT = 100

    private fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private fun track(song: VideoPreview): JSONObject {
        val url = VideoLink.normalize(song.url) ?: error("Invalid history video")
        return JSONObject().put("url", url)
            .put("title", song.title.take(250))
            .put("creator", song.author.take(150))
            .put("artwork", song.thumbnail.take(2000))
            .put("duration", song.durationSeconds.coerceIn(0, 86400))
    }

    fun validatedHistory(history: List<VideoPreview>): List<VideoPreview> {
        require(history.size <= MAX_RECENT) { "Too much listening history" }
        val clean = history.map { song ->
            val url = VideoLink.normalize(song.url) ?: error("Invalid history URL")
            VideoPreview(song.title.take(250), song.author.take(150),
                url, song.thumbnail.take(2000), song.durationSeconds.coerceIn(0, 86400))
        }
        require(clean.map { it.url }.distinct().size == clean.size) { "Duplicate history URL" }
        return clean
    }

    fun encode(manualBackup: ByteArray, history: List<VideoPreview>, includeHistory: Boolean): ByteArray {
        // Validate v1 archive before ever exporting it to the cloud.
        LibraryBackupCodec.decode(manualBackup)
        val recent = if (includeHistory) validatedHistory(history.take(MAX_RECENT)) else emptyList()
        val payload = JSONObject()
            .put("format", 2)
            .put("guest_archive", Base64.getEncoder().encodeToString(manualBackup))
            .put("includes_history", includeHistory)
            .put("recent", JSONArray().also { array -> recent.forEach { array.put(track(it)) } })
            .toString().toByteArray(Charsets.UTF_8)
        val output = JSONObject()
            .put("payload", Base64.getEncoder().encodeToString(payload))
            .put("sha256", digest(payload)).toString().toByteArray(Charsets.UTF_8)
        require(output.size in 1..MAX_BYTES) { "Cloud archive exceeds size limit" }
        return output
    }

    fun decode(bytes: ByteArray): CloudBackupPreview {
        require(bytes.size in 1..MAX_BYTES) { "Invalid cloud archive size" }
        val wrapper = JSONObject(bytes.toString(Charsets.UTF_8))
        require(wrapper.keys().asSequence().toSet() == setOf("payload", "sha256"))
        val payload = Base64.getDecoder().decode(wrapper.getString("payload"))
        require(payload.size <= MAX_BYTES && digest(payload) == wrapper.getString("sha256")) {
            "Cloud archive integrity verification failed"
        }
        val data = JSONObject(payload.toString(Charsets.UTF_8))
        require(data.keys().asSequence().toSet() ==
            setOf("format", "guest_archive", "includes_history", "recent"))
        require(data.getInt("format") == 2) { "Unsupported cloud archive" }
        val embedded = Base64.getDecoder().decode(data.getString("guest_archive"))
        val archive = LibraryBackupCodec.decode(embedded)
        val enabled = data.getBoolean("includes_history")
        val rows = data.getJSONArray("recent")
        require(rows.length() <= MAX_RECENT && (enabled || rows.length() == 0))
        val recent = (0 until rows.length()).map { index ->
            val item = rows.getJSONObject(index)
            require(item.keys().asSequence().toSet() ==
                setOf("url", "title", "creator", "artwork", "duration"))
            val url = VideoLink.normalize(item.getString("url")) ?: error("Invalid history URL")
            VideoPreview(item.getString("title").take(250),
                item.getString("creator").take(150), url,
                item.getString("artwork").take(2000),
                item.getLong("duration").coerceIn(0, 86400))
        }
        return CloudBackupPreview(archive, validatedHistory(recent), enabled)
    }
}
