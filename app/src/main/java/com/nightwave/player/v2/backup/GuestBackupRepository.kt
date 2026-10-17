package com.nightwave.player.v2.backup

import android.content.Context
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.v2.data.GuestProfileStore
import com.nightwave.player.v2.model.ListeningPrivacy
import java.io.File
import java.io.FileOutputStream

/**
 * Guest backup orchestrator. Existing library and profile remain authoritative.
 * This is not Google sync: users must explicitly save a file somewhere durable.
 */
class GuestBackupRepository(context: Context) {
    private val app = context.applicationContext
    private val library = MusicLibrary(app)
    private val profiles = GuestProfileStore(app)
    private val rescue = File(app.filesDir, "nightwave_v2_pending_restore_rescue.json")

    @Synchronized
    fun createExport(includeHistory: Boolean): ByteArray {
        check(!rescue.exists()) { "A previous restore needs recovery first." }
        val settings = profiles.load()
        val snapshot = library.snapshotForBackup()
        return BackupFormat.export(PortableBackup(
            profile = BackupPreferences.from(settings),
            library = if (includeHistory) snapshot else snapshot.copy(recent = emptyList()),
            createdAtMs = System.currentTimeMillis(),
            includesHistory = includeHistory
        ))
    }

    fun inspect(raw: ByteArray): Pair<PortableBackup, ImportPreview> {
        val parsed = BackupFormat.import(raw)
        return parsed to BackupMerge.preview(parsed)
    }

    /**
     * Writes an app-private rescue copy, then commits library in one operation.
     * Profile is written second. On a failed profile write, the old library is
     * restored. After unexpected process death, recoverPendingRestore() restores
     * the pre-import copy before opening library views.
     *
     * SHA-256 only checks file integrity; importing untrusted backups is not
     * an authentication mechanism.
     */
    @Synchronized
    fun applyImport(data: PortableBackup, strategy: ImportStrategy, restoreSettings: Boolean): Boolean {
        if (strategy == ImportStrategy.KEEP_EXISTING && !restoreSettings) return true
        if (rescue.exists()) throw IllegalStateException("A previous restore requires recovery.")
        // Decode our canonical export to enforce EVERY boundary before any write.
        BackupFormat.import(BackupFormat.export(data))
        val oldProfile = profiles.load()
        val oldLibrary = library.snapshotForBackup()
        val proposed = BackupMerge.prepare(oldLibrary, data, strategy)
        if (strategy == ImportStrategy.KEEP_EXISTING && restoreSettings) {
            if (!profiles.save(data.profile.onto(oldProfile))) {
                throw IllegalStateException("Profile settings could not be written.")
            }
            return true
        }
        val original = BackupFormat.export(PortableBackup(
            BackupPreferences.from(oldProfile), oldLibrary,
            System.currentTimeMillis(), includesHistory = true
        ))
        try {
            FileOutputStream(rescue).use { out ->
                out.write(original)
                out.fd.sync()
            }
        } catch (error: Exception) {
            rescue.delete()
            throw IllegalStateException("Could not safely prepare an import recovery copy.", error)
        }
        try {
            if (!library.commitImportedLibrary(proposed))
                throw IllegalStateException("Library commit failed.")
            if (restoreSettings && !profiles.save(data.profile.onto(oldProfile)))
                throw IllegalStateException("Profile settings commit failed.")
            if (!rescue.delete()) {
                throw IllegalStateException("Could not finalize the recovery journal.")
            }
            return true
        } catch (problem: Exception) {
            val oldLibraryRestored = library.commitImportedLibrary(oldLibrary)
            val profileRestored = profiles.save(oldProfile)
            if (oldLibraryRestored && profileRestored) rescue.delete()
            throw IllegalStateException(
                if (oldLibraryRestored && profileRestored)
                    "Import failed; your previous library and settings were restored."
                else "Import failed and recovery is pending. Do not clear app storage.", problem
            )
        }
    }

    @Synchronized
    fun recoverPendingRestore(): Boolean {
        if (!rescue.exists()) return true
        val raw = rescue.readBytes()
        val original = BackupFormat.import(raw)
        val oldProfile = profiles.load()
        if (!library.commitImportedLibrary(original.library)) return false
        if (!profiles.save(original.profile.onto(oldProfile))) return false
        return rescue.delete()
    }

    fun hasPendingRecovery(): Boolean = rescue.exists()
}
