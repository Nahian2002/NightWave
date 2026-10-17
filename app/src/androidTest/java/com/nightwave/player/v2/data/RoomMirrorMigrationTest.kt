package com.nightwave.player.v2.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.backup.MusicCollection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real-device/emulator Room transaction check; not run by local JVM Gradle tests. */
@RunWith(AndroidJUnit4::class)
class RoomMirrorMigrationTest {
    private fun song(id: String) = VideoPreview(
        "Song", "Creator", "https://www.youtube.com/watch?v=" + id,
        "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg", 200
    )

    @Test fun migratesAndReadsVerifiedLibrarySnapshot() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = RoomMirrorRepository(context)
        val data = MusicCollection(
            favorites = listOf(song("dQw4w9WgXcQ")),
            recent = listOf(song("J---aiyznGQ")),
            playlists = linkedMapOf(
                "Evening" to listOf(song("dQw4w9WgXcQ"), song("J---aiyznGQ")),
                "Empty" to emptyList()
            )
        )
        val status = repository.stage(data)
        assertEquals(4, status.collectionCount)
        assertEquals(4, status.trackCount)
        assertEquals(data, repository.readSnapshot())
        assertEquals(status.trackCount, repository.status()?.trackCount)
    }

    @Test fun importedLibraryCanBeRestagedWithoutDuplicateTracks() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = RoomMirrorRepository(context)
        val data = MusicCollection(listOf(song("dQw4w9WgXcQ")), emptyList(), linkedMapOf())
        repository.stage(data)
        repository.stage(data)
        assertEquals(data, repository.readSnapshot())
    }
}
