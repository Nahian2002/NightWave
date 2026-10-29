package com.nightwave.player.offline

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Shared, privacy-safe WorkManager fields. Old queued requests keep old labels. */
internal object AudioTransferData {
    const val TAG = "nightwave-validated-range-v1"
    const val DETAILS = "nw136_transfer_details"
    const val RESUMED = "nw136_resumed_bytes"

    /** On API 26–30, consult persisted state on the transfer thread only.
     * WorkManager records explicit cancellation before stopping its worker.
     * A missing/failed read fails closed; process death retains synced bytes
     * without requiring onStopped to run. Never block the UI/onStopped thread.
     */
    fun legacyMayRetain(context: Context, id: UUID): Boolean = runCatching {
        val info = WorkManager.getInstance(context).getWorkInfoById(id)
            .get(2, TimeUnit.SECONDS)
        info != null && info.state != WorkInfo.State.CANCELLED
    }.getOrDefault(false)
}
