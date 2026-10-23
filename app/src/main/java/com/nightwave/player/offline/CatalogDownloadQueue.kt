package com.nightwave.player.offline

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * WorkManager owns each permitted catalog job, not an Activity's lifecycle.
 * Records survive Activity recreation and ordinary application restarts.
 * KEEP prevents duplicate *unfinished* jobs for the same original file.
 */
object CatalogDownloadQueue {
    const val TAG = "nightwave-nw23-offline-queue"
    const val TITLE_TAG = "nw23-title:"
    const val KEY_URL = "source_url"
    const val KEY_NAME = "safe_file_name"
    const val KEY_TITLE = "display_title"
    const val KEY_LICENSE = "license_short_name"
    const val KEY_DESCRIPTION = "source_description_url"
    const val KEY_BYTES = "expected_source_bytes"
    const val KEY_SHA1 = "expected_source_sha1"
    const val KEY_DONE = "saved_library_id"
    const val KEY_SAVED_TITLE = "saved_library_title"
    const val KEY_ERROR = "download_error"
    const val KEY_RECEIVED = "downloaded_bytes"
    const val KEY_TOTAL = "total_bytes"

    fun identifier(url: String, sha1: String): String =
        CatalogWorkIdentity.uniqueName(url, sha1)

    fun valid(entry: FreeAudioCandidate): Boolean =
        FreeAudioCatalogPolicy.permittedLicense(entry.licenseName) &&
            FreeAudioCatalogPolicy.safeMediaUrl(entry.sourceUrl) &&
            FreeAudioCatalogPolicy.safeDescriptionUrl(entry.descriptionUrl) &&
            FreeAudioIntegrity.isValidSourceHash(entry.sha1Base36) &&
            FreeAudioCatalogPolicy.validSize(entry.bytes) &&
            entry.saveName.length in 5..120 &&
            runCatching {
                PermittedAudioDownloadPolicy.prepare(entry.sourceUrl, entry.saveName)
            }.isSuccess

    fun enqueue(context: Context, entry: FreeAudioCandidate) {
        require(valid(entry)) { "Catalog entry is not eligible for offline download." }
        val safeTitle = entry.title.trim().take(80).ifBlank { "Audio file" }
        val request = OneTimeWorkRequestBuilder<CatalogDownloadWorker>()
            .setInputData(workDataOf(
                KEY_URL to entry.sourceUrl,
                KEY_NAME to entry.saveName,
                KEY_TITLE to safeTitle,
                KEY_LICENSE to entry.licenseName,
                KEY_DESCRIPTION to entry.descriptionUrl,
                KEY_BYTES to entry.bytes,
                KEY_SHA1 to entry.sha1Base36
            ))
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .addTag(TITLE_TAG + safeTitle)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            identifier(entry.sourceUrl, entry.sha1Base36),
            ExistingWorkPolicy.KEEP, request
        )
    }
}
