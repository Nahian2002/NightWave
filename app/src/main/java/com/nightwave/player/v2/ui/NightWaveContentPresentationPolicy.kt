package com.nightwave.player.v2.ui

import com.nightwave.player.v2.model.Palette
import kotlin.math.roundToInt

internal object NightWaveContentPresentationPolicy {
    fun errorSurface(palette: Palette): Long {
        fun channel(shift: Int): Long {
            val base = (palette.surface shr shift) and 255L
            val error = (palette.danger shr shift) and 255L
            return (base * .88 + error * .12).roundToInt().toLong()
        }
        return 0xFF000000L or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
    // Used only under the existing Automatic collections heading; preserve membership/count evidence.
    fun automaticSummary(detail: String): String = detail.replace(" · Automatic collection", "")

    fun historySummary(songs: Int, entries: Int): String {
        val count = songs.coerceAtLeast(0)
        val noun = if (count == 1) "song" else "songs"
        return "$count $noun" + if (entries > count) " · $entries plays" else ""
    }
}
