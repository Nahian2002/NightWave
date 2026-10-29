package com.nightwave.player.offline

/** Shared, privacy-safe WorkManager fields. Old queued requests keep old labels. */
internal object AudioTransferData {
    const val TAG = "nightwave-validated-range-v1"
    const val DETAILS = "nw136_transfer_details"
    const val RESUMED = "nw136_resumed_bytes"
}
