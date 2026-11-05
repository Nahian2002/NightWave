package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class NW148CanonicalArtworkTest {
    private val id = "00000000001"
    private val track = "https://www.youtube.com/watch?v=$id"
    private val still = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
    private val original = "$still?sqp=Public_meta-1&rs=Public_signature-2"
    private fun candidates(url: String = original, identity: String = track, large: Boolean = true) =
        NightWaveArtworkPolicy.trackCandidates(url, identity, large)

    @Test fun matchingPublicMetadataUsesBoundedSameVideoStillsWithExactFallback() {
        assertEquals(listOf("maxresdefault.jpg", "sddefault.jpg", "hqdefault.jpg")
            .map { "https://i.ytimg.com/vi/$id/$it" } + original, candidates())
        assertEquals(original, candidates().last())
        assertEquals(4, candidates().size)
        assertEquals(4, candidates(original.replace("sqp=Public_meta-1&rs=Public_signature-2",
            "rs=Public_signature-2&sqp=Public_meta-1")).size)
        assertEquals(4, candidates(original.replace("i.ytimg.com", "img.youtube.com")).size)
    }
    @Test fun noCanonicalBindingOrDifferentRecordingCannotAuthorizeQueryRewrite() {
        for (identity in listOf("", "fixture:track", track.replace(id, "00000000002"),
            track + "&list=other", track + "#x", track.replace("www.youtube.com", "youtube.com"),
            "https://www.youtube.com.evil.test/watch?v=$id")) {
            assertEquals(identity, listOf(original), candidates(identity = identity))
        }
        assertEquals(listOf(original), NightWaveArtworkPolicy.requestCandidates(original, true))
        assertEquals(listOf(original), candidates(large = false))
    }
    @Test fun credentialsForeignPathsAndEncodedIdentityStayUntouched() {
        for (url in listOf(original.replace("https://", "http://"),
            original.replace("i.ytimg.com", "i.ytimg.com.evil.test"),
            original.replace("i.ytimg.com", "user:password@i.ytimg.com"),
            original.replace("i.ytimg.com", "i.ytimg.com:8443"),
            original.replace("/vi/", "/vi_webp/"), original.replace("/vi/", "/%76i/"),
            original.replace(id, "00000000002"), original.replace("hqdefault.jpg", "custom.jpg"),
            original + "#fragment", original.replace("https://i.ytimg.com", "content://local"))) {
            assertEquals(url, listOf(url), candidates(url))
        }
    }
    @Test fun arbitraryOrAmbiguousQueriesAreNeverReinterpreted() {
        for (query in listOf("", "sqp=x", "rs=x", "sqp=x&rs=y&token=z", "sqp=x&sqp=y",
            "sqp=x&rs=", "sqp=&rs=y", "sqp=x&rs=y&", "sqp=x;rs=y", "s%71p=x&rs=y",
            "sqp=x&rs=%79", "sqp=x&rs=a+b", "SQP=x&rs=y", "token=x&signature=y")) {
            val url = "$still?$query"
            assertEquals(query, listOf(url), candidates(url))
        }
    }
    @Test fun existingUnsignedAndLocalPlansRemainUnchanged() {
        for (url in listOf(still, "", "content://media/art/1", "file:///art.jpg",
            "https://art.fixture.invalid/art.jpg?token=x")) for (large in listOf(false, true)) {
            assertEquals(NightWaveArtworkPolicy.requestCandidates(url, large), candidates(url, large = large))
        }
    }

    @Test fun sharpSmallTileTriesOnlyOneSameTrackUpgradeAndRetainsExactFallback() {
        fun tile(url: String, identity: String = track) =
            NightWaveArtworkPolicy.trackCandidates(url, identity, large = false, sharperTile = true)
        assertEquals(listOf(still, original), tile(original))
        assertEquals(listOf(original), tile(original, track.replace(id, "00000000002")))
        val privateQuery = "$still?token=x&signature=y"
        assertEquals(listOf(privateQuery), tile(privateQuery))
        assertEquals(listOf("content://local/cover"), tile("content://local/cover"))
    }
}
