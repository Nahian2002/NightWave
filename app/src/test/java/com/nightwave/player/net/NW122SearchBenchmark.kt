package com.nightwave.player.net

import com.nightwave.player.v2.library.LibraryBrowsePolicy
import java.io.File
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/** Opt-in JavaExec entry point, not a timing assertion in the JUnit suite. */
object NW122SearchBenchmark {
    private var sink = 0

    @JvmStatic fun main(args: Array<String>) {
        val songs = (0 until 5_000).map { i ->
            val title = when (i % 5) {
                0 -> "Moonlight $i (Official Audio)"
                1 -> "ভালোবাসা $i"
                2 -> "Alo Alo $i"
                3 -> "Midnight $i - lyrics"
                else -> "Ocean Dreams $i"
            }
            VideoPreview(title, "Creator ${i % 40}", "https://www.youtube.com/watch?v=${i.toString().padStart(11, '0')}", "", 210)
        }
        val queries = listOf("moonlight", "ভালোবাসা", "alo alo", "valobasha", "midnigth", "missingword")
        val outputs = mutableListOf<String>()
        val rows = JSONArray()
        fun measure(name: String, operation: () -> List<VideoPreview>) {
            repeat(12) { sink = sink xor operation().hashCode() }
            val elapsed = LongArray(40) {
                val start = System.nanoTime()
                val result = operation()
                val time = System.nanoTime() - start
                sink = sink xor result.hashCode()
                time
            }.sorted()
            val result = operation()
            outputs += name + ":" + result.joinToString("|") { it.url + ":" + it.title }
            rows.put(JSONObject().put("scenario", name).put("samples", elapsed.size)
                .put("medianMs", (elapsed[19] + elapsed[20]) / 2.0 / 1e6)
                .put("p95Ms", elapsed[37] / 1e6).put("maxMs", elapsed.last() / 1e6)
                .put("resultCount", result.size))
        }
        queries.forEach { query -> measure("saved:$query") { SavedTrackSearch.match(query, songs) } }
        measure("public:moonlight") { SearchRelevancePolicy.rankPublic("moonlight", songs.take(150)) }
        measure("library:title") { LibraryBrowsePolicy.songs(songs, "", LibraryBrowsePolicy.SORT_TITLE) }
        measure("library:creator") { LibraryBrowsePolicy.songs(songs, "", LibraryBrowsePolicy.SORT_CREATOR) }
        val digest = MessageDigest.getInstance("SHA-256").digest(outputs.joinToString("\n").toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val report = JSONObject().put("scope", "synthetic host JVM; not Android latency, battery or soak")
            .put("java", System.getProperty("java.runtime.version"))
            .put("vm", System.getProperty("java.vm.name"))
            .put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"))
            .put("processors", Runtime.getRuntime().availableProcessors())
            .put("heapArguments", "-Xms512m -Xmx1024m (benchmark task)")
            .put("warmupsPerScenario", 12).put("libraryRows", songs.size)
            .put("outputSha256", digest).put("measurements", rows)
        val sourceHashes = JSONObject()
        listOf("net/SavedTrackSearch.kt", "net/SearchRelevancePolicy.kt", "net/SearchTokenMatchingPolicy.kt",
            "net/MusicIdentity.kt", "net/SearchScriptNormalization.kt", "net/SearchMultilingualAliasPolicy.kt",
            "net/SearchMusicIntentPolicy.kt", "v2/library/LibraryBrowsePolicy.kt").forEach { relative ->
            val path = "app/src/main/java/com/nightwave/player/$relative"
            sourceHashes.put(path, MessageDigest.getInstance("SHA-256").digest(File(path).readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 255) })
        }
        val harness = File("app/src/test/java/com/nightwave/player/net/NW122SearchBenchmark.kt")
        report.put("sourceSha256", sourceHashes).put("harnessSha256",
            MessageDigest.getInstance("SHA-256").digest(harness.readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 255) })
        File(args.single()).also { it.parentFile?.mkdirs(); it.writeText(report.toString(2) + "\n") }
        println(report.toString(2))
    }
}
