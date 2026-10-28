package com.nightwave.player.v2.ui

import com.nightwave.player.v2.insights.ListeningTrendPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Read-only labels for the existing UTC aggregates; never changes collection, consent or totals. */
internal object NightWaveListeningPresentationPolicy {
    private val day = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val dated = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH)
    fun duration(milliseconds: Long): String {
        if (milliseconds <= 0L) return "0 min"
        val minutes = milliseconds / 60_000L
        if (minutes == 0L) return "<1 min"
        if (minutes < 60L) return "$minutes min"
        val hours = minutes / 60L
        val rest = minutes % 60L
        return if (rest == 0L) "$hours h" else "$hours h $rest min"
    }
    fun columns(widthDp: Float, fontScale: Float): Int =
        if (widthDp.isFinite() && widthDp >= 320f && fontScale.isFinite() && fontScale in .7f..<1.3f) 3 else 1

    private fun range(start: LocalDate, end: LocalDate, includeYear: Boolean): String = when {
        start == end -> (if (includeYear) dated else day).format(start)
        start.year != end.year -> "${dated.format(start)}–${dated.format(end)}"
        start.month == end.month -> "${start.dayOfMonth}–${(if (includeYear) dated else day).format(end)}"
        else -> "${day.format(start)}–${(if (includeYear) dated else day).format(end)}"
    }
    fun dateLabel(point: ListeningTrendPoint): String = range(
        LocalDate.ofEpochDay(point.startDayUtc), LocalDate.ofEpochDay(point.endDayUtc), false)
    fun periodLabel(points: List<ListeningTrendPoint>): String = if (points.isEmpty()) "Dates in UTC" else
        range(LocalDate.ofEpochDay(points.first().startDayUtc), LocalDate.ofEpochDay(points.last().endDayUtc), true) + " · UTC"
}
