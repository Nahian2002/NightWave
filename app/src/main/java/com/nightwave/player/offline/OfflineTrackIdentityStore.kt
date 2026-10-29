package com.nightwave.player.offline

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
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
internal class OfflineTrackIdentityStore(private val context: Context,
    private val cachedImages: () -> coil.disk.DiskCache? = { context.imageLoader.diskCache }) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val linkPrefs = context.applicationContext.getSharedPreferences("nightwave_search_offline_v1", Context.MODE_PRIVATE)
    private val sourceLinks by lazy {
        OfflineArtworkRecoveryPolicy.uniqueLinks(linkPrefs.all.entries.mapNotNull { (key, value) ->
            (value as? String)?.let { key to it }
        })
    }
    private val publicQueue by lazy { runCatching { QueueStateStore(context).read().tracks.associateBy { it.url } }
        .getOrDefault(emptyMap()) }
    private val artworkHome = File(context.applicationContext.filesDir.canonicalFile, "offline_artwork")
    private fun record(id: String): String? = runCatching { prefs.getString(id, null) }.getOrNull()
    private fun validId(id: String) = Regex("[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)").matches(id)
    private fun artFile(id: String): File? {
        if (!validId(id) || Files.isSymbolicLink(artworkHome.toPath()) ||
            artworkHome.canonicalFile != artworkHome.absoluteFile) return null
        val result = File(artworkHome, AudioRangePolicy.key(id) + ".img")
        return result.takeIf { !Files.isSymbolicLink(it.toPath()) && it.canonicalFile.parentFile == artworkHome.canonicalFile }
    }

    fun display(song: OfflineSong): OfflineDisplayIdentity {
        val saved = OfflineTrackIdentityPolicy.decode(record(song.id))
        val known = saved ?: sourceLinks[song.id]?.let { publicQueue[it] }?.let {
            OfflineTrackIdentityPolicy.from(it.url, it.title, it.creator)
        }
        val display = MusicIdentity.display(known?.title ?: song.title, known?.creator.orEmpty())
        val art = artFile(song.id)?.takeIf { it.isFile && it.length() in 1..MAX_ART_BYTES }
        return OfflineDisplayIdentity(display.title,
            if (known?.creator.isNullOrBlank()) "Saved audio" else display.artistOrCreator,
            art?.let { Uri.fromFile(it).toString() }.orEmpty())
    }

    /** Save-worker capture remains optional and uses only already-cached art. */
    fun capture(song: OfflineSong, source: VideoPreview): Boolean = runCatching {
        val identity = OfflineTrackIdentityPolicy.from(source.url, source.title, source.author) ?: return false
        publishKnown(song, identity, source.thumbnail, record(song.id), requiresLink = true) { true }
    }.getOrDefault(false)

    /** Visible/selected candidates only; caller owns a bounded background worker.
     * Missing cache or identity leaves the original song completely untouched.
     */
    fun recoverKnown(songs: List<OfflineSong>, active: () -> Boolean = { true }): Boolean {
        val started = SystemClock.elapsedRealtime()
        val mayContinue = { active() && !Thread.currentThread().isInterrupted &&
            SystemClock.elapsedRealtime() - started < OfflineArtworkRecoveryPolicy.BUDGET_MS }
        var changed = false
        for (song in OfflineArtworkRecoveryPolicy.batch(songs, emptySet())) {
            if (!mayContinue()) break
            runCatching {
                val raw = record(song.id)
                val saved = OfflineTrackIdentityPolicy.decode(raw)
                val linked = sourceLinks[song.id]?.let { publicQueue[it] }
                val known = saved ?: linked?.let { OfflineTrackIdentityPolicy.from(it.url, it.title, it.creator) }
                if (known != null) {
                    val matching = publicQueue["https://www.youtube.com/watch?v=${known.videoId}"]
                    changed = publishKnown(song, known, matching?.artwork.orEmpty(), raw, saved == null, mayContinue) || changed
                }
            }
        }
        return changed
    }

    /** Stage outside the index monitor; revalidate membership and source at commit.
     * The artwork lock is always acquired before the short index commit lock.
     * Removal uses just the index monitor, so it never waits for image copying.
     */
    private fun publishKnown(song: OfflineSong, identity: OfflineTrackIdentity, original: String,
        expectedMetadata: String?, requiresLink: Boolean, active: () -> Boolean): Boolean = synchronized(artworkLock) {
        if (!validId(song.id)) return false
        val destination = runCatching { artFile(song.id) }.getOrNull()
        if (!active()) return false
        val raw = record(song.id)
        if (raw != expectedMetadata) return false
        val fence = OfflineArtworkCommitFence(song, raw, identity.videoId, requiresLink)
        val encoded = OfflineTrackIdentityPolicy.encode(identity)
        val hasArt = runCatching { destination?.isFile == true && destination.length() in 1..MAX_ART_BYTES }.getOrDefault(false)
        val staged = if (hasArt || destination == null) null else runCatching {
            copyCachedArtwork(song.id, identity.videoId, original, active)
        }.getOrNull()
        try {
            synchronized(OfflineLibraryIndexLock.monitor) {
                val library = OfflineAudioLibrary(context)
                val current = runCatching { library.find(song.id) }.getOrNull()
                if (!fence.allows(current, record(song.id),
                        linkPrefs.getString("v_${fence.videoId}", null), active()) ||
                    library.fileFor(song) == null) return false
                if (!prefs.contains(song.id) && prefs.all.keys.count(::validId) >= OfflineAudioLibrary.MAX_SONGS) return false
                val publishedArt = staged != null && destination != null && runCatching {
                    Files.move(staged.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
                }.isSuccess
                val changed = raw != encoded || publishedArt
                if (!changed) return false
                val revision = revision(context)
                val committed = prefs.edit().putString(song.id, encoded)
                    .putLong(REVISION_KEY, if (revision == Long.MAX_VALUE) 0L else revision + 1).commit()
                if (!committed && publishedArt) destination?.delete()
                committed
            }
        } finally { staged?.delete() }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    private fun copyCachedArtwork(id: String, videoId: String, original: String, active: () -> Boolean): File? {
        val destination = artFile(id) ?: return null
        require(artworkHome.isDirectory || artworkHome.mkdirs())
        val entries = artworkHome.listFiles().orEmpty().filter { it.isFile && !Files.isSymbolicLink(it.toPath()) }
        if (entries.sumOf { it.length() } + MAX_ART_BYTES > 64L * 1024 * 1024) return null
        val cache = cachedImages() ?: return null
        for (candidate in OfflineArtworkRecoveryPolicy.cacheCandidates(videoId, original)) {
            if (!active()) return null
            val snapshot = cache.openSnapshot(candidate) ?: continue
            val temp = File(artworkHome, destination.name + ".tmp")
            require(!Files.isSymbolicLink(temp.toPath()))
            var keep = false
            try {
                snapshot.use snapshotUse@ {
                    val file = snapshot.data.toFile()
                    if (!file.isFile || file.length() !in 1..MAX_ART_BYTES) return@snapshotUse
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, bounds)
                    if (bounds.outWidth !in 1..4096 || bounds.outHeight !in 1..4096 ||
                        bounds.outMimeType !in setOf("image/jpeg", "image/png", "image/webp") ||
                        NightWaveArtworkPolicy.isUnavailableLargeStill(candidate, bounds.outWidth, bounds.outHeight)) return@snapshotUse
                    file.inputStream().use { input -> FileOutputStream(temp).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var count = 0L
                        while (true) {
                            if (!active()) return@snapshotUse
                            val n = input.read(buffer); if (n < 0) break
                            count += n
                            require(count <= MAX_ART_BYTES)
                            output.write(buffer, 0, n)
                        }
                        output.flush(); output.fd.sync()
                        keep = active()
                    } }
                }
                if (keep) return temp
            } finally { if (!keep) temp.delete() }
        }
        return null
    }
    fun remove(id: String) = synchronized(OfflineLibraryIndexLock.monitor) {
        if (!validId(id)) return
        val revision = revision(context)
        prefs.edit().remove(id).putLong(REVISION_KEY,
            if (revision == Long.MAX_VALUE) 0L else revision + 1).apply()
        artFile(id)?.let { if (it.isFile) it.delete() }
        Unit
    }
    companion object {
        const val PREFS_NAME = "nightwave_offline_identity_v1"
        const val REVISION_KEY = "artwork_revision"
        fun revision(context: Context): Long = runCatching {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(REVISION_KEY, 0L)
        }.getOrDefault(0L)
        private const val MAX_ART_BYTES = 1024L * 1024L
        private val artworkLock = Any()
    }
}
