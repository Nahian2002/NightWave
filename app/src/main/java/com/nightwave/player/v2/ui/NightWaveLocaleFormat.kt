package com.nightwave.player.v2.ui

import java.text.NumberFormat
import java.util.Locale

/** Presentation only: never used for keys, URLs, file formats or stored numbers. */
internal object NightWaveLocaleFormat {
    fun number(value: Long, locale: Locale, minimumDigits: Int = 1): String =
        NumberFormat.getIntegerInstance(locale).apply {
            isGroupingUsed = false
            minimumIntegerDigits = minimumDigits.coerceIn(1, 2)
        }.format(value)

    fun duration(ms: Long, locale: Locale): String {
        val seconds = ms.coerceAtLeast(0L) / 1000L
        val tail = number(seconds % 60, locale, 2)
        return if (seconds >= 3600L)
            number(seconds / 3600, locale) + ":" + number(seconds / 60 % 60, locale, 2) + ":" + tail
        else number(seconds / 60, locale) + ":" + tail
    }
}
