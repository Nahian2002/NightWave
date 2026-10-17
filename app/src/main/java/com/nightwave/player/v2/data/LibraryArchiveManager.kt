package com.nightwave.player.v2.data

import android.content.Context
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.model.NightWaveTheme
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manual guest export/restore. Restore is explicitly requested by the user:
 * it never runs during app startup or Google sign-in.
 */
class LibraryArchiveManager(context: Context) {
    private val app = context.applicationContext
    private val profileStore = GuestProfileStore(app)
    private val library = MusicLibrary(app)
    private val libraryPrefs = app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
    private val recoveryPrefs = app.getSharedPreferences("nightwave_archive_recovery_v1", Context.MODE_PRIVATE)

    fun export(): ByteArray {
        val profile = profileStore.load()
        return LibraryBackupCodec.encode(LibraryArchive(
            displayName = profile.displayName,
            theme = profile.theme.name,
            avatar = profile.avatarStyle,
            favorites = library.favorites(),
            playlists = library.playlistNames().map { it to library.playlist(it) }
        ))
    }

    fun preview(bytes: ByteArray): LibraryArchive = LibraryBackupCodec.decode(bytes)

    private fun serialize(videos: List<VideoPreview>): String = JSONArray().also { arr ->
        videos.forEach {
            arr.put(JSONObject().put("url", it.url).put("title", it.title)
                .put("creator", it.author).put("artwork", it.thumbnail)
                .put("duration", it.durationSeconds))
        }
    }.toString()

    /**
     * Fully stage library in one SharedPreferences commit.
     * Retain a separate recovery snapshot BEFORE mutation. Only then update
     * profile settings; if that step fails roll the library back immediately.
     */
    @Synchronized
    fun restoreWithConfirmation(archive: LibraryArchive): Boolean {
        // Round-trip normalization validates the entire input before modifying data.
        val checked = LibraryBackupCodec.decode(LibraryBackupCodec.encode(archive))
        val previous = libraryPrefs.all.toMap()
        val backup = JSONObject().also { snapshot ->
            previous.forEach { (key, value) ->
                if (value is String) snapshot.put(key, value)
            }
        }.toString()
        if (!recoveryPrefs.edit().putString("previous_library", backup).commit()) return false

        val change = libraryPrefs.edit().clear()
            .putString("favorites", serialize(checked.favorites))
            .putString("playlist_names", JSONArray(checked.playlists.map { it.first }).toString())
            .putString("recent", serialize(library.recent())) // preserve local-only history
        checked.playlists.forEach { (name, tracks) ->
            change.putString("list_" + name, serialize(tracks))
        }
        if (!change.commit()) return false
        val oldProfile = profileStore.load()
        val proposed = oldProfile.copy(
            displayName = checked.displayName,
            avatarStyle = checked.avatar,
            theme = NightWaveTheme.valueOf(checked.theme)
        )
        if (!profileStore.save(proposed)) {
            restoreOldLibrary(backup)
            return false
        }
        // Recovery snapshot intentionally retained; user can recover manually
        // on a future release, never overwritten by an automatic import.
        return true
    }

    private fun restoreOldLibrary(serialized: String): Boolean = runCatching {
        val obj = JSONObject(serialized)
        val editor = libraryPrefs.edit().clear()
        obj.keys().forEach { key -> editor.putString(key, obj.getString(key)) }
        editor.commit()
    }.getOrDefault(false)
}
