package com.nightwave.player.v2.recommendation

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic device-only metadata; never accesses a provider or user history. */
class NightWaveHomeCurationPolicyTest {
    private fun song(n: Int, artist: String = "Artist ${n % 5}"): VideoPreview =
        VideoPreview("Track $n", artist,
            "https://www.youtube.com/watch?v=" + n.toString().padStart(11, '0'),
            "https://i.ytimg.com/vi/" + n.toString().padStart(11, '0') + "/hqdefault.jpg",
            180L)

    private fun mix(key: String, vararg songs: VideoPreview): PersonalMix =
        PersonalMix(key, key, "Real local songs", "Saved on device", songs.toList())

    private fun plan(
        favorites: List<VideoPreview> = emptyList(),
        recent: List<VideoPreview> = emptyList(),
        playlists: List<VideoPreview> = emptyList(),
        daily: List<PersonalMix> = emptyList(),
        personal: List<PersonalMix> = emptyList(),
        day: Long = 12345L
    ) = NightWaveHomeCurationPolicy.curate(favorites, recent, playlists, daily, personal, day)

    @Test fun noSavedDataShowsNoInventedQuickPicksOrMixes() {
        assertTrue(plan().nextListens.isEmpty())
        assertTrue(plan().mixes.isEmpty())
        assertTrue(plan(recent = listOf(song(1), song(2))).nextListens.isEmpty())
    }

    @Test fun nextListensExcludeRecentAndKeepOriginalSourceMetadata() {
        val favorites = (1..5).map(::song)
        val playlists = (5..10).map(::song)
        val recent = listOf(song(1), song(7))
        val output = plan(favorites, recent, playlists).nextListens
        val candidates = (favorites + playlists).mapTo(hashSetOf()) { it.url }
        assertTrue(output.isNotEmpty())
        assertTrue(output.size <= NightWaveHomeCurationPolicy.MAX_QUICK_PICKS)
        assertTrue(output.all { it.url in candidates && it.url !in recent.map { r -> r.url } })
        assertEquals(output.size, output.map { it.url }.distinct().size)
        assertTrue(output.all { candidate ->
            (favorites + playlists).any { original -> original == candidate }
        })
        assertEquals("Liked song", NightWaveHomeCurationPolicy.sourceLabel(song(5), favorites))
        assertEquals("From your playlists",
            NightWaveHomeCurationPolicy.sourceLabel(song(8), favorites))
    }

    @Test fun invalidLinksAndDuplicateSavedReferencesNeverBecomeRecommendations() {
        val invalid = VideoPreview("Fake", "Unknown", "https://invalid.invalid/nope", "", 0L)
        val valid = song(2)
        val output = plan(
            favorites = listOf(invalid, song(1), song(1), valid),
            playlists = listOf(song(1), invalid, song(3))).nextListens
        assertEquals(3, output.size)
        assertEquals(3, output.map { it.url }.toSet().size)
        assertFalse(output.any { it.url == invalid.url })
        assertTrue(output.all { it.thumbnail.startsWith("https://i.ytimg.com/") })
    }

    @Test fun sameDaySameSourcesRemainStableWithoutMutatingInputs() {
        val favorites = (1..20).map(::song).toMutableList()
        val recent = listOf(song(2), song(4))
        val playlists = (21..40).map(::song).toMutableList()
        val savedFavorites = favorites.toList()
        val savedPlaylists = playlists.toList()
        val a = plan(favorites, recent, playlists, day = 8123L)
        val b = plan(favorites, recent, playlists, day = 8123L)
        assertEquals(a, b)
        assertEquals(savedFavorites, favorites)
        assertEquals(savedPlaylists, playlists)
    }

    @Test fun nearDuplicateMixCardsDoNotRepeatSameCatalogOrRewriteTracks() {
        val a = (1..6).map(::song)
        val b = (7..12).map(::song)
        val first = mix("daily_mix_1", *a.toTypedArray())
        val clone = mix("replay", *a.take(5).toTypedArray())
        val independent = mix("rediscover", *b.toTypedArray())
        val duplicateKey = mix("daily_mix_1", *b.toTypedArray())
        val output = plan(daily = listOf(first),
            personal = listOf(clone, independent, duplicateKey)).mixes
        assertEquals(listOf("daily_mix_1", "rediscover"), output.map { it.key })
        assertEquals(first.tracks, output.first().tracks)
        assertEquals(independent.tracks, output.last().tracks)
    }

    @Test fun uniqueMixCardsAreCappedWithOriginalPriorityAndNoFabrication() {
        val mixes = (0..7).map { n ->
            mix("mix_$n", *((n * 5 + 1)..(n * 5 + 5)).map(::song).toTypedArray())
        }
        val output = plan(daily = mixes.take(3), personal = mixes.drop(3)).mixes
        assertEquals(NightWaveHomeCurationPolicy.MAX_MIX_CARDS, output.size)
        assertEquals(mixes.take(5), output)
    }

    @Test fun localQuickPicksNeverConsumeOptInListeningMemory() {
        val first = plan(favorites = (1..15).map(::song), day = 500L)
        val second = plan(favorites = (1..15).map(::song), day = 500L)
        assertEquals(first.nextListens, second.nextListens)
        // Curation takes no listening-memory or analytics parameters.
        assertTrue(first.nextListens.all { it.url.startsWith("https://www.youtube.com/watch?v=") })
    }
}
