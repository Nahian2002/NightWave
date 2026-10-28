package com.nightwave.player.net

import com.nightwave.player.offline.OfflineSong
import com.nightwave.player.v2.data.room.LegacyLibrarySnapshot
import com.nightwave.player.v2.data.room.LegacyLibrarySnapshotParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException
import java.security.MessageDigest

/** Shared synthetic metadata fixture. No real audio, accounts or device seeding. */
object NW129SearchFixture {
    fun track(i: Int, title: String = "Music $i", creator: String = "Creator ${i % 40}") =
        VideoPreview(title, creator, "https://www.youtube.com/watch?v=${i.toString().padStart(11, '0')}", "", 210)
    fun snapshot(count: Int = 10_000): LocalMusicSnapshot {
        val tracks = (0 until count).map { track(it) }
        return LocalMusicSnapshot(LegacyLibrarySnapshot(tracks.take(500), tracks.takeLast(100),
            tracks.chunked(300).mapIndexed { i, rows -> "Collection ${i + 1}" to rows }, "fixture"),
            List(5_000) { OfflineSong(it.toString(16).padStart(36, '0') + ".mp3", "Offline seeker $it", "audio/mpeg", 100) })
    }
    fun strings(snapshot: LocalMusicSnapshot): Map<String, String> {
        fun rows(tracks: List<VideoPreview>) = JSONArray().apply { tracks.forEach {
            put(JSONObject().put("url", it.url).put("title", it.title).put("creator", it.author)
                .put("artwork", it.thumbnail).put("duration", it.durationSeconds))
        } }.toString()
        val lib = snapshot.publicLibrary
        return buildMap {
            put("favorites", rows(lib.favorites)); put("recent", rows(lib.recent))
            put("playlist_names", JSONArray(lib.playlists.map { it.first }).toString())
            lib.playlists.forEach { (name, tracks) -> put("list_$name", rows(tracks)) }
        }
    }
}

class LocalMusicSearchIndexTest {
    companion object {
        val fixture by lazy { NW129SearchFixture.snapshot() }
        val index by lazy { LocalMusicSearchIndex.build(fixture) }
    }

    @Test fun completeParsedSnapshotFindsPlaylist26AndTheTenThousandthItem() {
        val parsed = LegacyLibrarySnapshotParser.parse(NW129SearchFixture.strings(fixture))
        val full = LocalMusicSearchIndex.build(fixture.copy(publicLibrary = parsed))
        assertEquals(34, parsed.playlists.size)
        assertEquals(10_000, full.publicCount)
        for (i in listOf(350, 450, 7_799, 9_999)) {
            assertEquals(listOf(NW129SearchFixture.track(i)), full.search("Music $i").saved)
        }
        assertTrue(full.origin(NW129SearchFixture.track(7_799).url)!!.playlists.contains("Collection 26"))
    }

    @Test fun streamedSourceDigestMatchesTheOriginalUtf8Contract() {
        val maps = listOf(emptyMap(), NW129SearchFixture.strings(fixture),
            NW129SearchFixture.strings(LocalMusicSnapshot(LegacyLibrarySnapshot(
                listOf(NW129SearchFixture.track(1, "মালা – Café")), emptyList(),
                listOf("বাংলা" to listOf(NW129SearchFixture.track(2))), ""), emptyList())))
        for (source in maps) {
            val parsed = LegacyLibrarySnapshotParser.parse(source)
            val keys = (listOf("favorites", "recent", "playlist_names") + parsed.playlists.map { "list_${it.first}" }).sorted()
            val previous = keys.joinToString("\u0000") { key ->
                val raw = source[key]
                key.length.toString() + ":" + key + ":" + (raw?.length ?: -1) + ":" + (raw ?: "")
            }
            val expected = MessageDigest.getInstance("SHA-256").digest(previous.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            assertEquals(expected, parsed.digest)
        }
    }

    @Test fun completeOfflineMembershipFindsBeyond400AndAtItsStorageBoundary() {
        for (i in listOf(400, 4_999)) {
            assertEquals(listOf(fixture.offline[i]), index.search("Offline seeker $i").offline)
        }
        assertEquals(5_000, index.search("Offline seeker").offline.size)
    }

    @Test fun broadResultsRetainEveryMemberWhileCompatibilityPreviewStays24() {
        val all = index.search("Music")
        assertEquals(10_000, all.saved.size)
        val tracks = fixture.publicLibrary.playlists.flatMap { it.second }
        assertEquals(all.saved.take(24), SavedTrackSearch.match("Music", tracks))
        assertEquals(24, SavedTrackSearch.match("Music", tracks, 100).size)
        assertTrue(all.saved.contains(NW129SearchFixture.track(9_999)))
    }

    @Test fun laterBestDuplicateMetadataAndOriginalUrlSurviveActualIndexAssembly() {
        val weak = NW129SearchFixture.track(1, "Moonlight extended", "Unknown creator")
        val exact = weak.copy(title = "Moonlight", author = "Original creator")
        val raw = LocalMusicSnapshot(LegacyLibrarySnapshot(listOf(weak), emptyList(),
            List(26) { "List $it" to if (it == 25) listOf(exact) else emptyList() }, "fixture"), emptyList())
        val built = LocalMusicSearchIndex.build(raw)
        assertSame(exact, built.search("Moonlight").saved.single())
        assertEquals(weak.url, built.search("Moonlight").saved.single().url)
        assertEquals("Liked · In a playlist", built.origin(weak.url)!!.label())
        assertEquals(2, built.referenceCount)
    }

    @Test fun historyMembershipCannotBecomeALikeOrPlaylist() {
        val track = NW129SearchFixture.track(3)
        val built = LocalMusicSearchIndex.build(LocalMusicSnapshot(
            LegacyLibrarySnapshot(emptyList(), listOf(track), emptyList(), "fixture"), emptyList()))
        assertEquals(LocalMusicOrigin(false, true, emptySet()), built.origin(track.url))
        assertEquals("Recent", built.origin(track.url)!!.label())
    }

    @Test fun editedAndRemovedMetadataCannotLeakThroughANewIndex() {
        val old = NW129SearchFixture.track(1, "Moonlight")
        fun build(tracks: List<VideoPreview>) = LocalMusicSearchIndex.build(LocalMusicSnapshot(
            LegacyLibrarySnapshot(tracks, emptyList(), emptyList(), "fixture"), emptyList()))
        val before = build(listOf(old)); val after = build(listOf(old.copy(title = "ভালোবাসা")))
        val result = before.search("Moonlight")
        assertFalse(result.accepts("Moonlight", after))
        assertFalse(result.accepts("ভালোবাসা", before))
        assertTrue(after.search("Moonlight").saved.isEmpty())
        assertEquals(old.url, after.search("valobasha").saved.single().url)
        assertTrue(build(emptyList()).search("Moonlight").saved.isEmpty())
    }

    @Test fun preparedIndexPreservesBanglaVowelsRepetitionAliasesAndTypoFallback() {
        val tracks = listOf(NW129SearchFixture.track(1, "মালা"), NW129SearchFixture.track(2, "মেলা"),
            NW129SearchFixture.track(3, "Alo"), NW129SearchFixture.track(4, "Alo Alo"),
            NW129SearchFixture.track(5, "ভালোবাসা"), NW129SearchFixture.track(6, "Midnight"))
        val built = LocalMusicSearchIndex.build(LocalMusicSnapshot(
            LegacyLibrarySnapshot(tracks, emptyList(), emptyList(), "fixture"), emptyList()))
        for (query in listOf("মালা", "মেলা", "alo alo", "valobasha", "midnigth")) {
            assertEquals(SavedTrackSearch.match(query, tracks), built.search(query).saved.take(24))
        }
        assertEquals(listOf(tracks[3]), built.search("alo alo").saved)
    }

    @Test fun fullCreatorSuggestionsAndGroupsKeepLateMembershipAndVowelIdentity() {
        val rows = (0 until 5_001).map { NW129SearchFixture.track(it) } +
            NW129SearchFixture.track(8_000, "Evening", "Late Creator")
        val lib = LegacyLibrarySnapshot(emptyList(), emptyList(), rows.chunked(300).mapIndexed { i, r -> "List$i" to r }, "fixture")
        val built = LocalMusicSearchIndex.build(LocalMusicSnapshot(lib, emptyList()))
        assertTrue(built.search("Late Creator").suggestions.any { it.kind == LocalSearchSuggestion.Kind.CREATOR })
        val vowels = listOf(NW129SearchFixture.track(1, "One", "মালা"), NW129SearchFixture.track(2, "Two", "মেলা"))
        assertEquals(2, SavedCreatorBrowser.groups(vowels, scanLimit = 15_600).size)
    }

    @Test fun unsupportedCorpusFailsExplicitlyRatherThanSilentlyTruncatingIt() {
        val oversized = fixture.publicLibrary.copy(favorites = List(501) { NW129SearchFixture.track(it) })
        assertThrows(IllegalArgumentException::class.java) { LocalMusicSearchIndex.build(fixture.copy(publicLibrary = oversized)) }
        assertThrows(IllegalArgumentException::class.java) { LocalMusicSearchIndex.build(fixture.copy(offline = fixture.offline + fixture.offline[0])) }
    }

    @Test fun cooperativeCheckpointsCanCancelPreparationAndAnInFlightQuery() {
        var calls = 0
        assertThrows(CancellationException::class.java) {
            LocalMusicSearchIndex.build(fixture) { if (++calls == 2) throw CancellationException() }
        }
        calls = 0
        assertThrows(CancellationException::class.java) {
            index.search("Music") { if (++calls == 2) throw CancellationException() }
        }
    }
}
