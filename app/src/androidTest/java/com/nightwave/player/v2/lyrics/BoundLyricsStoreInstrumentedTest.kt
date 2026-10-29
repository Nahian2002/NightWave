package com.nightwave.player.v2.lyrics

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Unique files/prefs for each test; never replace the installed app's real library. */
@RunWith(AndroidJUnit4::class)
class BoundLyricsStoreInstrumentedTest {
    private class IsolatedContext(app: Context, val root: File, val prefix: String) : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = super.getSharedPreferences(prefix + name, mode)
    }
    private fun fixture(block: (IsolatedContext, PrivateLyricsStore, BoundLyricsStore) -> Unit) {
        val app: Context = ApplicationProvider.getApplicationContext()
        val token = UUID.randomUUID().toString(); val root = File(app.cacheDir, "nw137-$token").apply { check(mkdir()) }
        val c = IsolatedContext(app, root, "nw137_${token}_")
        val local = PrivateLyricsStore(c); val store = BoundLyricsStore(c, local, PrivateLyricsOffsetStore(c))
        try { block(c, local, store) } finally {
            listOf("nightwave_offline_v1", "nightwave_private_lrc_v1", "nightwave_private_lrc_offset_v1", BoundLyricsStore.PREFS_NAME)
                .forEach { app.deleteSharedPreferences(c.prefix + it) }
            check(root.canonicalFile.parentFile == app.cacheDir.canonicalFile && root.name == "nw137-$token")
            root.deleteRecursively()
        }
    }
    private val publicKey = "youtube:abcdefghijk"
    private val text = "[00:01.00] বাংলা\n[00:02.00] Next".toByteArray()
    @Test fun publicLyricsAndOffsetsNeverEnterTheLegacyCloudExport() = fixture { c, local, store ->
        assertTrue(store.saveReviewed(publicKey, text, null))
        assertTrue(store.saveOffset(publicKey, 250))
        assertEquals(250, store.readOffset(publicKey))
        assertEquals(2, store.read(publicKey)!!.cues.size)
        assertTrue(local.exportCloud().isEmpty())
        assertFalse(c.getSharedPreferences("nightwave_private_lrc_v1", 0).contains("lrc:$publicKey"))
    }
    @Test fun twoFacadeInstancesRejectAStaleReplacementFingerprint() = fixture { c, local, store ->
        store.saveReviewed(publicKey, text, null)
        val old = store.fingerprint(publicKey)
        val other = BoundLyricsStore(c, local, PrivateLyricsOffsetStore(c))
        assertTrue(other.saveReviewed(publicKey, "[00:03.00] Changed".toByteArray(), old))
        assertFalse(store.saveReviewed(publicKey, text, old))
        assertEquals("Changed", store.read(publicKey)!!.cues.single().text)
    }
    @Test fun inactiveReviewAndOffsetNeverWrite() = fixture { c, _, store ->
        assertFalse(store.saveReviewed(publicKey, text, null) { false })
        assertNull(store.read(publicKey))
        assertTrue(store.saveReviewed(publicKey, text, null))
        assertFalse(store.saveOffset(publicKey, 500) { false })
        assertEquals(0, store.readOffset(publicKey))
        assertTrue(c.getSharedPreferences(BoundLyricsStore.PREFS_NAME, 0).contains("lrc:$publicKey"))
    }
    @Test fun localIdentityAndCloudRepresentationRemainUnchangedAndDeletionRejectsCommit() = fixture { c, local, store ->
        val id = UUID.randomUUID().toString() + ".mp3"
        val index = c.getSharedPreferences("nightwave_offline_v1", 0)
        val raw = JSONArray().put(JSONObject().put("id", id).put("title", "Local")
            .put("mime", "audio/mpeg").put("size", 256)).toString()
        index.edit().putString("items", raw).commit()
        assertTrue(store.saveReviewed(id, text, null))
        assertEquals(local.read(id)?.cues, store.read(id)?.cues)
        assertEquals(listOf(id), local.exportCloud().map { it.localSongId })
        val fingerprint = store.fingerprint(id)
        index.edit().putString("items", "[]").commit()
        try { store.saveReviewed(id, text, fingerprint); fail("Removed ID accepted") } catch (_: IllegalArgumentException) { }
        assertFalse(c.getSharedPreferences(BoundLyricsStore.PREFS_NAME, 0).contains("lrc:$id"))
    }
    @Test fun invalidPublicImportCannotReplaceExistingWordsAndDeleteClearsOnlyThatSource() = fixture { _, _, store ->
        store.saveReviewed(publicKey, text, null)
        val fp = store.fingerprint(publicKey)
        try { store.saveReviewed(publicKey, byteArrayOf(0xc3.toByte(), 0x28), fp); fail("Invalid UTF8 accepted") }
        catch (_: java.nio.charset.CharacterCodingException) { }
        val second = "youtube:12345678901"; store.saveReviewed(second, text, null)
        assertEquals(fp, store.fingerprint(publicKey))
        assertTrue(store.delete(publicKey))
        assertNull(store.read(publicKey)); assertNotNull(store.read(second))
    }
}
