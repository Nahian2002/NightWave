package com.nightwave.player.playback

/**
 * NW68: only report usable Android audio effects when the exact active
 * playback session owns them. The system mixer / Bluetooth route can still
 * bypass an effect despite a successful framework acknowledgement.
 */
internal object EqualizerSessionPolicy {
    fun canPublish(sessionId: Int, hasControl: Boolean,
                   bandCount: Int, minimumMb: Int, maximumMb: Int): Boolean =
        sessionId > 0 && hasControl && bandCount in 1..32 &&
            minimumMb <= maximumMb

    fun canReuse(requestedPreset: Int, effectSessionId: Int,
                 playbackSessionId: Int, enabled: Boolean,
                 hasControl: Boolean): Boolean =
        requestedPreset != 0 && playbackSessionId > 0 &&
            effectSessionId == playbackSessionId && enabled && hasControl
}

/**
 * NW68: never swap the MediaSession player to a stale, stopped, or wrong
 * second decoder. Only the selected next private audio track can be adopted.
 */
internal object CrossfadeHandoffPolicy {
    fun mayHandoff(
        offline: Boolean, gaplessTimeline: Boolean, shuffled: Boolean,
        repeatOne: Boolean, timerPaused: Boolean,
        outgoingPlaying: Boolean, incomingPlaying: Boolean,
        oldIndex: Int, nextIndex: Int, count: Int,
        selectedId: String?, outgoingMediaId: String?,
        incomingMediaId: String?
    ): Boolean =
        offline && gaplessTimeline && !shuffled && !repeatOne &&
            !timerPaused && outgoingPlaying && incomingPlaying &&
            oldIndex >= 0 && nextIndex == oldIndex + 1 &&
            nextIndex in 1 until count &&
            !selectedId.isNullOrBlank() &&
            outgoingMediaId == "offline:$selectedId" &&
            !incomingMediaId.isNullOrBlank() &&
            incomingMediaId != outgoingMediaId
}
