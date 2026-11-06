package com.nightwave.player.net

import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverySearchTest {
    private val uuid = "a9a6a490-e496-4c02-9a8f-e3c5771c2120"
    private val audius = """{"data":[{"id":"Track9","title":"Piano","user":{"name":"Creator"},
        "is_unlisted":false,"permalink":"/artist/piano","duration":70,
        "is_streamable":true,"is_downloadable":true}]}"""

    @Test fun discoveryDoesNotInferPlaybackOrOfflineRightsFromProviderFlags() {
        val track = DiscoverySources.decode(DiscoverySource.AUDIUS, audius).single()
        assertEquals("https://audius.co/artist/piano", track.canonicalUrl.toString())
        assertEquals("Creator", track.creator)
        assertEquals(70, track.durationSeconds)
        assertEquals(DiscoveryPermission.AVAILABLE, track.capabilities.openExternally)
        assertEquals(DiscoveryPermission.UNKNOWN, track.capabilities.playInApp)
        assertEquals(DiscoveryPermission.UNAVAILABLE, track.capabilities.offline)
        assertNotEquals(track.id, DiscoveryItemId(DiscoverySource.PEERTUBE, track.id.remoteId))
    }

    @Test fun peerTubeAcceptsCanonicalWatchLinksOnDistinctInstancesAndRejectsUnsafeDestinations() {
        fun row(id: String, url: String, nsfw: Boolean = false) = """{"uuid":"$id","name":"Same title",
            "privacy":{"id":1},"nsfw":$nsfw,"url":"$url","channel":{"displayName":"Channel"}}"""
        val second = uuid.replaceFirst("a", "b")
        val rows = listOf(
            row(uuid, "https://exode.me/videos/watch/$uuid"),
            row(second, "https://play.mittdata.se/videos/watch/$second"),
            row(second, "http://play.mittdata.se/videos/watch/$second"),
            row(second, "https://127.0.0.1/videos/watch/$second"),
            row(second, "https://evil.local/videos/watch/$second"),
            row(second, "https://u:p@exode.me/videos/watch/$second"),
            row(second, "https://exode.me/videos/watch/$uuid"),
            row(second, "https://exode.me/videos/watch/$second", nsfw = true)
        )
        val results = rows.flatMap { row ->
            DiscoverySources.decode(DiscoverySource.PEERTUBE, """{"data":[$row]}""")
        }
        assertEquals(listOf(uuid, second), results.map { it.id.remoteId })
        assertEquals(listOf("exode.me", "play.mittdata.se"), results.map { it.canonicalUrl.host })
    }

    @Test fun radioRemainsLiveAndUsesStationPageInsteadOfStream() {
        val payload = """[{"stationuuid":"$uuid","name":"Community radio","countrycode":"BD",
            "lastcheckok":1,"url":"https://stream.example.org/live","homepage":"https://radio.example.org/"}]"""
        val row = DiscoverySources.decode(DiscoverySource.RADIO, payload).single()
        assertEquals(DiscoveryKind.LIVE_RADIO, row.id.source.kind)
        assertEquals("https://radio.example.org/", row.canonicalUrl.toString())
        assertNull(row.durationSeconds)
        assertEquals(DiscoveryPermission.UNAVAILABLE, row.capabilities.offline)
    }

    @Test fun unicodeQueryIsEncodedAsDataAndUnavailableSourceHasNoEndpoint() {
        val query = "বাংলা & count=200"
        val url = DiscoverySources.endpoint(DiscoverySource.PEERTUBE, query)!!
        assertEquals(query, url.queryParameter("search"))
        assertEquals("20", url.queryParameter("count"))
        assertNull(DiscoverySources.endpoint(DiscoverySource.JAMENDO, query))
        assertNull(DiscoverySources.query("a\nb"))
        assertNull(DiscoverySources.query("x".repeat(121)))
    }

    @Test(timeout = 4000) fun partialResultsArriveBeforeStalledSourceAndTimeoutStaysScoped() = runBlocking {
        val search = DiscoverySearch(fetch = { url ->
            if (url.host == "sepiasearch.org") awaitCancellation()
            else DiscoveryResponse(200, audius)
        }, timeoutMs = 250)
        val updates = search.search("piano", setOf(DiscoverySource.PEERTUBE, DiscoverySource.AUDIUS)).toList()
        assertEquals(DiscoverySource.AUDIUS, updates.first().source)
        assertTrue(updates.first().outcome is DiscoveryOutcome.Results)
        assertEquals(DiscoveryUpdate(DiscoverySource.PEERTUBE, DiscoveryOutcome.Problem.TIMEOUT), updates.last())
    }

    @Test(timeout = 4000) fun cancelledRequestsCannotEmitAfterReplacementAndOnlySelectedSourceIsCalled() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val stopped = CompletableDeferred<Unit>()
        val received = mutableListOf<HttpUrl>()
        val search = DiscoverySearch(fetch = { url ->
            received += url
            if (url.queryParameter("query") == "first") {
                try { started.complete(Unit); awaitCancellation() }
                finally { stopped.complete(Unit) }
            } else DiscoveryResponse(200, audius)
        })
        val stale = mutableListOf<DiscoveryUpdate>()
        val selected = setOf(DiscoverySource.AUDIUS)
        val job = launch { search.search("first", selected).toList(stale) }
        started.await()
        job.cancelAndJoin()
        stopped.await()
        assertEquals(1, search.search("second", selected).toList().size)
        assertTrue(stale.isEmpty())
        assertEquals(listOf("first", "second"), received.map { it.queryParameter("query") })
        assertTrue(received.all { it.host == "api.audius.co" })
    }

    @Test fun responseProblemsAreDistinctFromEmptyResults() = runBlocking {
        val selected = setOf(DiscoverySource.AUDIUS)
        suspend fun outcome(fetch: suspend (HttpUrl) -> DiscoveryResponse) =
            DiscoverySearch(fetch).search("piano", selected).toList().single().outcome
        assertEquals(DiscoveryOutcome.Empty, outcome { DiscoveryResponse(200, """{"data":[]}""") })
        assertEquals(DiscoveryOutcome.Problem.MALFORMED, outcome { DiscoveryResponse(200, "<html>") })
        assertEquals(DiscoveryOutcome.Problem.MALFORMED, outcome { DiscoveryResponse(200, null) })
        assertEquals(DiscoveryOutcome.Problem.UNAVAILABLE, outcome { DiscoveryResponse(503, null) })
        assertEquals(DiscoveryOutcome.Problem.UNAVAILABLE, outcome { DiscoveryResponse(302, null) })
        assertEquals(DiscoveryOutcome.Problem.UNAVAILABLE, outcome { throw SSLException("fixture") })
        assertEquals(DiscoveryOutcome.Problem.AUTH, outcome { DiscoveryResponse(401, null) })
        assertEquals(DiscoveryOutcome.Problem.PERMISSION, outcome { DiscoveryResponse(403, null) })
        assertEquals(DiscoveryOutcome.Problem.NETWORK, outcome { throw UnknownHostException("fixture") })
    }

    @Test fun quotaCooldownIsSourceScopedAndDoesNotRetryEarly() = runBlocking {
        var nanos = 0L
        var audiusCalls = 0
        var peerCalls = 0
        val engine = DiscoverySearch(fetch = { url ->
            if (url.host == "api.audius.co") {
                audiusCalls++
                DiscoveryResponse(429, null, 10L)
            } else {
                peerCalls++
                DiscoveryResponse(200, """{"data":[]}""")
            }
        }, nowNanos = { nanos })
        val sources = setOf(DiscoverySource.AUDIUS, DiscoverySource.PEERTUBE)
        repeat(2) {
            val outcomes = engine.search("piano", sources).toList().associate { it.source to it.outcome }
            assertEquals(DiscoveryOutcome.Problem.QUOTA, outcomes[DiscoverySource.AUDIUS])
            assertEquals(DiscoveryOutcome.Empty, outcomes[DiscoverySource.PEERTUBE])
        }
        assertEquals(1, audiusCalls)
        assertEquals(2, peerCalls)
        nanos = TimeUnit.SECONDS.toNanos(10)
        engine.search("piano", setOf(DiscoverySource.AUDIUS)).toList()
        assertEquals(2, audiusCalls)
    }
}
