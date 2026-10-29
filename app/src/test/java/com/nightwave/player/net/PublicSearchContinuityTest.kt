package com.nightwave.player.net

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class PublicSearchContinuityTest {
    private fun result(title: String = "Original (Live)", fallback: Boolean = true) = VideoSearchResults(
        listOf(VideoPreview(title, "Known uploader", "https://www.youtube.com/watch?v=abcdefghijk",
            "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg", 215)), fallback, "Independent provider")

    @Test fun freshStaleAndExpiredWindowsAreExplicitAndClockSafe() {
        var now = 0L
        val cache = PublicSearchResultCache { now }
        assertTrue(cache.put("Blue", SearchMode.SONGS, result()))
        assertTrue(cache.get(" blue ", SearchMode.SONGS)!!.fresh)
        now = PublicSearchResultCache.FRESH_MS
        assertFalse(cache.get("Blue", SearchMode.SONGS)!!.fresh)
        now = PublicSearchResultCache.STALE_MS
        assertNull(cache.get("Blue", SearchMode.SONGS))
        now = 1000; cache.put("Blue", SearchMode.SONGS, result()); now = 999
        assertNull(cache.get("Blue", SearchMode.SONGS))
    }
    @Test fun modeQueryAndRepeatedWordsNeverShareDifferentResults() {
        val cache = PublicSearchResultCache { 0 }
        cache.put("alo alo", SearchMode.SONGS, result())
        assertNull(cache.get("alo", SearchMode.SONGS))
        assertNull(cache.get("alo alo", SearchMode.VIDEOS))
        assertNull(cache.get("alone", SearchMode.SONGS))
        cache.put("বাংলা গান", SearchMode.SONGS, result("বাংলা গান"))
        assertEquals("বাংলা গান", cache.get("বাংলা গান", SearchMode.SONGS)!!.result.videos.single().title)
    }
    @Test fun lruEvictsTheLeastRecentlyUsedEntryRatherThanTheMostRecentlyViewed() {
        val cache = PublicSearchResultCache { 0 }
        for (i in 0..11) cache.put("query$i", SearchMode.SONGS, result())
        cache.get("query0", SearchMode.SONGS)
        cache.put("new", SearchMode.SONGS, result())
        assertEquals(12, cache.entryCount)
        assertNotNull(cache.get("query0", SearchMode.SONGS))
        assertNull(cache.get("query1", SearchMode.SONGS))
    }
    @Test fun textBudgetBoundsMemoryWithoutTruncatingTheLiveResult() {
        val cache = PublicSearchResultCache { 0 }
        val large = result("x".repeat(40_000))
        for (i in 0..11) assertTrue(cache.put("$i", SearchMode.SONGS, large))
        assertTrue(cache.retainedTextUnits <= PublicSearchResultCache.MAX_TEXT_UNITS)
        assertTrue(cache.entryCount < 12)
        val oversized = result("y".repeat(60_000))
        assertFalse(cache.put("large", SearchMode.SONGS, oversized))
        assertEquals(60_000, oversized.videos.single().title.length)
    }
    @Test fun emptyFailuresAndSignedPlaybackAddressesAreNeverAdmitted() {
        val cache = PublicSearchResultCache { 0 }
        assertFalse(cache.put("", SearchMode.SONGS, result()))
        assertFalse(cache.put("empty", SearchMode.SONGS, VideoSearchResults(emptyList(), false)))
        assertFalse(cache.put("signed", SearchMode.SONGS, result().copy(videos = listOf(
            result().videos.single().copy(url = "https://media.example/audio?token=fixture")))))
        assertEquals(0, cache.entryCount)
    }
    @Test fun sourceVersionsAndOriginalUrlsSurviveDefensiveSnapshots() {
        val original = result()
        val videos = original.videos.toMutableList()
        val cache = PublicSearchResultCache { 0 }
        cache.put("Original", SearchMode.SONGS, original.copy(videos = videos))
        videos.clear()
        val hit = cache.get("Original", SearchMode.SONGS)!!.result
        assertEquals(original.videos, hit.videos)
        assertEquals(original.usedVideoFallback, hit.usedVideoFallback)
        assertEquals(original.note, hit.note)
        assertTrue(hit.fromCache)
    }
    @Test fun cacheInstancesNeverShareRecentQueriesAcrossOwnerScopes() {
        val first = PublicSearchResultCache { 0 }
        val second = PublicSearchResultCache { 0 }
        first.put("private session query", SearchMode.SONGS, result())
        assertNull(second.get("private session query", SearchMode.SONGS))
        assertEquals(0, second.entryCount)
    }
    @Test fun diagnosticsKeepPhaseAndHttpWorkSeparateAndOmitPrivateValues() {
        var now = 0L
        val metrics = PublicSearchDiagnostics { now }
        metrics.begin(PublicSearchPhase.DIRECT)
        metrics.httpCompleted(50_000_000)
        now = 120_000_000; metrics.begin(PublicSearchPhase.COMMUNITY)
        metrics.httpCompleted(10_000_000)
        now = 150_000_000
        val first = metrics.snapshot()
        assertEquals(first, metrics.snapshot())
        assertTrue(first.contains("Elapsed 150 ms"))
        assertTrue(first.contains("Direct 120 ms · Community 30 ms"))
        assertTrue(first.contains("HTTP calls 2 · Cumulative HTTP work 60 ms"))
        metrics.cacheHit(); assertTrue(metrics.snapshot().contains("Recent results"))
        assertFalse(first.contains("https://"))
    }
    @Test fun exhaustedDirectSocketLeavesTimeForHealthyCommunityResponse() = runBlocking {
        val direct = MockWebServer(); val fallback = MockWebServer()
        direct.start(); fallback.start()
        try {
            direct.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            fallback.enqueue(MockResponse().setBody("healthy"))
            val client = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build()
            val response = PublicMusicSearchRunner.run(5000) { request ->
                request.beginPhase(PublicSearchPhase.DIRECT, 250)
                try {
                    request.execute(client.newCall(Request.Builder().url(direct.url("/direct")).build())) { it.body!!.string() }
                    fail("Slow direct socket escaped its phase budget")
                } catch (_: IOException) { request.checkActive() }
                request.beginPhase(PublicSearchPhase.COMMUNITY)
                val body = request.execute(client.newCall(Request.Builder().url(fallback.url("/fallback")).build())) { it.body!!.string() }
                assertEquals("healthy", body)
                assertTrue(request.details().contains("HTTP calls 2"))
                result()
            }
            assertEquals(1, direct.requestCount); assertEquals(1, fallback.requestCount)
            assertEquals("Original (Live)", response.videos.single().title)
        } finally { direct.shutdown(); fallback.shutdown() }
    }
    @Test fun cancelledRequestCannotCommitCachedResultsOrTouchAnotherRequest() {
        val cache = PublicSearchResultCache { 0 }
        val cancelled = PublicSearchRequest(); cancelled.cancel()
        try { cancelled.commitIfActive { cache.put("cancelled", SearchMode.SONGS, result()) }; fail("Late cache commit") }
        catch (_: CancellationException) { }
        assertNull(cache.get("cancelled", SearchMode.SONGS))
        val fresh = PublicSearchRequest()
        assertTrue(fresh.commitIfActive { cache.put("healthy", SearchMode.SONGS, result()) })
        assertNotNull(cache.get("healthy", SearchMode.SONGS))
    }
}
