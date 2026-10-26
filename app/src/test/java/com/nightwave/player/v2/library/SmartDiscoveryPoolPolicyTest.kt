package com.nightwave.player.v2.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartDiscoveryPoolPolicyTest {
    private val p = SmartDiscoveryPoolPolicy

    @Test fun hugeFavoritesAndRecentDoNotHidePlaylistOnlyTracks() {
        val favorites = (0 until 600).map { "favorite-$it" }
        val recent = (0 until 500).map { "recent-$it" }
        // The production scan caps EACH playlist at 200 tracks. Two distinct
        // playlists are required to fill the 250-track playlist category quota.
        val playlist = (0 until 400).map { "playlist-$it" }
        val pool = p.gather(favorites, recent,
            listOf(playlist.take(200), playlist.drop(200)), { it }, { true })
        assertEquals(600, pool.size)
        assertEquals(200, pool.count { it.startsWith("favorite-") })
        assertEquals(150, pool.count { it.startsWith("recent-") })
        assertEquals(250, pool.count { it.startsWith("playlist-") })
    }

    @Test fun duplicatesAndInvalidSourcesCannotFillSourceQuotas() {
        val pool = p.gather(
            listOf("a", "a", "unsafe", "b"),
            listOf("b", "c"),
            listOf(listOf("c", "d", "unsafe")),
            { it }, { it != "unsafe" })
        assertEquals(listOf("a", "b", "c", "d"), pool)
    }

    @Test fun playlistScanCapsAndDoesNotMutateSource() {
        val saved = (1..500).map { "playlist-$it" }.toMutableList()
        val before = saved.toList()
        val pool = p.gather(emptyList(), emptyList(), listOf(saved), { it }, { true })
        assertEquals(200, pool.size) // per playlist cap prevents costly unbounded scans
        assertEquals(before, saved)
    }

    @Test fun rediscoveryIncludesOnlyLikedTracksOutsideRecentHistory() {
        val pool = listOf("liked-recent", "liked-old", "playlist-only")
        val actual = p.rediscover(pool, setOf("liked-recent", "liked-old"),
            setOf("liked-recent"), { it })
        assertEquals(listOf("liked-old"), actual)
        assertTrue(p.rediscover(pool, setOf("liked-recent"), setOf("liked-recent"), { it }).isEmpty())
    }

    @Test fun rediscoveryRespectsLimitAndDoesNotTouchSource() {
        val pool = (0..100).map { "song-$it" }
        val original = pool.toList()
        assertEquals(2, p.rediscover(pool, pool.toSet(), emptySet(), { it }, limit = 2).size)
        assertTrue(p.rediscover(pool, pool.toSet(), emptySet(), { it }, limit = 0).isEmpty())
        assertEquals(original, pool)
    }
}
