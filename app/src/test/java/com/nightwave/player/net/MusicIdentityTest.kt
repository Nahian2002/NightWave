package com.nightwave.player.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Display-only, local, deterministic music identity; no provider/network. */
class MusicIdentityTest {
    private fun video(title: String, author: String, id: String) =
        VideoPreview(title, author, "https://www.youtube.com/watch?v=$id", "", 210)

    @Test fun verifiedVevoArtistGetsCleanTrackTitle() {
        val label = MusicIdentity.display(
            "Ellie Goulding - Love Me Like You Do (Official Music Video)", "EllieGouldingVEVO")
        assertEquals("Love Me Like You Do", label.title)
        assertEquals("Ellie Goulding", label.artistOrCreator)
    }

    @Test fun verifiedOfficialCreatorAndTopicChannelsRemainReadable() {
        assertEquals(MusicDisplayIdentity("Shape of You", "Ed Sheeran"),
            MusicIdentity.display("Ed Sheeran - Shape of You [Official Video]", "Ed Sheeran"))
        assertEquals(MusicDisplayIdentity("Song", "Some Artist"),
            MusicIdentity.display("Some Artist - Song (Official Audio)", "Some Artist - Topic"))
    }

    @Test fun mismatchedPublisherCannotCreateUnverifiedArtistAttribution() {
        val original = "Artist One - Song (Official Video)"
        val label = MusicIdentity.display(original, "Publisher Two")
        assertEquals("Artist One - Song", label.title)
        assertEquals("Publisher Two", label.artistOrCreator)
    }

    @Test fun meaningfulVersionsAndMiddleWordsArePreserved() {
        assertEquals("Foo - Live at Wembley (Remix)",
            MusicIdentity.display("Foo - Live at Wembley (Remix)", "Other channel").title)
        assertEquals("The Official Story of Love",
            MusicIdentity.display("The Official Story of Love", "Creator").title)
        assertEquals("Song (Acoustic)",
            MusicIdentity.display("Song (Acoustic)", "Creator").title)
    }

    @Test fun explicitTrailingLabelsAreRemovedWithoutCorruptingBlankNames() {
        assertEquals("Hello", MusicIdentity.display(" Hello [Lyrics] ", "Channel").title)
        assertEquals("Hello", MusicIdentity.display("Hello - Official Audio", "Channel").title)
        assertEquals("Hello", MusicIdentity.display(
            "Hello (Lyric Video) [Official Video]", "Channel").title)
        assertEquals("Untitled video", MusicIdentity.display("   ", "  ").title)
        assertEquals("Unknown creator", MusicIdentity.display("", "").artistOrCreator)
    }

    @Test fun onlyDistinctSavedUrlsCountTowardCreatorShortcut() {
        val ellie = video("Ellie Goulding - Track One", "EllieGouldingVEVO", "abc12345678")
        val other = video("Ellie Goulding - Track Two", "EllieGouldingVEVO", "abc12345679")
        val results = MusicIdentity.creatorsFrom(listOf(ellie, ellie, other))
        assertEquals(listOf(SavedCreatorShortcut("Ellie Goulding", 2)), results)
    }

    @Test fun genericCreatorIsNeverPresentedAsARealArtist() {
        val generic = video("A Random Tune", "YouTube creator", "abc12345678")
        assertTrue(MusicIdentity.creatorsFrom(listOf(generic)).isEmpty())
    }

    @Test fun artistSuggestionsAreBoundedAndRanked() {
        val songs = (0 until 22).map {
            video("Track $it", "Creator $it", "abc" + it.toString().padStart(8, '0'))
        }
        assertEquals(8, MusicIdentity.creatorsFrom(songs).size)
        assertTrue(MusicIdentity.creatorsFrom(songs, 0).isEmpty())
        assertEquals(12, MusicIdentity.creatorsFrom(songs, 99).size)
    }

    @Test fun metadataFormattingDoesNotMutateTheActualPlayableVideo() {
        val input = video("Ed Sheeran - Shape of You (Official Video)",
            "Ed Sheeran", "abc12345678")
        val display = MusicIdentity.display(input.title, input.author)
        assertEquals("Shape of You", display.title)
        assertEquals("Ed Sheeran - Shape of You (Official Video)", input.title)
        assertEquals("https://www.youtube.com/watch?v=abc12345678", input.url)
    }

    @Test fun searchMatchesDisplayArtistWithSpacingEvenWhenUploaderHasNone() {
        val song = video("Ellie Goulding - Love Me Like You Do (Official Music Video)",
            "EllieGouldingVEVO", "abc12345678")
        assertEquals(listOf(song), SavedTrackSearch.match("ellie goulding", listOf(song)))
        assertEquals(listOf(song), SavedTrackSearch.match("love me like you do", listOf(song)))
        assertFalse(SavedTrackSearch.match("different artist", listOf(song)).isNotEmpty())
    }

    @Test fun nw81RemovesOnlyExplicitTrailingVideoPackaging() {
        assertEquals("Yellow", MusicIdentity.display(
            "Coldplay - Yellow [Official Video]", "Other uploader").title
            .substringAfter(" - "))
        assertEquals("Song", MusicIdentity.display(
            "Song [Official Audio] (Official Lyric Video) [Official Visualizer]",
            "Original channel").title)
        assertEquals("Song", MusicIdentity.display(
            "Song - Official Visualiser", "Original channel").title)
        assertEquals("Song", MusicIdentity.display(
            "Song | [Official Video]", "Original channel").title)
        assertEquals("Song", MusicIdentity.display(
            "Song - [Official Video]", "Original channel").title)
    }

    @Test fun nw81NeverErasesMeaningfulRecordingOrFeaturedArtistNames() {
        for (original in listOf(
            "Song (Live)", "Song (Acoustic)", "Song (Remix)",
            "Song (Cover)", "Song feat. Singer",
            "Song (Radio Edit)", "Song (Live at Wembley)",
            "The Official Story", "Official Video Game Soundtrack",
            "Song [Instrumental]", "Song (Karaoke)"
        )) assertEquals(original, MusicIdentity.display(original, "Uploader").title)
    }

    @Test fun nw81BanglaVowelsMustNotFalselyCorroborateArtist() {
        assertEquals(MusicDisplayIdentity("গান", "মালা"),
            MusicIdentity.display("মালা - গান (Official Audio)", "মালা - Topic"))
        assertEquals(MusicDisplayIdentity("মালা - গান", "মেলা - Topic"),
            MusicIdentity.display("মালা - গান (Official Audio)", "মেলা - Topic"))
    }

    @Test fun nw81VideoModeNeverPretendsProviderCaptionIsMusicMetadata() {
        val raw = "Ed Sheeran - Shape of You [Official Video]"
        assertEquals(MusicDisplayIdentity(raw, "Ed Sheeran"),
            MusicIdentity.videoDisplay(raw, "Ed Sheeran"))
        assertEquals("Shape of You", MusicIdentity.display(raw, "Ed Sheeran").title)
        assertEquals("Untitled video", MusicIdentity.videoDisplay("", "").title)
        assertEquals("Unknown creator", MusicIdentity.videoDisplay("", "").artistOrCreator)
    }

    @Test fun nw81PresentationNeverMutatesSongMetadataOrIdentity() {
        val original = video(
            "Ed Sheeran - Shape of You (Official Lyric Video)",
            "Ed Sheeran - Topic", "abcdefghijk")
        val shown = MusicIdentity.display(original.title, original.author)
        assertEquals("Shape of You", shown.title)
        assertEquals("Ed Sheeran", shown.artistOrCreator)
        assertEquals("Ed Sheeran - Shape of You (Official Lyric Video)", original.title)
        assertEquals("Ed Sheeran - Topic", original.author)
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", original.url)
    }
}
