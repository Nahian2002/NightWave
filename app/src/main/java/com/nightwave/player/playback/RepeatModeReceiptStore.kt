package com.nightwave.player.playback

import android.content.Context

/** Short-lived service confirmation for foreground repeat UI; contains no music data. */
class RepeatModeReceiptStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_repeat_receipt_v1", Context.MODE_PRIVATE
    )

    fun acknowledge(requestId: String, mode: PlaybackQueue.Repeat): Boolean {
        if (requestId.length !in 1..64) return false
        return prefs.edit().putString("request_id", requestId)
            .putString("mode", mode.name).commit()
    }

    fun matches(requestId: String, mode: PlaybackQueue.Repeat): Boolean =
        requestId.isNotEmpty() &&
            prefs.getString("request_id", null) == requestId &&
            prefs.getString("mode", null) == mode.name
}
