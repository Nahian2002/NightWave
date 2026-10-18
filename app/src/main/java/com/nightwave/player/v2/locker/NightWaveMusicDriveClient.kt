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

    fun list(): List<LockerRemoteTrack> =
        folder(createIfMissing = false)?.let { listInFolder(it) }.orEmpty()

    private fun listInFolder(folder: String): List<LockerRemoteTrack> {
        require(fileId.matches(folder))
        val result = mutableListOf<LockerRemoteTrack>()
        val seenIds = mutableSetOf<String>()
        val seenTokens = mutableSetOf<String>()
        var token: String? = null
        repeat(2) {
            val builder = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                .addQueryParameter("q", "'$folder' in parents and trashed = false")
                .addQueryParameter("fields", "nextPageToken,files(id,name,mimeType,size,md5Checksum,appProperties)")
                .addQueryParameter("pageSize", "100")
            token?.let { builder.addQueryParameter("pageToken", it) }
            val page = json(request(builder.build().toString()), 150_000)
            val records = page.getJSONArray("files")
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                val props = record.optJSONObject("appProperties")
                val size = record.optString("size").toLongOrNull() ?: continue
                val track = LockerRemoteTrack(record.optString("id"), record.optString("name"),
                    record.optString("mimeType"), size, record.optString("md5Checksum"),
                    props?.optString("nightwave_sha256").orEmpty())
                if (runCatching { LockerAudioPolicy.validateRemote(track) }.isSuccess &&
                    seenIds.add(track.id)) result.add(track)
            }
            val next = page.optString("nextPageToken")
            if (next.isBlank()) return result
            require(next.length <= 2048 && seenTokens.add(next)) { "Invalid Drive page token" }
            token = next
        }
        return result
    }

    /** Upload one already-imported user audio file; no automatic media scraping. */
    fun upload(song: OfflineSong, file: File, progress: (Int) -> Unit): LockerUploadOutcome {
        LockerAudioPolicy.validateLocal(song, file)
        val (sha256, md5) = LockerAudioPolicy.hashes(file)
        val parent = requireNotNull(folder(createIfMissing = true)) {
            "Could not create the NightWave Music folder"
        }
        val existing = listInFolder(parent).firstOrNull {
            it.sha256 == sha256 && it.bytes == song.bytes && it.mimeType == song.mimeType &&
                it.md5.equals(md5, true)
        }
        if (existing != null) return LockerUploadOutcome(existing, created = false)
        val meta = JSONObject().put("name", LockerAudioPolicy.fileName(song))
            .put("mimeType", song.mimeType).put("parents", JSONArray().put(parent))
            .put("appProperties", JSONObject()
                .put("nightwave_sha256", sha256).put("nightwave_local_id", song.id))
        val start = "https://www.googleapis.com/upload/drive/v3/files".toHttpUrl()
            .newBuilder().addQueryParameter("uploadType", "resumable")
            .addQueryParameter("fields", "id,name,mimeType,size,md5Checksum,parents,appProperties")
            .build().toString()
        val location = http.newCall(request(start)
            .header("X-Upload-Content-Type", song.mimeType)
            .header("X-Upload-Content-Length", song.bytes.toString())
            .post(meta.toString().toRequestBody(jsonType)).build()).execute().use { response ->
            require(response.isSuccessful) { "Drive upload setup failed (${response.code})" }
            response.header("Location") ?: error("Missing resumable upload session")
        }
        val session = location.toHttpUrlOrNull() ?: error("Invalid upload session")
        require(session.isHttps && (session.host == "www.googleapis.com" ||
            session.host.endsWith(".googleapis.com"))) { "Untrusted upload host" }
        var offset = 0L
        RandomAccessFile(file, "r").use { reader ->
            while (offset < song.bytes) {
                check(!Thread.currentThread().isInterrupted) { "Transfer cancelled" }
                val count = minOf(LockerAudioPolicy.CHUNK_BYTES.toLong(),
                    song.bytes - offset).toInt()
                val chunk = ByteArray(count)
                reader.seek(offset)
                reader.readFully(chunk)
                val end = offset + count - 1L
                val upload = request(session.toString())
                    .header("Content-Range", "bytes $offset-$end/${song.bytes}")
                    .put(chunk.toRequestBody(song.mimeType.toMediaType())).build()
                http.newCall(upload).execute().use { response ->
                    if (response.code == 308) {
                        val last = response.header("Range")?.substringAfter("bytes=0-")
                            ?.toLongOrNull()
                        val accepted = if (last == null) 0L else last + 1
                        require(accepted > offset && accepted <= end + 1) {
                            "Drive upload progress inconsistent"
                        }
                        offset = accepted
                        progress((offset * 100L / song.bytes).toInt())
                    } else {
                        require(response.isSuccessful && end + 1 == song.bytes) {
                            "Drive transfer failed (${response.code})"
                        }
                        val result = JSONObject(readLimited(response.body?.byteStream()
                            ?: error("Missing upload acknowledgement"), 16_384)
                            .toString(Charsets.UTF_8))
                        val props = result.optJSONObject("appProperties")
                        val row = LockerAudioPolicy.validateRemote(LockerRemoteTrack(
                            result.optString("id"), result.optString("name"),
                            result.optString("mimeType"),
                            result.optString("size").toLongOrNull() ?: -1,
                            result.optString("md5Checksum"),
                            props?.optString("nightwave_sha256").orEmpty()))
                        require(row.bytes == song.bytes && row.md5.equals(md5, true) &&
                            row.sha256 == sha256 &&
                            result.optJSONArray("parents")?.optString(0) == parent) {
                            "Drive upload checksum or parent mismatch"
                        }
                        progress(100)
                        return LockerUploadOutcome(row, created = true)
                    }
                }
            }
        }
        error("Drive did not acknowledge upload")
    }
}
