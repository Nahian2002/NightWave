package com.nightwave.player.offline

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.Operation
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Background transfer of a USER-AUTHORIZED direct HTTPS audio file.
 * The permission checkbox is confirmed by the launching Activity.
 * No YouTube/video URLs, streaming manifests, cookies, or DRM sources.
 * No global account or server needed. WorkManager owns the lifetime.
 */
object DirectAudioDownloadQueue {
    const val TAG = "nightwave-authorized-direct-audio"
    const val TITLE_TAG = "nightwave-direct-title:"
    const val KEY_URL = "authorized_audio_url"
    const val KEY_NAME = "authorized_audio_name"
    const val KEY_DONE = "authorized_saved_song_id"
    const val KEY_ERROR = "authorized_download_failure"
    const val KEY_RECEIVED = "authorized_download_received"
    const val KEY_TOTAL = "authorized_download_total"
    const val KEY_VERIFYING = "nw79_transfer_verifying"

    fun enqueue(context: Context, source: PermittedAudioSource): Operation {
        // Independently revalidate even if callers already checked.
        val verified = PermittedAudioDownloadPolicy.prepare(source.url, source.fileName)
        require(verified.url == source.url && verified.fileName == source.fileName &&
            verified.extension == source.extension) {
            "Download link must be a validated direct HTTPS audio file."
        }
        val bytes = MessageDigest.getInstance("SHA-256").digest(
            (source.url + "\n" + source.fileName).toByteArray(Charsets.UTF_8)
        )
        val id = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val request = OneTimeWorkRequestBuilder<DirectAudioDownloadWorker>()
            .setInputData(workDataOf(
                KEY_URL to verified.url,
                KEY_NAME to verified.fileName
            ))
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .addTag(TITLE_TAG + verified.fileName.take(72))
            .addTag(OfflineSaveRetryPolicy.CREATED_TAG_PREFIX + System.currentTimeMillis())
            .build()
        return WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "nightwave-direct-audio-" + id,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
