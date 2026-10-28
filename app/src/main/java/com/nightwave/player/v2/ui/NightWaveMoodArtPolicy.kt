package com.nightwave.player.v2.ui

import java.util.Locale
import kotlin.math.min

/** Complete normalized motifs, drawn inside a separate header rather than behind captions. */
internal object NightWaveMoodArtPolicy {
    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float,
        val strong: Boolean = false, val strokeDp: Float = 1.5f)
    data class Ring(val x: Float, val y: Float, val radius: Float, val filled: Boolean = false)
    data class Motif(val lines: List<Line> = emptyList(), val rings: List<Ring> = emptyList())
    data class Viewport(val left: Float, val top: Float, val side: Float)

    private val night = Motif(rings = listOf(.42f, .31f, .20f).map { Ring(.5f, .5f, it) })
    private val focus = Motif(lines = (0..5).map { i ->
        val x = .15f + i * .14f
        Line(x, .16f, x, .84f, i == 2, if (i == 2) 3f else 1.5f)
    })
    private val pulse = Motif(lines = (0..8).map { i ->
        val x = .10f + i * .10f; val amplitude = (i % 4 + 1) * .09f
        Line(x, .5f - amplitude, x, .5f + amplitude, i % 3 == 0, 2f)
    })
    private val acoustic = Motif(lines = (0..4).map { i ->
        val y = .13f + i * .12f
        Line(.12f, y, .88f, y + .20f, i == 2)
    })
    private val energy = Motif(lines = (0..4).map { i ->
        val x = .14f + i * .18f
        Line(x, .86f, x, .72f - i * .14f, i == 4, 4f)
    })
    private val morning = Motif(lines = (0..2).map { i ->
        val y = .64f + i * .12f
        Line(.12f, y, .88f, y)
    }, rings = listOf(Ring(.5f, .30f, .18f, true)))

    fun motif(title: String): Motif = when (title.trim().lowercase(Locale.ROOT)) {
        "after hours" -> night
        "deep focus" -> focus
        "desi pulse" -> pulse
        "unplugged" -> acoustic
        "momentum" -> energy
        else -> morning
    }

    /** Padding includes round caps/strokes; tiny or invalid bounds draw nothing safely. */
    fun viewport(width: Float, height: Float, padding: Float): Viewport? {
        if (!width.isFinite() || !height.isFinite() || !padding.isFinite() || padding < 0f) return null
        val side = min(width, height) - padding * 2
        if (side <= 0f) return null
        return Viewport((width - side) / 2, (height - side) / 2, side)
    }
}
