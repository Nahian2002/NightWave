package com.nightwave.player.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic fixtures, not claims of provider-wide search precision. */
class SearchMusicIntentPolicyTest {
    private fun video(id: Int, title: String, creator: String = "Uploader") =
        VideoPreview(title, creator,
            "https://www.youtube.com/watch?v=" + id.toString().padStart(11, '0'),
            "", 215L)

    @Test fun fortyCommonMusicIntentsPreferTheUnmodifiedTrackOverUnrequestedVariants() {
        // Query, intended public video title, unrelated/alternate video title.
        val cases = listOf(
            Triple("alone 2", "Alan Walker & Ava Max - Alone, Pt. II (Official Audio)", "Home Alone 2 Movie Trailer"),
            Triple("alone pt ii", "Alan Walker - Alone Part 2 (Official Audio)", "Home Alone 2 Trailer"),
            Triple("believer", "Imagine Dragons - Believer (Official Audio)", "Believer reaction"),
            Triple("halo", "Beyoncé - Halo (Official Audio)", "Halo videogame scene"),
            Triple("shape of you", "Ed Sheeran - Shape of You (Official Audio)", "Shape of You cover"),
            Triple("blinding lights", "The Weeknd - Blinding Lights (Official Audio)", "Blinding Lights slowed reverb"),
            Triple("bad guy", "Billie Eilish - Bad Guy (Official Audio)", "Bad Guy karaoke"),
            Triple("levitating", "Dua Lipa - Levitating (Official Audio)", "Levitating remix"),
            Triple("faded", "Alan Walker - Faded (Official Audio)", "Faded sped up"),
            Triple("perfect", "Ed Sheeran - Perfect (Official Audio)", "Perfect acoustic"),
            Triple("stay", "The Kid LAROI - Stay (Official Audio)", "Stay 8D"),
            Triple("closer", "The Chainsmokers - Closer (Official Audio)", "Closer live"),
            Triple("heat waves", "Glass Animals - Heat Waves (Official Audio)", "Heat Waves instrumental"),
            Triple("lovely", "Billie Eilish - Lovely (Official Audio)", "Lovely nightcore"),
            Triple("dance monkey", "Tones and I - Dance Monkey (Official Audio)", "Dance Monkey reaction"),
            Triple("counting stars", "OneRepublic - Counting Stars (Official Audio)", "Counting Stars tutorial"),
            Triple("calm down", "Rema - Calm Down (Official Audio)", "Calm Down movie scene"),
            Triple("cheap thrills", "Sia - Cheap Thrills (Official Audio)", "Cheap Thrills live"),
            Triple("senorita", "Shawn Mendes - Señorita (Official Audio)", "Senorita cover"),
            Triple("sunflower", "Post Malone - Sunflower (Official Audio)", "Sunflower karaoke"),
            Triple("happier", "Marshmello - Happier (Official Audio)", "Happier acoustic"),
            Triple("dusk till dawn", "ZAYN - Dusk Till Dawn (Official Audio)", "Dusk Till Dawn remix"),
            Triple("something just like this", "The Chainsmokers - Something Just Like This (Official Audio)", "Something Just Like This sped up"),
            Triple("stitches", "Shawn Mendes - Stitches (Official Audio)", "Stitches reaction"),
            Triple("take on me", "a-ha - Take On Me (Official Audio)", "Take On Me live"),
            Triple("one dance", "Drake - One Dance (Official Audio)", "One Dance 8D"),
            Triple("attention", "Charlie Puth - Attention (Official Audio)", "Attention slowed"),
            Triple("rockabye", "Clean Bandit - Rockabye (Official Audio)", "Rockabye remix"),
            Triple("treat you better", "Shawn Mendes - Treat You Better (Official Audio)", "Treat You Better cover"),
            Triple("new rules", "Dua Lipa - New Rules (Official Audio)", "New Rules reaction"),
            Triple("love yourself", "Justin Bieber - Love Yourself (Official Audio)", "Love Yourself instrumental"),
            Triple("ocean eyes", "Billie Eilish - Ocean Eyes (Official Audio)", "Ocean Eyes cover"),
            Triple("despacito", "Luis Fonsi - Despacito (Official Audio)", "Despacito karaoke"),
            Triple("let me down slowly", "Alec Benjamin - Let Me Down Slowly (Official Audio)", "Let Me Down Slowly remix"),
            Triple("we dont talk anymore", "Charlie Puth - We Don't Talk Anymore (Official Audio)", "We Dont Talk Anymore slowed"),
            Triple("someone you loved", "Lewis Capaldi - Someone You Loved (Official Audio)", "Someone You Loved live"),
            Triple("secrets", "OneRepublic - Secrets (Official Audio)", "Secrets tutorial"),
            Triple("memories", "Maroon 5 - Memories (Official Audio)", "Memories cover"),
            Triple("wake me up", "Avicii - Wake Me Up (Official Audio)", "Wake Me Up nightcore"),
            Triple("radioactive", "Imagine Dragons - Radioactive (Official Audio)", "Radioactive 8D")
        )
        assertEquals(40, cases.size)
        cases.forEachIndexed { i, (query, preferredTitle, otherTitle) ->
            val preferred = video(1000 + i * 2, preferredTitle)
            val other = video(1001 + i * 2, otherTitle)
            val ranked = SearchRelevancePolicy.rankPublic(query, listOf(other, preferred))
            assertEquals("Ranking regression on: $query", preferred, ranked.first())
            assertEquals(setOf(other.url, preferred.url), ranked.map { it.url }.toSet())
        }
    }

    @Test fun partNumberAliasesAreContextualNotBroadRomanNumeralSubstitution() {
        assertEquals("alone 2", SearchMusicIntentPolicy.canonical("Alone, Pt. II"))
        assertEquals("alone 2", SearchMusicIntentPolicy.canonical("Alone Part Two"))
        assertEquals("chapter ii", SearchMusicIntentPolicy.canonical("Chapter II"))
        assertEquals("i love you", SearchMusicIntentPolicy.canonical("I Love You"))
        assertEquals("home alone 2024", SearchMusicIntentPolicy.canonical("Home Alone 2024"))
    }

    @Test fun explicitVariantIntentDoesNotPenalizeTheRequestedVersion() {
        val remix = video(20, "Alone Remix")
        val original = video(21, "Alone (Official Audio)")
        assertEquals(0, SearchMusicIntentPolicy.variantPenalty("alone remix", remix))
        assertTrue(SearchRelevancePolicy.publicScore(
            SearchRelevancePolicy.prepare("alone remix"), remix) >
            SearchRelevancePolicy.publicScore(
                SearchRelevancePolicy.prepare("alone remix"), original))
    }

    @Test fun exactMovieQueryCanStillFindItsMovieAndVideosKeepTheirOrder() {
        val movie = video(30, "Home Alone 2")
        val music = video(31, "Alan Walker - Alone, Pt. II (Official Audio)")
        assertEquals(movie, SearchRelevancePolicy.rankPublic(
            "home alone 2", listOf(music, movie)).first())
        assertEquals(listOf(music, movie), SearchResultPresentationPolicy.groupMusic(
            "home alone 2", SearchMode.VIDEOS, listOf(music, movie)).moreResults)
    }

    @Test fun resultsRemainPublicMetadataUnalteredAndNoneAreHidden() {
        val inputs = listOf(
            video(1, "Home Alone 2 Movie Trailer"),
            video(2, "Alan Walker - Alone, Pt. II"),
            video(3, "Alone Pt II remix"),
            video(4, "Home Alone 2 movie scene")
        )
        val result = SearchResultPresentationPolicy.groupMusic(
            "alone 2", SearchMode.SONGS, inputs)
        assertEquals(inputs.toSet(),
            (result.bestMatches + result.otherVersions + result.moreResults).toSet())
        assertEquals(inputs.size,
            result.bestMatches.size + result.otherVersions.size + result.moreResults.size)
        assertEquals(inputs[1].url, result.bestMatches.first().url)
    }

    @Test fun equalRankAndNoMatchPreserveProviderOrder() {
        val first = video(10, "Unrelated footage")
        val second = video(11, "Other channel footage")
        assertEquals(listOf(first, second),
            SearchRelevancePolicy.rankPublic("moonlight", listOf(first, second)))
        val noBest = SearchResultPresentationPolicy.groupMusic(
            "moonlight", SearchMode.SONGS, listOf(first, second))
        assertTrue(noBest.bestMatches.isEmpty())
        assertEquals(listOf(first, second), noBest.moreResults)
    }
}
