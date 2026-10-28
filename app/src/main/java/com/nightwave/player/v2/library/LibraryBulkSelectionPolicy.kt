package com.nightwave.player.v2.library

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview

/** Ephemeral UI-only selection; never writes to or deletes songs. */
object LibraryBulkSelectionPolicy {
    const val MAX_SELECTED = 20

    private fun eligible(url: String): Boolean = VideoLink.normalize(url) == url

    fun selected(tracks: List<VideoPreview>, urls: Set<String>): List<VideoPreview> =
        tracks.filter { it.url in urls && eligible(it.url) }.distinctBy { it.url }.take(MAX_SELECTED)

    /**
     * Safely restore ephemeral UI selections after playlist edits, filters or
     * process recreation. Ignore vanished song IDs and never exceed the same
     * 20-item guard used by atomic playlist actions. Keep existing selection
     * identity order where it remains valid; do not modify any library data.
     */
    fun sanitize(current: Set<String>, source: List<VideoPreview>): Set<String> {
        val allowed = source.asSequence().map { it.url }.filter(::eligible).toHashSet()
        return current.asSequence().filter { it in allowed }
            .take(MAX_SELECTED).toCollection(linkedSetOf())
    }

    fun toggle(urls: Set<String>, url: String, tracks: List<VideoPreview>): Set<String> {
        val safe = sanitize(urls, tracks)
        if (!eligible(url) || tracks.none { it.url == url }) return safe
        if (url in safe) return safe - url
        if (safe.size >= MAX_SELECTED) return safe
        return safe + url
    }

    fun selectVisible(tracks: List<VideoPreview>): Set<String> =
        tracks.asSequence().map { it.url }.filter(::eligible).distinct().take(MAX_SELECTED).toSet()

    /**
     * NW86: add just the currently visible songs to existing selection. If
     * every visible song is already selected, deselect only those songs.
     * Hidden selection survives filters and is always counted in the UI;
     * no underlying playlist order or membership is changed.
     */
    fun toggleVisible(
        current: Set<String>, visible: List<VideoPreview>, source: List<VideoPreview>
    ): Set<String> {
        val allowed = source.asSequence().map { it.url }.filter(::eligible).toHashSet()
        val safe = sanitize(current, source).toMutableSet()
        val visibleUrls = visible.asSequence().map { it.url }
            .filter { it in allowed }.distinct().toList()
        if (visibleUrls.isEmpty()) return safe
        if (visibleUrls.all { it in safe }) return safe - visibleUrls.toSet()
        for (url in visibleUrls) {
            if (safe.size >= MAX_SELECTED) break
            safe.add(url)
        }
        return safe
    }

    fun hiddenSelectionCount(
        selected: Set<String>, visible: List<VideoPreview>
    ): Int {
        val shown = visible.asSequence().map { it.url }.toSet()
        return selected.count { it !in shown }
    }

    fun visibleActionLabel(
        current: Set<String>, visible: List<VideoPreview>
    ): String {
        val eligibleVisible = visible.asSequence().map { it.url }.filter(::eligible).toSet()
        return if (eligibleVisible.isNotEmpty() && eligibleVisible.all { it in current })
            "Deselect visible" else "Select visible"
    }
}
