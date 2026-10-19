package com.nightwave.player.auto

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.nightwave.player.core.VideoLink
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.offline.OfflineAudioLibrary
import com.nightwave.player.playback.QueueEntry

/**
 * Media3 car hierarchy built from existing app-owned local indexes.
 * Browsing does not play audio, resolve public stream links or scan the network.
 */
sealed interface AutoPlaySelection {
    data class Offline(val songId: String, val queueIds: List<String>) : AutoPlaySelection
    data class Streaming(val queue: List<QueueEntry>, val index: Int) : AutoPlaySelection
}

class AutoBrowseCatalog(private val context: Context) {
    private val library by lazy { MusicLibrary(context) }
    private val local by lazy { OfflineAudioLibrary(context) }

    private fun folder(id: String, title: String): MediaItem =
        MediaItem.Builder().setMediaId(id)
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle(title).setIsBrowsable(true).setIsPlayable(false).build())
            .build()

    private fun localItem(song: com.nightwave.player.offline.OfflineSong): MediaItem =
        MediaItem.Builder().setMediaId(AutoBrowsePolicy.offlineId(song.id))
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle(song.title.take(110)).setArtist("Imported music")
                .setIsBrowsable(false).setIsPlayable(true).build())
            .build()

    private fun publicItem(parent: String, video: VideoPreview): MediaItem =
        MediaItem.Builder().setMediaId(AutoBrowsePolicy.publicId(parent, video.url))
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle(video.title.take(160)).setArtist(video.author.take(120))
                .setIsBrowsable(false).setIsPlayable(true).build())
            .build()

    private fun localSongs() = runCatching { local.songs() }.getOrDefault(emptyList())
        .take(400)

    private fun playlistNames() = runCatching { library.playlistNames() }
        .getOrDefault(emptyList()).take(300)

    private fun videos(parent: String): List<VideoPreview>? {
        val raw = when (parent) {
            AutoBrowsePolicy.FAVORITES -> library.favorites()
            AutoBrowsePolicy.RECENT -> library.recent()
            else -> {
                val name = AutoBrowsePolicy.playlistName(parent) ?: return null
                if (name !in playlistNames()) return null
                library.playlist(name)
            }
        }
        return raw.filter { VideoLink.normalize(it.url) == it.url }
            .distinctBy { it.url }.take(100)
    }

    /** null means an unknown node; an empty list is a valid empty folder. */
    fun children(parent: String): List<MediaItem>? = when (parent) {
        AutoBrowsePolicy.ROOT -> listOf(
            folder(AutoBrowsePolicy.IMPORTED, "Imported music"),
            folder(AutoBrowsePolicy.FAVORITES, "Liked songs"),
            folder(AutoBrowsePolicy.RECENT, "Recently played"),
            folder(AutoBrowsePolicy.PLAYLISTS, "My playlists")
        )
        AutoBrowsePolicy.IMPORTED -> localSongs().map(::localItem)
        AutoBrowsePolicy.PLAYLISTS -> playlistNames().map {
            folder(AutoBrowsePolicy.playlistId(it), it.take(80))
        }
        else -> videos(parent)?.map { publicItem(parent, it) }
    }

    fun item(id: String): MediaItem? {
        if (id == AutoBrowsePolicy.ROOT) return folder(id, "NightWave")
        val source = if (id == AutoBrowsePolicy.IMPORTED ||
            id == AutoBrowsePolicy.FAVORITES ||
            id == AutoBrowsePolicy.RECENT ||
            id == AutoBrowsePolicy.PLAYLISTS) AutoBrowsePolicy.ROOT
        else if (AutoBrowsePolicy.playlistName(id) != null) AutoBrowsePolicy.PLAYLISTS
        else if (AutoBrowsePolicy.offlineTarget(id) != null) AutoBrowsePolicy.IMPORTED
        else AutoBrowsePolicy.publicTarget(id)?.first ?: return null
        return children(source)?.firstOrNull { it.mediaId == id }
    }

    fun selection(id: String): AutoPlaySelection? {
        val offlineId = AutoBrowsePolicy.offlineTarget(id)
        if (offlineId != null) {
            val song = localSongs().firstOrNull { it.id == offlineId } ?: return null
            if (local.fileFor(song) == null) return null
            return AutoPlaySelection.Offline(offlineId, localSongs().map { it.id })
        }
        val (parent, url) = AutoBrowsePolicy.publicTarget(id) ?: return null
        val songs = runCatching { videos(parent) }.getOrNull() ?: return null
        val chosen = songs.indexOfFirst { it.url == url }
        if (chosen < 0) return null
        return AutoPlaySelection.Streaming(songs.map {
            QueueEntry(it.url, it.title, it.author, it.thumbnail)
        }, chosen)
    }

    fun search(query: String): List<MediaItem> {
        val needle = query.trim().lowercase()
        if (needle.length !in 1..60) return emptyList()
        val sources = mutableListOf<MediaItem>()
        sources.addAll(children(AutoBrowsePolicy.IMPORTED).orEmpty())
        sources.addAll(children(AutoBrowsePolicy.FAVORITES).orEmpty())
        sources.addAll(children(AutoBrowsePolicy.RECENT).orEmpty())
        for (name in playlistNames().take(20)) {
            sources.addAll(children(AutoBrowsePolicy.playlistId(name)).orEmpty().take(20))
        }
        return sources.distinctBy(MediaItem::mediaId)
            .filter { it.mediaMetadata.title?.contains(needle, ignoreCase = true) == true ||
                it.mediaMetadata.artist?.contains(needle, ignoreCase = true) == true }
            .take(AutoBrowsePolicy.MAX_RESULTS)
    }
}
