package com.nightwave.player.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeLabelTest {
    @Test fun clockFormatting() {
        assertEquals("0:00", TimeLabel.format(0))
        assertEquals("0:01", TimeLabel.format(1_999))
        assertEquals("1:02", TimeLabel.format(62_000))
        assertEquals("59:59", TimeLabel.format(3_599_000))
        assertEquals("1:00:00", TimeLabel.format(3_600_000))
        assertEquals("0:00", TimeLabel.format(-5))
    }
}
