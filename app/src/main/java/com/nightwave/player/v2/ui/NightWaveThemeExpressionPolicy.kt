package com.nightwave.player.v2.ui

import com.nightwave.player.v2.model.Palette
import java.util.Locale

/** Theme-only roles. Semantic section colors never imply artwork, statistics or a provider identity. */
object NightWaveThemeExpressionPolicy {
    fun sectionSurface(palette: Palette, secondary: Boolean): Long =
        if (secondary) palette.secondarySoft else palette.accentSoft

    fun sectionAccent(palette: Palette, secondary: Boolean): Long =
        if (secondary) palette.secondaryAccent else palette.accent

    fun secondaryMood(title: String): Boolean =
        title.trim().lowercase(Locale.ROOT) in setOf("after hours", "unplugged", "golden hour")

    /** A full-width singleton uses the same bounded horizontal feature treatment. */
    fun featuredMixCell(pairSize: Int): Boolean = pairSize == 1
}
