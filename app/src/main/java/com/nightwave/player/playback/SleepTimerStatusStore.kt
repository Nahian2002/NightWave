package com.nightwave.player.playback

import android.content.Context
import android.os.SystemClock

/**
 * Read-only UI status, not a persistent timer/scheduling engine.
 * On service creation/destruction the timer is explicitly cleared.
 * No music identifiers or usage data are stored.
 */
class SleepTimerStatusStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "nightwave_sleep_timer_ephemeral", Context.MODE_PRIVATE
    )

    fun setDeadline(deadlineElapsedMs: Long): Boolean =
        preferences.edit().putLong("deadline_elapsed_ms", deadlineElapsedMs).commit()

    fun clear(): Boolean =
        preferences.edit().remove("deadline_elapsed_ms").commit()

    fun remainingMs(nowElapsedMs: Long = SystemClock.elapsedRealtime()): Long =
        SleepTimerPolicy.remainingMs(nowElapsedMs,
            preferences.getLong("deadline_elapsed_ms", 0L))
}
