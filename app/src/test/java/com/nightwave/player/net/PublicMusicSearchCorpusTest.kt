package com.nightwave.player.net

import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class PublicMusicSearchCorpusTest {
    @Test fun frozenCorpusReportsEveryTopResultErrorAndPreservesAllOriginalSelections() {
        val bytes = javaClass.getResourceAsStream("/nw130-public-search-corpus.json")!!.use { it.readBytes() }
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it.toInt() and 255) }
        assertEquals("8EEF27755ECFA3CC941B2DB72BD756A0E223420CED271C20A2C3211794763DF3", sha)
        val cases = JSONObject(bytes.toString(Charsets.UTF_8)).getJSONArray("cases")
        assertEquals(36, cases.length())
        val errors = mutableListOf<String>(); val results = JSONArray()
        repeat(cases.length()) { i ->
            val case = cases.getJSONObject(i); val query = case.getString("query")
            val candidates = case.getJSONArray("candidates")
            val rows = (0 until candidates.length()).map { n ->
                val item = candidates.getJSONObject(n)
                VideoPreview(item.getString("title"), item.getString("author"),
                    "https://www.youtube.com/watch?v=${(i * 10 + n).toString().padStart(11, '0')}", "", item.getLong("duration"))
            }
            val ranked = SearchRelevancePolicy.rankPublic(query, rows)
            val expected = rows[case.getInt("expected_top")]
            val ok = ranked.first() === expected
            if (!ok) errors.add(case.getString("id"))
            val grouped = SearchResultPresentationPolicy.groupMusic(query, SearchMode.SONGS, rows)
            val visible = grouped.bestMatches + grouped.otherVersions + grouped.moreResults
            assertEquals(rows.size, visible.size)
            assertEquals(rows.toSet(), visible.toSet())
            assertTrue(visible.all { selected -> rows.any { it === selected } })
            assertEquals(rows, SearchResultPresentationPolicy.groupMusic(query, SearchMode.VIDEOS, rows).moreResults)
            results.put(JSONObject().put("id", case.getString("id")).put("query", query)
                .put("expected_title", expected.title).put("actual_title", ranked.first().title).put("top_error", !ok))
        }
        val report = JSONObject().put("corpus_sha256", sha).put("cases", cases.length())
            .put("top_errors", errors.size).put("live_provider_test", false).put("results", results)
        File("build/reports/nw130-public-corpus.json").also { it.parentFile?.mkdirs(); it.writeText(report.toString(2), Charsets.UTF_8) }
        assertEquals("Frozen corpus errors: $errors", emptyList<String>(), errors)
    }
}
