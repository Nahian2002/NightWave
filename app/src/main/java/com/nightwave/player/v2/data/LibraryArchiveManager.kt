package com.nightwave.player.v2.data

import android.content.Context
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.library.LegacyLibraryTransaction
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

    fun export(): ByteArray = LegacyLibraryTransaction.exclusive {
        val profile = profileStore.load()
        LibraryBackupCodec.encode(LibraryArchive(
            displayName = profile.displayName,
            theme = profile.theme.name,
            avatar = profile.avatarStyle,
            favorites = library.favorites(),
            playlists = library.playlistNames().map { it to library.playlist(it) }
        ))
    }

    fun preview(bytes: ByteArray): LibraryArchive = LibraryBackupCodec.decode(bytes)

    /**
     * Apply the explicitly confirmed merge only if the library still exactly
     * matches what the user saw in the comparison dialog. Comparison and
     * replacement share the legacy transaction lock; playlist edits made while
     * the dialog was open cannot be overwritten by a stale merge.
     */
    fun mergeIfUnchanged(
        plan: com.nightwave.player.v2.cloud.CloudMergePlan
    ): CloudMergeApplyResult = LegacyLibraryTransaction.exclusive {
        val current = runCatching { export() }.getOrNull()
            ?: return@exclusive CloudMergeApplyResult.FAILED
        if (!current.contentEquals(plan.expectedLocalBackup)) {
            return@exclusive CloudMergeApplyResult.STALE_PREVIEW
        }
        if (!plan.hasChanges) return@exclusive CloudMergeApplyResult.NO_CHANGES
        // Re-check limits and format before touching any preference keys.
        val validated = runCatching {
            LibraryBackupCodec.decode(LibraryBackupCodec.encode(plan.mergedArchive))
        }.getOrNull() ?: return@exclusive CloudMergeApplyResult.FAILED
        // A merge cannot alter this device's profile.
        val currentProfile = LibraryBackupCodec.decode(current)
        if (validated.displayName != currentProfile.displayName ||
            validated.theme != currentProfile.theme ||
            validated.avatar != currentProfile.avatar) {
            return@exclusive CloudMergeApplyResult.FAILED
        }
        if (restoreWithConfirmation(validated)) CloudMergeApplyResult.APPLIED
        else CloudMergeApplyResult.FAILED
    }

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
    fun restoreWithConfirmation(
        archive: LibraryArchive, restoredHistory: List<VideoPreview>? = null
    ): Boolean = LegacyLibraryTransaction.exclusive {
        // Validate all library data before modifying anything.
        val checked = LibraryBackupCodec.decode(LibraryBackupCodec.encode(archive))
        val validatedHistory = restoredHistory?.let {
            com.nightwave.player.v2.cloud.CloudBackupCodec.validatedHistory(it)
        }
        val previous = libraryPrefs.all.toMap()
        val backup = JSONObject().also { snapshot ->
            previous.forEach { (key, value) ->
                if (value is String) snapshot.put(key, value)
            }
        }.toString()
        if (!recoveryPrefs.edit().putString("previous_library", backup).commit()) return@exclusive false

        // Only replace declared playlists. Keep unlisted/orphaned keys for recovery.
        val currentNames = library.playlistNames()
        val change = libraryPrefs.edit()
            .putString("favorites", serialize(checked.favorites))
            .putString("playlist_names", JSONArray(checked.playlists.map { it.first }).toString())
            .putString("recent", serialize(validatedHistory ?: library.recent()))
            // History changes only on explicit confirmed cloud-history restore.
        val replacementNames = checked.playlists.map { it.first }.toSet()
        for (name in currentNames) {
            if (name !in replacementNames) change.remove("list_" + name)
        }
        checked.playlists.forEach { (name, tracks) ->
            change.putString("list_" + name, serialize(tracks))
        }
        if (!change.commit()) return@exclusive false
        val oldProfile = profileStore.load()
        val proposed = oldProfile.copy(
            displayName = checked.displayName,
            avatarStyle = checked.avatar,
            theme = NightWaveTheme.valueOf(checked.theme)
        )
        if (!profileStore.save(proposed)) {
            restoreOldLibrary(backup)
            return@exclusive false
        }
        // Recovery snapshot intentionally retained; user can recover manually
        // on a future release, never overwritten by an automatic import.
        true
    }

    private fun restoreOldLibrary(serialized: String): Boolean = runCatching {
        val obj = JSONObject(serialized)
        val before = obj.keys().asSequence().associateWith { key -> obj.getString(key) }
        val current = libraryPrefs.all
        val editor = libraryPrefs.edit()
        // Undo only the imported collection fields, not unrelated app preferences.
        for (key in current.keys) {
            if ((key in setOf("favorites", "recent", "playlist_names") || key.startsWith("list_")) &&
                key !in before) editor.remove(key)
        }
        for ((key, value) in before) editor.putString(key, value)
        editor.commit()
    }.getOrDefault(false)
}

enum class CloudMergeApplyResult { APPLIED, NO_CHANGES, STALE_PREVIEW, FAILED }
