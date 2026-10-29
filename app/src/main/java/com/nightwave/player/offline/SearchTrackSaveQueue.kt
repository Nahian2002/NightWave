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
import com.nightwave.player.net.VideoPreview
import java.util.concurrent.TimeUnit

/** User-initiated saving of an explicitly permissioned public, audio-only track. */
object SearchTrackSaveQueue {
    const val TAG = "nightwave-search-offline"
    const val TITLE_TAG = "nightwave-search-title:"
    const val KEY_URL = "search_track_url"
    const val KEY_TITLE = "search_track_title"
    const val KEY_ID = "offline_saved_id"
    const val KEY_ERROR = "offline_save_error"
    const val KEY_RECEIVED = "offline_received_bytes"
    const val KEY_TOTAL = "offline_expected_bytes"
    const val KEY_VERIFYING = "nw79_transfer_verifying"

    fun enqueue(context: Context, video: VideoPreview): Operation {
        val canonical = SearchTrackSavePolicy.canonicalTrack(video.url)
            ?: throw IllegalArgumentException("Invalid search track URL")
        require(video.title.isNotBlank()) { "Track title is missing" }
        val safeTitle = SearchTrackSavePolicy.safeTitle(video.title)
        val videoId = canonical.substringAfter("v=")
        val request = OneTimeWorkRequestBuilder<SearchTrackSaveWorker>()
            .setInputData(workDataOf(KEY_URL to canonical, KEY_TITLE to safeTitle))
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .addTag(AudioTransferData.TAG)
            .addTag(TITLE_TAG + safeTitle.take(70))
            .addTag(OfflineSaveRetryPolicy.sourceTag(canonical))
            .addTag(OfflineSaveRetryPolicy.CREATED_TAG_PREFIX + System.currentTimeMillis())
            .build()
        return WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "nightwave-search-audio-" + videoId,
            ExistingWorkPolicy.KEEP, request
        )
    }
}
