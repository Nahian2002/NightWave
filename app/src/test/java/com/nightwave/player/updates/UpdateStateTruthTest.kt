package com.nightwave.player.updates

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection

class UpdateStateTruthTest {
    @Test fun authenticatedReleaseDecisionPreservesVersionAndOsBoundaries() {
        assertEquals(UpdateCheckStatus.DISABLED, UpdateCheckPolicy.authenticated(false, 41, 42, 23, 23))
        assertEquals(UpdateCheckStatus.NO_COMPATIBLE, UpdateCheckPolicy.authenticated(true, 41, 41, 23, 23))
        assertEquals(UpdateCheckStatus.NO_COMPATIBLE, UpdateCheckPolicy.authenticated(true, 41, 42, 23, 24))
        assertEquals(UpdateCheckStatus.AVAILABLE, UpdateCheckPolicy.authenticated(true, 41, 42, 24, 24))
    }
    @Test fun unsignedHttp404WithDisabledLookingBodyNeverBecomesDisabled() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(404).setBody("{\"enabled\":false}"))
            try {
                WeeklyFeedTransport.read(server.url("/feed").toUrl().openConnection() as HttpURLConnection)
                fail("Unsigned HTTP failure must not be accepted")
            } catch (e: UpdateCheckException) { assertEquals(UpdateCheckStatus.UNAVAILABLE, e.status) }
        } finally { server.shutdown() }
    }
    @Test fun authenticatedPrivateFeedDistinguishesRevokedAccessFromBadMetadata() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(404).setBody("{\"enabled\":false}"))
            try {
                WeeklyFeedTransport.read(server.url("/feed").toUrl().openConnection() as HttpURLConnection,
                    authenticatedGithub = true)
                fail("Private access denial must not become a release status")
            } catch (e: UpdateCheckException) { assertEquals(UpdateCheckStatus.PRIVATE_ACCESS, e.status) }
        }
    }

    @Test fun oversized200ResponseIsInvalidRatherThanAnAvailabilityDecision() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody("x".repeat(24 * 1024 + 1)))
            try {
                WeeklyFeedTransport.read(server.url("/feed").toUrl().openConnection() as HttpURLConnection)
                fail("Unbounded metadata accepted")
            } catch (e: UpdateCheckException) { assertEquals(UpdateCheckStatus.INVALID_METADATA, e.status) }
        } finally { server.shutdown() }
    }
}
