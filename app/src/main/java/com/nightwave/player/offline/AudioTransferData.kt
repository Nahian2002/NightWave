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
    private const val RECEIPT_VERSION = "finish02_receipt_version"
    private const val RECEIPT_BYTES = "finish02_receipt_bytes"
    private const val RECEIPT_ELAPSED = "finish02_receipt_elapsed_ms"

    fun putCompletion(builder: androidx.work.Data.Builder, receipt: CompletedAudioTransfer?) {
        if (receipt?.label() == null) return
        builder.putInt(RECEIPT_VERSION, 1)
            .putLong(RECEIPT_BYTES, receipt.receivedBytes)
            .putLong(RECEIPT_ELAPSED, receipt.elapsedMs)
    }

    fun completion(data: androidx.work.Data): CompletedAudioTransfer? =
        if (data.getInt(RECEIPT_VERSION, 0) != 1) null else
            CompletedAudioTransfer(data.getLong(RECEIPT_BYTES, 0),
                data.getLong(RECEIPT_ELAPSED, 0)).takeIf { it.label() != null }

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
