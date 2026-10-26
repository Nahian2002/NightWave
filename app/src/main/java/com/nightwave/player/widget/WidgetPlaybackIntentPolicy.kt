package com.nightwave.player.widget

/** Only Media3 loaded-item state can authorize a widget Play/Pause action.
 * During buffering isPlaying can be false while playWhenReady remains true;
 * the widget must then cancel the pending Play request, not play again.
 * A missing MediaItem never resurrects the stored queue.
 */
internal object WidgetPlaybackIntentPolicy {
    enum class Command { NONE, PLAY, PAUSE }

    fun command(hasLoadedMedia: Boolean, playWhenReady: Boolean,
                canPlayPause: Boolean): Command = when {
        !hasLoadedMedia || !canPlayPause -> Command.NONE
        playWhenReady -> Command.PAUSE
        else -> Command.PLAY
    }

    /** Icon represents the next available action, even while buffering. */
    fun showsPauseIcon(hasLoadedMedia: Boolean, playWhenReady: Boolean): Boolean =
        hasLoadedMedia && playWhenReady
}
