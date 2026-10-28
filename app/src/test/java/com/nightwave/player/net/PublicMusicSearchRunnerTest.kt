package com.nightwave.player.net

import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class PublicMusicSearchRunnerTest {
    private val json = """[{"type":"video","title":"Play Date","videoId":"dQw4w9WgXcQ","author":"Fixture uploader","lengthSeconds":215}]"""
    private fun search(vararg servers: MockWebServer) = CommunityVideoSearch(instances = servers.map { it.url("/").toString().trimEnd('/') })
    private fun result(videos: List<VideoPreview>) = VideoSearchResults(videos, true)

    @Test fun cancelClosesActiveSocketAndNeverStartsFallbackEndpoint() = runBlocking {
        val slow = MockWebServer(); val fallback = MockWebServer()
        slow.start(); fallback.start()
        try {
            slow.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            fallback.enqueue(MockResponse().setBody(json))
            val engine = search(slow, fallback)
            var published = false
            val task = launch { PublicMusicSearchRunner.run { result(engine.search("play date").videos) }; published = true }
            withContext(Dispatchers.IO) { assertNotNull(slow.takeRequest(4, TimeUnit.SECONDS)) }
            withTimeout(4_000) { task.cancelAndJoin() }
            assertFalse(published); assertEquals(0, fallback.requestCount)
        } finally { slow.shutdown(); fallback.shutdown() }
    }

    @Test fun totalTimeoutStopsReadingBodyAndCannotFallThroughToAnotherProvider() = runBlocking {
        val slow = MockWebServer(); val fallback = MockWebServer()
        slow.start(); fallback.start()
        try {
            slow.enqueue(MockResponse().setBody(json).throttleBody(1, 1, TimeUnit.SECONDS))
            fallback.enqueue(MockResponse().setBody(json))
            var timedOut = false
            val engine = search(slow, fallback)
            try { PublicMusicSearchRunner.run(750) { result(engine.search("play date").videos) } }
            catch (_: TimeoutCancellationException) { timedOut = true }
            catch (_: PublicSearchDeadlineException) { timedOut = true }
            assertTrue(timedOut); assertEquals(1, slow.requestCount); assertEquals(0, fallback.requestCount)
        } finally { slow.shutdown(); fallback.shutdown() }
    }

    @Test fun cancelledSearchCannotCancelADifferentSearchOrUnscopedTransport() = runBlocking {
        val slow = MockWebServer(); val healthy = MockWebServer()
        slow.start(); healthy.start()
        try {
            slow.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            healthy.enqueue(MockResponse().setBody(json))
            val old = launch { PublicMusicSearchRunner.run { result(search(slow).search("old").videos) } }
            withContext(Dispatchers.IO) { assertNotNull(slow.takeRequest(4, TimeUnit.SECONDS)) }
            old.cancel()
            val fresh = PublicMusicSearchRunner.run { result(search(healthy).search("new").videos) }
            withTimeout(4_000) { old.join() }
            assertEquals("Play Date", fresh.videos.single().title)
            assertNull(PublicSearchRequest.current.get())
            healthy.enqueue(MockResponse().setBody(json))
            // A normal unscoped transport retains its original lifetime and works after cancellation.
            assertEquals(1, withContext(Dispatchers.IO) { search(healthy).search("unscoped").videos.size })
        } finally { slow.shutdown(); healthy.shutdown() }
    }

    @Test fun successAndFailureAlwaysClearThreadScopeAndOnlyPublicQueryIsSent() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody(json))
            val answer = PublicMusicSearchRunner.run { result(search(server).search("ঝুম cover").videos) }
            assertEquals(215, answer.videos.single().durationSeconds.toInt())
            val sent = server.takeRequest()
            assertEquals("ঝুম cover", sent.requestUrl!!.queryParameter("q"))
            assertNull(sent.getHeader("Authorization")); assertNull(sent.getHeader("Cookie"))
            withContext(Dispatchers.IO) {
                try { PublicSearchRequest().scoped { error("fixture") } } catch (_: IllegalStateException) { }
                assertNull(PublicSearchRequest.current.get())
            }
        } finally { server.shutdown() }
    }

    @Test fun cancelledScopeRejectsFutureCallsBeforeAnyNetworkUse() {
        val request = PublicSearchRequest(); request.cancel()
        try { request.scoped { fail("Cancelled scope ran") }; fail("Accepted cancelled scope") }
        catch (_: CancellationException) { }
        assertNull(PublicSearchRequest.current.get())
    }
}
