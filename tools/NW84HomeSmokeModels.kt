package com.nightwave.player.net

/**
 * NW84 JVM smoke compile-only test data model. This file is in tools/, never
 * compiled into the app; production VideoPreview is in YouTubeGateway.kt.
 */
data class VideoPreview(
    val title: String, val author: String, val url: String,
    val thumbnail: String, val durationSeconds: Long
)
