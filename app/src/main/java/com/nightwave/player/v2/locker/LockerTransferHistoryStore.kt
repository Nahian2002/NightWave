package com.nightwave.player.v2.locker

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Small, app-private, atomic, read-only-by-default journal.
 *
 * Never stores OAuth/account identifiers, Drive file IDs, stream addresses,
 * path names, upload capabilities, exception messages or audio bytes.
 * Journal failures do not decide whether an upload/download succeeded.
 */
class LockerTransferHistoryStore(
    context: Context,
    journalFile: File = File(context.applicationContext.filesDir,
        "nightwave_locker_transfer_history_v1.json")
) {
    companion object { private val monitor = Any() }
    private val file = journalFile
    private val atomic = AtomicFile(file)

    init {
        val app = context.applicationContext
        // File.parentFile is nullable even for a canonical path. Reject
        // parentless destinations explicitly before checking the private roots.
        val parent = file.canonicalFile.parentFile
            ?: throw IllegalArgumentException("Transfer history path must have a parent")
        require(parent == app.filesDir.canonicalFile ||
            parent == app.cacheDir.canonicalFile
        ) { "Transfer history must be app-private" }
    }

    private fun exists(): Boolean = atomic.baseFile.exists() ||
        File(atomic.baseFile.path + ".bak").exists()

    fun read(): LockerTransferHistorySnapshot = synchronized(monitor) { readLocked() }

    private fun readLocked(): LockerTransferHistorySnapshot {
        if (!exists()) return LockerTransferHistorySnapshot(emptyList())
        return try {
            val bytes = atomic.openRead().use { stream ->
                val output = ByteArrayOutputStream()
                val chunk = ByteArray(4096)
                while (true) {
                    val n = stream.read(chunk)
                    if (n < 0) break
                    require(output.size() + n <= LockerTransferHistoryPolicy.MAX_BYTES) {
                        "Transfer history too large"
                    }
                    output.write(chunk, 0, n)
                }
                output.toByteArray()
            }
            val root = JSONObject(bytes.toString(Charsets.UTF_8))
            require(root.keys().asSequence().toSet() == setOf("version", "entries"))
            require(root.getInt("version") == 1)
            val arr = root.getJSONArray("entries")
            require(arr.length() <= LockerTransferHistoryPolicy.MAX_ENTRIES)
            val entries = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                require(obj.keys().asSequence().toSet() ==
                    setOf("id", "kind", "state", "title", "bytes", "time"))
                LockerTransferRecord(
                    id = obj.getString("id"),
                    kind = LockerTransferKind.valueOf(obj.getString("kind")),
                    state = LockerTransferState.valueOf(obj.getString("state")),
                    title = obj.getString("title"),
                    bytes = obj.getLong("bytes"),
                    timeMs = obj.getLong("time")
                ).also { require(LockerTransferHistoryPolicy.valid(it)) }
            }
            require(entries.distinctBy { it.id }.size == entries.size)
            LockerTransferHistorySnapshot(entries)
        } catch (_: Exception) {
            // Malformed history is never silently deleted or rewritten.
            LockerTransferHistorySnapshot(emptyList(), unreadable = true)
        }
    }

    private fun writeLocked(entries: List<LockerTransferRecord>) {
        require(entries.size <= LockerTransferHistoryPolicy.MAX_ENTRIES)
        require(entries.all(LockerTransferHistoryPolicy::valid))
        val array = JSONArray()
        entries.forEach { event ->
            array.put(JSONObject().put("id", event.id)
                .put("kind", event.kind.name).put("state", event.state.name)
                .put("title", event.title).put("bytes", event.bytes)
                .put("time", event.timeMs))
        }
        val bytes = JSONObject().put("version", 1).put("entries", array)
            .toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= LockerTransferHistoryPolicy.MAX_BYTES)
        val stream = atomic.startWrite()
        try {
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }

    /** Called only after explicit user authorization, immediately before work. */
    fun start(kind: LockerTransferKind, title: String, bytes: Long): String =
        synchronized(monitor) {
            require(bytes in 1L..LockerAudioPolicy.MAX_FILE_BYTES)
            val previous = readLocked()
            require(!previous.unreadable) { "Transfer journal needs manual attention" }
            val id = UUID.randomUUID().toString()
            val entry = LockerTransferRecord(
                id, kind, LockerTransferState.STARTED,
                LockerTransferHistoryPolicy.displayTitle(title),
                bytes, System.currentTimeMillis().coerceAtLeast(1L))
            writeLocked(LockerTransferHistoryPolicy.capped(listOf(entry) + previous.entries))
            id
        }

    /** A status change never invents success for a missing/incomplete entry. */
    fun finish(id: String, result: LockerTransferState) = synchronized(monitor) {
        val previous = readLocked()
        require(!previous.unreadable) { "Transfer journal needs manual attention" }
        writeLocked(LockerTransferHistoryPolicy.finish(previous.entries, id, result))
    }

    /** Explicit privacy control: history only, NOT local audio or checkpoints. */
    fun clear() = synchronized(monitor) { atomic.delete() }
}
