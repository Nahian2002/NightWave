package com.nightwave.player.core

object TimeLabel {
    fun format(milliseconds: Long): String {
        val safe = milliseconds.coerceAtLeast(0) / 1000
        val hours = safe / 3600
        val minutes = (safe / 60) % 60
        val seconds = safe % 60
        return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        else "${safe / 60}:${seconds.toString().padStart(2, '0')}"
    }
}
