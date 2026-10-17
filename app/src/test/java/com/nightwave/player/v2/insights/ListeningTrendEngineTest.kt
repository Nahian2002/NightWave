package com.nightwave.player.v2.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningTrendEngineTest {
    @Test fun emptyHistoryProducesZeroNotInventedPlayback() {
        val points = ListeningTrendEngine.rolling(emptyList(), 100L, 7)
        assertEquals(7, points.size)
        assertTrue(points.all { it.observedMs == 0L && it.qualifiedSessions == 0 })
        assertEquals(94L, points.first().startDayUtc)
        assertEquals(100L, points.last().endDayUtc)
    }

    @Test fun includesOnlyRealDatesInsideChosenWindow() {
        val saved = listOf(
            DailyListeningBucket(60L, 9999L, 7),
            DailyListeningBucket(96L, 120_000L, 2),
            DailyListeningBucket(100L, 60_000L, 1),
            DailyListeningBucket(101L, 9999L, 7)
        )
        val seven = ListeningTrendEngine.rolling(saved, 100L, 7)
        assertEquals(180_000L, seven.sumOf { it.observedMs })
        assertEquals(3, seven.sumOf { it.qualifiedSessions })
        assertEquals(0L, seven.last().endDayUtc - 100L)
    }

    @Test fun utcDayBoundaryIsDeterministic() {
        assertEquals(0L, ListeningTrendEngine.utcEpochDay(0L))
        assertEquals(0L, ListeningTrendEngine.utcEpochDay(86_399_999L))
        assertEquals(1L, ListeningTrendEngine.utcEpochDay(86_400_000L))
        assertEquals(-1L, ListeningTrendEngine.utcEpochDay(-1L))
    }

    @Test fun thirtyDayViewGroupsIntoFiveHonestRanges() {
        val thirty = ListeningTrendEngine.rolling(
            listOf(DailyListeningBucket(200L, 60_000L, 1)), 200L, 30)
        val grouped = ListeningTrendEngine.groupedByWeek(thirty)
        assertEquals(5, grouped.size)
        assertEquals(171L, grouped.first().startDayUtc)
        assertEquals(200L, grouped.last().endDayUtc)
        assertEquals(60_000L, grouped.sumOf { it.observedMs })
        assertEquals(1, grouped.sumOf { it.qualifiedSessions })
    }

    @Test fun invalidWindowCannotExpandMemoryHistory() {
        assertThrows(IllegalArgumentException::class.java) {
            ListeningTrendEngine.rolling(emptyList(), 100, 31)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ListeningTrendEngine.rolling(emptyList(), 100, 0)
        }
    }

    @Test fun exposesTrueUtcDayLabels() {
        val day = ListeningTrendPoint(0L, 0L, 5_000L, 0)
        assertEquals("1970-01-01", day.labelUtc())
        val grouped = ListeningTrendPoint(0L, 6L, 5_000L, 1)
        assertEquals("1970-01-01 – 1970-01-07", grouped.labelUtc())
    }

    @Test fun sevenDayViewRetainsFullCalendarContinuity() {
        val list = ListeningTrendEngine.rolling(
            listOf(DailyListeningBucket(99L, 120_000L, 2)), 100L, 7)
        assertEquals((94L..100L).toList(), list.map { it.startDayUtc })
    }
}
