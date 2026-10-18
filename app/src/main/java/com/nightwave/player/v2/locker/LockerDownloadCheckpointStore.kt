package com.nightwave.player.v2.locker

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.util.UUID

/**
 * One explicit user-initiated download at a time. Only expected metadata and
 * a private pending filename survive process death; NO OAuth tokens, resumable
 * upload session URLs, cookies or access to user Drive contents are saved.
 */
data class LockerPendingDownload(
    val account: String,
    val remote: LockerRemoteTrack,
    val fileName: String,
    val committedBytes: Long
)

class LockerDownloadCheckpointStore(context: Context) {
    private val root = context.applicationContext.filesDir
    private val atomic = AtomicFile(File(root, "nightwave_locker_download_v1.json"))
    private val pendingPattern = Regex("[0-9a-f-]{36}\\.pending")

    @Synchronized
    fun read(): LockerPendingDownload? = runCatching {
        val bytes = atomic.openRead().use { input ->
            val array = input.readBytes()
            require(array.size in 1..8192)
            array
        }
        val data = JSONObject(bytes.toString(Charsets.UTF_8))
        require(data.keys().asSequence().toSet() ==
            setOf("format", "account", "remote", "file", "committed"))
        require(data.getInt("format") == 1)
        val item = data.getJSONObject("remote")
        require(item.keys().asSequence().toSet() ==
            setOf("id", "name", "mime", "bytes", "md5", "sha256", "source"))
        val remote = LockerAudioPolicy.requireDownloadable(LockerRemoteTrack(
            item.getString("id"), item.getString("name"), item.getString("mime"),
            item.getLong("bytes"), item.getString("md5"), item.getString("sha256"),
            item.getString("source")
        ))
        val name = data.getString("file")
        val account = data.getString("account")
        val committed = data.getLong("committed")
        require(account.length in 3..254 && account.contains('@') &&
            pendingPattern.matches(name) && committed in 0..remote.bytes)
        LockerPendingDownload(account, remote, name, committed)
    }.getOrNull()

    /**
     * Corrupt checkpoints are never resumed or overwritten. They can be
     * explicitly discarded from the locker UI after an explanation.
     */
    @Synchronized
    fun needsRepair(): Boolean = read() == null &&
        (File(root, "nightwave_locker_download_v1.json").exists() ||
            File(root, "nightwave_locker_download_v1.json.bak").exists())

    /** Do not silently replace a prior incomplete download. */
    @Synchronized
    fun begin(account: String, track: LockerRemoteTrack): LockerPendingDownload {
        require(account.length in 3..254 && account.contains('@'))
        LockerAudioPolicy.requireDownloadable(track)
        val old = read()
        if (old != null) {
            require(old.account.equals(account, true) && old.remote == track) {
                "An unfinished cloud download exists. Resume or discard it first."
            }
            return old
        }
        // Do not create a new checkpoint over a corrupted legacy checkpoint.
        require(!needsRepair()) {
            "Download checkpoint needs explicit discard before retrying"
        }
        val new = LockerPendingDownload(account, track,
            UUID.randomUUID().toString() + ".pending", 0)
        write(new)
        return new
    }

    @Synchronized
    fun commit(previous: LockerPendingDownload, committed: Long): LockerPendingDownload {
        val old = requireNotNull(read()) { "Download recovery state missing" }
        require(old.account == previous.account && old.remote == previous.remote &&
            old.fileName == previous.fileName &&
            old.committedBytes <= committed && committed <= old.remote.bytes) {
            "Download checkpoint changed"
        }
        val next = old.copy(committedBytes = committed)
        write(next)
        return next
    }

    /**
     * Read-only readiness check. Corrupt checkpoints and missing/truncated
     * temporary audio remain untouched until the user explicitly discards.
     */
    @Synchronized
    fun inspect(connectedAccount: String): LockerCheckpointPreview {
        val item = read()
        if (item == null) return LockerCheckpointPreview(
            LockerCheckpointReadiness.classify(null, needsRepair(),
                false, false, false, false, 0L))
        if (!item.account.equals(connectedAccount, ignoreCase = true)) {
            return LockerCheckpointPreview(LockerCheckpointState.ACCOUNT_MISMATCH)
        }
        return try {
            val rawDir = File(root, "locker_staging")
            val path = File(rawDir, item.fileName)
            val unsafe = Files.isSymbolicLink(rawDir.toPath()) ||
                Files.isSymbolicLink(path.toPath()) ||
                (rawDir.exists() && !rawDir.isDirectory) ||
                path.canonicalFile.parentFile != rawDir.canonicalFile ||
                rawDir.canonicalFile.parentFile != root.canonicalFile
            val exists = path.exists()
            val regular = path.isFile
            val actual = if (regular) path.length() else 0L
            LockerCheckpointPreview(LockerCheckpointReadiness.classify(
                item, false, true, exists, regular, unsafe, actual), item, actual)
        } catch (_: Exception) {
            LockerCheckpointPreview(LockerCheckpointState.UNSAFE_STAGING, item)
        }
    }

    /** Returns a canonical, app-private file path only. */
    fun stagedFile(item: LockerPendingDownload): File {
        require(pendingPattern.matches(item.fileName))
        val rawDir = File(root, "locker_staging")
        require(!Files.isSymbolicLink(rawDir.toPath())) {
            "Unsafe private staging folder"
        }
        val dir = rawDir.canonicalFile
        require(dir.parentFile == root.canonicalFile) { "Bad private staging folder" }
        require(dir.isDirectory || dir.mkdirs()) { "Private staging folder unavailable" }
        val candidate = File(dir, item.fileName)
        require(!Files.isSymbolicLink(candidate.toPath())) { "Staging path is a link" }
        val file = candidate.canonicalFile
        require(file.parentFile == dir) { "Invalid staged audio path" }
        return file
    }

    /** Explicit user action or post-import cleanup; never touches remote Drive. */
    @Synchronized
    fun discard() {
        read()?.let { pending ->
            stagedFile(pending).delete()
        }
        atomic.delete()
    }

    @Synchronized
    private fun write(item: LockerPendingDownload) {
        LockerAudioPolicy.requireDownloadable(item.remote)
        val data = JSONObject().put("format", 1)
            .put("account", item.account)
            .put("file", item.fileName)
            .put("committed", item.committedBytes)
            .put("remote", JSONObject()
                .put("id", item.remote.id)
                .put("name", item.remote.name)
                .put("mime", item.remote.mimeType)
                .put("bytes", item.remote.bytes)
                .put("md5", item.remote.md5)
                .put("sha256", item.remote.sha256)
                .put("source", item.remote.sourceLocalId))
            .toString().toByteArray(Charsets.UTF_8)
        require(data.size in 1..8192)
        val stream = atomic.startWrite()
        try {
            stream.write(data)
            atomic.finishWrite(stream)
        } catch (ex: Exception) {
            atomic.failWrite(stream)
            throw ex
        }
    }
}
