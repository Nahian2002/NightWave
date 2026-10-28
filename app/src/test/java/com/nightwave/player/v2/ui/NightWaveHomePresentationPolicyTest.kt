package com.nightwave.player.v2.ui

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.recommendation.PersonalMix
import com.nightwave.player.v2.recommendation.NightWaveHomeCurationPolicy
import org.junit.Assert.*
import org.junit.Test

class NightWaveHomePresentationPolicyTest {
    private fun song(n: Int, author: String = "Uploader $n") = VideoPreview(
        "Song $n", author, "https://www.youtube.com/watch?v=" + n.toString().padStart(11, '0'), "", 60L)
    private fun mix(key: String, tracks: List<VideoPreview>) = PersonalMix(key, "Original title", "", "", tracks)
    private fun entry(liked: List<VideoPreview> = emptyList(), recent: List<VideoPreview> = emptyList(),
        playlists: List<VideoPreview> = emptyList()) = NightWaveHomePresentationPolicy.entry(liked, recent, playlists)

    @Test fun newListenerHasOneSearchPrimary() {
        val state = entry()
        assertEquals(NightWaveHomePresentationPolicy.Kind.NEW, state.kind)
        assertEquals(NightWaveHomeLayoutPolicy.EmptyRecentDestination.SEARCH, state.primary!!.destination)
        assertFalse(state.showSearchShortcut)
        assertFalse(state.showLikedShortcut)
    }
    @Test fun historyIsNotSavedMembershipAndContinueRemainsPrimary() {
        val state = entry(recent = listOf(song(1)))
        assertEquals(NightWaveHomePresentationPolicy.Kind.HISTORY_ONLY, state.kind)
        assertNull(state.primary)
        assertFalse(state.showLikedShortcut)
        assertTrue(state.showSearchShortcut)
    }
    @Test fun likedOnlyOpensLikesWithoutDuplicatingShortcut() {
        val state = entry(liked = listOf(song(1)))
        assertEquals(NightWaveHomePresentationPolicy.Kind.SAVED_ONLY, state.kind)
        assertEquals(NightWaveHomeLayoutPolicy.EmptyRecentDestination.LIKED_SONGS, state.primary!!.destination)
        assertFalse(state.showLikedShortcut)
        assertTrue(state.showSearchShortcut)
    }
    @Test fun playlistOnlyOpensLibrary() {
        assertEquals(NightWaveHomeLayoutPolicy.EmptyRecentDestination.LIBRARY,
            entry(playlists = listOf(song(1))).primary!!.destination)
    }
    @Test fun matureLibraryKeepsContinueAndSeparateLikedDestination() {
        val state = entry(liked = listOf(song(1)), recent = listOf(song(2)), playlists = listOf(song(3)))
        assertEquals(NightWaveHomePresentationPolicy.Kind.MIXED, state.kind)
        assertNull(state.primary)
        assertTrue(state.showLikedShortcut)
    }
    @Test fun invalidReferencesCannotInventSavedOrHistoryState() {
        val invalid = song(1).copy(url = "https://invalid.invalid/song")
        assertEquals(NightWaveHomePresentationPolicy.Kind.NEW, entry(listOf(invalid), listOf(invalid), listOf(invalid)).kind)
    }
    @Test fun mixExplanationCountsActualUniqueMembershipWithoutRewritingSources() {
        val a = song(1); val b = song(2); val c = song(3)
        val tracks = mutableListOf(a, b, c, a)
        val before = tracks.toList()
        val presentation = NightWaveHomePresentationPolicy.mix(mix("daily_mix_1", tracks),
            listOf(a, a), listOf(a, b), listOf(c), false)
        assertEquals("Familiar picks", presentation.title)
        assertEquals("YOUR MUSIC", presentation.caption)
        assertEquals("On this device · 1 liked · 1 in playlists · 2 recent", presentation.reason)
        assertEquals("Starts with Song 1", presentation.startsWith)
        assertEquals(before, tracks)
        assertSame(a, tracks.first())
    }
    @Test fun historyOnlyMixNeverClaimsSavedReferences() {
        val a = song(1)
        val presentation = NightWaveHomePresentationPolicy.mix(mix("daily_mix_1", listOf(a)), emptyList(), listOf(a), emptyList(), false)
        assertEquals("Recent rotation", presentation.title)
        assertEquals("RECENT MUSIC", presentation.caption)
        assertFalse(presentation.reason.contains("memory", true))
    }
    @Test fun olderHistoryStillCountsAsRecentEvidenceBeyondHomeWindow() {
        val history = (1..150).map(::song)
        val selected = history.last()
        val presentation = NightWaveHomePresentationPolicy.mix(mix("daily_mix_2", listOf(selected)),
            emptyList(), history, emptyList(), false)
        assertEquals("More recent plays", presentation.title)
        assertEquals("On this device · 1 recent", presentation.reason)
        assertEquals("RECENT MUSIC", presentation.caption)
    }
    @Test fun outsideRecentAndUploaderLabelsRequireActualEvidence() {
        val a = song(1, "Unknown artist"); val b = song(2, "Unknown creator")
        fun view(key: String, tracks: List<VideoPreview>, recent: List<VideoPreview> = emptyList()) =
            NightWaveHomePresentationPolicy.mix(mix(key, tracks), tracks, recent, emptyList(), false)
        assertEquals("More recent plays", view("daily_mix_2", listOf(a), listOf(a)).title)
        assertEquals("Beyond recent plays", view("daily_mix_2", listOf(a, b), listOf(a)).title)
        assertEquals("Another way to listen", view("daily_mix_3", listOf(a, b)).title)
        assertEquals("Uploader variety", view("daily_mix_3", listOf(song(1), song(2))).title)
    }
    @Test fun memoryConsentLabelDoesNotInventListeningStatsOrGenres() {
        val tracks = listOf(song(1))
        val off = NightWaveHomePresentationPolicy.mix(mix("memory", tracks), tracks, emptyList(), emptyList(), false)
        val on = NightWaveHomePresentationPolicy.mix(mix("memory", tracks), tracks, emptyList(), emptyList(), true)
        assertEquals(off.copy(reason = off.reason + " · Listening memory enabled"), on)
        assertEquals("SAVED MUSIC", on.caption)
        assertFalse(on.reason.contains("plays"))
    }
    @Test fun smallMixRepeatingQuickShelfIsSuppressedButKeyRemainsAvailable() {
        val liked = (1..6).map(::song)
        val repeated = mix("same", liked)
        val distinct = mix("same", (7..12).map(::song))
        val plan = NightWaveHomeCurationPolicy.curate(liked, emptyList(), emptyList(), listOf(repeated, distinct), emptyList(), 1L)
        assertEquals(liked.toSet(), plan.nextListens.toSet())
        assertEquals(listOf(distinct), plan.mixes)
    }
    @Test fun largerMixRetainsMeaningfulAdditionalSongs() {
        val liked = (1..12).map(::song)
        val larger = mix("daily_mix_1", liked)
        val plan = NightWaveHomeCurationPolicy.curate(liked, emptyList(), emptyList(), listOf(larger), emptyList(), 1L)
        assertEquals(6, plan.nextListens.size)
        assertEquals(listOf(larger), plan.mixes)
        assertSame(larger.tracks, plan.mixes.single().tracks)
    }
    @Test fun realMixSourcesHaveDifferentPurposeDescriptions() {
        val recent = listOf(song(1), song(2))
        val first = NightWaveHomePresentationPolicy.mix(mix("daily_mix_1", recent), emptyList(), recent, emptyList(), false)
        val next = NightWaveHomePresentationPolicy.mix(mix("daily_mix_2", recent.reversed()), emptyList(), recent, emptyList(), false)
        assertNotEquals(first.purpose, next.purpose)
        assertTrue(first.reason.contains("2 recent"))
        assertTrue(next.reason.contains("2 recent"))
    }
    @Test fun beyondRecentPurposeRequiresAnActualOutsideTrack() {
        val p = NightWaveHomePresentationPolicy.mix(mix("daily_mix_2", listOf(song(1), song(2))),
            listOf(song(2)), listOf(song(1)), emptyList(), false)
        assertEquals("Beyond your recent plays", p.purpose)
        assertTrue(p.reason.contains("1 liked"))
    }

}
