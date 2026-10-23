package com.nightwave.player.offline

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import com.nightwave.player.v2.locker.LockerAudioPolicy
import com.nightwave.player.v2.locker.LockerRemoteTrack
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
        const val MAX_LIBRARY_BYTES = OfflineLibraryCapacity.MAX_LIBRARY_BYTES
        const val MAX_SONGS = OfflineLibraryCapacity.MAX_ITEMS
        private val extensions = setOf("mp3", "m4a", "aac", "flac", "wav",
            "ogg", "opus", "mp4", "webm", "3gp")
        // Keep older index IDs valid, while excluding separators and paths.
        private val indexedFileId = Regex(
            "[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)"
        )
        private val mimeByExtension = mapOf(
            "mp3" to "audio/mpeg", "m4a" to "audio/mp4",
            "aac" to "audio/aac", "flac" to "audio/flac",
            "wav" to "audio/wav", "ogg" to "audio/ogg",
            "opus" to "audio/ogg", "mp4" to "audio/mp4",
            "webm" to "audio/webm", "3gp" to "audio/3gpp"
        )
    }

    private fun parse(raw: String): List<OfflineSong> {
        require(OfflineLibraryCapacity.safeIndex(raw)) { "Offline library index is too large" }
        val arr = JSONArray(raw)
        require(arr.length() <= MAX_SONGS) { "Offline library index needs repair" }
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val id = obj.getString("id")
            require(indexedFileId.matches(id)) {
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
    }.toString().also { serialized ->
        require(OfflineLibraryCapacity.safeIndex(serialized)) { "Offline library index size limit reached" }
    }

    fun songs(): List<OfflineSong> = parse(prefs.getString("items", "[]") ?: "[]")

    /** Never opens untrusted paths or assumes a matching index proves a file exists. */
    private fun inspectFile(song: OfflineSong): Pair<OfflineFileHealth, File?> {
        // Legacy IDs are kept compatible with the existing index parser; they
        // cannot contain separators, and extension/MIME/size must still agree.
        if (!indexedFileId.matches(song.id) ||
            mimeByExtension[song.id.substringAfterLast('.')] != song.mimeType ||
            song.bytes !in 1L..MAX_SONG_BYTES) {
            return OfflineFileHealth.UNSAFE_METADATA to null
        }
        return try {
            val appHome = context.applicationContext.filesDir.canonicalFile
            val base = root.canonicalFile
            if (base.parentFile != appHome || base.name != "offline_audio") {
                return OfflineFileHealth.UNSAFE_METADATA to null
            }
            val direct = File(root, song.id)
            // Refuse symlinks even if their canonical destination is in the
            // private directory: the indexed path must name a regular file.
            if (Files.isSymbolicLink(direct.toPath())) {
                return OfflineFileHealth.UNSAFE_METADATA to null
            }
            val file = direct.canonicalFile
            if (file.parentFile != base) {
                return OfflineFileHealth.UNSAFE_METADATA to null
            }
            val health = OfflineLibraryHealthPolicy.classify(
                song.bytes, file.exists(), file.isFile, file.canRead(), file.length()
            )
            health to if (health == OfflineFileHealth.AVAILABLE) file else null
        } catch (_: Exception) {
            OfflineFileHealth.UNREADABLE to null
        }
    }

    /**
     * Reads the existing index and file metadata only. A corrupted index is
     * reported without rewriting it or touching any audio. Intended for UI
     * diagnostics, not as a background auto-cleanup job.
     */
    fun healthReport(): OfflineLibraryHealthReport {
        // SharedPreferences supplies one consistent index value per read.
        // Don't wait on the write lock: a concurrent cloud import may hash a
        // large file under that lock. Playback/upload/delete revalidate each
        // selected file before using or mutating it.
        val indexed = try { songs() } catch (_: Exception) {
            return OfflineLibraryHealthReport(false, emptyList())
        }
        return OfflineLibraryHealthReport(true, indexed.map { song ->
            OfflineLibraryHealthEntry(song, inspectFile(song).first)
        })
    }

    fun fileFor(song: OfflineSong): File? = try {
        // Legacy app code may supply a stale row. Validate exact membership
        // before handing Media3 or the Drive uploader a private file path.
        if (song !in songs()) null else inspectFile(song).second
    } catch (_: Exception) {
        // Malformed index, permission or missing file must not crash Media3.
        // Keep the original index untouched for explicit recovery.
        null
    }

    fun find(id: String): OfflineSong? = songs().firstOrNull { it.id == id }

    fun import(uri: Uri): OfflineSong = synchronized(OfflineLibraryIndexLock.monitor) {
        val previous = songs()
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
        require(total in 0..MAX_LIBRARY_BYTES) { "Offline index reports invalid total size" }
        val id = UUID.randomUUID().toString() + "." + extension
        val title = displayName.substringBeforeLast('.')
            .replace('\n', ' ').replace('\r', ' ').trim().take(110).ifBlank { "Audio track" }
        val mime = mimeByExtension.getValue(extension)
        require(root.isDirectory || root.mkdirs()) { "Can't create private offline library" }
        require(root.usableSpace > OfflineLibraryCapacity.MIN_FREE_HEADROOM_BYTES) {
            "Insufficient free space to start an import"
        }
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
                        require(count <= MAX_SONG_BYTES) { "File exceeds the 250 MB audio limit" }
                        if (count % (4L * 1024L * 1024L) < read) {
                            require(root.usableSpace > OfflineLibraryCapacity.MIN_FREE_HEADROOM_BYTES) {
                                "Device storage is almost full; import safely stopped"
                            }
                        }
                        output.write(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            }
            require(count > 0L) { "Empty audio file" }
            // Identical authorized imports reuse the existing ID, preserving lyrics,
            // playlists and cloud references. Duplicate discovery precedes the cap.
            val hash = audioSha256(pending)
            val existing = previous.asSequence()
                .filter { it.bytes == count && it.mimeType == mime }
                .firstOrNull { candidate ->
                    inspectFile(candidate).second?.let { file ->
                        MessageDigest.isEqual(hash, audioSha256(file))
                    } == true
                }
            if (existing != null) {
                pending.delete()
                return@synchronized existing
            }
            require(OfflineLibraryCapacity.acceptsNew(previous.size, total, count)) {
                "Offline Library capacity reached (5,000 songs / 32 GiB)"
            }
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

    /** Stream SHA-256; never allocate the whole 250 MiB song in memory. */
    private fun audioSha256(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val chunk = ByteArray(32_768)
            while (true) {
                val count = input.read(chunk)
                if (count < 0) break
                if (count > 0) digest.update(chunk, 0, count)
            }
        }
        return digest.digest()
    }

    /**
     * Import a fully downloaded, signature-validated direct audio file from
     * NightWave's private HTTPS staging folder. Never trust arbitrary file
     * paths or overwrite an existing song. Index commit is the final step.
     */
    fun importPermittedStagedAudio(staged: File, filename: String): OfflineSong =
        synchronized(OfflineLibraryIndexLock.monitor) {
            val appHome = context.applicationContext.filesDir.canonicalFile
            val base = File(appHome, "permitted_download_staging").canonicalFile
            require(base.parentFile == appHome) { "Unexpected download folder" }
            require(Regex("[0-9a-f-]{36}\\.pending").matches(staged.name) &&
                !Files.isSymbolicLink(staged.toPath()) &&
                staged.canonicalFile.parentFile == base) {
                "Invalid temporary download file"
            }
            require(staged.isFile && staged.canRead()) { "Download is incomplete" }
            val size = staged.length()
            require(size in 1..MAX_SONG_BYTES) { "Audio download exceeds the file limit" }
            val ext = filename.substringAfterLast('.', "").lowercase(Locale.ROOT)
            require(ext in extensions) { "Unsupported audio file type" }
            val title = filename.substringBeforeLast('.').replace('\\n', ' ')
                .replace('\\r', ' ').trim().take(110).ifBlank { "Downloaded audio" }
            val current = songs()
            val expectedSha256 = audioSha256(staged)
            // A repeated URL should not consume another slot or silently
            // create a duplicate. Reuse only the same verified bytes + format.
            val existing = current.asSequence()
                .filter { it.bytes == size && it.mimeType == mimeByExtension.getValue(ext) }
                .firstOrNull { song ->
                    fileFor(song)?.let { file ->
                        MessageDigest.isEqual(expectedSha256, audioSha256(file))
                    } == true
                }
            if (existing != null) return@synchronized existing
            require(OfflineLibraryCapacity.acceptsNew(
                current.size, current.sumOf(OfflineSong::bytes), size)) {
                "Offline Library capacity reached (5,000 songs / 32 GiB)"
            }
            require(root.isDirectory || root.mkdirs()) { "Can't create offline library" }
            val id = UUID.randomUUID().toString() + "." + ext
            val pending = File(root, "$id.pending")
            val finalFile = File(root, id)
            try {
                staged.inputStream().use { input ->
                    FileOutputStream(pending).use { output ->
                        input.copyTo(output, 32_768)
                        output.fd.sync()
                    }
                }
                require(pending.length() == size) { "Downloaded audio copy is incomplete" }
                // Verify copied bytes, not just length: do not commit a damaged
                // file under the correct metadata.
                require(MessageDigest.isEqual(expectedSha256, audioSha256(pending))) {
                    "Downloaded audio changed during local import"
                }
                require(pending.renameTo(finalFile)) { "Couldn't finalize downloaded audio" }
                val song = OfflineSong(id, title, mimeByExtension.getValue(ext), size)
                check(prefs.edit().putString("items", encode(current + song)).commit()) {
                    "Couldn't update offline library index"
                }
                song
            } catch (error: Exception) {
                pending.delete()
                finalFile.delete()
                throw error
            }
        }

    /**
     * Accept a complete, SHA+MD5 verified app-private staging file only.
     * No SAF URI, cloud permission, stream link or unverified remote metadata
     * can be used as a shortcut to register playback.
     *
     * Duplicate content is reused, never overwritten. When possible the
     * original private NightWave song ID is preserved so already backed-up
     * LRC lyrics can be attached after explicit cloud-library restore.
     */
    fun importVerifiedCloud(
        staged: File, remote: LockerRemoteTrack
    ): CloudOfflineImportResult = synchronized(OfflineLibraryIndexLock.monitor) {
        LockerAudioPolicy.requireDownloadable(remote)
        val stagedRoot = File(context.applicationContext.filesDir, "locker_staging").canonicalFile
        val file = staged.canonicalFile
        require(file.parentFile == stagedRoot &&
            Regex("[0-9a-f-]{36}\\.pending").matches(file.name)) {
            "Refusing untrusted staged audio location"
        }
        require(file.isFile && file.canRead() && file.length() == remote.bytes) {
            "Staged audio incomplete"
        }
        val (sha, md5) = LockerAudioPolicy.hashes(file)
        require(sha == remote.sha256 && md5.equals(remote.md5, true)) {
            "Staged audio checksum mismatch"
        }
        val previous = songs()
        for (song in previous) {
            if (song.bytes != remote.bytes || song.mimeType != remote.mimeType) continue
            val current = fileFor(song) ?: continue
            if (LockerAudioPolicy.hashes(current).first == sha) {
                return CloudOfflineImportResult(song, created = false,
                    originalIdPreserved = song.id == remote.sourceLocalId)
            }
        }
        require(OfflineLibraryCapacity.acceptsNew(
            previous.size, previous.sumOf(OfflineSong::bytes), remote.bytes)) {
            "Offline Library capacity reached (5,000 songs / 32 GiB)"
        }
        val songId = LockerAudioPolicy.chooseRestoredId(remote) { id ->
            previous.any { song -> song.id == id } || File(root, id).exists()
        }
        val saved = OfflineSong(
            songId, LockerAudioPolicy.safeTitle(remote.name), remote.mimeType, remote.bytes
        )
        require(root.isDirectory || root.mkdirs()) { "Offline storage unavailable" }
        val finalFile = File(root, songId)
        require(!finalFile.exists()) { "Offline filename already exists" }
        // Both directories live under app filesDir, so Files.move is local.
        // With no REPLACE_EXISTING option, an existing destination is not replaced.
        Files.move(file.toPath(), finalFile.toPath())
        try {
            check(prefs.edit().putString("items", encode(previous + saved)).commit()) {
                "Could not register the downloaded audio"
            }
        } catch (error: Exception) {
            // No persisted library entry means the new file must not remain as
            // a hidden, unindexed download. Older tracks remain untouched.
            finalFile.delete()
            throw error
        }
        return CloudOfflineImportResult(saved, created = true,
            originalIdPreserved = saved.id == remote.sourceLocalId)
    }

    fun delete(song: OfflineSong): Boolean = synchronized(OfflineLibraryIndexLock.monitor) {
        val previous = songs()
        if (song !in previous) return false
        // A missing, changed or unreadable file might still be recoverable.
        // Never discard its index entry through the normal delete path.
        if (inspectFile(song).first != OfflineFileHealth.AVAILABLE) return false
        val updated = previous.filterNot { it.id == song.id }
        if (!prefs.edit().putString("items", encode(updated)).commit()) return false
        File(root, song.id).delete()
        return true
    }
}

/** A separate result prevents a duplicate cloud download from changing local IDs. */
data class CloudOfflineImportResult(
    val song: OfflineSong, val created: Boolean, val originalIdPreserved: Boolean
)
