package com.nightwave.player.v2.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class NightWaveLocaleFormatTest {
    private val bn = Locale.forLanguageTag("bn-BD")
    @Test fun banglaDigitsAndTwoDigitSeconds() {
        assertEquals("১২:০৩", NightWaveLocaleFormat.duration(723_000, bn))
        assertEquals("১২৩৪৫", NightWaveLocaleFormat.number(12_345, bn))
    }
    @Test fun englishSecondsMinutesAndHours() {
        assertEquals("0:00", NightWaveLocaleFormat.duration(0, Locale.ENGLISH))
        assertEquals("1:01", NightWaveLocaleFormat.duration(61_999, Locale.ENGLISH))
        assertEquals("1:02:03", NightWaveLocaleFormat.duration(3_723_000, Locale.ENGLISH))
    }
    @Test fun negativeDurationIsClampedWithoutChangingStoredValue() {
        assertEquals("০:০০", NightWaveLocaleFormat.duration(Long.MIN_VALUE, bn))
    }
    @Test fun longDurationsDoNotOverflow() {
        assertEquals("2562047788015:12:55", NightWaveLocaleFormat.duration(Long.MAX_VALUE, Locale.ENGLISH))
    }
    @Test fun digitWidthCannotTruncateLargeValues() {
        assertEquals("১২৩", NightWaveLocaleFormat.number(123, bn, 2))
        assertEquals("০৫", NightWaveLocaleFormat.number(5, bn, Int.MAX_VALUE))
    }
}
