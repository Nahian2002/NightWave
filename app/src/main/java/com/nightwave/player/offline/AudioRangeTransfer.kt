package com.nightwave.player.offline

import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Independent bounded single-range engine. The caller owns endpoint/DNS checks,
 * foreground lifetime, format validation and library commit. No network address
 * is persisted. Only synced, SHA-256 checked segments survive interruption.
 */
internal class AudioRangeTransfer(
    private val home: File,
    private val metrics: AudioTransferMetrics,
    private val execute: (start: Long, end: Long, validator: String?) -> Response,
    private val stopped: () -> Boolean,
    private val space: (total: Long, staged: Long) -> Unit,
    private val progress: (Long, Long?) -> Unit,
    private val now: () -> Long = System::currentTimeMillis
) {
    companion object {
        private val stripes = Array(32) { Any() }
        private val admission = Any()
        private val active = mutableSetOf<String>()
        private const val TTL_MS = 24L * 60 * 60 * 1000
        private const val QUOTA = 512L * 1024 * 1024
        private const val MAX_CHECKPOINTS = 8
        private val name = Regex("[a-f0-9]{64}\\.(part|json|tmp)")
    }
    private data class Segment(val end: Long, val hash: String)
    private data class State(val created: Long, val total: Long, val validator: String,
                             val type: String, val segments: MutableList<Segment>) {
        val offset: Long get() = segments.lastOrNull()?.end ?: 0L
    }

    /** Returns MIME on success; null means safely restart with the full-GET path. */
    fun transfer(key: String, destination: File, keepOnStop: () -> Boolean): String? {
        require(Regex("[a-f0-9]{64}").matches(key))
        return synchronized(stripes[(key.hashCode() and Int.MAX_VALUE) % stripes.size]) transferLock@{
            require(!Files.isSymbolicLink(home.toPath())) { "Unsafe checkpoint folder" }
            require(home.isDirectory || home.mkdirs()) { "Cannot create checkpoint folder" }
            require(home.canonicalFile == home.absoluteFile) { "Unsafe checkpoint path" }
            val part = safeFile("$key.part")
            val meta = safeFile("$key.json")
            val temp = safeFile("$key.tmp")
            val admitted = synchronized(admission) {
                prune(key)
                if (!part.exists() && (home.listFiles().orEmpty().count {
                        it.name.endsWith(".part") && name.matches(it.name)
                    } >= MAX_CHECKPOINTS || reservedBytes() + AudioRangePolicy.MAX_BYTES > QUOTA)) {
                    metrics.mode = "Full transfer · checkpoint capacity reached"
                    false
                } else {
                    // Reserve one bounded slot before releasing admission.
                    if (!part.exists()) require(part.createNewFile())
                    active.add(key)
                    true
                }
            }
            if (!admitted) return@transferLock null
            var discard = false
            var completedType: String? = null
            try {
                RandomAccessFile(part, "rw").use { file ->
                    val lock = runCatching { file.channel.tryLock() }.getOrNull()
                        ?: throw IOException("Audio checkpoint is busy")
                    lock.use {
                        var state = restore(meta, file)
                        file.setLength(state?.offset ?: 0L)
                        metrics.resumedBytes = state?.offset ?: 0L
                        metrics.mode = if (state == null) "Checking resume support" else "Validated resume"
                        var resets = 0
                        while (state == null || state!!.offset < state!!.total) {
                            checkRunning()
                            val offset = state?.offset ?: 0L
                            val end = minOf(offset + AudioRangePolicy.CHUNK_BYTES - 1,
                                (state?.total ?: AudioRangePolicy.MAX_BYTES) - 1)
                            if (state != null) progress(offset, state!!.total)
                            execute(offset, end, state?.validator).use { response ->
                                if (AudioDownloadNetworkPolicy.shouldBackOff(response.code))
                                    throw RetryableAudioSourceResponse(response.code)
                                // Ignored Range or a changed representation must never append.
                                if (response.code == 200) {
                                    discard = true
                                    metrics.resumedBytes = 0
                                    metrics.mode = "Full transfer · server restarted range"
                                    return@use
                                }
                                require(response.code == 206) { "Audio server returned HTTP ${response.code}." }
                                val range = AudioRangePolicy.parse(response.header("Content-Range"))
                                val validator = AudioRangePolicy.validator(response.headers)
                                val type = response.header("Content-Type").orEmpty()
                                    .substringBefore(';').trim().lowercase(java.util.Locale.ROOT)
                                require(type.length <= 128 && !type.startsWith("multipart/")) {
                                    "Invalid audio range format"
                                }
                                val encoding = response.header("Content-Encoding")
                                require(encoding == null || encoding.equals("identity", true)) {
                                    "Encoded audio ranges are unsafe"
                                }
                                require(range != null && range.start == offset && range.end <= end) {
                                    "Partial audio range does not match the request"
                                }
                                if (validator == null) {
                                    discard = true
                                    metrics.resumedBytes = 0
                                    metrics.mode = "Full transfer · no strong resume validator"
                                    return@use
                                }
                                if (state != null && (state!!.validator != validator ||
                                        state!!.total != range.total || state!!.type != type)) {
                                    require(resets++ == 0) { "Audio source changed repeatedly" }
                                    file.setLength(0)
                                    state = null
                                    require(!meta.exists() || meta.delete()) { "Cannot reset audio checkpoint" }
                                    metrics.mode = "Restarted · source changed"
                                    metrics.resumedBytes = 0
                                    progress(0, null)
                                    return@use
                                }
                                val body = requireNotNull(response.body) { "Empty audio range" }
                                val wanted = range.end - range.start + 1
                                require(body.contentLength() == -1L || body.contentLength() == wanted) {
                                    "Partial audio length does not match the range"
                                }
                                space(range.total, offset)
                                if (state == null) state = State(now(), range.total, validator, type, mutableListOf())
                                file.seek(offset)
                                val digest = MessageDigest.getInstance("SHA-256")
                                var count = 0L
                                body.byteStream().use { input ->
                                    val buffer = ByteArray(256 * 1024)
                                    var buffered = 0
                                    fun flushBuffer() {
                                        if (buffered == 0) return
                                        val writeAt = System.nanoTime()
                                        file.write(buffer, 0, buffered)
                                        metrics.writeNanos += System.nanoTime() - writeAt
                                        buffered = 0
                                    }
                                    while (true) {
                                        checkRunning()
                                        val readAt = System.nanoTime()
                                        val n = try { input.read(buffer, buffered, buffer.size - buffered) }
                                            finally { metrics.readNanos += System.nanoTime() - readAt }
                                        if (n < 0) break
                                        if (n == 0) continue
                                        require(count + n <= wanted) { "Partial audio exceeded its range" }
                                        digest.update(buffer, buffered, n)
                                        buffered += n
                                        if (buffered == buffer.size) flushBuffer()
                                        count += n
                                        metrics.wireBytes += n
                                        progress(offset + count, range.total)
                                    }
                                    flushBuffer()
                                }
                                if (count != wanted) throw IOException("Audio download was incomplete")
                                val syncAt = System.nanoTime()
                                file.fd.sync()
                                metrics.syncNanos += System.nanoTime() - syncAt
                                require(state!!.segments.size < 1024) { "Too many audio segments" }
                                state!!.segments.add(Segment(offset + count, hex(digest.digest())))
                                val metaAt = System.nanoTime()
                                persist(meta, temp, state!!)
                                metrics.syncNanos += System.nanoTime() - metaAt
                                metrics.mode = if (metrics.resumedBytes > 0) "Validated resume" else "Bounded ranges"
                                space(range.total, offset + count)
                            }
                            if (discard) break
                        }
                        if (!discard && state != null && state!!.offset == state!!.total) {
                            checkRunning()
                            completedType = state!!.type
                        }
                    }
                }
                if (completedType != null) {
                    require(!destination.exists() || destination.delete()) { "Cannot prepare staged audio" }
                    Files.move(part.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
                    discard = true
                }
                return@transferLock completedType
            } catch (e: Exception) {
                // Interrupted reads may leave a tail; restore truncates to the last
                // durable segment. Protocol/identity/storage failures discard it.
                discard = e !is IOException || (stopped() && !keepOnStop())
                throw e
            } finally {
                try {
                    if (discard) {
                        require(!part.exists() || part.delete()) { "Cannot remove audio checkpoint" }
                        require(!meta.exists() || meta.delete()) { "Cannot remove audio checkpoint metadata" }
                    }
                    if (temp.exists()) temp.delete()
                } finally {
                    synchronized(admission) { active.remove(key) }
                }
            }
        }
    }

    private fun safeFile(value: String): File {
        val file = File(home, value)
        require(!Files.isSymbolicLink(file.toPath()) && (!file.exists() || file.isFile) &&
            file.canonicalFile.parentFile == home.canonicalFile) {
            "Unsafe checkpoint file"
        }
        return file
    }
    private fun checkRunning() { if (stopped()) throw IOException("Download cancelled") }

    private fun restore(meta: File, file: RandomAccessFile): State? = runCatching {
        if (!meta.isFile || meta.length() !in 1..131072) return null
        val json = JSONObject(meta.readText(Charsets.UTF_8))
        require(json.getInt("version") == 1)
        val created = json.getLong("created")
        require(now() - created in 0..TTL_MS)
        val total = json.getLong("total")
        require(total in 1..AudioRangePolicy.MAX_BYTES)
        val validator = json.getString("validator")
        require(AudioRangePolicy.validStoredValidator(validator))
        val type = json.getString("type")
        require(type.length <= 128)
        val rows = json.getJSONArray("segments")
        require(rows.length() in 1..1024)
        val segments = mutableListOf<Segment>()
        var start = 0L
        val buffer = ByteArray(256 * 1024)
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val end = row.getLong("end")
            require(end > start && end <= total && end - start <= AudioRangePolicy.CHUNK_BYTES && file.length() >= end)
            val hash = row.getString("sha256")
            require(Regex("[a-f0-9]{64}").matches(hash))
            file.seek(start)
            var remaining = end - start
            val digest = MessageDigest.getInstance("SHA-256")
            while (remaining > 0) {
                checkRunning()
                val n = file.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                require(n > 0)
                digest.update(buffer, 0, n)
                remaining -= n
            }
            require(hex(digest.digest()) == hash)
            segments.add(Segment(end, hash))
            start = end
        }
        State(created, total, validator, type, segments)
    }.getOrElse {
        // Cancellation during prefix hashing is not corrupt metadata. Never
        // truncate a valid checkpoint merely because its worker was paused.
        if (it is IOException && stopped()) throw it
        null
    }

    private fun persist(meta: File, temp: File, state: State) {
        val rows = JSONArray()
        state.segments.forEach { rows.put(JSONObject().put("end", it.end).put("sha256", it.hash)) }
        val bytes = JSONObject().put("version", 1).put("created", state.created)
            .put("total", state.total).put("validator", state.validator).put("type", state.type)
            .put("segments", rows).toString().toByteArray(Charsets.UTF_8)
        FileOutputStream(temp).use { it.write(bytes); it.flush(); it.fd.sync() }
        Files.move(temp.toPath(), meta.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun reservedBytes(): Long = home.listFiles().orEmpty().filter {
        name.matches(it.name) && it.name.endsWith(".part")
    }.sumOf { part ->
        val meta = safeFile(part.name.removeSuffix(".part") + ".json")
        runCatching { require(meta.length() in 1..131072); JSONObject(meta.readText()).getLong("total")
            .coerceIn(part.length(), AudioRangePolicy.MAX_BYTES) }.getOrDefault(AudioRangePolicy.MAX_BYTES)
    }

    private fun prune(current: String) {
        // Called under admission. Only this store's names, no recursive cleanup.
        home.listFiles().orEmpty().filter { name.matches(it.name) }
            .groupBy { it.name.substringBefore('.') }.forEach { (key, files) ->
                if (key != current && key !in active && files.all {
                        !Files.isSymbolicLink(it.toPath()) && it.isFile &&
                            now() - it.lastModified() > TTL_MS }) {
                    val part = safeFile("$key.part")
                    val unlocked = if (!part.exists()) true else runCatching {
                        RandomAccessFile(part, "rw").use { f ->
                            val lock = f.channel.tryLock() ?: return@use false
                            lock.release()
                            true
                        }
                    }.getOrDefault(false)
                    if (unlocked) files.forEach { require(it.delete()) { "Cannot prune audio checkpoint" } }
                }
            }
    }
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
