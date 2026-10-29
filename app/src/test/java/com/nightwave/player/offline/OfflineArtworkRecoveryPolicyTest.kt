package com.nightwave.player.offline

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class OfflineArtworkRecoveryPolicyTest {
    private fun song(n: Int = 1) = OfflineSong(UUID(0, n.toLong()).toString() + ".mp3", "Original", "audio/mpeg", 256)

    @Test fun conflictingSourceLinksNeverChooseAnArbitraryVideo() {
        val id = song().id
        assertTrue(OfflineArtworkRecoveryPolicy.uniqueLinks(listOf("v_abcdefghijk" to id,
            "v_12345678901" to id)).isEmpty())
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk",
            OfflineArtworkRecoveryPolicy.uniqueLinks(listOf("v_abcdefghijk" to id,
                "v_abcdefghijk" to id))[id])
    }
    @Test fun unknownAndUnsafeLinksAreNotRecoveryEvidence() {
        assertTrue(OfflineArtworkRecoveryPolicy.uniqueLinks(listOf("signed_address" to song().id,
            "v_abcdefghijk" to "../../song.mp3", "v_short" to song().id)).isEmpty())
    }
    @Test fun visibleSelectionIsBoundedDistinctAndRespectsAttempts() {
        val songs = (1..100).map(::song)
        val batch = OfflineArtworkRecoveryPolicy.batch(listOf(songs[90]) + songs + songs, setOf(songs[0].id))
        assertEquals(24, batch.size)
        assertEquals(songs[90], batch.first())
        assertFalse(batch.contains(songs[0]))
        assertEquals(batch.size, batch.map { it.id }.distinct().size)
    }
    @Test fun cachedVariantsAreBoundedAndKeepKnownOriginal() {
        val original = "https://i.ytimg.com/vi/abcdefghijk/mqdefault.jpg"
        val urls = OfflineArtworkRecoveryPolicy.cacheCandidates("abcdefghijk", original)
        assertTrue(original in urls)
        assertTrue(urls.any { it.endsWith("hqdefault.jpg") })
        assertTrue(urls.size <= 12)
        assertTrue(urls.all { "/abcdefghijk/" in it })
        assertTrue(OfflineArtworkRecoveryPolicy.cacheCandidates("invalid", original).isEmpty())
    }
    @Test fun exactMembershipAndRecordFreshnessGatePublication() {
        val song = song()
        val fence = OfflineArtworkCommitFence(song, "before", "abcdefghijk", true)
        assertTrue(fence.allows(song, "before", song.id, true))
        assertFalse(fence.allows(null, "before", song.id, true))
        assertFalse(fence.allows(song.copy(bytes = 512), "before", song.id, true))
        assertFalse(fence.allows(song, "changed", song.id, true))
        assertFalse(fence.allows(song, "before", song(2).id, true))
        assertFalse(fence.allows(song, "before", song.id, false))
    }
    @Test fun authoritativeExistingSidecarDoesNotRequireHistoricalQueueLink() {
        val song = song()
        assertTrue(OfflineArtworkCommitFence(song, "known", "abcdefghijk", false)
            .allows(song, "known", null, true))
    }
    @Test fun contradictingVideoArtAndCredentialUrlsAreNotCopied() {
        val wrong = "https://i.ytimg.com/vi/12345678901/hqdefault.jpg"
        assertFalse(wrong in OfflineArtworkRecoveryPolicy.cacheCandidates("abcdefghijk", wrong))
        val credential = "https://user:secret@example.com/art.jpg"
        assertFalse(credential in OfflineArtworkRecoveryPolicy.cacheCandidates("abcdefghijk", credential))
    }
    private fun allowed(media: String? = "offline:a", selected: String? = "a", generation: Int = 7,
        index: String? = "original", revision: Long = 3, offline: Boolean = true): Boolean =
        OfflineArtworkPlaybackFence.allows("offline:a", media, selected, 7, generation,
            "original", index, 3, revision, offline)

    @Test fun serviceRejectsDeletedReplacedSelectedOrStaleCompletions() {
        assertTrue(allowed())
        assertFalse(allowed(media = "offline:b"))
        assertFalse(allowed(selected = "b"))
        assertFalse(allowed(generation = 8))
        assertFalse(allowed(index = "changed"))
        assertFalse(allowed(revision = 4))
        assertFalse(allowed(offline = false))
        assertFalse(allowed(media = null, selected = null))
    }
}
