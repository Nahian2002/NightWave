package com.nightwave.player.offline

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

/**
 * Failure-safe copy of an already authorized and verified staged audio file.
 *
 * The source is never moved or deleted here: an index commit failure must
 * leave cloud download checkpoints able to retry the same staged bytes.
 * A temporary copy is fsynced and reverified before promotion. The caller
 * owns the index transaction and must hold OfflineLibraryIndexLock.monitor.
 */
internal object OfflineFileRegistration {
    fun copyVerifiedAndRegister(
        staged: File,
        destination: File,
        verifyCopiedFile: (File) -> Boolean,
        registerIndex: () -> Boolean
    ) {
        require(staged.isFile && staged.canRead()) { "Verified staged audio is unavailable" }
        require(destination.parentFile?.isDirectory == true) { "Offline destination is unavailable" }
        val pending = File(destination.parentFile, destination.name + ".pending")
        require(!destination.exists() && !pending.exists()) {
            "Offline destination is already occupied"
        }
        var promoted = false
        try {
            staged.inputStream().use { input ->
                FileOutputStream(pending).use { output ->
                    input.copyTo(output, 32_768)
                    output.fd.sync()
                }
            }
            check(verifyCopiedFile(pending)) { "Copied cloud audio did not verify" }
            // No REPLACE_EXISTING: protect files created by another operation.
            Files.move(pending.toPath(), destination.toPath())
            promoted = true
            check(registerIndex()) { "Could not register verified cloud audio" }
        } catch (error: Exception) {
            // Only this attempt's unindexed copy may be removed. The staged
            // checkpoint audio remains available for explicit retry.
            pending.delete()
            if (promoted) destination.delete()
            throw error
        }
    }
}
