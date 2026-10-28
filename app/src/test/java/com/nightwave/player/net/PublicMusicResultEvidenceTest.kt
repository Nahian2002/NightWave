package com.nightwave.player.net

import org.junit.Assert.*
import org.junit.Test

class PublicMusicResultEvidenceTest {
    private fun track(id: Int, title: String = "Play Date", duration: Long = 215) =
        VideoPreview(title, "Melanie Martinez", "https://www.youtube.com/watch?v=${id.toString().padStart(11, '0')}", "", duration)

    @Test fun durationsUseSuppliedSecondsAndMissingNeverLooksReadyOrLive() {
        assertEquals("3:35", PublicMusicResultEvidence.duration(215))
        assertEquals("1:01:01", PublicMusicResultEvidence.duration(3661))
        assertEquals("Duration unavailable", PublicMusicResultEvidence.duration(0))
        assertEquals("Duration unavailable", PublicMusicResultEvidence.duration(-1))
        assertEquals("2562047788015215:30:07", PublicMusicResultEvidence.duration(Long.MAX_VALUE))
    }

    @Test fun sourceUploaderDurationAndTitleClaimsAreSeparateFromVerification() {
        val video = track(1, "Play Date (Official Audio) (Live)")
        val evidence = PublicMusicResultEvidence.forResults(listOf(video), SearchMode.SONGS).getValue(video.url)
        assertEquals("YouTube video · 3:35", evidence.sourceAndDuration)
        assertEquals("Uploader: Melanie Martinez", evidence.uploader)
        assertEquals("Title labels: Live · Official audio", evidence.titleLabels)
        assertNull(evidence.duplicateReference)
        assertEquals("Play Date (Official Audio) (Live)", video.title)
    }

    @Test fun sameCaptionDurationAndLabelsReceiveDistinctActualIds() {
        val first = track(1, "Play Date (Official Audio)")
        val second = track(2, "Play Date (Official Audio)")
        val evidence = PublicMusicResultEvidence.forResults(listOf(first, second), SearchMode.SONGS)
        assertEquals("Video ID: 00000000001", evidence[first.url]!!.duplicateReference)
        assertEquals("Video ID: 00000000002", evidence[second.url]!!.duplicateReference)
        assertEquals(2, evidence.size)
    }

    @Test fun differentDurationAlreadyDisambiguatesWithoutExtraReferenceNoise() {
        val rows = listOf(track(1), track(2, duration = 205))
        assertTrue(PublicMusicResultEvidence.forResults(rows, SearchMode.SONGS).values.all { it.duplicateReference == null })
    }

    @Test fun labelsRequireWholeWordsAndBanglaMarkersRemainIntact() {
        assertEquals(emptyList<String>(), SearchVersionGroupingPolicy.titleLabels("Discover covering livelihood"))
        assertEquals(listOf("লাইভ", "Lyrics"), SearchVersionGroupingPolicy.titleLabels("রাতের গান লাইভ Lyrics"))
    }

    @Test fun creditedCreatorQueryGroupsAlternativesButNeverChangesObjectsOrUrls() {
        val source = track(1, "Melanie Martinez - Play Date (Official Audio)")
        val cover = track(2, "Play Date Cover")
        val grouped = SearchResultPresentationPolicy.groupMusic("Melanie Martinez Play Date", SearchMode.SONGS, listOf(cover, source))
        assertSame(source, grouped.bestMatches.first())
        assertEquals(listOf(cover), grouped.otherVersions)
        assertSame(cover, grouped.otherVersions.single())
    }

    @Test fun requestedVersionAndRepeatedTokenCoverageAreRespected() {
        assertFalse(SearchVersionGroupingPolicy.isOtherVersion("play date cover", track(1, "Play Date Cover")))
        assertFalse(SearchVersionGroupingPolicy.isOtherVersion("alo alo", track(1, "Alo Cover")))
        assertTrue(SearchVersionGroupingPolicy.isOtherVersion("alo alo", track(1, "Alo Alo Cover")))
        assertTrue(SearchVersionGroupingPolicy.isOtherVersion("ঝুম", track(1, "Jhum Live")))
    }

    @Test fun videosModeNeverSortsOrGuessesRecordingIdentity() {
        val rows = listOf(track(1, "Play Date Cover"), track(2, "Play Date (Official Audio)"))
        val result = SearchResultPresentationPolicy.groupMusic("play date", SearchMode.VIDEOS, rows)
        assertSame(rows, result.moreResults)
        assertTrue(result.bestMatches.isEmpty()); assertTrue(result.otherVersions.isEmpty())
    }
}
