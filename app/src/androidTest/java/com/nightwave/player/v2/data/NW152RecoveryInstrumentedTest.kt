package com.nightwave.player.v2.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.net.VideoPreview
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable preference namespaces and two unique cache files; no account, picker grant or installer. */
@RunWith(AndroidJUnit4::class)
class NW152RecoveryInstrumentedTest {
    private val first = VideoPreview("Fixture one", "Fixture", "https://www.youtube.com/watch?v=abcdefghijk", "", 90L)
    private val second = first.copy(url = "https://www.youtube.com/watch?v=01234567890", title = "Fixture two")
    private val replacement = LibraryArchive("New guest", "AMOLED", "orbit", listOf(second), emptyList())

    private fun sandbox(block: (Context) -> Unit) {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val suffix = "nw152-fixture-" + UUID.randomUUID()
        val isolated = mutableSetOf<SharedPreferences>()
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                base.getSharedPreferences("$suffix-$name", mode).also { isolated.add(it) }
        }
        try { block(context) } finally { isolated.forEach { it.edit().clear().commit() } }
    }

    @Test fun staleRawLibraryOrProfileCannotWriteRecoveryOrReplaceMusic() = sandbox { context ->
        val manager = LibraryArchiveManager(context)
        val library = MusicLibrary(context)
        val profiles = GuestProfileStore(context)
        val initial = manager.guestRestoreSnapshot()
        assertTrue(library.toggleFavorite(first))
        assertEquals(CloudRestoreApplyResult.STALE_PREVIEW, manager.restoreGuestIfUnchanged(initial, replacement))
        val reviewed = manager.guestRestoreSnapshot()
        val current = profiles.load()
        assertTrue(profiles.save(current.copy(displayName = "Newer local edit")))
        assertEquals(CloudRestoreApplyResult.STALE_PREVIEW, manager.restoreGuestIfUnchanged(reviewed, replacement))
        assertEquals(listOf(first.url), library.favorites().map { it.url })
        assertTrue(context.getSharedPreferences("nightwave_archive_recovery_v1", Context.MODE_PRIVATE).all.isEmpty())
    }

    @Test fun confirmedReplacementKeepsHistoryAddedAfterPreviewAndGuestIdentity() = sandbox { context ->
        val manager = LibraryArchiveManager(context)
        val library = MusicLibrary(context)
        val profiles = GuestProfileStore(context)
        val profile = profiles.load()
        val reviewed = manager.guestRestoreSnapshot()
        library.addRecent(first)
        assertEquals(CloudRestoreApplyResult.APPLIED, manager.restoreGuestIfUnchanged(reviewed, replacement))
        assertEquals(listOf(first.url), library.recent().map { it.url })
        assertEquals(listOf(second.url), library.favorites().map { it.url })
        assertEquals(profile.localId, profiles.load().localId)
        assertEquals("New guest", profiles.load().displayName)
    }

    @Test fun conflictingShareCreatesSeparatePlaylistAndRepeatedImportIsNoOp() = sandbox { context ->
        val library = MusicLibrary(context)
        assertTrue(library.importSharedPlaylist("Fixture", listOf(first)).changed)
        assertTrue(library.importSharedPlaylist("Fixture", listOf(second)).changed)
        assertFalse(library.importSharedPlaylist("Fixture", listOf(second)).changed)
        assertEquals(listOf(first.url), library.playlist("Fixture").map { it.url })
        assertEquals(listOf(second.url), library.playlist("Fixture (Shared)").map { it.url })
    }

    @Test fun updateProviderCanReadOnlyItsNarrowCacheRoot() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.cacheDir, "approved-updates").apply { mkdirs() }
        val inside = File.createTempFile("nw152-fixture-", ".apk", root)
        val outside = File.createTempFile("nw152-fixture-", ".apk", context.cacheDir)
        try {
            val bytes = byteArrayOf(1, 2, 3)
            inside.writeBytes(bytes)
            val authority = "${context.packageName}.updates"
            val uri = FileProvider.getUriForFile(context, authority, inside)
            assertEquals("content", uri.scheme)
            assertArrayEquals(bytes, context.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
            try { FileProvider.getUriForFile(context, authority, outside); fail() }
            catch (_: IllegalArgumentException) { }
        } finally {
            inside.delete()
            outside.delete()
        }
    }
}
