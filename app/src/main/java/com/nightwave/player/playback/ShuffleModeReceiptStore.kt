package com.nightwave.player.playback

import android.content.Context

class ShuffleModeReceiptStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nightwave_public_shuffle_receipt_v1", Context.MODE_PRIVATE)

    fun acknowledge(requestId: String, enabled: Boolean): Boolean {
        if (requestId.length !in 1..64) return false
        return runCatching { prefs.edit().putString("request_id", requestId).putBoolean("enabled", enabled).commit() }.getOrDefault(false)
    }

    fun matches(requestId: String, enabled: Boolean): Boolean =
        requestId.isNotBlank() && runCatching {
            prefs.getString("request_id", null) == requestId &&
                prefs.getBoolean("enabled", !enabled) == enabled
        }.getOrDefault(false)
}
