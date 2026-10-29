package com.nightwave.player.offline

import okhttp3.Call
import okhttp3.EventListener
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
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

class AudioParallelTransferTest {
    private lateinit var root: File
    private lateinit var server: MockWebServer
    private val data = ByteArray(2 * 1024 * 1024 + 19) { (it * 17).toByte() }
    private val key = AudioRangePolicy.key("parallel-source")
    private val active = CopyOnWriteArraySet<Call>()
    private val starts = java.util.Collections.synchronizedList(mutableListOf<Long>())
    private val client = OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS)
        .eventListener(object : EventListener() {
            override fun callEnd(call: Call) { active.remove(call) }
            override fun callFailed(call: Call, ioe: IOException) { active.remove(call) }
        }).build()
    @Before fun setup() {
        root = Files.createTempDirectory("nw-parallel").toFile()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = range(request)
        }
        server.start()
    }
    @After fun cleanup() { active.forEach { it.cancel() }; server.shutdown(); root.deleteRecursively() }
    private fun range(request: RecordedRequest): MockResponse {
        val pieces = request.getHeader("Range")!!.removePrefix("bytes=").split('-')
        val start = pieces[0].toLong();val end = minOf(pieces[1].toLong(), data.lastIndex.toLong())
        starts.add(start)
        return MockResponse().setResponseCode(206).setHeader("ETag", "\"v1\"")
            .setHeader("Content-Type", "audio/mpeg").setHeader("Content-Range", "bytes $start-$end/${data.size}")
            .setBody(Buffer().write(data, start.toInt(), (end-start+1).toInt()))
    }
    private fun engine(threads: Int=4, stop: () -> Boolean={false},
        progress: (Long,Long?) -> Unit={_,_->}, metrics: AudioTransferMetrics=AudioTransferMetrics()
    )=AudioRangeTransfer(File(root,"checkpoints"), metrics,
        execute={start,end,validator->
            val call=client.newCall(Request.Builder().url(server.url("/audio"))
                .header("Range","bytes=$start-$end").header("Accept-Encoding","identity")
                .apply { if(validator!=null) header("If-Range",validator) }.build())
            active.add(call)
            if(Thread.currentThread().isInterrupted) call.cancel()
            call.execute()
        }, stopped=stop,space={_,_->},progress=progress,parallelism=threads,
        abortRequests={active.forEach { it.cancel() }})
    @Test fun fourBoundedWorkersCommitExactBytesInOrder() {
        val target=File(root,"complete")
        assertEquals("audio/mpeg",engine().transfer(key,target){false})
        assertArrayEquals(data,target.readBytes())
        assertEquals(0L,starts.first())
        assertTrue(starts.contains(65536L))
        assertTrue(starts.size>=5)
        assertTrue(active.isEmpty())
    }
    @Test fun outOfOrderNetworkCompletionCannotMixSegments() {
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse {
            val result=range(request)
            return if(request.getHeader("Range")!!.startsWith("bytes=65536-"))
                result.setBodyDelay(150,TimeUnit.MILLISECONDS) else result
        }}
        val target=File(root,"complete")
        engine().transfer(key,target){false}
        assertArrayEquals(data,target.readBytes())
    }
    @Test fun changedParallelValidatorRequiresCleanFullFallback() {
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse {
            val result=range(request)
            return if(request.getHeader("If-Range")!=null) result.setHeader("ETag","\"changed\"") else result
        }}
        assertNull(engine().transfer(key,File(root,"complete")){false})
        assertFalse(File(root,"complete").exists())
        assertFalse(File(root,"checkpoints/$key.part").exists())
    }
    @Test fun ignoredParallelRangeRequiresCleanFullFallback() {
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse =
            if(request.getHeader("If-Range")!=null) MockResponse().setBody("full response") else range(request)}
        assertNull(engine().transfer(key,File(root,"complete")){false})
        assertTrue(active.isEmpty())
    }
    @Test fun invalidParallelOffsetsAreNeverCommitted() {
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse {
            val result=range(request)
            return if(request.getHeader("If-Range")!=null) result.setHeader("Content-Range","bytes 0-9/${data.size}") else result
        }}
        try{engine().transfer(key,File(root,"complete")){false};fail()}catch(_:IllegalArgumentException){}
        assertFalse(File(root,"complete").exists())
        assertFalse(File(root,"checkpoints/$key.json").exists())
    }
    @Test fun pauseRetainsOnlyDurableProbeAndResumesCheckedOffset() {
        var stopped=false
        try{engine(stop={stopped},progress={done,_->if(done>65536L)stopped=true})
            .transfer(key,File(root,"complete")){true};fail()}catch(_:IOException){}
        val metrics=AudioTransferMetrics();starts.clear()
        val target=File(root,"resumed")
        engine(metrics=metrics).transfer(key,target){false}
        assertEquals(65536L,metrics.resumedBytes)
        assertArrayEquals(data,target.readBytes())
    }
    @Test fun changedParallelTotalRequiresCleanFallback() {
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse {
            val result=range(request)
            if(request.getHeader("If-Range")==null)return result
            val range=result.headers["Content-Range"]!!.substringBefore('/')
            return result.setHeader("Content-Range",range+"/"+(data.size+1))
        }}
        assertNull(engine().transfer(key,File(root,"complete")){false})
        assertFalse(File(root,"complete").exists())
    }
    @Test fun parallelRateLimitAbortsBatchWithoutImmediateRetry() {
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse {
            if(request.getHeader("Range")!!.startsWith("bytes=65536-"))
                return MockResponse().setResponseCode(429)
            val result=range(request)
            return if(request.getHeader("If-Range")!=null)result.setBodyDelay(2,TimeUnit.SECONDS)else result
        }}
        try{engine().transfer(key,File(root,"complete")){false};fail()}
        catch(e:RetryableAudioSourceResponse){assertEquals(429,e.statusCode)}
        assertTrue(server.requestCount<=5)
        assertTrue(File(root,"checkpoints/$key.json").isFile)
        assertFalse(File(root,"complete").exists())
    }
    @Test fun cancellationDoesNotCancelUnrelatedCallOnSharedClient() {
        val other=client.newCall(Request.Builder().url(server.url("/not-started"))
            .header("Range","bytes=0-9").build())
        var stopped=false
        try{engine(stop={stopped},progress={done,_->if(done>65536L)stopped=true})
            .transfer(key,File(root,"complete")){false};fail()}catch(_:IOException){}
        assertFalse(other.isCanceled())
        other.execute().use{assertEquals(206,it.code)}
        assertFalse(File(root,"checkpoints/$key.part").exists())
    }
    @Test fun controlledConnectionPacingComparisonRecordsBothCompletedTransfers() {
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest)=
            range(request).throttleBody(64*1024,250,TimeUnit.MILLISECONDS)}
        fun run(threads:Int):Long {
            val target=File(root,"benchmark-$threads");val at=System.nanoTime()
            engine(threads).transfer(key,target){false}
            val elapsed=System.nanoTime()-at
            assertArrayEquals(data,target.readBytes())
            println("OFFLINE_FINISH_CONTROLLED threads=$threads bytes=${data.size} nanos=$elapsed " +
                "mbps=${data.size.toDouble()*8000/elapsed} scope=per-connection-paced-loopback-not-phone")
            return elapsed
        }
        run(1);run(4)
        // Deliberately no flaky universal speed assertion; the server enforces
        // per-response pacing, not a real source's aggregate bandwidth policy.
    }
}
