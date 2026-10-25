package com.nightwave.player.playback

/**
 * NW49 display of *configured* audio transitions, not a claim of an active
 * second decoder. PlaybackService remains the authority on local track
 * verification, Media3 playlist readiness and handoff.
 */
object AudioTransitionUiPolicy {
    fun description(
        offline: Boolean,
        crossfadeSeconds: Int,
        shuffled: Boolean,
        repeatOne: Boolean,
        normalSpeed: Boolean
    ): String = when {
        !offline -> "Streaming playback uses normal track transitions."
        crossfadeSeconds <= 0 ->
            "Crossfade off · gapless is available for eligible local playlists."
        shuffled -> "Crossfade temporarily inactive while Shuffle is on."
        repeatOne -> "Crossfade temporarily inactive on Repeat One."
        !normalSpeed -> "Crossfade temporarily inactive away from 1× speed."
        else -> crossfadeSeconds.coerceAtMost(CrossfadePolicy.MAX_SECONDS).toString() +
            "s requested · starts only when the next verified offline track is ready."
    }

    fun dialogDetail(seconds: Int): String =
        if (seconds <= 0) "Off · use normal track transitions"
        else seconds.coerceIn(1, CrossfadePolicy.MAX_SECONDS).toString() +
            " seconds · eligible local tracks"
}
