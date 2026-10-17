package com.nightwave.player.v2.ui

/**
 * Curated *search shortcuts*, not claimed recommendations or fabricated songs.
 * Selecting one always runs the same public search route as a typed query.
 */
data class DiscoveryShortcut(
    val key: String,
    val title: String,
    val subtitle: String,
    val query: String
)

object DiscoveryShortcuts {
    val picks = listOf(
        DiscoveryShortcut("late", "After hours", "Midnight drives", "night drive synthwave music"),
        DiscoveryShortcut("focus", "Deep focus", "Minimal & ambient", "ambient study focus music"),
        DiscoveryShortcut("desi", "Desi pulse", "Bangla & Hindi", "new bangla hindi music"),
        DiscoveryShortcut("acoustic", "Unplugged", "Acoustic sessions", "acoustic indie songs"),
        DiscoveryShortcut("energy", "Momentum", "Workout energy", "workout electronic music"),
        DiscoveryShortcut("lofi", "Slow mornings", "Laid-back beats", "lofi chill instrumental")
    )

    fun find(key: String): DiscoveryShortcut? = picks.firstOrNull { it.key == key }

    /** User-entered searches remain independent of suggested queries. */
    fun isValid(): Boolean = picks.isNotEmpty() &&
        picks.map { it.key }.distinct().size == picks.size &&
        picks.all { it.key.isNotBlank() && it.title.isNotBlank() &&
            it.subtitle.isNotBlank() && it.query.isNotBlank() && it.query.length <= 160 }
}
