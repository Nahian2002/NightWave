package com.nightwave.player.v2.cloud

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.data.LibraryArchive
import com.nightwave.player.v2.data.LibraryBackupCodec
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

/** Executes the real Drive client and codecs through an in-memory HTTP interceptor.
 * No real account, token, network, audio or on-device preferences are accessed. */
class NightWaveDriveClientRoundTripTest {
    private val track = VideoPreview("Synthetic song", "Test creator",
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", 120L)
    private val archive = LibraryArchive("Disposable guest", "MIDNIGHT", "orbit",
        listOf(track), listOf("Test playlist" to listOf(track)))
    private val payload get() = CloudBackupCodec.encode(LibraryBackupCodec.encode(archive), emptyList(), false)

    private class FakeDrive(private val payload: ByteArray) {
        var stored: ByteArray? = null
        var name = ""
        var writes = 0
        var corruptReadback = false
        var downloadStatus = 200
        var unavailable = false
        val methods = mutableListOf<String>()
        val client = NightWaveDriveClient("test-only-token", OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("https", request.url.scheme)
            assertEquals("www.googleapis.com", request.url.host)
            assertEquals("Bearer test-only-token", request.header("Authorization"))
            methods += request.method
            if (unavailable) throw IOException("synthetic network unavailable")
            var code = 200
            val bytes = when (request.url.encodedPath) {
                "/drive/v3/about" -> """{"user":{"emailAddress":"guest@example.test"}}""".toByteArray()
                "/drive/v3/files" -> {
                    assertEquals("GET", request.method)
                    assertEquals("appDataFolder", request.url.queryParameter("spaces"))
                    JSONObject().put("files", JSONArray().also { items ->
                        if (stored != null) items.put(JSONObject().put("id", "snapshot_00001")
                            .put("name", name).put("size", stored!!.size.toString()))
                    }).toString().toByteArray()
                }
                "/upload/drive/v3/files" -> {
                    assertEquals("POST", request.method)
                    assertEquals("multipart", request.url.queryParameter("uploadType"))
                    val buffer = Buffer()
                    request.body!!.writeTo(buffer)
                    val body = buffer.readUtf8()
                    assertTrue(body.contains(payload.toString(Charsets.UTF_8)))
                    val metadata = JSONObject(body.lineSequence().first {
                        it.startsWith("{") && it.contains("\"parents\"") })
                    assertEquals("appDataFolder", metadata.getJSONArray("parents").getString(0))
                    name = metadata.getString("name")
                    stored = payload.copyOf()
                    writes++
                    JSONObject().put("id", "snapshot_00001").put("name", name).toString().toByteArray()
                }
                "/drive/v3/files/snapshot_00001" -> {
                    assertEquals("GET", request.method)
                    assertEquals("media", request.url.queryParameter("alt"))
                    code = downloadStatus
                    if (corruptReadback) "corrupt readback".toByteArray()
                    else requireNotNull(stored).copyOf()
                }
                else -> throw AssertionError("Unexpected fake Drive route")
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code)
                .message("Synthetic response").body(bytes.toResponseBody("application/json".toMediaType())).build()
        }.followRedirects(false).build())
    }

    @Test fun disposableGuestUploadReadbackAndRestoreDecodeRoundTrip() {
        val fake = FakeDrive(payload)
        assertEquals("guest@example.test", fake.client.authorizedEmailAddress())
        val result = fake.client.backUpIfChangedVerified(payload)
        assertTrue(result.created)
        val recovered = CloudBackupCodec.decode(fake.client.download(result.snapshot.id))
        assertEquals(archive, recovered.archive)
        assertEquals(1, fake.writes)
        assertFalse(fake.methods.contains("DELETE"))
        assertFalse(fake.methods.contains("PATCH"))
    }

    @Test fun unchangedVerifiedCopyIsReadAgainWithoutAnotherUpload() {
        val fake = FakeDrive(payload)
        assertTrue(fake.client.backUpIfChangedVerified(payload).created)
        assertFalse(fake.client.backUpIfChangedVerified(payload).created)
        assertEquals(1, fake.writes)
    }

    @Test fun corruptReadbackNeverProducesAVerifiedOutcome() {
        val fake = FakeDrive(payload).apply { corruptReadback = true }
        assertThrows(IllegalStateException::class.java) { fake.client.backUpIfChangedVerified(payload) }
        assertEquals(1, fake.writes) // Copy exists remotely, but was not acknowledged verified.
    }

    @Test fun deniedReadbackKeepsTheActualHttpCode() {
        val fake = FakeDrive(payload).apply { downloadStatus = 401 }
        val error = assertThrows(DriveHttpFailure::class.java) { fake.client.backUpIfChangedVerified(payload) }
        assertEquals(401, error.statusCode)
    }

    @Test fun networkOrInvalidInputCannotClaimSuccessOrWriteAnotherCopy() {
        val fake = FakeDrive(payload).apply { unavailable = true }
        assertThrows(IOException::class.java) { fake.client.backUpIfChangedVerified(payload) }
        assertEquals(0, fake.writes)
        fake.unavailable = false
        assertThrows(Exception::class.java) { fake.client.backUpIfChangedVerified("{}".toByteArray()) }
        assertEquals(0, fake.writes)
    }
}
