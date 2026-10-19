package com.nightwave.player.v2.library

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Optional playlist organization metadata, never a replacement for playlist
 * storage. Mutations commit atomically, and a damaged snapshot is never
 * overwritten by a seemingly empty "repaired" layout.
 */
data class PlaylistFolderSnapshot(
    val folders: List<String> = emptyList(),
    val placements: Map<String, String> = emptyMap()
) {
    fun folderOf(playlist: String): String? =
        placements[playlist]?.takeIf { it in folders }

    fun playlists(folder: String, validNames: List<String>): List<String> =
        validNames.filter { folderOf(it) == folder }
}

object PlaylistFolderPolicy {
    const val MAX_FOLDERS = 40
    const val MAX_PLACEMENTS = 300
    fun validName(name: String): Boolean =
        name == name.trim() && name.length in 1..60 &&
            name.none { Character.isISOControl(it) || it == '/' || it == '\\' }

    fun add(before: PlaylistFolderSnapshot, name: String): PlaylistFolderSnapshot? =
        if (!validName(name) || before.folders.size >= MAX_FOLDERS ||
            before.folders.any { it.equals(name, ignoreCase = true) }) null
        else before.copy(folders = before.folders + name)

    fun rename(before: PlaylistFolderSnapshot, old: String, updated: String): PlaylistFolderSnapshot? {
        if (old !in before.folders || !validName(updated) ||
            before.folders.any { it != old && it.equals(updated, ignoreCase = true) }) return null
        return before.copy(folders = before.folders.map { if (it == old) updated else it },
            placements = before.placements.mapValues { (_, folder) ->
                if (folder == old) updated else folder
            })
    }

    fun delete(before: PlaylistFolderSnapshot, folder: String): PlaylistFolderSnapshot? =
        if (folder !in before.folders) null else before.copy(
            folders = before.folders.filterNot { it == folder },
            placements = before.placements.filterValues { it != folder })

    fun assign(before: PlaylistFolderSnapshot, playlist: String,
        folder: String?, knownPlaylists: List<String>): PlaylistFolderSnapshot? {
        if (playlist !in knownPlaylists ||
            (folder != null && folder !in before.folders)) return null
        val changed = before.placements.toMutableMap()
        if (folder == null) changed.remove(playlist)
        else changed[playlist] = folder
        return before.copy(placements = changed)
    }

    fun playlistRenamed(before: PlaylistFolderSnapshot, old: String,
        newName: String): PlaylistFolderSnapshot {
        val folder = before.placements[old] ?: return before
        if (newName.isBlank() || old == newName) return before
        val changed = before.placements.toMutableMap()
        changed.remove(old)
        changed[newName] = folder
        return before.copy(placements = changed)
    }

    fun playlistDeleted(before: PlaylistFolderSnapshot, name: String): PlaylistFolderSnapshot =
        before.copy(placements = before.placements - name)
}

class PlaylistFolderStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_playlist_folders_v1", Context.MODE_PRIVATE
    )
    private val lock = Companion.lock
    companion object { private val lock = Any() }

    private fun decode(raw: String?): PlaylistFolderSnapshot? {
        if (raw == null) return if (prefs.contains("snapshot")) null else PlaylistFolderSnapshot()
        return runCatching {
            val obj = JSONObject(raw)
            val rawFolders = obj.getJSONArray("folders")
            val rawAssignments = obj.getJSONObject("placements")
            require(rawFolders.length() <= PlaylistFolderPolicy.MAX_FOLDERS)
            require(rawAssignments.length() <= PlaylistFolderPolicy.MAX_PLACEMENTS)
            val folders = (0 until rawFolders.length()).map { rawFolders.getString(it) }
            require(folders.all(PlaylistFolderPolicy::validName))
            require(folders.distinctBy { it.lowercase() }.size == folders.size)
            val names = rawAssignments.keys()
            val assignments = linkedMapOf<String, String>()
            while (names.hasNext()) {
                val key = names.next()
                require(key.length in 1..120)
                val value = rawAssignments.getString(key)
                require(value in folders)
                assignments[key] = value
            }
            PlaylistFolderSnapshot(folders, assignments)
        }.getOrNull()
    }

    fun snapshot(): PlaylistFolderSnapshot = synchronized(lock) {
        decode(runCatching { prefs.getString("snapshot", null) }.getOrNull())
            ?: PlaylistFolderSnapshot()
    }

    private fun mutate(change: (PlaylistFolderSnapshot) -> PlaylistFolderSnapshot?): Boolean =
        synchronized(lock) {
            val before = decode(runCatching { prefs.getString("snapshot", null) }.getOrNull())
                ?: return@synchronized false
            val after = change(before) ?: return@synchronized false
            if (after.placements.size > PlaylistFolderPolicy.MAX_PLACEMENTS) return@synchronized false
            val obj = JSONObject().put("folders", JSONArray(after.folders))
                .put("placements", JSONObject(after.placements))
            runCatching { prefs.edit().putString("snapshot", obj.toString()).commit() }
                .getOrDefault(false)
        }

    fun create(name: String): Boolean = mutate { PlaylistFolderPolicy.add(it, name) }
    fun rename(old: String, updated: String): Boolean =
        mutate { PlaylistFolderPolicy.rename(it, old, updated) }
    fun delete(folder: String): Boolean = mutate { PlaylistFolderPolicy.delete(it, folder) }
    fun assign(playlist: String, folder: String?, known: List<String>): Boolean =
        mutate { PlaylistFolderPolicy.assign(it, playlist, folder, known) }
    fun playlistRenamed(old: String, updated: String): Boolean =
        mutate { PlaylistFolderPolicy.playlistRenamed(it, old, updated) }
    fun playlistDeleted(name: String): Boolean =
        mutate { PlaylistFolderPolicy.playlistDeleted(it, name) }
}
