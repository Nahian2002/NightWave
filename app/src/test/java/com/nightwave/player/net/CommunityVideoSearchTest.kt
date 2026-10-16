package com.nightwave.player.net

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class CommunityVideoSearchTest {
    private val json = """[{"type":"video","title":"Baby Shark","videoId":"dQw4w9WgXcQ","author":"Music Creator","lengthSeconds":97}]"""

    @Test fun usesEncodedKeywordSearchAndParsesVideo() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(200).setBody(json))
            val engine = CommunityVideoSearch(instances = listOf(server.url("/").toString().trimEnd('/')))
            val items = engine.search("baby shark")
            assertEquals(1, items.videos.size)
            assertEquals("Baby Shark", items.videos.single().title)
            assertEquals("baby shark", server.takeRequest().requestUrl?.queryParameter("q"))
        } finally { server.shutdown() }
    }

    @Test fun failsOverOnServerErrorInsteadOfGivingUp() {
        val failed = MockWebServer()
        val healthy = MockWebServer()
        failed.start()
        healthy.start()
        try {
            failed.enqueue(MockResponse().setResponseCode(503).setBody("unavailable"))
            healthy.enqueue(MockResponse().setResponseCode(200).setBody(json))
            val engine = CommunityVideoSearch(instances = listOf(
                failed.url("/").toString().trimEnd('/'),
                healthy.url("/").toString().trimEnd('/')
            ))
            val result = engine.search("Bangla songs")
            assertEquals(1, result.videos.size)
            assertEquals(healthy.hostName, result.server.substringBefore(':'))
        } finally {
            failed.shutdown()
            healthy.shutdown()
        }
    }
}
