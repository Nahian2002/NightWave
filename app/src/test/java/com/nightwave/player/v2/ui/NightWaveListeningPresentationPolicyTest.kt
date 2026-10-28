package com.nightwave.player.v2.ui

import com.nightwave.player.v2.insights.ListeningTrendPoint
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class NightWaveListeningPresentationPolicyTest {
    private val p = NightWaveListeningPresentationPolicy
    private fun point(start: String, end: String = start) = ListeningTrendPoint(
        LocalDate.parse(start).toEpochDay(), LocalDate.parse(end).toEpochDay(), 60_000, 1)
    @Test fun incompleteMinutesDoNotBecomeZeroOrAnInventedFullMinute() {
        assertEquals("0 min", p.duration(0)); assertEquals("0 min", p.duration(-1))
        assertEquals("<1 min", p.duration(1)); assertEquals("<1 min", p.duration(59_999))
        assertEquals("1 min", p.duration(60_000))
    }
    @Test fun longDurationsUseReadableUnitsWithoutOverflow() {
        assertEquals("59 min", p.duration(3_599_999))
        assertEquals("1 h", p.duration(3_600_000)); assertEquals("1 h 1 min", p.duration(3_660_000))
        assertFalse(p.duration(Long.MAX_VALUE).contains("-"))
    }
    @Test fun dailyUtcDatesUseReadableCalendarLabels() {
        assertEquals("22 Oct", p.dateLabel(point("2026-10-22")))
        assertEquals("29 Feb", p.dateLabel(point("2024-02-29")))
    }
    @Test fun groupedDatesKeepActualPeriodBoundaries() {
        assertEquals("22–28 Oct", p.dateLabel(point("2026-10-22", "2026-10-28")))
        assertEquals("29 Sep–5 Oct", p.dateLabel(point("2026-09-29", "2026-10-05")))
    }
    @Test fun crossYearGroupsExposeBothYears() {
        assertEquals("29 Dec 2025–4 Jan 2026", p.dateLabel(point("2025-12-29", "2026-01-04")))
    }
    @Test fun chartContextRetainsUtcAndYearWithoutInventingAnEmptyPeriod() {
        assertEquals("22–28 Oct 2026 · UTC", p.periodLabel(listOf(point("2026-10-22"),point("2026-10-28"))))
        assertEquals("Dates in UTC", p.periodLabel(emptyList()))
    }
    @Test fun measuredNarrowLargeAndInvalidConfigurationsStackStatistics() {
        assertEquals(3,p.columns(320f,1f)); assertEquals(3,p.columns(600f,1.2f))
        for (width in listOf(319f,0f,-1f,Float.NaN,Float.POSITIVE_INFINITY)) assertEquals(1,p.columns(width,1f))
        for (scale in listOf(1.3f,2f,3f,Float.NaN,Float.POSITIVE_INFINITY,-1f)) assertEquals(1,p.columns(600f,scale))
    }
}
