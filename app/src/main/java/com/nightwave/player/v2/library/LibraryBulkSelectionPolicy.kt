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
}
