package com.nightwave.player.offline

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import coil.imageLoader
import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.playback.QueueStateStore
import com.nightwave.player.v2.ui.NightWaveArtworkPolicy
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal data class OfflineDisplayIdentity(val title: String, val creator: String, val artwork: String)

/** Best-effort, bounded identity sidecar. Artwork is copied only from Coil's
 * existing disk cache: this class performs no HTTP or audio/library mutation.
 */
internal class OfflineTrackIdentityStore(private val context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nightwave_offline_identity_v1", Context.MODE_PRIVATE)
    private val metadata by lazy { prefs.all }
    private val sourceLinks by lazy {
        context.applicationContext.getSharedPreferences("nightwave_search_offline_v1", Context.MODE_PRIVATE)
            .all.entries.take(OfflineAudioLibrary.MAX_SONGS).mapNotNull { (key, value) ->
                if (value is String && Regex("v_[A-Za-z0-9_-]{11}").matches(key))
                    value to "https://www.youtube.com/watch?v=${key.removePrefix("v_")}" else null
            }.toMap()
    }
    private val publicQueue by lazy { runCatching { QueueStateStore(context).read().tracks.associateBy { it.url } }
        .getOrDefault(emptyMap()) }
    private val artworkHome = File(context.applicationContext.filesDir.canonicalFile, "offline_artwork")
    private fun validId(id: String) = Regex("[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)").matches(id)
    private fun artFile(id: String): File? {
        if (!validId(id) || Files.isSymbolicLink(artworkHome.toPath()) ||
            artworkHome.canonicalFile != artworkHome.absoluteFile) return null
        val result = File(artworkHome, AudioRangePolicy.key(id) + ".img")
        return result.takeIf { !Files.isSymbolicLink(it.toPath()) && it.canonicalFile.parentFile == artworkHome.canonicalFile }
    }

    fun display(song: OfflineSong): OfflineDisplayIdentity {
        val saved = OfflineTrackIdentityPolicy.decode(metadata[song.id] as? String)
        val known = saved ?: sourceLinks[song.id]?.let { publicQueue[it] }?.let {
            OfflineTrackIdentityPolicy.from(it.url, it.title, it.creator)
        }
        val display = MusicIdentity.display(known?.title ?: song.title, known?.creator.orEmpty())
        val art = artFile(song.id)?.takeIf { it.isFile && it.length() in 1..MAX_ART_BYTES }
        return OfflineDisplayIdentity(display.title,
            if (known?.creator.isNullOrBlank()) "Saved audio" else display.artistOrCreator,
            art?.let { Uri.fromFile(it).toString() }.orEmpty())
    }

    /** Called on the save worker after validated audio import. Optional art
     * failure never invalidates a song or silently fetches an image online.
     */
    fun capture(song: OfflineSong, source: VideoPreview): Boolean = runCatching {
        require(validId(song.id))
        val identity = OfflineTrackIdentityPolicy.from(source.url, source.title, source.author) ?: return false
        if (!prefs.contains(song.id) && prefs.all.size >= OfflineAudioLibrary.MAX_SONGS) return false
        val committed = prefs.edit().putString(song.id, OfflineTrackIdentityPolicy.encode(identity)).commit()
        if (committed) runCatching { copyCachedArtwork(song.id, source.thumbnail) }
        committed
    }.getOrDefault(false)

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    private fun copyCachedArtwork(id: String, original: String) = synchronized(artworkLock) {
        val destination = artFile(id) ?: return
        if (destination.isFile) return
        require(artworkHome.isDirectory || artworkHome.mkdirs())
        val entries = artworkHome.listFiles().orEmpty().filter { it.isFile && !Files.isSymbolicLink(it.toPath()) }
        if (entries.sumOf { it.length() } + MAX_ART_BYTES > 64L * 1024 * 1024) return
        val cache = context.imageLoader.diskCache ?: return
        val candidates = (NightWaveArtworkPolicy.requestCandidates(original, true) +
            NightWaveArtworkPolicy.requestCandidates(original, false)).distinct()
        for (candidate in candidates) {
            val snapshot = cache.openSnapshot(candidate) ?: continue
            snapshot.use {
                val file = snapshot.data.toFile()
                if (!file.isFile || file.length() !in 1..MAX_ART_BYTES) return@use
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth !in 1..4096 || bounds.outHeight !in 1..4096 ||
                    bounds.outMimeType !in setOf("image/jpeg", "image/png", "image/webp")) return@use
                val temp = File(artworkHome, destination.name + ".tmp")
                require(!Files.isSymbolicLink(temp.toPath()))
                try {
                    file.inputStream().use { input -> FileOutputStream(temp).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var count = 0L
                        while (true) {
                            val n = input.read(buffer); if (n < 0) break
                            count += n
                            require(count <= MAX_ART_BYTES)
                            output.write(buffer, 0, n)
                        }
                        output.flush(); output.fd.sync()
                    } }
                    Files.move(temp.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
                } finally { temp.delete() }
            }
            if (destination.isFile) return
        }
    }
    fun remove(id: String) {
        if (!validId(id)) return
        prefs.edit().remove(id).apply()
        artFile(id)?.let { if (it.isFile) it.delete() }
    }
    companion object {
        private const val MAX_ART_BYTES = 1024L * 1024L
        private val artworkLock = Any()
    }
}
