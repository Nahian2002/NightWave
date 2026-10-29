package com.nightwave.player.offline

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.disk.DiskCache
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** All files and preferences use a unique isolated fixture; no real index replacement. */
@OptIn(coil.annotation.ExperimentalCoilApi::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class OfflineArtworkRecoveryInstrumentedTest {
    private class FixtureContext(app: Context, val home: File, val prefix: String) : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = home
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences(prefix + name, mode)
    }
    private fun fixture(block: (FixtureContext, OfflineSong, DiskCache) -> Unit) {
        val app: Context = ApplicationProvider.getApplicationContext()
        val token = UUID.randomUUID().toString()
        val home = File(app.cacheDir, "art02-$token").apply { check(mkdir()) }
        val context = FixtureContext(app, home, "art02_${token}_")
        val cache = DiskCache.Builder().directory(File(home, "image_cache")).maxSizeBytes(4L * 1024 * 1024).build()
        val song = OfflineSong(UUID.randomUUID().toString() + ".mp3", "Original title", "audio/mpeg", 256)
        val audio = File(File(home, "offline_audio").apply { check(mkdir()) }, song.id)
        audio.writeBytes(ByteArray(256) { 7 })
        context.getSharedPreferences("nightwave_offline_v1", 0).edit().putString("items",
            JSONArray().put(JSONObject().put("id", song.id).put("title", song.title)
                .put("mime", song.mimeType).put("size", song.bytes)).toString()).commit()
        context.getSharedPreferences(OfflineTrackIdentityStore.PREFS_NAME, 0).edit().putString(song.id,
            OfflineTrackIdentityPolicy.encode(OfflineTrackIdentity("abcdefghijk", "Known song", "Known creator"))).commit()
        try { block(context, song, cache) } finally {
            cache.clear()
            listOf("nightwave_offline_v1", OfflineTrackIdentityStore.PREFS_NAME,
                "nightwave_search_offline_v1", "nightwave_queue_v1").forEach {
                app.deleteSharedPreferences(context.prefix + it)
            }
            check(home.canonicalFile.parentFile == app.cacheDir.canonicalFile && home.name == "art02-$token")
            home.deleteRecursively()
        }
    }
    private fun seed(cache: DiskCache, name: String, width: Int = 320, height: Int = 320) {
        val editor = checkNotNull(cache.openEditor("https://i.ytimg.com/vi/abcdefghijk/$name"))
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            editor.metadata.toFile().writeBytes(byteArrayOf())
            editor.data.toFile().outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            editor.commit()
        } catch (error: Throwable) { editor.abort(); throw error } finally { bitmap.recycle() }
    }
    @Test fun cachedCoverIsRecoveredWithoutChangingAudioOrIndexAndDoesNotRewriteAgain() = fixture { c, song, cache ->
        seed(cache, "hqdefault.jpg")
        val prefs = c.getSharedPreferences("nightwave_offline_v1", 0)
        val before = prefs.getString("items", null)
        val store = OfflineTrackIdentityStore(c) { cache }
        assertTrue(store.recoverKnown(listOf(song)))
        val art = store.display(song).artwork
        assertTrue(art.startsWith("file:"))
        assertTrue(File(checkNotNull(Uri.parse(art).path)).isFile)
        assertEquals(before, prefs.getString("items", null))
        assertArrayEquals(ByteArray(256) { 7 }, checkNotNull(OfflineAudioLibrary(c).fileFor(song)).readBytes())
        val revision = OfflineTrackIdentityStore.revision(c)
        assertFalse(store.recoverKnown(listOf(song)))
        assertEquals(revision, OfflineTrackIdentityStore.revision(c))
    }
    @Test fun missingOrInvalidCacheKeepsPlaceholderAndNeverTouchesAudioReadiness() = fixture { c, song, cache ->
        val store = OfflineTrackIdentityStore(c) { cache }
        assertFalse(store.recoverKnown(listOf(song)))
        val editor = checkNotNull(cache.openEditor("https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg"))
        editor.metadata.toFile().writeBytes(byteArrayOf())
        editor.data.toFile().writeText("not an image")
        editor.commit()
        assertFalse(store.recoverKnown(listOf(song)))
        assertTrue(store.display(song).artwork.isEmpty())
        assertNotNull(OfflineAudioLibrary(c).fileFor(song))
    }
    @Test fun tinyMissingLargeStillFallsBackToAnExistingCachedCover() = fixture { c, song, cache ->
        seed(cache, "maxresdefault.jpg", 120, 90)
        seed(cache, "hqdefault.jpg", 320, 320)
        val store = OfflineTrackIdentityStore(c) { cache }
        assertTrue(store.recoverKnown(listOf(song)))
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(Uri.parse(store.display(song).artwork).path, bounds)
        assertEquals(320, bounds.outWidth)
    }
    @Test fun deletingDuringStagingCannotResurrectArtworkOrMetadata() = fixture { c, song, cache ->
        seed(cache, "hqdefault.jpg")
        val store = OfflineTrackIdentityStore(c) { cache }
        var removed = false
        assertFalse(store.recoverKnown(listOf(song)) {
            if (!removed && File(c.filesDir, "offline_artwork").listFiles().orEmpty().any { it.name.endsWith(".tmp") }) {
                removed = true
                assertTrue(OfflineAudioLibrary(c).delete(song))
                store.remove(song.id)
            }
            true
        })
        assertTrue(removed)
        assertTrue(OfflineAudioLibrary(c).songs().isEmpty())
        assertFalse(c.getSharedPreferences(OfflineTrackIdentityStore.PREFS_NAME, 0).contains(song.id))
        assertTrue(File(c.filesDir, "offline_artwork").listFiles().orEmpty().isEmpty())
    }
    @Test fun metadataOnlyItemReplacementRetainsPlaybackConfiguration() {
        val before = MediaItem.Builder().setMediaId("offline:test").setUri("file:///isolated/test.mp3")
            .setMimeType("audio/mpeg").setMediaMetadata(MediaMetadata.Builder().setTitle("Original").build()).build()
        val after = before.buildUpon().setMediaMetadata(before.mediaMetadata.buildUpon()
            .setArtworkUri(Uri.parse("file:///isolated/cover.img")).setTitle("Known song").build()).build()
        assertEquals(before.mediaId, after.mediaId)
        assertEquals(before.localConfiguration, after.localConfiguration)
        assertEquals(before.clippingConfiguration, after.clippingConfiguration)
        assertEquals(before.liveConfiguration, after.liveConfiguration)
        assertEquals(before.requestMetadata, after.requestMetadata)
    }
    @Test fun optionalCacheFailureCannotPreventKnownIdentityCapture() = fixture { c, song, _ ->
        c.getSharedPreferences(OfflineTrackIdentityStore.PREFS_NAME, 0).edit().remove(song.id).commit()
        c.getSharedPreferences("nightwave_search_offline_v1", 0).edit().putString("v_abcdefghijk", song.id).commit()
        val store = OfflineTrackIdentityStore(c) { error("Unavailable optional cache") }
        assertTrue(store.capture(song, com.nightwave.player.net.VideoPreview("Known song", "Known creator",
            "https://www.youtube.com/watch?v=abcdefghijk", "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg", 0)))
        assertEquals("Known song", store.display(song).title)
        assertTrue(store.display(song).artwork.isEmpty())
        assertNotNull(OfflineAudioLibrary(c).fileFor(song))
    }
}
