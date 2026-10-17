package com.nightwave.player.v2.insights

import java.time.LocalDate

/**
 * The UTC calendar day in which a bounded listening pulse was committed.
 * This is a daily aggregate, not a raw event timeline or proof of all audio
 * played on that date (process loss, opt-out and suspend can undercount).
 */
data class DailyListeningBucket(
    val epochDayUtc: Long,
    val observedMs: Long,
    val qualifiedSessions: Int
)

data class ListeningTrendPoint(
    val startDayUtc: Long,
    val endDayUtc: Long,
    val observedMs: Long,
    val qualifiedSessions: Int
) {
    fun labelUtc(): String {
        val start = LocalDate.ofEpochDay(startDayUtc).toString()
        val end = LocalDate.ofEpochDay(endDayUtc).toString()
        return if (startDayUtc == endDayUtc) start else "$start – $end"
    }
}

/** Pure, deterministic selectors; no network, artificial totals or date guessing. */
object ListeningTrendEngine {
    const val MAX_DAYS = 30
    const val MS_PER_DAY = 86_400_000L

    fun utcEpochDay(nowWallMs: Long): Long = Math.floorDiv(nowWallMs, MS_PER_DAY)

    fun rolling(
        saved: List<DailyListeningBucket>,
        todayUtc: Long,
        days: Int
    ): List<ListeningTrendPoint> {
        require(days in 1..MAX_DAYS)
        val lower = todayUtc - days + 1
        val grouped = saved.filter { it.epochDayUtc in lower..todayUtc }
            .distinctBy { it.epochDayUtc }
            .associateBy { it.epochDayUtc }
        return (0 until days).map { index ->
            val day = lower + index
            val found = grouped[day]
            ListeningTrendPoint(day, day, found?.observedMs ?: 0L,
                found?.qualifiedSessions ?: 0)
        }
    }

    /** Compress the 30-day overview into at most five honest UTC date ranges. */
    fun groupedByWeek(days: List<ListeningTrendPoint>): List<ListeningTrendPoint> =
        days.chunked(7).map { week ->
            ListeningTrendPoint(
                startDayUtc = week.first().startDayUtc,
                endDayUtc = week.last().endDayUtc,
                observedMs = week.sumOf { it.observedMs },
                qualifiedSessions = week.sumOf { it.qualifiedSessions }
            )
        }
}
