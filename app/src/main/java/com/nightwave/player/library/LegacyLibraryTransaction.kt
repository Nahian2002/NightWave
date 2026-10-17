package com.nightwave.player.library

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.data.room.LegacyLibrarySnapshotParser
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * One in-process mutation lane shared by the legacy player, Compose preview
 * and manual import. Each logical library change commits in ONE preference edit.
 *
 * SharedPreferences has no cross-process compare-and-swap or SQLite transaction.
 * Do not enable Room as a writer while legacy mutations remain possible.
 */
object LegacyLibraryTransaction {
    private val lock = Any()
    private const val SOURCE = "nightwave_library_v1"
    private const val RECOVERY = "nightwave_v2_library_write_recovery"
    private const val LAST_BEFORE = "last_before"
    private const val LAST_AFTER = "last_after_fingerprint"

    fun <T> exclusive(action: () -> T): T = synchronized(lock) { action() }

    private fun source(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(SOURCE, Context.MODE_PRIVATE)

    private fun existingStrings(prefs: SharedPreferences): Map<String, String> =
        prefs.all.filterKeys {
            it in setOf("favorites", "recent", "playlist_names") || it.startsWith("list_")
        }.mapValues { (key, value) ->
            require(value is String) { "Library data '$key' is not text" }
            value
        }

    private fun readCollections(values: Map<String, String>): LibraryCollections {
        val parsed = LegacyLibrarySnapshotParser.parse(values)
        return LibraryCollections(parsed.favorites, parsed.recent, parsed.playlists)
    }

    private fun jsonTracks(tracks: List<VideoPreview>): String =
        JSONArray().apply {
            for (video in tracks) put(
                JSONObject().put("url", video.url).put("title", video.title)
                    .put("creator", video.author).put("artwork", video.thumbnail)
                    .put("duration", video.durationSeconds)
            )
        }.toString()

    private fun desiredStrings(collections: LibraryCollections): Map<String, String> =
        buildMap {
            put("favorites", jsonTracks(collections.favorites))
            put("recent", jsonTracks(collections.recent))
            put("playlist_names", JSONArray(collections.names()).toString())
            for ((name, tracks) in collections.playlists) put("list_$name", jsonTracks(tracks))
        }

    private fun fingerprint(values: Map<String, String>): String {
        val canonical = values.toSortedMap().entries.joinToString("\u0000") { (key, value) ->
            key.length.toString() + ":" + key + ":" + value.length + ":" + value
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun encodeSnapshot(values: Map<String, String>): String =
        JSONObject().apply { values.forEach { (key, value) -> put(key, value) } }.toString()

    /**
     * Returns a failed result without modifying user data if the source cannot
     * be parsed or its pre-write recovery snapshot cannot be persisted.
     */
    fun mutate(context: Context, change: LibraryChange): LibraryChangeResult = exclusive {
        val preferences = source(context)
        val raw = try {
            existingStrings(preferences)
        } catch (error: Exception) {
            Log.w("NightWaveLibrary", "Blocked unsafe library mutation: unreadable source")
            return@exclusive LibraryChangeResult(LibraryCollections(emptyList(), emptyList(), emptyList()), false, false)
        }
        val before = try {
            readCollections(raw)
        } catch (error: Exception) {
            Log.w("NightWaveLibrary", "Blocked unsafe library mutation: source validation failed")
            return@exclusive LibraryChangeResult(LibraryCollections(emptyList(), emptyList(), emptyList()), false, false)
        }
        val planned = LibraryMutationPlan.apply(before, change)
        if (!planned.accepted || !planned.changed) return@exclusive planned

        val newValues = desiredStrings(planned.updated)
        val afterValues = raw.toMutableMap()
        for (name in before.names()) {
            val key = "list_" + name
            if (key !in newValues) afterValues.remove(key)
        }
        afterValues.putAll(newValues)
        val checkpoint = fingerprint(afterValues)
        val backup = encodeSnapshot(raw)
        // Keep the last two snapshots for diagnosis. A successful commit is never
        // claimed unless the pre-write snapshot is durably committed.
        val recovery = context.applicationContext.getSharedPreferences(RECOVERY, Context.MODE_PRIVATE)
        val saved = recovery.edit()
            .putString("previous_before", recovery.getString(LAST_BEFORE, null))
            .putString("previous_after_digest", recovery.getString(LAST_AFTER, null))
            .putString(LAST_BEFORE, backup)
            .putString(LAST_AFTER, checkpoint)
            .commit()
        if (!saved) {
            Log.e("NightWaveLibrary", "Cannot persist recovery snapshot; write cancelled")
            return@exclusive planned.copy(accepted = false, changed = false, added = false)
        }

        // The editor is committed once for the complete collection, including
        // playlist names and their ordered entries. Unrelated preferences stay.
        val editor = preferences.edit()
        // Remove only playlists that were explicitly registered in the
        // validated previous snapshot. Preserve orphaned keys for recovery.
        for (name in before.names()) {
            val key = "list_" + name
            if (key !in newValues) editor.remove(key)
        }
        for ((key, value) in newValues) editor.putString(key, value)
        if (!editor.commit()) {
            Log.e("NightWaveLibrary", "Library write failed; verify recovery before retry")
            return@exclusive planned.copy(accepted = false, changed = false, added = false)
        }
        planned
    }

    /**
     * Last-write recovery is only callable by a deliberate user recovery action,
     * never automatically during startup. A subsequent edit invalidates the
     * after-digest guard so old snapshots cannot overwrite newer changes.
     */
    fun recoverLastWriteIfUnchanged(context: Context): Boolean = exclusive {
        val prefs = source(context)
        val recovery = context.applicationContext.getSharedPreferences(RECOVERY, Context.MODE_PRIVATE)
        val saved = recovery.getString(LAST_BEFORE, null) ?: return@exclusive false
        val expectedAfter = recovery.getString(LAST_AFTER, null) ?: return@exclusive false
        val current = try {
            existingStrings(prefs)
        } catch (_: Exception) { return@exclusive false }
        val digest = try {
            fingerprint(current)
        } catch (_: Exception) { return@exclusive false }
        if (digest != expectedAfter) return@exclusive false
        val obj = try { JSONObject(saved) } catch (_: Exception) { return@exclusive false }
        val previous = try {
            obj.keys().asSequence().associateWith { key -> obj.getString(key) }
        } catch (_: Exception) { return@exclusive false }
        // Verify snapshot before ANY changes. Only library keys may be restored.
        try { LegacyLibrarySnapshotParser.parse(previous) }
        catch (_: Exception) { return@exclusive false }

        val editor = prefs.edit()
        for (key in current.keys) if (key !in previous) editor.remove(key)
        for ((key, value) in previous) editor.putString(key, value)
        val ok = editor.commit()
        if (ok) recovery.edit().remove(LAST_AFTER).commit() // one-shot recovery
        ok
    }
}
