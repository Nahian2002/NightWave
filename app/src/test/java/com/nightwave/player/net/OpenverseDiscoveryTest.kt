package com.nightwave.player.net

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenverseDiscoveryTest {
    private val id = "5db133d0-cd7f-43ef-9d4a-c3e6134f85c4"
    private val jamendoPage = "https://www.jamendo.com/track/150798"
    private fun row() = JSONObject("""{
        "id":"$id","title":"Le piano","creator":"Torley on Piano","license":"by-sa","license_version":"3.0",
        "category":"music","mature":false,"duration":122000,"source":"jamendo","foreign_landing_url":"$jamendoPage",
        "url":"https://cdn.example.org/track.mp3"
    }""")
    private fun decode(row: JSONObject) =
        DiscoverySources.decode(DiscoverySource.OPENVERSE, """{"results":[$row]}""").singleOrNull()

    @Test fun sendsOneBoundedMusicQueryToOpenverse() {
        val query = "বাংলা & page_size=200"
        val url = DiscoverySources.endpoint(DiscoverySource.OPENVERSE, query)!!
        assertEquals("api.openverse.org", url.host)
        assertEquals("/v1/audio/", url.encodedPath)
        assertEquals(query, url.queryParameter("q"))
        assertEquals("20", url.queryParameter("page_size"))
        assertEquals("music", url.queryParameter("category"))
        assertEquals("false", url.queryParameter("mature"))
        assertEquals("jamendo,wikimedia_audio", url.queryParameter("source"))
        assertEquals("cc0,pdm,by,by-sa", url.queryParameter("license"))
    }

    @Test fun carriesAttributionWithoutAdmittingTheStreamToPlaybackOrDownloads() {
        val result = decode(row())!!
        assertEquals(DiscoveryItemId(DiscoverySource.OPENVERSE, id), result.id)
        assertEquals("Torley on Piano", result.creator)
        assertEquals("CC BY-SA 3.0", result.licenseLabel)
        assertEquals(122, result.durationSeconds)
        assertEquals(jamendoPage, result.canonicalUrl.toString())
        assertEquals(DiscoveryPermission.UNAVAILABLE, result.capabilities.playInApp)
        assertEquals(DiscoveryPermission.UNAVAILABLE, result.capabilities.offline)
    }

    @Test fun rejectsUnsupportedLicensesMatureAudioAndUnexpectedLandingPages() {
        assertNull(decode(row().put("license", "by-nc")))
        assertNull(decode(row().put("license", JSONObject.NULL)))
        assertNull(decode(row().put("mature", true)))
        assertNull(decode(row().put("mature", "false")))
        assertNull(decode(row().put("category", "podcast")))
        assertNull(decode(row().put("foreign_landing_url", "https://www.jamendo.com/download/150798")))
        assertNull(decode(row().put("foreign_landing_url", "https://www.jamendo.com.evil.org/track/150798")))
        assertNull(decode(row().put("foreign_landing_url", "$jamendoPage?redirect=elsewhere")))
    }

    @Test fun supportsCommonsFileDescriptionPagesOnlyForMatchingSource() {
        val commonsPage = "https://commons.wikimedia.org/wiki/File:Recording.ogg"
        val commons = row().put("source", "wikimedia_audio").put("foreign_landing_url", commonsPage)
        assertEquals(commonsPage, decode(commons)?.canonicalUrl.toString())
        assertNull(decode(commons.put("foreign_landing_url", jamendoPage)))
    }
}
