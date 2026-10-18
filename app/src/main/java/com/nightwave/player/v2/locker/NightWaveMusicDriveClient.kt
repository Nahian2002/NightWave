package com.nightwave.player.v2.locker

import com.nightwave.player.offline.OfflineSong
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

/** My Drive music files, entirely separate from hidden appDataFolder backups. */
class NightWaveMusicDriveClient(private val token: String) {
    init { require(token.isNotBlank()) }
    private val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false).build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val fileId = Regex("[A-Za-z0-9_-]{8,200}")

    private fun request(url: String) = Request.Builder().url(url)
        .header("Authorization", "Bearer $token").header("Accept", "application/json")

    private fun readLimited(stream: InputStream, max: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val n = stream.read(chunk)
            if (n < 0) break
            require(out.size() + n <= max) { "Drive response too large" }
            out.write(chunk, 0, n)
        }
        return out.toByteArray()
    }

    private fun json(request: Request, limit: Int = 100_000): JSONObject =
        http.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "Drive metadata request failed (${response.code})" }
            JSONObject(readLimited(response.body?.byteStream()
                ?: error("Empty Drive response"), limit).toString(Charsets.UTF_8))
        }

    private fun folder(createIfMissing: Boolean): String? {
        val query = "name = 'NightWave Music' and " +
            "mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val list = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("q", query).addQueryParameter("fields", "files(id,name,mimeType)")
            .addQueryParameter("pageSize", "30").build().toString()
        val files = json(request(list).get().build()).getJSONArray("files")
        for (i in 0 until files.length()) {
            val record = files.getJSONObject(i)
            val id = record.optString("id")
            if (fileId.matches(id) &&
                record.optString("name") == LockerAudioPolicy.FOLDER_NAME &&
                record.optString("mimeType") == "application/vnd.google-apps.folder") return id
        }
        if (!createIfMissing) return null
        val metadata = JSONObject().put("name", LockerAudioPolicy.FOLDER_NAME)
            .put("mimeType", "application/vnd.google-apps.folder")
        val create = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("fields", "id,name,mimeType").build().toString()
        val created = json(request(create)
            .post(metadata.toString().toRequestBody(jsonType)).build())
        val id = created.optString("id")
        require(fileId.matches(id) &&
            created.optString("name") == LockerAudioPolicy.FOLDER_NAME &&
            created.optString("mimeType") == "application/vnd.google-apps.folder") {
            "New Drive music folder not verified"
        }
        return id
    }

    /**
     * Read-only, bounded metadata inventory. Drive continuation tokens stay
     * in Activity memory only and are never included in logs or on disk.
     */
    fun inventory(): LockerCloudInventory =
        folder(createIfMissing = false)?.let { inventoryInFolder(it) }
            ?: LockerCloudInventory(emptyList())

    /** Explicit user-initiated next batch. Reject stale folder/cursor state. */
    fun inventoryMore(previous: LockerCloudInventory): LockerCloudInventory {
        require(LockerVaultBrowsePolicy.canLoadMore(previous)) {
            "No safe Drive continuation is available; refresh the folder."
        }
        val folder = requireNotNull(folder(createIfMissing = false)) {
            "NightWave Music folder is no longer accessible"
        }
        require(folder == previous.folderId) {
            "Drive folder changed; refresh before continuing"
        }
        return inventoryInFolder(folder, previous)
    }

    /** Compatibility for older callers; no remote mutation. */
    fun list(): List<LockerRemoteTrack> = inventory().tracks

    private fun inventoryInFolder(
        folder: String,
        previous: LockerCloudInventory? = null
    ): LockerCloudInventory {
        require(fileId.matches(folder))
        if (previous != null) require(previous.folderId == folder &&
            previous.pagesRead in 1 until LockerVaultBrowsePolicy.MAX_TOTAL_PAGES &&
            previous.tracks.size <= LockerVaultBrowsePolicy.MAX_TOTAL_PAGES *
                LockerVaultBrowsePolicy.PAGE_SIZE)

        val result = (previous?.tracks.orEmpty()).toMutableList()
        val seenIds = result.mapTo(mutableSetOf()) { it.id }
        val seenTokens = (previous?.seenTokens.orEmpty()).toMutableSet()
        var token: String? = previous?.nextPageToken
        if (previous != null) require(token != null && token in seenTokens)
        val previousPages = previous?.pagesRead ?: 0
        val remaining = LockerVaultBrowsePolicy.MAX_TOTAL_PAGES - previousPages
        repeat(minOf(LockerVaultBrowsePolicy.MAX_PAGES, remaining)) { pageIndex ->
            val builder = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                .addQueryParameter("q", "'$folder' in parents and trashed = false")
                .addQueryParameter("fields",
                    "nextPageToken,files(id,name,mimeType,size,md5Checksum,appProperties)")
                .addQueryParameter("pageSize", LockerVaultBrowsePolicy.PAGE_SIZE.toString())
            token?.let {
                require(it.length in 1..2048) { "Drive cursor is invalid" }
                builder.addQueryParameter("pageToken", it)
            }
            val page = json(request(builder.build().toString()).get().build(), 150_000)
            val records = page.getJSONArray("files")
            require(records.length() <= LockerVaultBrowsePolicy.PAGE_SIZE)
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                val props = record.optJSONObject("appProperties")
                val size = record.optString("size").toLongOrNull() ?: continue
                val track = LockerRemoteTrack(record.optString("id"), record.optString("name"),
                    record.optString("mimeType"), size, record.optString("md5Checksum"),
                    props?.optString("nightwave_sha256").orEmpty(),
                    props?.optString("nightwave_local_id").orEmpty())
                if (runCatching { LockerAudioPolicy.validateRemote(track) }.isSuccess &&
                    seenIds.add(track.id)) result.add(track)
            }
            val next = page.optString("nextPageToken")
            if (next.isBlank()) return LockerCloudInventory(
                tracks = result, hasMore = false, pagesRead = previousPages + pageIndex + 1,
                folderId = folder, seenTokens = emptySet())
            require(next.length <= 2048 && seenTokens.add(next)) {
                "Invalid or repeated Drive page token"
            }
            token = next
        }
        // A maximum of 4,000 records is deliberately held in memory. A
        // remaining cursor is retained only while another batch is allowed.
        val totalPages = previousPages +
            minOf(LockerVaultBrowsePolicy.MAX_PAGES, remaining)
        return LockerCloudInventory(
            tracks = result, hasMore = true, pagesRead = totalPages,
            nextPageToken = token.takeIf {
                totalPages < LockerVaultBrowsePolicy.MAX_TOTAL_PAGES
            }, folderId = folder,
            seenTokens = if (totalPages < LockerVaultBrowsePolicy.MAX_TOTAL_PAGES)
                seenTokens else emptySet()
        )
    }

    /**
     * Foreground, explicitly selected Drive download. Never trusts only a
     * list row: check the file's exact current metadata, parent and download
     * capability before accepting bytes. A temporary file is deleted on any
     * network, cancellation, length or checksum failure.
     */
    fun downloadToStaging(
        selected: LockerRemoteTrack, stagingDirectory: File, progress: (Int) -> Unit
    ): File {
        LockerAudioPolicy.requireDownloadable(selected)
        val parent = requireNotNull(folder(createIfMissing = false)) {
            "No NightWave Music folder found"
        }
        // Exact file metadata below independently verifies selected identity,
        // current parent membership and canDownload. A capped folder scan
        // would incorrectly reject valid songs beyond the first 500 records.
        val metaUrl = "https://www.googleapis.com/drive/v3/files/${selected.id}"
            .toHttpUrl().newBuilder()
            .addQueryParameter("fields",
                "id,name,mimeType,size,md5Checksum,appProperties,parents,capabilities(canDownload)")
            .build().toString()
        val metadata = json(request(metaUrl).get().build(), 32_000)
        val properties = metadata.optJSONObject("appProperties")
        val current = LockerAudioPolicy.requireDownloadable(LockerRemoteTrack(
            metadata.optString("id"), metadata.optString("name"),
            metadata.optString("mimeType"),
            metadata.optString("size").toLongOrNull() ?: -1L,
            metadata.optString("md5Checksum"),
            properties?.optString("nightwave_sha256").orEmpty(),
            properties?.optString("nightwave_local_id").orEmpty()
        ))
        require(current == selected &&
            metadata.optJSONArray("parents")?.let { arr ->
                (0 until arr.length()).any { arr.optString(it) == parent }
            } == true &&
            metadata.optJSONObject("capabilities")?.optBoolean("canDownload") == true) {
            "Cloud song metadata or download permission changed"
        }
        require(stagingDirectory.canonicalFile.name == "locker_staging" &&
            stagingDirectory.parentFile?.canonicalFile?.isDirectory == true) {
            "Invalid NightWave staging directory"
        }
        require(stagingDirectory.isDirectory || stagingDirectory.mkdirs()) {
            "Could not create private download staging area"
        }
        val partial = File(stagingDirectory,
            UUID.randomUUID().toString() + ".pending")
        require(partial.createNewFile()) { "Unable to create a private download file" }
        try {
            val downloadUrl = "https://www.googleapis.com/drive/v3/files/${selected.id}"
                .toHttpUrl().newBuilder().addQueryParameter("alt", "media")
                .build().toString()
            http.newCall(request(downloadUrl).get().build()).execute().use { response ->
                require(response.code == 200) {
                    "Google Drive audio download failed (${response.code})"
                }
                val announced = response.body?.contentLength() ?: -1L
                require(announced == -1L || announced == selected.bytes) {
                    "Google Drive audio length changed"
                }
                val input = response.body?.byteStream() ?: error("Empty cloud audio response")
                var received = 0L
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        check(!Thread.currentThread().isInterrupted) {
                            "Cloud download cancelled"
                        }
                        val read = input.read(buffer)
                        if (read < 0) break
                        received += read
                        require(received <= selected.bytes &&
                            received <= LockerAudioPolicy.MAX_FILE_BYTES) {
                            "Cloud audio exceeded expected length"
                        }
                        output.write(buffer, 0, read)
                        progress((received * 100L / selected.bytes).toInt().coerceAtMost(99))
                    }
                    output.fd.sync()
                }
                require(received == selected.bytes) { "Cloud audio download incomplete" }
            }
            val (sha256, md5) = LockerAudioPolicy.hashes(partial)
            require(sha256 == selected.sha256 &&
                md5.equals(selected.md5, ignoreCase = true)) {
                "Downloaded audio failed SHA-256 or MD5 verification"
            }
            progress(100)
            return partial
        } catch (failure: Exception) {
            partial.delete()
            throw failure
        }
    }

    /**
     * Explicitly resumed foreground download. The file is re-authorized and
     * remote metadata checked before using a local checkpoint. Range bytes
     * are appended only after the server proves the exact requested offset.
     * A failed network request retains only fsynced, journaled bytes.
     */
    fun downloadWithRecovery(
        selected: LockerRemoteTrack,
        stagingDirectory: File,
        account: String,
        checkpoints: LockerDownloadCheckpointStore,
        progress: (Int) -> Unit
    ): File {
        LockerAudioPolicy.requireDownloadable(selected)
        require(account.contains('@') && account.length in 3..254)
        val parent = requireNotNull(folder(createIfMissing = false)) {
            "No NightWave Music folder found"
        }
        // Exact file metadata below independently verifies selected identity,
        // current parent membership and canDownload for any browsed page.
        val metaUrl = "https://www.googleapis.com/drive/v3/files/${selected.id}"
            .toHttpUrl().newBuilder()
            .addQueryParameter("fields",
                "id,name,mimeType,size,md5Checksum,appProperties,parents,capabilities(canDownload)")
            .build().toString()
        val metadata = json(request(metaUrl).get().build(), 32_000)
        val props = metadata.optJSONObject("appProperties")
        val fresh = LockerAudioPolicy.requireDownloadable(LockerRemoteTrack(
            metadata.optString("id"), metadata.optString("name"),
            metadata.optString("mimeType"), metadata.optString("size").toLongOrNull() ?: -1L,
            metadata.optString("md5Checksum"),
            props?.optString("nightwave_sha256").orEmpty(),
            props?.optString("nightwave_local_id").orEmpty()
        ))
        require(fresh == selected && metadata.optJSONArray("parents")?.let { arr ->
            (0 until arr.length()).any { arr.optString(it) == parent }
        } == true &&
            metadata.optJSONObject("capabilities")?.optBoolean("canDownload") == true) {
            "Cloud download authorization or song identity changed"
        }
        val checkpoint = checkpoints.begin(account, selected)
        val partial = checkpoints.stagedFile(checkpoint)
        require(partial.parentFile == stagingDirectory.canonicalFile) {
            "Download staging folder changed"
        }
        if (!partial.exists()) {
            require(checkpoint.committedBytes == 0L && partial.createNewFile()) {
                "Partial download disappeared; discard the old checkpoint"
            }
        }
        require(partial.isFile && partial.canRead() &&
            partial.length() >= checkpoint.committedBytes) {
            "Partial file was truncated; discard the old checkpoint"
        }

        val endpoint = "https://www.googleapis.com/drive/v3/files/${selected.id}"
            .toHttpUrl().newBuilder().addQueryParameter("alt", "media").build().toString()
        // A crash can leave bytes beyond the last durable checkpoint. Never
        // accept or request a Range based on those uncommitted bytes.
        var attempts = 0
        while (attempts < LockerResumePolicy.MAX_ATTEMPTS) {
            check(!Thread.currentThread().isInterrupted) { "Download paused" }
            val durable = requireNotNull(checkpoints.read()) { "Recovery record missing" }
            require(durable.account.equals(account, true) && durable.remote == selected &&
                durable.fileName == checkpoint.fileName) { "Recovery record changed" }
            java.io.RandomAccessFile(partial, "rw").use { file ->
                require(file.length() >= durable.committedBytes)
                file.setLength(durable.committedBytes)
            }
            val offset = durable.committedBytes
            if (offset == selected.bytes) break
            try {
                val builder = request(endpoint)
                if (offset > 0L) builder.header("Range", "bytes=$offset-")
                http.newCall(builder.get().build()).execute().use { response ->
                    if (response.code in listOf(408, 429, 500, 502, 503, 504)) {
                        throw java.io.IOException("Google Drive temporarily unavailable")
                    }
                    val body = response.body ?: error("Drive audio response missing")
                    val span = LockerResumePolicy.verifiedSpan(
                        offset, selected.bytes, response.code,
                        response.header("Content-Range"), body.contentLength()
                    )
                    var received = 0L
                    var sinceSync = 0L
                    FileOutputStream(partial, true).use { output ->
                        val input = body.byteStream()
                        val buffer = ByteArray(32 * 1024)
                        while (received < span) {
                            check(!Thread.currentThread().isInterrupted) { "Download paused" }
                            val count = input.read(buffer, 0,
                                minOf(buffer.size.toLong(), span - received).toInt())
                            if (count < 0) throw java.io.EOFException("Short Drive audio response")
                            if (count == 0) throw java.io.IOException("Drive stalled during resume")
                            received += count
                            require(offset + received <= selected.bytes)
                            output.write(buffer, 0, count)
                            sinceSync += count
                            if (sinceSync >= LockerResumePolicy.CHECKPOINT_STEP) {
                                output.fd.sync()
                                checkpoints.commit(durable, offset + received)
                                progress(LockerResumePolicy.percent(offset + received, selected.bytes))
                                sinceSync = 0L
                            }
                        }
                        output.fd.sync()
                        checkpoints.commit(durable, offset + received)
                        progress(LockerResumePolicy.percent(offset + received, selected.bytes))
                    }
                }
                attempts = 0
            } catch (ex: java.io.IOException) {
                attempts++
                if (!LockerResumePolicy.shouldRetry(attempts, ex)) throw ex
                check(!Thread.currentThread().isInterrupted) { "Download paused" }
                Thread.sleep(250L * attempts)
            }
        }
        val completed = requireNotNull(checkpoints.read()) { "Download checkpoint missing" }
        require(completed.remote == selected && completed.committedBytes == selected.bytes &&
            partial.length() == selected.bytes) { "Cloud audio transfer incomplete" }
        val (sha256, md5) = LockerAudioPolicy.hashes(partial)
        if (sha256 != selected.sha256 || !md5.equals(selected.md5, true)) {
            checkpoints.discard()
            error("Recovered download failed checksum verification")
        }
        progress(100)
        return partial
    }

    /**
     * Parse an actual Drive 200/201 completion acknowledgement. The local SHA
     * and Google-computed MD5 must match; no speculative 100% on a 308 reply.
     */
    private fun uploadCompletion(
        response: okhttp3.Response,
        song: OfflineSong,
        expectedParent: String,
        sha256: String,
        md5: String
    ): LockerUploadOutcome {
        require(response.code == 200 || response.code == 201) {
            "Upload has not completed"
        }
        val bytes = readLimited(response.body?.byteStream()
            ?: error("Missing Drive upload acknowledgement"), 16_384)
        val data = JSONObject(bytes.toString(Charsets.UTF_8))
        return LockerDriveReceiptPolicy.requireCompletedUpload(
            data, song, expectedParent, sha256, md5
        )
    }

    private sealed class UploadProbe {
        data class Resume(val offset: Long) : UploadProbe()
        data class Complete(val outcome: LockerUploadOutcome) : UploadProbe()
    }

    /** Google requires a zero-byte PUT status query to the current session. */
    private fun queryUploadStatus(
        session: String,
        song: OfflineSong,
        parent: String,
        sha256: String,
        md5: String,
        confirmed: Long
    ): UploadProbe {
        val request = request(session)
            .header("Content-Range", "bytes */${song.bytes}")
            .put(ByteArray(0).toRequestBody()).build()
        http.newCall(request).execute().use { response ->
            return when (response.code) {
                200, 201 -> UploadProbe.Complete(
                    uploadCompletion(response, song, parent, sha256, md5))
                308 -> UploadProbe.Resume(LockerUploadResumePolicy.acknowledged(
                    response.header("Range"), song.bytes, confirmed))
                else -> {
                    if (LockerUploadResumePolicy.transientStatus(response.code)) {
                        throw java.io.IOException("Temporary Drive upload status failure")
                    }
                    error("Drive upload session expired or refused status check")
                }
            }
        }
    }

    /**
     * Targeted Drive query for a matching SHA-256 in this authorized folder.
     * Unlike a 500-file inventory scan, it scales with the number of matching
     * hashes and refuses to upload when Drive cannot prove the search ended.
     */
    private fun findMatchingUpload(
        folder: String, song: OfflineSong, sha256: String, md5: String
    ): LockerRemoteTrack? {
        require(fileId.matches(folder) && LockerAudioPolicy.validSha256(sha256))
        val q = "'$folder' in parents and trashed = false and " +
            "appProperties has { key='nightwave_sha256' and value='$sha256' }"
        var cursor: String? = null
        val seenTokens = mutableSetOf<String>()
        repeat(LockerVaultBrowsePolicy.MAX_DUPLICATE_QUERY_PAGES) {
            val builder = "https://www.googleapis.com/drive/v3/files".toHttpUrl()
                .newBuilder()
                .addQueryParameter("q", q)
                .addQueryParameter("fields",
                    "nextPageToken,files(id,name,mimeType,size,md5Checksum,appProperties)")
                .addQueryParameter("pageSize", LockerVaultBrowsePolicy.PAGE_SIZE.toString())
            cursor?.let { builder.addQueryParameter("pageToken", it) }
            val page = json(request(builder.build().toString()).get().build(), 150_000)
            val items = page.getJSONArray("files")
            require(items.length() <= LockerVaultBrowsePolicy.PAGE_SIZE)
            for (index in 0 until items.length()) {
                val obj = items.getJSONObject(index)
                val properties = obj.optJSONObject("appProperties")
                val candidate = LockerRemoteTrack(
                    obj.optString("id"), obj.optString("name"), obj.optString("mimeType"),
                    obj.optString("size").toLongOrNull() ?: -1L,
                    obj.optString("md5Checksum"),
                    properties?.optString("nightwave_sha256").orEmpty(),
                    properties?.optString("nightwave_local_id").orEmpty()
                )
                // Invalid / mismatched targeted records need investigation,
                // not a speculative duplicate upload.
                LockerAudioPolicy.validateRemote(candidate)
                require(candidate.sha256 == sha256) {
                    "Drive returned a mismatched upload checksum"
                }
                if (candidate.bytes == song.bytes && candidate.mimeType == song.mimeType &&
                    candidate.md5.equals(md5, ignoreCase = true)) return candidate
            }
            val next = page.optString("nextPageToken")
            if (next.isBlank()) return null
            require(next.length in 1..2048 && seenTokens.add(next)) {
                "Invalid or repeated upload-check cursor"
            }
            cursor = next
        }
        error("Cloud duplicate search incomplete; upload not started.")
    }

    /** Upload user-imported audio with bounded, status-reconciled in-process recovery. */
    fun upload(song: OfflineSong, file: File, progress: (Int) -> Unit): LockerUploadOutcome {
        LockerAudioPolicy.validateLocal(song, file)
        val (sha256, md5) = LockerAudioPolicy.hashes(file)
        val parent = requireNotNull(folder(createIfMissing = true)) {
            "Could not create the NightWave Music folder"
        }
        // Only a complete, SHA-targeted query can authorize a new upload.
        // Search failure, pagination loops or an excess of matching hashes
        // prevent uploading instead of accidentally duplicating cloud audio.
        val existing = findMatchingUpload(parent, song, sha256, md5)
        if (existing != null) return LockerUploadOutcome(existing, created = false)
        val metadata = JSONObject().put("name", LockerAudioPolicy.fileName(song))
            .put("mimeType", song.mimeType).put("parents", JSONArray().put(parent))
            .put("appProperties", JSONObject()
                .put("nightwave_sha256", sha256)
                .put("nightwave_local_id", song.id))
        val start = "https://www.googleapis.com/upload/drive/v3/files".toHttpUrl()
            .newBuilder().addQueryParameter("uploadType", "resumable")
            .addQueryParameter("fields", "id,name,mimeType,size,md5Checksum,parents,appProperties")
            .build().toString()
        val location = http.newCall(request(start)
            .header("X-Upload-Content-Type", song.mimeType)
            .header("X-Upload-Content-Length", song.bytes.toString())
            .post(metadata.toString().toRequestBody(jsonType)).build()).execute().use { response ->
            require(response.isSuccessful) { "Drive upload setup failed (${response.code})" }
            response.header("Location") ?: error("Missing resumable upload session")
        }
        val uri = location.toHttpUrlOrNull() ?: error("Invalid upload session URI")
        require(uri.isHttps &&
            (uri.host == "www.googleapis.com" || uri.host.endsWith(".googleapis.com"))) {
            "Untrusted Drive upload session host"
        }
        // Never write uri (an upload-bearing capability) to disk or diagnostics.
        val session = uri.toString()
        var offset = 0L
        var failures = 0
        RandomAccessFile(file, "r").use { reader ->
            while (offset < song.bytes) {
                check(!Thread.currentThread().isInterrupted) { "Upload cancelled" }
                check(LockerUploadResumePolicy.canSendNext(offset, song.bytes)) {
                    "Drive status returned an unsupported non-final chunk boundary"
                }
                val remaining = song.bytes - offset
                val count = minOf(LockerAudioPolicy.CHUNK_BYTES.toLong(), remaining).toInt()
                val bytes = ByteArray(count)
                reader.seek(offset)
                reader.readFully(bytes)
                val end = offset + count - 1
                try {
                    val part = request(session)
                        .header("Content-Range", "bytes $offset-$end/${song.bytes}")
                        .put(bytes.toRequestBody(song.mimeType.toMediaType())).build()
                    http.newCall(part).execute().use { response ->
                        when (response.code) {
                            200, 201 -> {
                                val complete = uploadCompletion(
                                    response, song, parent, sha256, md5)
                                progress(100)
                                return complete
                            }
                            308 -> {
                                val next = LockerUploadResumePolicy.acknowledged(
                                    response.header("Range"), song.bytes, offset)
                                require(next <= end + 1) {
                                    "Drive acknowledged bytes not sent in this chunk"
                                }
                                if (next == offset) {
                                    throw java.io.IOException("No accepted upload progress")
                                }
                                offset = next
                                failures = 0
                                progress((offset * 100 / song.bytes).toInt().coerceAtMost(99))
                            }
                            else -> {
                                if (LockerUploadResumePolicy.transientStatus(response.code)) {
                                    throw java.io.IOException("Temporary Drive upload failure")
                                }
                                error("Drive rejected the upload request")
                            }
                        }
                    }
                } catch (interrupted: java.io.IOException) {
                    failures++
                    if (!LockerUploadResumePolicy.retryAllowed(failures)) throw interrupted
                    check(!Thread.currentThread().isInterrupted) { "Upload cancelled" }
                    Thread.sleep(300L * failures)
                    // Status probe is essential: the network may have lost a
                    // successful chunk acknowledgement. Never resend blindly.
                    val status = try {
                        queryUploadStatus(session, song, parent, sha256, md5, offset)
                    } catch (probeFailure: java.io.IOException) {
                        failures++
                        if (!LockerUploadResumePolicy.retryAllowed(failures))
                            throw probeFailure
                        continue
                    }
                    when (status) {
                        is UploadProbe.Complete -> {
                            progress(100)
                            return status.outcome
                        }
                        is UploadProbe.Resume -> {
                            if (status.offset > offset) {
                                offset = status.offset
                                failures = 0
                                progress((offset * 100 / song.bytes).toInt().coerceAtMost(99))
                            }
                            // An unchanged status is not success: bounded retry
                            // will resend only from Drive's confirmed offset.
                        }
                    }
                }
            }
        }
        // A 308 acknowledgement that covered all source bytes isn't proof of
        // final file creation. Demand the Drive 200/201 resource before success.
        when (val completion = queryUploadStatus(
            session, song, parent, sha256, md5, offset)) {
            is UploadProbe.Complete -> {
                progress(100)
                return completion.outcome
            }
            is UploadProbe.Resume -> error("Drive has not finalized the upload")
        }
    }
}
