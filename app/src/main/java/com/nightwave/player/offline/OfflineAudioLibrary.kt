package com.nightwave.player.offline

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Offline music from user-selected files only (SAF): never fetches or caches
 * protected YouTube streams or time-limited signed media URLs.
 *
 * Every selected file is copied to the application's private storage so an
 * expired document-provider grant or unplugged SD card won't break playback.
 */
data class OfflineSong(
    val id: String,
    val title: String,
    val mimeType: String,
    val bytes: Long
)

class OfflineAudioLibrary(private val context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("nightwave_offline_v1", Context.MODE_PRIVATE)
    private val root = File(context.applicationContext.filesDir, "offline_audio")

    companion object {
        const val MAX_SONG_BYTES = 250L * 1024L * 1024L
        const val MAX_LIBRARY_BYTES = 3L * 1024L * 1024L * 1024L
        const val MAX_SONGS = 400
        private val extensions = setOf("mp3", "m4a", "aac", "flac", "wav",
            "ogg", "opus", "mp4", "webm", "3gp")
        private val mimeByExtension = mapOf(
            "mp3" to "audio/mpeg", "m4a" to "audio/mp4",
            "aac" to "audio/aac", "flac" to "audio/flac",
            "wav" to "audio/wav", "ogg" to "audio/ogg",
            "opus" to "audio/ogg", "mp4" to "audio/mp4",
            "webm" to "audio/webm", "3gp" to "audio/3gpp"
        )
    }

    private fun parse(raw: String): List<OfflineSong> {
        val arr = JSONArray(raw)
        require(arr.length() <= MAX_SONGS) { "Offline library index needs repair" }
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val id = obj.getString("id")
            require(Regex("[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)").matches(id)) {
                "Invalid offline song identifier"
            }
            OfflineSong(id, obj.getString("title"), obj.getString("mime"), obj.getLong("size"))
        }.also {
            require(it.distinctBy(OfflineSong::id).size == it.size) { "Duplicate offline song" }
        }
    }

    private fun encode(items: List<OfflineSong>): String = JSONArray().apply {
        items.forEach { song ->
            put(JSONObject().put("id", song.id).put("title", song.title)
                .put("mime", song.mimeType).put("size", song.bytes))
        }
    }.toString()

    fun songs(): List<OfflineSong> = parse(prefs.getString("items", "[]") ?: "[]")

    fun fileFor(song: OfflineSong): File? = try {
        if (song !in songs()) return null
        val parent = root.canonicalFile
        val file = File(parent, song.id).canonicalFile
        if (file.parentFile != parent || !file.isFile || !file.canRead()) null else file
    } catch (_: Exception) {
        // Malformed index, permission or missing file must not crash Media3.
        // Keep the index intact for user-guided recovery.
        null
    }

    fun find(id: String): OfflineSong? = songs().firstOrNull { it.id == id }

    @Synchronized
    fun import(uri: Uri): OfflineSong {
        val previous = songs()
        require(previous.size < MAX_SONGS) { "Offline library reached its 400-song limit" }
        val displayName = runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()?.take(180)
            ?: uri.lastPathSegment?.substringAfterLast('/')?.take(180) ?: "Audio file"
        val extension = displayName.substringAfterLast('.', "").lowercase()
        require(extension in extensions) { "Supported formats: MP3, M4A, AAC, FLAC, WAV, OGG, OPUS, MP4 and WebM" }
        val total = previous.sumOf(OfflineSong::bytes)
        require(total < MAX_LIBRARY_BYTES) { "Offline storage limit reached" }
        val id = UUID.randomUUID().toString() + "." + extension
        val title = displayName.substringBeforeLast('.')
            .replace('\n', ' ').replace('\r', ' ').trim().take(110).ifBlank { "Audio track" }
        val mime = mimeByExtension.getValue(extension)
        require(root.isDirectory || root.mkdirs()) { "Can't create private offline library" }
        val pending = File(root, id + ".pending")
        val finalFile = File(root, id)
        try {
            var count = 0L
            requireNotNull(context.contentResolver.openInputStream(uri)) { "Selected file cannot be opened" }.use { input ->
                FileOutputStream(pending).use { output ->
                    val buffer = ByteArray(32768)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        count += read
                        require(count <= MAX_SONG_BYTES && total + count <= MAX_LIBRARY_BYTES) {
                            "File exceeds the offline storage limit"
                        }
                        output.write(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            }
            require(count > 0L) { "Empty audio file" }
            require(pending.renameTo(finalFile)) { "Could not finish offline file import" }
            val song = OfflineSong(id, title, mime, count)
            require(prefs.edit().putString("items", encode(previous + song)).commit()) {
                "Could not save offline library index"
            }
            return song
        } catch (error: Exception) {
            pending.delete()
            finalFile.delete()
            throw error
        }
    }

    @Synchronized
    fun delete(song: OfflineSong): Boolean {
        val previous = songs()
        if (song !in previous) return false
        val updated = previous.filterNot { it.id == song.id }
        if (!prefs.edit().putString("items", encode(updated)).commit()) return false
        File(root, song.id).delete()
        return true
    }
}
