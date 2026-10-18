package com.nightwave.player.v2.cloud

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

data class DriveSnapshot(val id: String, val name: String)

data class DriveBackupOutcome(val snapshot: DriveSnapshot, val created: Boolean)

/**
 * Uses only the Drive appDataFolder. Each successful upload creates a separate
 * snapshot; never overwrites/deletes existing remote data. OAuth tokens are
 * request-local and are never persisted.
 */
class NightWaveDriveClient(private val accessToken: String) {
    init { require(accessToken.isNotBlank()) }
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()
    private val prefix = "nightwave-cloud-v2-"
    private val expectedName = Regex("^nightwave-cloud-v2-[0-9]{13}-[a-f0-9]{8}\\.json$")

    private fun perform(request: Request, maxBytes: Int): ByteArray {
        http.newCall(request).execute().use { response ->
            // Never log token, account, raw URL, or remote response body.
            if (!response.isSuccessful) throw IllegalStateException(
                "Google Drive request failed (HTTP ${response.code})"
            )
            val source = response.body?.byteStream() ?: error("Empty Drive response")
            val buffer = ByteArray(8192)
            val result = ByteArrayOutputStream()
            while (true) {
                val count = source.read(buffer)
                if (count < 0) break
                require(result.size() + count <= maxBytes) { "Drive response too large" }
                result.write(buffer, 0, count)
            }
            return result.toByteArray()
        }
    }

    private fun request(url: String): Request.Builder = Request.Builder()
        .url(url).header("Authorization", "Bearer $accessToken")
        .header("Accept", "application/json")

    fun listSnapshots(): List<DriveSnapshot> {
        val endpoint = "https://www.googleapis.com/drive/v3/files".toHttpUrl()
            .newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name contains 'nightwave-cloud-v2-' and trashed = false")
            .addQueryParameter("orderBy", "createdTime desc")
            .addQueryParameter("fields", "nextPageToken,files(id,name)")
            .addQueryParameter("pageSize", "100")
            .build().toString()
        val json = JSONObject(perform(request(endpoint).get().build(), 100_000)
            .toString(Charsets.UTF_8))
        val files = json.getJSONArray("files")
        // Prefer a visible bounded snapshot set; never guess or read other files.
        return (0 until files.length()).mapNotNull { i ->
            val file = files.getJSONObject(i)
            val id = file.optString("id")
            val name = file.optString("name")
            if (id.isBlank() || !expectedName.matches(name)) null
            else DriveSnapshot(id, name)
        }.sortedByDescending { it.name }
    }

    fun download(id: String): ByteArray {
        require(id.matches(Regex("[a-zA-Z0-9_-]{8,200}"))) { "Invalid Drive file ID" }
        val url = "https://www.googleapis.com/drive/v3/files/$id".toHttpUrl()
            .newBuilder().addQueryParameter("alt", "media").build().toString()
        return perform(request(url).get().build(), CloudBackupCodec.MAX_BYTES)
    }

    /**
     * Smart, no-deletion upload. Compare with the latest *verified* Drive
     * snapshot, not a local checksum/cache that could survive remote deletion.
     * Network/validation failures during the comparison cannot claim success.
     * A new copy-on-write snapshot is still uploaded and read back.
     */
    fun backUpIfChangedVerified(bytes: ByteArray): DriveBackupOutcome {
        CloudBackupCodec.decode(bytes)
        val latest = listSnapshots().firstOrNull()
        if (latest != null) {
            val remote = runCatching { download(latest.id) }.getOrNull()
            if (remote != null && CloudSnapshotPolicy.sameVerifiedContent(bytes, remote)) {
                return DriveBackupOutcome(latest, created = false)
            }
        }
        return DriveBackupOutcome(uploadVerified(bytes), created = true)
    }

    fun uploadVerified(bytes: ByteArray): DriveSnapshot {
        require(bytes.size in 1..CloudBackupCodec.MAX_BYTES)
        CloudBackupCodec.decode(bytes)
        val name = prefix + System.currentTimeMillis() + "-" +
            UUID.randomUUID().toString().take(8) + ".json"
        val boundary = "nightwave_" + UUID.randomUUID().toString().replace("-", "")
        val metadata = JSONObject()
            .put("name", name)
            .put("mimeType", "application/json")
            .put("parents", org.json.JSONArray().put("appDataFolder")).toString()
        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"
                .toByteArray(Charsets.UTF_8))
            write(metadata.toByteArray(Charsets.UTF_8))
            write("\r\n--$boundary\r\nContent-Type: application/json\r\n\r\n"
                .toByteArray(Charsets.UTF_8))
            write(bytes)
            write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        }.toByteArray()
        val endpoint = "https://www.googleapis.com/upload/drive/v3/files".toHttpUrl()
            .newBuilder()
            .addQueryParameter("uploadType", "multipart")
            .addQueryParameter("fields", "id,name")
            .build().toString()
        val response = JSONObject(perform(request(endpoint).post(
            body.toRequestBody("multipart/related; boundary=$boundary".toMediaType())
        ).build(), 16_000).toString(Charsets.UTF_8))
        val id = response.optString("id")
        require(response.optString("name") == name && id.isNotBlank()) {
            "Drive upload acknowledgement was incomplete"
        }
        val remote = download(id)
        check(remote.contentEquals(bytes)) { "Drive upload read-back mismatch" }
        CloudBackupCodec.decode(remote)
        return DriveSnapshot(id, name)
    }
}
