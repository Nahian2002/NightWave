package com.nightwave.player.v2.library

import com.nightwave.player.net.VideoPreview

/** Ephemeral UI-only selection; never writes to or deletes songs. */
object LibraryBulkSelectionPolicy {
    const val MAX_SELECTED = 20

    fun selected(tracks: List<VideoPreview>, urls: Set<String>): List<VideoPreview> =
        tracks.filter { it.url in urls }.distinctBy { it.url }.take(MAX_SELECTED)

    fun toggle(urls: Set<String>, url: String, tracks: List<VideoPreview>): Set<String> {
        if (tracks.none { it.url == url }) return urls
        if (url in urls) return urls - url
        if (urls.size >= MAX_SELECTED) return urls
        return urls + url
    }

    fun selectVisible(tracks: List<VideoPreview>): Set<String> =
        tracks.asSequence().map { it.url }.distinct().take(MAX_SELECTED).toSet()

    /**
     * NW86: add just the currently visible songs to existing selection. If
     * every visible song is already selected, deselect only those songs.
     * Hidden selection survives filters and is always counted in the UI;
     * no underlying playlist order or membership is changed.
     */
    fun toggleVisible(
        current: Set<String>, visible: List<VideoPreview>, source: List<VideoPreview>
    ): Set<String> {
        val allowed = source.map { it.url }.toSet()
        val safe = current.filterTo(linkedSetOf()) { it in allowed }
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
    ): String = if (visible.isNotEmpty() && visible.all { it.url in current })
        "Deselect visible" else "Select visible"
}
