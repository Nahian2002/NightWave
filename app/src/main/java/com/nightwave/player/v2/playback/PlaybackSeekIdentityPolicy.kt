package com.nightwave.player.v2.playback

/** Keep stale seek gestures from being applied to a newly selected Media3 item. */
object PlaybackSeekIdentityPolicy {
    fun accepts(expected: String, shown: String, mediaId: String?): Boolean =
        expected.isNotBlank() && expected == shown && expected == mediaId
}
