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

data class DriveSnapshot(
    val id: String, val name: String, val sizeBytes: Long? = null
)

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
            if (!response.isSuccessful) throw DriveHttpFailure(response.code)
            val source = response.body?.byteStream() ?: error("Empty Drive response")
            val buffer = ByteArray(8192)
            val result = ByteArrayOutputStream()
            var consecutiveEmptyReads = 0
            while (true) {
                val count = source.read(buffer)
                if (count < 0) break
                if (count == 0) {
                    check(++consecutiveEmptyReads <= 8) {
                        "Google Drive response did not make progress"
                    }
                    continue
                }
                consecutiveEmptyReads = 0
                // Subtraction avoids integer overflow if a future API
                // unexpectedly advertises a very large response.
                require(count <= maxBytes - result.size()) {
                    "Drive response too large"
                }
                result.write(buffer, 0, count)
            }
            return result.toByteArray()
        }
    }

    private fun request(url: String): Request.Builder = Request.Builder()
        .url(url).header("Authorization", "Bearer $accessToken")
        .header("Accept", "application/json")

    /**
     * Resolve the Google account from the already authorized Drive token.
     * AuthorizationResult.toGoogleSignInAccount() can legally be null after
     * an interactive scope grant; don't use a missing legacy profile as proof
     * that the Drive grant failed. No extra OAuth scope is requested.
     */
    fun authorizedEmailAddress(): String {
        val url = "https://www.googleapis.com/drive/v3/about".toHttpUrl()
            .newBuilder().addQueryParameter("fields", "user(emailAddress)")
            .build().toString()
        val payload = perform(request(url).get().build(), 16_384)
        val email = JSONObject(payload.toString(Charsets.UTF_8))
            .optJSONObject("user")?.optString("emailAddress").orEmpty().trim()
        check(email.length in 3..254 && '@' in email &&
            email.substringAfter('@').contains('.') &&
            email.none { it.isWhitespace() }) {
            "Google Drive did not provide account identity"
        }
        return email
    }

    /**
     * List bounded app-only backup history. Metadata from Drive is a display
     * hint, never a validation or a basis for recovery; selected snapshots are
     * downloaded and checksum-verified before any restore preview.
     */
    fun listSnapshots(): List<DriveSnapshot> {
        val output = mutableListOf<DriveSnapshot>()
        val seenIds = mutableSetOf<String>()
        val seenTokens = mutableSetOf<String>()
        var nextToken: String? = null
        repeat(3) {
            val builder = "https://www.googleapis.com/drive/v3/files".toHttpUrl()
                .newBuilder()
                .addQueryParameter("spaces", "appDataFolder")
                .addQueryParameter("q", "name contains 'nightwave-cloud-v2-' and trashed = false")
                .addQueryParameter("orderBy", "createdTime desc")
                .addQueryParameter("fields", "nextPageToken,files(id,name,size)")
                .addQueryParameter("pageSize", "100")
            nextToken?.let { builder.addQueryParameter("pageToken", it) }
            val json = JSONObject(perform(request(builder.build().toString()).get().build(), 100_000)
                .toString(Charsets.UTF_8))
            val files = json.getJSONArray("files")
            for (i in 0 until files.length()) {
                val file = files.getJSONObject(i)
                val id = file.optString("id")
                val name = file.optString("name")
                if (id.isBlank() || !expectedName.matches(name) || !seenIds.add(id)) continue
                val size = file.optString("size").toLongOrNull()?.takeIf {
                    it in 1..CloudBackupCodec.MAX_BYTES.toLong()
                }
                output.add(DriveSnapshot(id, name, size))
            }
            val upcoming = json.optString("nextPageToken").takeIf { it.isNotBlank() }
                ?: return output.sortedByDescending { it.name }
            require(upcoming.length <= 2048 && seenTokens.add(upcoming)) {
                "Invalid Drive listing page token"
            }
            nextToken = upcoming
        }
        return output.sortedByDescending { it.name }
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
