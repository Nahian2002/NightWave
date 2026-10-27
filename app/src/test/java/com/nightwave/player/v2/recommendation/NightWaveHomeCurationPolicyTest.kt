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

    @Test fun visibleRecentShelfShowsEachRealSongOnlyOnceInNewestOrder() {
        val first = song(1)
        val olderDuplicate = first.copy(title = "Outdated title", author = "Old uploader")
        val input = listOf(first, song(2), olderDuplicate, song(3), song(2))
        val visible = NightWaveHomeCurationPolicy.distinctRecent(input)
        assertEquals(listOf(first, song(2), song(3)), visible)
        assertEquals(listOf(first.url, song(2).url, song(3).url),
            visible.map { it.url })
        assertEquals(visible.size, visible.map { it.url }.toSet().size)
        // The first (most recent) original metadata wins; no title guessing.
        assertEquals(first.title, visible.first().title)
        assertEquals("Outdated title", input[2].title)
    }

    @Test fun visibleRecentShelfNeverMutatesSavedHistoryOrFabricatesTracks() {
        val original = mutableListOf(song(4), song(4), song(5), song(6), song(5))
        val before = original.toList()
        val visible = NightWaveHomeCurationPolicy.distinctRecent(original)
        assertEquals(before, original)
        assertEquals(listOf(song(4), song(5), song(6)), visible)
        assertEquals(emptyList<VideoPreview>(),
            NightWaveHomeCurationPolicy.distinctRecent(emptyList()))
        assertTrue(visible.all { it in original })
    }

    @Test fun rejectedNearCloneDoesNotClaimMixKeyForLaterDistinctCard() {
        val main = mix("daily_mix_1", *((1..6).map(::song)).toTypedArray())
        val rejected = mix("fresh", *((1..5).map(::song)).toTypedArray())
        val distinct = mix("fresh", *((7..12).map(::song)).toTypedArray())
        val plan = plan(daily = listOf(main),
            personal = listOf(rejected, distinct))
        assertEquals(listOf("daily_mix_1", "fresh"), plan.mixes.map { it.key })
        assertEquals(listOf(main, distinct), plan.mixes)
        assertEquals(distinct.tracks, plan.mixes[1].tracks)
    }

    @Test fun displayedMixKeysStillPreventDuplicateDestinationCards() {
        val first = mix("same", *((1..5).map(::song)).toTypedArray())
        val second = mix("same", *((6..10).map(::song)).toTypedArray())
        val distinct = mix("third", *((11..15).map(::song)).toTypedArray())
        val actual = plan(daily = listOf(first), personal = listOf(second, distinct))
        assertEquals(listOf(first, distinct), actual.mixes)
        assertEquals(2, actual.mixes.map { it.key }.distinct().size)
    }

    @Test fun multipleRejectedClonesCannotSuppressLaterRealShelvesOrChangeInput() {
        val first = mix("daily", *((1..6).map(::song)).toTypedArray())
        val clone1 = mix("new", *((1..4).map(::song)).toTypedArray())
        val clone2 = mix("new", *((2..6).map(::song)).toTypedArray())
        val unique = mix("new", *((20..24).map(::song)).toTypedArray())
        val personal = listOf(clone1, clone2, unique)
        val original = personal.toList()
        val output = plan(daily = listOf(first), personal = personal).mixes
        assertEquals(listOf(first, unique), output)
        assertEquals(original, personal)
        assertEquals("new", output.last().key)
    }

    @Test fun firstOccurrenceDeDupKeepsStablePublicIdentityAcrossRepeatedCalls() {
        val incoming = (1..40).map { song(it % 9 + 1) }
        val a = NightWaveHomeCurationPolicy.distinctRecent(incoming)
        val b = NightWaveHomeCurationPolicy.distinctRecent(incoming)
        assertEquals(a, b)
        assertEquals(9, a.size)
        assertEquals(a.size, a.map { it.url }.distinct().size)
        assertTrue(a.all { track -> incoming.any { it === track } })
    }
}
