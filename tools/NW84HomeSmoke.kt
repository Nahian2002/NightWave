package com.nightwave.player.v2.recommendation

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.ui.NightWaveHomeLayoutPolicy

/**
 * Source-only standalone JVM smoke, without Android runtime or a media engine.
 * The PersonalMix model here is a compile-only duplicate, solely because the
 * production model is co-located with the app's recommendation engine.
 *
 * kotlinc app/src/main/java/com/nightwave/player/core/VideoLink.kt \
 *   app/src/main/java/com/nightwave/player/v2/recommendation/MixDiversityPolicy.kt \
 *   app/src/main/java/com/nightwave/player/v2/recommendation/NightWaveHomeCurationPolicy.kt \
 *   app/src/main/java/com/nightwave/player/v2/ui/NightWaveHomeLayoutPolicy.kt \
 *   tools/NW84HomeSmokeModels.kt tools/NW84HomeSmoke.kt \
 *   -include-runtime -d nw84-home.jar
 * java -cp nw84-home.jar com.nightwave.player.v2.recommendation.NW84HomeSmoke
 */
data class PersonalMix(
    val key: String,
    val title: String,
    val description: String,
    val reason: String,
    val tracks: List<VideoPreview>
)

object NW84HomeSmoke {
    private fun song(id: Int) = VideoPreview(
        "Track $id", "Artist ${id % 4}",
        "https://www.youtube.com/watch?v=" + id.toString().padStart(11, '0'),
        "", 180L
    )
    private fun mix(name: String, songs: List<VideoPreview>) =
        PersonalMix(name, name, "From saved music", "Local only", songs)

    @JvmStatic fun main(args: Array<String>) {
        val liked = (1..6).map(::song)
        val playlists = (5..12).map(::song)
        val recent = listOf(song(2), song(8))
        val a = mix("daily_mix_1", liked)
        val clone = mix("replay", liked.take(5))
        val different = mix("rediscover", (7..12).map(::song))
        val curate = NightWaveHomeCurationPolicy::curate
        val plan = curate(liked, recent, playlists,
            listOf(a), listOf(clone, different), 900L)
        val allKnown = (liked + playlists).map { it.url }.toSet()
        check(plan.nextListens.isNotEmpty())
        check(plan.nextListens.all { it.url in allKnown && it.url !in recent.map(VideoPreview::url) })
        check(plan.nextListens.size == plan.nextListens.distinctBy { it.url }.size)
        check(plan.mixes.map { it.key } == listOf("daily_mix_1", "rediscover"))
        check(plan == curate(liked, recent, playlists,
            listOf(a), listOf(clone, different), 900L))
        check(NightWaveHomeCurationPolicy.sourceLabel(song(1), liked) == "Liked song")
        check(NightWaveHomeCurationPolicy.sourceLabel(song(10), liked) == "From your playlists")
        check(NightWaveHomeLayoutPolicy.mixColumns(320f, 1f) == 1)
        check(NightWaveHomeLayoutPolicy.mixColumns(430f, 1f) == 2)
        check(NightWaveHomeLayoutPolicy.mixColumns(430f, 1.5f) == 1)
        check(NightWaveHomeLayoutPolicy.mixColumns(Float.NaN, 1f) == 1)
        println("NW84_HOME_LOCAL_CURATION_SMOKE_PASS")
    }
}
