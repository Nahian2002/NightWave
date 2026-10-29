package com.nightwave.player.playback

import okhttp3.CookieJar
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import androidx.media3.exoplayer.DefaultLoadControl

class PlaybackMediaNetworkTest {
    @Test fun mediaUsesReusableHttp2CapableTransportWithoutCookiesCacheOrWholeStreamDeadline() {
        val client=PlaybackMediaNetwork.client
        assertSame(client,PlaybackMediaNetwork.client)
        assertTrue(Protocol.HTTP_2 in client.protocols); assertTrue(Protocol.HTTP_1_1 in client.protocols)
        assertSame(CookieJar.NO_COOKIES,client.cookieJar); assertNull(client.cache)
        assertEquals(0,client.callTimeoutMillis); assertEquals(20_000,client.readTimeoutMillis)
    }
    @Test fun successiveRequestsActuallyReuseTheConnectionAndPreserveFullBodies() {
        val server=MockWebServer(); server.start()
        try {
            repeat(2) { n ->
                val body="original audio bytes $n"; server.enqueue(MockResponse().setBody(body))
                PlaybackMediaNetwork.client.newCall(Request.Builder().url(server.url("/fixture")).build())
                    .execute().use { assertEquals(body,it.body!!.string()) }
                assertEquals(n,server.takeRequest(5,TimeUnit.SECONDS)!!.sequenceNumber)
            }
        } finally { server.shutdown() }
    }
    @Test fun cancellingOneStalledRequestDoesNotCancelAnotherSongRequest() {
        val server=MockWebServer(); server.start(); val worker=Executors.newSingleThreadExecutor()
        val call=PlaybackMediaNetwork.client.newCall(Request.Builder().url(server.url("/slow")).build())
        try {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val stopped=worker.submit<Boolean> { try { call.execute().close(); false } catch (_:IOException) { true } }
            assertNotNull(server.takeRequest(5,TimeUnit.SECONDS)); call.cancel()
            assertTrue(stopped.get(5,TimeUnit.SECONDS))
            server.enqueue(MockResponse().setBody("other song"))
            PlaybackMediaNetwork.client.newCall(Request.Builder().url(server.url("/other")).build())
                .execute().use { assertEquals("other song",it.body!!.string()) }
        } finally { call.cancel(); worker.shutdownNow(); server.shutdown() }
    }
    @Test fun readAheadRemainsTimeAndMemoryBoundedWithUnchangedStartupThresholds() {
        assertTrue(PlaybackMediaNetwork.MAX_BUFFER_MS > PlaybackMediaNetwork.MIN_BUFFER_MS)
        assertTrue(PlaybackMediaNetwork.MIN_BUFFER_MS >= PlaybackMediaNetwork.REBUFFER_MS)
        assertEquals(DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,PlaybackMediaNetwork.START_BUFFER_MS)
        assertEquals(DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,PlaybackMediaNetwork.REBUFFER_MS)
        assertTrue(PlaybackMediaNetwork.TARGET_BYTES <= 16*1024*1024)
    }
}
