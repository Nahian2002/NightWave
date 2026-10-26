package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

class PublicBookmarkCheckpointPolicyTest {
    private val first = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val second = "https://www.youtube.com/watch?v=J---aiyznGQ"

    @Test fun permitsOnlyCurrentSelectedPublicSong() {
        assertTrue(PublicBookmarkCheckpointPolicy.shouldPersist(first, first, false))
    }

    @Test fun oldMediaItemCannotOverwriteNewSelectionAfterReplace() {
        assertFalse(PublicBookmarkCheckpointPolicy.shouldPersist(first, second, false))
    }

    @Test fun pausedOfflineTrackCannotUpdatePublicBookmark() {
        assertFalse(PublicBookmarkCheckpointPolicy.shouldPersist(first, first, true))
    }

    @Test fun missingSelectedSongAndNullMediaFailClosed() {
        assertFalse(PublicBookmarkCheckpointPolicy.shouldPersist(first, null, false))
        assertFalse(PublicBookmarkCheckpointPolicy.shouldPersist(null, first, false))
    }

    @Test fun signedAndPrivateSourcesCannotBePersisted() {
        val expiring = "https://rr.googlevideo.com/videoplayback?signature=secret"
        assertFalse(PublicBookmarkCheckpointPolicy.shouldPersist(expiring, expiring, false))
        assertFalse(PublicBookmarkCheckpointPolicy.shouldPersist(
            "file:///data/user/0/audio", "file:///data/user/0/audio", false))
    }

    @Test fun noncanonicalPublicWatchUrlDoesNotPassEvenIfIdentical() {
        val signed = "$first&sig=secret"
        assertFalse(PublicBookmarkCheckpointPolicy.shouldPersist(signed, signed, false))
    }
}
