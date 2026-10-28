package com.nightwave.player.net

import com.nightwave.player.v2.data.room.LegacyLibrarySnapshotParser
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Opt-in host measurement of the actual production typing runner, not JUnit. */
object NW129SearchBenchmark {
    @JvmStatic fun main(args: Array<String>) = runBlocking {
        val initial = NW129SearchFixture.snapshot()
        val fixture = initial.copy(publicLibrary = initial.publicLibrary.copy(
            playlists = initial.publicLibrary.playlists.map { (name, rows) -> name to rows.map { song ->
                when (song.url.substringAfter("v=").toInt()) {
                    7998 -> song.copy(title = "ভালোবাসা")
                    7999 -> song.copy(title = "Alo Alo")
                    8000 -> song.copy(title = "Midnight")
                    else -> song
                }
            } }))
        val source = NW129SearchFixture.strings(fixture)
        val builds = mutableListOf<Double>()
        var index: LocalMusicSearchIndex? = null
        repeat(5) {
            val start = System.nanoTime()
            val parsed = withContext(Dispatchers.IO) { LegacyLibrarySnapshotParser.parse(source) }
            index = withContext(Dispatchers.Default) { LocalMusicSearchIndex.build(fixture.copy(publicLibrary = parsed)) }
            builds += (System.nanoTime() - start) / 1e6
        }
        val prepared = index!!
        val queries = listOf("Music 9999", "Creator 39", "valobasha", "alo alo", "midnigth", "Offline seeker 4999", "missingword")
        val measurements = JSONArray()
        val outputs = mutableListOf<String>()
        for (query in queries) {
            repeat(2) { LocalMusicQueryRunner.run(prepared, query) }
            val times = mutableListOf<Double>()
            repeat(20) {
                val start = System.nanoTime()
                val result = LocalMusicQueryRunner.run(prepared, query)
                times += (System.nanoTime() - start) / 1e6
                if (it == 0) outputs += query + result.saved.joinToString { v -> v.url } + result.offline.joinToString { a -> a.id }
            }
            times.sort()
            measurements.put(JSONObject().put("query", query).put("samples", times.size)
                .put("medianMs", (times[9] + times[10]) / 2).put("p95Ms", times[18]).put("maxMs", times.last()))
        }
        fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        val hashes = JSONObject()
        listOf("net/SavedTrackSearch.kt", "net/LocalSearchSuggestions.kt", "net/LocalMusicSearchIndex.kt",
            "net/LocalMusicSearchController.kt", "net/SearchRelevancePolicy.kt", "net/SearchTokenMatchingPolicy.kt",
            "net/SearchScriptNormalization.kt", "net/SearchMultilingualAliasPolicy.kt", "net/MusicIdentity.kt",
            "net/SavedCreatorBrowser.kt", "offline/OfflineSearchRanking.kt", "offline/OfflineTrackSearch.kt",
            "library/MusicLibrary.kt", "v2/data/room/LegacyLibrarySnapshot.kt", "v2/ui/NightWaveLocalSearchCorpus.kt",
            "v2/ui/NightWaveStudioActivity.kt").forEach { name ->
            val path = "app/src/main/java/com/nightwave/player/$name"
            hashes.put(path, hash(File(path).readBytes()))
        }
        val report = JSONObject().put("scope", "Host JVM production query runner including 120ms debounce; excludes Android IME, prefs disk IO, rendering and audible/device acceptance")
            .put("publicUniqueRows", prepared.publicCount).put("publicReferences", prepared.referenceCount)
            .put("offlineRows", prepared.offlineCount).put("playlists", fixture.publicLibrary.playlists.size)
            .put("java", System.getProperty("java.runtime.version")).put("os", System.getProperty("os.name"))
            .put("buildSamples", builds.size).put("parseAndPrepareMs", JSONArray(builds))
            .put("measurements", measurements).put("sourceSha256", hashes)
            .put("outputSha256", hash(outputs.joinToString("\n").toByteArray(Charsets.UTF_8)))
            .put("harnessSha256", hash(File("app/src/test/java/com/nightwave/player/net/NW129SearchBenchmark.kt").readBytes()))
            .put("fixtureSha256", hash(File("app/src/test/java/com/nightwave/player/net/LocalMusicSearchIndexTest.kt").readBytes()))
        File(args.single()).also { it.parentFile?.mkdirs(); it.writeText(report.toString(2) + "\n", Charsets.UTF_8) }
        println("NW129 host measurements saved; Android end-to-end/device acceptance NOT_RUN")
    }
}
