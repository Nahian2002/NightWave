package com.nightwave.player.net

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest
import com.nightwave.player.playback.PlaybackMediaNetwork
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer

class MediaTransferIoTest {
    private class Sink : OutputStream() {
        val digest=MessageDigest.getInstance("SHA-256")
        var writes=0; var bytes=0L; var closed=false
        override fun write(value:Int) { write(byteArrayOf(value.toByte()),0,1) }
        override fun write(data:ByteArray,offset:Int,length:Int) {
            writes++; bytes+=length; digest.update(data,offset,length)
        }
        override fun close() { closed=true }
    }
    @Test fun smallSocketReadsBecomeFewerDiskWritesWithoutChangingAnyBytes() {
        val chunk=ByteArray(8192) { (it % 251).toByte() }
        val before=Sink(); repeat(2048) { before.write(chunk) }
        val after=Sink(); MediaTransferIo.bufferedWrite(after) { out -> repeat(2048) { out.write(chunk) } }
        assertEquals(16L*1024*1024,after.bytes)
        assertArrayEquals(before.digest.digest(),after.digest.digest())
        assertEquals(2048,before.writes); assertEquals(64,after.writes)
        assertFalse(after.closed) // The caller still owns fsync and stream lifetime.
    }
    @Test fun finalShortTailFlushesBeforeTheCallerCanCommit() {
        val raw=ByteArrayOutputStream(); val input=ByteArray(1001) { it.toByte() }
        val result=MediaTransferIo.bufferedWrite(raw) { out -> out.write(input); "done" }
        assertEquals("done",result); assertArrayEquals(input,raw.toByteArray())
    }
    @Test fun aRealHttpBodyCopiesCompletelyThroughTheBufferedPath() {
        val bytes=ByteArray(2*1024*1024+123) { (it%251).toByte() }
        val server=MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody(Buffer().write(bytes)))
            val sink=Sink()
            PlaybackMediaNetwork.client.newCall(Request.Builder().url(server.url("/audio-fixture")).build())
                .execute().use { response -> response.body!!.byteStream().use { input ->
                    MediaTransferIo.bufferedWrite(sink) { out -> input.copyTo(out,8192) }
                } }
            assertEquals(bytes.size.toLong(),sink.bytes)
            assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(bytes),sink.digest.digest())
            assertTrue(sink.writes < 20)
        } finally { server.shutdown() }
    }
    @Test fun aDiskFailureCannotBeReportedAsSuccessfulCopy() {
        val raw=object:OutputStream() { override fun write(value:Int) { throw IOException("fixture disk full") } }
        assertThrows(IOException::class.java) { MediaTransferIo.bufferedWrite(raw) { it.write(byteArrayOf(1,2,3)) } }
    }
    @Test fun cancellationOrValidationFailureDoesNotFlushUncommittedTail() {
        val raw=ByteArrayOutputStream()
        assertThrows(IOException::class.java) {
            MediaTransferIo.bufferedWrite(raw) { out -> out.write(byteArrayOf(1,2,3)); throw IOException("fixture cancelled") }
        }
        assertEquals(0,raw.size())
    }
}
