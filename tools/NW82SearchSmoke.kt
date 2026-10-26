package com.nightwave.player.net

import com.nightwave.player.offline.OfflineSearchRanking

/**
 * Exact production source smoke with a compile-only VideoPreview/SearchMode stub.
 * This tools/ file is NOT included in Android app sources.
 *
 * kotlinc app/src/main/java/com/nightwave/player/net/SearchScriptNormalization.kt \
 *   app/src/main/java/com/nightwave/player/net/MusicIdentity.kt \
 *   app/src/main/java/com/nightwave/player/net/SearchTokenMatchingPolicy.kt \
 *   app/src/main/java/com/nightwave/player/net/SearchMusicIntentPolicy.kt \
 *   app/src/main/java/com/nightwave/player/net/SearchRelevancePolicy.kt \
 *   app/src/main/java/com/nightwave/player/net/SearchVersionGroupingPolicy.kt \
 *   app/src/main/java/com/nightwave/player/net/SearchResultPresentationPolicy.kt \
 *   app/src/main/java/com/nightwave/player/net/SavedTrackSearch.kt \
 *   app/src/main/java/com/nightwave/player/offline/OfflineSearchRanking.kt \
 *   tools/NW82SearchSmoke.kt -include-runtime -d nw82-search.jar
 * java -cp nw82-search.jar com.nightwave.player.net.NW82SearchSmoke
 *
 * Pure JVM policy smoke only; not Gradle, network/provider or physical device QA.
 */
data class VideoPreview(
    val title: String,
    val author: String,
    val url: String,
    val thumbnail: String,
    val durationSeconds: Long
)
enum class SearchMode { SONGS, VIDEOS }

object NW82SearchSmoke {
    private fun video(n: Int, title: String, artist: String = "Music"): VideoPreview =
        VideoPreview(title, artist,
            "https://www.youtube.com/watch?v=" + n.toString().padStart(11, '0'), "",
            200L)

    @JvmStatic fun main(args: Array<String>) {
        val exact = video(1, "Alo Alo (Official Audio)")
        val wrong = video(2, "Alan Walker - Alone (Official Audio)", "Alan Walker")
        check(SearchRelevancePolicy.rankPublic("alo alo", listOf(wrong, exact)).first() == exact)
        check(SavedTrackSearch.match("alo alo", listOf(wrong)).isEmpty())
        check(SavedTrackSearch.match("alo alo", listOf(wrong, exact)) == listOf(exact))
        check(SearchTokenMatchingPolicy.evidence("alo", listOf("alone"), true) == 0)

        val bangla = video(3, "মালা")
        val differentBangla = video(4, "মেলা")
        check(SavedTrackSearch.match("মালা", listOf(differentBangla, bangla)) == listOf(bangla))
        val moon = video(5, "Moonlight (Official Audio)")
        val remix = video(6, "Moonlight Remix")
        val live = video(7, "Moonlight (Live)")
        val results = SearchResultPresentationPolicy.groupMusic(
            "moonlight", SearchMode.SONGS, listOf(remix, live, moon, wrong))
        check(results.bestMatches.first() == moon)
        check(results.otherVersions.toSet() == setOf(remix, live))
        check((results.bestMatches + results.otherVersions + results.moreResults).toSet() ==
            setOf(moon, remix, live, wrong))
        check(SearchResultPresentationPolicy.groupMusic(
            "moonlight", SearchMode.VIDEOS, listOf(remix, live, moon)
        ).moreResults == listOf(remix, live, moon))

        val misspelled = SavedTrackSearch.match(
            "midnigth", listOf(video(8, "Midnight Drive")))
        check(misspelled.single().title == "Midnight Drive")
        check(OfflineSearchRanking.score(
            OfflineSearchRanking.terms("alo"), "Alone", "mp3") == -1)
        check(OfflineSearchRanking.score(
            OfflineSearchRanking.terms("alo alo"), "Alo Alo", "mp3") > 0)
        println("NW82_SEARCH_POLICY_SMOKE_PASS")
    }
}
