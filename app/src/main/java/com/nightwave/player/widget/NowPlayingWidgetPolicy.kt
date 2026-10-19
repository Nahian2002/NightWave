package com.nightwave.player.widget

/** Pure presentation contract: never embed a stream URL, file path or track ID in a widget. */
data class WidgetPlaybackSnapshot(
    val hasTrack: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = ""
)

object NowPlayingWidgetPolicy {
    const val MAX_TITLE = 96
    const val MAX_ARTIST = 80

    private fun safeText(input: String, limit: Int): String =
        input.filter { !Character.isISOControl(it) }.trim().take(limit)

    fun normalized(snapshot: WidgetPlaybackSnapshot): WidgetPlaybackSnapshot =
        if (!snapshot.hasTrack) WidgetPlaybackSnapshot()
        else WidgetPlaybackSnapshot(
            hasTrack = true,
            isPlaying = snapshot.isPlaying,
            title = safeText(snapshot.title, MAX_TITLE),
            artist = safeText(snapshot.artist, MAX_ARTIST)
        )

    fun title(snapshot: WidgetPlaybackSnapshot): String {
        val safe = normalized(snapshot)
        return if (!safe.hasTrack) "Ready when you are"
            else safe.title.ifBlank { "Unknown track" }
    }

    fun subtitle(snapshot: WidgetPlaybackSnapshot): String {
        val safe = normalized(snapshot)
        return if (!safe.hasTrack) "Open NightWave to start listening"
            else safe.artist.ifBlank { "NightWave audio" }
    }

    fun playbackDescription(snapshot: WidgetPlaybackSnapshot): String =
        if (!snapshot.hasTrack) "Open NightWave"
        else if (snapshot.isPlaying) "Pause playback" else "Resume playback"
}
