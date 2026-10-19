package com.nightwave.player.auto

import java.util.Base64

/**
 * Strict IDs for car browsing. IDs never grant access to arbitrary stream URLs,
 * paths or user-private files. The catalog independently validates membership.
 */
object AutoBrowsePolicy {
    const val ROOT = "nightwave:auto:root"
    const val IMPORTED = "nightwave:auto:imported"
    const val FAVORITES = "nightwave:auto:favorites"
    const val RECENT = "nightwave:auto:recent"
    const val PLAYLISTS = "nightwave:auto:playlists"
    const val PLAYLIST_PREFIX = "nightwave:auto:playlist:"
    const val PUBLIC_PREFIX = "nightwave:auto:public:"
    const val OFFLINE_PREFIX = "nightwave:auto:offline:"
    const val MAX_PAGE_SIZE = 100
    const val MAX_RESULTS = 150

    fun encode(value: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(Charsets.UTF_8))

    fun decode(value: String): String? {
        if (value.length !in 1..1000 || !value.matches(Regex("[A-Za-z0-9_-]+"))) return null
        return runCatching {
            val decoded = Base64.getUrlDecoder().decode(value)
            require(decoded.size in 1..500)
            val text = String(decoded, Charsets.UTF_8)
            require(text.toByteArray(Charsets.UTF_8).contentEquals(decoded))
            text
        }.getOrNull()
    }

    fun playlistId(name: String): String = PLAYLIST_PREFIX + encode(name)

    fun playlistName(id: String): String? =
        id.takeIf { it.startsWith(PLAYLIST_PREFIX) }
            ?.removePrefix(PLAYLIST_PREFIX)?.let(::decode)

    fun publicId(parent: String, url: String): String =
        PUBLIC_PREFIX + encode(parent) + ":" + encode(url)

    fun publicTarget(id: String): Pair<String, String>? {
        if (!id.startsWith(PUBLIC_PREFIX)) return null
        val pieces = id.removePrefix(PUBLIC_PREFIX).split(':')
        if (pieces.size != 2) return null
        val parent = decode(pieces[0]) ?: return null
        val url = decode(pieces[1]) ?: return null
        if (parent != FAVORITES && parent != RECENT && playlistName(parent) == null) return null
        return parent to url
    }

    fun offlineId(id: String): String = OFFLINE_PREFIX + id

    fun offlineTarget(id: String): String? =
        id.takeIf { it.startsWith(OFFLINE_PREFIX) }
            ?.removePrefix(OFFLINE_PREFIX)
            ?.takeIf { it.matches(Regex("[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)")) }

    fun <T> page(items: List<T>, page: Int, pageSize: Int): List<T> {
        if (page < 0 || pageSize !in 1..MAX_PAGE_SIZE) return emptyList()
        val start = page.toLong() * pageSize
        if (start >= items.size) return emptyList()
        return items.subList(start.toInt(), minOf(items.size, start.toInt() + pageSize))
    }
}
