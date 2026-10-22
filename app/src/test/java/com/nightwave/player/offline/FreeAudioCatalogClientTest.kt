package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

class FreeAudioCatalogClientTest {
    private fun resultPage(
        license: String = "CC0 1.0",
        hash: String = "0".repeat(31),
        mime: String = "audio/ogg",
        url: String = "https://upload.wikimedia.org/wikipedia/commons/a/ab/Open_song.ogg"
    ): JSONObject {
        val details = JSONObject().put("url", url)
            .put("descriptionurl", "https://commons.wikimedia.org/wiki/File:Open_song.ogg")
            .put("size", 54321)
            .put("mime", mime)
            .put("sha1", hash)
            .put("extmetadata", JSONObject().put("LicenseShortName",
                JSONObject().put("value", license)))
        return JSONObject().put("ns", 6).put("title", "File:Open_song.ogg")
            .put("imageinfo", JSONArray().put(details))
    }

    private fun parse(vararg pages: JSONObject): List<FreeAudioCandidate> {
        val json = JSONObject().put("query",
            JSONObject().put("pages", JSONArray().apply { pages.forEach { put(it) } }))
        return FreeAudioCatalogClient.parseResults(json.toString())
    }

    @Test fun cc0SongIncludesExactSourceChecksum() {
        val found = parse(resultPage())
        assertEquals(1, found.size)
        assertEquals("Open song.ogg", found.single().saveName)
        assertEquals("0".repeat(31), found.single().sha1Base36)
        assertEquals(54321L, found.single().bytes)
    }

    @Test fun excludesCopyrightedVideoAndMissingHash() {
        assertTrue(parse(resultPage(license = "CC BY-NC 4.0")).isEmpty())
        assertTrue(parse(resultPage(mime = "video/webm")).isEmpty())
        assertTrue(parse(resultPage(hash = "")).isEmpty())
        assertTrue(parse(resultPage(hash = "not a hash")).isEmpty())
    }

    @Test fun forgedSourceIsNeverMadeAvailableForDownload() {
        assertTrue(parse(resultPage(url = "https://upload.wikimedia.org.attacker.com/a.ogg")).isEmpty())
    }

    @Test fun duplicateCatalogUrlsAppearOnce() {
        assertEquals(1, parse(resultPage(), resultPage()).size)
    }
}
