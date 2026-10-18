package com.nightwave.player.playback

import android.content.Context

/** Local diagnostic receipt, not a guarantee that a hardware output route applies an effect. */
class EqualizerStatusStore(context: Context) {
    private val prefs = context.getSharedPreferences("nightwave_equalizer_status", Context.MODE_PRIVATE)
    data class Status(val selected: Int, val active: Boolean, val message: String, val commandId: String)
    fun read(): Status = Status(
        prefs.getInt("selected", 0),
        prefs.getBoolean("active", false),
        prefs.getString("message", "Off") ?: "Off",
        prefs.getString("commandId", "") ?: ""
    )
    fun write(selected: Int, active: Boolean, message: String, commandId: String = "") {
        prefs.edit().putInt("selected", selected).putBoolean("active", active)
            .putString("message", message.take(100)).putString("commandId", commandId.take(80)).commit()
    }
}
