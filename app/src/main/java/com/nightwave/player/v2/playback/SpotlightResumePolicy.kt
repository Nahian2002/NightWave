package com.nightwave.player.v2.playback

/** Pure decision logic: avoid re-extracting the already playing public stream. */
enum class SpotlightAction { KEEP_CURRENT, RESUME_CURRENT, RETRY_CURRENT, START_SELECTED }

object SpotlightResumePolicy {
    fun choose(
        tappedUrl: String,
        currentUrl: String,
        currentQueueIndex: Int,
        offline: Boolean,
        playing: Boolean,
        buffering: Boolean,
        hasError: Boolean
    ): SpotlightAction {
        if (tappedUrl.isBlank() || offline || tappedUrl != currentUrl || currentQueueIndex < 0)
            return SpotlightAction.START_SELECTED
        return when {
            hasError -> SpotlightAction.RETRY_CURRENT
            playing || buffering -> SpotlightAction.KEEP_CURRENT
            else -> SpotlightAction.RESUME_CURRENT
        }
    }
}
