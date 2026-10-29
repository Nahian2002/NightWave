package com.nightwave.player.offline

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class AudioRangeTransferTest {
    private lateinit var root: File
    private lateinit var server: MockWebServer
    private val client = OkHttpClient.Builder().callTimeout(3, TimeUnit.SECONDS).build()
    private val bytes = ByteArray(2 * 1024 * 1024 + 771) { ((it * 31 + it / 17) and 255).toByte() }
    private val key = AudioRangePolicy.key("fixture-recording-representation")
    private var etag = "\"fixture-v1\""
    private val starts = mutableListOf<Long>()

    @Before fun before() {
        root = Files.createTempDirectory("nw136-test").toFile()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = rangeResponse(request)
        }
        server.start()
    }
    @After fun after() { server.shutdown(); root.deleteRecursively() }

    private fun rangeResponse(request: RecordedRequest): MockResponse {
        val range = request.getHeader("Range")!!.removePrefix("bytes=").split('-')
        val start = range[0].toLong()
        val end = minOf(range[1].toLong(), bytes.lastIndex.toLong())
        starts.add(start)
        return MockResponse().setResponseCode(206)
            .setHeader("ETag", etag).setHeader("Content-Type", "audio/mpeg")
            .setHeader("Content-Range", "bytes $start-$end/${bytes.size}")
            .setBody(Buffer().write(bytes, start.toInt(), (end - start + 1).toInt()))
    }
    private fun engine(metrics: AudioTransferMetrics = AudioTransferMetrics(),
        stop: () -> Boolean = { false }, checkSpace: (Long, Long) -> Unit = { _, _ -> },
        progress: (Long, Long?) -> Unit = { _, _ -> }, now: () -> Long = System::currentTimeMillis
    ) = AudioRangeTransfer(File(root, "checkpoints"), metrics,
        execute = { start, end, validator ->
            client.newCall(Request.Builder().url(server.url("/audio"))
                .header("Range", "bytes=$start-$end").header("Accept-Encoding", "identity")
                .apply { if (validator != null) header("If-Range", validator) }.build()).execute()
        }, stopped = stop, space = checkSpace, progress = progress, now = now)
    private fun failAfterSegment() {
        try {
            engine(progress = { done, _ ->
                if (done > AudioRangePolicy.CHUNK_BYTES + 4096) throw IOException("fixture connection lost")
            }).transfer(key, File(root, "unused")) { true }
            fail("Expected interruption")
        } catch (_: IOException) { }
        assertTrue(File(root, "checkpoints/$key.json").isFile)
    }

    @Test fun validRangesProduceExactlyTheOriginalBytesAndCleanCheckpoint() {
        val target = File(root, "complete")
        assertEquals("audio/mpeg", engine().transfer(key, target) { false })
        assertArrayEquals(bytes, target.readBytes())
        assertEquals(listOf(0L, 1048576L, 2097152L), starts)
        assertTrue(File(root, "checkpoints").listFiles()!!.isEmpty())
        val first = server.takeRequest()
        assertEquals("identity", first.getHeader("Accept-Encoding"))
        assertNull(first.getHeader("If-Range"))
        assertEquals(etag, server.takeRequest().getHeader("If-Range"))
    }
    @Test fun interruptedTransferResumesOnlyDurableHashedSegments() {
        failAfterSegment()
        starts.clear()
        val target = File(root, "complete")
        val metrics = AudioTransferMetrics()
        engine(metrics).transfer(key, target) { false }
        assertEquals(1048576L, starts.first())
        assertEquals(1048576L, metrics.resumedBytes)
        assertArrayEquals(bytes, target.readBytes())
    }
    @Test fun corruptRetainedByteRestartsFromZero() {
        failAfterSegment()
        RandomAccessFile(File(root, "checkpoints/$key.part"), "rw").use { it.write(7) }
        starts.clear()
        val target = File(root, "complete")
        engine().transfer(key, target) { false }
        assertEquals(0L, starts.first())
        assertArrayEquals(bytes, target.readBytes())
    }
    @Test fun malformedCheckpointIsDiscardedWithoutTouchingUnrelatedFiles() {
        failAfterSegment()
        File(root, "checkpoints/$key.json").writeText("not json")
        val unrelated = File(root, "checkpoints/owner-notes.txt").apply { writeText("keep") }
        starts.clear()
        engine().transfer(key, File(root, "complete")) { false }
        assertEquals(0L, starts.first())
        assertEquals("keep", unrelated.readText())
    }
    @Test fun changedStrongValidatorCannotAppendToOldBytes() {
        failAfterSegment(); starts.clear(); etag = "\"fixture-v2\""
        val target = File(root, "complete")
        engine().transfer(key, target) { false }
        assertEquals(listOf(1048576L, 0L, 1048576L, 2097152L), starts)
        assertArrayEquals(bytes, target.readBytes())
    }
    @Test fun ignoredRangeAfterResumeFallsBackWithoutKeepingPartial() {
        failAfterSegment()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setBody("full response")
        }
        assertNull(engine().transfer(key, File(root, "complete")) { false })
        assertFalse(File(root, "complete").exists())
        assertFalse(File(root, "checkpoints/$key.part").exists())
    }
    @Test fun weakValidatorRequiresSafeFullTransferFallback() {
        etag = "W/\"weak\""
        assertNull(engine().transfer(key, File(root, "complete")) { false })
        assertFalse(File(root, "complete").exists())
        assertEquals(1, server.requestCount)
    }
    @Test fun noValidatorRequiresSafeFullTransferFallback() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = rangeResponse(request).removeHeader("ETag")
        }
        assertNull(engine().transfer(key, File(root, "complete")) { false })
    }
    @Test fun malformedRangeIsRejectedAndNeverSaved() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = rangeResponse(request)
                .setHeader("Content-Range", "bytes 1-1048575/${bytes.size}")
        }
        expectInvalid { engine().transfer(key, File(root, "complete")) { false } }
        assertFalse(File(root, "checkpoints/$key.part").exists())
    }
    @Test fun shortChunkedResponseRetainsOnlyThePriorSegment() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val result = rangeResponse(request)
                return if (request.getHeader("Range")!!.startsWith("bytes=1048576-"))
                    result.setChunkedBody(Buffer().write(bytes, 1048576, 4096), 1024)
                else result
            }
        }
        try { engine().transfer(key, File(root, "complete")) { false }; fail() }
        catch (_: IOException) { }
        assertFalse(File(root, "complete").exists())
        starts.clear()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = rangeResponse(request)
        }
        val target = File(root, "complete")
        engine().transfer(key, target) { false }
        assertEquals(1048576L, starts.first())
        assertArrayEquals(bytes, target.readBytes())
    }
    @Test fun representationCannotChangeOnEveryRequestForever() {
        var responseCount = 0
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                etag = "\"v-${responseCount++}\""
                return rangeResponse(request)
            }
        }
        expectInvalid { engine().transfer(key, File(root, "complete")) { false } }
        assertEquals(4, server.requestCount)
        assertFalse(File(root, "complete").exists())
    }
    @Test fun checkpointDirectoryMasqueradingAsMetadataIsRejected() {
        val home = File(root, "checkpoints").apply { mkdirs() }
        File(home, "$key.json").mkdir()
        expectInvalid { engine().transfer(key, File(root, "complete")) { false } }
        assertEquals(0, server.requestCount)
    }
    @Test fun contradictoryBodyLengthIsRejected() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = rangeResponse(request)
                .setHeader("Content-Length", "19")
        }
        expectInvalid { engine().transfer(key, File(root, "complete")) { false } }
    }
    @Test fun compressedOffsetsAreRejected() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = rangeResponse(request)
                .setHeader("Content-Encoding", "gzip")
        }
        expectInvalid { engine().transfer(key, File(root, "complete")) { false } }
    }
    @Test fun transientServerFailureUsesOuterBackoffAndRetainsGoodCheckpoint() {
        failAfterSegment()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(429)
        }
        val priorRequests = server.requestCount
        try { engine().transfer(key, File(root, "complete")) { false }; fail() }
        catch (e: RetryableAudioSourceResponse) { assertEquals(429, e.statusCode) }
        assertEquals(priorRequests + 1, server.requestCount)
        assertTrue(File(root, "checkpoints/$key.json").isFile)
    }
    @Test fun unsatisfiableRangeIsNotMistakenForCompletion() {
        failAfterSegment()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(416)
        }
        expectInvalid { engine().transfer(key, File(root, "complete")) { false } }
        assertFalse(File(root, "complete").exists())
    }
    @Test fun deliberateCancelRemovesRetainedBytes() {
        failAfterSegment()
        try { engine(stop = { true }).transfer(key, File(root, "complete")) { false }; fail() }
        catch (_: IOException) { }
        assertFalse(File(root, "checkpoints/$key.part").exists())
        assertFalse(File(root, "checkpoints/$key.json").exists())
    }
    @Test fun pauseRetainsTheCheckedPrefix() {
        failAfterSegment()
        try { engine(stop = { true }).transfer(key, File(root, "complete")) { true }; fail() }
        catch (_: IOException) { }
        assertTrue(File(root, "checkpoints/$key.json").exists())
        assertTrue(File(root, "checkpoints/$key.part").length() >= AudioRangePolicy.CHUNK_BYTES)
        starts.clear()
        val target = File(root, "resumed-after-pause")
        engine().transfer(key, target) { false }
        assertEquals(1048576L, starts.first())
        assertArrayEquals(bytes, target.readBytes())
    }
    @Test fun expiredCheckpointRestartsRatherThanClaimingResume() {
        failAfterSegment(); starts.clear()
        engine(now = { System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2) })
            .transfer(key, File(root, "complete")) { false }
        assertEquals(0L, starts.first())
    }
    @Test fun unsafeKeyCannotEscapeCheckpointFolder() {
        expectInvalid { engine().transfer("../outside", File(root, "complete")) { false } }
        assertEquals(0, server.requestCount)
    }
    @Test fun storageFailureRemovesCheckpointAndDoesNotCommit() {
        failAfterSegment()
        expectInvalid { engine(checkSpace = { _, _ -> require(false) { "Not enough storage" } })
            .transfer(key, File(root, "complete")) { false } }
        assertFalse(File(root, "checkpoints/$key.part").exists())
        assertFalse(File(root, "complete").exists())
    }
    @Test fun checkpointCapacityFallsBackWithoutDeletingExistingFiles() {
        val home = File(root, "checkpoints").apply { mkdirs() }
        repeat(2) { File(home, AudioRangePolicy.key("other-$it") + ".part").writeBytes(byteArrayOf(1)) }
        assertNull(engine().transfer(key, File(root, "complete")) { false })
        assertEquals(2, home.listFiles()!!.size)
        assertEquals(0, server.requestCount)
    }
    @Test fun staleOwnedCheckpointCleanupPreservesUnrelatedData() {
        val home = File(root, "checkpoints").apply { mkdirs() }
        val stale = File(home, AudioRangePolicy.key("old") + ".part").apply {
            writeBytes(byteArrayOf(1)); setLastModified(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2))
        }
        val unrelated = File(home, "family.mp3").apply { writeText("keep") }
        engine().transfer(key, File(root, "complete")) { false }
        assertFalse(stale.exists())
        assertEquals("keep", unrelated.readText())
    }
    @Test fun hostThroughputReceiptMeasuresCompletedBytesWithoutProviderClaim() {
        val at = System.nanoTime()
        val target = File(root, "benchmark")
        engine().transfer(key, target) { false }
        val elapsed = (System.nanoTime() - at).coerceAtLeast(1)
        assertArrayEquals(bytes, target.readBytes())
        // Recorded as evidence, never a flaky host-speed pass threshold.
        println("NW136_HOST_RECEIPT bytes=${target.length()} nanos=$elapsed " +
            "mbps=${target.length().toDouble() * 8_000 / elapsed} scope=loopback-not-phone")
    }
    private fun expectInvalid(block: () -> Unit) {
        try { block(); fail("Expected invalid response rejection") }
        catch (_: IllegalArgumentException) { }
    }
}
