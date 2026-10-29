package com.nightwave.player.net

import com.nightwave.player.core.VideoLink
import java.util.Locale
import java.util.concurrent.TimeUnit

internal data class CachedPublicSearch(val result: VideoSearchResults, val ageMillis: Long, val fresh: Boolean)

/** One gateway's public-only memory. No disk, account, local corpus or playback URL cache. */
internal class PublicSearchResultCache(private val clockMillis: () -> Long = {
    TimeUnit.NANOSECONDS.toMillis(System.nanoTime())
}) {
    private data class Key(val query: String, val mode: SearchMode)
    private data class Entry(val at: Long, val result: VideoSearchResults, val units: Int)
    private val entries = LinkedHashMap<Key, Entry>(12, .75f, true)
    private var textUnits = 0
    internal val entryCount: Int @Synchronized get() = entries.size
    internal val retainedTextUnits: Int @Synchronized get() = textUnits
    private fun key(query: String, mode: SearchMode) = Key(query.trim().take(160).lowercase(Locale.ROOT), mode)

    @Synchronized fun get(query: String, mode: SearchMode): CachedPublicSearch? {
        val key = key(query, mode)
        val entry = entries[key] ?: return null
        val age = clockMillis() - entry.at
        if (age !in 0 until STALE_MS) {
            entries.remove(key); textUnits -= entry.units
            return null
        }
        return CachedPublicSearch(entry.result.copy(videos = entry.result.videos.toList(), fromCache = true), age, age < FRESH_MS)
    }

    @Synchronized fun put(query: String, mode: SearchMode, result: VideoSearchResults): Boolean {
        if (query.trim().isEmpty() || result.videos.size !in 1..55) return false
        if (result.videos.any { VideoLink.normalizeSearchResult(it.url) != it.url }) return false
        val units = result.videos.sumOf { it.title.length.toLong() + it.author.length + it.url.length + it.thumbnail.length } +
            result.note.length + query.take(160).length
        if (units > MAX_ENTRY_UNITS) return false // Live results are never truncated to fit the cache.
        val now = clockMillis()
        val stale = entries.filterValues { now - it.at !in 0 until STALE_MS }.keys.toList()
        stale.forEach { textUnits -= entries.remove(it)!!.units }
        val key = key(query, mode)
        entries.remove(key)?.let { textUnits -= it.units }
        val copy = result.copy(videos = result.videos.toList(), fromCache = false)
        entries[key] = Entry(now, copy, units.toInt()); textUnits += units.toInt()
        while (entries.size > MAX_ENTRIES || textUnits > MAX_TEXT_UNITS) {
            val oldest = entries.keys.first(); textUnits -= entries.remove(oldest)!!.units
        }
        return true
    }
    companion object {
        const val FRESH_MS = 120_000L
        const val STALE_MS = 600_000L
        const val MAX_ENTRIES = 12
        const val MAX_ENTRY_UNITS = 50_000
        const val MAX_TEXT_UNITS = 250_000
    }
}
