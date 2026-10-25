package com.nightwave.player.v2.library

/**
 * NW71 read-only pool allocation for Smart Library cards.
 *
 * Each category receives an explicit quota before the global cap is applied.
 * This prevents an enormous favorites list from silently hiding playlist-only
 * recordings. It never creates URLs or modifies the source lists.
 */
internal object SmartDiscoveryPoolPolicy {
    const val MAX_POOL = 600
    const val FAVORITE_QUOTA = 200
    const val RECENT_QUOTA = 150
    const val PLAYLIST_QUOTA = 250

    fun <T> gather(
        favorites: List<T>,
        recent: List<T>,
        playlists: List<List<T>>,
        key: (T) -> String,
        eligible: (T) -> Boolean
    ): List<T> {
        val result = ArrayList<T>(MAX_POOL)
        val seen = HashSet<String>()
        fun accept(source: Sequence<T>, maxAccepted: Int) {
            var accepted = 0
            for (item in source) {
                if (accepted >= maxAccepted || result.size >= MAX_POOL) break
                val id = key(item)
                if (id.isBlank() || !eligible(item) || !seen.add(id)) continue
                result.add(item)
                accepted++
            }
        }
        accept(favorites.asSequence().take(500), FAVORITE_QUOTA)
        accept(recent.asSequence().take(300), RECENT_QUOTA)
        // The older implementation flattened every playlist, potentially
        // allocating a large temporary list even though the output was capped.
        accept(playlists.asSequence().take(60).flatMap {
            it.asSequence().take(200)
        }, PLAYLIST_QUOTA)
        return result
    }

    /**
     * Rediscovery should feature saved favorites the user has NOT just heard,
     * not simply reverse the entire favorites list.
     */
    fun <T> rediscover(
        pool: List<T>, favoriteKeys: Set<String>, recentKeys: Set<String>,
        key: (T) -> String, limit: Int = 60
    ): List<T> {
        if (limit <= 0) return emptyList()
        return pool.asReversed().asSequence().filter {
            val id = key(it)
            id in favoriteKeys && id !in recentKeys
        }.take(limit.coerceAtMost(60)).toList()
    }
}
